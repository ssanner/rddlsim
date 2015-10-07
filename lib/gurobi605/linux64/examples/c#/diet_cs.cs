/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */

using System;
using Gurobi;

class diet_cs
{
  static void Main()
  {
    try {

      // Nutrition guidelines, based on
      // USDA Dietary Guidelines for Americans, 2005
      // http://www.health.gov/DietaryGuidelines/dga2005/
      string[] Categories =
          new string[] { "calories", "protein", "fat", "sodium" };
      int nCategories = Categories.Length;
      double[] minNutrition = new double[] { 1800, 91, 0, 0 };
      double[] maxNutrition = new double[] { 2200, GRB.INFINITY, 65, 1779 };

      // Set of foods
      string[] Foods =
          new string[] { "hamburger", "chicken", "hot dog", "fries",
              "macaroni", "pizza", "salad", "milk", "ice cream" };
      int nFoods = Foods.Length;
      double[] cost =
          new double[] { 2.49, 2.89, 1.50, 1.89, 2.09, 1.99, 2.49, 0.89,
              1.59 };

      // Nutrition values for the foods
      double[,] nutritionValues = new double[,] {
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
      model.Set(GRB.StringAttr.ModelName, "diet");

      // Create decision variables for the nutrition information,
      // which we limit via bounds
      GRBVar[] nutrition = new GRBVar[nCategories];
      for (int i = 0; i < nCategories; ++i) {
        nutrition[i] =
            model.AddVar(minNutrition[i], maxNutrition[i], 0, GRB.CONTINUOUS,
                         Categories[i]);
      }

      // Create decision variables for the foods to buy
      GRBVar[] buy = new GRBVar[nFoods];
      for (int j = 0; j < nFoods; ++j) {
        buy[j] =
            model.AddVar(0, GRB.INFINITY, cost[j], GRB.CONTINUOUS, Foods[j]);
      }

      // The objective is to minimize the costs
      model.Set(GRB.IntAttr.ModelSense, 1);

      // Update model to integrate new variables
      model.Update();

      // Nutrition constraints
      for (int i = 0; i < nCategories; ++i) {
        GRBLinExpr ntot = 0.0;
        for (int j = 0; j < nFoods; ++j)
          ntot.AddTerm(nutritionValues[j,i], buy[j]);
        model.AddConstr(ntot == nutrition[i], Categories[i]);
      }

      // Solve
      model.Optimize();
      PrintSolution(model, buy, nutrition);

      Console.WriteLine("\nAdding constraint: at most 6 servings of dairy");
      model.AddConstr(buy[7] + buy[8] <= 6.0, "limit_dairy");

      // Solve
      model.Optimize();
      PrintSolution(model, buy, nutrition);

      // Dispose of model and env
      model.Dispose();
      env.Dispose();

    } catch (GRBException e) {
      Console.WriteLine("Error code: " + e.ErrorCode + ". " +
          e.Message);
    }
  }

  private static void PrintSolution(GRBModel model, GRBVar[] buy,
                                    GRBVar[] nutrition) {
    if (model.Get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
      Console.WriteLine("\nCost: " + model.Get(GRB.DoubleAttr.ObjVal));
      Console.WriteLine("\nBuy:");
      for (int j = 0; j < buy.Length; ++j) {
        if (buy[j].Get(GRB.DoubleAttr.X) > 0.0001) {
          Console.WriteLine(buy[j].Get(GRB.StringAttr.VarName) + " " +
              buy[j].Get(GRB.DoubleAttr.X));
        }
      }
      Console.WriteLine("\nNutrition:");
      for (int i = 0; i < nutrition.Length; ++i) {
        Console.WriteLine(nutrition[i].Get(GRB.StringAttr.VarName) + " " +
            nutrition[i].Get(GRB.DoubleAttr.X));
      }
    } else {
      Console.WriteLine("No solution");
    }
  }
}
