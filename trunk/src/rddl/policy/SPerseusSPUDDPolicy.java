/**
 * RDDL: This policy is for use with the Symbolic Perseus and SPUDD
 *       translations.  The planner displays state (MDP) or observations
 *       (POMDP) as true fluents as well as possible actions and
 *       then randomly takes an action.  State-action constraints are
 *       not checked.
 * 
 * NOTE: This just skeleton code... to see a full solver implementation,
 *       see the implementation of Value Iteration in rddl.solver.mdp.vi.VI.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 12/18/10
 *
 **/

package rddl.policy;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;
import rddl.translate.RDDL2Format;

///////////////////////////////////////////////////////////////////////////
//                             Helper Functions
///////////////////////////////////////////////////////////////////////////

public class SPerseusSPUDDPolicy extends Policy {
	
	public final static boolean SHOW_STATE   = true;
	public final static boolean SHOW_ACTIONS = true;
	public final static boolean SHOW_ACTION_TAKEN = true;
	//public final static boolean ALLOW_NOOP   = false;
	
	// Just use the default random seed
	public Random _rand = new Random();
	
	public SPerseusSPUDDPolicy () { }
	
	public SPerseusSPUDDPolicy(String instance_name) {
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
		
		//System.out.println("FULL STATE:\n\n" + getStateDescription(s));

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
		TreeSet<String> true_vars = getTrueFluents(s, fluent_type);
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
		
		// Return a random action selection
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
	//                             Helper Methods
	//
	// You likely won't need to understand the code below, only the above code.
	///////////////////////////////////////////////////////////////////////////
	
	public static TreeSet<String> getTrueFluents(State s, String fluent_type) {
				
		// Go through all variable types (state, interm, observ, action, nonfluent)
		TreeSet<String> true_fluents = new TreeSet<String>();
		for (PVAR_NAME p : (ArrayList<PVAR_NAME>)s._hmTypeMap.get(fluent_type)) {
			
			try {
				// Go through all term groundings for variable p
				ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);										
				for (ArrayList<LCONST> gfluent : gfluents) {
					if ((Boolean)s.getPVariableAssign(p, gfluent)) {
						true_fluents.add(RDDL2Format.CleanFluentName(p._sPVarName + gfluent));
					}
				}
			} catch (Exception ex) {
				System.err.println("SPerseusSPUDDPolicy: could not retrieve assignment for " + p + "\n");
			}
		}
				
		return true_fluents;
	}

	public static String getStateDescription(State s) {
		StringBuilder sb = new StringBuilder();
		
		// Go through all variable types (state, interm, observ, action, nonfluent)
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : s._hmTypeMap.entrySet()) {
			
			if (e.getKey().equals("nonfluent"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) {
				sb.append(p + "\n");
				try {
					// Go through all term groundings for variable p
					ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);										
					for (ArrayList<LCONST> gfluent : gfluents)
						sb.append("- " + e.getKey() + ": " + p + 
								(gfluent.size() > 0 ? gfluent : "") + " := " + 
								s.getPVariableAssign(p, gfluent) + "\n");
						
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment " + s + " for " + p + "\n");
				}
			}
		}
				
		return sb.toString();
	}

}
