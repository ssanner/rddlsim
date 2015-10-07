/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example formulates and solves the following simple MIP model:

     maximize    x +   y + 2 z
     subject to  x + 2 y + 3 z <= 4
                 x +   y       >= 1
     x, y, z binary
*/

using System;
using Gurobi;

class mip1_cs
{
  static void Main()
  {
    try {
      GRBEnv    env   = new GRBEnv("mip1.log");
      GRBModel  model = new GRBModel(env);

      // Create variables

      GRBVar x = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
      GRBVar y = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
      GRBVar z = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, "z");

      // Integrate new variables

      model.Update();

      // Set objective: maximize x + y + 2 z

      model.SetObjective(x + y + 2 * z, GRB.MAXIMIZE);

      // Add constraint: x + 2 y + 3 z <= 4

      model.AddConstr(x + 2 * y + 3 * z <= 4.0, "c0");

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

      Console.WriteLine("Obj: " + model.Get(GRB.DoubleAttr.ObjVal));

      // Dispose of model and env

      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
