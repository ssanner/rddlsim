/* Copyright 2014, Gurobi Optimization, Inc. */

/* Use parameters that are associated with a model.

   A MIP is solved for 5 seconds with different sets of parameters.
   The one with the smallest MIP gap is selected, and the optimization
   is resumed until the optimal solution is found.
*/

using System;
using Gurobi;

class params_cs
{
  static void Main(string[] args)
  {
    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: params_cs filename");
      return;
    }

    try {
      // Read model and verify that it is a MIP
      GRBEnv env = new GRBEnv();
      GRBModel m = new GRBModel(env, args[0]);
      if (m.Get(GRB.IntAttr.IsMIP) == 0) {
        Console.WriteLine("The model is not an integer program");
        Environment.Exit(1);
      }

      // Set a 5 second time limit
      m.GetEnv().Set(GRB.DoubleParam.TimeLimit, 5);

      // Now solve the model with different values of MIPFocus
      GRBModel bestModel = new GRBModel(m);
      bestModel.Optimize();
      for (int i = 1; i <= 3; ++i) {
        m.Reset();
        m.GetEnv().Set(GRB.IntParam.MIPFocus, i);
        m.Optimize();
        if (bestModel.Get(GRB.DoubleAttr.MIPGap) >
                    m.Get(GRB.DoubleAttr.MIPGap)) {
          GRBModel swap = bestModel;
          bestModel = m;
          m = swap;
        }
      }

      // Finally, delete the extra model, reset the time limit and
      // continue to solve the best model to optimality
      m.Dispose();
      bestModel.GetEnv().Set(GRB.DoubleParam.TimeLimit, GRB.INFINITY);
      bestModel.Optimize();
      Console.WriteLine("Solved with MIPFocus: " +
          bestModel.GetEnv().Get(GRB.IntParam.MIPFocus));

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " +
          e.Message);
    }
  }
}
