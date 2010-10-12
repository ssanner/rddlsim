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
	
	public Policy(String instance_name) {
		_sInstanceName = instance_name;
	}
	
	public String _sInstanceName;

	public abstract ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException;
	
	public String toString() {
		return "Policy for '" + _sInstanceName + "'";
	}
}
