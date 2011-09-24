/**
 * RDDL: POMCP variant of UCT for POMDPs.  (In progress.)
 * 
 * See the following reference:
 * 
 *   D. Silver and J. Veness.
 *   Monte-Carlo Planning in Large POMDPs.
 *   NIPS 2010.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 9/25/11
 *
 **/
package rddl.solver.pomdp.pomcp;

import java.util.ArrayList;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.policy.Policy;

public class POMCP extends Policy {

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		// TODO Auto-generated method stub
		return null;
	}

}
