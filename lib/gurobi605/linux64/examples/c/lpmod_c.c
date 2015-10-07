/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads an LP model from a file and solves it.
   If the model can be solved, then it finds the smallest positive variable,
   sets its upper bound to zero, and resolves the model two ways:
   first with an advanced start, then without an advanced start
   (i.e. 'from scratch'). */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  int       j;
  int       numvars, isMIP, status, minVar = 0;
  double    minVal = GRB_INFINITY, sol, lb;
  char     *varname;
  double    warmCount, warmTime, coldCount, coldTime;

  if (argc < 2)
  {
    fprintf(stderr, "Usage: lpmod_c filename\n");
    exit(1);
  }

  error = GRBloadenv(&env, "lpmod.log");
  if (error) goto QUIT;

  /* Read model and determine whether it is an LP */
  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "IsMIP", &isMIP);
  if (error) goto QUIT;
  if (isMIP)
  {
    printf("The model is not a linear program\n");
    goto QUIT;
  }

  error = GRBoptimize(model);
  if (error) goto QUIT;

  error = GRBgetintattr(model, "Status", &status);
  if (error) goto QUIT;

  if ((status == GRB_INF_OR_UNBD) || (status == GRB_INFEASIBLE) ||
      (status == GRB_UNBOUNDED))
  {
    printf("The model cannot be solved because it is ");
    printf("infeasible or unbounded\n");
    goto QUIT;
  }

  if (status != GRB_OPTIMAL)
  {
    printf("Optimization was stopped with status %i\n", status);
    goto QUIT;
  }

  /* Find the smallest variable value */
  error = GRBgetintattr(model, "NumVars", &numvars);
  if (error) goto QUIT;
  for (j = 0; j < numvars; ++j)
  {
    error = GRBgetdblattrelement(model, "X", j, &sol);
    if (error) goto QUIT;
    error = GRBgetdblattrelement(model, "LB", j, &lb);
    if (error) goto QUIT;
    if ((sol > 0.0001) && (sol < minVal) &&
        (lb == 0.0))
    {
      minVal = sol;
      minVar = j;
    }
  }

  error = GRBgetstrattrelement(model, "VarName", minVar, &varname);
  if (error) goto QUIT;
  printf("\n*** Setting %s from %f to zero ***\n\n", varname, minVal);
  error = GRBsetdblattrelement(model, "LB", minVar, 0.0);
  if (error) goto QUIT;

  /* Solve from this starting point */
  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Save iteration & time info */
  error = GRBgetdblattr(model, "IterCount", &warmCount);
  if (error) goto QUIT;
  error = GRBgetdblattr(model, "Runtime", &warmTime);
  if (error) goto QUIT;

  /* Reset the model and resolve */
  printf("\n*** Resetting and solving ");
  printf("without an advanced start ***\n\n");
  error = GRBresetmodel(model);
  if (error) goto QUIT;
  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Save iteration & time info */
  error = GRBgetdblattr(model, "IterCount", &coldCount);
  if (error) goto QUIT;
  error = GRBgetdblattr(model, "Runtime", &coldTime);
  if (error) goto QUIT;

  printf("\n*** Warm start: %f iterations, %f seconds\n",
         warmCount, warmTime);
  printf("*** Cold start: %f iterations, %f seconds\n",
         coldCount, coldTime);


QUIT:

  /* Error reporting */

  if (error)
  {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
