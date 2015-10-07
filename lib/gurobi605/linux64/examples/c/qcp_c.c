/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QCP model:

     maximize    x
     subject to  x + y + z = 1
                 x^2 + y^2 <= z^2 (second-order cone)
                 x^2 <= yz        (rotated second-order cone)
*/

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
  double    sol[3];
  int       ind[3];
  double    val[3];
  double    obj[] = {1, 0, 0};
  int       qrow[3];
  int       qcol[3];
  double    qval[3];
  int       optimstatus;
  double    objval;

  /* Create environment */

  error = GRBloadenv(&env, "qcp.log");
  if (error) goto QUIT;

  /* Create an empty model */

  error = GRBnewmodel(env, &model, "qcp", 0, NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;


  /* Add variables */

  error = GRBaddvars(model, 3, 0, NULL, NULL, NULL, obj, NULL, NULL, NULL,
                     NULL);
  if (error) goto QUIT;

  /* Change sense to maximization */

  error = GRBsetintattr(model, GRB_INT_ATTR_MODELSENSE, GRB_MAXIMIZE);
  if (error) goto QUIT;

  /* Integrate new variables */

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* Linear constraint: x + y + z = 1 */

  ind[0] = 0; ind[1] = 1; ind[2] = 2;
  val[0] = 1; val[1] = 1; val[2] = 1;

  error = GRBaddconstr(model, 3, ind, val, GRB_EQUAL, 1.0, "c0");
  if (error) goto QUIT;

  /* Cone: x^2 + y^2 <= z^2 */

  qrow[0] = 0; qcol[0] = 0; qval[0] = 1.0;
  qrow[1] = 1; qcol[1] = 1; qval[1] = 1.0;
  qrow[2] = 2; qcol[2] = 2; qval[2] = -1.0;

  error = GRBaddqconstr(model, 0, NULL, NULL, 3, qrow, qcol, qval,
                        GRB_LESS_EQUAL, 0.0, "qc0");
  if (error) goto QUIT;

  /* Rotated cone: x^2 <= yz */

  qrow[0] = 0; qcol[0] = 0; qval[0] = 1.0;
  qrow[1] = 1; qcol[1] = 2; qval[1] = -1.0;

  error = GRBaddqconstr(model, 0, NULL, NULL, 2, qrow, qcol, qval,
                        GRB_LESS_EQUAL, 0.0, "qc1");
  if (error) goto QUIT;

  /* Optimize model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Write model to 'qcp.lp' */

  error = GRBwrite(model, "qcp.lp");
  if (error) goto QUIT;

  /* Capture solution information */

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, &objval);
  if (error) goto QUIT;

  error = GRBgetdblattrarray(model, GRB_DBL_ATTR_X, 0, 3, sol);
  if (error) goto QUIT;

  printf("\nOptimization complete\n");
  if (optimstatus == GRB_OPTIMAL) {
    printf("Optimal objective: %.4e\n", objval);

    printf("  x=%.2f, y=%.2f, z=%.2f\n", sol[0], sol[1], sol[2]);
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
