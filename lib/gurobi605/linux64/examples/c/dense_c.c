/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QP model:

     minimize    x + y + x^2 + x*y + y^2 + y*z + z^2
     subject to  x + 2 y + 3 z >= 4
                 x +   y       >= 1

   The example illustrates the use of dense matrices to store A and Q
   (and dense vectors for the other relevant data).  We don't recommend
   that you use dense matrices, but this example may be helpful if you
   already have your data in this format.
*/

#include <stdlib.h>
#include <stdio.h>
#include "gurobi_c.h"

/*
  Solve an LP/QP/MILP/MIQP represented using dense matrices.  This
  routine assumes that A and Q are both stored in row-major order.
  It returns 1 if the optimization succeeds.  When successful,
  it returns the optimal objective value in 'objvalP', and the
  optimal solution vector in 'solution'.
*/

static int
dense_optimize(GRBenv *env,
               int     rows,
               int     cols,
               double *c,     /* linear portion of objective function */
               double *Q,     /* quadratic portion of objective function */
               double *A,     /* constraint matrix */
               char   *sense, /* constraint senses */
               double *rhs,   /* RHS vector */
               double *lb,    /* variable lower bounds */
               double *ub,    /* variable upper bounds */
               char   *vtype, /* variable types (continuous, binary, etc.) */
               double *solution,
               double *objvalP)
{
  GRBmodel *model = NULL;
  int       i, j, optimstatus;
  int       error = 0;
  int       success = 0;

  /* Create an empty model */

  error = GRBnewmodel(env, &model, "dense", cols, c, lb, ub, vtype, NULL);
  if (error) goto QUIT;

  error = GRBaddconstrs(model, rows, 0, NULL, NULL, NULL, sense, rhs, NULL);
  if (error) goto QUIT;

  /* Integrate new rows and columns */

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* Populate A matrix */

  for (i = 0; i < rows; i++) {
    for (j = 0; j < cols; j++) {
      if (A[i*cols+j] != 0) {
        error = GRBchgcoeffs(model, 1, &i, &j, &A[i*cols+j]);
        if (error) goto QUIT;
      }
    }
  }

  /* Populate Q matrix */

  if (Q) {
    for (i = 0; i < cols; i++) {
      for (j = 0; j < cols; j++) {
        if (Q[i*cols+j] != 0) {
          error = GRBaddqpterms(model, 1, &i, &j, &Q[i*cols+j]);
          if (error) goto QUIT;
        }
      }
    }
  }

  /* Integrate new coefficients */

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* Write model to 'dense.lp' */

  error = GRBwrite(model, "dense.lp");
  if (error) goto QUIT;

  /* Optimize model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Capture solution information */

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  if (optimstatus == GRB_OPTIMAL) {

    error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, objvalP);
    if (error) goto QUIT;

    error = GRBgetdblattrarray(model, GRB_DBL_ATTR_X, 0, cols, solution);
    if (error) goto QUIT;

    success = 1;
  }

QUIT:

  /* Error reporting */

  if (error) {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free model */

  GRBfreemodel(model);

  return success;
}

int
main(int   argc,
     char *argv[])
{
  GRBenv *env     = NULL;
  int     error   = 0;
  double  c[]     = {1, 1, 0};
  double  Q[3][3] = {{1, 1, 0}, {0, 1, 1}, {0, 0, 1}};
  double  A[2][3] = {{1, 2, 3}, {1, 1, 0}};
  char    sense[] = {'>', '>'};
  double  rhs[]   = {4, 1};
  double  lb[]    = {0, 0, 0};
  double  sol[3];
  int     solved;
  double  objval;

  /* Create environment */

  error = GRBloadenv(&env, "dense.log");
  if (error) goto QUIT;

  /* Solve the model */

  solved = dense_optimize(env, 2, 3, c, &Q[0][0], &A[0][0], sense, rhs, lb,
                          NULL, NULL, sol, &objval);

  if (solved)
    printf("Solved: x=%.4f, y=%.4f, z=%.4f\n", sol[0], sol[1], sol[2]);

QUIT:

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
