/**
 * UCT implementation, based in paper "Bandit based Monte-Carlo Planning" and in the ICAPS 2010 
 * presentation "Monte-Carlo Planning: Basic Principles and Recent Progress"
 * 
 * @author Daniel Dias (dbdias at ime.usp.br) and Luis Rocha ( ludygrv at cecm.usp.br )
 * @version 2011-11-13
 *
 * The original UCT paper is here:
 *    L. Kocsis and C. Szepesvári.
 *    Bandit based Monte-Carlo Planning.
 *    ECML 2006.
 * 
 * The ICAPS 2010 presentation is here:
 *    A. Fern
 *    Monte-Carlo Planning: Basic Principles and Recent Progress
 *    http://videolectures.net/icaps2010_fern_mcpbprp/
 **/
package rddl.solver.mdp.uct;

import java.math.*;
import java.util.*;

import rddl.*;
import rddl.RDDL.*;
import rddl.policy.*;
import util.*;

/**
 * Implements the UCT algorithm.
 */
public class UCT extends EnumerableStatePolicy {

	/**
	 * Default constructor.
	 */
	public UCT() { }
	
	/**
	 * Initialize this class with the instance name to be solved by this algorithm. 
	 * @param instance_name Instance name to be solved by this algorithm
	 */
	public UCT(String instance_name) {
		super(instance_name);
	}
	
	protected final int TIMEOUT_ORDER = 1000; //1000 milliseconds
	protected int TIMEOUT = 160 * TIMEOUT_ORDER; 
	//private final int TIMEOUT = 600 * TIMEOUT_ORDER; //10 minutes used for debug purposes
	
	/**
	 * Constant used in UCB to increase/decrease exploration bias.
	 */
	protected double C = 1.0;
	
	protected List<HashMap<BigInteger, HashMap<String, Double>>> rewardsPerHorizon = null;
	protected List<HashMap<BigInteger, HashMap<String, Integer>>> pullsPerHorizon = null;
	protected List<HashMap<BigInteger, Integer>> visitsPerHorizon = null;
	
	/**
	 * Returns the best action given a state.
	 */
	@Override
	protected String getBestAction(State s) {
		
		BigInteger stateAsNumber = this.getStateLabel(s);
		
		long timeout = this.getTimePerAction();
		
		Pair<Integer, Long> searchResult = buildSearchTree(s, timeout); 
		
		int completedSearches = searchResult._o1;
		long elapsedTime = searchResult._o2;
		
		//choose an action without the exploration bias
		Pair<String, Double> result = this.getUCTBestAction(stateAsNumber, this.getRemainingHorizons(), 0.0); 
		String action = result._o1;
		double reward = result._o2;
		
		//Get the Search Tree Depth only to debug purposes
		int searchTreeDepth = 0;
		
		for (int i = 0; i < this.rewardsPerHorizon.size(); i++) {
			if (this.rewardsPerHorizon.get(i).size() > 0) {
				searchTreeDepth = this.rewardsPerHorizon.size() - i;
				break;
			}
		}
		
		System.out.printf("Action: [%s] selected with reward [%f] after [%d] searches in [%f] seconds. Search tree depth: [%d]", 
				action, reward, completedSearches, ((double) elapsedTime) / TIMEOUT_ORDER, searchTreeDepth);
		System.out.println();
		
		return action;
	}

	protected Pair<Integer, Long> buildSearchTree(State s, long timeout) {
		int completedSearches = 0;
		
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;
		
		do {
			this.search(s, this.getRemainingHorizons());
			
			completedSearches++;
			elapsedTime = System.currentTimeMillis() - startTime;
		} while (elapsedTime < timeout);
		
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
		double reward = simulationResult._o2;
		
		double q = reward + this.getDiscountFactor() * this.search(nextState, remainingHorizons - 1);
		
		updateValue(stateAsNumber, action, q, remainingHorizons);
		
		return q;
	}

