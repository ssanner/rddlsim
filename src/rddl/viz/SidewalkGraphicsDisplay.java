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

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class SidewalkGraphicsDisplay extends StateViz {

	public SidewalkGraphicsDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public SidewalkGraphicsDisplay(int time_delay_per_frame) {
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

		// Initialize display if not already
		PVAR_NAME sidewalk_size = new PVAR_NAME("SIDEWALK-SIZE");
		PVARIABLE_DEF pvar_def = s._hmPVariables.get(sidewalk_size);
		int SIZE_X = (Integer)s.getPVariableAssign(sidewalk_size, new ArrayList<LCONST>());
		int SIZE_Y = 2;
		if (_bd == null)
			_bd= new BlockDisplay("RDDL Sidewalk Simulation", "RDDL Sidewalk Simulation", SIZE_Y, SIZE_X);	
		
		PVAR_NAME goal = new PVAR_NAME("GOAL");
		PVARIABLE_DEF goal_def = s._hmPVariables.get(goal);
		PVAR_NAME xpos = new PVAR_NAME("xPos");
		PVARIABLE_DEF xpos_def = s._hmPVariables.get(xpos);
		PVAR_NAME ypos = new PVAR_NAME("yPos");
		PVARIABLE_DEF ypos_def = s._hmPVariables.get(ypos);

		TYPE_NAME person_type = new TYPE_NAME("person");
		ArrayList<LCONST> persons = s._hmObject2Consts.get(person_type);

		// Set up an arity-1 parameter list
		ArrayList<LCONST> params = new ArrayList<LCONST>(1);
		params.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
		for (int i = 0; i < persons.size(); i++) {
			
			// Get person info and select a color
			LCONST person = persons.get(i);
			params.set(0, person);
			Color c = _bd._colors[i % _bd._colors.length];
			
			// Get state values
			int goal_pos = (Integer)s.getPVariableAssign(goal, params);
			int x_pos = (Integer)s.getPVariableAssign(xpos, params);
			int y_pos = (Integer)s.getPVariableAssign(ypos, params);
			System.out.println(person + " @ (" + x_pos + "," + y_pos + ")");
			
			// Update screen
			//_bd.setCell(1, goal_pos, c, "*");
			_bd.addLine(c, goal_pos, 0, goal_pos + 1, 2); // Here x first, y second
			_bd.setCell(y_pos, x_pos, c, null);
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

