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

using System;
using System.IO;
using Gurobi;

class sudoku_cs
{
  static void Main(string[] args)
  {
    int n = 9;
    int s = 3;

    if (args.Length < 1) {
      Console.Out.WriteLine("Usage: sudoku_cs filename");
      return;
    }

    try {
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);

      // Create 3-D array of model variables

      GRBVar[,,] vars = new GRBVar[n,n,n];

      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          for (int v = 0; v < n; v++) {
            string st = "G_" + i.ToString() + "_" + j.ToString()
                             + "_" + v.ToString();
            vars[i,j,v] = model.AddVar(0.0, 1.0, 0.0, GRB.BINARY, st);
          }
        }
      }

      // Integrate variables into model

      model.Update();

      // Add constraints

      GRBLinExpr expr;

      // Each cell must take one value

      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          expr = 0.0;
          for (int v = 0; v < n; v++)
            expr.AddTerm(1.0, vars[i,j,v]);
          string st = "V_" + i.ToString() + "_" + j.ToString();
          model.AddConstr(expr == 1.0, st);
        }
      }

      // Each value appears once per row

      for (int i = 0; i < n; i++) {
        for (int v = 0; v < n; v++) {
          expr = 0.0;
          for (int j = 0; j < n; j++)
            expr.AddTerm(1.0, vars[i,j,v]);
          string st = "R_" + i.ToString() + "_" + v.ToString();
          model.AddConstr(expr == 1.0, st);
        }
      }

      // Each value appears once per column

      for (int j = 0; j < n; j++) {
        for (int v = 0; v < n; v++) {
          expr = 0.0;
          for (int i = 0; i < n; i++)
            expr.AddTerm(1.0, vars[i,j,v]);
          string st = "C_" + j.ToString() + "_" + v.ToString();
          model.AddConstr(expr == 1.0, st);
        }
      }

      // Each value appears once per sub-grid

      for (int v = 0; v < n; v++) {
        for (int i0 = 0; i0 < s; i0++) {
          for (int j0 = 0; j0 < s; j0++) {
            expr = 0.0;
            for (int i1 = 0; i1 < s; i1++) {
              for (int j1 = 0; j1 < s; j1++) {
                expr.AddTerm(1.0, vars[i0*s+i1,j0*s+j1,v]);
              }
            }
            string st = "Sub_" + v.ToString() + "_" + i0.ToString()
                               + "_" + j0.ToString();
            model.AddConstr(expr == 1.0, st);
          }
        }
      }

      // Update model

      model.Update();

      // Fix variables associated with pre-specified cells

      StreamReader sr = File.OpenText(args[0]);

      for (int i = 0; i < n; i++) {
        string input = sr.ReadLine();
        for (int j = 0; j < n; j++) {
          int val = (int) input[j] - 48 - 1; // 0-based

          if (val >= 0)
            vars[i,j,val].Set(GRB.DoubleAttr.LB, 1.0);
        }
      }

      // Optimize model

      model.Optimize();

      // Write model to file
      model.Write("sudoku.lp");

      double[,,] x = model.Get(GRB.DoubleAttr.X, vars);

      Console.WriteLine();
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          for (int v = 0; v < n; v++) {
            if (x[i,j,v] > 0.5) {
              Console.Write(v+1);
            }
          }
        }
        Console.WriteLine();
      }

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
