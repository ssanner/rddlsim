package rddl.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.Help;
import rddl.RDDL;
import rddl.State;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.policy.Policy;
import rddl.viz.StateViz;
import rddl.viz.ValueVectorDisplay;
import util.ArgsParser;

/**
 * @author Wuga
 *
 */
public class DomainExplorer {
	
	public State      _state;
	public INSTANCE   _i;
	public NONFLUENTS _n;
	public DOMAIN     _d;
	public Policy     _p;
	public ValueVectorDisplay   _v;
	public RandomDataGenerator _rand;
	
	public DomainExplorer(RDDL rddl, String instance_name,Policy policy, ValueVectorDisplay viz) throws Exception {
		_state = new State();
		_v = viz;
		_p = policy;

		// Set up instance, nonfluent, and domain information
		_i = rddl._tmInstanceNodes.get(instance_name);
		if (_i == null)
			throw new Exception("\nERROR: Instance '" + instance_name + 
					"' not found, choices are " + rddl._tmInstanceNodes.keySet());
		_n = null;
		if (_i._sNonFluents != null) {
			_n = rddl._tmNonFluentNodes.get(_i._sNonFluents);
			if (_n == null)
				throw new Exception("\nERROR: Nonfluents '" + _i._sNonFluents + 
						"' not found, choices are " + rddl._tmNonFluentNodes.keySet());
		}
		_d = rddl._tmDomainNodes.get(_i._sDomain);
		if (_n != null && !_i._sDomain.equals(_n._sDomain))
			throw new Exception("\nERROR: Domain name of instance and fluents do not match: " + 
					_i._sDomain + " vs. " + _n._sDomain);	
		resetState();
	}
	
	public void resetState() throws EvalException {
		_state.init(_d._hmObjects, _n != null ? _n._hmObjects : null, _i._hmObjects,  
				_d._hmTypes, _d._hmPVariables, _d._hmCPF,
				_i._alInitState, _n == null ? null : _n._alNonFluents,
				_d._alStateConstraints, _d._alActionPreconditions, _d._alStateInvariants,  
				_d._exprReward, _i._nNonDefActions);
	}
	
	//////////////////////////////////////////////////////////////////////////////

	public void run(long rand_seed) throws EvalException {
		
		// Signal start of new session-independent round
		_p.roundInit(Double.MAX_VALUE, _i._nHorizon, 1, 1);
		
		// Reset to initial state
		resetState();
		
		// Set random seed for repeatability
		_rand = new RandomDataGenerator();
		_rand.reSeed(rand_seed);

		// Keep track of reward
		double accum_reward = 0.0d;
		double cur_discount = 1.0d;
		ArrayList<Double> rewards = new ArrayList<Double>(_i._nHorizon != Integer.MAX_VALUE ? _i._nHorizon : 1000);
		
		// Run problem for specified horizon 
		for (int t = 0; t < _i._nHorizon; t++) {
			
			// Check state invariants to verify legal state -- can only reference current 
			// state / derived fluents
			_state.checkStateInvariants();
			// Get action from policy 
			// (if POMDP and first state, no observations available yet so a null is passed)
			State state_info = ((_state._alObservNames.size() > 0) && t == 0) ? null : _state;
			ArrayList<PVAR_INST_DEF> action_list = _p.getActions(state_info);
			
			// Check action preconditions / state-action constraints (latter now deprecated)
			// (these constraints can mention actions and current state / derived fluents)
			_state.checkStateActionConstraints(action_list);
			
			// Compute next state (and all intermediate / observation variables)
			_state.computeNextState(action_list, _rand);
			
			// Set visualization to print state-action pair
			_v.stateAction();
			_v.display(_state, t);

			// Calculate reward / objective and store
			double reward = RDDL.ConvertToNumber(
					_state._reward.sample(new HashMap<LVAR,LCONST>(), _state, _rand)).doubleValue();
			rewards.add(reward);
			accum_reward += cur_discount * reward;
			cur_discount *= _i._dDiscount;
			
			// Done with this iteration, advance to next round
			_state.advanceNextState(false /* do not clear observations */);
			
			// Set visualization to print only state info
			_v.stateOnly();
			_v.display(_state, t);
			
			// A "terminate-when" condition in the horizon specification may lead to early termination
			if (_i._termCond != null && _state.checkTerminationCondition(_i._termCond))
				break;
		}

		// Signal start of new session-independent round
		_p.roundEnd(accum_reward);
	}
	
