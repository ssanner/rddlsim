package rddl.solver.mdp.uct;

import java.math.BigInteger;
import java.util.*;

import dd.discrete.ADD;
import dd.discrete.DD;

import rddl.State;
import rddl.solver.DDUtils;
import rddl.solver.mdp.Action;
import util.CString;
import util.Pair;

public class UCTWithBackupAfterNVisits extends UCT {

	/**
	 * Default constructor.
	 */
	public UCTWithBackupAfterNVisits() { }
	
	/**
	 * Initialize this class with the instance name to be solved by this algorithm. 
	 * @param instance_name Instance name to be solved by this algorithm
	 */
	public UCTWithBackupAfterNVisits(String instance_name) {
		super(instance_name);
	}

	/**
	 * List with an ADD representation of Value function per horizon.
	 */
	private List<Integer> valueADDPerHorizon = null;
	
	/**
	 * Contains data (probabilities, rewards) about each action in domain. 
	 */
	protected HashMap<String, Action> actionData = null; 
	
	/**
	 * Contains all ADDs used by this class.
	 */
	private List<Integer> allADDs = null;
		
	private final double MAX_FREE_MEMORY_ALLOWED = 0.3;
	
	private final int VISITS_THRESHOLD = 30;
	
	/**
	 * Initialize all class attributes.
	 */
	@Override
	public void roundInit(double timeLeft, int horizon, int roundNumber, int totalRounds) {
		super.roundInit(timeLeft, horizon, roundNumber, totalRounds);
		
		if (this.allADDs == null) {
			this.allADDs = new ArrayList<Integer>();
			this.valueADDPerHorizon = new ArrayList<Integer>();
			this.actionData = new HashMap<String, Action>();
			for (String a : this.getTranslation()._hmActionMap.keySet()) {
				HashMap<CString, Integer> cpts = new HashMap<CString, Integer>();
				int reward = this.getTranslation()._context.getConstantNode(0d);

				// Build reward from additive decomposition
				ArrayList<Integer> reward_summands = this.getTranslation()._act2rewardDD.get(a);

				for (int summand : reward_summands)
					reward = this.getTranslation()._context.applyInt(reward, summand, ADD.ARITH_SUM);

				this.allADDs.add(reward);

				// Build CPTs
				for (String s : this.getTranslation()._alStateVars) {
					int dd = this.getTranslation()._var2transDD.get(new Pair(a,s));

					int dd_true = this.getTranslation()._context.getVarNode(s + "'", 0d, 1d);
					dd_true = this.getTranslation()._context.applyInt(dd_true, dd, ADD.ARITH_PROD);

					int dd_false = this.getTranslation()._context.getVarNode(s + "'", 1d, 0d);
					int one_minus_dd = this.getTranslation()._context.applyInt(this.getTranslation()._context.getConstantNode(1d), dd, ADD.ARITH_MINUS);
					dd_false = this.getTranslation()._context.applyInt(dd_false, one_minus_dd, ADD.ARITH_PROD);

					// Now have "dual action diagram" cpt DD
					int cpt = this.getTranslation()._context.applyInt(dd_true, dd_false, ADD.ARITH_SUM);

					cpts.put(new CString(s + "'"), cpt);
					this.allADDs.add(cpt);
				}

				// Build Action and add to HashMap
				CString action_name = new CString(a);
				Action action = new Action(this.getTranslation()._context, action_name, cpts, reward);
				this.actionData.put(a, action);
			}
			
			double rewardRange = 0d;
			double maxReward = Double.NEGATIVE_INFINITY;
			
			for (Action a : this.actionData.values()) {
				rewardRange = Math.max(rewardRange, 
						this.getTranslation()._context.getMaxValue(a._reward) - this.getTranslation()._context.getMinValue(a._reward));
				
				maxReward = Math.max(maxReward, this.getTranslation()._context.getMaxValue(a._reward));
			}
			
			this.C = rewardRange;
			
			for (int i = 0; i < horizon; i++) {
				double reward = maxReward * (i + 1);

				int valueADD = this.getTranslation()._context.getConstantNode(reward);
				this.valueADDPerHorizon.add(valueADD);
			}
		}		
	}
	
	/**
	 * Logic of the Monte Carlo Tree search algorithm.
	 */
	protected Pair<Integer, Long> buildSearchTree(State s, long timeout) {
		int completedSearches = 0;
		
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;
		
		do {		
			this.search(s, this.getRemainingHorizons());
			
			completedSearches++;
			elapsedTime = System.currentTimeMillis() - startTime;
		} while (elapsedTime < timeout);
			
		this.flushCaches();
		
		return new Pair<Integer, Long>(completedSearches, elapsedTime);
	}
	
