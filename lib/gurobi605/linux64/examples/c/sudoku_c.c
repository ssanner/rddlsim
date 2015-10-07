/* Copyright 2014, Gurobi Optimization, Inc. */
/*
  Sudoku example.

  The Sudoku board is a 9x9 grid, which is further divided into a 3x3 grid
  of 3x3 grids.  Each cell in the grid must take a value from 0 to 9.
  No two grid cells in the same row, column, or 3x3 subgrid may take the
  same value.

  In the MIP formulation, binary variables x[i,j,v] indicate whether
  cell <i,j> takes value 'v'.  The constraints are as follows:
    1. Each cell must take exactly one value (sum_v x[i,j,v] = 1)
    2. Each value is used exactly once per row (sum_i x[i,j,v] = 1)
    3. Each value is used exactly once per column (sum_j x[i,j,v] = 1)
    4. Each value is used exactly once per 3x3 subgrid (sum_grid x[i,j,v] = 1)

  Input datasets for this example can be found in examples/data/sudoku*.
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "gurobi_c.h"

#define SUBDIM  3
#define DIM    (SUBDIM*SUBDIM)

int
main(int   argc,
     char *argv[])
{
  FILE     *fp    = NULL;
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       board[DIM][DIM];
  char      inputline[100];
  int       ind[DIM];
  double    val[DIM];
  double    lb[DIM*DIM*DIM];
  char      vtype[DIM*DIM*DIM];
  char     *names[DIM*DIM*DIM];
  char      namestorage[10*DIM*DIM*DIM];
  char     *cursor;
  int       optimstatus;
  double    objval;
  int       zero = 0;
  int       i, j, v, ig, jg, count;
  int       error = 0;

  if (argc < 2) {
    fprintf(stderr, "Usage: sudoku_c datafile\n");
    exit(1);
  }

  fp = fopen(argv[1], "r");
  if (fp == NULL) {
    fprintf(stderr, "Error: unable to open input file %s\n", argv[1]);
    exit(1);
  }

  for (i = 0; i < DIM; i++) {
    fgets(inputline, 100, fp);
    if (strlen(inputline) < 9) {
      fprintf(stderr, "Error: not enough board positions specified\n");
      exit(1);
    }
    for (j = 0; j < DIM; j++) {
      board[i][j] = (int) inputline[j] - (int) '1';
      if (board[i][j] < 0 || board[i][j] >= DIM)
        board[i][j] = -1;
    }
  }

  /* Create an empty model */

  cursor = namestorage;
  for (i = 0; i < DIM; i++) {
    for (j = 0; j < DIM; j++) {
      for (v = 0; v < DIM; v++) {
        if (board[i][j] == v)
          lb[i*DIM*DIM+j*DIM+v] = 1;
        else
          lb[i*DIM*DIM+j*DIM+v] = 0;
        vtype[i*DIM*DIM+j*DIM+v] = GRB_BINARY;

        names[i*DIM*DIM+j*DIM+v] = cursor;
        sprintf(names[i*DIM*DIM+j*DIM+v], "x[%d,%d,%d]", i, j, v+1);
        cursor += strlen(names[i*DIM*DIM+j*DIM+v]) + 1;
      }
    }
  }

  /* Create environment */

  error = GRBloadenv(&env, "sudoku.log");
  if (error) goto QUIT;

  /* Create new model */

  error = GRBnewmodel(env, &model, "sudoku", DIM*DIM*DIM, NULL, lb, NULL,
                      vtype, names);
  if (error) goto QUIT;

  /* Each cell gets a value */

  for (i = 0; i < DIM; i++) {
    for (j = 0; j < DIM; j++) {
      for (v = 0; v < DIM; v++) {
        ind[v] = i*DIM*DIM + j*DIM + v;
        val[v] = 1.0;
      }

      error = GRBaddconstr(model, DIM, ind, val, GRB_EQUAL, 1.0, NULL);
      if (error) goto QUIT;
    }
  }

  /* Each value must appear once in each row */

  for (v = 0; v < DIM; v++) {
    for (j = 0; j < DIM; j++) {
      for (i = 0; i < DIM; i++) {
        ind[i] = i*DIM*DIM + j*DIM + v;
        val[i] = 1.0;
      }

      error = GRBaddconstr(model, DIM, ind, val, GRB_EQUAL, 1.0, NULL);
      if (error) goto QUIT;
    }
  }

  /* Each value must appear once in each column */

  for (v = 0; v < DIM; v++) {
    for (i = 0; i < DIM; i++) {
      for (j = 0; j < DIM; j++) {
        ind[j] = i*DIM*DIM + j*DIM + v;
        val[j] = 1.0;
      }

      error = GRBaddconstr(model, DIM, ind, val, GRB_EQUAL, 1.0, NULL);
      if (error) goto QUIT;
    }
  }

  /* Each value must appear once in each subgrid */

  for (v = 0; v < DIM; v++) {
    for (ig = 0; ig < SUBDIM; ig++) {
      for (jg = 0; jg < SUBDIM; jg++) {
        count = 0;
        for (i = ig*SUBDIM; i < (ig+1)*SUBDIM; i++) {
          for (j = jg*SUBDIM; j < (jg+1)*SUBDIM; j++) {
            ind[count] = i*DIM*DIM + j*DIM + v;
            val[count] = 1.0;
            count++;
          }
        }

        error = GRBaddconstr(model, DIM, ind, val, GRB_EQUAL, 1.0, NULL);
        if (error) goto QUIT;
      }
    }
  }

  /* Optimize model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Write model to 'sudoku.lp' */

  error = GRBwrite(model, "sudoku.lp");
  if (error) goto QUIT;

  /* Capture solution information */

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, &objval);
  if (error) goto QUIT;

  printf("\nOptimization complete\n");
  if (optimstatus == GRB_OPTIMAL)
    printf("Optimal objective: %.4e\n", objval);
  else if (optimstatus == GRB_INF_OR_UNBD)
    printf("Model is infeasible or unbounded\n");
  else
    printf("Optimization was stopped early\n");
  printf("\n");

QUIT:

  /* Error reporting */

  if (error) {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
