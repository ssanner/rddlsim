/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. If the problem cannot be solved, relax the model
   to determine which constraints cannot be satisfied, and how much
   they need to be relaxed. */

import gurobi.*;

public class Workforce3 {

  public static void main(String[] args) {
    try {

      // Sample data
      // Sets of days and workers
      String Shifts[] =
          new String[] { "Mon1", "Tue2", "Wed3", "Thu4", "Fri5", "Sat6",
              "Sun7", "Mon8", "Tue9", "Wed10", "Thu11", "Fri12", "Sat13",
              "Sun14" };
      String Workers[] =
          new String[] { "Amy", "Bob", "Cathy", "Dan", "Ed", "Fred", "Gu" };

      int nShifts = Shifts.length;
      int nWorkers = Workers.length;

      // Number of workers required for each shift
      double shiftRequirements[] =
          new double[] { 3, 2, 4, 4, 5, 6, 5, 2, 2, 3, 4, 6, 7, 5 };

      // Amount each worker is paid to work one shift
      double pay[] = new double[] { 10, 12, 10, 8, 8, 9, 11 };

      // Worker availability: 0 if the worker is unavailable for a shift
      double availability[][] =
          new double[][] { { 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1 },
              { 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0 },
              { 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
              { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
              { 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1 },
              { 1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1 },
              { 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } };

      // Model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.set(GRB.StringAttr.ModelName, "assignment");

      // Assignment variables: x[w][s] == 1 if worker w is assigned
      // to shift s. Since an assignment model always produces integer
      // solutions, we use continuous variables and solve as an LP.
      GRBVar[][] x = new GRBVar[nWorkers][nShifts];
      for (int w = 0; w < nWorkers; ++w) {
        for (int s = 0; s < nShifts; ++s) {
          x[w][s] =
              model.addVar(0, availability[w][s], pay[w], GRB.CONTINUOUS,
                           Workers[w] + "." + Shifts[s]);
        }
      }

      // The objective is to minimize the total pay costs
      model.set(GRB.IntAttr.ModelSense, 1);

      // Update model to integrate new variables
      model.update();

      // Constraint: assign exactly shiftRequirements[s] workers
      // to each shift s
      for (int s = 0; s < nShifts; ++s) {
        GRBLinExpr lhs = new GRBLinExpr();
        for (int w = 0; w < nWorkers; ++w) {
          lhs.addTerm(1.0, x[w][s]);
        }
        model.addConstr(lhs, GRB.EQUAL, shiftRequirements[s], Shifts[s]);
      }

      // Optimize
      model.optimize();
      int status = model.get(GRB.IntAttr.Status);
      if (status == GRB.UNBOUNDED) {
        System.out.println("The model cannot be solved "
            + "because it is unbounded");
        return;
      }
      if (status == GRB.OPTIMAL) {
        System.out.println("The optimal objective is " +
            model.get(GRB.DoubleAttr.ObjVal));
        return;
      }
      if (status != GRB.INF_OR_UNBD &&
          status != GRB.INFEASIBLE    ) {
        System.out.println("Optimization was stopped with status " + status);
        return;
      }

      // Relax the constraints to make the model feasible
      System.out.println("The model is infeasible; relaxing the constraints");
      int orignumvars = model.get(GRB.IntAttr.NumVars);
      model.feasRelax(0, false, false, true);
      model.optimize();
      status = model.get(GRB.IntAttr.Status);
      if (status == GRB.INF_OR_UNBD ||
          status == GRB.INFEASIBLE  ||
          status == GRB.UNBOUNDED     ) {
        System.out.println("The relaxed model cannot be solved "
            + "because it is infeasible or unbounded");
        return;
      }
      if (status != GRB.OPTIMAL) {
        System.out.println("Optimization was stopped with status " + status);
        return;
      }

      System.out.println("\nSlack values:");
      GRBVar[] vars = model.getVars();
      for (int i = orignumvars; i < model.get(GRB.IntAttr.NumVars); ++i) {
        GRBVar sv = vars[i];
        if (sv.get(GRB.DoubleAttr.X) > 1e-6) {
          System.out.println(sv.get(GRB.StringAttr.VarName) + " = " +
              sv.get(GRB.DoubleAttr.X));
        }
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
