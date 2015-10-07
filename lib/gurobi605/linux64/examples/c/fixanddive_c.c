/* Copyright 2014, Gurobi Optimization, Inc. */

/* Implement a simple MIP heuristic.  Relax the model,
   sort variables based on fractionality, and fix the 25% of
   the fractional variables that are closest to integer variables.
   Repeat until either the relaxation is integer feasible or
   linearly infeasible. */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

typedef struct
{
  int index;
  double X;
}
var_t ;

int vcomp(const void* v1, const void* v2);


int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL, *modelenv = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  int       j, iter, nfix;
  int       numvars, numintvars, numfractional;
  int      *intvars = NULL;
  int       status;
  char      vtype, *vname;
  double    sol, obj, fixval;
  var_t    *fractional = NULL;

  if (argc < 2)
  {
    fprintf(stderr, "Usage: fixanddive_c filename\n");
    exit(1);
  }

  error = GRBloadenv(&env, "fixanddive.log");
  if (error) goto QUIT;

  /* Read model */
  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;

  /* Collect integer variables and relax them */
  error = GRBgetintattr(model, "NumVars", &numvars);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "NumIntVars", &numintvars);
  if (error) goto QUIT;
  intvars = malloc(sizeof(int) * numintvars);
  if (!intvars) goto QUIT;
  fractional = malloc(sizeof(var_t) * numintvars);
  if (!fractional) goto QUIT;
  numfractional = 0;
  for (j = 0; j < numvars; j++)
  {
    error = GRBgetcharattrelement(model, "VType", j, &vtype);
    if (error) goto QUIT;
    if (vtype != GRB_CONTINUOUS)
    {
      intvars[numfractional++] = j;
      error = GRBsetcharattrelement(model, "VType", j, GRB_CONTINUOUS);
      if (error) goto QUIT;
    }
  }

  modelenv = GRBgetenv(model);
  if (!modelenv) goto QUIT;
  error = GRBsetintparam(modelenv, "OutputFlag", 0);
  if (error) goto QUIT;
  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Perform multiple iterations. In each iteration, identify the first
     quartile of integer variables that are closest to an integer value
     in the relaxation, fix them to the nearest integer, and repeat. */

  for (iter = 0; iter < 1000; ++iter)
  {

    /* create a list of fractional variables, sorted in order of
       increasing distance from the relaxation solution to the nearest
       integer value */

    numfractional = 0;
    for (j = 0; j < numintvars; ++j)
    {
      error = GRBgetdblattrelement(model, "X", intvars[j], &sol);
      if (error) goto QUIT;
      if (fabs(sol - floor(sol + 0.5)) > 1e-5)
      {
        fractional[numfractional].index = intvars[j];
        fractional[numfractional++].X = sol;
      }
    }

    error = GRBgetdblattr(model, "ObjVal", &obj);
    if (error) goto QUIT;
    printf("Iteration %i, obj %f, fractional %i\n",
           iter, obj, numfractional);

    if (numfractional == 0)
    {
      printf("Found feasible solution - objective %f\n", obj);
      break;
    }

    /* Fix the first quartile to the nearest integer value */
    qsort(fractional, numfractional, sizeof(var_t), vcomp);
    nfix = numfractional / 4;
    nfix = (nfix > 1) ? nfix : 1;
    for (j = 0; j < nfix; ++j)
    {
      fixval = floor(fractional[j].X + 0.5);
      error = GRBsetdblattrelement(model, "LB", fractional[j].index, fixval);
      if (error) goto QUIT;
      error = GRBsetdblattrelement(model, "UB", fractional[j].index, fixval);
      if (error) goto QUIT;
      error = GRBgetstrattrelement(model, "VarName",
                                   fractional[j].index, &vname);
      if (error) goto QUIT;
      printf("  Fix %s to %f ( rel %f )\n", vname, fixval, fractional[j].X);
    }

    error = GRBoptimize(model);
    if (error) goto QUIT;

    /* Check optimization result */

    error = GRBgetintattr(model, "Status", &status);
    if (error) goto QUIT;
    if (status != GRB_OPTIMAL)
    {
      printf("Relaxation is infeasible\n");
      break;
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

  free(intvars);
  free(fractional);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}


int vcomp(const void* v1, const void* v2)
{
  double sol1, sol2, frac1, frac2;
  sol1 = fabs(((var_t *)v1)->X);
  sol2 = fabs(((var_t *)v2)->X);
  frac1 = fabs(sol1 - floor(sol1 + 0.5));
  frac2 = fabs(sol2 - floor(sol2 + 0.5));
  return (frac1 < frac2) ? -1 : ((frac1 == frac2) ? 0 : 1);
}
