/**
 * RDDL: Factored Value Iteration for POMDPs.  (In progress.)
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 9/25/11
 *
 * The intended implementation will follow the value iteration subset of 
 * Symbolic Perseus:
 * 
 *   Exploiting Structure to Efficiently Solve Large Scale Partially. 
 *   Observable Markov Decision Processes.
 *   Pascal Poupart
 *   PhD thesis, Department of Computer Science, University of Toronto, 2005.
 *   (Symbolic Perseus is discussed in Chapter 5.)
 *   
 **/
package rddl.solver.pomdp.vi;

import java.util.ArrayList;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.policy.Policy;

public class VI extends Policy {

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		// TODO Auto-generated method stub
		return null;
	}

}
