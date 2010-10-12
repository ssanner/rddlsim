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
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class GameOfLifeScreenDisplay extends StateViz {

	public GameOfLifeScreenDisplay(boolean suppress_nonfluents) {
		_bSuppressNonFluents = suppress_nonfluents;
	}
	
	public boolean _bSuppressNonFluents = false;
	
	public void display(State s, int time) {
		
		System.out.println("TIME = " + time + ": " + getStateDescription(s));
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) {
		StringBuilder sb = new StringBuilder();
		
		PVAR_NAME alive = new PVAR_NAME("alive");
		PVARIABLE_DEF pvar_def = s._hmPVariables.get(alive);
		TYPE_NAME type_x = pvar_def._alParamTypes.get(0);
		TYPE_NAME type_y = pvar_def._alParamTypes.get(1);
		ArrayList<LCONST> x_objects = s._hmObject2Consts.get(type_x);
		ArrayList<LCONST> y_objects = s._hmObject2Consts.get(type_y);
		ArrayList<LCONST> params = new ArrayList<LCONST>(2);
		params.add(new LCONST("X"));
		params.add(new LCONST("Y"));
		
		sb.append("\n\n");
		for (LCONST y : y_objects) {
			for (LCONST x : x_objects) {
				params.set(0,x);
				params.set(1,y);
				//sb.append("" + alive + params + "=");
				sb.append((Boolean)s.getPVariableAssign(alive, params) ? "X" : ".");
			}
			sb.append("\n");
		}			
				
		return sb.toString();
	}
}

