/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"

int printSolution(GRBmodel* model, int nCategories, int nFoods);


int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  int       i, j;
  int      *cbeg, *cind, idx;
  double   *cval, *rhs;
  char     *sense;

  /* Nutrition guidelines, based on
     USDA Dietary Guidelines for Americans, 2005
     http://www.health.gov/DietaryGuidelines/dga2005/ */

  const int nCategories = 4;
  char *Categories[] =
    { "calories", "protein", "fat", "sodium" };
  double minNutrition[] = { 1800, 91, 0, 0 };
  double maxNutrition[] = { 2200, GRB_INFINITY, 65, 1779 };

  /* Set of foods */
  const int nFoods = 9;
  char* Foods[] =
    { "hamburger", "chicken", "hot dog", "fries",
      "macaroni", "pizza", "salad", "milk", "ice cream" };
  double cost[] =
    { 2.49, 2.89, 1.50, 1.89, 2.09, 1.99, 2.49, 0.89, 1.59 };

  /* Nutrition values for the foods */
  double nutritionValues[][4] = {
                                  { 410, 24, 26, 730 },
                                  { 420, 32, 10, 1190 },
                                  { 560, 20, 32, 1800 },
                                  { 380, 4, 19, 270 },
                                  { 320, 12, 10, 930 },
                                  { 320, 15, 12, 820 },
                                  { 320, 31, 12, 1230 },
                                  { 100, 8, 2.5, 125 },
                                  { 330, 8, 10, 180 }
                                };

  /* Create environment */
  error = GRBloadenv(&env, "diet.log");
  if (error) goto QUIT;

  /* Create initial model */
  error = GRBnewmodel(env, &model, "diet", nFoods + nCategories,
                      NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;

  /* Initialize decision variables for the foods to buy */
  for (j = 0; j < nFoods; ++j)
  {
    error = GRBsetdblattrelement(model, "Obj", j, cost[j]);
    if (error) goto QUIT;
    error = GRBsetstrattrelement(model, "VarName", j, Foods[j]);
    if (error) goto QUIT;
  }

  /* Initialize decision variables for the nutrition information,
     which we limit via bounds */
  for (j = 0; j < nCategories; ++j)
  {
    error = GRBsetdblattrelement(model, "LB", j + nFoods, minNutrition[j]);
    if (error) goto QUIT;
    error = GRBsetdblattrelement(model, "UB", j + nFoods, maxNutrition[j]);
    if (error) goto QUIT;
    error = GRBsetstrattrelement(model, "VarName", j + nFoods, Categories[j]);
    if (error) goto QUIT;
  }

  /* The objective is to minimize the costs */
  error = GRBsetintattr(model, "ModelSense", 1);
  if (error) goto QUIT;

  /* Nutrition constraints */
  cbeg = malloc(sizeof(int) * nCategories);
  if (!cbeg) goto QUIT;
  cind = malloc(sizeof(int) * nCategories * (nFoods + 1));
  if (!cind) goto QUIT;
  cval = malloc(sizeof(double) * nCategories * (nFoods + 1));
  if (!cval) goto QUIT;
  rhs = malloc(sizeof(double) * nCategories);
  if (!rhs) goto QUIT;
  sense = malloc(sizeof(char) * nCategories);
  if (!sense) goto QUIT;
  idx = 0;
  for (i = 0; i < nCategories; ++i)
  {
    cbeg[i] = idx;
    rhs[i] = 0.0;
    sense[i] = GRB_EQUAL;
    for (j = 0; j < nFoods; ++j)
    {
      cind[idx] = j;
      cval[idx++] = nutritionValues[j][i];
    }
    cind[idx] = nFoods + i;
    cval[idx++] = -1.0;
  }

  error = GRBaddconstrs(model, nCategories, idx, cbeg, cind, cval, sense,
                        rhs, Categories);
  if (error) goto QUIT;

  /* Solve */
  error = GRBoptimize(model);
  if (error) goto QUIT;
  error = printSolution(model, nCategories, nFoods);
  if (error) goto QUIT;

  printf("\nAdding constraint: at most 6 servings of dairy\n");
  cind[0] = 7;
  cval[0] = 1.0;
  cind[1] = 8;
  cval[1] = 1.0;
  error = GRBaddconstr(model, 2, cind, cval, GRB_LESS_EQUAL, 6.0,
                       "limit_dairy");
  if (error) goto QUIT;

  /* Solve */
  error = GRBoptimize(model);
  if (error) goto QUIT;
  error = printSolution(model, nCategories, nFoods);
  if (error) goto QUIT;



QUIT:

  /* Error reporting */

  if (error)
  {
    printf("ERROR: %s\n", GRBgeterrormsg(env));
    exit(1);
  }

  /* Free data */

  free(cbeg);
  free(cind);
  free(cval);
  free(rhs);
  free(sense);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}

int printSolution(GRBmodel* model, int nCategories, int nFoods)
{
  int error, status, i, j;
  double obj, x;
  char* vname;

  error = GRBgetintattr(model, "Status", &status);
  if (error) return error;
  if (status == GRB_OPTIMAL)
  {
    error = GRBgetdblattr(model, "ObjVal", &obj);
    if (error) return error;
    printf("\nCost: %f\n\nBuy:\n", obj);
    for (j = 0; j < nFoods; ++j)
    {
      error = GRBgetdblattrelement(model, "X", j, &x);
      if (error) return error;
      if (x > 0.0001)
      {
        error = GRBgetstrattrelement(model, "VarName", j, &vname);
        if (error) return error;
        printf("%s %f\n", vname, x);
      }
    }
    printf("\nNutrition:\n");
    for (i = 0; i < nCategories; ++i)
    {
      error = GRBgetdblattrelement(model, "X", i + nFoods, &x);
      if (error) return error;
      error = GRBgetstrattrelement(model, "VarName", i + nFoods, &vname);
      if (error) return error;
      printf("%s %f\n", vname, x);
    }
  }
  else
  {
    printf("No solution\n");
  }

  return 0;
}
