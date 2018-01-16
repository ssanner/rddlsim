/**
 * RDDL: Factored RTDP Implementation.
 * 
 * @version 9/24/11
 *
 **/

package rddl.solver.mdp.rtdp;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

import com.sun.org.apache.bcel.internal.generic.INSTANCEOF;


import dd.discrete.ADD;
import dd.discrete.ADDBNode;
import dd.discrete.ADDDNode;
import dd.discrete.ADDINode;
import dd.discrete.ADDNode;

import rddl.*;
import rddl.RDDL.*;
import rddl.policy.Policy;
import rddl.policy.SPerseusSPUDDPolicy;
import rddl.translate.RDDL2Format;
import rddl.solver.TimeOutException;
import rddl.solver.DDUtils;
import rddl.solver.mdp.Action;
import util.CString;
import util.Pair;

///////////////////////////////////////////////////////////////////////////
//                             Helper Functions
///////////////////////////////////////////////////////////////////////////

public class RTDP1 extends Policy {

	public final static int SOLVER_TIME_LIMIT = 20; // Solver time limit (seconds)
	
	public final static boolean SHOW_STATE   = false;
	public final static boolean SHOW_ACTIONS = false;
	public final static boolean SHOW_ACTION_TAKEN = true;
	
	// Only for diagnostics, comment this out when evaluating
	public final static boolean DISPLAY_SPUDD_ADDS_GRAPHVIZ = false;
	public final static boolean DISPLAY_SPUDD_ADDS_TEXT = false;
	
	public RDDL2Format _translation = null;
	
	// Using CString wrapper to speedup hash lookups
	public ADD _context;
	public ArrayList<CString> _alStateVars;
	public ArrayList<Integer> _allMDPADDs;
	public ArrayList<CString> _alActionNames;
	public ArrayList<CString> _Vars;
	public HashMap<CString, Action> _hmActionName2Action; // Holds transition function
	public HashMap<Integer, Integer> _hmPrimeVarID2VarID;
	public int _value  = -1; // Value function ADD
	
	
	public State _initState;
	// Just use the default random seed
	public Random _rand = new Random();
		
	// Constructors
	public RTDP1 () { }
	
	public RTDP1(String instance_name) {
		super(instance_name);
	}

