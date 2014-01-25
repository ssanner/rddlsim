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
		
	public int MAX_CONCURRENT_ACTIONS = 20; // Since we could have: max-nondef-actions = pos-inf;
	public int MAX_INT_VALUE = 5; // Max int value to use when selecting random action
	public double MAX_REAL_VALUE = 5.0d; // Max real value to use when selecting random action
	
	public RandomConcurrentPolicy () { 
		super();
	}
	
	public RandomConcurrentPolicy(String instance_name) {
		super(instance_name);
	}
	
	public void setActionMaxIntValue(int max_int_value) {
		MAX_INT_VALUE = max_int_value;
	}
	
	public void setActionMaxRealValue(double max_real_value) {
		MAX_REAL_VALUE = max_real_value; 
	}
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();

		if (s == null) return actions; 
		
		int num_concurrent_actions = Math.min(MAX_CONCURRENT_ACTIONS, s._nMaxNondefActions);
		//System.out.println("Allowing maximum " + num_concurrent_actions + " concurrent actions.");
		
		ArrayList<PVAR_NAME> action_types = s._hmTypeMap.get("action-fluent");
		
		boolean passed_constraints = false;
		for(int j =0; j< s._alActionNames.size(); j++){
			
			// Get a random action
			PVAR_NAME p = s._alActionNames.get(j);
			PVARIABLE_DEF pvar_def = s._hmPVariables.get(p);
			
			// Get term instantations for that action and select *one*
			ArrayList<ArrayList<LCONST>> inst = s.generateAtoms(p);
			//System.out.println("Legal instances for " + p + ": " + inst);
			int[] index_permutation = Permutation.permute(inst.size(), _random);
			
			for (int i = 0; i < index_permutation.length; i++) {
				ArrayList<LCONST> terms = inst.get(index_permutation[i]);
				
				// IMPORTANT: get random assignment that matches action type
				RDDL.PVARIABLE_ACTION_DEF action_def = (RDDL.PVARIABLE_ACTION_DEF)pvar_def;
				
				// Flip a coin to decide whether to execute the default value or not
				// ... this allows actions with names occurring later in the outer
				//     for loop to have a chance if there are action constraints
				Object value = null;
				if (_random.nextUniform(0d, 1d) < 0.5d) {
					continue;
					//System.out.println("DEFAULT");
					//value = action_def._oDefValue;
				} else { 
					//System.out.println("NOT DEFAULT");
					value = getRandomValue(s, action_def._typeRange);
				}
	
				// Now set the action
				actions.add(new PVAR_INST_DEF(p._sPVarName, value, terms));
				try {
					s.checkStateActionConstraints(actions);
				} catch (EvalException e) { 
					// Got an eval exception, constraint violated
					actions.remove(actions.size()-1); 
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
				if (actions.size() == num_concurrent_actions)
					break;
			}
			if(actions.size() == num_concurrent_actions)
				break;
		}

		// If noop, potentially no action was legal so should check that noop is legal
		if (actions.size() == 0) {
			// Try empty action
			actions.clear();
			try {
				s.checkStateActionConstraints(actions);
			} catch (EvalException e) {
				System.out.println(actions + " : " + e);
				throw new EvalException("No actions satisfied state constraints, not even noop!");
			}
		}
				
		// Return the action list
		System.out.println("**Action: " + actions);
		return actions;
	}

	// Recursive?
	public Object getRandomValue(State s, TYPE_NAME type) throws EvalException {

		if (type.equals(RDDL.TYPE_NAME.BOOL_TYPE)) {
			// bool
			return new Boolean(true); // Not random: we'll assume false is default so return non-default value 
		} else if (type.equals(RDDL.TYPE_NAME.INT_TYPE)) {
			// int
			return new Integer(_random.nextInt(0, MAX_INT_VALUE));
		} else if (type.equals(RDDL.TYPE_NAME.REAL_TYPE)) {
			// real
			return new Double(_random.nextUniform(-MAX_REAL_VALUE,MAX_REAL_VALUE));
		}  else {
			// a more complex type -- have to retrieve and process
			TYPE_DEF tdef = s._hmTypes.get(type);
			
			if (tdef == null) {
				throw new EvalException("Cannot find type definition for '" + type + "' to generate policy.");
			} else if (tdef instanceof STRUCT_TYPE_DEF) {	
				
				// recursively get values for subtypes
				STRUCT_TYPE_DEF sdef = (STRUCT_TYPE_DEF)tdef;
				STRUCT_VAL sval = new STRUCT_VAL();
				for (STRUCT_TYPE_DEF_MEMBER m : sdef._alMembers) {
					sval.addMember(m._sName, getRandomValue(s, m._type));
				}
				return sval;
				
			} else if (tdef instanceof LCONST_TYPE_DEF) {
				
				// randomly return one of the legal values for this ENUM or OBJECT type
				ArrayList<LCONST> possible_values = ((LCONST_TYPE_DEF)tdef)._alPossibleValues;
				int index = _random.nextInt(0, possible_values.size() - 1);
				return possible_values.get(index);
				
			} else {
				throw new EvalException("Don't know how to sample from '" + type + "' of type '" + tdef + "'.");
			}
		}
	}
}
