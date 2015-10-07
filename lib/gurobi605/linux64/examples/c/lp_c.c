/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads an LP model from a file and solves it.
   If the model is infeasible or unbounded, the example turns off
   presolve and solves the model again. If the model is infeasible,
   the example computes an Irreducible Inconsistent Subsystem (IIS),
   and writes it to a file */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *masterenv = NULL;
  GRBmodel *model     = NULL;
  GRBenv   *modelenv  = NULL;
  int       error     = 0;
  int       optimstatus;
  double    objval;

  if (argc < 2) {
    fprintf(stderr, "Usage: lp_c filename\n");
    exit(1);
  }

  /* Create environment */

  error = GRBloadenv(&masterenv, "lp.log");
  if (error) goto QUIT;

  /* Read model from file */

  error = GRBreadmodel(masterenv, argv[1], &model);
  if (error) goto QUIT;

  /* Solve model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Capture solution information */

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  /* If model is infeasible or unbounded, turn off presolve and resolve */

  if (optimstatus == GRB_INF_OR_UNBD) {
    modelenv = GRBgetenv(model);
    if (!modelenv) {
      fprintf(stderr, "Error: could not get model environment\n");
      goto QUIT;
    }

    /* Change parameter on model environment.  The model now has
       a copy of the master environment, so changing the master will
       no longer affect the model.  */

    error = GRBsetintparam(modelenv, "PRESOLVE", 0);
    if (error) goto QUIT;

    error = GRBoptimize(model);
    if (error) goto QUIT;

    error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
    if (error) goto QUIT;
  }

  if (optimstatus == GRB_OPTIMAL) {
    error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, &objval);
    if (error) goto QUIT;
    printf("Optimal objective: %.4e\n\n", objval);
  } else if (optimstatus == GRB_INFEASIBLE) {
    printf("Model is infeasible\n\n");

    error = GRBcomputeIIS(model);
    if (error) goto QUIT;

    error = GRBwrite(model, "model.ilp");
    if (error) goto QUIT;
  } else if (optimstatus == GRB_UNBOUNDED) {
    printf("Model is unbounded\n\n");
  } else {
    printf("Optimization was stopped with status = %d\n\n", optimstatus);
  }

QUIT:

  /* Error reporting */

  if (error) {
    printf("ERROR: %s\n", GRBgeterrormsg(masterenv));
    exit(1);
  }

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(masterenv);

  return 0;
}
