/* Copyright 2014, Gurobi Optimization, Inc. */

/*
   This example reads a model from a file, sets up a callback that
   monitors optimization progress and implements a custom
   termination strategy, and outputs progress information to the
   screen and to a log file.

   The termination strategy implemented in this callback stops the
   optimization of a MIP model once at least one of the following two
   conditions have been satisfied:
     1) The optimality gap is less than 10%
     2) At least 10000 nodes have been explored, and an integer feasible
        solution has been found.
   Note that termination is normally handled through Gurobi parameters
   (MIPGap, NodeLimit, etc.).  You should only use a callback for
   termination if the available parameters don't capture your desired
   termination criterion.
*/

import gurobi.*;
import java.io.FileWriter;
import java.io.IOException;

public class Callback extends GRBCallback {
  private double     lastiter;
  private double     lastnode;
  private GRBVar[]   vars;
  private FileWriter logfile;

  public Callback(GRBVar[] xvars, FileWriter xlogfile) {
    lastiter = lastnode = -GRB.INFINITY;
    vars = xvars;
    logfile = xlogfile;
  }

  protected void callback() {
    try {
      if (where == GRB.CB_POLLING) {
        // Ignore polling callback
      } else if (where == GRB.CB_PRESOLVE) {
        // Presolve callback
        int cdels = getIntInfo(GRB.CB_PRE_COLDEL);
        int rdels = getIntInfo(GRB.CB_PRE_ROWDEL);
        if (cdels != 0 || rdels != 0) {
          System.out.println(cdels + " columns and " + rdels
              + " rows are removed");
        }
      } else if (where == GRB.CB_SIMPLEX) {
        // Simplex callback
        double itcnt = getDoubleInfo(GRB.CB_SPX_ITRCNT);
        if (itcnt - lastiter >= 100) {
          lastiter = itcnt;
          double obj    = getDoubleInfo(GRB.CB_SPX_OBJVAL);
          int    ispert = getIntInfo(GRB.CB_SPX_ISPERT);
          double pinf   = getDoubleInfo(GRB.CB_SPX_PRIMINF);
          double dinf   = getDoubleInfo(GRB.CB_SPX_DUALINF);
          char ch;
          if (ispert == 0)      ch = ' ';
          else if (ispert == 1) ch = 'S';
          else                  ch = 'P';
          System.out.println(itcnt + " " + obj + ch + " "
              + pinf + " " + dinf);
        }
      } else if (where == GRB.CB_MIP) {
        // General MIP callback
        double nodecnt = getDoubleInfo(GRB.CB_MIP_NODCNT);
        double objbst  = getDoubleInfo(GRB.CB_MIP_OBJBST);
        double objbnd  = getDoubleInfo(GRB.CB_MIP_OBJBND);
        int    solcnt  = getIntInfo(GRB.CB_MIP_SOLCNT);
        if (nodecnt - lastnode >= 100) {
          lastnode = nodecnt;
          int actnodes = (int) getDoubleInfo(GRB.CB_MIP_NODLFT);
          int itcnt    = (int) getDoubleInfo(GRB.CB_MIP_ITRCNT);
          int cutcnt   = getIntInfo(GRB.CB_MIP_CUTCNT);
          System.out.println(nodecnt + " " + actnodes + " "
              + itcnt + " " + objbst + " " + objbnd + " "
              + solcnt + " " + cutcnt);
        }
        if (Math.abs(objbst - objbnd) < 0.1 * (1.0 + Math.abs(objbst))) {
          System.out.println("Stop early - 10% gap achieved");
          abort();
        }
        if (nodecnt >= 10000 && solcnt > 0) {
          System.out.println("Stop early - 10000 nodes explored");
          abort();
        }
      } else if (where == GRB.CB_MIPSOL) {
        // MIP solution callback
        int      nodecnt = (int) getDoubleInfo(GRB.CB_MIPSOL_NODCNT);
        double   obj     = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
        int      solcnt  = getIntInfo(GRB.CB_MIPSOL_SOLCNT);
        double[] x       = getSolution(vars);
        System.out.println("**** New solution at node " + nodecnt
            + ", obj " + obj + ", sol " + solcnt
            + ", x[0] = " + x[0] + " ****");
      } else if (where == GRB.CB_MIPNODE) {
        // MIP node callback
        System.out.println("**** New node ****");
        if (getIntInfo(GRB.CB_MIPNODE_STATUS) == GRB.OPTIMAL) {
          double[] x = getNodeRel(vars);
          setSolution(vars, x);
        }
      } else if (where == GRB.CB_BARRIER) {
        // Barrier callback
        int    itcnt   = getIntInfo(GRB.CB_BARRIER_ITRCNT);
        double primobj = getDoubleInfo(GRB.CB_BARRIER_PRIMOBJ);
        double dualobj = getDoubleInfo(GRB.CB_BARRIER_DUALOBJ);
        double priminf = getDoubleInfo(GRB.CB_BARRIER_PRIMINF);
        double dualinf = getDoubleInfo(GRB.CB_BARRIER_DUALINF);
        double cmpl    = getDoubleInfo(GRB.CB_BARRIER_COMPL);
        System.out.println(itcnt + " " + primobj + " " + dualobj + " "
            + priminf + " " + dualinf + " " + cmpl);
      } else if (where == GRB.CB_MESSAGE) {
        // Message callback
        String msg = getStringInfo(GRB.CB_MSG_STRING);
        if (msg != null) logfile.write(msg);
      }
    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode());
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.out.println("Error during callback");
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {

    if (args.length < 1) {
      System.out.println("Usage: java Callback filename");
      System.exit(1);
    }

    FileWriter logfile = null;

    try {
      // Create environment
      GRBEnv env = new GRBEnv();

      // Turn off display and heuristics
      env.set(GRB.IntParam.OutputFlag, 0);
      env.set(GRB.DoubleParam.Heuristics, 0.0);

      // Read model from file
      GRBModel model = new GRBModel(env, args[0]);

      // Open log file
      logfile = new FileWriter("cb.log");

      // Create a callback object and associate it with the model
      GRBVar[] vars = model.getVars();
      Callback cb   = new Callback(vars, logfile);

      model.setCallback(cb);

      // Solve model and capture solution information
      model.optimize();

      System.out.println("");
      System.out.println("Optimization complete");
      if (model.get(GRB.IntAttr.SolCount) == 0) {
        System.out.println("No solution found, optimization status = "
            + model.get(GRB.IntAttr.Status));
      } else {
        System.out.println("Solution found, objective = "
            + model.get(GRB.DoubleAttr.ObjVal));

        String[] vnames = model.get(GRB.StringAttr.VarName, vars);
        double[] x      = model.get(GRB.DoubleAttr.X, vars);

        for (int j = 0; j < vars.length; j++) {
          if (x[j] != 0.0) System.out.println(vnames[j] + " " + x[j]);
        }
      }

      // Dispose of model and environment
      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode());
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.out.println("Error during optimization");
      e.printStackTrace();
    } finally {
      // Close log file
      if (logfile != null) {
        try { logfile.close(); } catch (IOException e) {}
      }
    }
  }
}
