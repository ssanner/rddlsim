/* Copyright 2014, Gurobi Optimization, Inc. */

/* Use parameters that are associated with a model.

   A MIP is solved for 5 seconds with different sets of parameters.
   The one with the smallest MIP gap is selected, and the optimization
   is resumed until the optimal solution is found.
*/

import gurobi.*;

public class Params {

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Params filename");
      System.exit(1);
    }

    try {
      // Read model and verify that it is a MIP
      GRBEnv env = new GRBEnv();
      GRBModel m = new GRBModel(env, args[0]);
      if (m.get(GRB.IntAttr.IsMIP) == 0) {
        System.out.println("The model is not an integer program");
        System.exit(1);
      }

      // Set a 5 second time limit
      m.getEnv().set(GRB.DoubleParam.TimeLimit, 5);

      // Now solve the model with different values of MIPFocus
      GRBModel bestModel = new GRBModel(m);
      bestModel.optimize();
      for (int i = 1; i <= 3; ++i) {
        m.reset();
        m.getEnv().set(GRB.IntParam.MIPFocus, i);
        m.optimize();
        if (bestModel.get(GRB.DoubleAttr.MIPGap) >
                    m.get(GRB.DoubleAttr.MIPGap)) {
          GRBModel swap = bestModel;
          bestModel = m;
          m = swap;
        }
      }

      // Finally, delete the extra model, reset the time limit and
      // continue to solve the best model to optimality
      m.dispose();
      bestModel.getEnv().set(GRB.DoubleParam.TimeLimit, GRB.INFINITY);
      bestModel.optimize();
      System.out.println("Solved with MIPFocus: " +
          bestModel.getEnv().get(GRB.IntParam.MIPFocus));

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
