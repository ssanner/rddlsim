/**
 * RDDL: Implements a random policy for a domain with a single boolean action.
 *       The action selected to be true is the first ground instance.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.policy;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;

public class RandomBoolPolicy extends Policy {
	
	public Random _rand = new Random();

	public RandomBoolPolicy () {
		
	}
	
	public RandomBoolPolicy(String instance_name) {
		super(instance_name);
	}

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			// when no observations have been generated, for now, we simply
			// send a noop.  An approach that actually reads the domain
			// should probably execute a different legal action since the
			// initial state is assumed known.
			return new ArrayList<PVAR_INST_DEF>();
		}

		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);
		
		// Return a random action selection
		ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
		String action_taken = actions.get(_rand.nextInt(action_map.size()));
		//System.out.println("\n--> Action taken: " + action_taken);
		
		return action_map.get(action_taken);
	}
}
