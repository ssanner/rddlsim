/* Copyright 2014, Gurobi Optimization, Inc. */

/* Facility location: a company currently ships its product from 5 plants
   to 4 warehouses. It is considering closing some plants to reduce
   costs. What plant(s) should the company close, in order to minimize
   transportation and fixed costs?

   Based on an example from Frontline Systems:
   http://www.solver.com/disfacility.htm
   Used with permission.
 */

import gurobi.*;

public class Facility {

  public static void main(String[] args) {
    try {

      // Warehouse demand in thousands of units
      double Demand[] = new double[] { 15, 18, 14, 20 };

      // Plant capacity in thousands of units
      double Capacity[] = new double[] { 20, 22, 17, 19, 18 };

      // Fixed costs for each plant
      double FixedCosts[] =
          new double[] { 12000, 15000, 17000, 13000, 16000 };

      // Transportation costs per thousand units
      double TransCosts[][] =
          new double[][] { { 4000, 2000, 3000, 2500, 4500 },
              { 2500, 2600, 3400, 3000, 4000 },
              { 1200, 1800, 2600, 4100, 3000 },
              { 2200, 2600, 3100, 3700, 3200 } };

      // Number of plants and warehouses
      int nPlants = Capacity.length;
      int nWarehouses = Demand.length;

      // Model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.set(GRB.StringAttr.ModelName, "facility");

      // Plant open decision variables: open[p] == 1 if plant p is open.
      GRBVar[] open = new GRBVar[nPlants];
      for (int p = 0; p < nPlants; ++p) {
        open[p] = model.addVar(0, 1, FixedCosts[p], GRB.BINARY, "Open" + p);
      }

      // Transportation decision variables: how much to transport from
      // a plant p to a warehouse w
      GRBVar[][] transport = new GRBVar[nWarehouses][nPlants];
      for (int w = 0; w < nWarehouses; ++w) {
        for (int p = 0; p < nPlants; ++p) {
          transport[w][p] =
              model.addVar(0, GRB.INFINITY, TransCosts[w][p], GRB.CONTINUOUS,
                           "Trans" + p + "." + w);
        }
      }

      // The objective is to minimize the total fixed and variable costs
      model.set(GRB.IntAttr.ModelSense, 1);

      // Update model to integrate new variables
      model.update();

      // Production constraints
      // Note that the right-hand limit sets the production to zero if
      // the plant is closed
      for (int p = 0; p < nPlants; ++p) {
        GRBLinExpr ptot = new GRBLinExpr();
        for (int w = 0; w < nWarehouses; ++w) {
          ptot.addTerm(1.0, transport[w][p]);
        }
        GRBLinExpr limit = new GRBLinExpr();
        limit.addTerm(Capacity[p], open[p]);
        model.addConstr(ptot, GRB.LESS_EQUAL, limit, "Capacity" + p);
      }

      // Demand constraints
      for (int w = 0; w < nWarehouses; ++w) {
        GRBLinExpr dtot = new GRBLinExpr();
        for (int p = 0; p < nPlants; ++p) {
          dtot.addTerm(1.0, transport[w][p]);
        }
        model.addConstr(dtot, GRB.EQUAL, Demand[w], "Demand" + w);
      }

      // Guess at the starting point: close the plant with the highest
      // fixed costs; open all others

      // First, open all plants
      for (int p = 0; p < nPlants; ++p) {
        open[p].set(GRB.DoubleAttr.Start, 1.0);
      }

      // Now close the plant with the highest fixed cost
      System.out.println("Initial guess:");
      double maxFixed = -GRB.INFINITY;
      for (int p = 0; p < nPlants; ++p) {
        if (FixedCosts[p] > maxFixed) {
          maxFixed = FixedCosts[p];
        }
      }
      for (int p = 0; p < nPlants; ++p) {
        if (FixedCosts[p] == maxFixed) {
          open[p].set(GRB.DoubleAttr.Start, 0.0);
          System.out.println("Closing plant " + p + "\n");
          break;
        }
      }

      // Use barrier to solve root relaxation
      model.getEnv().set(GRB.IntParam.Method, GRB.METHOD_BARRIER);

      // Solve
      model.optimize();

      // Print solution
      System.out.println("\nTOTAL COSTS: " + model.get(GRB.DoubleAttr.ObjVal));
      System.out.println("SOLUTION:");
      for (int p = 0; p < nPlants; ++p) {
        if (open[p].get(GRB.DoubleAttr.X) == 1.0) {
          System.out.println("Plant " + p + " open:");
          for (int w = 0; w < nWarehouses; ++w) {
            if (transport[w][p].get(GRB.DoubleAttr.X) > 0.0001) {
              System.out.println("  Transport " +
                  transport[w][p].get(GRB.DoubleAttr.X) +
                  " units to warehouse " + w);
            }
          }
        } else {
          System.out.println("Plant " + p + " closed!");
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
