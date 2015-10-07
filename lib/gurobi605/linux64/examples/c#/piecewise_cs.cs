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

using System;
using Gurobi;

class piecewise_cs
{

  private static double f(double u) { return Math.Exp(-u); }
  private static double g(double u) { return 2 * u * u - 4 * u; }

  static void Main()
  {
    try {

      // Create environment

      GRBEnv env = new GRBEnv();

      // Create a new model

      GRBModel model = new GRBModel(env);

      // Create variables

      double lb = 0.0, ub = 1.0;

      GRBVar x = model.AddVar(lb, ub, 0.0, GRB.CONTINUOUS, "x");
      GRBVar y = model.AddVar(lb, ub, 0.0, GRB.CONTINUOUS, "y");
      GRBVar z = model.AddVar(lb, ub, 0.0, GRB.CONTINUOUS, "z");

      // Integrate new variables

      model.Update();

      // Set objective for y

      model.SetObjective(-y);

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

      model.SetPWLObj(x, ptu, ptf);
      model.SetPWLObj(z, ptu, ptg);

      // Add constraint: x + 2 y + 3 z <= 4

      model.AddConstr(x + 2 * y + 3 * z <= 4.0, "c0");

      // Add constraint: x + y >= 1

      model.AddConstr(x + y >= 1.0, "c1");

      // Optimize model as an LP

      model.Optimize();

      Console.WriteLine("IsMIP: " + model.Get(GRB.IntAttr.IsMIP));

      Console.WriteLine(x.Get(GRB.StringAttr.VarName)
                         + " " + x.Get(GRB.DoubleAttr.X));
      Console.WriteLine(y.Get(GRB.StringAttr.VarName)
                         + " " + y.Get(GRB.DoubleAttr.X));
      Console.WriteLine(z.Get(GRB.StringAttr.VarName)
                         + " " + z.Get(GRB.DoubleAttr.X));

      Console.WriteLine("Obj: " + model.Get(GRB.DoubleAttr.ObjVal));

      Console.WriteLine();

      // Negate piecewise-linear objective function for x

      for (int i = 0; i < npts; i++) {
        ptf[i] = -ptf[i];
      }

      model.SetPWLObj(x, ptu, ptf);

      // Optimize model as a MIP

      model.Optimize();

      Console.WriteLine("IsMIP: " + model.Get(GRB.IntAttr.IsMIP));

      Console.WriteLine(x.Get(GRB.StringAttr.VarName)
                         + " " + x.Get(GRB.DoubleAttr.X));
      Console.WriteLine(y.Get(GRB.StringAttr.VarName)
                         + " " + y.Get(GRB.DoubleAttr.X));
      Console.WriteLine(z.Get(GRB.StringAttr.VarName)
                         + " " + z.Get(GRB.DoubleAttr.X));

      Console.WriteLine("Obj: " + model.Get(GRB.DoubleAttr.ObjVal));

      // Dispose of model and environment

      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
