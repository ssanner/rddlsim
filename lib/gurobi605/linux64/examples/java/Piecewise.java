/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example considers the following separable, convex problem:

     minimize    f(x) - y + g(z)
     subject to  x + 2 y + 3 z <= 4
                 x +   y       >= 1
                 x,    y,    z <= 1

   where f(u) = exp(-u) and g(u) = 2 u^2 - 4 u, for all real u. It
   formulates and solves a simpler LP model by approximating f and
   g with piecewise-linear functions. Then it transforms the model
   into a MIP by negating the approximation for f, which corresponds
   to a non-convex piecewise-linear function, and solves it again.
*/

import gurobi.*;

public class Piecewise {

  private static double f(double u) { return Math.exp(-u); }
  private static double g(double u) { return 2 * u * u - 4 * u; }

  public static void main(String[] args) {
    try {

      // Create environment

      GRBEnv env = new GRBEnv();

      // Create a new model

      GRBModel model = new GRBModel(env);

      // Create variables

      double lb = 0.0, ub = 1.0;

      GRBVar x = model.addVar(lb, ub, 0.0, GRB.CONTINUOUS, "x");
      GRBVar y = model.addVar(lb, ub, 0.0, GRB.CONTINUOUS, "y");
      GRBVar z = model.addVar(lb, ub, 0.0, GRB.CONTINUOUS, "z");

      // Integrate new variables

      model.update();

      // Set objective for y

      GRBLinExpr obj = new GRBLinExpr();
      obj.addTerm(-1.0, y);
      model.setObjective(obj);

      // Add piecewise-linear objective functions for x and z

      int npts = 101;
      double[] ptu = new double[npts];
      double[] ptf = new double[npts];
      double[] ptg = new double[npts];

      for (int i = 0; i < npts; i++) {
        ptu[i] = lb + (ub - lb) * i / (npts - 1);
        ptf[i] = f(ptu[i]);
        ptg[i] = g(ptu[i]);
      }

      model.setPWLObj(x, ptu, ptf);
      model.setPWLObj(z, ptu, ptg);

      // Add constraint: x + 2 y + 3 z <= 4

      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
      model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

      // Add constraint: x + y >= 1

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y);
      model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

      // Optimize model as an LP

      model.optimize();

      System.out.println("IsMIP: " + model.get(GRB.IntAttr.IsMIP));

      System.out.println(x.get(GRB.StringAttr.VarName)
                         + " " +x.get(GRB.DoubleAttr.X));
      System.out.println(y.get(GRB.StringAttr.VarName)
                         + " " +y.get(GRB.DoubleAttr.X));
      System.out.println(z.get(GRB.StringAttr.VarName)
                         + " " +z.get(GRB.DoubleAttr.X));

      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

      System.out.println();

      // Negate piecewise-linear objective function for x

      for (int i = 0; i < npts; i++) {
        ptf[i] = -ptf[i];
      }

      model.setPWLObj(x, ptu, ptf);

      // Optimize model as a MIP

      model.optimize();

      System.out.println("IsMIP: " + model.get(GRB.IntAttr.IsMIP));

      System.out.println(x.get(GRB.StringAttr.VarName)
                         + " " +x.get(GRB.DoubleAttr.X));
      System.out.println(y.get(GRB.StringAttr.VarName)
                         + " " +y.get(GRB.DoubleAttr.X));
      System.out.println(z.get(GRB.StringAttr.VarName)
                         + " " +z.get(GRB.DoubleAttr.X));

      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

      // Dispose of model and environment

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
