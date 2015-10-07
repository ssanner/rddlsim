/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve a model with different values of the Method parameter;
   show which value gives the shortest solve time. */

import gurobi.*;

public class Lpmethod {

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Lpmethod filename");
      System.exit(1);
    }

    try {
      // Read model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);
      GRBEnv menv = model.getEnv();

      // Solve the model with different values of Method
      int bestMethod = -1;
      double bestTime = menv.get(GRB.DoubleParam.TimeLimit);
      for (int i = 0; i <= 2; ++i) {
        model.reset();
        menv.set(GRB.IntParam.Method, i);
        model.optimize();
        if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
          bestTime = model.get(GRB.DoubleAttr.Runtime);
          bestMethod = i;
          // Reduce the TimeLimit parameter to save time
          // with other methods
          menv.set(GRB.DoubleParam.TimeLimit, bestTime);
        }
      }

      // Report which method was fastest
      if (bestMethod == -1) {
        System.out.println("Unable to solve this model");
      } else {
        System.out.println("Solved in " + bestTime
            + " seconds with Method: " + bestMethod);
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". "
          + e.getMessage());
    }
  }
}
