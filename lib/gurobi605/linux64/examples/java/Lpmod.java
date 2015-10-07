/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads an LP model from a file and solves it.
   If the model can be solved, then it finds the smallest positive variable,
   sets its upper bound to zero, and resolves the model two ways:
   first with an advanced start, then without an advanced start
   (i.e. 'from scratch'). */

import gurobi.*;

public class Lpmod {
  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Lpmod filename");
      System.exit(1);
    }

    try {
      // Read model and determine whether it is an LP
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);
      if (model.get(GRB.IntAttr.IsMIP) != 0) {
        System.out.println("The model is not a linear program");
        System.exit(1);
      }

      model.optimize();

      int status = model.get(GRB.IntAttr.Status);

      if (status == GRB.Status.INF_OR_UNBD ||
          status == GRB.Status.INFEASIBLE  ||
          status == GRB.Status.UNBOUNDED     ) {
        System.out.println("The model cannot be solved because it is "
            + "infeasible or unbounded");
        System.exit(1);
      }

      if (status != GRB.Status.OPTIMAL) {
        System.out.println("Optimization was stopped with status " + status);
        System.exit(0);
      }

      // Find the smallest variable value
      double minVal = GRB.INFINITY;
      GRBVar minVar = null;
      for (GRBVar v : model.getVars()) {
        double sol = v.get(GRB.DoubleAttr.X);
        if ((sol > 0.0001) && (sol < minVal) &&
            (v.get(GRB.DoubleAttr.LB) == 0.0)) {
          minVal = sol;
          minVar = v;
        }
      }

      System.out.println("\n*** Setting " +
          minVar.get(GRB.StringAttr.VarName) + " from " + minVal +
          " to zero ***\n");
      minVar.set(GRB.DoubleAttr.UB, 0.0);

      // Solve from this starting point
      model.optimize();

      // Save iteration & time info
      double warmCount = model.get(GRB.DoubleAttr.IterCount);
      double warmTime = model.get(GRB.DoubleAttr.Runtime);

      // Reset the model and resolve
      System.out.println("\n*** Resetting and solving "
          + "without an advanced start ***\n");
      model.reset();
      model.optimize();

      double coldCount = model.get(GRB.DoubleAttr.IterCount);
      double coldTime = model.get(GRB.DoubleAttr.Runtime);

      System.out.println("\n*** Warm start: " + warmCount + " iterations, " +
          warmTime + " seconds");
      System.out.println("*** Cold start: " + coldCount + " iterations, " +
          coldTime + " seconds");

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
