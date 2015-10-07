/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example creates a very simple Special Ordered Set (SOS) model.
   The model consists of 3 continuous variables, no linear constraints,
   and a pair of SOS constraints of type 1. */

using System;
using Gurobi;

class sos_cs
{
  static void Main()
  {
    try {
      GRBEnv env = new GRBEnv();

      GRBModel model = new GRBModel(env);

      // Create variables

      double[] ub    = {1, 1, 2};
      double[] obj   = {-2, -1, -1};
      string[] names = {"x0", "x1", "x2"};

      GRBVar[] x = model.AddVars(null, ub, obj, null, names);

      // Integrate new variables

      model.Update();

      // Add first SOS1: x0=0 or x1=0

      GRBVar[] sosv1  = {x[0], x[1]};
      double[] soswt1 = {1, 2};

      model.AddSOS(sosv1, soswt1, GRB.SOS_TYPE1);

      // Add second SOS1: x0=0 or x2=0

      GRBVar[] sosv2  = {x[0], x[2]};
      double[] soswt2 = {1, 2};

      model.AddSOS(sosv2, soswt2, GRB.SOS_TYPE1);

      // Optimize model

      model.Optimize();

      for (int i = 0; i < 3; i++)
        Console.WriteLine(x[i].Get(GRB.StringAttr.VarName) + " "
                           + x[i].Get(GRB.DoubleAttr.X));

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
