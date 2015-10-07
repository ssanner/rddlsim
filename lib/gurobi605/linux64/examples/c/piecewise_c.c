/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example considers the following separable, convex problem:

     minimize    f(x) - y + g(z)
     subject to  x + 2 y + 3 z <= 4
                 x +   y       >= 1
                 x,    y,    z <= 1

   where f(u) = exp(-u) and g(u) = 2 u^2 - 4 u, for all real u. It
   formulates and solves a simpler LP model by approximating f and
   g with piecewise-linear functions. Then it transforms the model
   into a MIP by negating the approximation for f, which corresponds
   to a non-convex piecewise-linear function, and solves it again.
*/

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

double f(double u) { return exp(-u); }
double g(double u) { return 2 * u * u - 4 * u; }

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  double    lb, ub;
  int       npts, i;
  double   *ptu = NULL;
  double   *ptf = NULL;
  double   *ptg = NULL;
  int       ind[3];
  double    val[3];
  int       ismip;
  double    objval;
  double    sol[3];

  /* Create environment */

  error = GRBloadenv(&env, NULL);
  if (error) goto QUIT;

  /* Create a new model */

  error = GRBnewmodel(env, &model, NULL, 0, NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;

  /* Add variables */

  lb = 0.0; ub = 1.0;

  error = GRBaddvar(model, 0, NULL, NULL, 0.0, lb, ub, GRB_CONTINUOUS, "x");
  if (error) goto QUIT;
  error = GRBaddvar(model, 0, NULL, NULL, 0.0, lb, ub, GRB_CONTINUOUS, "y");
  if (error) goto QUIT;
  error = GRBaddvar(model, 0, NULL, NULL, 0.0, lb, ub, GRB_CONTINUOUS, "z");
  if (error) goto QUIT;

  /* Integrate new variables */

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* Set objective for y */

  error = GRBsetdblattrelement(model, GRB_DBL_ATTR_OBJ, 1, -1.0);
  if (error) goto QUIT;

  /* Add piecewise-linear objective functions for x and z */

  npts = 101;
  ptu = (double *) malloc(npts * sizeof(double));
  ptf = (double *) malloc(npts * sizeof(double));
  ptg = (double *) malloc(npts * sizeof(double));

  for (i = 0; i < npts; i++) {
    ptu[i] = lb + (ub - lb) * i / (npts - 1);
    ptf[i] = f(ptu[i]);
    ptg[i] = g(ptu[i]);
  }

  error = GRBsetpwlobj(model, 0, npts, ptu, ptf);
  if (error) goto QUIT;
  error = GRBsetpwlobj(model, 2, npts, ptu, ptg);
  if (error) goto QUIT;

  /* Add constraint: x + 2 y + 3 z <= 4 */

  ind[0] = 0; ind[1] = 1; ind[2] = 2;
  val[0] = 1; val[1] = 2; val[2] = 3;

  error = GRBaddconstr(model, 3, ind, val, GRB_LESS_EQUAL, 4.0, "c0");
  if (error) goto QUIT;

  /* Add constraint: x + y >= 1 */

  ind[0] = 0; ind[1] = 1;
  val[0] = 1; val[1] = 1;

  error = GRBaddconstr(model, 2, ind, val, GRB_GREATER_EQUAL, 1.0, "c1");
  if (error) goto QUIT;

  /* Optimize model as an LP */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  error = GRBgetintattr(model, "IsMIP", &ismip);
  if (error) goto QUIT;
  error = GRBgetdblattr(model, "ObjVal", &objval);
  if (error) goto QUIT;
  error = GRBgetdblattrarray(model, "X", 0, 3, sol);
  if (error) goto QUIT;

  printf("IsMIP: %d\n", ismip);
  printf("x %g\ny %g\nz %g\n", sol[0], sol[1], sol[2]);
  printf("Obj: %g\n", objval);
  printf("\n");

  /* Negate piecewise-linear objective function for x */

  for (i = 0; i < npts; i++) {
    ptf[i] = -ptf[i];
  }

  error = GRBsetpwlobj(model, 0, npts, ptu, ptf);
  if (error) goto QUIT;

  /* Optimize model as a MIP */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  error = GRBgetintattr(model, "IsMIP", &ismip);
  if (error) goto QUIT;
  error = GRBgetdblattr(model, "ObjVal", &objval);
  if (error) goto QUIT;
  error = GRBgetdblattrarray(model, "X", 0, 3, sol);
  if (error) goto QUIT;

  printf("IsMIP: %d\n", ismip);
  printf("x %g\ny %g\nz %g\n", sol[0], sol[1], sol[2]);
  printf("Obj: %g\n", objval);

QUIT:

  /* Error reporting */

  if (error) {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free data */

  free(ptu);
  free(ptf);
  free(ptg);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