	/**
	 * Update all internal tables with search results.
	 */
	protected void updateValue(BigInteger stateAsNumber, String action, double q, int remainingHorizons) {
		HashMap<BigInteger, HashMap<String, Double>> rewards = this.rewardsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, HashMap<String, Integer>> pulls = this.pullsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, Integer> visits = this.visitsPerHorizon.get(remainingHorizons - 1);
		
		if (!rewards.containsKey(stateAsNumber))
			rewards.put(stateAsNumber, new HashMap<String, Double>());
		
		if (!pulls.containsKey(stateAsNumber))
			pulls.put(stateAsNumber, new HashMap<String, Integer>());
		
		HashMap<String, Double> rewardsForAction = rewards.get(stateAsNumber);
		HashMap<String, Integer> pullsForAction = pulls.get(stateAsNumber);
		
		//Count visits
		int visitCount = 1;
		
		if (visits.containsKey(stateAsNumber))
			visitCount += visits.get(stateAsNumber);
		
		visits.put(stateAsNumber, visitCount);
		
		//Count pulls
		int pullCount = 1;
		
		if (pullsForAction.containsKey(action))
			pullCount += pullsForAction.get(action);
		
		pullsForAction.put(action, pullCount);
		
		//Increment average
		double average = 0.0;
		
		if (rewardsForAction.containsKey(action))
			average = rewardsForAction.get(action);
		
		average = ((q - average) / pullCount) + average;
		
		rewardsForAction.put(action, average);
	}

	/**
	 * Verify if a state is a leaf of the search tree.
	 */
	protected boolean isLeaf(BigInteger state, int remainingHorizons) {
		HashMap<BigInteger, HashMap<String, Double>> rewards = this.rewardsPerHorizon.get(remainingHorizons - 1);
		if (!rewards.containsKey(state)) return true;
		
		HashMap<String, Double> actionRewards = rewards.get(state);
		return (actionRewards == null || actionRewards.keySet().size() < this.getActions().size());
	}

	/**
	 * Evaluate a leaf, executing a policy rollout in a random policy.
	 */
	protected double evaluate(State state, BigInteger stateAsNumber, int remainingHorizons) {
		HashMap<String, Double> rewards = this.rewardsPerHorizon.get(remainingHorizons - 1).get(stateAsNumber);
		
		String action = null;
		
		//Find an action to execute
		for (CString possibleAction : this.getActions()) {
			if (rewards == null || !rewards.containsKey(possibleAction._string)) {
				action = possibleAction._string;
				break;
			}
		}
		
		if (action == null) //if tested all actions, choose a random action 
			action = this.getActions().get(_random.nextInt(0, this.getActions().size() - 1))._string;
		
		Pair<State, Double> simulationResult = this.simulateSingleAction(state, action);
		
		//Simulate the next steps
		double q = simulationResult._o2 + this.getDiscountFactor() * this.simulateRandomPolicy(simulationResult._o1, remainingHorizons - 1);
				
		updateValue(stateAsNumber, action, q, remainingHorizons);
		
		return q;
	}

	/**
	 * Simulate a random policy given an initial state and the remaining horizons.
	 */
	protected double simulateRandomPolicy(State state, int remainingHorizons) {
		double policyReward = 0;
		
		for (int h = remainingHorizons; h > 0; h--) {
			//get a random action
			String action = this.getActions().get(_random.nextInt(0, this.getActions().size() - 1))._string;
			
			Pair<State, Double> simulationResult = this.simulateSingleAction(state, action);
			policyReward += this.getDiscountFactor() * simulationResult._o2;
		}
		
		return policyReward;
	}
	
	/**
	 * Sample an action to execute.
	 */
	protected String selectAction(BigInteger state, int remainingHorizons) {
		HashMap<BigInteger, HashMap<String, Double>> rewards = this.rewardsPerHorizon.get(remainingHorizons - 1);
		
		//select an unused action
		for (CString action : this.getActions()) {
			if (!rewards.get(state).containsKey(action._string))
				return action._string;
		}
		
		//select an action by UCB if tested all actions once
		Pair<String, Double> result = this.getUCTBestAction(state, remainingHorizons); 
		return result._o1;
	}
	
