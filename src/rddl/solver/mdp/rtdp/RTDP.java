/**
 * RDDL: Factored RTDP Implementation.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 9/11/11
 *
 **/

package rddl.solver.mdp.rtdp;

import java.util.*;

import dd.discrete.ADD;

import rddl.*;
import rddl.RDDL.*;
import rddl.policy.Policy;
import rddl.policy.SPerseusSPUDDPolicy;
import rddl.translate.RDDL2Format;
import util.CString;
import util.Pair;

///////////////////////////////////////////////////////////////////////////
//                             Helper Functions
///////////////////////////////////////////////////////////////////////////

public class RTDP extends Policy {
	
	public final static boolean SHOW_STATE   = true;
	public final static boolean SHOW_ACTIONS = true;
	public final static boolean SHOW_ACTION_TAKEN = true;
	
	// Only for diagnostics, comment this out when evaluating
	public final static boolean DISPLAY_SPUDD_ADDS_GRAPHVIZ = false;
	public final static boolean DISPLAY_SPUDD_ADDS_TEXT = true;
	
	public RDDL2Format _translation = null;
	
	// Using CString wrapper to speedup hash lookups
	public ADD _context;
	public ArrayList<CString> _alStateVars;
	public ArrayList<CString> _alActionNames;
	public HashMap<CString, Action> _hmActionName2Action; // Holds transition function
	public int _value  = -1; // Value function ADD
	
	// Just use the default random seed
	public Random _rand = new Random();
	
	// Action class for MDP definition
	public class Action {
		public HashMap<CString,Integer> _hmStateVar2CPT = null; // Map of CPT ADDs
		public int _reward = -1; // Action-specific reward function ADD

		public Action(HashMap<CString,Integer> cpts, Integer reward) {
			_hmStateVar2CPT = cpts;
			_reward = reward;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (CString var : _alStateVars)
				sb.append("- CPT for state var '" + var + "'\n" + 
						_context.printNode(_hmStateVar2CPT.get(var)) + "\n");
			sb.append("- Reward: " + _context.printNode(_reward) + "\n");
			return sb.toString();
		}
	}
	
	// Constructors
	public RTDP () { }
	
	public RTDP(String instance_name) {
		super(instance_name);
	}

	///////////////////////////////////////////////////////////////////////////
	//                      Main Action Selection Method
	//
	// If you're using Java and the SPUDD / Symbolic Perseus Format, this 
	// method is the only client method you need to understand to implement
	// your own custom policy.
	///////////////////////////////////////////////////////////////////////////

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		//System.out.println("FULL STATE:\n\n" + SPerseusSPUDDPolicy.getStateDescription(s));

		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			// when no observations have been generated, for now, we just
			// return a 'noop'
			System.out.println("NO STATE/OBS: taking noop\n\n");
			return new ArrayList<PVAR_INST_DEF>();
		}
		
		// If the domain is partially observed, we only see observations,
		// otherwise if it is fully observed, we see the state
		String fluent_type = s._alObservNames.size() > 0 ? "observ" : "states";
		
		// Get a set of all true observation or state variables
		// Note: for a POMDP, the agent should only see the observations and
		//       *never* have access to the underlying state.  State information 
		//       is not provided to a Policy when using the Client/Server interface 
		//       (to prevent cheating in a competition setting); it *is* provided 
		//       via the Simulator interface, but it should be ignored for purposes 
		//       of policy evaluation.
		TreeSet<String> true_vars = SPerseusSPUDDPolicy.getTrueFluents(s, fluent_type);
		if (SHOW_STATE) {
			System.out.println("\n==============================================");
			System.out.println("\nTrue " + 
					           (fluent_type.equals("states") ? "state" : "observation") + 
							   " variables:");
			for (String prop_var : true_vars)
				System.out.println(" - " + prop_var);
		}
		
		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);

		if (SHOW_STATE) {
			System.out.println("\nLegal action names:");
			for (String action_name : action_map.keySet())
				System.out.println(" - " + action_name);
		}
		
		//////////////////////////// BEGIN RTDP ///////////////////////////////
		// TODO: We're at a state in a trial, implement RTDP algorithm here
		//       for value function update and action selection
		///////////////////////////////////////////////////////////////////////
		
		// (For now, returning a random action selection)
		ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
		String action_taken = actions.get(_rand.nextInt(actions.size()));
		if (SHOW_ACTION_TAKEN)
			System.out.println("\n--> Action taken: " + action_taken);
		
		return action_map.get(action_taken);
	}

	///////////////////////////////////////////////////////////////////////////
	//                             Trial Signals
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
			_context = _translation._context;
			_value = _context.getConstantNode(0d); // Initialize to 0			

			// Get the state var and action names
			_alStateVars = new ArrayList<CString>();
			for (String s : _translation._alStateVars)
				_alStateVars.add(new CString(s));
			
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

					cpts.put(new CString(s), cpt);
				}
				
				// Build Action and add to HashMap
				Action action = new Action(cpts, reward);
				_hmActionName2Action.put(new CString(a), action);
			}
			
			// Display ADDs on terminal?
			if (DISPLAY_SPUDD_ADDS_TEXT) {
				System.out.println("State variables: " + _alStateVars);
				System.out.println("Action names: " + _alActionNames);
				
				for (CString a : _alActionNames) {
					Action action = _hmActionName2Action.get(a);
					System.out.println("Content of action '" + a + "'\n" + action);
				}
				
				System.out.println("Value ADD: " + _context.printNode(_value));
			}
			
			// Display ADDs in graph visualization window?
			// (only show a subset... 100's to display otherwise)
			final int MAX_DISPLAY = 10;
			if (DISPLAY_SPUDD_ADDS_GRAPHVIZ) {
				_context.getGraph(_value).launchViewer();
				int displayed = 1;
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
}
