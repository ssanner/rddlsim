/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */

import gurobi.*;

public class Diet {

  public static void main(String[] args) {
    try {

      // Nutrition guidelines, based on
      // USDA Dietary Guidelines for Americans, 2005
      // http://www.health.gov/DietaryGuidelines/dga2005/
      String Categories[] =
          new String[] { "calories", "protein", "fat", "sodium" };
      int nCategories = Categories.length;
      double minNutrition[] = new double[] { 1800, 91, 0, 0 };
      double maxNutrition[] = new double[] { 2200, GRB.INFINITY, 65, 1779 };

      // Set of foods
      String Foods[] =
          new String[] { "hamburger", "chicken", "hot dog", "fries",
              "macaroni", "pizza", "salad", "milk", "ice cream" };
      int nFoods = Foods.length;
      double cost[] =
          new double[] { 2.49, 2.89, 1.50, 1.89, 2.09, 1.99, 2.49, 0.89,
              1.59 };

      // Nutrition values for the foods
      double nutritionValues[][] = new double[][] {
          { 410, 24, 26, 730 },   // hamburger
          { 420, 32, 10, 1190 },  // chicken
          { 560, 20, 32, 1800 },  // hot dog
          { 380, 4, 19, 270 },    // fries
          { 320, 12, 10, 930 },   // macaroni
          { 320, 15, 12, 820 },   // pizza
          { 320, 31, 12, 1230 },  // salad
          { 100, 8, 2.5, 125 },   // milk
          { 330, 8, 10, 180 }     // ice cream
          };

      // Model
      GRBEnv env = new GRBEnv();
      GRBModel model = new GRBModel(env);
      model.set(GRB.StringAttr.ModelName, "diet");

      // Create decision variables for the nutrition information,
      // which we limit via bounds
      GRBVar[] nutrition = new GRBVar[nCategories];
      for (int i = 0; i < nCategories; ++i) {
        nutrition[i] =
            model.addVar(minNutrition[i], maxNutrition[i], 0, GRB.CONTINUOUS,
                         Categories[i]);
      }

      // Create decision variables for the foods to buy
      GRBVar[] buy = new GRBVar[nFoods];
      for (int j = 0; j < nFoods; ++j) {
        buy[j] =
            model.addVar(0, GRB.INFINITY, cost[j], GRB.CONTINUOUS, Foods[j]);
      }

      // The objective is to minimize the costs
      model.set(GRB.IntAttr.ModelSense, 1);

      // Update model to integrate new variables
      model.update();

      // Nutrition constraints
      for (int i = 0; i < nCategories; ++i) {
        GRBLinExpr ntot = new GRBLinExpr();
        for (int j = 0; j < nFoods; ++j) {
          ntot.addTerm(nutritionValues[j][i], buy[j]);
        }
        model.addConstr(ntot, GRB.EQUAL, nutrition[i], Categories[i]);
      }

      // Solve
      model.optimize();
      printSolution(model, buy, nutrition);

      System.out.println("\nAdding constraint: at most 6 servings of dairy");
      GRBLinExpr lhs = new GRBLinExpr();
      lhs.addTerm(1.0, buy[7]);
      lhs.addTerm(1.0, buy[8]);
      model.addConstr(lhs, GRB.LESS_EQUAL, 6.0, "limit_dairy");

      // Solve
      model.optimize();
      printSolution(model, buy, nutrition);

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
          e.getMessage());
    }
  }

  private static void printSolution(GRBModel model, GRBVar[] buy,
                                    GRBVar[] nutrition) throws GRBException {
    if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
      System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
      System.out.println("\nBuy:");
      for (int j = 0; j < buy.length; ++j) {
        if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
          System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +
              buy[j].get(GRB.DoubleAttr.X));
        }
      }
      System.out.println("\nNutrition:");
      for (int i = 0; i < nutrition.length; ++i) {
        System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " +
            nutrition[i].get(GRB.DoubleAttr.X));
      }
    } else {
      System.out.println("No solution");
    }
  }
}
