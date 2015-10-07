/* Copyright 2014, Gurobi Optimization, Inc. */

/*
  Solve a traveling salesman problem on a randomly generated set of
  points using lazy constraints.   The base MIP model only includes
  'degree-2' constraints, requiring each node to have exactly
  two incident edges.  Solutions to this model may contain subtours -
  tours that don't visit every node.  The lazy constraint callback
  adds new constraints to cut them off.
*/

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"


/* Define structure to pass data to the callback function */

struct callback_data {
  int n;
};


/* Given an integer-feasible solution 'sol', find the smallest
   sub-tour.  Result is returned in 'tour', and length is
   returned in 'tourlenP'. */

static void
findsubtour(int     n,
            double *sol,
            int    *tourlenP,
            int    *tour)
{
  int i, node, len, start;
  int bestind, bestlen;
  int *seen = NULL;

  seen = (int *) malloc(n*sizeof(int));
  if (seen == NULL) {
    fprintf(stderr, "Out of memory\n");
    exit(1);
  }

  for (i = 0; i < n; i++)
    seen[i] = 0;

  start   = 0;
  bestlen = n+1;
  bestind = -1;
  while (start < n) {
    for (node = 0; node < n; node++)
      if (seen[node] == 0)
        break;
    if (node == n)
      break;
    for (len = 0; len < n; len++) {
      tour[start+len] = node;
      seen[node] = 1;
      for (i = 0; i < n; i++) {
        if (sol[node*n+i] > 0.5 && !seen[i]) {
          node = i;
          break;
        }
      }
      if (i == n) {
        len++;
        if (len < bestlen) {
          bestlen = len;
          bestind = start;
        }
        start += len;
        break;
      }
    }
  }

  for (i = 0; i < bestlen; i++)
    tour[i] = tour[bestind+i];
  *tourlenP = bestlen;

  free(seen);
}


/* Subtour elimination callback.  Whenever a feasible solution is found,
   find the shortest subtour, and add a subtour elimination constraint
   if that tour doesn't visit every node. */

int __stdcall
subtourelim(GRBmodel *model,
            void     *cbdata,
            int       where,
            void     *usrdata)
{
  struct callback_data *mydata = (struct callback_data *) usrdata;
  int n = mydata->n;
  int *tour = NULL;
  double *sol = NULL;
  int i, j, len, nz;
  int error = 0;

  if (where == GRB_CB_MIPSOL) {
    sol  = (double *) malloc(n*n*sizeof(double));
    tour = (int *)    malloc(n*sizeof(int));
    if (sol == NULL || tour == NULL) {
      fprintf(stderr, "Out of memory\n");
      exit(1);
    }

    GRBcbget(cbdata, where, GRB_CB_MIPSOL_SOL, sol);

    findsubtour(n, sol, &len, tour);

    if (len < n) {
      int    *ind = NULL;
      double *val = NULL;

      ind = (int *)    malloc(len*(len-1)/2*sizeof(int));
      val = (double *) malloc(len*(len-1)/2*sizeof(double));

      if (ind == NULL || val == NULL) {
        fprintf(stderr, "Out of memory\n");
        exit(1);
      }

      /* Add subtour elimination constraint */

      nz = 0;
      for (i = 0; i < len; i++)
        for (j = i+1; j < len; j++)
          ind[nz++] = tour[i]*n+tour[j];
      for (i = 0; i < nz; i++)
        val[i] = 1.0;

      error = GRBcblazy(cbdata, nz, ind, val, GRB_LESS_EQUAL, len-1);

      free(ind);
      free(val);
    }

    free(sol);
    free(tour);
  }

  return error;
}

/* Euclidean distance between points 'i' and 'j'. */

