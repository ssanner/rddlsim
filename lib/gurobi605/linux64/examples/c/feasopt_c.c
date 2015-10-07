/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, adds artificial
   variables to each constraint, and then minimizes the sum of the
   artificial variables.  A solution with objective zero corresponds
   to a feasible solution to the input model.
   We can also use FeasRelax feature to do it. In this example, we
   use minrelax=1, i.e. optimizing the returned model finds a solution
   that minimizes the original objective, but only from among those
   solutions that minimize the sum of the artificial variables. */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include "gurobi_c.h"

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env       = NULL;
  GRBmodel *model     = NULL;
  GRBmodel *feasmodel = NULL;
  double   *rhspen      = NULL;
  int       error = 0;
  int       i, j;
  int       numvars, numconstrs;
  char      sense;
  int       vind[1];
  double    vval[1];
  double    feasobj;
  char      *cname, *vname;

  if (argc < 2)
  {
    fprintf(stderr, "Usage: feasopt_c filename\n");
    exit(1);
  }

  error = GRBloadenv(&env, "feasopt.log");
  if (error) goto QUIT;

  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;

  /* Create a copy to use FeasRelax feature later */

  feasmodel = GRBcopymodel(model);
  if (error) goto QUIT;

  /* clear objective */
  error = GRBgetintattr(model, "NumVars", &numvars);
  if (error) goto QUIT;
  for (j = 0; j < numvars; ++j)
  {
    error = GRBsetdblattrelement(model, "Obj", j, 0.0);
    if (error) goto QUIT;
  }

  /* add slack variables */
  error = GRBgetintattr(model, "NumConstrs", &numconstrs);
  if (error) goto QUIT;
  for (i = 0; i < numconstrs; ++i)
  {
    error = GRBgetcharattrelement(model, "Sense", i, &sense);
    if (error) goto QUIT;
    if (sense != '>')
    {
      error = GRBgetstrattrelement(model, "ConstrName", i, &cname);
      if (error) goto QUIT;
      vname = malloc(sizeof(char) * (6 + strlen(cname)));
      if (!vname) goto QUIT;
      strcpy(vname, "ArtN_");
      strcat(vname, cname);
      vind[0] = i;
      vval[0] = -1.0;
      error = GRBaddvar(model, 1, vind, vval, 1.0, 0.0, GRB_INFINITY,
                        GRB_CONTINUOUS, vname);
      if (error) goto QUIT;
      free(vname);
    }
    if (sense != '<')
    {
      error = GRBgetstrattrelement(model, "ConstrName", i, &cname);
      if (error) goto QUIT;
      vname = malloc(sizeof(char) * (6 + strlen(cname)));
      if (!vname) goto QUIT;
      strcpy(vname, "ArtP_");
      strcat(vname, cname);
      vind[0] = i;
      vval[0] = 1.0;
      error = GRBaddvar(model, 1, vind, vval, 1.0, 0.0, GRB_INFINITY,
                        GRB_CONTINUOUS, vname);
      if (error) goto QUIT;
      free(vname);
    }
  }
  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  /* optimize modified model */
  error = GRBwrite(model, "feasopt.lp");
  if (error) goto QUIT;

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Use FeasRelax feature */

  rhspen = (double *) malloc(numconstrs*sizeof(double));
  if (rhspen == NULL) {
    printf("ERROR: out of memory\n");
    goto QUIT;
  }

  /* set penalties for artificial variables */
  for (i = 0; i < numconstrs; i++) rhspen[i] = 1;

  /* create a FeasRelax model with the original objective recovered
     and enforcement on minimum of aretificial variables */
  error = GRBfeasrelax(feasmodel, GRB_FEASRELAX_LINEAR, 1,
                       NULL, NULL, rhspen, &feasobj);
  if (error) goto QUIT;

  /* optimize FeasRelax model */
  error = GRBwrite(feasmodel, "feasopt1.lp");
  if (error) goto QUIT;

  error = GRBoptimize(feasmodel);
  if (error) goto QUIT;


QUIT:

  /* Error reporting */

  if (error)
  {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free models, env and etc. */

  if (rhspen) free(rhspen);

  GRBfreemodel(model);
  GRBfreemodel(feasmodel);

  GRBfreeenv(env);

  return 0;
}
