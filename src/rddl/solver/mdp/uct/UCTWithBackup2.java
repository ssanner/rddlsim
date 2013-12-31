package rddl.solver.mdp.uct;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omg.CORBA.portable.RemarshalException;

import com.sun.org.apache.xpath.internal.operations.Bool;

import dd.discrete.ADD;
import dd.discrete.DD;

import rddl.State;
import rddl.solver.DDUtils;
import rddl.solver.mdp.Action;
import rddl.solver.mdp.rtdp.RTDP.QUpdateResult;
import util.CString;
import util.Pair;

public class UCTWithBackup2 extends UCT {

	/**
	 * Default constructor.
	 */
	public UCTWithBackup2() { }
	
	/**
	 * Initialize this class with the instance name to be solved by this algorithm. 
	 * @param instance_name Instance name to be solved by this algorithm
	 */
	public UCTWithBackup2(String instance_name) {
		super(instance_name);
	}

	private List<HashMap<BigInteger,Double>> valuePerHorizon = null;
	private List<HashMap<BigInteger,String>> bestActionPerHorizon = null;
	private List<HashMap<BigInteger,HashMap<String,Double>>> accumProbPerHorizon = null;
	private List<HashMap<BigInteger,HashMap<String,HashMap<BigInteger,Double>>>> descendentsValPerHorizon = null;
	private List<HashMap<BigInteger,HashMap<String,HashMap<BigInteger,Double>>>> descendentsProbPerHorizon = null;
	
	protected HashMap<String, Action> actionData = null; 
	
	private List<Integer> allADDs = null;
	
	private void testUpdateValue(BigInteger s, int h, double x, String action){
		HashMap<BigInteger,Double> v = valuePerHorizon.get(h-1);
		HashMap<BigInteger, String> ba = bestActionPerHorizon.get(h-1);
		if (!v.containsKey(s)){
			v.put(s,x);
			ba.put(s, action);
			return;
		}
		if ( x >= v.get(s)){
			v.put(s,x);
			ba.put(s, action);
			return;
		}
		if (action == ba.get(s)){
			Pair<String, Double> best = this.getUCTBestAction(s, h, 0.0);
			v.put(s,best._o2);
			ba.put(s, best._o1);
		}
	}
	
	
	
