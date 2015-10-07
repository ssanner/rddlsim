/* Copyright 2014, Gurobi Optimization, Inc. */

// Solve a traveling salesman problem on a randomly generated set of
// points using lazy constraints.   The base MIP model only includes
// 'degree-2' constraints, requiring each node to have exactly
// two incident edges.  Solutions to this model may contain subtours -
// tours that don't visit every node.  The lazy constraint callback
// adds new constraints to cut them off.

import gurobi.*;

public class Tsp extends GRBCallback {
  private GRBVar[][] vars;

  public Tsp(GRBVar[][] xvars) {
    vars = xvars;
  }

  // Subtour elimination callback.  Whenever a feasible solution is found,
  // find the subtour that contains node 0, and add a subtour elimination
  // constraint if the tour doesn't visit every node.

  protected void callback() {
    try {
      if (where == GRB.CB_MIPSOL) {
        // Found an integer feasible solution - does it visit every node?
        int n = vars.length;
        int[] tour = findsubtour(getSolution(vars));

        if (tour.length < n) {
          // Add subtour elimination constraint
          GRBLinExpr expr = new GRBLinExpr();
          for (int i = 0; i < tour.length; i++)
            for (int j = i+1; j < tour.length; j++)
              expr.addTerm(1.0, vars[tour[i]][tour[j]]);
          addLazy(expr, GRB.LESS_EQUAL, tour.length-1);
        }
      }
    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
      e.printStackTrace();
    }
  }

  // Given an integer-feasible solution 'sol', return the smallest
  // sub-tour (as a list of node indices).

  protected static int[] findsubtour(double[][] sol)
  {
    int n = sol.length;
    boolean[] seen = new boolean[n];
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
          if (sol[node][i] > 0.5 && !seen[i]) {
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

    int result[] = new int[bestlen];
    for (i = 0; i < bestlen; i++)
      result[i] = tour[bestind+i];
    return result;
  }

  // Euclidean distance between points 'i' and 'j'

  protected static double distance(double[] x,
                                   double[] y,
                                   int      i,
                                   int      j) {
    double dx = x[i]-x[j];
    double dy = y[i]-y[j];
    return Math.sqrt(dx*dx+dy*dy);
  }

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Tsp ncities");
      System.exit(1);
    }

    int n = Integer.parseInt(args[0]);

    try {
      GRBEnv   env   = new GRBEnv();
      GRBModel model = new GRBModel(env);

      // Must set LazyConstraints parameter when using lazy constraints

      model.getEnv().set(GRB.IntParam.LazyConstraints, 1);

      double[] x = new double[n];
      double[] y = new double[n];

      for (int i = 0; i < n; i++) {
        x[i] = Math.random();
        y[i] = Math.random();
      }

      // Create variables

      GRBVar[][] vars = new GRBVar[n][n];

      for (int i = 0; i < n; i++)
        for (int j = 0; j <= i; j++) {
          vars[i][j] = model.addVar(0.0, 1.0, distance(x, y, i, j),
                                    GRB.BINARY,
                                  "x"+String.valueOf(i)+"_"+String.valueOf(j));
          vars[j][i] = vars[i][j];
        }

      // Integrate variables

      model.update();

      // Degree-2 constraints

      for (int i = 0; i < n; i++) {
        GRBLinExpr expr = new GRBLinExpr();
        for (int j = 0; j < n; j++)
          expr.addTerm(1.0, vars[i][j]);
        model.addConstr(expr, GRB.EQUAL, 2.0, "deg2_"+String.valueOf(i));
      }

      // Forbid edge from node back to itself

      for (int i = 0; i < n; i++)
        vars[i][i].set(GRB.DoubleAttr.UB, 0.0);

      model.setCallback(new Tsp(vars));
      model.optimize();

      if (model.get(GRB.IntAttr.SolCount) > 0) {
        int[] tour = findsubtour(model.get(GRB.DoubleAttr.X, vars));

        System.out.print("Tour: ");
        for (int i = 0; i < tour.length; i++)
          System.out.print(String.valueOf(tour[i]) + " ");
        System.out.println();
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
      e.printStackTrace();
    }
  }
}
