/**
 * RDDL: UCT Implementation.  (In progress.)
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 9/25/11
 *
 * The original UCT paper is here:
 * 
 *    L. Kocsis and C. Szepesvári.
 *    Bandit based Monte-Carlo Planning.
 *    ECML 2006.
 *
 **/
package rddl.solver.mdp.uct;

import java.util.ArrayList;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.policy.Policy;

public class UCT extends Policy {

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		// TODO Auto-generated method stub
		return null;
	}

}
