/**
 * RDDL: Factored value iteration implementation.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 9/18/11
 *
 * This is essentially a Java version of SPUDD:
 * 
 *   J. Hoey, R. St-Aubin, A. J. Hu and C. Boutilier.
 *   SPUDD: Stochastic Planning Using Decision Diagrams. 
 *   UAI 1999.
 *
 * ./run rddl.sim.Simulator files/rddl/test/ rddl.solver.mdp.vi.VI sysadmin_test1 rddl.viz.SysAdminScreenDisplay
 **/

package rddl.solver.mdp.vi;

import graph.Graph;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

import dd.discrete.DD;
import dd.discrete.ADD;

import rddl.*;
import rddl.RDDL.*;
import rddl.policy.Policy;
import rddl.policy.SPerseusSPUDDPolicy;
import rddl.solver.DDUtils;
import rddl.solver.TimeOutException;
import rddl.solver.mdp.Action;
import rddl.translate.RDDL2Format;
import util.CString;
import util.Pair;

public class VI extends Policy {
	
	public static int SOLVER_TIME_LIMIT = 40; // Solver time limit (seconds)
	
	public final static boolean SHOW_STATE   = true;
	public final static boolean SHOW_ACTIONS = true;
	public final static boolean SHOW_ACTION_TAKEN = true;
	
	// Only for diagnostics, comment this out when evaluating
	public final static boolean DISPLAY_SPUDD_ADDS_GRAPHVIZ = false;
	public final static boolean DISPLAY_SPUDD_ADDS_TEXT = false;
	
	public RDDL2Format _translation = null;
	
	// Using CString wrapper to speedup hash lookups
	public ADD _context;
	public ArrayList<Integer> _allMDPADDs;
	public ArrayList<CString> _alStateVars;
	public ArrayList<CString> _alPrimeStateVars;
	public ArrayList<CString> _alActionNames;
	public HashMap<CString, Action> _hmActionName2Action; // Holds transition function
	
	// Just use the default random seed
	public Random _rand = new Random();
		
	// Constructors
	public VI () { }
	
	public VI(String instance_name) {
		super(instance_name);
	}

