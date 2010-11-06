/**
 * RDDL: Implements a random policy for a domain with  a single enum action
 *       The enum values are passed into the constructor as strings.
 * 
 * @author Tom Walsh (thomasjwalsh@gmail.com)
 * @version 10/15/10
 *
 **/

package rddl.policy;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;

public class RandomEnumPolicy extends Policy {
	
	public RandomEnumPolicy () {
		
	}
	
	public RandomEnumPolicy(String instance_name) {
		super(instance_name);
	}

	// TODO: does not necessarily respect state constraints, so may generate
	//       illegal actions, should fix in future
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		// Get an action (assuming all actions are enum type)
		PVAR_NAME p = s._alActionNames.get(new Random().nextInt(s._alActionNames.size()));
		
		// Get list of enum values for this action
		PVARIABLE_DEF pdef = s._hmPVariables.get(p);
		ENUM_TYPE_DEF tdef = (ENUM_TYPE_DEF)s._hmTypes.get(pdef._sRange);
		ArrayList<ENUM_VAL> enums = tdef._alPossibleValues;
		
		// Get term instantations for that action and select *one*
		ArrayList<ArrayList<LCONST>> inst = s.generateAtoms(p);
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
		for(ArrayList<LCONST> terms: inst){
			ENUM_VAL r = enums.get(new Random().nextInt(enums.size()));
			PVAR_INST_DEF d = new PVAR_INST_DEF(p._sPVarName, r, terms);
			actions.add(d);
		}
		
		// Generate the action list
		return actions;
	}

}
