/* Copyright 2014, Gurobi Optimization, Inc. */

/* Implement a simple MIP heuristic.  Relax the model,
 sort variables based on fractionality, and fix the 25% of
 the fractional variables that are closest to integer variables.
 Repeat until either the relaxation is integer feasible or
 linearly infeasible. */

import gurobi.*;
import java.util.*;

public class Fixanddive {
  public static void main(String[] args) {

    // Comparison class used to sort variable list based on relaxation
    // fractionality

    class FractionalCompare implements Comparator<GRBVar> {
      public int compare(GRBVar v1, GRBVar v2) {
        try {
          double sol1 = Math.abs(v1.get(GRB.DoubleAttr.X));
          double sol2 = Math.abs(v2.get(GRB.DoubleAttr.X));
          double frac1 = Math.abs(sol1 - Math.floor(sol1 + 0.5));
          double frac2 = Math.abs(sol2 - Math.floor(sol2 + 0.5));
          if (frac1 < frac2) {
            return -1;
          } else if (frac1 == frac2) {
            return 0;
          } else {
            return 1;
          }
        } catch (GRBException e) {
          System.out.println("Error code: " + e.getErrorCode() + ". " +
              e.getMessage());
        }
        return 0;
      }
    }

    if (args.length < 1) {
      System.out.println("Usage: java Fixanddive filename");
      System.exit(1);
    }

    try {
      // Read model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env, args[0]);

      // Collect integer variables and relax them
      ArrayList<GRBVar> intvars = new ArrayList<GRBVar>();
      for (GRBVar v : model.getVars()) {
        if (v.get(GRB.CharAttr.VType) != GRB.CONTINUOUS) {
          intvars.add(v);
          v.set(GRB.CharAttr.VType, GRB.CONTINUOUS);
        }
      }

      model.getEnv().set(GRB.IntParam.OutputFlag, 0);
      model.optimize();

      // Perform multiple iterations. In each iteration, identify the first
      // quartile of integer variables that are closest to an integer value
      // in the relaxation, fix them to the nearest integer, and repeat.

      for (int iter = 0; iter < 1000; ++iter) {

        // create a list of fractional variables, sorted in order of
        // increasing distance from the relaxation solution to the nearest
        // integer value

        ArrayList<GRBVar> fractional = new ArrayList<GRBVar>();
        for (GRBVar v : intvars) {
          double sol = Math.abs(v.get(GRB.DoubleAttr.X));
          if (Math.abs(sol - Math.floor(sol + 0.5)) > 1e-5) {
            fractional.add(v);
          }
        }

        System.out.println("Iteration " + iter + ", obj " +
            model.get(GRB.DoubleAttr.ObjVal) + ", fractional " +
            fractional.size());

        if (fractional.size() == 0) {
          System.out.println("Found feasible solution - objective " +
              model.get(GRB.DoubleAttr.ObjVal));
          break;
        }

        // Fix the first quartile to the nearest integer value

        Collections.sort(fractional, new FractionalCompare());
        int nfix = Math.max(fractional.size() / 4, 1);
        for (int i = 0; i < nfix; ++i) {
          GRBVar v = fractional.get(i);
          double fixval = Math.floor(v.get(GRB.DoubleAttr.X) + 0.5);
          v.set(GRB.DoubleAttr.LB, fixval);
          v.set(GRB.DoubleAttr.UB, fixval);
          System.out.println("  Fix " + v.get(GRB.StringAttr.VarName) +
              " to " + fixval + " ( rel " + v.get(GRB.DoubleAttr.X) + " )");
        }

        model.optimize();

        // Check optimization result

        if (model.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
          System.out.println("Relaxation is infeasible");
          break;
        }
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }

}
