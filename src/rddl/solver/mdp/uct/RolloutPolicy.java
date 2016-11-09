package rddl.solver.mdp.uct;

import java.math.*;
import java.util.*;

import rddl.*;
import rddl.RDDL.*;
import rddl.policy.*;

public class RolloutPolicy extends EnumerableStatePolicy {

	public RolloutPolicy(EnumerableStatePolicy policy) {
		this(policy, null);
	}
	
	private RolloutPolicy(EnumerableStatePolicy policy, HashMap<BigInteger, String> internalPolicy) {
		super(policy);
		
		if (internalPolicy != null)
			this.internalPolicy = new HashMap<BigInteger, String>(internalPolicy);
		else
			this.internalPolicy = new HashMap<BigInteger, String>();
	}
	
	private HashMap<BigInteger, String> internalPolicy = null;
	
	public void changePolicy(State s, String action) {
		BigInteger stateAsInteger = this.getStateLabel(s);
		this.internalPolicy.put(stateAsInteger, action);
	}

	@Override
	public String getBestAction(State s) throws EvalException {
		BigInteger stateAsInteger = this.getStateLabel(s);
		  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = ActionGenerator.getLegalBoolActionMap(s);
		
		if (!this.internalPolicy.containsKey(stateAsInteger)) {
			ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
			this.internalPolicy.put(stateAsInteger, actions.get(this._random.nextInt(0, action_map.size() - 1)));
		}
		
		return this.internalPolicy.get(stateAsInteger);
	}
	
	protected RolloutPolicy copy() {
		return new RolloutPolicy(this, this.internalPolicy);
	}
	
	public double rollOut(State s, int horizon) {
		/* Executing a RollOut will modify the state, you must create a copy before calling this method */
		double accum_reward = 0.0d;
		double cur_discount = 1.0;
		
		try {
			for (int t = horizon; t > 0; t--) {
				Map<String,ArrayList<PVAR_INST_DEF>> action_map = ActionGenerator.getLegalBoolActionMap(s);
				String selectedAction = this.getBestAction(s);
				
				ArrayList<PVAR_INST_DEF> action_list = action_map.get(selectedAction);
				
				s.checkStateActionConstraints(action_list);
				s.computeNextState(action_list, this._random);
				
				double reward = ((Number) s._reward.sample(new HashMap<LVAR,LCONST>(), s, this._random)).doubleValue();
				
				accum_reward += cur_discount * reward;
				cur_discount *= this.getDiscountFactor();
				
				// Done with this iteration, advance to next round
				s.advanceNextState(false);
			}
		} catch (EvalException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return accum_reward;
	}
}
