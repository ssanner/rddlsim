/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, solves it and
   prints the objective values from all feasible solutions
   generated while solving the MIP. Then it creates the fixed
   model and solves that model. */

using System;
using Gurobi;

class mip2_cs
{
  static void Main(string[] args)
  {
    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: mip2_cs filename");
      return;
    }

    try {
      GRBEnv    env   = new GRBEnv();
      GRBModel  model = new GRBModel(env, args[0]);
      if (model.Get(GRB.IntAttr.IsMIP) == 0) {
        Console.WriteLine("Model is not a MIP");
        return;
      }

      model.Optimize();

      int optimstatus = model.Get(GRB.IntAttr.Status);
      double objval = 0;
      if (optimstatus == GRB.Status.OPTIMAL) {
        objval = model.Get(GRB.DoubleAttr.ObjVal);
        Console.WriteLine("Optimal objective: " + objval);
      } else if (optimstatus == GRB.Status.INF_OR_UNBD) {
        Console.WriteLine("Model is infeasible or unbounded");
        return;
      } else if (optimstatus == GRB.Status.INFEASIBLE) {
        Console.WriteLine("Model is infeasible");
        return;
      } else if (optimstatus == GRB.Status.UNBOUNDED) {
        Console.WriteLine("Model is unbounded");
        return;
      } else {
        Console.WriteLine("Optimization was stopped with status = "
                           + optimstatus);
        return;
      }

      /* Iterate over the solutions and compute the objectives */

      GRBVar[] vars = model.GetVars();
      model.GetEnv().Set(GRB.IntParam.OutputFlag, 0);

      Console.WriteLine();
      for (int k = 0; k < model.Get(GRB.IntAttr.SolCount); ++k) {
        model.GetEnv().Set(GRB.IntParam.SolutionNumber, k);
        double objn = 0.0;

        for (int j = 0; j < vars.Length; j++) {
          objn += vars[j].Get(GRB.DoubleAttr.Obj)
            * vars[j].Get(GRB.DoubleAttr.Xn);
        }

        Console.WriteLine("Solution " + k + " has objective: " + objn);
      }
      Console.WriteLine();
      model.GetEnv().Set(GRB.IntParam.OutputFlag, 1);

      /* Create a fixed model, turn off presolve and solve */

      GRBModel fixedmodel = model.FixedModel();

      fixedmodel.GetEnv().Set(GRB.IntParam.Presolve, 0);

      fixedmodel.Optimize();

      int foptimstatus = fixedmodel.Get(GRB.IntAttr.Status);

      if (foptimstatus != GRB.Status.OPTIMAL) {
        Console.WriteLine("Error: fixed model isn't optimal");
        return;
      }

      double fobjval = fixedmodel.Get(GRB.DoubleAttr.ObjVal);

      if (Math.Abs(fobjval - objval) > 1.0e-6 * (1.0 + Math.Abs(objval))) {
        Console.WriteLine("Error: objective values are different");
        return;
      }

      GRBVar[] fvars  = fixedmodel.GetVars();
      double[] x      = fixedmodel.Get(GRB.DoubleAttr.X, fvars);
      string[] vnames = fixedmodel.Get(GRB.StringAttr.VarName, fvars);

      for (int j = 0; j < fvars.Length; j++) {
        if (x[j] != 0.0) Console.WriteLine(vnames[j] + " " + x[j]);
      }

      // Dispose of models and env
      fixedmodel.Dispose();
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
