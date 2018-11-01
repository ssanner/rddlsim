/**
 * RDDL: A simple graphics display for the Ambulance domain. 
 * 
 * @author Parth Jaggi (parthjaggi@iitrpr.ac.in)
 * @version 31/10/18
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

public class AmbulanceDisplay extends StateViz {

	public AmbulanceDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public AmbulanceDisplay(int time_delay_per_frame) {
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
		
		TYPE_NAME ambl_type = new TYPE_NAME("ambulance");
		ArrayList<LCONST> ambulances = s._hmObject2Consts.get(ambl_type);

		TYPE_NAME xpos_type = new TYPE_NAME("x_pos");
		ArrayList<LCONST> list_xpos = s._hmObject2Consts.get(xpos_type);

		TYPE_NAME ypos_type = new TYPE_NAME("y_pos");
		ArrayList<LCONST> list_ypos = s._hmObject2Consts.get(ypos_type);

		PVAR_NAME HOSPITAL = new PVAR_NAME("HOSPITAL");
		PVAR_NAME emergency_at = new PVAR_NAME("emergency-at");
		PVAR_NAME ambulance_at = new PVAR_NAME("ambulance-at");
		PVAR_NAME with_patient = new PVAR_NAME("with-patient");

		if (_bd == null) {
			int max_row = list_ypos.size() - 1;
			int max_col = list_xpos.size() - 1;

			_bd= new BlockDisplay("RDDL Ambulance Simulation", "RDDL Ambulance Simulation", max_row + 2, max_col + 2);	
		}
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> params1 = new ArrayList<LCONST>(2);
		params1.add(null);
		params1.add(null);

		ArrayList<LCONST> params2 = new ArrayList<LCONST>(3);
		params2.add(null);
		params2.add(null);
		params2.add(null);

		ArrayList<LCONST> params3 = new ArrayList<LCONST>(1);
		params3.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
		for (LCONST xpos : list_xpos) {
			for (LCONST ypos : list_ypos) {
				for (LCONST ambl : ambulances) {
					int col = new Integer(xpos.toString().substring(2, xpos.toString().length()));
					int row = new Integer(ypos.toString().substring(2, ypos.toString().length())) - 1;

					params1.set(0, xpos);
					params1.set(1, ypos);
					params2.set(0, ambl);
					params2.set(1, xpos);
					params2.set(2, ypos);
					params3.set(0, ambl);

					boolean b_is_hospital  = (Boolean)s.getPVariableAssign(HOSPITAL, params1);
					boolean b_emergency_at = (Boolean)s.getPVariableAssign(emergency_at, params1);
					boolean b_ambulance_at = (Boolean)s.getPVariableAssign(ambulance_at, params2);
					boolean b_with_patient = (Boolean)s.getPVariableAssign(with_patient, params3);
					
					String letter = null;
					if (b_is_hospital)
						letter = "H";
					
					Color color = Color.green;

					if (b_emergency_at && b_ambulance_at)
						color = Color.pink;
					else if (b_ambulance_at && b_with_patient)
						color = Color.yellow;
					else if (b_emergency_at)
						color = Color.red;
					else if (b_ambulance_at)
						color = Color.white;

					_bd.setCell(row, col, color, letter);

					// If a0 is found on cell, color regarding a1 would not be displayed on it.
					if (b_ambulance_at)
						break;
				}
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