	/**
	 * Simulate a state transition. (based in Simulator.run method)
	 */
	protected Pair<State, Double> simulateSingleAction(State state, String action) {
		
		State s = cloneState(state);
		double reward = 0.0;
		
		try {
			Map<String,ArrayList<PVAR_INST_DEF>> action_map = ActionGenerator.getLegalBoolActionMap(s);
			
			ArrayList<PVAR_INST_DEF> action_list = action_map.get(action);
			
			// Check state-action constraints
			s.checkStateActionConstraints(action_list);
			
			// Compute next state (and all intermediate / observation variables)
			s.computeNextState(action_list, this._random);
			
			// Calculate reward / objective and store
			reward = ((Number) s._reward.sample(new HashMap<LVAR,LCONST>(), s, this._random)).doubleValue();
			
			s.advanceNextState(false);
		} catch (EvalException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		return new Pair<State, Double>(s, reward);
	}
	
	/**
	 * Apply the UCB algorithm to choose an action.
	 */
	protected Pair<String, Double> getUCTBestAction(BigInteger stateAsNumber, int remainingHorizons) {
		return this.getUCTBestAction(stateAsNumber, remainingHorizons, this.C);
	}
	
	/**
	 * Apply the UCB algorithm to choose an action.
	 */
	protected Pair<String, Double> getUCTBestAction(BigInteger stateAsNumber, int remainingHorizons, double biasModifier) {
		HashMap<BigInteger, HashMap<String, Double>> rewards = this.rewardsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, Integer> visits = this.visitsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, HashMap<String, Integer>> pulls = this.pullsPerHorizon.get(remainingHorizons - 1);
		
		String bestAction = null;
		double bestActionReward = Double.NEGATIVE_INFINITY;		
		int stateOccurrences = 0;
		
		if (visits.containsKey(stateAsNumber))
			stateOccurrences = visits.get(stateAsNumber);
		
		for (CString a : this.getActions()) {
			String action = a._string;
			double averageReward = Double.NEGATIVE_INFINITY; 
			
			if (rewards.containsKey(stateAsNumber) && rewards.get(stateAsNumber).containsKey(action)) 
				averageReward = rewards.get(stateAsNumber).get(action);
			
			int pull = 0;
			
			if (pulls.containsKey(stateAsNumber) && pulls.get(stateAsNumber).containsKey(action)) 
				pull = pulls.get(stateAsNumber).get(action);
			
			double bias = 0.0;
			
			if (pull != 0 && stateOccurrences != 0)
				bias = biasModifier * Math.sqrt(Math.log(stateOccurrences) / pull);
			
			double rewardWithRegret = averageReward + bias;
			
			if (rewardWithRegret > bestActionReward) {
				bestActionReward = rewardWithRegret;
				bestAction = action;
			}
		}
		
		return new Pair<String, Double>(bestAction, bestActionReward);
	}
	
	/**
	 * Clone a state to use in simulation.
	 */
	private State cloneState(State currentState) {
		
		State s = new State();
		
		s._hmPVariables = currentState._hmPVariables;
		s._hmTypes = currentState._hmTypes;
		s._hmCPFs = currentState._hmCPFs;
		
		s._hmObject2Consts = new HashMap<RDDL.TYPE_NAME, ArrayList<LCONST>>(currentState._hmObject2Consts);
		
		s._alStateNames = new ArrayList<RDDL.PVAR_NAME>(currentState._alStateNames);
		s._alActionNames = new ArrayList<RDDL.PVAR_NAME>(currentState._alActionNames);
		s._tmIntermNames = new TreeMap<Pair, RDDL.PVAR_NAME>(currentState._tmIntermNames);
		s._alIntermNames = new ArrayList<RDDL.PVAR_NAME>(currentState._alIntermNames);
		s._alObservNames = new ArrayList<RDDL.PVAR_NAME>(currentState._alObservNames);
		s._alNonFluentNames = new ArrayList<RDDL.PVAR_NAME>(currentState._alNonFluentNames);
		
		s._hmTypeMap = new HashMap<String, ArrayList<PVAR_NAME>>();
		for (String key : currentState._hmTypeMap.keySet()) {
			ArrayList<PVAR_NAME> value = currentState._hmTypeMap.get(key);
			s._hmTypeMap.put(key, new ArrayList<RDDL.PVAR_NAME>(value));
		}
		
		s._state = new HashMap<RDDL.PVAR_NAME, HashMap<ArrayList<LCONST>,Object>>();
		for (PVAR_NAME key : currentState._state.keySet()) {
			HashMap<ArrayList<LCONST>,Object> value = currentState._state.get(key);
			s._state.put(key, new HashMap<ArrayList<LCONST>, Object>(value));
		} 
		
		s._nonfluents = new HashMap<RDDL.PVAR_NAME, HashMap<ArrayList<LCONST>,Object>>();
		for (PVAR_NAME key : currentState._nonfluents.keySet()) {
			HashMap<ArrayList<LCONST>,Object> value = currentState._nonfluents.get(key);
			s._nonfluents.put(key, new HashMap<ArrayList<LCONST>, Object>(value));
		}
		
		s._actions = new HashMap<RDDL.PVAR_NAME, HashMap<ArrayList<LCONST>,Object>>();
		for (PVAR_NAME key : currentState._actions.keySet()) {
			HashMap<ArrayList<LCONST>,Object> value = currentState._actions.get(key);
			s._actions.put(key, new HashMap<ArrayList<LCONST>, Object>(value));
		} 
		
		s._interm = new HashMap<RDDL.PVAR_NAME, HashMap<ArrayList<LCONST>,Object>>();
		for (PVAR_NAME key : currentState._interm.keySet()) {
			HashMap<ArrayList<LCONST>,Object> value = currentState._interm.get(key);
			s._interm.put(key, new HashMap<ArrayList<LCONST>, Object>(value));
		}
		
		s._observ = new HashMap<RDDL.PVAR_NAME, HashMap<ArrayList<LCONST>,Object>>();
		for (PVAR_NAME key : currentState._observ.keySet()) {
			HashMap<ArrayList<LCONST>,Object> value = currentState._observ.get(key);
			s._observ.put(key, new HashMap<ArrayList<LCONST>, Object>(value));
		}
		
		s._alActionPreconditions = currentState._alActionPreconditions;
		s._alStateInvariants = currentState._alStateInvariants;
		s._reward = currentState._reward;
		s._nMaxNondefActions = currentState._nMaxNondefActions;
		
		s._nextState = new HashMap<RDDL.PVAR_NAME, HashMap<ArrayList<LCONST>,Object>>();
		for (PVAR_NAME key : currentState._nextState.keySet()) {
			HashMap<ArrayList<LCONST>,Object> value = currentState._nextState.get(key);
			s._nextState.put(key, new HashMap<ArrayList<LCONST>, Object>(value));
		}
		
		return s;
	}
	
	/**
	 * Gets an amount of time to execute an single action in getBestAction method.
	 * @return Amount of time to execute an single action in getBestAction method.
	 */
	private long getTimePerAction() {
		int t = this.getRemainingHorizons();
		int n = this.getTotalHorizons();
		
		double s = n * (n+1) * (2*n + 1) / 6;
		
		double timeShare = t * t / s;
		
		return (long) (TIMEOUT * timeShare);
	}
	
	/**
	 * Initialize all class attributes.
	 */
	@Override
	public void roundInit(double timeLeft, int horizon, int roundNumber, int totalRounds) {
		super.roundInit(timeLeft, horizon, roundNumber, totalRounds);
		
		if (this.rewardsPerHorizon == null) {
			this.rewardsPerHorizon = new ArrayList<HashMap<BigInteger,HashMap<String,Double>>>();
			this.visitsPerHorizon = new ArrayList<HashMap<BigInteger,Integer>>();
			this.pullsPerHorizon = new ArrayList<HashMap<BigInteger,HashMap<String,Integer>>>();
			
			for (int i = 0; i < this.getRemainingHorizons(); i++) {
				this.rewardsPerHorizon.add(new HashMap<BigInteger, HashMap<String,Double>>());
				this.visitsPerHorizon.add(new HashMap<BigInteger, Integer>());
				this.pullsPerHorizon.add(new HashMap<BigInteger, HashMap<String,Integer>>());
			}	
		}
		
		//Force a "random" seed for each round to avoid the same 
		//pseudo random numbers used in the Simulator
		this.setRandSeed(System.currentTimeMillis());
	}
}
