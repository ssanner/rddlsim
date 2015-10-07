/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, adds artificial
   variables to each constraint, and then minimizes the sum of the
   artificial variables.  A solution with objective zero corresponds
   to a feasible solution to the input model.
   We can also use FeasRelax feature to do it. In this example, we
   use minrelax=1, i.e. optimizing the returned model finds a solution
   that minimizes the original objective, but only from among those
   solutions that minimize the sum of the artificial variables. */

using Gurobi;
using System;

class feasopt_cs
{
  static void Main(string[] args)
  {
    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: feasopt_cs filename");
      return;
    }

    try {
      GRBEnv env = new GRBEnv();
      GRBModel feasmodel = new GRBModel(env, args[0]);

      // Create a copy to use FeasRelax feature later */
      GRBModel feasmodel1 = new GRBModel(feasmodel);

      // Clear objective
      feasmodel.SetObjective(new GRBLinExpr());

      // Add slack variables
      GRBConstr[] c = feasmodel.GetConstrs();
      for (int i = 0; i < c.Length; ++i) {
        char sense = c[i].Get(GRB.CharAttr.Sense);
        if (sense != '>') {
          GRBConstr[] constrs = new GRBConstr[] { c[i] };
          double[] coeffs = new double[] { -1 };
          feasmodel.AddVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, constrs,
                           coeffs, "ArtN_" + c[i].Get(GRB.StringAttr.ConstrName));
        }
        if (sense != '<') {
          GRBConstr[] constrs = new GRBConstr[] { c[i] };
          double[] coeffs = new double[] { 1 };
          feasmodel.AddVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, constrs,
                           coeffs, "ArtP_" +
                               c[i].Get(GRB.StringAttr.ConstrName));
        }
      }
      feasmodel.Update();

      // Optimize modified model
      feasmodel.Write("feasopt.lp");
      feasmodel.Optimize();

      // Use FeasRelax feature */
      feasmodel1.FeasRelax(GRB.FEASRELAX_LINEAR, true, false, true);
      feasmodel1.Write("feasopt1.lp");
      feasmodel1.Optimize();

      // Dispose of model and env
      feasmodel1.Dispose();
      feasmodel.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
