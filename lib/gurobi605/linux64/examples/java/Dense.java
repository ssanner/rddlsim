/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QP model:

     minimize    x + y + x^2 + x*y + y^2 + y*z + z^2
     subject to  x + 2 y + 3 z >= 4
                 x +   y       >= 1

   The example illustrates the use of dense matrices to store A and Q
   (and dense vectors for the other relevant data).  We don't recommend
   that you use dense matrices, but this example may be helpful if you
   already have your data in this format.
*/

import gurobi.*;

public class Dense {

  protected static boolean
    dense_optimize(GRBEnv     env,
                   int        rows,
                   int        cols,
                   double[]   c,      // linear portion of objective function
                   double[][] Q,      // quadratic portion of objective function
                   double[][] A,      // constraint matrix
                   char[]     sense,  // constraint senses
                   double[]   rhs,    // RHS vector
                   double[]   lb,     // variable lower bounds
                   double[]   ub,     // variable upper bounds
                   char[]     vtype,  // variable types (continuous, binary, etc.)
                   double[]   solution) {

    boolean success = false;

    try {
      GRBModel model = new GRBModel(env);

      // Add variables to the model

      GRBVar[] vars = model.addVars(lb, ub, null, vtype, null);
      model.update();

      // Populate A matrix

      for (int i = 0; i < rows; i++) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int j = 0; j < cols; j++)
          if (A[i][j] != 0)
            expr.addTerm(A[i][j], vars[j]);
        model.addConstr(expr, sense[i], rhs[i], "");
      }

      // Populate objective

      GRBQuadExpr obj = new GRBQuadExpr();
      if (Q != null) {
        for (int i = 0; i < cols; i++)
          for (int j = 0; j < cols; j++)
            if (Q[i][j] != 0)
              obj.addTerm(Q[i][j], vars[i], vars[j]);
        for (int j = 0; j < cols; j++)
          if (c[j] != 0)
            obj.addTerm(c[j], vars[j]);
        model.setObjective(obj);
      }

      // Solve model

      model.optimize();

      // Extract solution

      if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
        success = true;

        for (int j = 0; j < cols; j++)
          solution[j] = vars[j].get(GRB.DoubleAttr.X);
      }

      model.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
      e.printStackTrace();
    }

    return success;
  }

  public static void main(String[] args) {
    try {
      GRBEnv env = new GRBEnv();

      double c[] = new double[] {1, 1, 0};
      double Q[][] = new double[][] {{1, 1, 0}, {0, 1, 1}, {0, 0, 1}};
      double A[][] = new double[][] {{1, 2, 3}, {1, 1, 0}};
      char sense[] = new char[] {'>', '>'};
      double rhs[] = new double[] {4, 1};
      double lb[] = new double[] {0, 0, 0};
      boolean success;
      double sol[] = new double[3];


      success = dense_optimize(env, 2, 3, c, Q, A, sense, rhs,
                               lb, null, null, sol);

      if (success) {
        System.out.println("x: " + sol[0] + ", y: " + sol[1] + ", z: " + sol[2]);
      }

      // Dispose of environment
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
      e.printStackTrace();
    }
  }
}
