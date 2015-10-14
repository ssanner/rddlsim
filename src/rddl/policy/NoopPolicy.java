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
import rddl.det.mip.ReservoirStateViz;
import rddl.viz.StateViz;

public class NoopPolicy implements Policy {
	
	public NoopPolicy ( List<String> args ) { }
	
	private StateViz viz = new ReservoirStateViz();
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {

		viz.display(s , 0 );
		
		return new ArrayList<PVAR_INST_DEF>(); // NOOP

	}
}
