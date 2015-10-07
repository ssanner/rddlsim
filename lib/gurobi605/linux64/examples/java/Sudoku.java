/* Copyright 2014, Gurobi Optimization, Inc. */
/*
  Sudoku example.

  The Sudoku board is a 9x9 grid, which is further divided into a 3x3 grid
  of 3x3 grids.  Each cell in the grid must take a value from 0 to 9.
  No two grid cells in the same row, column, or 3x3 subgrid may take the
  same value.

  In the MIP formulation, binary variables x[i,j,v] indicate whether
  cell <i,j> takes value 'v'.  The constraints are as follows:
    1. Each cell must take exactly one value (sum_v x[i,j,v] = 1)
    2. Each value is used exactly once per row (sum_i x[i,j,v] = 1)
    3. Each value is used exactly once per column (sum_j x[i,j,v] = 1)
    4. Each value is used exactly once per 3x3 subgrid (sum_grid x[i,j,v] = 1)

  Input datasets for this example can be found in examples/data/sudoku*.
*/

import gurobi.*;
import java.io.*;

public class Sudoku {
  public static void main(String[] args) {
    int n = 9;
    int s = 3;

    if (args.length < 1) {
      System.out.println("Usage: java Sudoku filename");
      System.exit(1);
    }

    try {
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);

      // Create 3-D array of model variables

      GRBVar[][][] vars = new GRBVar[n][n][n];

      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          for (int v = 0; v < n; v++) {
            String st = "G_" + String.valueOf(i) + "_" + String.valueOf(j)
                             + "_" + String.valueOf(v);
            vars[i][j][v] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
          }
        }
      }

      // Integrate variables into model

      model.update();

      // Add constraints

      GRBLinExpr expr;

      // Each cell must take one value

      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          expr = new GRBLinExpr();
          expr.addTerms(null, vars[i][j]);
          String st = "V_" + String.valueOf(i) + "_" + String.valueOf(j);
          model.addConstr(expr, GRB.EQUAL, 1.0, st);
        }
      }

      // Each value appears once per row

      for (int i = 0; i < n; i++) {
        for (int v = 0; v < n; v++) {
          expr = new GRBLinExpr();
          for (int j = 0; j < n; j++)
            expr.addTerm(1.0, vars[i][j][v]);
          String st = "R_" + String.valueOf(i) + "_" + String.valueOf(v);
          model.addConstr(expr, GRB.EQUAL, 1.0, st);
        }
      }

      // Each value appears once per column

      for (int j = 0; j < n; j++) {
        for (int v = 0; v < n; v++) {
          expr = new GRBLinExpr();
          for (int i = 0; i < n; i++)
            expr.addTerm(1.0, vars[i][j][v]);
          String st = "C_" + String.valueOf(j) + "_" + String.valueOf(v);
          model.addConstr(expr, GRB.EQUAL, 1.0, st);
        }
      }

      // Each value appears once per sub-grid

      for (int v = 0; v < n; v++) {
        for (int i0 = 0; i0 < s; i0++) {
          for (int j0 = 0; j0 < s; j0++) {
            expr = new GRBLinExpr();
            for (int i1 = 0; i1 < s; i1++) {
              for (int j1 = 0; j1 < s; j1++) {
                expr.addTerm(1.0, vars[i0*s+i1][j0*s+j1][v]);
              }
            }
            String st = "Sub_" + String.valueOf(v) + "_" + String.valueOf(i0)
                               + "_" + String.valueOf(j0);
            model.addConstr(expr, GRB.EQUAL, 1.0, st);
          }
        }
      }

      // Update model

      model.update();

      // Fix variables associated with pre-specified cells

      File file = new File(args[0]);
      FileInputStream fis = new FileInputStream(file);
      byte[] input = new byte[n];

      for (int i = 0; i < n; i++) {
        fis.read(input);
        for (int j = 0; j < n; j++) {
          int val = (int) input[j] - 48 - 1; // 0-based

          if (val >= 0)
            vars[i][j][val].set(GRB.DoubleAttr.LB, 1.0);
        }
        // read the endline byte
        fis.read();
      }

      // Optimize model

      model.optimize();

      // Write model to file
      model.write("sudoku.lp");

      double[][][] x = model.get(GRB.DoubleAttr.X, vars);

      System.out.println();
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          for (int v = 0; v < n; v++) {
            if (x[i][j][v] > 0.5) {
              System.out.print(v+1);
            }
          }
        }
        System.out.println();
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    } catch (IOException e) {
      System.out.println("IO Error");
    }
  }
}
