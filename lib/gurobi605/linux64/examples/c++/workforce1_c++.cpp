/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. If the problem cannot be solved, use IIS to find a set of
   conflicting constraints. Note that there may be additional conflicts
   besides what is reported via IIS. */

#include "gurobi_c++.h"
#include <sstream>
using namespace std;

int
main(int argc,
     char *argv[])
{
  GRBEnv* env = 0;
  GRBConstr* c = 0;
  GRBVar** x = 0;
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

    // Amount each worker is paid to work one shift
    double pay[] = { 10, 12, 10, 8, 8, 9, 11 };

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
    // to shift s. Since an assignment model always produces integer
    // solutions, we use continuous variables and solve as an LP.
    x = new GRBVar*[nWorkers];
    for (int w = 0; w < nWorkers; ++w)
    {
      x[w] = model.addVars(nShifts);
      xCt++;
      model.update();
      for (int s = 0; s < nShifts; ++s)
      {
        ostringstream vname;
        vname << Workers[w] << "." << Shifts[s];
        x[w][s].set(GRB_DoubleAttr_UB, availability[w][s]);
        x[w][s].set(GRB_DoubleAttr_Obj, pay[w]);
        x[w][s].set(GRB_StringAttr_VarName, vname.str());
      }
    }

    // The objective is to minimize the total pay costs
    model.set(GRB_IntAttr_ModelSense, 1);

    // Update model to integrate new variables
    model.update();

    // Constraint: assign exactly shiftRequirements[s] workers
    // to each shift s
    for (int s = 0; s < nShifts; ++s)
    {
      GRBLinExpr lhs = 0;
      for (int w = 0; w < nWorkers; ++w)
      {
        lhs += x[w][s];
      }
      model.addConstr(lhs == shiftRequirements[s], Shifts[s]);
    }

    // Optimize
    model.optimize();
    int status = model.get(GRB_IntAttr_Status);
    if (status == GRB_UNBOUNDED)
    {
      cout << "The model cannot be solved "
      << "because it is unbounded" << endl;
      return 1;
    }
    if (status == GRB_OPTIMAL)
    {
      cout << "The optimal objective is " <<
      model.get(GRB_DoubleAttr_ObjVal) << endl;
      return 0;
    }
    if ((status != GRB_INF_OR_UNBD) && (status != GRB_INFEASIBLE))
    {
      cout << "Optimization was stopped with status " << status << endl;
      return 1;
    }

    // do IIS
    cout << "The model is infeasible; computing IIS" << endl;
    model.computeIIS();
    cout << "\nThe following constraint(s) "
    << "cannot be satisfied:" << endl;
    c = model.getConstrs();
    for (int i = 0; i < model.get(GRB_IntAttr_NumConstrs); ++i)
    {
      if (c[i].get(GRB_IntAttr_IISConstr) == 1)
      {
        cout << c[i].get(GRB_StringAttr_ConstrName) << endl;
      }
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
  for (int i = 0; i < xCt; ++i) {
    delete[] x[i];
  }
  delete[] x;
  delete env;
  return 0;
}
