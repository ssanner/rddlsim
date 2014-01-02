/**
 * RDDL: A simple text display for the SysAdmin domain state. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.util.ArrayList;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class GameOfLifeScreenDisplay extends StateViz {

	public GameOfLifeScreenDisplay() {
		_bSuppressNonFluents = true;
	}
	
	public GameOfLifeScreenDisplay(boolean suppress_nonfluents) {
		_bSuppressNonFluents = suppress_nonfluents;
	}
	
	public boolean _bSuppressNonFluents = false;
	
	public void display(State s, int time) {
		try {
			System.out.println("TIME = " + time + ": " + getStateDescription(s));
		} catch (EvalException e) {
			System.out.println("\n\nError during visualization:\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) throws EvalException {
		StringBuilder sb = new StringBuilder();
		
		PVAR_NAME alive = new PVAR_NAME("alive");
		PVARIABLE_DEF pvar_def = s._hmPVariables.get(alive);

		TYPE_NAME type_x = new TYPE_NAME("x_pos");
		ArrayList<LCONST> x_objects = s._hmObject2Consts.get(type_x);

		TYPE_NAME type_y = new TYPE_NAME("y_pos");
		ArrayList<LCONST> y_objects = s._hmObject2Consts.get(type_y);

		// Set up an arity-2 parameter list
		ArrayList<LCONST> params = new ArrayList<LCONST>(2);
		params.add(null);
		params.add(null);
		
		sb.append("\n\n");
		
		// Show the status of alive(x,y) at each point (x,y) in the grid
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

