/* Copyright 2014, Gurobi Optimization, Inc. */

/* A simple sensitivity analysis example which reads a MIP model
   from a file and solves it. Then each binary variable is set
   to 1-X, where X is its value in the optimal solution, and
   the impact on the objective function value is reported.
*/

#include <stdlib.h>
#include <stdio.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env = NULL, *modelenv = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  int       ismip, status, numvars, i, j;
  double    origobjval, lb, ub, objval;
  double   *origx = NULL;
  char      vtype, *vname;

  if (argc < 2)
  {
    fprintf(stderr, "Usage: sensitivity_c filename\n");
    exit(1);
  }

  /* Create environment */

  error = GRBloadenv(&env, "sensitivity.log");
  if (error) goto QUIT;

  /* Read and solve model */

  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;

  error = GRBgetintattr(model, "IsMIP", &ismip);
  if (error) goto QUIT;
  if (ismip == 0) {
    printf("Model is not a MIP\n");
    exit(1);
  }

  error = GRBoptimize(model);
  if (error) goto QUIT;

  error = GRBgetintattr(model, "Status", &status);
  if (error) goto QUIT;
  if (status != GRB_OPTIMAL) {
    printf("Optimization ended with status %d\n", status);
    exit(1);
  }

  /* Store the optimal solution */

  error = GRBgetdblattr(model, "ObjVal", &origobjval);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "NumVars", &numvars);
  if (error) goto QUIT;
  origx = (double *) malloc(numvars * sizeof(double));
  if (origx == NULL) {
    printf("Out of memory\n");
    exit(1);
  }
  error = GRBgetdblattrarray(model, "X", 0, numvars, origx);
  if (error) goto QUIT;

  /* Disable solver output for subsequent solves */

  modelenv = GRBgetenv(model);
  if (!modelenv) {
    printf("Cannot retrieve model environment\n");
    exit(1);
  }
  error = GRBsetintparam(modelenv, "OutputFlag", 0);
  if (error) goto QUIT;

  /* Iterate through unfixed, binary variables in model */

  for (i = 0; i < numvars; i++) {
    error = GRBgetdblattrelement(model, "LB", i, &lb);
    if (error) goto QUIT;
    error = GRBgetdblattrelement(model, "UB", i, &ub);
    if (error) goto QUIT;
    error = GRBgetcharattrelement(model, "VType", i, &vtype);
    if (error) goto QUIT;

    if (lb == 0 && ub == 1
        && (vtype == GRB_BINARY || vtype == GRB_INTEGER)) {

      /* Set variable to 1-X, where X is its value in optimal solution */

      if (origx[i] < 0.5) {
        error = GRBsetdblattrelement(model, "LB", i, 1.0);
        if (error) goto QUIT;
        error = GRBsetdblattrelement(model, "Start", i, 1.0);
        if (error) goto QUIT;
      } else {
        error = GRBsetdblattrelement(model, "UB", i, 0.0);
        if (error) goto QUIT;
        error = GRBsetdblattrelement(model, "Start", i, 0.0);
        if (error) goto QUIT;
      }

      /* Update MIP start for the other variables */

      for (j = 0; j < numvars; j++) {
        if (j != i) {
          error = GRBsetdblattrelement(model, "Start", j, origx[j]);
          if (error) goto QUIT;
        }
      }

      /* Solve for new value and capture sensitivity information */

      error = GRBoptimize(model);
      if (error) goto QUIT;

      error = GRBgetintattr(model, "Status", &status);
      if (error) goto QUIT;
      error = GRBgetstrattrelement(model, "VarName", i, &vname);
      if (error) goto QUIT;
      if (status == GRB_OPTIMAL) {
        error = GRBgetdblattr(model, "ObjVal", &objval);
        if (error) goto QUIT;
        printf("Objective sensitivity for variable %s is %g\n",
            vname, objval - origobjval);
      } else {
        printf("Objective sensitivity for variable %s is infinite\n",
            vname);
      }

      /* Restore the original variable bounds */

      error = GRBsetdblattrelement(model, "LB", i, 0.0);
      if (error) goto QUIT;
      error = GRBsetdblattrelement(model, "UB", i, 1.0);
      if (error) goto QUIT;
    }
  }


QUIT:

  /* Error reporting */

  if (error)
  {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free data */

  free(origx);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
