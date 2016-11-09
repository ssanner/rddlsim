package rddl.policy.domain.inventory;

import java.util.ArrayList;
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

public class StochasticInventoryPolicy extends Policy{
	public int MAX_CONCURRENT_ACTIONS = 20;
	public StochasticInventoryPolicy () { 
		super();
	}
	
	public StochasticInventoryPolicy(String instance_name) {
		super(instance_name);
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
		if (s == null) return actions; 		
		int num_concurrent_actions = Math.min(MAX_CONCURRENT_ACTIONS, s._nMaxNondefActions);
		ArrayList<PVAR_NAME> action_types = s._hmTypeMap.get("action-fluent");
		boolean passed_constraints = false;
		
		int current_capacity = (int)s.getPVariableAssign(new PVAR_NAME("MAX_INVENTORY"), new ArrayList<LCONST>());
		int occupation = 0;
		try{
			occupation=(int)s.getPVariableAssign(new PVAR_NAME("after_demand_sum"), new ArrayList<LCONST>());
		}catch(NullPointerException e){
		}
		current_capacity-=occupation;
		
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
				
				value = getReasonableValue(s, action_def._typeRange, terms,current_capacity);
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
	
	private Object getReasonableValue(State s, TYPE_NAME _typeRange, ArrayList<LCONST> terms, int Capacity) throws EvalException {
		Random rand = new Random();
		int value = rand.nextInt(Capacity);
		Capacity-=value;
		return value;
	}
}
