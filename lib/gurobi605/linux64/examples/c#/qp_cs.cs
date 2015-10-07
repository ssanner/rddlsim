/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple QP model:

     minimize    x^2 + x*y + y^2 + y*z + z^2 + 2 x
     subject to  x + 2 y + 3 z >= 4
                 x +   y       >= 1

   It solves it once as a continuous model, and once as an integer model.
*/

using System;
using Gurobi;

class qp_cs
{
  static void Main()
  {
    try {
      GRBEnv    env   = new GRBEnv("qp.log");
      GRBModel  model = new GRBModel(env);

      // Create variables

      GRBVar x = model.AddVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "x");
      GRBVar y = model.AddVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "y");
      GRBVar z = model.AddVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "z");

      // Integrate new variables

      model.Update();

      // Set objective

      GRBQuadExpr obj = x*x + x*y + y*y + y*z + z*z + 2*x;
      model.SetObjective(obj);

      // Add constraint: x + 2 y + 3 z >= 4

      model.AddConstr(x + 2 * y + 3 * z >= 4.0, "c0");

      // Add constraint: x + y >= 1

      model.AddConstr(x + y >= 1.0, "c1");

      // Optimize model

      model.Optimize();

      Console.WriteLine(x.Get(GRB.StringAttr.VarName)
                         + " " + x.Get(GRB.DoubleAttr.X));
      Console.WriteLine(y.Get(GRB.StringAttr.VarName)
                         + " " + y.Get(GRB.DoubleAttr.X));
      Console.WriteLine(z.Get(GRB.StringAttr.VarName)
                         + " " + z.Get(GRB.DoubleAttr.X));

      Console.WriteLine("Obj: " + model.Get(GRB.DoubleAttr.ObjVal) + " " +
                        obj.Value);


      // Change variable types to integer

      x.Set(GRB.CharAttr.VType, GRB.INTEGER);
      y.Set(GRB.CharAttr.VType, GRB.INTEGER);
      z.Set(GRB.CharAttr.VType, GRB.INTEGER);

      // Optimize model

      model.Optimize();

      Console.WriteLine(x.Get(GRB.StringAttr.VarName)
                         + " " + x.Get(GRB.DoubleAttr.X));
      Console.WriteLine(y.Get(GRB.StringAttr.VarName)
                         + " " + y.Get(GRB.DoubleAttr.X));
      Console.WriteLine(z.Get(GRB.StringAttr.VarName)
                         + " " + z.Get(GRB.DoubleAttr.X));

      Console.WriteLine("Obj: " + model.Get(GRB.DoubleAttr.ObjVal) + " " +
                        obj.Value);

      // Dispose of model and env

      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
