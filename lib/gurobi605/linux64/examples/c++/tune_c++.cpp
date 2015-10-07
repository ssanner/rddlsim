/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a model from a file and tunes it.
   It then writes the best parameter settings to a file
   and solves the model using these parameters. */

#include "gurobi_c++.h"
#include <cmath>
using namespace std;

int
main(int   argc,
     char *argv[])
{
  if (argc < 2) {
    cout << "Usage: tune_c++ filename" << endl;
    return 1;
  }

  GRBEnv *env = 0;
  try {
    env = new GRBEnv();

    // Read model from file

    GRBModel model = GRBModel(*env, argv[1]);

    GRBEnv menv = model.getEnv();

    // Set the TuneResults parameter to 1

    menv.set(GRB_IntParam_TuneResults, 1);

    // Tune the model

    model.tune();

    // Get the number of tuning results

    int resultcount = model.get(GRB_IntAttr_TuneResultCount);

    if (resultcount > 0) {

      // Load the tuned parameters into the model's environment

      model.getTuneResult(0);

      // Write tuned parameters to a file

      model.write("tune.prm");

      // Solve the model using the tuned parameters

      model.optimize();
    }
  } catch(GRBException e) {
    cout << "Error code = " << e.getErrorCode() << endl;
    cout << e.getMessage() << endl;
  } catch (...) {
    cout << "Error during tuning" << endl;
  }

  delete env;
  return 0;
}
