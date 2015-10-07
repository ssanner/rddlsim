/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example reads a MIP model from a file, solves it and
   prints the objective values from all feasible solutions
   generated while solving the MIP. Then it creates the fixed
   model and solves that model. */

import gurobi.*;

public class Mip2 {
  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Mip2 filename");
      System.exit(1);
    }

    try {
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);
      if (model.get(GRB.IntAttr.IsMIP) == 0) {
        System.out.println("Model is not a MIP");
        System.exit(1);
      }

      model.optimize();

      int optimstatus = model.get(GRB.IntAttr.Status);
      double objval = 0;
      if (optimstatus == GRB.Status.OPTIMAL) {
        objval = model.get(GRB.DoubleAttr.ObjVal);
        System.out.println("Optimal objective: " + objval);
      } else if (optimstatus == GRB.Status.INF_OR_UNBD) {
        System.out.println("Model is infeasible or unbounded");
        return;
      } else if (optimstatus == GRB.Status.INFEASIBLE) {
        System.out.println("Model is infeasible");
        return;
      } else if (optimstatus == GRB.Status.UNBOUNDED) {
        System.out.println("Model is unbounded");
        return;
      } else {
        System.out.println("Optimization was stopped with status = "
            + optimstatus);
        return;
      }

      /* Iterate over the solutions and compute the objectives */
      GRBVar[] vars = model.getVars();
      model.getEnv().set(GRB.IntParam.OutputFlag, 0);

      System.out.println();
      for (int k = 0; k < model.get(GRB.IntAttr.SolCount); ++k) {
        model.getEnv().set(GRB.IntParam.SolutionNumber, k);
        double objn = 0.0;

        for (int j = 0; j < vars.length; j++) {
          objn += vars[j].get(GRB.DoubleAttr.Obj)
              * vars[j].get(GRB.DoubleAttr.Xn);
        }

        System.out.println("Solution " + k + " has objective: " + objn);
      }
      System.out.println();
      model.getEnv().set(GRB.IntParam.OutputFlag, 1);

      /* Create a fixed model, turn off presolve and solve */

      GRBModel fixed = model.fixedModel();

      fixed.getEnv().set(GRB.IntParam.Presolve, 0);

      fixed.optimize();

      int foptimstatus = fixed.get(GRB.IntAttr.Status);

      if (foptimstatus != GRB.Status.OPTIMAL) {
        System.err.println("Error: fixed model isn't optimal");
        return;
      }

      double fobjval = fixed.get(GRB.DoubleAttr.ObjVal);

      if (Math.abs(fobjval - objval) > 1.0e-6 * (1.0 + Math.abs(objval))) {
        System.err.println("Error: objective values are different");
        return;
      }

      GRBVar[] fvars  = fixed.getVars();
      double[] x      = fixed.get(GRB.DoubleAttr.X, fvars);
      String[] vnames = fixed.get(GRB.StringAttr.VarName, fvars);

      for (int j = 0; j < fvars.length; j++) {
        if (x[j] != 0.0) {
          System.out.println(vnames[j] + " " + x[j]);
        }
      }

      // Dispose of models and environment
      fixed.dispose();
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". "
          + e.getMessage());
    }
  }
}
