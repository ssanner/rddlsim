/* Copyright 2014, Gurobi Optimization, Inc. */

/*
   This example reads a model from a file, sets up a callback that
   monitors optimization progress and implements a custom
   termination strategy, and outputs progress information to the
   screen and to a log file.

   The termination strategy implemented in this callback stops the
   optimization of a MIP model once at least one of the following two
   conditions have been satisfied:
     1) The optimality gap is less than 10%
     2) At least 10000 nodes have been explored, and an integer feasible
        solution has been found.
   Note that termination is normally handled through Gurobi parameters
   (MIPGap, NodeLimit, etc.).  You should only use a callback for
   termination if the available parameters don't capture your desired
   termination criterion.
*/

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

/* Define structure to pass my data to the callback function */

struct callback_data {
  double  lastiter;
  double  lastnode;
  double *solution;
  FILE   *logfile;
};

/* Define my callback function */

int __stdcall
mycallback(GRBmodel *model,
           void     *cbdata,
           int       where,
           void     *usrdata)
{
  struct callback_data *mydata = (struct callback_data *) usrdata;

  if (where == GRB_CB_POLLING) {
    /* Ignore polling callback */
  } else if (where == GRB_CB_PRESOLVE) {
    /* Presolve callback */
    int cdels, rdels;
    GRBcbget(cbdata, where, GRB_CB_PRE_COLDEL, &cdels);
    GRBcbget(cbdata, where, GRB_CB_PRE_ROWDEL, &rdels);
    if (cdels || rdels) {
      printf("%7d columns and %7d rows are removed\n", cdels, rdels);
    }
  } else if (where == GRB_CB_SIMPLEX) {
    /* Simplex callback */
    double itcnt, obj, pinf, dinf;
    int    ispert;
    char   ch;
    GRBcbget(cbdata, where, GRB_CB_SPX_ITRCNT, &itcnt);
    if (itcnt - mydata->lastiter >= 100) {
      mydata->lastiter = itcnt;
      GRBcbget(cbdata, where, GRB_CB_SPX_OBJVAL, &obj);
      GRBcbget(cbdata, where, GRB_CB_SPX_ISPERT, &ispert);
      GRBcbget(cbdata, where, GRB_CB_SPX_PRIMINF, &pinf);
      GRBcbget(cbdata, where, GRB_CB_SPX_DUALINF, &dinf);
      if      (ispert == 0) ch = ' ';
      else if (ispert == 1) ch = 'S';
      else                  ch = 'P';
      printf("%7.0f %14.7e%c %13.6e %13.6e\n", itcnt, obj, ch, pinf, dinf);
    }
  } else if (where == GRB_CB_MIP) {
    /* General MIP callback */
    double nodecnt, objbst, objbnd, actnodes, itcnt;
    int    solcnt, cutcnt;
    GRBcbget(cbdata, where, GRB_CB_MIP_NODCNT, &nodecnt);
    GRBcbget(cbdata, where, GRB_CB_MIP_OBJBST, &objbst);
    GRBcbget(cbdata, where, GRB_CB_MIP_OBJBND, &objbnd);
    GRBcbget(cbdata, where, GRB_CB_MIP_SOLCNT, &solcnt);
    if (nodecnt - mydata->lastnode >= 100) {
      mydata->lastnode = nodecnt;
      GRBcbget(cbdata, where, GRB_CB_MIP_NODLFT, &actnodes);
      GRBcbget(cbdata, where, GRB_CB_MIP_ITRCNT, &itcnt);
      GRBcbget(cbdata, where, GRB_CB_MIP_CUTCNT, &cutcnt);
      printf("%7.0f %7.0f %8.0f %13.6e %13.6e %7d %7d\n",
             nodecnt, actnodes, itcnt, objbst, objbnd, solcnt, cutcnt);
    }
    if (fabs(objbst - objbnd) < 0.1 * (1.0 + fabs(objbst))) {
      printf("Stop early - 10%% gap achieved\n");
      GRBterminate(model);
    }
    if (nodecnt >= 10000 && solcnt) {
      printf("Stop early - 10000 nodes explored\n");
      GRBterminate(model);
    }
  } else if (where == GRB_CB_MIPSOL) {
    /* MIP solution callback */
    double nodecnt, obj;
    int    solcnt;
    GRBcbget(cbdata, where, GRB_CB_MIPSOL_NODCNT, &nodecnt);
    GRBcbget(cbdata, where, GRB_CB_MIPSOL_OBJ, &obj);
    GRBcbget(cbdata, where, GRB_CB_MIPSOL_SOLCNT, &solcnt);
    GRBcbget(cbdata, where, GRB_CB_MIPSOL_SOL, mydata->solution);
    printf("**** New solution at node %.0f, obj %g, sol %d, x[0] = %.2f ****\n",
           nodecnt, obj, solcnt, mydata->solution[0]);
  } else if (where == GRB_CB_MIPNODE) {
    int status;
    /* MIP node callback */
    printf("**** New node ****\n");
    GRBcbget(cbdata, where, GRB_CB_MIPNODE_STATUS, &status);
    if (status == GRB_OPTIMAL) {
      GRBcbget(cbdata, where, GRB_CB_MIPNODE_REL, mydata->solution);
      GRBcbsolution(cbdata, mydata->solution);
    }
  } else if (where == GRB_CB_BARRIER) {
    /* Barrier callback */
    int    itcnt;
    double primobj, dualobj, priminf, dualinf, compl;
    GRBcbget(cbdata, where, GRB_CB_BARRIER_ITRCNT, &itcnt);
    GRBcbget(cbdata, where, GRB_CB_BARRIER_PRIMOBJ, &primobj);
    GRBcbget(cbdata, where, GRB_CB_BARRIER_DUALOBJ, &dualobj);
    GRBcbget(cbdata, where, GRB_CB_BARRIER_PRIMINF, &priminf);
    GRBcbget(cbdata, where, GRB_CB_BARRIER_DUALINF, &dualinf);
    GRBcbget(cbdata, where, GRB_CB_BARRIER_COMPL, &compl);
    printf("%d %.4e %.4e %.4e %.4e %.4e\n",
           itcnt, primobj, dualobj, priminf, dualinf, compl);
  } else if (where == GRB_CB_MESSAGE) {
    /* Message callback */
    char *msg;
    GRBcbget(cbdata, where, GRB_CB_MSG_STRING, &msg);
    fprintf(mydata->logfile, "%s", msg);
  }
  return 0;
}

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  int       numvars, solcount, optimstatus, j;
  double    objval, x;
  char     *varname;
  struct callback_data mydata;

  mydata.lastiter = -GRB_INFINITY;
  mydata.lastnode = -GRB_INFINITY;
  mydata.solution = NULL;
  mydata.logfile  = NULL;

  if (argc < 2) {
    fprintf(stderr, "Usage: callback_c filename\n");
    goto QUIT;
  }

  /* Open log file */
  mydata.logfile = fopen("cb.log", "w");
  if (!mydata.logfile) {
    fprintf(stderr, "Cannot open cb.log for callback message\n");
    goto QUIT;
  }

  /* Create environment */

  error = GRBloadenv(&env, NULL);
  if (error) goto QUIT;

  /* Turn off display and heuristics */

  error = GRBsetintparam(env, GRB_INT_PAR_OUTPUTFLAG, 0);
  if (error) goto QUIT;

  error = GRBsetdblparam(env, GRB_DBL_PAR_HEURISTICS, 0.0);
  if (error) goto QUIT;

  /* Read model from file */

  error = GRBreadmodel(env, argv[1], &model);
  if (error) goto QUIT;

  /* Allocate space for solution */

  error = GRBgetintattr(model, GRB_INT_ATTR_NUMVARS, &numvars);
  if (error) goto QUIT;

  mydata.solution = malloc(numvars*sizeof(double));
  if (mydata.solution == NULL) {
    fprintf(stderr, "Failed to allocate memory\n");
    exit(1);
  }

  /* Set callback function */

  error = GRBsetcallbackfunc(model, mycallback, (void *) &mydata);
  if (error) goto QUIT;

  /* Solve model */

  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Capture solution information */

  printf("\nOptimization complete\n");

  error = GRBgetintattr(model, GRB_INT_ATTR_SOLCOUNT, &solcount);
  if (error) goto QUIT;

  error = GRBgetintattr(model, GRB_INT_ATTR_STATUS, &optimstatus);
  if (error) goto QUIT;

  if (solcount == 0) {
    printf("No solution found, optimization status = %d\n", optimstatus);
    goto QUIT;
  }

  error = GRBgetdblattr(model, GRB_DBL_ATTR_OBJVAL, &objval);
  if (error) goto QUIT;

  printf("Solution found, objective = %.4e\n", objval);

  for ( j = 0; j < numvars; ++j ) {
    error = GRBgetstrattrelement(model, GRB_STR_ATTR_VARNAME, j, &varname);
    if (error) goto QUIT;
    error = GRBgetdblattrelement(model, GRB_DBL_ATTR_X, j, &x);
    if (error) goto QUIT;
    if (x != 0.0) {
      printf("%s %f\n", varname, x);
    }
  }

QUIT:

  /* Error reporting */

  if (error) {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Close log file */

  if (mydata.logfile)
    fclose(mydata.logfile);

  /* Free solution */

  if (mydata.solution)
    free(mydata.solution);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
