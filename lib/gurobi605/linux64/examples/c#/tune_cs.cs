/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a model from a file and tunes it.
   It then writes the best parameter settings to a file
   and solves the model using these parameters. */

using System;
using Gurobi;

class tune_cs
{
  static void Main(string[] args)
  {
    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: tune_cs filename");
      return;
    }

    try {
      GRBEnv env = new GRBEnv();

      // Read model from file
      GRBModel model = new GRBModel(env, args[0]);

      // Set the TuneResults parameter to 1
      model.GetEnv().Set(GRB.IntParam.TuneResults, 1);

      // Tune the model
      model.Tune();

      // Get the number of tuning results
      int resultcount = model.Get(GRB.IntAttr.TuneResultCount);

      if (resultcount > 0) {

        // Load the tuned parameters into the model's environment
        model.GetTuneResult(0);

        // Write the tuned parameters to a file
        model.Write("tune.prm");

        // Solve the model using the tuned parameters
        model.Optimize();
      }

      // Dispose of model and environment
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
