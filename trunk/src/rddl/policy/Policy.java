/**
 * RDDL: Implements abstract policy interface.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.policy;

import java.util.*;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.*;
import rddl.RDDL.*;

public abstract class Policy {
	
	public long RAND_SEED = -1;	

	public RandomDataGenerator _random = new RandomDataGenerator();
	public String _sInstanceName;
	public RDDL _rddl;

	public Policy() {
		
	}
	
	public Policy(String instance_name) {
		_sInstanceName = instance_name;
	}
	
	public void setInstance(String instance_name) {
		_sInstanceName = instance_name;
	}

	public void setRDDL(RDDL rddl) {
		_rddl = rddl;
	}

	public void setLimitTime(Integer time) {
	}
	
	public int getNumberUpdate() {
		return 0;
	}
	
	public void setRandSeed(long rand_seed) {
		RAND_SEED = rand_seed;
		_random = new RandomDataGenerator();
		_random.reSeed(RAND_SEED);
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
