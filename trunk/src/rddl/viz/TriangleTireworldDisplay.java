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
import java.util.Arrays;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.*;

public class TriangleTireworldDisplay extends StateViz {

	public TriangleTireworldDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public TriangleTireworldDisplay(int time_delay_per_frame) {
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

		TYPE_NAME location_type = new TYPE_NAME("location");
		ArrayList<LCONST> list_location = s._hmObject2Consts.get(location_type);

		PVAR_NAME vehicle_at = new PVAR_NAME("vehicle-at");
		PVAR_NAME spare_in = new PVAR_NAME("spare-in");
		PVAR_NAME goal_location = new PVAR_NAME("goal-location"); 
		PVAR_NAME not_flattire = new PVAR_NAME("not-flattire");
		PVAR_NAME hasspare = new PVAR_NAME("hasspare"); 
		PVAR_NAME goal_reward_received = new PVAR_NAME("goal-reward-received"); 

		if (_bd == null) {
			int dim = (int)Math.floor(Math.sqrt(2*list_location.size()));
			int max_row = dim;
			int max_col = dim;

			_bd= new BlockDisplay("RDDL Triangle Tireworld Simulation", "RDDL Triangle Tireworld Simulation", max_row + 2, max_col + 2);	
		}
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> empty = new ArrayList<LCONST>(0);
		ArrayList<LCONST> params = new ArrayList<LCONST>(1);
		params.add(null);
		//params.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
//		for (LCONST xpos : list_reach) {
		for (LCONST loc : list_location) {
			params.set(0, loc);
			//System.out.println(params);

			String[] split = loc._sConstValue.split("a"); // FIXME: How did a $ get in this _sConstValue?
			int sl = split.length;
			//System.out.println(slot + " : " + Arrays.asList(split) + " : " + slot.getClass() + " : " + new OBJECT_VAL("$test")._sConstValue);
			int row = new Integer(split[sl-2]) - 1;
			int col = new Integer(split[sl-1]);
			
			boolean b_vehicle_at = (Boolean)s.getPVariableAssign(vehicle_at, params);
			boolean b_spare_in   = (Boolean)s.getPVariableAssign(spare_in, params);
			boolean b_goal_location = (Boolean)s.getPVariableAssign(goal_location, params);
			boolean b_not_flattire = (Boolean)s.getPVariableAssign(not_flattire, empty);
			boolean b_hasspare   = (Boolean)s.getPVariableAssign(hasspare, empty);
			boolean b_goal_reward_received = (Boolean)s.getPVariableAssign(goal_reward_received, empty);
			
			// ! - vehicle-at and goal-location
			// G - goal-location
			// V - vehicle-at and not-flat
			// F - vehicle-at and flat
			String letter = null;
			if (b_vehicle_at && b_goal_location)
				letter = "!";
			else if (b_goal_location)
				letter = "G";
			else if (b_vehicle_at && b_hasspare)
				letter = "H";
			else if (b_vehicle_at && b_not_flattire) // no hasspare
				letter = "V";
			else if (b_vehicle_at && !b_not_flattire) // no hasspare (but still could be spare-in - green)
				letter = "F";
			
			// black default
			// yellow if vehicle-at and goal-reward received
			// else red if vehicle-at and flat but no spare-in
			// else green if has spare, 
			Color color = Color.black;
			
			if (b_vehicle_at && b_goal_reward_received)
				color = Color.yellow;
			else if (b_vehicle_at && !b_not_flattire && !b_hasspare && !b_spare_in) 
				color = Color.red;
			else if (b_spare_in)
				color = Color.green;
			
			_bd.setCell(row, col, color, letter);
		}
//		}
			
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

