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

public class TamariskDisplay extends StateViz {

	public TamariskDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public TamariskDisplay(int time_delay_per_frame) {
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

		TYPE_NAME slot_type = new TYPE_NAME("slot");
		ArrayList<LCONST> list_slot = s._hmObject2Consts.get(slot_type);

		TYPE_NAME reach_type = new TYPE_NAME("reach");
		ArrayList<LCONST> list_reach = s._hmObject2Consts.get(reach_type);
		
		PVAR_NAME tamarisk_at = new PVAR_NAME("tamarisk-at");
		PVAR_NAME native_at = new PVAR_NAME("native-at");

		if (_bd == null) {
			int max_row = list_reach.size() - 1;
			int max_col = (list_slot.size() - 1)*2;

			_bd= new BlockDisplay("RDDL Tamarisk Simulation", "RDDL Tamarisk Simulation", max_row + 2, max_col + 2);	
		}
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> params = new ArrayList<LCONST>(2);
		params.add(null);
		//params.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
//		for (LCONST xpos : list_reach) {
		for (LCONST slot : list_slot) {
			String[] split = slot._sConstValue.split("s"); // FIXME: How did a $ get in this _sConstValue?
			int sl = split.length;
			//System.out.println(slot + " : " + Arrays.asList(split) + " : " + slot.getClass() + " : " + new OBJECT_VAL("$test")._sConstValue);
			int row = new Integer(split[sl-2]) - 1;
			int col = new Integer(split[sl-1]);
			if (row % 2 == 0)
				col = col + list_slot.size() - 1;
			else
				col = list_slot.size() - col;
			params.set(0, slot);
			//params.set(1, new OBJECT_VAL("r" + row));
			//System.out.println(params);
			boolean b_tamarisk_at = (Boolean)s.getPVariableAssign(tamarisk_at, params);
			boolean b_native_at   = (Boolean)s.getPVariableAssign(native_at, params);
			
			String letter = null;
			if (b_tamarisk_at && b_native_at)
				letter = "X";
			
			Color color = Color.green;
			
			if (b_tamarisk_at && b_native_at)
				color = new Color(139, 69, 19); // brown
			else if (b_tamarisk_at) 
				color = Color.red;
			else if (b_native_at)
				color = Color.green;
			else 
				color = Color.white;
			
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

