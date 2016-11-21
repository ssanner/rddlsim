package rddl.policy.domain.navigation;

import java.util.ArrayList;
import java.util.Random;

import rddl.EvalException;
import rddl.RDDL;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;
import rddl.policy.Policy;
import util.Permutation;
import java.lang.Math;

public class FixNavigationPolicy extends Policy{
	public int MAX_CONCURRENT_ACTIONS = 20;
	public FixNavigationPolicy () { 
		super();
	}
	
	public FixNavigationPolicy(String instance_name) {
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
	
	private Object getReasonableValue(State s, TYPE_NAME _typeRange, ArrayList<LCONST> terms) throws EvalException {
		double min_bound=0;
		double max_bound=0;
		double location=0;
		double goal=0;
		double output=0;
		
		if(s.getPVariableAssign(new PVAR_NAME("MINACTIONBOUND"), terms) instanceof Double){
			min_bound = (double) s.getPVariableAssign(new PVAR_NAME("MINACTIONBOUND"), terms);
		}else{
			min_bound = (double)(int) s.getPVariableAssign(new PVAR_NAME("MINACTIONBOUND"), terms);
		}
		if(s.getPVariableAssign(new PVAR_NAME("MAXACTIONBOUND"), terms) instanceof Double){
			max_bound = (double) s.getPVariableAssign(new PVAR_NAME("MAXACTIONBOUND"), terms);
		}else{
			max_bound = (double)(int) s.getPVariableAssign(new PVAR_NAME("MAXACTIONBOUND"), terms);
		}
		if( s.getPVariableAssign(new PVAR_NAME("location"), terms) instanceof Double ){
			location = (double) s.getPVariableAssign(new PVAR_NAME("location"), terms);
		}else{
			location = (double)(int) s.getPVariableAssign(new PVAR_NAME("location"), terms);
		}
		
		if( s.getPVariableAssign(new PVAR_NAME("GOAL"), terms) instanceof Double ){
			goal = (double) s.getPVariableAssign(new PVAR_NAME("GOAL"), terms);
		}else{
			goal = (double)(int) s.getPVariableAssign(new PVAR_NAME("GOAL"), terms);
		}
		
		if(Math.abs(goal-location)>=1){
			output=1;
		}else{
			output=goal-location;
		}
		
		if(output>max_bound){output=max_bound;}
		if(output<min_bound){output=min_bound;}
		return output;
	
	}
}
