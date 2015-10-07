/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a model from a file and tunes it.
   It then writes the best parameter settings to a file
   and solves the model using these parameters. */

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
  int       tuneresultcount;
  int       error = 0;

  if (argc < 2) {
    fprintf(stderr, "Usage: tune_c filename\n");
    exit(1);
  }

  /* Create environment */

  error = GRBloadenv(&env, "tune_c.log");
  if (error) goto QUIT;

  /* Read model from file */

  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;

  /* Set the TuneResults parameter to 1 */

  error = GRBsetintparam(GRBgetenv(model), GRB_INT_PAR_TUNERESULTS, 1);
  if (error) goto QUIT;

  /* Tune the model */

  error = GRBtunemodel(model);
  if (error) goto QUIT;

  /* Get the number of tuning results */

  error = GRBgetintattr(model, GRB_INT_ATTR_TUNE_RESULTCOUNT, &tuneresultcount);
  if (error) goto QUIT;

  if (tuneresultcount > 0) {

    /* Load the best tuned parameters into the model's environment */

    error = GRBgettuneresult(model, 0);
    if (error) goto QUIT;

    /* Write tuned parameters to a file */

    error = GRBwrite(model, "tune.prm");
    if (error) goto QUIT;

    /* Solve the model using the tuned parameters */

    error = GRBoptimize(model);
    if (error) goto QUIT;
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
