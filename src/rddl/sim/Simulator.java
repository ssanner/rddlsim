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

public class Simulator {
	
	public State      _state;
	public INSTANCE   _i;
	public NONFLUENTS _n;
	public DOMAIN     _d;
	public Policy     _p;
	public StateViz   _v;
	public Random     _rand;
	
	public Simulator(RDDL rddl, String instance_name) throws Exception {
		_state = new State();

		// Set up instance, nonfluent, and domain information
		_i = rddl._tmInstanceNodes.get(instance_name);
		_n = null;
		if (_i._sNonFluents != null)
			_n = rddl._tmNonFluentNodes.get(_i._sNonFluents);
		_d = rddl._tmDomainNodes.get(_i._sDomain);
		if (_n != null && !_i._sDomain.equals(_n._sDomain))
			throw new Exception("Domain name of instance and fluents do not match: " + 
					_i._sDomain + " vs. " + _n._sDomain);
		
	}
	
	public void resetState() {
		//System.out.println("Resetting state:" +
		//		"\n  Types:      " + _d._hmTypes + 
		//		"\n  PVars:      " + _d._hmPVariables + 
		//		"\n  InitState:  " + _i._alInitState + 
		//		"\n  NonFluents: " + _n._alNonFluents);
		_state.init(_n != null ? _n._hmObjects : null, _i._hmObjects,  
				_d._hmTypes, _d._hmPVariables, _d._hmCPF,
				_i._alInitState, _n == null ? null : _n._alNonFluents);
	}
	
	//////////////////////////////////////////////////////////////////////////////

	public Result run(Policy p, StateViz v, long rand_seed) throws EvalException {
		
		// Reset to initial state
		resetState();
		
		// Set random seed for repeatability
		_rand = new Random(rand_seed);

		// Keep track of reward
		double accum_reward = 0.0d;
		double cur_discount = 1.0d;
		ArrayList<Double> rewards = new ArrayList<Double>(_i._nHorizon);
		
		// Run problem for specified horizon
		for (int t = 0; t < _i._nHorizon; t++) {
			
			// Get action from policy
			ArrayList<PVAR_INST_DEF> action_list = p.getActions(_state);
			
			// Compute next state (and all intermediate / observation variables)
			_state.computeNextState(action_list, _rand);
			
			// Calculate reward / objective and store
			double reward = ((Number)_d._exprReward.sample(new HashMap<LVAR,LCONST>(), _state, _rand)).doubleValue();
			rewards.add(reward);
			accum_reward += cur_discount * reward;
			cur_discount *= _i._dDiscount;
			
			// Display current state before advanced
			v.display(_state, t);
			
			// Done with this iteration, advance to next round
			_state.advanceNextState();
		}
		
		// Problem over, return objective and list of rewards (e.g., for std error calc)
		return new Result(accum_reward, rewards);
	}
	
	//////////////////////////////////////////////////////////////////////////////
	
	/** Test on SysAdmin **/
	public static void main(String[] args) throws Exception {
		
		// Parse file
		//RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin.rddl"));
		RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin_test.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/game_of_life.rddl"));
		
		// Get first instance name in file and create a simulator
		String instance_name = rddl._tmInstanceNodes.firstKey();
		Simulator s = new Simulator(rddl, instance_name);
		
		// Reset, pass a policy, a visualization interface, a random seed, and simulate!
		Result r = s.run(
				new RandomBoolPolicy(instance_name)
				/* new FixedBoolPolicy(instance_name),*/, 
				/*new GameOfLifeScreenDisplay(true)*/
				new GenericScreenDisplay(true)
				/* new SysAdminScreenDisplay(true)*/,
				123456);
		System.out.println("==> " + r);
	}
}
