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

public class CrossingTrafficDisplay extends StateViz {

	public CrossingTrafficDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public CrossingTrafficDisplay(int time_delay_per_frame) {
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

		TYPE_NAME xpos_type = new TYPE_NAME("xpos");
		ArrayList<LCONST> list_xpos = s._hmObject2Consts.get(xpos_type);

		TYPE_NAME ypos_type = new TYPE_NAME("ypos");
		ArrayList<LCONST> list_ypos = s._hmObject2Consts.get(ypos_type);
		
		PVAR_NAME GOAL = new PVAR_NAME("GOAL");
		PVAR_NAME robot_at = new PVAR_NAME("robot-at");
		PVAR_NAME obstacle_at = new PVAR_NAME("obstacle-at");

		if (_bd == null) {
			int max_row = list_ypos.size() - 1;
			int max_col = list_xpos.size() - 1;

			_bd= new BlockDisplay("RDDL Crossing Traffic Simulation", "RDDL Crossing Traffic Simulation", max_row + 2, max_col + 2);	
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
				boolean is_goal  = (Boolean)s.getPVariableAssign(GOAL, params);
				boolean robot    = (Boolean)s.getPVariableAssign(robot_at, params);
				boolean obstacle = (Boolean)s.getPVariableAssign(obstacle_at, params);
				
				if (robot && is_goal)
					_bd.setCell(row, col, Color.red, "G!");
				else if (is_goal) 
					_bd.setCell(row, col, Color.cyan, "G");
				else if (robot && !obstacle)
					_bd.setCell(row, col, Color.blue, null);
				else if (robot && obstacle)
					_bd.setCell(row, col, Color.black, "X");
				else if (obstacle)
					_bd.setCell(row, col, Color.green, null);
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

