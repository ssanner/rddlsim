/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QP model:

     minimize    x^2 + x*y + y^2 + y*z + z^2 + 2 x
     subject to  x + 2 y + 3 z >= 4
                 x +   y       >= 1

   It solves it once as a continuous model, and once as an integer model.
*/

import gurobi.*;

public class Qp {
  public static void main(String[] args) {
    try {
      GRBEnv    env   = new GRBEnv("qp.log");
      GRBModel  model = new GRBModel(env);

      // Create variables

      GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "x");
      GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "y");
      GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "z");

      // Integrate new variables

      model.update();

      // Set objective

      GRBQuadExpr obj = new GRBQuadExpr();
      obj.addTerm(1.0, x, x);
      obj.addTerm(1.0, x, y);
      obj.addTerm(1.0, y, y);
      obj.addTerm(1.0, y, z);
      obj.addTerm(1.0, z, z);
      obj.addTerm(2.0, x);
      model.setObjective(obj);

      // Add constraint: x + 2 y + 3 z >= 4

      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
      model.addConstr(expr, GRB.GREATER_EQUAL, 4.0, "c0");

      // Add constraint: x + y >= 1

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y);
      model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

      // Optimize model

      model.optimize();

      System.out.println(x.get(GRB.StringAttr.VarName)
                         + " " +x.get(GRB.DoubleAttr.X));
      System.out.println(y.get(GRB.StringAttr.VarName)
                         + " " +y.get(GRB.DoubleAttr.X));
      System.out.println(z.get(GRB.StringAttr.VarName)
                         + " " +z.get(GRB.DoubleAttr.X));

      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal) + " " +
                         obj.getValue());
      System.out.println();


      // Change variable types to integer

      x.set(GRB.CharAttr.VType, GRB.INTEGER);
      y.set(GRB.CharAttr.VType, GRB.INTEGER);
      z.set(GRB.CharAttr.VType, GRB.INTEGER);

      // Optimize again

      model.optimize();

      System.out.println(x.get(GRB.StringAttr.VarName)
                         + " " +x.get(GRB.DoubleAttr.X));
      System.out.println(y.get(GRB.StringAttr.VarName)
                         + " " +y.get(GRB.DoubleAttr.X));
      System.out.println(z.get(GRB.StringAttr.VarName)
                         + " " +z.get(GRB.DoubleAttr.X));

      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal) + " " +
                         obj.getValue());

      // Dispose of model and environment

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