	@Override
	public void roundInit(double timeLeft, int horizon, int roundNumber, int totalRounds) {
		super.roundInit(timeLeft, horizon, roundNumber, totalRounds);
		
		if (this.allADDs == null) {
			this.allADDs = new ArrayList<Integer>();
			this.valuePerHorizon = new ArrayList<HashMap<BigInteger,Double>>();
			this.bestActionPerHorizon = new ArrayList<HashMap<BigInteger,String>>();
			this.accumProbPerHorizon = new ArrayList<HashMap<BigInteger,HashMap<String,Double>>>();
			this.descendentsProbPerHorizon = new ArrayList<HashMap<BigInteger,HashMap<String,HashMap<BigInteger,Double>>>>();
			this.descendentsValPerHorizon = new ArrayList<HashMap<BigInteger,HashMap<String,HashMap<BigInteger,Double>>>>();
			
			for(int i=0;i<this.getTotalHorizons();i++){
				this.valuePerHorizon.add(new HashMap<BigInteger,Double>() );
				this.bestActionPerHorizon.add(new HashMap<BigInteger,String>() );
				this.accumProbPerHorizon.add( new HashMap<BigInteger,HashMap<String,Double>>() );
				this.descendentsValPerHorizon.add( new HashMap<BigInteger,HashMap<String,HashMap<BigInteger,Double>>>() );
				this.descendentsProbPerHorizon.add( new HashMap<BigInteger,HashMap<String,HashMap<BigInteger,Double>>>() );
			}
			
			this.actionData = new HashMap<String, Action>();
			for (String a : this.getTranslation()._hmActionMap.keySet()) {
				HashMap<CString, Integer> cpts = new HashMap<CString, Integer>();
				// Build CPTs
				for (String s : this.getTranslation()._alStateVars) {
					int dd = this.getTranslation()._var2transDD.get(new Pair<String,String>(a,s));

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
				Action action = new Action(this.getTranslation()._context, action_name, cpts, 0); //sem calcular rewards
				this.actionData.put(a, action);
				this.actionData.keySet();
			}
		}		
	}
	
	protected Pair<Integer, Long> buildSearchTree(State s, long timeout) {
		int completedSearches = 0;
		
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;
		
		do {		
			this.valSearch(s, this.getRemainingHorizons());
			
			completedSearches++;
			elapsedTime = System.currentTimeMillis() - startTime;
		} while (elapsedTime < timeout);
		
		BigInteger sNum = this.getStateLabel(s);
		System.out.println("### Debug ###");
		System.out.println("State" + sNum);
		System.out.println(valuePerHorizon.get(this.getRemainingHorizons()-1));
		System.out.println(rewardsPerHorizon.get(this.getRemainingHorizons()-1).get(sNum));
		this.flushCaches();
		
		return new Pair<Integer, Long>(completedSearches, elapsedTime);
	}
	
	protected void evaluateGetAction(State state, BigInteger stateAsNumber, int remainingHorizons) {
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
		testUpdateValue(stateAsNumber, remainingHorizons,q, action);
	}
	
	/**
	 * Execute and expands the Monte Carlo Search Tree.
	 */
	protected void valSearch(State state, int remainingHorizons) {

		if (remainingHorizons == 0) return;
		
		BigInteger stateAsNumber = this.getStateLabel(state);
		
		if (isLeaf(stateAsNumber, remainingHorizons)) {
			this.evaluateGetAction(state, stateAsNumber, remainingHorizons); 
			return;
		}
		String action = this.selectAction(stateAsNumber, remainingHorizons); 			
		Pair<State, Double> simulationResult = this.simulateSingleAction(state, action);
		
		State nextState = simulationResult._o1;
		this.valSearch(nextState, remainingHorizons - 1);
		//makes backup and updates value
		backUpdateValue(state, action, nextState, simulationResult._o2, remainingHorizons);
	}
		
	protected void backUpdateValue(State state, String action, State nextState, double immediate_reward, int remainingHorizons) {
		BigInteger stateAsNumber = this.getStateLabel(state);
		
		HashMap<BigInteger, HashMap<String, Double>> rewards = this.rewardsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, HashMap<String, Integer>> pulls = this.pullsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, Integer> visits = this.visitsPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, Double> values = this.valuePerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, HashMap<String, Double>> accumProbs = this.accumProbPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, HashMap<String, HashMap<BigInteger,Double>>> descendentsVal = this.descendentsValPerHorizon.get(remainingHorizons - 1);
		HashMap<BigInteger, HashMap<String, HashMap<BigInteger,Double>>> descendentsProb = this.descendentsProbPerHorizon.get(remainingHorizons - 1);
		
		// Shouldn't call back up without this values
		//if (!rewards.containsKey(stateAsNumber))
		//	rewards.put(stateAsNumber, new HashMap<String, Double>());		
		//if (!pulls.containsKey(stateAsNumber))
		//	pulls.put(stateAsNumber, new HashMap<String, Integer>());		
		
		//If first backup
		if (!accumProbs.containsKey(stateAsNumber))
			accumProbs.put(stateAsNumber, new HashMap<String, Double>());
		if (!descendentsProb.containsKey(stateAsNumber)){
			descendentsProb.put(stateAsNumber, new HashMap<String, HashMap<BigInteger,Double>>());
			descendentsVal.put(stateAsNumber, new HashMap<String, HashMap<BigInteger,Double>>());
		}
		
		HashMap<String, Double> qForAction = rewards.get(stateAsNumber);
		HashMap<String, Integer> pullsForAction = pulls.get(stateAsNumber);
		HashMap<String, Double> accumProbForAction = accumProbs.get(stateAsNumber);
		HashMap<String, HashMap<BigInteger,Double>> descendentsValForAction = descendentsVal.get(stateAsNumber);
		HashMap<String, HashMap<BigInteger,Double>> descendentsProbForAction = descendentsProb.get(stateAsNumber);
		
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
		
		//Makes Backup
		double oldQ = 0.0; //previous Q for this action initially avg of rewards 
		if (qForAction.containsKey(action))
			oldQ = (qForAction.get(action) - immediate_reward)/this.getDiscountFactor();
		
		if (!descendentsValForAction.containsKey(action) ){
			descendentsValForAction.put(action,new HashMap<BigInteger,Double>());
			descendentsProbForAction.put(action,new HashMap<BigInteger,Double>());
		}
		double oldAccumProb = 0.0; //previous accumulated probability
		if (accumProbForAction.containsKey(action))
			oldAccumProb = accumProbForAction.get(action);
		
		if (remainingHorizons ==1)
		{
			double v = immediate_reward;
			this.testUpdateValue(stateAsNumber, remainingHorizons, v, action);
			return;
		}
		
		BigInteger nextAsNumber = this.getStateLabel(nextState);
		HashMap<BigInteger, Double> v_next = valuePerHorizon.get(remainingHorizons -2);

		double pk;
		double newExpRew;
		
		HashMap<BigInteger,Double> descendV = descendentsValForAction.get(action);
		HashMap<BigInteger,Double> descendP = descendentsProbForAction.get(action);
		if (descendV.containsKey(nextAsNumber)) {		
			//Already visited state backup, don't change accum probability
			 
			double dif = v_next.get(nextAsNumber) - descendV.get(nextAsNumber);
			pk = descendP.get(nextAsNumber);
			newExpRew = oldQ + (pk*dif)/oldAccumProb;
		}
		else{
			pk = this.conditional_probability(nextState,state,action);
			//new state, increase accum prob
			newExpRew = (oldAccumProb*oldQ + pk*v_next.get(nextAsNumber))/(oldAccumProb+pk);
			accumProbForAction.put(action,oldAccumProb + pk);
			descendP.put(nextAsNumber,pk);
		}
		descendV.put(nextAsNumber, v_next.get(nextAsNumber));
		
		double v = immediate_reward + this.getDiscountFactor()*newExpRew;
		qForAction.put(action, v);
		
		if (!values.containsKey(stateAsNumber) ) {
			//shouldn`t happen 
			System.out.println("estado sem valor fazendo update!");
		}
		this.testUpdateValue(stateAsNumber, remainingHorizons, v, action);
	}

	private double conditional_probability(State nextState,State state, String a)
	{
		ArrayList<Boolean> factoredState = this.getVariableValues(state);
		ArrayList<Boolean> nextFactoredState = this.getVariableValues(nextState);
		factoredState.addAll(nextFactoredState);
		Action action = this.actionData.get(a);
		double prob_next=1.0;
		for (Map.Entry<Integer, Integer> me : action._hmVarID2CPT.entrySet()) {
			int cpt_dd = me.getValue();			
			// For each CPT, evaluate in current state for next state variable
			double prob = this.getTranslation()._context.evaluate(cpt_dd, factoredState);
			prob_next = prob_next*prob;
		}
		return prob_next;
	}
	
	private void flushCaches() {		
		Runtime runtime = Runtime.getRuntime();
		
		double freeMemoryPercentage = ((double) runtime.freeMemory() / (double) runtime.totalMemory());
		
//		System.out.printf("Memory status - Free Memory Percentage: [%f] Total Memory: [%d]", freeMemoryPercentage, runtime.totalMemory());
//		System.out.println();
		
		if (freeMemoryPercentage > 0.3) 
			return; // Still enough free mem to exceed minimum requirements

		this.getTranslation()._context.clearSpecialNodes();

		for (Integer dd : this.allADDs)
			this.getTranslation()._context.addSpecialNode(dd);

		this.getTranslation()._context.flushCaches(false);
	}
}