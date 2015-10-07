/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. If the problem cannot be solved, relax the model
   to determine which constraints cannot be satisfied, and how much
   they need to be relaxed. */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include "gurobi_c.h"


#define xcol(w,s)  nShifts*w+s
#define MAXSTR     128


int
main(int   argc,
     char *argv[])
{
  GRBenv   *env = NULL;
  GRBmodel *model = NULL;
  int       error = 0, status;
  int       s, w, col;
  int      *cbeg = NULL;
  int      *cind = NULL;
  int       idx;
  double   *cval = NULL;
  char     *sense = NULL;
  char      vname[MAXSTR];
  double    obj;
  int       i, j, orignumvars, numvars, numconstrs;
  double   *rhspen = NULL;
  double    sol;
  char     *sname;

  /* Sample data */
  const int nShifts = 14;
  const int nWorkers = 7;

  /* Sets of days and workers */
  char* Shifts[] =
    { "Mon1", "Tue2", "Wed3", "Thu4", "Fri5", "Sat6",
      "Sun7", "Mon8", "Tue9", "Wed10", "Thu11", "Fri12", "Sat13",
      "Sun14" };
  char* Workers[] =
    { "Amy", "Bob", "Cathy", "Dan", "Ed", "Fred", "Gu" };

  /* Number of workers required for each shift */
  double shiftRequirements[] =
    { 3, 2, 4, 4, 5, 6, 5, 2, 2, 3, 4, 6, 7, 5 };

  /* Amount each worker is paid to work one shift */
  double pay[] = { 10, 12, 10, 8, 8, 9, 11 };

  /* Worker availability: 0 if the worker is unavailable for a shift */
  double availability[][14] =
    { { 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1 },
      { 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0 },
      { 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
      { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
      { 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1 },
      { 1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1 },
      { 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } };

  /* Create environment */
  error = GRBloadenv(&env, "workforce3.log");
  if (error) goto QUIT;

  /* Create initial model */
  error = GRBnewmodel(env, &model, "workforce3", nWorkers * nShifts,
                      NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;

  /* Initialize assignment decision variables:
     x[w][s] == 1 if worker w is assigned
     to shift s. Since an assignment model always produces integer
     solutions, we use continuous variables and solve as an LP. */
  for (w = 0; w < nWorkers; ++w)
  {
    for (s = 0; s < nShifts; ++s)
    {
      col = xcol(w, s);
      sprintf(vname, "%s.%s", Workers[w], Shifts[s]);
      error = GRBsetdblattrelement(model, "UB", col, availability[w][s]);
      if (error) goto QUIT;
      error = GRBsetdblattrelement(model, "Obj", col, pay[w]);
      if (error) goto QUIT;
      error = GRBsetstrattrelement(model, "VarName", col, vname);
      if (error) goto QUIT;
    }
  }

  /* The objective is to minimize the total pay costs */
  error = GRBsetintattr(model, "ModelSense", 1);
  if (error) goto QUIT;

  /* Make space for constraint data */
  cbeg = malloc(sizeof(int) * nShifts);
  if (!cbeg) goto QUIT;
  cind = malloc(sizeof(int) * nShifts * nWorkers);
  if (!cind) goto QUIT;
  cval = malloc(sizeof(double) * nShifts * nWorkers);
  if (!cval) goto QUIT;
  sense = malloc(sizeof(char) * nShifts);
  if (!sense) goto QUIT;

  /* Constraint: assign exactly shiftRequirements[s] workers
     to each shift s */
  idx = 0;
  for (s = 0; s < nShifts; ++s)
  {
    cbeg[s] = idx;
    sense[s] = GRB_EQUAL;
    for (w = 0; w < nWorkers; ++w)
    {
      cind[idx] = xcol(w, s);
      cval[idx++] = 1.0;
    }
  }
  error = GRBaddconstrs(model, nShifts, idx, cbeg, cind, cval, sense,
                        shiftRequirements, Shifts);
  if (error) goto QUIT;

  /* Optimize */
  error = GRBoptimize(model);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "Status", &status);
  if (error) goto QUIT;
  if (status == GRB_UNBOUNDED)
  {
    printf("The model cannot be solved because it is unbounded\n");
    goto QUIT;
  }
  if (status == GRB_OPTIMAL)
  {
    error = GRBgetdblattr(model, "ObjVal", &obj);
    if (error) goto QUIT;
    printf("The optimal objective is %f\n", obj);
    goto QUIT;
  }
  if ((status != GRB_INF_OR_UNBD) && (status != GRB_INFEASIBLE))
  {
    printf("Optimization was stopped with status %i\n", status);
    goto QUIT;
  }

  /* Relax the constraints to make the model feasible */
  printf("The model is infeasible; relaxing the constraints\n");

  /* Determine the matrix size before relaxing the constraints */
  error = GRBgetintattr(model, "NumVars", &orignumvars);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "NumConstrs", &numconstrs);
  if (error) goto QUIT;

  /* Use FeasRelax feature with penalties for constraint violations */
  rhspen = malloc(sizeof(double) * numconstrs);
  if (!rhspen) goto QUIT;
  for (i = 0; i < numconstrs; i++) rhspen[i] = 1;
  error = GRBfeasrelax(model, GRB_FEASRELAX_LINEAR, 0,
                       NULL, NULL, rhspen, NULL);
  if (error) goto QUIT;
  error = GRBoptimize(model);
  if (error) goto QUIT;
  error = GRBgetintattr(model, "Status", &status);
  if (error) goto QUIT;
  if ((status == GRB_INF_OR_UNBD) || (status == GRB_INFEASIBLE) ||
      (status == GRB_UNBOUNDED))
  {
    printf("The relaxed model cannot be solved "
           "because it is infeasible or unbounded\n");
    goto QUIT;
  }
  if (status != GRB_OPTIMAL)
  {
    printf("Optimization was stopped with status %i\n", status);
    goto QUIT;
  }

  printf("\nSlack values:\n");
  error = GRBgetintattr(model, "NumVars", &numvars);
  if (error) goto QUIT;
  for (j = orignumvars; j < numvars; ++j)
  {
    error = GRBgetdblattrelement(model, "X", j, &sol);
    if (error) goto QUIT;
    if (sol > 1e-6)
    {
      error = GRBgetstrattrelement(model, "VarName", j, &sname);
      if (error) goto QUIT;
      printf("%s = %f\n", sname, sol);
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

  free(cbeg);
  free(cind);
  free(cval);
  free(sense);
  free(rhspen);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
