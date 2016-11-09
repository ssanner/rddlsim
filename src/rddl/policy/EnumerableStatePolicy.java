package rddl.policy;

import java.math.BigInteger;
import java.util.*;

import dd.discrete.ADD;
import dd.discrete.ADDBNode;
import dd.discrete.ADDDNode;
import dd.discrete.ADDINode;
import dd.discrete.ADDNode;
import dd.discrete.ADD.ADDLeafOperation;
import dd.discrete.DD;

import rddl.*;
import rddl.RDDL.*;
import rddl.solver.mdp.Action;
import rddl.translate.RDDL2Format;
import util.*;

/**
 * Represents a policy that handles with a Markov Decision Process with
 * enumerable states. 
 */
public abstract class EnumerableStatePolicy extends Policy {

	/**
	 * Default constructor.
	 */
	public EnumerableStatePolicy() { }
	
	/**
	 * Initialize this class with the instance name to be solved by this algorithm. 
	 * @param instance_name Instance name to be solved by this algorithm
	 */
	public EnumerableStatePolicy(String instance_name) {
		super(instance_name);
	}
	
	/**
	 * Initialize this class with same references to the attributes of another policy.
	 * @param policy Policy to copy.
	 */
	protected EnumerableStatePolicy(EnumerableStatePolicy policy) {
		this.stateVariableNames = policy.stateVariableNames;
		this.remainingHorizons = policy.remainingHorizons;
		this.discountFactor = policy.discountFactor;
		this.actions = policy.actions;
		this.rddlInstance = policy.rddlInstance;
		this.translation = policy.translation;
		this._random = policy._random;
		this._rddl = policy._rddl;
		this._sInstanceName = policy._sInstanceName;
	}

	private List<String> stateVariableNames = null;
	
	private int remainingHorizons = 0;
	
	private double discountFactor = 1.0;
	
	private List<CString> actions = null;

	protected INSTANCE rddlInstance = null;
	
	private RDDL2Format translation = null;

	/**
	 * Gets a handler to RDDL data.
	 * @return Handler to RDDL data.
	 */
	protected RDDL2Format getTranslation() {
		return translation;
	}
	

	/**
	 * Gets the remaining horizons to end a round.
	 * @return Remaining horizons to end a round
	 */
	protected int getRemainingHorizons() {
		return this.remainingHorizons;
	}

	/**
	 * Gets the size of the horizon to solve the current instance.
	 * @return Size of the horizon to solve the current instance
	 */
	protected int getTotalHorizons() {
		return this.rddlInstance._nHorizon;
	}
	
	/**
	 * Gets the actions that can be executed in the current instance.
	 * @return Actions that can be executed in the current instance
	 */
	protected List<CString> getActions() {
		return actions;
	}
	
	/**
	 * Gets the discount factor used in Bellman Backup. 
	 * @return Discount factor used in Bellman Backup.
	 */
	protected double getDiscountFactor() {
		return this.discountFactor;
	}
	
	/**
	 * Command executed at the round begin.
	 * @param timeLeft Time left to the policy execution
	 * @param horizon Number of horizons in to compute the policy
	 * @param roundNumber Number of the current round
	 * @param totalRound Number of the total rounds in the simulation
	 */
	@Override
	public void roundInit(double timeLeft, int horizon, int roundNumber, int totalRounds) {
		this.roundInit(timeLeft, horizon, roundNumber, totalRounds, false);
	}
	
