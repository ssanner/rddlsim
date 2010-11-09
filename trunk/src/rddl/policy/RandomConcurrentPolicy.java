/**
 * RDDL: Implements a random policy for a domain with concurrent actions
 *       (allows mixed action types)
 * 
 * @author Tom Walsh (thomasjwalsh@gmail.com)
 * @author Scott Saner (ssanner@gmail.com)
 * @version 11/7/10
 *
 **/

package rddl.policy;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;
import util.Permutation;

public class RandomConcurrentPolicy extends Policy {
		
	public int NUM_CONCURRENT_ACTIONS = 3; // Max number of non-default concurrent actions
	public int MAX_INT_VALUE = 5; // Max int value to use when selecting random action
	public double MAX_REAL_VALUE = 5.0d; // Max real value to use when selecting random action
	
	public RandomConcurrentPolicy () { 
		super();
	}
	
	public RandomConcurrentPolicy(String instance_name) {
		super(instance_name);
	}

	public void setNumConcurrentActions(int num_concurrent) {
		NUM_CONCURRENT_ACTIONS = num_concurrent;
	}
	
	public void setActionMaxIntValue(int max_int_value) {
		MAX_INT_VALUE = max_int_value;
	}
	
	public void setActionMaxRealValue(double max_real_value) {
		MAX_REAL_VALUE = max_real_value; 
	}
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
		ArrayList<PVAR_NAME> action_types = s._hmTypeMap.get("action-fluent");
		
		boolean passed_constraints = false;
		for(int j =0; j< s._alActionNames.size(); j++){
			
			// Get a random action
			PVAR_NAME p = s._alActionNames.get(j);
			PVARIABLE_DEF pvar_def = s._hmPVariables.get(p);
			
			// Get term instantations for that action and select *one*
			ArrayList<ArrayList<LCONST>> inst = s.generateAtoms(p);
			int[] index_permutation = Permutation.permute(inst.size(), _random);
			
			for (int i = 0; i < index_permutation.length; i++) {
				ArrayList<LCONST> terms = inst.get(index_permutation[i]);
				
				// IMPORTANT: get random assignment that matches action type
				Object value = null;
				if (pvar_def._sRange.equals(RDDL.TYPE_NAME.BOOL_TYPE)) {
					// bool
					value = new Boolean(true);
				} else if (pvar_def._sRange.equals(RDDL.TYPE_NAME.INT_TYPE)) {
					// int
					value = new Integer(_random.nextInt(MAX_INT_VALUE));
				} else if (pvar_def._sRange.equals(RDDL.TYPE_NAME.REAL_TYPE)) {
					// real
					value = new Double(_random.nextDouble() * MAX_REAL_VALUE);
				} else {
					// enum: only other option for a range
					ENUM_TYPE_DEF enum_type_def = 
						(ENUM_TYPE_DEF)s._hmTypes.get(pvar_def._sRange);
					int rand_index = _random.nextInt(enum_type_def._alPossibleValues.size());
					
					value = enum_type_def._alPossibleValues.get(rand_index);
				}
	
				// Now set the action
				actions.add(new PVAR_INST_DEF(p._sPVarName, value, terms));
				passed_constraints = true;
				try {
					s.checkStateActionConstraints(actions);
				} catch (EvalException e) { 
					// Got an eval exception, constraint violated
					passed_constraints = false;
					//System.out.println(actions + " : " + e);
					//System.out.println(s);
					//System.exit(1);
				} catch (Exception e) { 
					// Got a real exception, something is wrong
					System.out.println("\nERROR evaluating constraint on action set: " + 
							actions + /*"\nConstraint: " +*/ e + "\n");
					e.printStackTrace();
					throw new EvalException(e.toString());
				}
				if(!passed_constraints)
					actions.remove(actions.size()-1); 
				if (actions.size() == NUM_CONCURRENT_ACTIONS)
					break;
			}
			if(actions.size() == NUM_CONCURRENT_ACTIONS)
				break;
		}

		// Check if no single action passed constraint
		if (!passed_constraints) {
			// Try empty action
			passed_constraints = true;
			actions.clear();
			try {
				s.checkStateActionConstraints(actions);
			} catch (EvalException e) {
				passed_constraints = false;
				System.out.println(actions + " : " + e);
				throw new EvalException("No actions (even a) satisfied state constraints!");
			}
		}
				
		// Return the action list
		//System.out.println("**Action: " + actions);
		return actions;
	}

}
