/* Copyright 2014, Gurobi Optimization, Inc. */

// Solve a traveling salesman problem on a randomly generated set of
// points using lazy constraints.   The base MIP model only includes
// 'degree-2' constraints, requiring each node to have exactly
// two incident edges.  Solutions to this model may contain subtours -
// tours that don't visit every node.  The lazy constraint callback
// adds new constraints to cut them off.

using System;
using Gurobi;

class tsp_cs : GRBCallback {
  private GRBVar[,] vars;

  public tsp_cs(GRBVar[,] xvars) {
    vars = xvars;
  }

  // Subtour elimination callback.  Whenever a feasible solution is found,
  // find the smallest subtour, and add a subtour elimination
  // constraint if the tour doesn't visit every node.

  protected override void Callback() {
    try {
      if (where == GRB.Callback.MIPSOL) {
        // Found an integer feasible solution - does it visit every node?

        int n = vars.GetLength(0);
        int[] tour = findsubtour(GetSolution(vars));

        if (tour.Length < n) {
          // Add subtour elimination constraint
          GRBLinExpr expr = 0;
          for (int i = 0; i < tour.Length; i++)
            for (int j = i+1; j < tour.Length; j++)
              expr.AddTerm(1.0, vars[tour[i], tour[j]]);
          AddLazy(expr <= tour.Length-1);
        }
      }
    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
      Console.WriteLine(e.StackTrace);
    }
  }

  // Given an integer-feasible solution 'sol', return the smallest
  // sub-tour (as a list of node indices).

  protected static int[] findsubtour(double[,] sol)
  {
    int n = sol.GetLength(0);
    bool[] seen = new bool[n];
    int[] tour = new int[n];
    int bestind, bestlen;
    int i, node, len, start;

    for (i = 0; i < n; i++)
      seen[i] = false;

    start = 0;
    bestlen = n+1;
    bestind = -1;
    node = 0;
    while (start < n) {
      for (node = 0; node < n; node++)
        if (!seen[node])
          break;
      if (node == n)
        break;
      for (len = 0; len < n; len++) {
        tour[start+len] = node;
        seen[node] = true;
        for (i = 0; i < n; i++) {
          if (sol[node, i] > 0.5 && !seen[i]) {
            node = i;
            break;
          }
        }
        if (i == n) {
          len++;
          if (len < bestlen) {
            bestlen = len;
            bestind = start;
          }
          start += len;
          break;
        }
      }
    }

    for (i = 0; i < bestlen; i++)
      tour[i] = tour[bestind+i];
    System.Array.Resize(ref tour, bestlen);

    return tour;
  }

  // Euclidean distance between points 'i' and 'j'

  protected static double distance(double[] x,
                                   double[] y,
                                   int      i,
                                   int      j) {
    double dx = x[i]-x[j];
    double dy = y[i]-y[j];
    return Math.Sqrt(dx*dx+dy*dy);
  }

  public static void Main(String[] args) {

    if (args.Length < 1) {
      Console.WriteLine("Usage: tsp_cs nnodes");
      return;
    }

    int n = Convert.ToInt32(args[0]);

    try {
      GRBEnv   env   = new GRBEnv();
      GRBModel model = new GRBModel(env);

      // Must set LazyConstraints parameter when using lazy constraints

      model.GetEnv().Set(GRB.IntParam.LazyConstraints, 1);

      double[] x = new double[n];
      double[] y = new double[n];

      Random r = new Random();
      for (int i = 0; i < n; i++) {
        x[i] = r.NextDouble();
        y[i] = r.NextDouble();
      }

      // Create variables

      GRBVar[,] vars = new GRBVar[n, n];

      for (int i = 0; i < n; i++) {
        for (int j = 0; j <= i; j++) {
          vars[i, j] = model.AddVar(0.0, 1.0, distance(x, y, i, j),
                                    GRB.BINARY, "x"+i+"_"+j);
          vars[j, i] = vars[i, j];
        }
      }

      // Integrate variables

      model.Update();

      // Degree-2 constraints

      for (int i = 0; i < n; i++) {
        GRBLinExpr expr = 0;
        for (int j = 0; j < n; j++)
          expr.AddTerm(1.0, vars[i, j]);
        model.AddConstr(expr == 2.0, "deg2_"+i);
      }

      // Forbid edge from node back to itself

      for (int i = 0; i < n; i++)
        vars[i, i].Set(GRB.DoubleAttr.UB, 0.0);

      model.SetCallback(new tsp_cs(vars));
      model.Optimize();

      if (model.Get(GRB.IntAttr.SolCount) > 0) {
        int[] tour = findsubtour(model.Get(GRB.DoubleAttr.X, vars));

        Console.Write("Tour: ");
        for (int i = 0; i < tour.Length; i++)
          Console.Write(tour[i] + " ");
        Console.WriteLine();
      }

      // Dispose of model and environment
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
      Console.WriteLine(e.StackTrace);
    }
  }
}
