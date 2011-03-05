/**
 * RDDL: A simple text display for the SysAdmin domain state. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;

public class SysAdminScreenDisplay extends StateViz {

	public SysAdminScreenDisplay() {
		_bSuppressNonFluents = true;
	}

	public SysAdminScreenDisplay(boolean suppress_nonfluents) {
		_bSuppressNonFluents = suppress_nonfluents;
	}
	
	public boolean _bSuppressNonFluents = false;
	
	public void display(State s, int time) {
		
		System.out.println("TIME = " + time + ": " + getStateDescription(s));
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) {
		StringBuilder sb = new StringBuilder();
		
		PVAR_NAME state = new PVAR_NAME("running");
		PVAR_NAME obs   = new PVAR_NAME("running-obs");
		
		try {
			// Go through all term groundings for variable p
			ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(state);										
			for (ArrayList<LCONST> gfluent : gfluents)
				sb.append(((Boolean)s.getPVariableAssign(state, gfluent) ? "." : "X"));
			
			if (s._hmPVariables.get(obs) != null) {
				sb.append(" ---obs--> ");
				for (ArrayList<LCONST> gfluent : gfluents)
					sb.append(((Boolean)s.getPVariableAssign(obs, gfluent) ? "." : "X"));
			}
						
		} catch (EvalException ex) {
			sb.append("- could not retrieve assignment for " + state + "/" + obs + "\n");
		}
				
		return sb.toString();
	}
}
