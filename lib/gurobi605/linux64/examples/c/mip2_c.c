/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, solves it and
   prints the objective values from all feasible solutions
   generated while solving the MIP. Then it creates the fixed
   model and solves that model. */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  GRBmodel *fixed = NULL;
  int       error = 0;
  int       ismip;
  int       j, k, solcount, numvars;
  double    objn, vobj, xn;
  int       optimstatus, foptimstatus;
  double    objval, fobjval;
  char      *varname;
  double    x;

  /* To change settings for a loaded model, we need to get
     the model environment, which will be freed when the model
     is freed. */

  GRBenv   *menv, *fenv;

  if (argc < 2) {
    fprintf(stderr, "Usage: mip2_c filename\n");
    exit(1);
  }

  /* Create environment */

  error = GRBloadenv(&env, "mip2.log");
  if (error) goto QUIT;

  /* Read model from file */

  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;

  error = GRBgetintattr(model, "IsMIP", &ismip);
  if (error) goto QUIT;

  if (ismip == 0) {
    printf("Model is not a MIP\n");
    goto QUIT;
  }

  /* Get model environment */

  menv = GRBgetenv(model);
  if (!menv) {
    fprintf(stderr, "Error: could not get model environment\n");
    goto QUIT;
  }

  /* Solve model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Capture solution information */

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  printf("\nOptimization complete\n");
  if (optimstatus == GRB_OPTIMAL) {
    error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, &objval);
    if (error) goto QUIT;
    printf("Optimal objective: %.4e\n\n", objval);
  } else if (optimstatus == GRB_INF_OR_UNBD) {
    printf("Model is infeasible or unbounded\n\n");
    goto QUIT;
  } else if (optimstatus == GRB_INFEASIBLE) {
    printf("Model is infeasible\n\n");
    goto QUIT;
  } else if (optimstatus == GRB_UNBOUNDED) {
    printf("Model is unbounded\n\n");
    goto QUIT;
  } else {
    printf("Optimization was stopped with status = %d\n\n", optimstatus);
    goto QUIT;
  }

  /* Iterate over the solutions and compute the objectives */

  error = GRBsetintparam(menv, "OutputFlag", 0);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "SolCount", &solcount);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "NumVars", &numvars);
  if (error) goto QUIT;

  printf("\n");
  for ( k = 0; k < solcount; ++k ) {
    error = GRBsetintparam(menv, "SolutionNumber", k);
    objn = 0.0;
    for ( j = 0; j < numvars; ++j ) {
      error = GRBgetdblattrelement(model, "Obj", j, &vobj);
      if (error) goto QUIT;
      error = GRBgetdblattrelement(model, "Xn", j, &xn);
      if (error) goto QUIT;
      objn += vobj * xn;
    }
    printf("Solution %i has objective: %f\n", k, objn);
  }
  printf("\n");

  error = GRBsetintparam(menv, "OutputFlag", 1);
  if (error) goto QUIT;

  /* Create a fixed model, turn off presolve and solve */

  fixed = GRBfixedmodel(model);
  if (!fixed) {
    fprintf(stderr, "Error: could not create fixed model\n");
    goto QUIT;
  }

  fenv = GRBgetenv(fixed);
  if (!fenv) {
    fprintf(stderr, "Error: could not get fixed model environment\n");
    goto QUIT;
  }

  error = GRBsetintparam(fenv, "PRESOLVE", 0);
  if (error) goto QUIT;

  error = GRBoptimize(fixed);
  if (error) goto QUIT;

  error = GRBgetintattr(fixed, GRB_INT_ATTR_STATUS, &foptimstatus);
  if (error) goto QUIT;

  if (foptimstatus != GRB_OPTIMAL) {
    fprintf(stderr, "Error: fixed model isn't optimal\n");
    goto QUIT;
  }

  error = GRBgetdblattr(fixed, GRB_DBL_ATTR_OBJVAL, &fobjval);
  if (error) goto QUIT;

  if (fabs(fobjval - objval) > 1.0e-6 * (1.0 + fabs(objval))) {
    fprintf(stderr, "Error: objective values are different\n");
  }

  /* Print values of nonzero variables */
  for ( j = 0; j < numvars; ++j ) {
    error = GRBgetstrattrelement(fixed, "VarName", j, &varname);
    if (error) goto QUIT;
    error = GRBgetdblattrelement(fixed, "X", j, &x);
    if (error) goto QUIT;
    if (x != 0.0) {
      printf("%s %f\n", varname, x);
    }
  }


QUIT:

  /* Error reporting */

  if (error) {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free models */

  GRBfreemodel(model);
  GRBfreemodel(fixed);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
