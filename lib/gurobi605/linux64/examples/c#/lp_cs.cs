/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads an LP model from a file and solves it.
   If the model is infeasible or unbounded, the example turns off
   presolve and solves the model again. If the model is infeasible,
   the example computes an Irreducible Inconsistent Subsystem (IIS),
   and writes it to a file. */

using System;
using Gurobi;

class lp_cs
{
  static void Main(string[] args)
  {
    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: lp_cs filename");
      return;
    }

    try {
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);

      model.Optimize();

      int optimstatus = model.Get(GRB.IntAttr.Status);

      if (optimstatus == GRB.Status.INF_OR_UNBD) {
        model.GetEnv().Set(GRB.IntParam.Presolve, 0);
        model.Optimize();
        optimstatus = model.Get(GRB.IntAttr.Status);
      }

      if (optimstatus == GRB.Status.OPTIMAL) {
        double objval = model.Get(GRB.DoubleAttr.ObjVal);
        Console.WriteLine("Optimal objective: " + objval);
      } else if (optimstatus == GRB.Status.INFEASIBLE) {
        Console.WriteLine("Model is infeasible");

        // compute and write out IIS

        model.ComputeIIS();
        model.Write("model.ilp");
      } else if (optimstatus == GRB.Status.UNBOUNDED) {
        Console.WriteLine("Model is unbounded");
      } else {
        Console.WriteLine("Optimization was stopped with status = "
                           + optimstatus);
      }

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
