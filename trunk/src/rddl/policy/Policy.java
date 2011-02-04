/**
 * RDDL: Implements abstract policy interface.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.policy;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;

public abstract class Policy {
	
	public Random _random = new Random();
	public long RAND_SEED = -1;	
	public String _sInstanceName;

	public Policy() {
		
	}
	public Policy(String instance_name) {
		_sInstanceName = instance_name;
	}
	
	public void setRandSeed(long rand_seed) {
		RAND_SEED = rand_seed;
		_random = new Random(RAND_SEED);
	}
	
	// Override if needed
	public void roundInit(double time_left, int horizon, int round_number, int total_rounds) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND INIT " + round_number + "/" + total_rounds + "; time remaining = " + time_left + ", horizon = " + horizon);
		System.out.println("*********************************************************");
	}
	
	// Override if needed
	public void roundEnd(double reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND END, reward = " + reward);
		System.out.println("*********************************************************");
	}
	
	// Override if needed
	public void sessionEnd(double total_reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> SESSION END, total reward = " + total_reward);
		System.out.println("*********************************************************");
	}

	// Must override
	public abstract ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException;
	
	public String toString() {
		return "Policy for '" + _sInstanceName + "'";
	}
}
