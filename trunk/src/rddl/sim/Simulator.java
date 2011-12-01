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
	
	public void resetState() {
		//System.out.println("Resetting state:" +
		//		"\n  Types:      " + _d._hmTypes + 
		//		"\n  PVars:      " + _d._hmPVariables + 
		//		"\n  InitState:  " + _i._alInitState + 
		//		"\n  NonFluents: " + _n._alNonFluents);
		_state.init(_n != null ? _n._hmObjects : null, _i._hmObjects,  
				_d._hmTypes, _d._hmPVariables, _d._hmCPF,
				_i._alInitState, _n == null ? null : _n._alNonFluents,
				_d._alStateConstraints, _d._exprReward, _i._nNonDefActions);
	}
	
	//////////////////////////////////////////////////////////////////////////////

	public Result run(Policy p, StateViz v, long rand_seed) throws EvalException {
		
		// Signal start of new session-independent round
		p.roundInit(Double.MAX_VALUE, _i._nHorizon, 1, 1);
		
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

			// Display state/observations that the agent sees
			v.display(_state, t);

			// Get action from policy 
			// (if POMDP and first state, no observations available yet so a null is passed)
			State state_info = ((_state._alObservNames.size() > 0) && t == 0) ? null : _state;
			ArrayList<PVAR_INST_DEF> action_list = p.getActions(state_info);
			
			// Check state-action constraints
			_state.checkStateActionConstraints(action_list);
			
			// Compute next state (and all intermediate / observation variables)
			_state.computeNextState(action_list, _rand);
			
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
			
		//try {	
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
			RDDL rddl = new RDDL();
			File f = new File(rddl_file);
			if (f.isDirectory()) {
				for (File f2 : f.listFiles())
					if (f2.getName().endsWith(".rddl")) {
						System.out.println("Loading: " + f2);
						rddl.addOtherRDDL(parser.parse(f2));
					}
			} else
				rddl.addOtherRDDL(parser.parse(f));
			
			Simulator sim = new Simulator(rddl, instance_name);
			Policy pol = (Policy)Class.forName(policy_class_name).getConstructor(
					new Class[]{String.class}).newInstance(new Object[]{instance_name});
			pol.setRandSeed(rand_seed_policy);
			pol.setRDDL(rddl);
			
			StateViz viz = (StateViz)Class.forName(state_viz_class_name).newInstance();
		
			// Parse file
			//RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin_test.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/game_of_life.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/game_of_life_stoch.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/sidewalk.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/dbn_prop.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/dbn_types_interm_po.rddl"));
			//RDDL rddl = parser.parse(new File("files/rddl/test/traffic_binary_ctm.rddl"));
		
			// Get first instance name in file and create a simulator
			//String instance_name = rddl._tmInstanceNodes.firstKey();
			
			// Reset, pass a policy, a visualization interface, a random seed, and simulate!
			Result r = sim.run(pol, viz, rand_seed_sim);
//				new RandomBoolPolicy(instance_name),
//				new RandomEnumPolicy(instance_name),
//				new FixedBoolPolicy(instance_name), 
//				new GameOfLifeScreenDisplay(true),
//				new SidewalkGraphicsDisplay(200),
//				new GenericScreenDisplay(true),
//				new SysAdminScreenDisplay(true),
//				new TrafficDisplay(300),
//				123456);
			System.out.println("==> " + r);
		//} catch (EvalException e) {
		//	System.err.println(e);
		//}
			
		if (RDDL.DEBUG_PVAR_NAMES)
			System.out.println(RDDL.PVAR_SRC_SET);
	}
}
