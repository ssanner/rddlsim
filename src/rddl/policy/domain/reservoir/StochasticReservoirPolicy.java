package rddl.policy.domain.reservoir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import rddl.EvalException;
import rddl.RDDL;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;
import rddl.State;
import rddl.policy.Policy;
import util.Permutation;

public class StochasticReservoirPolicy extends Policy {
	public int MAX_CONCURRENT_ACTIONS = 20;
	public StochasticReservoirPolicy () { 
		super();
	}
	
	public StochasticReservoirPolicy(String instance_name) {
		super(instance_name);
	}

	/* (non-Javadoc)
	 * @see rddl.policy.Policy#getActions(rddl.State)
	 * 
	 * This function should modified that allows to select 4 concurrent actions..
	 */
	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
		if (s == null) return actions; 		
		int num_concurrent_actions = Math.min(MAX_CONCURRENT_ACTIONS, s._nMaxNondefActions);
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
				
				RDDL.PVARIABLE_ACTION_DEF action_def = (RDDL.PVARIABLE_ACTION_DEF)pvar_def;
				
				Object value = null;
				if (terms.get(0).toString().equals("$sea")) {
					continue;
				}  
				
				value = getReasonableValue(s, action_def._typeRange, terms);
				actions.add(new PVAR_INST_DEF(p._sPVarName, value, terms));
				try {
					s.checkStateActionConstraints(actions);
				} catch (EvalException e) { 

					actions.remove(actions.size()-1); 
				} catch (Exception e) { 
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

		if (actions.size() == 0) {
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

	/**
	 * @param s
	 * @param _typeRange
	 * @param terms
	 * @return
	 * @throws EvalException
	 * 
	 * A reasonable value is that
	 * Flow(i) will never bigger than MAXCAP
	 */
	private Object getReasonableValue(State s, TYPE_NAME _typeRange, ArrayList<LCONST> terms) throws EvalException {
		
		HashMap<Integer, LCONST> knownterms=new HashMap<Integer, LCONST>();
		knownterms.put(0, terms.get(0));
		PVAR_NAME p = new PVAR_NAME("DOWNSTREAM");
		ArrayList<ArrayList<LCONST>> possible_terms=null;

		double current_rlevel = (Double) s.getPVariableAssign(new PVAR_NAME("rlevel"), terms);
		double current_high = (Double) s.getPVariableAssign(new PVAR_NAME("HIGH_BOUND"), terms);
		double current_low = (Double) s.getPVariableAssign(new PVAR_NAME("LOW_BOUND"), terms);
		double upper_bound = Math.max(0, current_rlevel-current_low);
		Random random = new Random();
		if(current_rlevel<=current_high){
			return random.nextDouble()*upper_bound;
		}else{
			return 0.5*current_rlevel;
		}
		
	}

}
