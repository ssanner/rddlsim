/* Copyright 2014, Gurobi Optimization, Inc. */

/* A simple sensitivity analysis example which reads a MIP model
   from a file and solves it. Then each binary variable is set
   to 1-X, where X is its value in the optimal solution, and
   the impact on the objective function value is reported.
*/

import gurobi.*;

public class Sensitivity {

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Sensitivity filename");
      System.exit(1);
    }

    try {

      // Create environment

      GRBEnv env = new GRBEnv();

      // Read and solve model

      GRBModel model = new GRBModel(env, args[0]);

      if (model.get(GRB.IntAttr.IsMIP) == 0) {
        System.out.println("Model is not a MIP");
        System.exit(1);
      }

      model.optimize();

      if (model.get(GRB.IntAttr.Status) != GRB.OPTIMAL) {
        System.out.println("Optimization ended with status "
            + model.get(GRB.IntAttr.Status));
        System.exit(1);
      }

      // Store the optimal solution

      double   origObjVal = model.get(GRB.DoubleAttr.ObjVal);
      GRBVar[] vars       = model.getVars();
      double[] origX      = model.get(GRB.DoubleAttr.X, vars);

      // Disable solver output for subsequent solves

      model.getEnv().set(GRB.IntParam.OutputFlag, 0);

      // Iterate through unfixed, binary variables in model

      for (int i = 0; i < vars.length; i++) {
        GRBVar v     = vars[i];
        char   vType = v.get(GRB.CharAttr.VType);

        if (v.get(GRB.DoubleAttr.LB) == 0 && v.get(GRB.DoubleAttr.UB) == 1
            && (vType == GRB.BINARY || vType == GRB.INTEGER)) {

          // Set variable to 1-X, where X is its value in optimal solution

          if (origX[i] < 0.5) {
            v.set(GRB.DoubleAttr.LB, 1.0);
            v.set(GRB.DoubleAttr.Start, 1.0);
          } else {
            v.set(GRB.DoubleAttr.UB, 0.0);
            v.set(GRB.DoubleAttr.Start, 0.0);
          }

          // Update MIP start for the other variables

          for (int j = 0; j < vars.length; j++) {
            if (j != i) {
              vars[j].set(GRB.DoubleAttr.Start, origX[j]);
            }
          }

          // Solve for new value and capture sensitivity information

          model.optimize();

          if (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
            System.out.println("Objective sensitivity for variable "
                + v.get(GRB.StringAttr.VarName) + " is "
                + (model.get(GRB.DoubleAttr.ObjVal) - origObjVal));
          } else {
            System.out.println("Objective sensitivity for variable "
                + v.get(GRB.StringAttr.VarName) + " is infinite");
          }

          // Restore the original variable bounds

          v.set(GRB.DoubleAttr.LB, 0.0);
          v.set(GRB.DoubleAttr.UB, 1.0);
        }
      }

      // Dispose of model and environment

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode());
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