	///////////////////////////////////////////////////////////////////////////
	//                      Main Action Selection Method
	///////////////////////////////////////////////////////////////////////////

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		//System.out.println("FULL STATE:\n\n" + SPerseusSPUDDPolicy.getStateDescription(s));

		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			System.err.println("ERROR: NO STATE/OBS: MDP must have state.");
			System.exit(1);
		}
		
		// Get a set of all true observation or state variables
		TreeSet<CString> true_vars = 
			CString.Convert2CString(SPerseusSPUDDPolicy.getTrueFluents(s, "states"));
		if (SHOW_STATE) {
			System.out.println("\n==============================================");
			System.out.println("\nTrue state variables:");
			for (CString prop_var : true_vars)
				System.out.println(" - " + prop_var);
		}
		
		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);
		
		// Use the precomputed q-functions (cached when the value function
		// was computed) to determine the best action for this state
		String action_taken = null;
		if (_hmAct2Regr == null) {
			// No VI results available, just take random action
			ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
			action_taken = actions.get(_rand.nextInt(actions.size()));			
			
			if (SHOW_ACTION_TAKEN)
				System.out.println("\n--> [Random] action taken: " + action_taken);
		} else {
			if (SHOW_ACTION_TAKEN)
				System.out.println("\nActions and Q-values:");

			double best_action_value = -Double.MAX_VALUE;
			ArrayList add_state_assign = DDUtils.ConvertTrueVars2DDAssign(_context, true_vars, _alStateVars);

			// Find best action by evaluating each Q-function
			for (Map.Entry<CString, Integer> me : _hmAct2Regr.entrySet()) {
				
				if (!action_map.keySet().contains(me.getKey()._string))
					continue; // Action is not legal in this state
				
				Integer q_function = me.getValue();
				//System.out.println("Qfun:\n" + _context.printNode(q_function) + "\n" + add_state_assign);
				double action_value = _context.evaluate(q_function, add_state_assign);
				if (SHOW_ACTION_TAKEN)
					System.out.println(" - " + me.getKey() + ": " + _df.format(action_value));
				if (action_taken == null || action_value > best_action_value) {
					action_taken = me.getKey()._string;
					best_action_value = action_value;
				}
			}
			
			if (SHOW_ACTION_TAKEN)
				System.out.println("\n--> Best action taken [" + best_action_value + "]: " 
						+ action_taken);
		}
		
		return action_map.get(action_taken);
	}

	///////////////////////////////////////////////////////////////////////////
	//                         Round / Session Signals
	//
	// If you need to keep track of state information across rounds or sessions, 
	// just modify these methods.  (Each session consists of total_rounds rounds.)
	///////////////////////////////////////////////////////////////////////////

	public void roundInit(double time_left, int horizon, int round_number, int total_rounds) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND INIT " + round_number + "/" + total_rounds + "; time remaining = " + time_left + ", horizon = " + horizon);
		System.out.println("*********************************************************");
				
		// Build ADDs for transition, reward and value function (if not already built)
		if (_translation == null) {
			
			// Use RDDL2Format to build SPUDD ADD translation of _sInstanceName
			try {
				_translation = new RDDL2Format(_rddl, _sInstanceName, RDDL2Format.SPUDD_CURR, "");
			} catch (Exception e) {
				System.err.println("Could not construct MDP for: " + _sInstanceName + "\n" + e);
				e.printStackTrace(System.err);
				System.exit(1);
			}

			// Get ADD context and initialize value function ADD
			_allMDPADDs = new ArrayList<Integer>();
			_context = _translation._context;

			// Get the state var and action names
			_alStateVars = new ArrayList<CString>();
			_alPrimeStateVars = new ArrayList<CString>();
			for (String s : _translation._alStateVars) {
				_alStateVars.add(new CString(s));
				_alPrimeStateVars.add(new CString(s + "'"));
			}
			
			_alActionNames = new ArrayList<CString>();
			for (String a : _translation._hmActionMap.keySet())
				_alActionNames.add(new CString(a));
	
			// Now extract the reward and transition ADDs
			_hmActionName2Action = new HashMap<CString,Action>();
			for (String a : _translation._hmActionMap.keySet()) {
				HashMap<CString,Integer> cpts = new HashMap<CString,Integer>();
				int reward = _context.getConstantNode(0d);
				
				// Build reward from additive decomposition
				ArrayList<Integer> reward_summands = _translation._act2rewardDD.get(a);
				for (int summand : reward_summands)
					reward = _context.applyInt(reward, summand, ADD.ARITH_SUM);
				_allMDPADDs.add(reward);
				
				// Build CPTs
				for (String s : _translation._alStateVars) {
					int dd = _translation._var2transDD.get(new Pair(a, s));
					
					int dd_true  = _context.getVarNode(s + "'", 0d, 1d);
					dd_true = _context.applyInt(dd_true, dd, ADD.ARITH_PROD);
		
					int dd_false = _context.getVarNode(s + "'", 1d, 0d);
					//System.out.println("Multiplying..." + dd + ", " + DD_ONE);
					//_context.printNode(dd);
					//_context.printNode(DD_ONE);
					int one_minus_dd = _context.applyInt(_context.getConstantNode(1d), dd, ADD.ARITH_MINUS);
					dd_false = _context.applyInt(dd_false, one_minus_dd, ADD.ARITH_PROD);
					
					// Now have "dual action diagram" cpt DD
					int cpt = _context.applyInt(dd_true, dd_false, ADD.ARITH_SUM);

					cpts.put(new CString(s + "'"), cpt);
					_allMDPADDs.add(cpt);
				}
				
				// Build Action and add to HashMap
				CString action_name = new CString(a);
				Action action = new Action(_context, action_name, cpts, reward);
				_hmActionName2Action.put(action_name, action);
			}
			
			// Display ADDs on terminal?
			if (DISPLAY_SPUDD_ADDS_TEXT) {
				System.out.println("State variables: " + _alStateVars);
				System.out.println("Action names: " + _alActionNames);
				
				for (CString a : _alActionNames) {
					Action action = _hmActionName2Action.get(a);
					System.out.println("Content of action '" + a + "'\n" + action);
				}
			}
			
			// Display ADDs in graph visualization window?
			// (only show a subset... 100's to display otherwise)
			final int MAX_DISPLAY = 10;
			if (DISPLAY_SPUDD_ADDS_GRAPHVIZ) {
				int displayed = 0;
				for (CString a : _alActionNames) {
					Action action = _hmActionName2Action.get(a);
					
					// Show cpts for each action/var
					for (CString var : _alStateVars) {
						_context.getGraph(action._hmStateVar2CPT.get(var)).launchViewer();

						if (++displayed >= MAX_DISPLAY)
							break;
					}
					
					// Show reward for action
					_context.getGraph(action._reward).launchViewer();
					
					if (++displayed >= MAX_DISPLAY)
						break;
				}				
			}
			
			// Call value iteration solver for SOLVER_TIME_LIMIT seconds
			try {
				resetSolver();
				solve(SOLVER_TIME_LIMIT);
			} catch (TimeOutException e) {
				System.out.println("TIME LIMIT EXCEEDED at " + _nIter + " iterations.");
			} catch (Exception e) {
				System.err.println("ERROR at " + _nIter + " iterations.");
				e.printStackTrace(System.err);
				System.exit(1);
			} finally {
				
				System.out.println("\n*********************************************************");
				System.out.println("Solution in VI exit at iteration " + _nIter + ": " + 
						_context.countExactNodes(_valueDD) + " nodes.");

				// Display value function if small enough
				if (_context.countExactNodes(_valueDD) < 20) {
					System.out.print("Value function DD:");
					StringWriter sw = new StringWriter();
					_context.exportTree(_valueDD, new PrintWriter(sw), true);
					System.out.println(sw);
				}
				
				System.out.println("*********************************************************");
			}
		}		
	}
	
	public void roundEnd(double reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND END, reward = " + reward);
		System.out.println("*********************************************************");
	}
	
	public void sessionEnd(double total_reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> SESSION END, total reward = " + total_reward);
		System.out.println("*********************************************************");
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                      Factored Value Iteration Solver
	///////////////////////////////////////////////////////////////////////////
	
	// Local constants
	public final static int VERBOSE_LEVEL = 1; // Determines how much output is displayed
	
	public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
	public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < this amt

	// For printing
	public final static DecimalFormat _df = new DecimalFormat("#.###");

	// Timing variables
	public long _lStartTime; // For timing purposes
	public int  _nTimeLimitSecs;
	public static Runtime RUNTIME = Runtime.getRuntime();

	// Local vars
	public INSTANCE _rddlInstance = null;
	public int _valueDD;
	public int _maxDD;
	public int _prevDD;
	public int _nDDType; // Type of DD to use
	public int _nIter;
	public double _dRewardRange; // Important if approximating
	public double _dDiscount;
	public int _nHorizon;
	public CString _sRegrAction;
	public ArrayList<Integer> _alSaveNodes; // Nodes to save during cache flushing
	public HashMap<CString,Integer> _hmAct2Regr; // Cached DDs from last regression step

	// Initialize all variables (call before starting value iteration)
	public void resetSolver() {
		_prevDD = _maxDD = -1;
		_valueDD = _context.getConstantNode(0d); // Initialize to 0			
		_nIter = -1;
		_sRegrAction = null;
		_rddlInstance = _rddl._tmInstanceNodes.get(this._sInstanceName);
		if (_rddlInstance == null) {
			System.err.println("ERROR: Could not fine RDDL instance '" + _rddlInstance + "'");
			System.exit(1);
		}
		_dDiscount = _rddlInstance._dDiscount;
		_nHorizon  = _rddlInstance._nHorizon;
		
		
		_hmAct2Regr   = null;
		_alSaveNodes  = new ArrayList<Integer>();
		
		_dRewardRange = 0d;
		for (Action a : _hmActionName2Action.values())
			_dRewardRange = Math.max(_dRewardRange, 
					_context.getMaxValue(a._reward) - 
			        _context.getMinValue(a._reward));
	}

	// MDP inference methods
	public int solve(int time_limit_secs) throws TimeOutException {

		_nTimeLimitSecs = time_limit_secs;
		
		System.out.println("Using time limit: " + _nTimeLimitSecs + " seconds");
		System.out.println("Using discount:   " + _dDiscount);
		System.out.println("Using horizon:    " + _nHorizon + "\n");

		// Other initialization
		_nIter = 0;
		double max_diff = Double.POSITIVE_INFINITY;
		boolean error_decreasing = true;
		_lStartTime = System.currentTimeMillis();

		// ////////////////////////////////////////////////////////////
		// Iterate until convergence (or max iterations)
		// ////////////////////////////////////////////////////////////
		while (_nIter < _nHorizon) {

			// Cache maintenance
			flushCaches();

			// Show diagnostics including whether error is decreasing
			System.out.print(error_decreasing ? "  " : "* ");
			System.out.println("Iteration #" + _nIter + ", "
					+ _context.countExactNodes(_valueDD) + " nodes / "
					+ _context.getCacheSize() + " cache / " + MemDisplay()
					+ " bytes, " + "diff:[" + _df.format(max_diff) + "], mr:["
					+ _df.format(_context.getMaxValue(_valueDD)) + "]");

			// Prime the value function
			_prevDD = _valueDD;
			_valueDD = _context.remapGIDsInt(_valueDD, _translation._hmPrimeRemap);

			// Cache maintenance -- clear out previous nodes, but save Q-functions
			clearSaveNodes();
			if (_hmAct2Regr != null) // Save previous regression (needed if time out)
				for (CString a : _alActionNames)
					saveNode(_hmAct2Regr.get(a));
			
			//////////////////////////////////////////////////////////////
			// Iterate over each action to obtain Q-function from _valueDD
			//////////////////////////////////////////////////////////////
			_maxDD = -1;
			HashMap<CString,Integer> temp_regr = new HashMap<CString,Integer>();
			for (Map.Entry<CString, Action> me : _hmActionName2Action.entrySet()) {

				CString action_name = me.getKey();
				Action a = me.getValue();

				//////////////////////////////////////////////////////////////
				// Regress the current value function through each action
				//////////////////////////////////////////////////////////////
				int regr = regress(_valueDD, a, true);
				temp_regr.put(action_name, regr);

				// Show debug info if required
				if (VERBOSE_LEVEL >= 1) {
					System.out.println("  - After regress '" + action_name + "', "
							+ _context.countExactNodes(regr) + " nodes / "
							+ _context.getCacheSize() + " cache");
					if (VERBOSE_LEVEL >= 3) {
						Graph g = _context.getGraph(regr);
						g.addNodeLabel("_temp_", action_name.toString());
						g.addNodeShape("_temp_", "square");
						g.addNodeStyle("_temp_", "filled");
						g.addNodeColor("_temp_", "lightblue");
						g.addUniLink("_temp_", "_temp_");
	
						// g.genDotFile(type + "value.dot");
						g.launchViewer(1300, 770);
					}
				}
				
				// Cache maintenance - after regression
				saveNode(regr); // Save latest Q-function regression
				flushCaches();
				checkTimeLimit();

				//////////////////////////////////////////////////////////////
				// Take the max over this action and the previous action
				//////////////////////////////////////////////////////////////
				_maxDD = ((_maxDD == -1) ? regr : 
						  _context.applyInt(_maxDD, regr, DD.ARITH_MAX));

				// Cache maintenance - after maximization
				flushCaches();
				checkTimeLimit();

				// Show debug info if required
				if (VERBOSE_LEVEL >= 1) {
					System.out.println("  - After max '" + action_name + "', "
							+ _context.countExactNodes(_maxDD) + " nodes / "
							+ _context.getCacheSize() + " cache");
				}
			}

			// We've done a full update of value DD, increment iteration counter
			// and update the cached Q-functions with the new ones
			_valueDD = _maxDD;
			_hmAct2Regr = temp_regr;
			_nIter++;
			
			////////////////////////////////////////////////////////////////////
			// Compute max difference between current and previous value function
			////////////////////////////////////////////////////////////////////
			int diff = _context.applyInt(_valueDD, _prevDD, DD.ARITH_MINUS);
			double max_diff_prev = max_diff;
			double max_pos_diff = _context.getMaxValue(diff);
			double max_neg_diff = _context.getMinValue(diff);
			max_diff = Math.max(Math.abs(max_pos_diff), Math.abs(max_neg_diff));
			error_decreasing = (max_diff < max_diff_prev);

			// Show debug info if required
			if (VERBOSE_LEVEL >= 1) {
				System.out.println("\n  - After sum, "
						+ _context.countExactNodes(_valueDD) + " nodes / "
						+ _context.getCacheSize() + " cache");
				if (VERBOSE_LEVEL >= 2) {
					Graph g1 = _context.getGraph(_valueDD);
					g1.addNodeLabel("_temp_", "V^" + _nIter);
					g1.addNodeShape("_temp_", "square");
					g1.addNodeStyle("_temp_", "filled");
					g1.addNodeColor("_temp_", "lightblue");
					g1.addUniLink("_temp_", "_temp_");
					g1.launchViewer(1300, 770);
					//Graph g2 = _context.getGraph(_prevDD);
					//g2.launchViewer(1300, 770);
				}
				System.out.println("\n  - Max diff: " + _df.format(max_diff));
			}
		}

		// Value iteration complete -- flush caches and return number of iterations
		flushCaches();
		return _nIter;
	}

	// Compute and return the Q-function from vfun for action a
	public int regress(int vfun, Action a, boolean flush_caches) throws TimeOutException {

		int dd_ret = vfun;

		// Find what gids (ADD level assignments of variables) are currently in vfun
		Set vfun_gids = _context.getGIDs(vfun);

		// Show debug info if required
		if (VERBOSE_LEVEL >= 1) {
			System.out.println("Regressing action: " + a._csActionName + "\nGIDs: " + vfun_gids);
		}

		//////////////////////////////////////////////////////////////
		// For each next-state variable in DBN for action 'a'
		//////////////////////////////////////////////////////////////
		for (Map.Entry<Integer, Integer> me : a._hmVarID2CPT.entrySet()) {
			
			Integer cpt_dd  = me.getValue();
			Integer head_var_gid = me.getKey();
			
			// No need to regress variables not in the value function  
			if (!vfun_gids.contains(head_var_gid)) {
				if (VERBOSE_LEVEL >= 1) {
					System.out.println("Skipping " + _context._hmID2VarName.get(head_var_gid) + " / " + head_var_gid);
					System.out.println("... not in " + vfun_gids);
				}
				continue;
			}

			// Show debug info if required
			if (VERBOSE_LEVEL >= 2)
				System.out.println("  - Summing out: " + _context._hmID2VarName.get(head_var_gid));

			///////////////////////////////////////////////////////////////////
			// Multiply next state variable DBN into current value function
			///////////////////////////////////////////////////////////////////
			clearSaveNode(dd_ret);
			dd_ret = _context.applyInt(dd_ret, cpt_dd, DD.ARITH_PROD);
			saveNode(dd_ret);
			flushCaches();
			checkTimeLimit();

			///////////////////////////////////////////////////////////////////
			// Sum out next state variable
			///////////////////////////////////////////////////////////////////
			clearSaveNode(dd_ret);
			dd_ret = _context.opOut(dd_ret, head_var_gid, DD.ARITH_SUM);
			saveNode(dd_ret);
			flushCaches();
			checkTimeLimit();
		}
		
		// Discount the regressed function (if needed)
		if (_dDiscount < 1d)
			dd_ret = _context.scalarMultiply(dd_ret, _dDiscount);

		// Add in action-dependent reward
		dd_ret = _context.applyInt(dd_ret, a._reward, DD.ARITH_SUM);
		
		// Return regressed value function
		return dd_ret;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// DD Cache Maintenance for MDPs
	////////////////////////////////////////////////////////////////////////////

	// Clear nodes on save list
	public void clearSaveNodes() {
		_alSaveNodes.clear();
	}
	
	// Remove nodes on save list
	public void clearSaveNode(Integer dd) {
		_alSaveNodes.remove(dd);
	}

	// Add node to save list
	public void saveNode(Integer dd) {
		_alSaveNodes.add(dd);
	}

	// Frees up memory... only do this if near limit?
	public void flushCaches() {
		if (!ALWAYS_FLUSH
				&& ((double) RUNTIME.freeMemory() / (double) RUNTIME
						.totalMemory()) > FLUSH_PERCENT_MINIMUM) {
			return; // Still enough free mem to exceed minimum requirements
		}

		_context.clearSpecialNodes();
		for (Integer dd : _alSaveNodes)
			_context.addSpecialNode(dd);
		for (Integer dd : _allMDPADDs)
			_context.addSpecialNode(dd);
		_context.addSpecialNode(_valueDD);
		if (_maxDD != -1)
			_context.addSpecialNode(_maxDD);
		if (_prevDD != -1)
			_context.addSpecialNode(_prevDD);

		_context.flushCaches(false);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Miscellaneous helper methods
	////////////////////////////////////////////////////////////////////////////

	public void checkTimeLimit() throws TimeOutException {
		double elapsed_time = (System.currentTimeMillis() - _lStartTime) / 1000d;
		if (elapsed_time > (double)_nTimeLimitSecs)
			throw new TimeOutException("Elapsed time " + elapsed_time + " (s) exceeded time limit of " + _nTimeLimitSecs + " (s)");
	}
	
	public static String MemDisplay() {
		long total = RUNTIME.totalMemory();
		long free = RUNTIME.freeMemory();
		return total - free + ":" + total;
	}
	public void setLimitTime(Integer time) {
		SOLVER_TIME_LIMIT=time;
	}

}
