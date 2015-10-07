/* Copyright 2014, Gurobi Optimization, Inc. */

/* This example creates a very simple Special Ordered Set (SOS) model.
   The model consists of 3 continuous variables, no linear constraints,
   and a pair of SOS constraints of type 1. */

import gurobi.*;

public class Sos {
  public static void main(String[] args) {
    try {
      GRBEnv env = new GRBEnv();

      GRBModel model = new GRBModel(env);

      // Create variables

      double ub[]    = {1, 1, 2};
      double obj[]   = {-2, -1, -1};
      String names[] = {"x0", "x1", "x2"};

      GRBVar[] x = model.addVars(null, ub, obj, null, names);

      // Integrate new variables

      model.update();

      // Add first SOS1: x0=0 or x1=0

      GRBVar sosv1[]  = {x[0], x[1]};
      double soswt1[] = {1, 2};

      model.addSOS(sosv1, soswt1, GRB.SOS_TYPE1);

      // Add second SOS1: x0=0 or x2=0

      GRBVar sosv2[]  = {x[0], x[2]};
      double soswt2[] = {1, 2};

      model.addSOS(sosv2, soswt2, GRB.SOS_TYPE1);

      // Optimize model

      model.optimize();

      for (int i = 0; i < 3; i++)
        System.out.println(x[i].get(GRB.StringAttr.VarName) + " "
                           + x[i].get(GRB.DoubleAttr.X));

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }
}
