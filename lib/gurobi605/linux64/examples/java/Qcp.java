/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QCP model:

     maximize    x
     subject to  x + y + z = 1
                 x^2 + y^2 <= z^2 (second-order cone)
                 x^2 <= yz        (rotated second-order cone)
*/

import gurobi.*;

public class Qcp {
  public static void main(String[] args) {
    try {
      GRBEnv    env   = new GRBEnv("qcp.log");
      GRBModel  model = new GRBModel(env);

      // Create variables

      GRBVar x = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "x");
      GRBVar y = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "y");
      GRBVar z = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "z");

      // Integrate new variables

      model.update();

      // Set objective

      GRBLinExpr obj = new GRBLinExpr();
      obj.addTerm(1.0, x);
      model.setObjective(obj, GRB.MAXIMIZE);

      // Add linear constraint: x + y + z = 1

      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y); expr.addTerm(1.0, z);
      model.addConstr(expr, GRB.EQUAL, 1.0, "c0");

      // Add second-order cone: x^2 + y^2 <= z^2

      GRBQuadExpr qexpr = new GRBQuadExpr();
      qexpr.addTerm(1.0, x, x);
      qexpr.addTerm(1.0, y, y);
      qexpr.addTerm(-1.0, z, z);
      model.addQConstr(qexpr, GRB.LESS_EQUAL, 0.0, "qc0");

      // Add rotated cone: x^2 <= yz

      qexpr = new GRBQuadExpr();
      qexpr.addTerm(1.0, x, x);
      qexpr.addTerm(-1.0, y, z);
      model.addQConstr(qexpr, GRB.LESS_EQUAL, 0.0, "qc1");

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

      // Dispose of model and environment

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
