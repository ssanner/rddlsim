/* Copyright 2014, Gurobi Optimization, Inc. */

/* Facility location: a company currently ships its product from 5 plants
   to 4 warehouses. It is considering closing some plants to reduce
   costs. What plant(s) should the company close, in order to minimize
   transportation and fixed costs?

   Based on an example from Frontline Systems:
   http://www.solver.com/disfacility.htm
   Used with permission.
 */

using System;
using Gurobi;

class facility_cs
{
  static void Main()
  {
    try {

      // Warehouse demand in thousands of units
      double[] Demand = new double[] { 15, 18, 14, 20 };

      // Plant capacity in thousands of units
      double[] Capacity = new double[] { 20, 22, 17, 19, 18 };

      // Fixed costs for each plant
      double[] FixedCosts =
          new double[] { 12000, 15000, 17000, 13000, 16000 };

      // Transportation costs per thousand units
      double[,] TransCosts =
          new double[,] { { 4000, 2000, 3000, 2500, 4500 },
              { 2500, 2600, 3400, 3000, 4000 },
              { 1200, 1800, 2600, 4100, 3000 },
              { 2200, 2600, 3100, 3700, 3200 } };

      // Number of plants and warehouses
      int nPlants = Capacity.Length;
      int nWarehouses = Demand.Length;

      // Model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.Set(GRB.StringAttr.ModelName, "facility");

      // Plant open decision variables: open[p] == 1 if plant p is open.
      GRBVar[] open = new GRBVar[nPlants];
      for (int p = 0; p < nPlants; ++p) {
        open[p] = model.AddVar(0, 1, FixedCosts[p], GRB.BINARY, "Open" + p);
      }

      // Transportation decision variables: how much to transport from
      // a plant p to a warehouse w
      GRBVar[,] transport = new GRBVar[nWarehouses,nPlants];
      for (int w = 0; w < nWarehouses; ++w) {
        for (int p = 0; p < nPlants; ++p) {
          transport[w,p] =
              model.AddVar(0, GRB.INFINITY, TransCosts[w,p], GRB.CONTINUOUS,
                           "Trans" + p + "." + w);
        }
      }

      // The objective is to minimize the total fixed and variable costs
      model.Set(GRB.IntAttr.ModelSense, 1);

      // Update model to integrate new variables
      model.Update();

      // Production constraints
      // Note that the right-hand limit sets the production to zero if
      // the plant is closed
      for (int p = 0; p < nPlants; ++p) {
        GRBLinExpr ptot = 0.0;
        for (int w = 0; w < nWarehouses; ++w)
          ptot.AddTerm(1.0, transport[w,p]);
        model.AddConstr(ptot <= Capacity[p] * open[p], "Capacity" + p);
      }

      // Demand constraints
      for (int w = 0; w < nWarehouses; ++w) {
        GRBLinExpr dtot = 0.0;
        for (int p = 0; p < nPlants; ++p)
          dtot.AddTerm(1.0, transport[w,p]);
        model.AddConstr(dtot == Demand[w], "Demand" + w);
      }

      // Guess at the starting point: close the plant with the highest
      // fixed costs; open all others

      // First, open all plants
      for (int p = 0; p < nPlants; ++p) {
        open[p].Set(GRB.DoubleAttr.Start, 1.0);
      }

      // Now close the plant with the highest fixed cost
      Console.WriteLine("Initial guess:");
      double maxFixed = -GRB.INFINITY;
      for (int p = 0; p < nPlants; ++p) {
        if (FixedCosts[p] > maxFixed) {
          maxFixed = FixedCosts[p];
        }
      }
      for (int p = 0; p < nPlants; ++p) {
        if (FixedCosts[p] == maxFixed) {
          open[p].Set(GRB.DoubleAttr.Start, 0.0);
          Console.WriteLine("Closing plant " + p + "\n");
          break;
        }
      }

      // Use barrier to solve root relaxation
      model.GetEnv().Set(GRB.IntParam.Method, GRB.METHOD_BARRIER);

      // Solve
      model.Optimize();

      // Print solution
      Console.WriteLine("\nTOTAL COSTS: " + model.Get(GRB.DoubleAttr.ObjVal));
      Console.WriteLine("SOLUTION:");
      for (int p = 0; p < nPlants; ++p) {
        if (open[p].Get(GRB.DoubleAttr.X) == 1.0) {
          Console.WriteLine("Plant " + p + " open:");
          for (int w = 0; w < nWarehouses; ++w) {
            if (transport[w,p].Get(GRB.DoubleAttr.X) > 0.0001) {
              Console.WriteLine("  Transport " +
                  transport[w,p].Get(GRB.DoubleAttr.X) +
                  " units to warehouse " + w);
            }
          }
        } else {
          Console.WriteLine("Plant " + p + " closed!");
        }
      }

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " + e.Message);
    }
  }
}
