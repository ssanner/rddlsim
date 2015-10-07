/* Copyright 2014, Gurobi Optimization, Inc. */

/* Assign workers to shifts; each worker may or may not be available on a
   particular day. If the problem cannot be solved, use IIS iteratively to
   find all conflicting constraints. */

using System;
using System.Collections.Generic;
using Gurobi;

class workforce2_cs
{
  static void Main()
  {
    try {

      // Sample data
      // Sets of days and workers
      string[] Shifts =
          new string[] { "Mon1", "Tue2", "Wed3", "Thu4", "Fri5", "Sat6",
              "Sun7", "Mon8", "Tue9", "Wed10", "Thu11", "Fri12", "Sat13",
              "Sun14" };
      string[] Workers =
          new string[] { "Amy", "Bob", "Cathy", "Dan", "Ed", "Fred", "Gu" };

      int nShifts = Shifts.Length;
      int nWorkers = Workers.Length;

      // Number of workers required for each shift
      double[] shiftRequirements =
          new double[] { 3, 2, 4, 4, 5, 6, 5, 2, 2, 3, 4, 6, 7, 5 };

      // Amount each worker is paid to work one shift
      double[] pay = new double[] { 10, 12, 10, 8, 8, 9, 11 };

      // Worker availability: 0 if the worker is unavailable for a shift
      double[,] availability =
          new double[,] { { 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1 },
              { 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0 },
              { 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
              { 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
              { 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1 },
              { 1, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1 },
              { 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 } };

      // Model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.Set(GRB.StringAttr.ModelName, "assignment");

      // Assignment variables: x[w][s] == 1 if worker w is assigned
      // to shift s. Since an assignment model always produces integer
      // solutions, we use continuous variables and solve as an LP.
      GRBVar[,] x = new GRBVar[nWorkers,nShifts];
      for (int w = 0; w < nWorkers; ++w) {
        for (int s = 0; s < nShifts; ++s) {
          x[w,s] =
              model.AddVar(0, availability[w,s], pay[w], GRB.CONTINUOUS,
                           Workers[w] + "." + Shifts[s]);
        }
      }

      // The objective is to minimize the total pay costs
      model.Set(GRB.IntAttr.ModelSense, 1);

      // Update model to integrate new variables
      model.Update();

      // Constraint: assign exactly shiftRequirements[s] workers
      // to each shift s
      for (int s = 0; s < nShifts; ++s) {
        GRBLinExpr lhs = 0.0;
        for (int w = 0; w < nWorkers; ++w)
          lhs.AddTerm(1.0, x[w, s]);
        model.AddConstr(lhs == shiftRequirements[s], Shifts[s]);
      }

      // Optimize
      model.Optimize();
      int status = model.Get(GRB.IntAttr.Status);
      if (status == GRB.Status.UNBOUNDED) {
        Console.WriteLine("The model cannot be solved "
            + "because it is unbounded");
        return;
      }
      if (status == GRB.Status.OPTIMAL) {
        Console.WriteLine("The optimal objective is " +
            model.Get(GRB.DoubleAttr.ObjVal));
        return;
      }
      if ((status != GRB.Status.INF_OR_UNBD) &&
          (status != GRB.Status.INFEASIBLE)) {
        Console.WriteLine("Optimization was stopped with status " + status);
        return;
      }

      // Do IIS
      Console.WriteLine("The model is infeasible; computing IIS");
      LinkedList<string> removed = new LinkedList<string>();

      // Loop until we reduce to a model that can be solved
      while (true) {
        model.ComputeIIS();
        Console.WriteLine("\nThe following constraint cannot be satisfied:");
        foreach (GRBConstr c in model.GetConstrs()) {
          if (c.Get(GRB.IntAttr.IISConstr) == 1) {
            Console.WriteLine(c.Get(GRB.StringAttr.ConstrName));
            // Remove a single constraint from the model
            removed.AddFirst(c.Get(GRB.StringAttr.ConstrName));
            model.Remove(c);
            break;
          }
        }

        Console.WriteLine();
        model.Optimize();
        status = model.Get(GRB.IntAttr.Status);

        if (status == GRB.Status.UNBOUNDED) {
          Console.WriteLine("The model cannot be solved "
              + "because it is unbounded");
          return;
        }
        if (status == GRB.Status.OPTIMAL) {
          break;
        }
        if ((status != GRB.Status.INF_OR_UNBD) &&
            (status != GRB.Status.INFEASIBLE)) {
          Console.WriteLine("Optimization was stopped with status " +
              status);
          return;
        }
      }

      Console.WriteLine("\nThe following constraints were removed "
          + "to get a feasible LP:");
      foreach (string s in removed) {
        Console.Write(s + " ");
      }
      Console.WriteLine();

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " +
          e.Message);
    }
  }
}
