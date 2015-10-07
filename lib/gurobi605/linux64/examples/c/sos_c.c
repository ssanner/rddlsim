/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example creates a very simple Special Ordered Set (SOS) model.
   The model consists of 3 continuous variables, no linear constraints,
   and a pair of SOS constraints of type 1. */

#include <stdlib.h>
#include <stdio.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  double    x[3];
  double    obj[3];
  double    ub[3];
  int       sostype[2];
  int       sosbeg[2];
  int       sosind[4];
  double    soswt[4];
  int       optimstatus;
  double    objval;

  /* Create environment */

  error = GRBloadenv(&env, "sos.log");
  if (error) goto QUIT;

  /* Create an empty model */

  error = GRBnewmodel(env, &model, "sos", 0, NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;


  /* Add variables */

  obj[0] = -2; obj[1] = -1; obj[2] = -1;
  ub[0] = 1.0; ub[1] = 1.0; ub[2] = 2.0;
  error = GRBaddvars(model, 3, 0, NULL, NULL, NULL, obj, NULL, ub, NULL,
                     NULL);
  if (error) goto QUIT;

  /* Integrate new variables */

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* Build first SOS1: x0=0 or x1=0 */

  sosind[0] = 0; sosind[1] = 1;
  soswt[0] = 1.0; soswt[1] = 2.0;
  sosbeg[0] = 0; sostype[0] = GRB_SOS_TYPE1;

  /* Build second SOS1: x0=0 or x2=0 */

  sosind[2] = 0; sosind[3] = 2;
  soswt[2] = 1.0; soswt[3] = 2.0;
  sosbeg[1] = 2; sostype[1] = GRB_SOS_TYPE1;

  /* Add SOSs to model */

  error = GRBaddsos(model, 2, 4, sostype, sosbeg, sosind, soswt);
  if (error) goto QUIT;

  /* Optimize model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Write model to 'sos.lp' */

  error = GRBwrite(model, "sos.lp");
  if (error) goto QUIT;

  /* Capture solution information */

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, &objval);
  if (error) goto QUIT;

  error = GRBgetdblattrarray(model, GRB_DBL_ATTR_X, 0, 3, x);
  if (error) goto QUIT;

  printf("\nOptimization complete\n");
  if (optimstatus == GRB_OPTIMAL) {
    printf("Optimal objective: %.4e\n", objval);

    printf("  x=%.4f, y=%.4f, z=%.4f\n", x[0], x[1], x[2]);
  } else if (optimstatus == GRB_INF_OR_UNBD) {
    printf("Model is infeasible or unbounded\n");
  } else {
    printf("Optimization was stopped early\n");
  }

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