	/**
	 * Execute and expands the Monte Carlo Search Tree.
	 */
	protected double search(State state, int remainingHorizons) {	
		if (remainingHorizons == 0) return 0.0;
		
		BigInteger stateAsNumber = this.getStateLabel(state);
		
		if (isLeaf(stateAsNumber, remainingHorizons)) 
			return this.evaluate(state, stateAsNumber, remainingHorizons);
		
		String action = this.selectAction(stateAsNumber, remainingHorizons);
		
		Pair<State, Double> simulationResult = this.simulateSingleAction(state, action);
		
		State nextState = simulationResult._o1;
		Double reward = simulationResult._o2;
		
		int visits = 0;
		
		HashMap<BigInteger, Integer> visitsPerState = this.visitsPerHorizon.get(remainingHorizons - 1);
		
		if (visitsPerState.containsKey(stateAsNumber))
			visits = visitsPerState.get(stateAsNumber);
		
		double q = 0.0;
		
		if (visits > VISITS_THRESHOLD) {
			this.search(nextState, remainingHorizons - 1);
			q = this.computeBellmanBackup(state, action, remainingHorizons); 
		}
		else {
			q = reward + this.getDiscountFactor() * this.search(nextState, remainingHorizons - 1);
		}
		
		updateValue(stateAsNumber, action, q, remainingHorizons);
		
		return q;
	}

	/**
	 * Update all internal tables with search results including the ADD list.
	 */
	@Override
	protected void updateValue(BigInteger stateAsNumber, String action, double q, int remainingHorizons) {
		super.updateValue(stateAsNumber, action, q, remainingHorizons);
		
		//select the best action reward
		Pair<String, Double> result = this.getUCTBestAction(stateAsNumber, remainingHorizons, 0.0);
		q = result._o2;
		
		int valueADD = this.valueADDPerHorizon.get(remainingHorizons - 1);
		
		ArrayList<Boolean> factoredState = this.getVariableValues(stateAsNumber);
		
		valueADD = DDUtils.UpdateValue(this.getTranslation()._context, valueADD, factoredState, q);
		
		this.valueADDPerHorizon.set(remainingHorizons - 1, valueADD);
	}
	
	/**
	 * Compute a Bellman Backup in a state given a horizon.
	 * @return Best reward obtained in this state.
	 */
	private double computeBellmanBackup(State state, String action, int horizon) {
		
		ArrayList<Boolean> factoredState = this.getVariableValues(state);
		
		int prime_vfun = -1;
		
		if (horizon >= 2) { //get value to all horizons except the last. In last horizon the expected reward must be 0 (zero).
			int valueADD = this.valueADDPerHorizon.get(horizon - 2);
			prime_vfun = this.getTranslation()._context.remapGIDsInt(valueADD, this.getTranslation()._hmPrimeRemap);
		}
		
		Action a = this.actionData.get(action);
		
		return getQValue(prime_vfun, factoredState, a);
	}
	
	public double getQValue(int prime_vfun, ArrayList<Boolean> cur_state, Action a) {
		ArrayList<Boolean> stateAsQuery = new ArrayList<Boolean>(cur_state);
		for (int i = 0; i < cur_state.size(); i++) stateAsQuery.add(null);
		
		double expectedReward = 0.0;
		
		// Get CPT for each next state variable, restrict out current
		// state and multiply into dd_ret... result should be a constant
		// that is discounted, added to reward and returned

		if (prime_vfun > 0) {
			// Find what gids (ADD level assignments of variables) are currently in vfun
			Set vfun_gids = this.getTranslation()._context.getGIDs(prime_vfun);
			
			// For each next-state variable in DBN for action 'a'
			for (Map.Entry<Integer, Integer> me : a._hmVarID2CPT.entrySet()) {

				int head_var_gid = me.getKey();
				int cpt_dd = me.getValue();

				// No need to regress variables not in the value function  
				if (!vfun_gids.contains(head_var_gid))
					continue;

				// For each CPT, evaluate in current state for next state variable true
				int level_prime = (Integer) this.getTranslation()._context._hmGVarToLevel.get(head_var_gid);
				stateAsQuery.set(level_prime, true);

				double prob_true = this.getTranslation()._context.evaluate(cpt_dd, stateAsQuery);

				if (Double.isNaN(prob_true)) {
					throw new RuntimeException("ERROR in getQValue: Expected single value when evaluating: " + stateAsQuery);
				}

				stateAsQuery.set(level_prime, null); // Undo so as not to change current_state
				int restricted_cpt_dd = this.getTranslation()._context.getVarNode(head_var_gid, 1d - prob_true, prob_true);

				// Multiply next state variable DBN into current value function
				prime_vfun = this.getTranslation()._context.applyInt(prime_vfun, restricted_cpt_dd, DD.ARITH_PROD);

				// Sum out next state variable
				prime_vfun = this.getTranslation()._context.opOut(prime_vfun, head_var_gid, DD.ARITH_SUM);
			}
			
			// Get action-dependent reward
			expectedReward = this.getTranslation()._context.evaluate(prime_vfun, (ArrayList) null);
		}
		
		double reward = this.getTranslation()._context.evaluate(a._reward, stateAsQuery);
		
		return reward + this.getDiscountFactor() * expectedReward;
	}

	private void flushCaches() {
		Runtime runtime = Runtime.getRuntime();
		
		double freeMemoryPercentage = ((double) runtime.freeMemory() / (double) runtime.totalMemory());
		
		if (freeMemoryPercentage > MAX_FREE_MEMORY_ALLOWED) 
			return; // Still enough free mem to exceed minimum requirements

		this.getTranslation()._context.clearSpecialNodes();

		for (Integer dd : this.allADDs)
			this.getTranslation()._context.addSpecialNode(dd);
		
		for (Integer valueADD : this.valueADDPerHorizon) 
			this.getTranslation()._context.addSpecialNode(valueADD);	

		this.getTranslation()._context.flushCaches(false);
	}
}

