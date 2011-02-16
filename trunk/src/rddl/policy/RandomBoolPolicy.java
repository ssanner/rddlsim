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
import rddl.translate.RDDL2Format;

public class RandomBoolPolicy extends Policy {
	
	public Random _rand = new Random();

	public RandomBoolPolicy () {
		
	}
	
	public RandomBoolPolicy(String instance_name) {
		super(instance_name);
	}

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = getLegalActionMap(s);
		
		// Return a random action selection
		ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
		String action_taken = actions.get(_rand.nextInt(action_map.size()));
		System.out.println("\n--> Action taken: " + action_taken);
		
		return action_map.get(action_taken);
	}
	
	public TreeMap<String,ArrayList<PVAR_INST_DEF>> getLegalActionMap(State s) 
		throws EvalException {

	ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();

	// Build a map from propositional action names to actions
	// that can be returned from this policy.
	TreeMap<String,ArrayList<PVAR_INST_DEF>> action_map = new TreeMap<String,ArrayList<PVAR_INST_DEF>>();
	//if (ALLOW_NOOP) {
	action_map.put("noop", (ArrayList<PVAR_INST_DEF>)actions.clone());
	//}
	
	for (PVAR_NAME p : s._alActionNames) {
		
		// Get term instantations for that action and select *one*
		ArrayList<ArrayList<LCONST>> inst = s.generateAtoms(p);
			
		boolean passed_constraints = false;
		for (int i = 0; i < inst.size(); i++) {
			ArrayList<LCONST> terms = inst.get(i);
			actions.clear();
			actions.add(new PVAR_INST_DEF(p._sPVarName, new Boolean(true), terms));
			passed_constraints = true;
			try {
				s.checkStateActionConstraints(actions);
			} catch (EvalException e) {
				passed_constraints = false;
			}
			if (passed_constraints)
				action_map.put(RDDL2Format.CleanFluentName(p._sPVarName + terms), 
						(ArrayList<PVAR_INST_DEF>)actions.clone());
		}
	}
	
	return action_map;
}

}
