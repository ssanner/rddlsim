/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, adds artificial
   variables to each constraint, and then minimizes the sum of the
   artificial variables.  A solution with objective zero corresponds
   to a feasible solution to the input model.
   We can also use FeasRelax feature to do it. In this example, we
   use minrelax=1, i.e. optimizing the returned model finds a solution
   that minimizes the original objective, but only from among those
   solutions that minimize the sum of the artificial variables. */

import gurobi.*;

public class Feasopt {
  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Feasopt filename");
      System.exit(1);
    }

    try {
      GRBEnv env = new GRBEnv();
      GRBModel feasmodel = new GRBModel(env, args[0]);

      // Create a copy to use FeasRelax feature later */
      GRBModel feasmodel1 = new GRBModel(feasmodel);

      // Clear objective
      feasmodel.setObjective(new GRBLinExpr());

      // Add slack variables
      GRBConstr[] c = feasmodel.getConstrs();
      for (int i = 0; i < c.length; ++i) {
        char sense = c[i].get(GRB.CharAttr.Sense);
        if (sense != '>') {
          GRBConstr[] constrs = new GRBConstr[] { c[i] };
          double[] coeffs = new double[] { -1 };
          feasmodel.addVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, constrs,
                           coeffs, "ArtN_" +
                               c[i].get(GRB.StringAttr.ConstrName));
        }
        if (sense != '<') {
          GRBConstr[] constrs = new GRBConstr[] { c[i] };
          double[] coeffs = new double[] { 1 };
          feasmodel.addVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, constrs,
                           coeffs, "ArtP_" +
                               c[i].get(GRB.StringAttr.ConstrName));
        }
      }
      feasmodel.update();

      // Optimize modified model
      feasmodel.write("feasopt.lp");
      feasmodel.optimize();

      // use FeasRelax feature */
      feasmodel1.feasRelax(GRB.FEASRELAX_LINEAR, true, false, true);
      feasmodel1.write("feasopt1.lp");
      feasmodel1.optimize();

      // Dispose of model and environment
      feasmodel1.dispose();
      feasmodel.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
