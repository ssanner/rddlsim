/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. We use Pareto optimization to solve the model:
   first, we minimize the linear sum of the slacks. Then, we constrain
   the sum of the slacks, and we minimize a quadratic objective that
   tries to balance the workload among the workers. */

#include "gurobi_c++.h"
#include <sstream>
using namespace std;

int solveAndPrint(GRBModel& model, GRBVar& totSlack,
                  int nWorkers, string* Workers,
                  GRBVar* totShifts) throw(GRBException);

int
main(int argc,
     char *argv[])
{
  GRBEnv* env = 0;
  GRBConstr* c = 0;
  GRBVar* v = 0;
  GRBVar** x = 0;
  GRBVar* slacks = 0;
  GRBVar* totShifts = 0;
  GRBVar* diffShifts = 0;
  int xCt = 0;
  try
  {

    // Sample data
    const int nShifts = 14;
    const int nWorkers = 7;

    // Sets of days and workers
    string Shifts[] =
      { "Mon1", "Tue2", "Wed3", "Thu4", "Fri5", "Sat6",
        "Sun7", "Mon8", "Tue9", "Wed10", "Thu11", "Fri12", "Sat13",
        "Sun14" };
    string Workers[] =
      { "Amy", "Bob", "Cathy", "Dan", "Ed", "Fred", "Gu" };

    // Number of workers required for each shift
    double shiftRequirements[] =
      { 3, 2, 4, 4, 5, 6, 5, 2, 2, 3, 4, 6, 7, 5 };

    // Worker availability: 0 if the worker is unavailable for a shift
    double availability[][nShifts] =
      { { 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1 },
        { 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0 },
        { 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
        { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
        { 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1 },
        { 1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1 },
        { 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } };

    // Model
    env = new GRBEnv();
    GRBModel model = GRBModel(*env);
    model.set(GRB_StringAttr_ModelName, "assignment");

    // Assignment variables: x[w][s] == 1 if worker w is assigned
    // to shift s. This is no longer a pure assignment model, so we must
    // use binary variables.
    x = new GRBVar*[nWorkers];
    int s, w;
    for (w = 0; w < nWorkers; ++w)
    {
      x[w] = model.addVars(nShifts);
      xCt++;
      model.update();
      for (s = 0; s < nShifts; ++s)
      {
        ostringstream vname;
        vname << Workers[w] << "." << Shifts[s];
        x[w][s].set(GRB_DoubleAttr_UB, availability[w][s]);
        x[w][s].set(GRB_CharAttr_VType, GRB_BINARY);
        x[w][s].set(GRB_StringAttr_VarName, vname.str());
      }
    }

    // Slack variables for each shift constraint so that the shifts can
    // be satisfied
    slacks = model.addVars(nShifts);
    model.update();
    for (s = 0; s < nShifts; ++s)
    {
      ostringstream vname;
      vname << Shifts[s] << "Slack";
      slacks[s].set(GRB_StringAttr_VarName, vname.str());
    }

    // Variable to represent the total slack
    GRBVar totSlack = model.addVar(0, GRB_INFINITY, 0, GRB_CONTINUOUS,
                                   "totSlack");

    // Variables to count the total shifts worked by each worker
    totShifts = model.addVars(nWorkers);
    model.update();
    for (w = 0; w < nWorkers; ++w)
    {
      ostringstream vname;
      vname << Workers[w] << "TotShifts";
      totShifts[w].set(GRB_StringAttr_VarName, vname.str());
    }

    // Update model to integrate new variables
    model.update();

    GRBLinExpr lhs;

    // Constraint: assign exactly shiftRequirements[s] workers
    // to each shift s
    for (s = 0; s < nShifts; ++s)
    {
      lhs = 0;
      lhs += slacks[s];
      for (w = 0; w < nWorkers; ++w)
      {
        lhs += x[w][s];
      }
      model.addConstr(lhs == shiftRequirements[s], Shifts[s]);
    }

    // Constraint: set totSlack equal to the total slack
    lhs = 0;
    for (s = 0; s < nShifts; ++s)
    {
      lhs += slacks[s];
    }
    model.addConstr(lhs == totSlack, "totSlack");

    // Constraint: compute the total number of shifts for each worker
    for (w = 0; w < nWorkers; ++w) {
      lhs = 0;
      for (s = 0; s < nShifts; ++s) {
        lhs += x[w][s];
      }
      ostringstream vname;
      vname << "totShifts" << Workers[w];
      model.addConstr(lhs == totShifts[w], vname.str());
    }

    // Objective: minimize the total slack
    GRBLinExpr obj = 0;
    obj += totSlack;
    model.setObjective(obj);

    // Optimize
    int status = solveAndPrint(model, totSlack, nWorkers, Workers, totShifts);
    if (status != GRB_OPTIMAL)
    {
      return 1;
    }

    // Constrain the slack by setting its upper and lower bounds
    totSlack.set(GRB_DoubleAttr_UB, totSlack.get(GRB_DoubleAttr_X));
    totSlack.set(GRB_DoubleAttr_LB, totSlack.get(GRB_DoubleAttr_X));

    // Variable to count the average number of shifts worked
    GRBVar avgShifts =
      model.addVar(0, GRB_INFINITY, 0, GRB_CONTINUOUS, "avgShifts");

    // Variables to count the difference from average for each worker;
    // note that these variables can take negative values.
    diffShifts = model.addVars(nWorkers);
    model.update();
    for (w = 0; w < nWorkers; ++w) {
      ostringstream vname;
      vname << Workers[w] << "Diff";
      diffShifts[w].set(GRB_StringAttr_VarName, vname.str());
      diffShifts[w].set(GRB_DoubleAttr_LB, -GRB_INFINITY);
    }

    // Update model to integrate new variables
    model.update();

    // Constraint: compute the average number of shifts worked
    lhs = 0;
    for (w = 0; w < nWorkers; ++w) {
      lhs += totShifts[w];
    }
    model.addConstr(lhs == nWorkers * avgShifts, "avgShifts");

    // Constraint: compute the difference from the average number of shifts
    for (w = 0; w < nWorkers; ++w) {
      lhs = 0;
      lhs += totShifts[w];
      lhs -= avgShifts;
      ostringstream vname;
      vname << Workers[w] << "Diff";
      model.addConstr(lhs == diffShifts[w], vname.str());
    }

    // Objective: minimize the sum of the square of the difference from the
    // average number of shifts worked
    GRBQuadExpr qobj;
    for (w = 0; w < nWorkers; ++w) {
      qobj += diffShifts[w] * diffShifts[w];
    }
    model.setObjective(qobj);

    // Optimize
    status = solveAndPrint(model, totSlack, nWorkers, Workers, totShifts);
    if (status != GRB_OPTIMAL)
    {
      return 1;
    }

  }
  catch (GRBException e)
  {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  }
  catch (...)
  {
    cout << "Exception during optimization" << endl;
  }

  delete[] c;
  delete[] v;
  for (int i = 0; i < xCt; ++i) {
    delete[] x[i];
  }
  delete[] x;
  delete[] slacks;
  delete[] totShifts;
  delete[] diffShifts;
  delete env;
  return 0;
}

int solveAndPrint(GRBModel& model, GRBVar& totSlack,
                  int nWorkers, string* Workers,
                  GRBVar* totShifts) throw(GRBException)
{
  model.optimize();
  int status = model.get(GRB_IntAttr_Status);

  if ((status == GRB_INF_OR_UNBD) || (status == GRB_INFEASIBLE) ||
      (status == GRB_UNBOUNDED))
  {
    cout << "The model cannot be solved " <<
    "because it is infeasible or unbounded" << endl;
    return status;
  }
  if (status != GRB_OPTIMAL)
  {
    cout << "Optimization was stopped with status " << status << endl;
    return status;
  }

  // Print total slack and the number of shifts worked for each worker
  cout << endl << "Total slack required: " <<
    totSlack.get(GRB_DoubleAttr_X) << endl;
  for (int w = 0; w < nWorkers; ++w) {
    cout << Workers[w] << " worked " <<
    totShifts[w].get(GRB_DoubleAttr_X) << " shifts" << endl;
  }
  cout << endl;
  return status;
}
