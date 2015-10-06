/**
 * RDDL: NOOP Policy.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 4/1/11
 *
 **/

package rddl.policy;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;

public class NoopPolicy implements Policy {
	
	public NoopPolicy () { }
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {

		return new ArrayList<PVAR_INST_DEF>(); // NOOP

	}
}
