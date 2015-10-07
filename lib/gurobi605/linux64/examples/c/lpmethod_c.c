/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve a model with different values of the Method parameter;
   show which value gives the shortest solve time. */

#include <stdlib.h>
#include <stdio.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env = NULL, *menv;
  GRBmodel *m = NULL;
  int       error = 0;
  int       i;
  int       optimstatus;
  int       bestMethod = -1;
  double    bestTime;

  if (argc < 2)
  {
    fprintf(stderr, "Usage: lpmethod_c filename\n");
    exit(1);
  }

  error = GRBloadenv(&env, "lpmethod.log");
  if (error) goto QUIT;

  /* Read model */
  error = GRBreadmodel(env, argv[1], &m);
  if (error) goto QUIT;
  menv = GRBgetenv(m);
  error = GRBgetdblparam(menv, "TimeLimit", &bestTime);
  if (error) goto QUIT;

  /* Solve the model with different values of Method */
  for (i = 0; i <= 2; ++i)
  {
    error = GRBresetmodel(m);
    if (error) goto QUIT;
    error = GRBsetintparam(menv, "Method", i);
    if (error) goto QUIT;
    error = GRBoptimize(m);
    if (error) goto QUIT;
    error = GRBgetintattr(m, "Status", &optimstatus);
    if (error) goto QUIT;
    if (optimstatus == GRB_OPTIMAL) {
      error = GRBgetdblattr(m, "Runtime", &bestTime);
      if (error) goto QUIT;
      bestMethod = i;
      /* Reduce the TimeLimit parameter to save time
         with other methods */
      error = GRBsetdblparam(menv, "TimeLimit", bestTime);
      if (error) goto QUIT;
    }
  }

  /* Report which method was fastest */
  if (bestMethod == -1) {
    printf("Unable to solve this model\n");
  } else {
    printf("Solved in %f seconds with Method: %i\n",
           bestTime, bestMethod);
  }

QUIT:

  /* Error reporting */

  if (error)
  {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free model */

  GRBfreemodel(m);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
