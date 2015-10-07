/* Copyright 2014, Gurobi Optimization, Inc. */

/* Facility location: a company currently ships its product from 5 plants
   to 4 warehouses. It is considering closing some plants to reduce
   costs. What plant(s) should the company close, in order to minimize
   transportation and fixed costs?

   Based on an example from Frontline Systems:
   http://www.solver.com/disfacility.htm
   Used with permission.
 */

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include "gurobi_c.h"


#define opencol(p)         p
#define transportcol(w,p)  nPlants*(w+1)+p
#define MAXSTR             128

int
main(int   argc,
     char *argv[])
{
  GRBenv   *env   = NULL;
  GRBmodel *model = NULL;
  int       error = 0;
  int       p, w, col;
  int      *cbeg = NULL;
  int      *cind = NULL;
  int       idx, rowct;
  double   *cval = NULL;
  double   *rhs = NULL;
  char     *sense = NULL;
  char     vname[MAXSTR];
  int      cnamect = 0;
  char   **cname = NULL;
  double   maxFixed = -GRB_INFINITY, sol, obj;

  /* Number of plants and warehouses */
  const int nPlants = 5;
  const int nWarehouses = 4;

  /* Warehouse demand in thousands of units */
  double Demand[] = { 15, 18, 14, 20 };

  /* Plant capacity in thousands of units */
  double Capacity[] = { 20, 22, 17, 19, 18 };

  /* Fixed costs for each plant */
  double FixedCosts[] =
    { 12000, 15000, 17000, 13000, 16000 };

  /* Transportation costs per thousand units */
  double TransCosts[4][5] = {
                              { 4000, 2000, 3000, 2500, 4500 },
                              { 2500, 2600, 3400, 3000, 4000 },
                              { 1200, 1800, 2600, 4100, 3000 },
                              { 2200, 2600, 3100, 3700, 3200 }
                            };

  /* Create environment */
  error = GRBloadenv(&env, "facility.log");
  if (error) goto QUIT;

  /* Create initial model */
  error = GRBnewmodel(env, &model, "facility", nPlants * (nWarehouses + 1),
                      NULL, NULL, NULL, NULL, NULL);
  if (error) goto QUIT;

  /* Initialize decision variables for plant open variables */
  for (p = 0; p < nPlants; ++p)
  {
    col = opencol(p);
    error = GRBsetcharattrelement(model, "VType", col, GRB_BINARY);
    if (error) goto QUIT;
    error = GRBsetdblattrelement(model, "Obj", col, FixedCosts[p]);
    if (error) goto QUIT;
    sprintf(vname, "Open%i", p);
    error = GRBsetstrattrelement(model, "VarName", col, vname);
    if (error) goto QUIT;
  }

  /* Initialize decision variables for transportation decision variables:
     how much to transport from a plant p to a warehouse w */
  for (w = 0; w < nWarehouses; ++w)
  {
    for (p = 0; p < nPlants; ++p)
    {
      col = transportcol(w, p);
      error = GRBsetdblattrelement(model, "Obj", col, TransCosts[w][p]);
      if (error) goto QUIT;
      sprintf(vname, "Trans%i.%i", p, w);
      error = GRBsetstrattrelement(model, "VarName", col, vname);
      if (error) goto QUIT;
    }
  }

  /* The objective is to minimize the total fixed and variable costs */
  error = GRBsetintattr(model, "ModelSense", 1);
  if (error) goto QUIT;

  /* Make space for constraint data */
  rowct = (nPlants > nWarehouses) ? nPlants : nWarehouses;
  cbeg = malloc(sizeof(int) * rowct);
  if (!cbeg) goto QUIT;
  cind = malloc(sizeof(int) * (nPlants * (nWarehouses + 1)));
  if (!cind) goto QUIT;
  cval = malloc(sizeof(double) * (nPlants * (nWarehouses + 1)));
  if (!cval) goto QUIT;
  rhs = malloc(sizeof(double) * rowct);
  if (!rhs) goto QUIT;
  sense = malloc(sizeof(char) * rowct);
  if (!sense) goto QUIT;
  cname = calloc(rowct, sizeof(char*));
  if (!cname) goto QUIT;

  /* Production constraints
     Note that the limit sets the production to zero if
     the plant is closed */
  idx = 0;
  for (p = 0; p < nPlants; ++p)
  {
    cbeg[p] = idx;
    rhs[p] = 0.0;
    sense[p] = GRB_LESS_EQUAL;
    cname[p] = malloc(sizeof(char) * MAXSTR);
    if (!cname[p]) goto QUIT;
    cnamect++;
    sprintf(cname[p], "Capacity%i", p);
    for (w = 0; w < nWarehouses; ++w)
    {
      cind[idx] = transportcol(w, p);
      cval[idx++] = 1.0;
    }
    cind[idx] = opencol(p);
    cval[idx++] = -Capacity[p];
  }
  error = GRBaddconstrs(model, nPlants, idx, cbeg, cind, cval, sense,
                        rhs, cname);
  if (error) goto QUIT;

  /* Demand constraints */
  idx = 0;
  for (w = 0; w < nWarehouses; ++w)
  {
    cbeg[w] = idx;
    sense[w] = GRB_EQUAL;
    sprintf(cname[w], "Demand%i", w);
    for (p = 0; p < nPlants; ++p)
    {
      cind[idx] = transportcol(w, p);
      cval[idx++] = 1.0;
    }
  }
  error = GRBaddconstrs(model, nWarehouses, idx, cbeg, cind, cval, sense,
                        Demand, cname);
  if (error) goto QUIT;

  /* Guess at the starting point: close the plant with the highest
     fixed costs; open all others */

  /* First, open all plants */
  for (p = 0; p < nPlants; ++p)
  {
    error = GRBsetdblattrelement(model, "Start", opencol(p), 1.0);
    if (error) goto QUIT;
  }

  /* Now close the plant with the highest fixed cost */
  printf("Initial guess:\n");
  for (p = 0; p < nPlants; ++p)
  {
    if (FixedCosts[p] > maxFixed)
    {
      maxFixed = FixedCosts[p];
    }
  }
  for (p = 0; p < nPlants; ++p)
  {
    if (FixedCosts[p] == maxFixed)
    {
      error = GRBsetdblattrelement(model, "Start", opencol(p), 0.0);
      if (error) goto QUIT;
      printf("Closing plant %i\n\n", p);
      break;
    }
  }

  /* Use barrier to solve root relaxation */
  error = GRBsetintparam(GRBgetenv(model),
                         GRB_INT_PAR_METHOD,
                         GRB_METHOD_BARRIER);
  if (error) goto QUIT;

  /* Solve */
  error = GRBoptimize(model);
  if (error) goto QUIT;

  /* Print solution */
  error = GRBgetdblattr(model, "ObjVal", &obj);
  if (error) goto QUIT;
  printf("\nTOTAL COSTS: %f\n", obj);
  printf("SOLUTION:\n");
  for (p = 0; p < nPlants; ++p)
  {
    error = GRBgetdblattrelement(model, "X", opencol(p), &sol);
    if (error) goto QUIT;
    if (sol == 1.0)
    {
      printf("Plant %i open:\n", p);
      for (w = 0; w < nWarehouses; ++w)
      {
        error = GRBgetdblattrelement(model, "X", transportcol(w, p), &sol);
        if (error) goto QUIT;
        if (sol > 0.0001)
        {
          printf("  Transport %f units to warehouse %i\n", sol, w);
        }
      }
    }
    else
    {
      printf("Plant %i closed!\n", p);
    }
  }


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
  for (p = 0; p < cnamect; ++p) {
    free(cname[p]);
  }
  free(cname);

  /* Free model */

  GRBfreemodel(model);

  /* Free environment */

  GRBfreeenv(env);

  return 0;
}