	/**
	 * Command executed at the round begin.
	 * @param timeLeft Time left to the policy execution
	 * @param horizon Number of horizons in to compute the policy
	 * @param roundNumber Number of the current round
	 * @param totalRound Number of the total rounds in the simulation
	 * @param suppressConsole If true, suppress super class call
	 */
	protected void roundInit(double timeLeft, int horizon, int roundNumber, int totalRounds, boolean suppressConsole) {
		if (!suppressConsole) //execute super class definitions for this method
			super.roundInit(timeLeft, horizon, roundNumber, totalRounds);
		
		this.remainingHorizons = horizon;
		
		this.rddlInstance = _rddl._tmInstanceNodes.get(_sInstanceName);
		
		this.discountFactor = this.rddlInstance._dDiscount;
		
		if (translation == null) {
			try {
				this.translation = new RDDL2Format(_rddl, _sInstanceName, RDDL2Format.SPUDD_CURR, "");
			} catch (Exception e) {
				System.err.println("Could not construct MDP for: " + _sInstanceName + "\n" + e);
				e.printStackTrace(System.err);
				System.exit(1);
			}
			
			this.stateVariableNames = new ArrayList<String>(this.translation._alStateVars);
			
			this.actions = new ArrayList<CString>();
			
			for (String a : this.translation._hmActionMap.keySet()) {
				CString action = new CString(a);
				
				this.actions.add(action);
			}
		}
	}
	
	/**
	 * Gets an number that labels a state.
	 * @param s State to be labeled
	 * @return Label of state s
	 */
	protected BigInteger getStateLabel(State s) {	
		ArrayList<Boolean> variableValues = this.getVariableValues(s);
		
		return convertVariableValuesToNumber(variableValues);
	}
	
	protected ArrayList<Boolean> getVariableValues(BigInteger state) {
		ArrayList<Boolean> variableValues = new ArrayList<Boolean>();
		
		for (int i = 0; i < stateVariableNames.size(); i++)
			variableValues.add(state.testBit(i));
		
		return variableValues;
	}
	
	protected ArrayList<Boolean> getVariableValues(State s) {
		HashMap<String, Boolean> variableValuesAsHashMap = getStateVariableValues(s);
		
		ArrayList<Boolean> variableValues = new ArrayList<Boolean>();
		
		for (int i = 0; i < this.stateVariableNames.size(); i++) {
			String variableName = this.stateVariableNames.get(i);
			Boolean variableValue = false;
			
			if (variableValuesAsHashMap.containsKey(variableName)) {
				variableValue = variableValuesAsHashMap.get(variableName);
				variableValues.add(i, variableValue);	
			}
			else {
				System.out.printf("Warning ! Variable [%s] not found in state representation", variableName);
				System.out.println();
			}
		}
		
		return variableValues;
	}
	
	/**
	 * Convert a list of boolean variable values in a number
	 * @param variableValues List of variable values
	 * @return Number representation for that variable values
	 */
	private BigInteger convertVariableValuesToNumber(ArrayList<Boolean> variableValues) {
		BigInteger number = BigInteger.ZERO;
		
		for (int i = 0; i < variableValues.size(); i++) {
			Boolean variableValue = variableValues.get(i);
			
			if (variableValue) 
				number = number.setBit(i);
		}
		
		return number;
	}
		
	private HashMap<String, Boolean> getStateVariableValues(State s) {
		final String fluent_type = "states";
		
		HashMap<String, Boolean> variable_values = new HashMap<String, Boolean>();
		
		for (PVAR_NAME p : (ArrayList<PVAR_NAME>) s._hmTypeMap.get(fluent_type)) {
			try {
				// Go through all term groundings for variable p
				ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);	
				
				for (ArrayList<LCONST> gfluent : gfluents) {
					String variableName = RDDL2Format.CleanFluentName(p._sPVarName + gfluent);
					
					Boolean variable_value = (Boolean)s.getPVariableAssign(p, gfluent); 
					variable_values.put(variableName, variable_value);
				}
			} catch (Exception ex) {
				System.err.println("EnumerableStatePolicy: could not retrieve assignment for " + p + "\n");
			}
		}
		
		return variable_values;
	}

	/**
	 * Get an action given a state.
	 */
	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			return new ArrayList<PVAR_INST_DEF>();
		}

		String action_taken = this.getBestAction(s);
		
		this.remainingHorizons--;
		
		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);
		
		// Return a random action if a action cannot be taken
		if (action_taken == null) {
			ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
			action_taken = actions.get(_random.nextInt(0, action_map.size() - 1));
		}
		
		return action_map.get(action_taken);
	}

	/**
	 * Get the best action given a state.
	 */
	protected abstract String getBestAction(State s) throws EvalException;
}