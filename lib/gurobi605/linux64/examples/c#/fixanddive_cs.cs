/* Copyright 2014, Gurobi Optimization, Inc. */

/* Implement a simple MIP heuristic.  Relax the model,
   sort variables based on fractionality, and fix the 25% of
   the fractional variables that are closest to integer variables.
   Repeat until either the relaxation is integer feasible or
   linearly infeasible. */

using System;
using System.Collections.Generic;
using Gurobi;

class fixanddive_cs
{
  // Comparison class used to sort variable list based on relaxation
  // fractionality

  class FractionalCompare : IComparer<GRBVar>
  {
    public int Compare(GRBVar v1, GRBVar v2)
    {
      try {
        double sol1 = Math.Abs(v1.Get(GRB.DoubleAttr.X));
        double sol2 = Math.Abs(v2.Get(GRB.DoubleAttr.X));
        double frac1 = Math.Abs(sol1 - Math.Floor(sol1 + 0.5));
        double frac2 = Math.Abs(sol2 - Math.Floor(sol2 + 0.5));
        if (frac1 < frac2) {
          return -1;
        } else if (frac1 > frac2) {
          return 1;
        } else {
          return 0;
        }
      } catch (GRBException e) {
        Console.WriteLine("Error code: " + e.ErrorCode + ". " +
            e.Message);
      }
      return 0;
    }
  }

  static void Main(string[] args)
  {
    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: fixanddive_cs filename");
      return;
    }

    try {
      // Read model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);

      // Collect integer variables and relax them
      List<GRBVar> intvars = new List<GRBVar>();
      foreach (GRBVar v in model.GetVars()) {
        if (v.Get(GRB.CharAttr.VType) != GRB.CONTINUOUS) {
          intvars.Add(v);
          v.Set(GRB.CharAttr.VType, GRB.CONTINUOUS);
        }
      }

      model.GetEnv().Set(GRB.IntParam.OutputFlag, 0);
      model.Optimize();

      // Perform multiple iterations. In each iteration, identify the first
      // quartile of integer variables that are closest to an integer value
      // in the relaxation, fix them to the nearest integer, and repeat.

      for (int iter = 0; iter < 1000; ++iter) {

        // create a list of fractional variables, sorted in order of
        // increasing distance from the relaxation solution to the nearest
        // integer value

        List<GRBVar> fractional = new List<GRBVar>();
        foreach (GRBVar v in intvars) {
          double sol = Math.Abs(v.Get(GRB.DoubleAttr.X));
          if (Math.Abs(sol - Math.Floor(sol + 0.5)) > 1e-5) {
            fractional.Add(v);
          }
        }

        Console.WriteLine("Iteration " + iter + ", obj " +
            model.Get(GRB.DoubleAttr.ObjVal) + ", fractional " +
            fractional.Count);

        if (fractional.Count == 0) {
          Console.WriteLine("Found feasible solution - objective " +
              model.Get(GRB.DoubleAttr.ObjVal));
          break;
        }

        // Fix the first quartile to the nearest integer value

        fractional.Sort(new FractionalCompare());
        int nfix = Math.Max(fractional.Count / 4, 1);
        for (int i = 0; i < nfix; ++i) {
          GRBVar v = fractional[i];
          double fixval = Math.Floor(v.Get(GRB.DoubleAttr.X) + 0.5);
          v.Set(GRB.DoubleAttr.LB, fixval);
          v.Set(GRB.DoubleAttr.UB, fixval);
          Console.WriteLine("  Fix " + v.Get(GRB.StringAttr.VarName) +
              " to " + fixval + " ( rel " + v.Get(GRB.DoubleAttr.X) + " )");
        }

        model.Optimize();

        // Check optimization result

        if (model.Get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
          Console.WriteLine("Relaxation is infeasible");
          break;
        }
      }

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " +
          e.Message);
    }
  }
}
