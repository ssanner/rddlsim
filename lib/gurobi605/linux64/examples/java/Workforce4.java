/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. We use Pareto optimization to solve the model:
   first, we minimize the linear sum of the slacks. Then, we constrain
   the sum of the slacks, and we minimize a quadratic objective that
   tries to balance the workload among the workers. */

import gurobi.*;

public class Workforce4 {

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
      // to shift s. This is no longer a pure assignment model, so we must
      // use binary variables.
      GRBVar[][] x = new GRBVar[nWorkers][nShifts];
      for (int w = 0; w < nWorkers; ++w) {
        for (int s = 0; s < nShifts; ++s) {
          x[w][s] =
              model.addVar(0, availability[w][s], 0, GRB.BINARY,
                           Workers[w] + "." + Shifts[s]);
        }
      }

      // Slack variables for each shift constraint so that the shifts can
      // be satisfied
      GRBVar[] slacks = new GRBVar[nShifts];
      for (int s = 0; s < nShifts; ++s) {
        slacks[s] =
            model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS,
                         Shifts[s] + "Slack");
      }

      // Variable to represent the total slack
      GRBVar totSlack = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS,
                                     "totSlack");

      // Variables to count the total shifts worked by each worker
      GRBVar[] totShifts = new GRBVar[nWorkers];
      for (int w = 0; w < nWorkers; ++w) {
        totShifts[w] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS,
                                    Workers[w] + "TotShifts");
      }

      // Update model to integrate new variables
      model.update();

      GRBLinExpr lhs;

      // Constraint: assign exactly shiftRequirements[s] workers
      // to each shift s, plus the slack
      for (int s = 0; s < nShifts; ++s) {
        lhs = new GRBLinExpr();
        lhs.addTerm(1.0, slacks[s]);
        for (int w = 0; w < nWorkers; ++w) {
          lhs.addTerm(1.0, x[w][s]);
        }
        model.addConstr(lhs, GRB.EQUAL, shiftRequirements[s], Shifts[s]);
      }

      // Constraint: set totSlack equal to the total slack
      lhs = new GRBLinExpr();
      lhs.addTerm(-1.0, totSlack);
      for (int s = 0; s < nShifts; ++s) {
        lhs.addTerm(1.0, slacks[s]);
      }
      model.addConstr(lhs, GRB.EQUAL, 0, "totSlack");

      // Constraint: compute the total number of shifts for each worker
      for (int w = 0; w < nWorkers; ++w) {
        lhs = new GRBLinExpr();
        lhs.addTerm(-1.0, totShifts[w]);
        for (int s = 0; s < nShifts; ++s) {
          lhs.addTerm(1.0, x[w][s]);
        }
        model.addConstr(lhs, GRB.EQUAL, 0, "totShifts" + Workers[w]);
      }

      // Objective: minimize the total slack
      GRBLinExpr obj = new GRBLinExpr();
      obj.addTerm(1.0, totSlack);
      model.setObjective(obj);

      // Optimize
      int status =
        solveAndPrint(model, totSlack, nWorkers, Workers, totShifts);
      if (status != GRB.Status.OPTIMAL ) {
        return;
      }

      // Constrain the slack by setting its upper and lower bounds
      totSlack.set(GRB.DoubleAttr.UB, totSlack.get(GRB.DoubleAttr.X));
      totSlack.set(GRB.DoubleAttr.LB, totSlack.get(GRB.DoubleAttr.X));

      // Variable to count the average number of shifts worked
      GRBVar avgShifts =
        model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "avgShifts");

      // Variables to count the difference from average for each worker;
      // note that these variables can take negative values.
      GRBVar[] diffShifts = new GRBVar[nWorkers];
      for (int w = 0; w < nWorkers; ++w) {
        diffShifts[w] = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0,
                                     GRB.CONTINUOUS, Workers[w] + "Diff");
      }

      // Update model to integrate new variables
      model.update();

      // Constraint: compute the average number of shifts worked
      lhs = new GRBLinExpr();
      lhs.addTerm(-nWorkers, avgShifts);
      for (int w = 0; w < nWorkers; ++w) {
        lhs.addTerm(1.0, totShifts[w]);
      }
      model.addConstr(lhs, GRB.EQUAL, 0, "avgShifts");

      // Constraint: compute the difference from the average number of shifts
      for (int w = 0; w < nWorkers; ++w) {
        lhs = new GRBLinExpr();
        lhs.addTerm(-1, diffShifts[w]);
        lhs.addTerm(-1, avgShifts);
        lhs.addTerm( 1, totShifts[w]);
        model.addConstr(lhs, GRB.EQUAL, 0, Workers[w] + "Diff");
      }

      // Objective: minimize the sum of the square of the difference from the
      // average number of shifts worked
      GRBQuadExpr qobj = new GRBQuadExpr();
      for (int w = 0; w < nWorkers; ++w) {
        qobj.addTerm(1.0, diffShifts[w], diffShifts[w]);
      }
      model.setObjective(qobj);

      // Optimize
      status =
        solveAndPrint(model, totSlack, nWorkers, Workers, totShifts);
      if (status != GRB.Status.OPTIMAL ) {
        return;
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }

  private static int solveAndPrint(GRBModel model, GRBVar totSlack,
                                   int nWorkers, String[] Workers,
                                   GRBVar[] totShifts) throws GRBException {

    model.optimize();
    int status = model.get(GRB.IntAttr.Status);
    if (status == GRB.Status.INF_OR_UNBD ||
        status == GRB.Status.INFEASIBLE  ||
        status == GRB.Status.UNBOUNDED     ) {
      System.out.println("The model cannot be solved "
          + "because it is infeasible or unbounded");
      return status;
    }
    if (status != GRB.Status.OPTIMAL ) {
      System.out.println("Optimization was stopped with status " + status);
      return status;
    }

    // Print total slack and the number of shifts worked for each worker
    System.out.println("\nTotal slack required: " +
                       totSlack.get(GRB.DoubleAttr.X));
    for (int w = 0; w < nWorkers; ++w) {
      System.out.println(Workers[w] + " worked " +
                         totShifts[w].get(GRB.DoubleAttr.X) + " shifts");
    }
    System.out.println("\n");
    return status;
  }

}
