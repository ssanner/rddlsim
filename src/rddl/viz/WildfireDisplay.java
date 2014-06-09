/**
 * RDDL: A simple graphics display for the Sidewalk domain. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class WildfireDisplay extends StateViz {

	public WildfireDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public WildfireDisplay(int time_delay_per_frame) {
		_nTimeDelay = time_delay_per_frame; // in milliseconds
	}
	
	public boolean _bSuppressNonFluents = false;
	public BlockDisplay _bd = null;
	public int _nTimeDelay = 0;
	
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

		TYPE_NAME xpos_type = new TYPE_NAME("x_pos");
		ArrayList<LCONST> list_xpos = s._hmObject2Consts.get(xpos_type);

		TYPE_NAME ypos_type = new TYPE_NAME("y_pos");
		ArrayList<LCONST> list_ypos = s._hmObject2Consts.get(ypos_type);
		
		PVAR_NAME TARGET = new PVAR_NAME("TARGET");
		PVAR_NAME burning = new PVAR_NAME("burning");
		PVAR_NAME out_of_fuel = new PVAR_NAME("out-of-fuel");

		if (_bd == null) {
			int max_row = list_ypos.size() - 1;
			int max_col = list_xpos.size() - 1;

			_bd= new BlockDisplay("RDDL Wildfire Simulation", "RDDL Wildfire Simulation", max_row + 2, max_col + 2);	
		}
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> params = new ArrayList<LCONST>(2);
		params.add(null);
		params.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
		for (LCONST xpos : list_xpos) {
			for (LCONST ypos : list_ypos) {
				int col = new Integer(xpos.toString().substring(2, xpos.toString().length()));
				int row = new Integer(ypos.toString().substring(2, ypos.toString().length())) - 1;
				params.set(0, xpos);
				params.set(1, ypos);
				boolean b_is_target   = (Boolean)s.getPVariableAssign(TARGET, params);
				boolean b_burning     = (Boolean)s.getPVariableAssign(burning, params);
				boolean b_out_of_fuel = (Boolean)s.getPVariableAssign(out_of_fuel, params);
				
				String letter = null;
				if (b_is_target)
					letter = "T";
				
				Color color = Color.green;
				
				if (b_burning && b_out_of_fuel)
					color = new Color(139, 69, 19); // brown
				else if (b_burning) 
					color = Color.red;
				else if (b_out_of_fuel)
					color = Color.black;
				
				_bd.setCell(row, col, color, letter);
			}
		}
			
		_bd.repaint();
		
		// Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
	    try {
			Thread.currentThread().sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}
				
		return sb.toString();
	}
	
	public void close() {
		_bd.close();
	}
}