	//////////////////////////////////////////////////////////////////////////////
	
	public void Search(int rounds, String data_path, String label_path){
		if(!data_path.equals("") && !label_path.equals(""))
			_v.writeFile(data_path, label_path);
			_v.stateAction();
			_v.initFileWriting(_state);
			_v.stateOnly();
			_v.initFileWriting(_state);
		for(int i=0;i<rounds;i++){
			int rand_seed = (int)System.currentTimeMillis();
			try {
				run(rand_seed);
			} catch (EvalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		_v.close();		
	}
	
	public void Search(int rounds){
		String data_path="";
		String label_path="";
		Search(rounds, data_path, label_path);
	}
	

	/**
	 * @param args
	 * @throws Exception
	 * Define arguments
	 * -R: rddle files
	 * -P: policy name
	 * -I: instance name
	 * -V: visualizer name
	 * -S: random seed for simulator
	 * -X: random seed for policy
	 * -K: number of rounds
	 * -D: output data path
	 * -L: output label path
	 */
	
	public static void main(String[] args) throws Exception{
		
		// TODO Auto-generated method stub
		if (ArgsParser.getOptionPos("R",args)==-1 ||ArgsParser.getOptionPos("P",args)==-1 || ArgsParser.getOptionPos("I",args)==-1 ) {
			System.out.println(Help.getSimulatorParaDescription());
			System.exit(1);
		}
		String rddl_file = ArgsParser.getOption("R", args);
		String policy_class_name = ArgsParser.getOption("P", args);
		String instance_name = ArgsParser.getOption("I", args);
		
		String state_viz_class_name = "rddl.viz.ValueVectorDisplay";
		String data_path="";
		String label_path="";
		int rand_seed_sim = (int)System.currentTimeMillis(); // 123456
		int rand_seed_policy = (int)System.currentTimeMillis(); // 123456
		int rounds = 1;
		
		if (ArgsParser.getOptionPos("V",args)!=-1)
			state_viz_class_name = ArgsParser.getOption("V", args);
		if (ArgsParser.getOptionPos("S",args)!=-1)
			rand_seed_sim = new Integer(ArgsParser.getOption("S", args));
		if (ArgsParser.getOptionPos("X",args)!=-1)
			rand_seed_policy = new Integer(ArgsParser.getOption("X", args));
		if (ArgsParser.getOptionPos("K",args)!=-1)
			rounds = new Integer(ArgsParser.getOption("K", args));
		if (ArgsParser.getOptionPos("D",args)!=-1&&ArgsParser.getOptionPos("L",args)!=-1){
			data_path=ArgsParser.getOption("D",args);
			label_path=ArgsParser.getOption("L",args);
		}
		
		// Load RDDL files
		RDDL rddl = new RDDL(rddl_file);
		
		// Initialize simulator, policy and state visualization
		Policy pol = (Policy)Class.forName(policy_class_name).getConstructor(
				new Class[]{String.class}).newInstance(new Object[]{instance_name});
		pol.setRandSeed(rand_seed_policy);
		pol.setRDDL(rddl);
		ValueVectorDisplay viz = (ValueVectorDisplay)Class.forName(state_viz_class_name).newInstance();
		DomainExplorer sim = new DomainExplorer(rddl, instance_name,pol,viz);
		sim.Search(rounds,data_path,label_path);
		
	}

}