	///////////////////////////////////////////////////////////////////////////
	//                      Main Action Selection Method
	//
	// If you're using Java and the SPUDD / Symbolic Perseus Format, this 
	// method is the only client method you need to understand to implement
	// your own custom policy.
	///////////////////////////////////////////////////////////////////////////

	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		//System.out.println("FULL STATE:\n\n" + SPerseusSPUDDPolicy.getStateDescription(s));

		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			// when no observations have been generated, for now, we just
			// return a 'noop'
			System.out.println("NO STATE/OBS: taking noop\n\n");
			return new ArrayList<PVAR_INST_DEF>();
		}
		
		
		// If the domain is partially observed, we only see observations,
		// otherwise if it is fully observed, we see the state
		String fluent_type = s._alObservNames.size() > 0 ? "observ" : "states";
		
		// Get a set of all true observation or state variables
		// Note: for a POMDP, the agent should only see the observations and
		//       *never* have access to the underlying state.  State information 
		//       is not provided to a Policy when using the Client/Server interface 
		//       (to prevent cheating in a competition setting); it *is* provided 
		//       via the Simulator interface, but it should be ignored for purposes 
		//       of policy evaluation.
		TreeSet<CString> true_vars = CString.Convert2CString(SPerseusSPUDDPolicy.getTrueFluents(s, "states"));
		if (SHOW_STATE) {
			System.out.println("\n==============================================");
			System.out.println("\nTrue " + 
					           (fluent_type.equals("states") ? "state" : "observation") + 
							   " variables:");
			for (CString prop_var : true_vars)
				System.out.println(" - " + prop_var);
		}
		
		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);

		if (SHOW_STATE) {
			System.out.println("\nLegal action names:");
			for (String action_name : action_map.keySet())
				System.out.println(" - " + action_name);
		}
		
		//////////////////////////// BEGIN RTDP ///////////////////////////////
		// TODO: We're at a state in a trial, implement RTDP algorithm here
		//       for value function update and action selection
		///////////////////////////////////////////////////////////////////////
		ArrayList add_state_assign = DDUtils.ConvertTrueVars2DDAssign(_context, true_vars, _alStateVars);
		QUpdateResult result=getBestQValue(add_state_assign);
		
		if (SHOW_ACTION_TAKEN)
			System.out.println("\n--> Action taken: " + result._csBestAction);
		return action_map.get(result._csBestAction.toString());
	}

	
	
	///////////////////////////////////////////////////////////////////////////
	//                             Trial Signals
	//
	// If you need to keep track of state information across rounds or sessions, 
	// just modify these methods.  (Each session consists of total_rounds rounds.)
	///////////////////////////////////////////////////////////////////////////

	public void roundInit(double time_left, int horizon, int round_number, int total_rounds) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND INIT " + round_number + "/" + total_rounds + "; time remaining = " + time_left + ", horizon = " + horizon);
		System.out.println("*********************************************************");
				
		// Build ADDs for transition, reward and value function (if not already built)
		if (_translation == null) {
			
			// Use RDDL2Format to build SPUDD ADD translation of _sInstanceName
			try {
				_translation = new RDDL2Format(_rddl, _sInstanceName, RDDL2Format.SPUDD_CURR, "");
			} catch (Exception e) {
				System.err.println("Could not construct MDP for: " + _sInstanceName + "\n" + e);
				e.printStackTrace(System.err);
				System.exit(1);
			}

			// Get ADD context and initialize value function ADD
			_context = _translation._context;
			_allMDPADDs = new ArrayList<Integer>();
			_value = _context.getConstantNode(0d); // Initialize to 0
			//System.out.println(_translation._alStateVars);

			// Get the state var and action names
			_alStateVars = new ArrayList<CString>();
			_Vars = new ArrayList<CString>();
			for (String s : _translation._alStateVars){
				_alStateVars.add(new CString(s));
				_Vars.add(new CString(s+"'"));
				_Vars.add(new CString(s));
			}
			
			_alActionNames = new ArrayList<CString>();
			for (String a : _translation._hmActionMap.keySet())
				_alActionNames.add(new CString(a));
	
			// Now extract the reward and transition ADDs
			_hmActionName2Action = new HashMap<CString,Action>();
			for (String a : _translation._hmActionMap.keySet()) {
				HashMap<CString,Integer> cpts = new HashMap<CString,Integer>();
				int reward = _context.getConstantNode(0d);
				
				// Build reward from additive decomposition
				ArrayList<Integer> reward_summands = _translation._act2rewardDD.get(a);
				for (int summand : reward_summands)
					reward = _context.applyInt(reward, summand, ADD.ARITH_SUM);
				_allMDPADDs.add(reward);
				// Build CPTs
				for (String s : _translation._alStateVars) {
					int dd = _translation._var2transDD.get(new Pair(a, s));
					
					int dd_true  = _context.getVarNode(s + "'", 0d, 1d);
					dd_true = _context.applyInt(dd_true, dd, ADD.ARITH_PROD);
		
					int dd_false = _context.getVarNode(s + "'", 1d, 0d);
					//System.out.println("Multiplying..." + dd + ", " + DD_ONE);
					//_context.printNode(dd);
					//_context.printNode(DD_ONE);
					int one_minus_dd = _context.applyInt(_context.getConstantNode(1d), dd, ADD.ARITH_MINUS);
					dd_false = _context.applyInt(dd_false, one_minus_dd, ADD.ARITH_PROD);
					
					// Now have "dual action diagram" cpt DD
					int cpt = _context.applyInt(dd_true, dd_false, ADD.ARITH_SUM);

					cpts.put(new CString(s + "'"), cpt);
					_allMDPADDs.add(cpt);
				}
				
				// Build Action and add to HashMap
				CString action_name = new CString(a);
				Action action = new Action(_context, action_name, cpts, reward);
				_hmActionName2Action.put(action_name, action);
			}
			// Display ADDs on terminal?
			if (DISPLAY_SPUDD_ADDS_TEXT) {
				System.out.println("State variables: " + _alStateVars);
				System.out.println("Action names: " + _alActionNames);
				
				for (CString a : _alActionNames) {
					Action action = _hmActionName2Action.get(a);
					System.out.println("Content of action '" + a + "'\n" + action);
				}
				
				System.out.println("Value ADD: " + _context.printNode(_value));
			}
			
			// Display ADDs in graph visualization window?
			// (only show a subset... 100's to display otherwise)
			final int MAX_DISPLAY = 10;
			if (DISPLAY_SPUDD_ADDS_GRAPHVIZ) {
				_context.getGraph(_value).launchViewer();
				int displayed = 1;
				for (CString a : _alActionNames) {
					Action action = _hmActionName2Action.get(a);
					
					// Show cpts for each action/var
					for (CString var : _alStateVars) {
						_context.getGraph(action._hmStateVar2CPT.get(var)).launchViewer();

						if (++displayed >= MAX_DISPLAY)
							break;
					}
					
					// Show reward for action
					_context.getGraph(action._reward).launchViewer();
					
					if (++displayed >= MAX_DISPLAY)
						break;
				}				
			}
			// Call RTDP solver for SOLVER_TIME_LIMIT seconds
            try {
                    resetSolver();
            		System.out.println("Inicio");
            		doRTDP(SOLVER_TIME_LIMIT);//36080);
            } catch (TimeOutException e) {
                    System.out.println("TIME LIMIT EXCEEDED at " + _nIter + " iterations.");
            } catch (Exception e) {
                    System.err.println("ERROR at " + _nIter + " iterations.");
                    e.printStackTrace(System.err);
                    System.exit(1);
            } finally {
                    System.out.println("Solution in sRTDP exit at iteration " + _nIter + ": " + 
                                    _context.countExactNodes(_valueDD) + " nodes.");
            }
			//System.out.println("Resuelto");
		}
	}
	
	public void roundEnd(double reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND END, reward = " + reward);
		System.out.println("*********************************************************");
		//_context.getGraph(_value).launchViewer();
	}
	
	public void sessionEnd(double total_reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> SESSION END, total reward = " + total_reward);
		System.out.println("*********************************************************");
	}
	
	
	///////////////////////////////////////////////////////////////////////////
    //                      Factored RTDP Solver
    ///////////////////////////////////////////////////////////////////////////
    
    // Local constants
    public final static int VERBOSE_LEVEL = 0; // Determines how much output is displayed
    
    public final static boolean ALWAYS_FLUSH = false; // Always flush DD caches?
    public final static double FLUSH_PERCENT_MINIMUM = 0.3d; // Won't flush until < this amt

    // For printing
    //public final static DecimalFormat _df = new DecimalFormat("#.###");

    // Timing variables
    public long _lStartTime; // For timing purposes
    public int  _nTimeLimitSecs;
    public static Runtime RUNTIME = Runtime.getRuntime();

    // Local vars
    public INSTANCE _rddlInstance = null;
    public int _valueDD;
    //public int _nDDType; // Type of DD to use
    public int _nIter;
    public double _dRewardRange; // Important if approximating
    public double _dDiscount;
    public int _nHorizon;
    
	//	 For now we assume that the ADD transition functions for all
    // actions apply in every state... will have to revisit this later
    // w.r.t. RDDL's state-action constraints
    public ArrayList sampleNextState(ArrayList current_state, CString action) {
           
            Action a = _hmActionName2Action.get(action);
            ArrayList next_state = (ArrayList)current_state.clone(); // ensure correct size
            //System.out.println(action);
            // Sample each next state variable to build a new state
            for (Map.Entry<Integer, Integer> me : a._hmVarID2CPT.entrySet()) {
                    int prime_var_id = me.getKey();
                    int cpt_dd = me.getValue();
                   
                    // For each CPT, evaluate in current state for next state variable true
                    int level_prime = (Integer)_context._hmGVarToLevel.get(prime_var_id);
                    current_state.set(level_prime, true);
                    double prob_true = _context.evaluate(cpt_dd, current_state);
                    if (Double.isNaN(prob_true)) {
                            System.err.println("ERROR in RTDP.sampleNextState: Expected single value when evaluating: " + current_state);
                            //System.err.println("in " + context.printNode(cpt_dd));
                            System.exit(1);
                    }
                    current_state.set(level_prime, null); // Undo so as not to change current_state
                   
                    // Draw sample
        			boolean is_true = _random.nextUniform(0d,1d) < prob_true; 
                   
                    // Assign truth value to level for unprimed variable
                    int var_id = _hmPrimeVarID2VarID.get(prime_var_id);
                    int level_unprime = (Integer)_context._hmGVarToLevel.get(var_id);
                    next_state.set(level_unprime, is_true);
            }
           
            return next_state;
    }
	
    // Set the properties of the initial state 
	public void resetState() {
		_initState = new State();
		_initState.init(_translation._d._hmObjects, _translation._n != null ? _translation._n._hmObjects : null, _translation._i._hmObjects,  
				_translation._d._hmTypes, _translation._d._hmPVariables, _translation._d._hmCPF,
                _translation._i._alInitState, _translation._n == null ? new ArrayList<PVAR_INST_DEF>() : _translation._n._alNonFluents, _translation._i._alNonFluents,
				_translation._d._alStateConstraints, _translation._d._alActionPreconditions, _translation._d._alStateInvariants, 
				_translation._d._exprReward, _translation._i._nNonDefActions);
	}
	
	
	public static class QUpdateResult {
        public CString _csBestAction;
        public double  _dBestQValue;
        public QUpdateResult(CString best_action, double best_qvalue) {
                _csBestAction = best_action;
                _dBestQValue  = best_qvalue;
        }
	}
	
	//	Get a var assignment ArrayList from a complete State
	public ArrayList getVarAssignmentFromState(State s) {
		TreeSet<CString> true_vars =
            CString.Convert2CString(SPerseusSPUDDPolicy.getTrueFluents(s, "states"));
		ArrayList state_assign = DDUtils.ConvertTrueVars2DDAssign(_context, true_vars, _alStateVars);
		return state_assign;
	}
	
	//	 Find best Q-value/action for given state
	private QUpdateResult getBestQValue(ArrayList state) {
		double max=Double.NEGATIVE_INFINITY;
		QUpdateResult result = new QUpdateResult(null, -Double.MAX_VALUE);
		for (int i=0;i<_alActionNames.size();i++){
			CString actionName = _alActionNames.get(i);
			double Qt = getQValue(state, actionName);
			max=Math.max(max,Qt);
			if(max==Qt){
				result._csBestAction=actionName;
				result._dBestQValue=max;
			}
		}
		updateValueBranch(max, state);
		return result;
	}
	
	//	 Initialize all variables (call before starting rtdp)
    public void resetSolver() {
    		resetState();                                 
            _nIter = -1;
            _rddlInstance = _rddl._tmInstanceNodes.get(this._sInstanceName);
            if (_rddlInstance == null) {
                    System.err.println("ERROR: Could not fine RDDL instance '" + _rddlInstance + "'");
                    System.exit(1);
            }
            _dDiscount = _rddlInstance._dDiscount;
            _nHorizon  = _rddlInstance._nHorizon;
            
            //if (_dDiscount == 1d)
    		//	_dDiscount = 0.99d;
            
//          In RTDP we need to map from CPT head var (primed) into non-prime state variable
            _hmPrimeVarID2VarID = new HashMap<Integer,Integer>();
            for (Map.Entry<String, String> me : _translation._hmPrimeRemap.entrySet()) {
                    String var = me.getKey();
                    String var_prime = me.getValue();
                    Integer var_id = (Integer)_context._hmVarName2ID.get(var);
                    Integer var_prime_id = (Integer)_context._hmVarName2ID.get(var_prime);
                    if (var_id == null || var_prime_id == null) {
                            System.err.println("ERROR: Could not get var IDs "
                                            + var_id + "/" + var_prime_id
                                            + " for " + var + "/" + var_prime);
                            System.exit(1);
                    }
                    _hmPrimeVarID2VarID.put(var_prime_id, var_id);
            }
           
            _dRewardRange = 0d;
            for (Action a : _hmActionName2Action.values())
                    _dRewardRange = Math.max(_dRewardRange,
                                    _context.getMaxValue(a._reward) -
                            _context.getMinValue(a._reward));
           
            // IMPORTANT: RTDP needs **optimistic upper bound initialization**
            double value_range = (_dDiscount < 1d)
                    ? _dRewardRange / (1d - _dDiscount) // being lazy: assume infinite horizon
                    : _nHorizon * _dRewardRange;        // assume max reward over horizon
            _value = _context.getConstantNode(value_range);
    }

    //  Main RTDP Algorithm
	public void doRTDP(int time_limit_secs)  throws TimeOutException {
		_nTimeLimitSecs = time_limit_secs;		
		_nIter = 0;
		_lStartTime = System.currentTimeMillis();
		// Run problem for specified horizon
		while(true){
			doRTDPTrial(_nHorizon, getVarAssignmentFromState(_initState));
			_nIter++;
		}
	}
	
	//	 Run a single RTDP trial, return best action as seen from initial state
	public void doRTDPTrial(int trial_depth, ArrayList cur_state) throws TimeOutException {
				
		ArrayList<ArrayList> visited_states = new ArrayList<ArrayList>();
		QUpdateResult result=null;
		int depth=0;		
		visited_states.clear();// clear visited states stack		
		//do trial 
		while((cur_state !=null)&& depth<trial_depth){
			flushCaches();
			checkTimeLimit();
			depth++;
			visited_states.add(cur_state);
			result = getBestQValue(cur_state); //compute Best Q value and action
			cur_state = sampleNextState(cur_state, result._csBestAction);
		}		
		//do optimization
		for (int i = 0; i<visited_states.size();i++){
			
			cur_state=visited_states.get(i);
			getBestQValue(cur_state);
		}	
		flushCaches();
		checkTimeLimit();
	}
	
	// Insert a number in a Value ADD branch
	private void updateValueBranch(/*char c,*/ double value, ArrayList state) {
		//if (c=='u'){
		Iterator it = _translation._alStateVars.iterator();
		_value = DDUtils.insertValueInDD(_value, state,  value, it, _translation._hmPrimeRemap, _context);
		//}
	    //This will be used for BRTDP
		/*else if(c=='l'){
			VLower = insertValueInDD(VLower, state,  value, _translation._hmPrimeRemap, _context);
		}
		else if(c=='g'){
			VGap = insertValueInDD(VGap, state,  value, _translation._hmPrimeRemap, _context);
		}*/
    }
	
	
	//	 Find Q-value for action in given state
	private double getQValue(ArrayList state, CString actionName) {
		// The Value function is already with prime variable
		int VPrime = _value;
		Action action = _hmActionName2Action.get(actionName);
		Iterator<CString> x=_alStateVars.iterator();
		for (Map.Entry<Integer, Integer> me : action._hmVarID2CPT.entrySet()) {
			int idxiprime = me.getKey();
            int cpt_a_xiprime = me.getValue();
			int level_prime = (Integer)_context._hmGVarToLevel.get(idxiprime);
            state.set(level_prime, true);            
			double probTrue=_context.evaluate(cpt_a_xiprime, state); //utilRtdp.evaluateState(cpt_a_xiprime,state, _Vars, _context, new CString(xi.toString()+"'"));
			double probFalse = 1 - probTrue;
			int Fh=_context.getConstantNode(probTrue);
			int Fl=_context.getConstantNode(probFalse);
			int newCPT=_context.getINode(idxiprime, Fl, Fh, true);
			VPrime = _context.applyInt(VPrime, newCPT, dd.discrete.DD.ARITH_PROD);
			VPrime = _context.opOut(VPrime, idxiprime, dd.discrete.DD.ARITH_SUM);
		}
		double stateValue = ((ADDDNode)_context.getNode(VPrime))._dLower;//ADD always is a value
		double reward = _context.evaluate(action._reward, state); //utilRtdp.evaluateState(action._reward, state, _Vars, _context, null);// utilRtdp.getRewardState(actionName, state, _vars, _translation, _context);
		return (_dDiscount*stateValue) + reward;
	}
	
	////////////////////////////////////////////////////////////////////////////
    // DD Cache Maintenance for MDPs
    ////////////////////////////////////////////////////////////////////////////
    
	// Frees up memory... only do this if near limit?
    public void flushCaches() {
            if (!ALWAYS_FLUSH
                            && ((double) RUNTIME.freeMemory() / (double) RUNTIME
                                            .totalMemory()) > FLUSH_PERCENT_MINIMUM) {
                    return; // Still enough free mem to exceed minimum requirements
            }
            _context.clearSpecialNodes();
            for (Integer dd : _allMDPADDs)
                    _context.addSpecialNode(dd);
            _context.addSpecialNode(_value);
            _context.flushCaches(false);
    }

	
		
	////////////////////////////////////////////////////////////////////////////
    // Miscellaneous helper methods
    ////////////////////////////////////////////////////////////////////////////

    public void checkTimeLimit() throws TimeOutException {
            double elapsed_time = (System.currentTimeMillis() - _lStartTime) / 1000d;
            if (elapsed_time > (double)_nTimeLimitSecs)
                    throw new TimeOutException("Elapsed time " + elapsed_time + " (s) exceeded time limit of " + _nTimeLimitSecs + " (s)");
    }
    
    public static String MemDisplay() {
        long total = RUNTIME.totalMemory();
        long free = RUNTIME.freeMemory();
        return total - free + ":" + total;
    }

}
