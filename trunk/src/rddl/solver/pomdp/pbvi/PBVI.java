/**
 * RDDL: Factored Point-based Value Iteration for POMDPs.  (In progress.)
 * 
 * The intended implementation will follow Symbolic Perseus:
 * 
 *   Exploiting Structure to Efficiently Solve Large Scale Partially. 
 *   Observable Markov Decision Processes.
 *   Pascal Poupart
 *   PhD thesis, Department of Computer Science, University of Toronto, 2005.
 *   (Symbolic Perseus is discussed in Chapter 5.)
 *   
 * also including ideas from this paper:
 * 
 *   G. Shani, R. I. Brafman, S. E. Shimony and P. Poupart.
 *   Efficient ADD Operations for Point-Based Algorithms.
 *   ICAPS 2008.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 9/25/11
 *
 **/
package rddl.solver.pomdp.pbvi;

import java.util.ArrayList;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.policy.Policy;

public class PBVI extends Policy {

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		// TODO Auto-generated method stub
		return null;
	}

}
