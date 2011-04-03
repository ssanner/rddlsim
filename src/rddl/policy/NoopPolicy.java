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

///////////////////////////////////////////////////////////////////////////
//                             Helper Functions
///////////////////////////////////////////////////////////////////////////

public class NoopPolicy extends Policy {
	
	public NoopPolicy () { }
	
	public NoopPolicy(String instance_name) {
		super(instance_name);
	}

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {

		return new ArrayList<PVAR_INST_DEF>(); // NOOP

	}
}
