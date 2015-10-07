/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a model from a file and tunes it.
   It then writes the best parameter settings to a file
   and solves the model using these parameters. */

import gurobi.*;

public class Tune {
  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Tune filename");
      System.exit(1);
    }

    try {
      GRBEnv env = new GRBEnv();

      // Read model from file
      GRBModel model = new GRBModel(env, args[0]);

      // Set the TuneResults parameter to 1
      model.getEnv().set(GRB.IntParam.TuneResults, 1);

      // Tune the model
      model.tune();

      // Get the number of tuning results
      int resultcount = model.get(GRB.IntAttr.TuneResultCount);

      if (resultcount > 0) {

        // Load the tuned parameters into the model's environment
        model.getTuneResult(0);

        // Write the tuned parameters to a file
        model.write("tune.prm");

        // Solve the model using the tuned parameters
        model.optimize();
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". "
          + e.getMessage());
    }
  }
}
