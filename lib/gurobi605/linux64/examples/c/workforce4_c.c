/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. We use Pareto optimization to solve the model:
   first, we minimize the linear sum of the slacks. Then, we constrain
   the sum of the slacks, and we minimize a quadratic objective that
   tries to balance the workload among the workers. */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include "gurobi_c.h"

int solveAndPrint(GRBmodel* model,
                  int nShifts, int nWorkers, char** Workers,
                  int* status);


#define xcol(w,s)         nShifts*w+s
#define slackcol(s)       nShifts*nWorkers+s
#define totSlackcol       nShifts*(nWorkers+1)
#define totShiftscol(w)   nShifts*(nWorkers+1)+1+w
#define avgShiftscol      (nShifts+1)*(nWorkers+1)
#define diffShiftscol(w)  (nShifts+1)*(nWorkers+1)+1+w
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
  char      vname[MAXSTR], cname[MAXSTR];
  double    val;

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
  error = GRBloadenv(&env, "workforce4.log");
  if (error) goto QUIT;

  /* Create initial model */
  error = GRBnewmodel(env, &model, "workforce4",
                      (nShifts + 1) * (nWorkers + 1),
                      NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;

  /* Initialize assignment decision variables:
     x[w][s] == 1 if worker w is assigned to shift s.
     This is no longer a pure assignment model, so we must
     use binary variables. */
  for (w = 0; w < nWorkers; ++w)
  {
    for (s = 0; s < nShifts; ++s)
    {
      col = xcol(w, s);
      sprintf(vname, "%s.%s", Workers[w], Shifts[s]);
      error = GRBsetcharattrelement(model, "VType", col, GRB_BINARY);
      if (error) goto QUIT;
      error = GRBsetdblattrelement(model, "UB", col, availability[w][s]);
      if (error) goto QUIT;
      error = GRBsetstrattrelement(model, "VarName", col, vname);
      if (error) goto QUIT;
    }
  }

  /* Initialize slack decision variables */
  for (s = 0; s < nShifts; ++s)
  {
    sprintf(vname, "%sSlack", Shifts[s]);
    error = GRBsetstrattrelement(model, "VarName", slackcol(s), vname);
    if (error) goto QUIT;
  }

  /* Initialize total slack decision variable */
  error = GRBsetstrattrelement(model, "VarName", totSlackcol, "totSlack");
  if (error) goto QUIT;

  /* Initialize variables to count the total shifts worked by each worker */
  for (w = 0; w < nWorkers; ++w)
  {
    sprintf(vname, "%sTotShifts", Workers[w]);
    error = GRBsetstrattrelement(model, "VarName", totShiftscol(w), vname);
    if (error) goto QUIT;
  }

  /* The objective is to minimize the sum of the slacks */
  error = GRBsetintattr(model, "ModelSense", 1);
  if (error) goto QUIT;
  error = GRBsetdblattrelement(model, "Obj", totSlackcol, 1.0);
  if (error) goto QUIT;

  /* Make space for constraint data */
  cbeg = malloc(sizeof(int) * nShifts);
  if (!cbeg) goto QUIT;
  cind = malloc(sizeof(int) * nShifts * (nWorkers + 1));
  if (!cind) goto QUIT;
  cval = malloc(sizeof(double) * nShifts * (nWorkers + 1));
  if (!cval) goto QUIT;
  sense = malloc(sizeof(char) * nShifts);
  if (!sense) goto QUIT;

  /* Constraint: assign exactly shiftRequirements[s] workers
     to each shift s, plus the slack */
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
    cind[idx] = slackcol(s);
    cval[idx++] = 1.0;
  }
  error = GRBaddconstrs(model, nShifts, idx, cbeg, cind, cval, sense,
                        shiftRequirements, Shifts);
  if (error) goto QUIT;

  /* Constraint: set totSlack column equal to the total slack */
  idx = 0;
  for (s = 0; s < nShifts; ++s)
  {
    cind[idx] = slackcol(s);
    cval[idx++] = 1.0;
  }
  cind[idx] = totSlackcol;
  cval[idx++] = -1.0;
  error = GRBaddconstr(model, idx, cind, cval, GRB_EQUAL,
                       0.0, "totSlack");
  if (error) goto QUIT;

  /* Constraint: compute the total number of shifts for each worker */
  for (w = 0; w < nWorkers; ++w)
  {
    idx = 0;
    for (s = 0; s < nShifts; ++s)
    {
      cind[idx] = xcol(w,s);
      cval[idx++] = 1.0;
    }
    sprintf(cname, "totShifts%s", Workers[w]);
    cind[idx] = totShiftscol(w);
    cval[idx++] = -1.0;
    error = GRBaddconstr(model, idx, cind, cval, GRB_EQUAL, 0.0, cname);
    if (error) goto QUIT;
  }

  /* Optimize */
  error = solveAndPrint(model, nShifts, nWorkers, Workers, &status);
  if (error) goto QUIT;
  if (status != GRB_OPTIMAL) goto QUIT;

  /* Constrain the slack by setting its upper and lower bounds */
  error = GRBgetdblattrelement(model, "X", totSlackcol, &val);
  if (error) goto QUIT;
  error = GRBsetdblattrelement(model, "UB", totSlackcol, val);
  if (error) goto QUIT;
  error = GRBsetdblattrelement(model, "LB", totSlackcol, val);
  if (error) goto QUIT;

  /* Variable to count the average number of shifts worked */
  error = GRBaddvar(model, 0, NULL, NULL, 0, 0, GRB_INFINITY, GRB_CONTINUOUS,
                    "avgShifts");
  if (error) goto QUIT;

  /* Variables to count the difference from average for each worker;
     note that these variables can take negative values. */
  error = GRBaddvars(model, nWorkers, 0, NULL, NULL, NULL, NULL, NULL, NULL,
                     NULL, NULL);
  if (error) goto QUIT;

  error = GRBupdatemodel(model);
  if (error) goto QUIT;

  for (w = 0; w < nWorkers; ++w)
  {
    sprintf(vname, "%sDiff", Workers[w]);
    error = GRBsetstrattrelement(model, "VarName", diffShiftscol(w), vname);
    if (error) goto QUIT;
    error = GRBsetdblattrelement(model, "LB", diffShiftscol(w), -GRB_INFINITY);
    if (error) goto QUIT;
  }

  /* Constraint: compute the average number of shifts worked */
  idx = 0;
  for (w = 0; w < nWorkers; ++w) {
    cind[idx] = totShiftscol(w);
    cval[idx++] = 1.0;
  }
  cind[idx] = avgShiftscol;
  cval[idx++] = -nWorkers;
  error = GRBaddconstr(model, idx, cind, cval, GRB_EQUAL, 0.0, "avgShifts");
  if (error) goto QUIT;

  /* Constraint: compute the difference from the average number of shifts */
  for (w = 0; w < nWorkers; ++w) {
    cind[0] = totShiftscol(w);
    cval[0] = 1.0;
    cind[1] = avgShiftscol;
    cval[1] = -1.0;
    cind[2] = diffShiftscol(w);
    cval[2] = -1.0;
    sprintf(cname, "%sDiff", Workers[w]);
    error = GRBaddconstr(model, 3, cind, cval, GRB_EQUAL, 0.0, cname);
    if (error) goto QUIT;
  }

  /* Objective: minimize the sum of the square of the difference from the
     average number of shifts worked */
  error = GRBsetdblattrelement(model, "Obj", totSlackcol, 0.0);
  if (error) goto QUIT;

  for (w = 0; w < nWorkers; ++w)
  {
    cind[w] = diffShiftscol(w);
    cval[w] = 1.0;
  }
  error = GRBaddqpterms(model, nWorkers, cind, cind, cval);
  if (error) goto QUIT;

  /* Optimize */
  error = solveAndPrint(model, nShifts, nWorkers, Workers, &status);
  if (error) goto QUIT;
  if (status != GRB_OPTIMAL) goto QUIT;


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

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}


int solveAndPrint(GRBmodel* model,
                  int nShifts, int nWorkers, char** Workers,
                  int* status)
{
  int error, w;
  double val;

  error = GRBoptimize(model);
  if (error) return error;

  error = GRBgetintattr(model, "Status", status);
  if (error) return error;

  if ((*status == GRB_INF_OR_UNBD) || (*status == GRB_INFEASIBLE) ||
      (*status == GRB_UNBOUNDED))
  {
    printf("The model cannot be solved "
           "because it is infeasible or unbounded\n");
    return 0;
  }
  if (*status != GRB_OPTIMAL)
  {
    printf("Optimization was stopped with status %i\n", *status);
    return 0;
  }

  /* Print total slack and the number of shifts worked for each worker */
  error = GRBgetdblattrelement(model, "X", totSlackcol, &val);
  if (error) return error;

  printf("\nTotal slack required: %f\n", val);
  for (w = 0; w < nWorkers; ++w)
  {
    error = GRBgetdblattrelement(model, "X", totShiftscol(w), &val);
    if (error) return error;
    printf("%s worked %f shifts\n", Workers[w], val);
  }
  printf("\n");
  return 0;
}
