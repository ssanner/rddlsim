/**
 * RDDL: Implements a fixed policy for a domain with a single boolean action.
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
import util.Permutation;

public class OtherRandomBoolPolicy extends Policy {
	
	public OtherRandomBoolPolicy () {
		
	}
	
	public OtherRandomBoolPolicy(String instance_name) {
		super(instance_name);
	}

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
		
		boolean passed_constraints = false;
		for(int j =0; j< s._alActionNames.size(); j++){
		// Get a random action
			PVAR_NAME p = s._alActionNames.get(j);
			
			// Get term instantations for that action and select *one*
			ArrayList<ArrayList<LCONST>> inst = s.generateAtoms(p);
			int[] index_permutation = Permutation.permute(inst.size(), _random);
			
			for (int i = 0; i < index_permutation.length; i++) {
				ArrayList<LCONST> terms = inst.get(index_permutation[i]);
				
				actions.add(new PVAR_INST_DEF(p._sPVarName, new Boolean(true), terms));
				passed_constraints = true;
				try {
					s.checkStateActionConstraints(actions);
				} catch (EvalException e) {
					passed_constraints = false;
					System.out.println(actions + " : " + e);
				}
				if(!passed_constraints)
					actions.remove(actions.size()-1); 
				if (actions.size() == 3)
					break;
			}
			if(actions.size()==3)
				break;
		}
			// Check if no single action passed constraint
//			if (!passed_constraints) {
//				// Try empty action
//				actions.clear();
//				try {
//					s.checkStateActionConstraints(actions);
//				} catch (EvalException e) {
//					System.out.println(actions + " : " + e);
//					throw new EvalException("No actions (even empty) satisfied state constraints!");
//				}
//			}
		
		// Test maxNondefActions by setting all actions to true
		//actions.clear();
		//for (int i = 0; i < inst.size(); i++) {
		//	actions.add(new PVAR_INST_DEF(p._sPVarName, new Boolean(true), inst.get(i)));
		//}
		
		// Generate the action list
		return actions;
	}

}
