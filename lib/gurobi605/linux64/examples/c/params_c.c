/* Copyright 2014, Gurobi Optimization, Inc. */

/* Use parameters that are associated with a model.

   A MIP is solved for 5 seconds with different sets of parameters.
   The one with the smallest MIP gap is selected, and the optimization
   is resumed until the optimal solution is found.
*/

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL, *modelenv = NULL, *bestenv = NULL;
  GRBmodel *model = NULL, *bestmodel = NULL;
  int       error = 0;
  int       ismip, i, mipfocus;
  double    bestgap, gap;

  if (argc < 2)
  {
    fprintf(stderr, "Usage: params_c filename\n");
    exit(1);
  }

  error = GRBloadenv(&env, "params.log");
  if (error) goto QUIT;

  /* Read model and verify that it is a MIP */
  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "IsMIP", &ismip);
  if (error) goto QUIT;
  if (ismip == 0)
  {
    printf("The model is not an integer program\n");
    exit(1);
  }

  /* Set a 5 second time limit */
  modelenv = GRBgetenv(model);
  if (!modelenv) {
    printf("Cannot retrieve model environment\n");
    exit(1);
  }
  error = GRBsetdblparam(modelenv, "TimeLimit", 5);
  if (error) goto QUIT;

  /* Now solve the model with different values of MIPFocus */
  bestmodel = GRBcopymodel(model);
  if (!bestmodel) {
    printf("Cannot copy model\n");
    exit(1);
  }
  error = GRBoptimize(bestmodel);
  if (error) goto QUIT;
  error = GRBgetdblattr(bestmodel, "MIPGap", &bestgap);
  if (error) goto QUIT;
  for (i = 1; i <= 3; ++i)
  {
    error = GRBresetmodel(model);
    if (error) goto QUIT;
    modelenv = GRBgetenv(model);
    if (!modelenv) {
      printf("Cannot retrieve model environment\n");
      exit(1);
    }
    error = GRBsetintparam(modelenv, "MIPFocus", i);
    if (error) goto QUIT;
    error = GRBoptimize(model);
    if (error) goto QUIT;
    error = GRBgetdblattr(model, "MIPGap", &gap);
    if (error) goto QUIT;
    if (bestgap > gap)
    {
      GRBmodel *tmp = bestmodel;
      bestmodel = model;
      model = tmp;
      bestgap = gap;
    }
  }

  /* Finally, free the extra model, reset the time limit and
     continue to solve the best model to optimality */
  GRBfreemodel(model);
  bestenv = GRBgetenv(bestmodel);
  if (!bestenv) {
    printf("Cannot retrieve best model environment\n");
    exit(1);
  }
  error = GRBsetdblparam(bestenv, "TimeLimit", GRB_INFINITY);
  if (error) goto QUIT;
  error = GRBoptimize(bestmodel);
  if (error) goto QUIT;
  error = GRBgetintparam(bestenv, "MIPFocus", &mipfocus);
  if (error) goto QUIT;

  printf("Solved with MIPFocus: %i\n", mipfocus);

QUIT:

  /* Error reporting */

  if (error)
  {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free best model */

  GRBfreemodel(bestmodel);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
