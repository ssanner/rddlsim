/**

 * RDDL: Implements abstract policy interface.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 * @author Aswin Raghavan
 * Refactored into an interface.
 **/

package rddl.policy;

import java.util.*;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.*;
import rddl.RDDL.*;

public interface Policy {
	
	// Override if needed
	public default void roundInit(double time_left, int horizon, int round_number, int total_rounds) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND INIT " + round_number + "/" + total_rounds + "; time remaining = " + time_left + ", horizon = " + horizon);
		System.out.println("*********************************************************");
	}
	
	// Override if needed
	public default void roundEnd(double reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> ROUND END, reward = " + reward);
		System.out.println("*********************************************************");
	}
	
	// Override if needed
	public default void sessionEnd(double total_reward) {
		System.out.println("\n*********************************************************");
		System.out.println(">>> SESSION END, total reward = " + total_reward);
		System.out.println("*********************************************************");
	}

	// Must override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException;
	
}
