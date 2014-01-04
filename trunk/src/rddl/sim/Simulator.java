/**
 * RDDL: Implements the RDDL simulator... see main().
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.sim;

import java.io.*;
import java.util.*;

import rddl.*;
import rddl.viz.*;
import rddl.policy.*;
import rddl.RDDL.*;
import rddl.parser.parser;
import org.apache.commons.math3.*;
import org.apache.commons.math3.random.*;

public class Simulator {
	
	public State      _state;
	public INSTANCE   _i;
	public NONFLUENTS _n;
	public DOMAIN     _d;
	public Policy     _p;
	public StateViz   _v;
	public RandomDataGenerator _rand;
	
	public Simulator(RDDL rddl, String instance_name) throws Exception {
		_state = new State();

		// Set up instance, nonfluent, and domain information
		_i = rddl._tmInstanceNodes.get(instance_name);
		if (_i == null)
			throw new Exception("Instance '" + instance_name + 
					"' not found, choices are " + rddl._tmInstanceNodes.keySet());
		_n = null;
		if (_i._sNonFluents != null)
			_n = rddl._tmNonFluentNodes.get(_i._sNonFluents);
		_d = rddl._tmDomainNodes.get(_i._sDomain);
		if (_n != null && !_i._sDomain.equals(_n._sDomain))
			throw new Exception("Domain name of instance and fluents do not match: " + 
					_i._sDomain + " vs. " + _n._sDomain);		
	}
	
	public void resetState() throws EvalException {
		//System.out.println("Resetting state:" +
		//		"\n  Types:      " + _d._hmTypes + 
		//		"\n  PVars:      " + _d._hmPVariables + 
		//		"\n  InitState:  " + _i._alInitState + 
		//		"\n  NonFluents: " + _n._alNonFluents);
		_state.init(_d._hmObjects, _n != null ? _n._hmObjects : null, _i._hmObjects,  
				_d._hmTypes, _d._hmPVariables, _d._hmCPF,
				_i._alInitState, _n == null ? null : _n._alNonFluents,
				_d._alStateConstraints, _d._alActionPreconditions, _d._alStateInvariants,  
				_d._exprReward, _i._nNonDefActions);
	}
	
	//////////////////////////////////////////////////////////////////////////////

	public Result run(Policy p, StateViz v, long rand_seed) throws EvalException {
		
		// Signal start of new session-independent round
		p.roundInit(Double.MAX_VALUE, _i._nHorizon, 1, 1);
		
		// Reset to initial state
		resetState();
		
		// Set random seed for repeatability
		_rand = new RandomDataGenerator();
		_rand.reSeed(rand_seed);

		// Keep track of reward
		double accum_reward = 0.0d;
		double cur_discount = 1.0d;
		ArrayList<Double> rewards = new ArrayList<Double>(_i._nHorizon);
		
		// Run problem for specified horizon
		for (int t = 0; t < _i._nHorizon; t++) {

			// MOVED AFTER ACTION SELECTION
			// v.display(_state, t);

			// Get action from policy 
			// (if POMDP and first state, no observations available yet so a null is passed)
			State state_info = ((_state._alObservNames.size() > 0) && t == 0) ? null : _state;
			ArrayList<PVAR_INST_DEF> action_list = p.getActions(state_info);
			
			// Check action preconditions / state-action constraints (latter deprecated) 
			_state.checkStateActionConstraints(action_list);
			
			// Compute next state (and all intermediate / observation variables)
			_state.computeNextState(action_list, _rand);

			// Check state invariants / state-action constraints (latter deprecated) 
			_state.checkStateInvariants();

			// Display state/observations that the agent sees
			v.display(_state, t);

			// Calculate reward / objective and store
			double reward = RDDL.ConvertToNumber(
					_state._reward.sample(new HashMap<LVAR,LCONST>(), _state, _rand)).doubleValue();
			rewards.add(reward);
			accum_reward += cur_discount * reward;
			cur_discount *= _i._dDiscount;
			
			// Done with this iteration, advance to next round
			_state.advanceNextState(false /* do not clear observations */);
		}

		// Signal start of new session-independent round
		p.roundEnd(accum_reward);

		// Problem over, return objective and list of rewards (e.g., for std error calc)
		v.close();
		return new Result(accum_reward, rewards);
	}
	
	//////////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) throws Exception {
			
		// Argument handling
		if (args.length < 3 || args.length > 6) {
			System.out.println("usage: RDDL-file policy-class-name instance-name [state-viz-class-name] [rand seed simulator] [rand seed policy]");
			System.exit(1);
		}
		String rddl_file = args[0];
		String policy_class_name = args[1];
		String instance_name = args[2];
		String state_viz_class_name = "rddl.viz.GenericScreenDisplay";
		if (args.length >= 4)
			state_viz_class_name = args[3];
		int rand_seed_sim = 123456;
		if (args.length == 5)
			rand_seed_sim = new Integer(args[4]);
		int rand_seed_policy = 123456;
		if (args.length == 6)
			rand_seed_policy = new Integer(args[5]);
		
		// Load RDDL files
		RDDL rddl = new RDDL(rddl_file);
		
		// Initialize simulator, policy and state visualization
		Simulator sim = new Simulator(rddl, instance_name);
		Policy pol = (Policy)Class.forName(policy_class_name).getConstructor(
				new Class[]{String.class}).newInstance(new Object[]{instance_name});
		pol.setRandSeed(rand_seed_policy);
		pol.setRDDL(rddl);
		
		StateViz viz = (StateViz)Class.forName(state_viz_class_name).newInstance();
			
		// Reset, pass a policy, a visualization interface, a random seed, and simulate!
		Result r = sim.run(pol, viz, rand_seed_sim);
		System.out.println("==> " + r);
	}
}