static double
distance(double *x,
         double *y,
         int     i,
         int     j)
{
  double dx = x[i] - x[j];
  double dy = y[i] - y[j];

  return sqrt(dx*dx + dy*dy);
}

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       i, j, len, n, solcount;
  int       error = 0;
  char      name[100];
  double   *x = NULL;
  double   *y = NULL;
  int      *ind = NULL;
  double   *val = NULL;
  struct callback_data mydata;

  if (argc < 2) {
    fprintf(stderr, "Usage: tsp_c size\n");
    exit(1);
  }

  n = atoi(argv[1]);
  if (n == 0) {
    fprintf(stderr, "Argument must be a positive integer.\n");
  } else if (n > 100) {
    printf("It will be a challenge to solve a TSP this large.\n");
  }

  x   = (double *) malloc(n*sizeof(double));
  y   = (double *) malloc(n*sizeof(double));
  ind = (int *)    malloc(n*sizeof(int));
  val = (double *) malloc(n*sizeof(double));

  if (x == NULL || y == NULL || ind == NULL || val == NULL) {
    fprintf(stderr, "Out of memory\n");
    exit(1);
  }

  /* Create random points */

  for (i = 0; i < n; i++) {
    x[i] = ((double) rand())/RAND_MAX;
    y[i] = ((double) rand())/RAND_MAX;
  }

  /* Create environment */

  error = GRBloadenv(&env, "tsp.log");
  if (error) goto QUIT;

  /* Create an empty model */

  error = GRBnewmodel(env, &model, "tsp", 0, NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;


  /* Add variables - one for every pair of nodes */
  /* Note: If edge from i to j is chosen, then x[i*n+j] = x[j*n+i] = 1. */
  /* The cost is split between the two variables. */

  for (i = 0; i < n; i++) {
    for (j = 0; j < n; j++) {
      sprintf(name, "x_%d_%d", i, j);
      error = GRBaddvar(model, 0, NULL, NULL, distance(x, y, i, j)/2,
                        0.0, 1.0, GRB_BINARY, name);
      if (error) goto QUIT;
    }
  }

  /* Integrate new variables */

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* Degree-2 constraints */

  for (i = 0; i < n; i++) {
    for (j = 0; j < n; j++) {
      ind[j] = i*n+j;
      val[j] = 1.0;
    }

    sprintf(name, "deg2_%d", i);

    error = GRBaddconstr(model, n, ind, val, GRB_EQUAL, 2, name);
    if (error) goto QUIT;
  }

  /* Forbid edge from node back to itself */

  for (i = 0; i < n; i++) {
    error = GRBsetdblattrelement(model, GRB_DBL_ATTR_UB, i*n+i, 0);
    if (error) goto QUIT;
  }

  /* Symmetric TSP */

  for (i = 0; i < n; i++) {
    for (j = 0; j < i; j++) {
      ind[0] = i*n+j;
      ind[1] = i+j*n;
      val[0] = 1;
      val[1] = -1;
      error = GRBaddconstr(model, 2, ind, val, GRB_EQUAL, 0, NULL);
      if (error) goto QUIT;
    }
  }

  /* Set callback function */

  mydata.n = n;

  error = GRBsetcallbackfunc(model, subtourelim, (void *) &mydata);
  if (error) goto QUIT;

  /* Must set LazyConstraints parameter when using lazy constraints */

  error = GRBsetintparam(GRBgetenv(model), GRB_INT_PAR_LAZYCONSTRAINTS, 1);
  if (error) goto QUIT;

  /* Optimize model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Extract solution */

  error = GRBgetintattr(model, GRB_INT_ATTR_SOLCOUNT, &solcount);
  if (error) goto QUIT;

  if (solcount > 0) {
    int *tour = NULL;
    double *sol = NULL;

    sol = (double *) malloc(n*n*sizeof(double));
    tour = (int *) malloc(n*sizeof(int));
    if (sol == NULL || tour == NULL) {
      fprintf(stderr, "Out of memory\n");
      exit(1);
    }

    error = GRBgetdblattrarray(model, GRB_DBL_ATTR_X, 0, n*n, sol);
    if (error) goto QUIT;

    /* Print tour */

    findsubtour(n, sol, &len, tour);

    printf("Tour: ");
    for (i = 0; i < len; i++)
      printf("%d ", tour[i]);
    printf("\n");

    free(tour);
    free(sol);
  }

QUIT:

  /* Free data */

  free(x);
  free(y);
  free(ind);
  free(val);

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
