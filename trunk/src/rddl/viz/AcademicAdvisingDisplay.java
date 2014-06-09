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

public class AcademicAdvisingDisplay extends StateViz {

	public AcademicAdvisingDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public AcademicAdvisingDisplay(int time_delay_per_frame) {
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

		TYPE_NAME course_type = new TYPE_NAME("course");
		ArrayList<LCONST> list_course = s._hmObject2Consts.get(course_type);

		PVAR_NAME PREREQ = new PVAR_NAME("PREREQ");
		PVAR_NAME PROGRAM_REQUIREMENT = new PVAR_NAME("PROGRAM_REQUIREMENT");
		PVAR_NAME passed = new PVAR_NAME("passed");
		PVAR_NAME taken  = new PVAR_NAME("taken");

		if (_bd == null) {
			int max_row = list_course.size() - 1;
			int max_col = list_course.size() + 3;

			_bd= new BlockDisplay("RDDL Academic Advising Simulation", "RDDL Academic Advising Simulation", max_row + 2, max_col + 2);	
		}
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> params2 = new ArrayList<LCONST>(2);
		params2.add(null);
		params2.add(null);
		ArrayList<LCONST> params1 = new ArrayList<LCONST>(1);
		params1.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
		for (int yindex = 0; yindex < list_course.size(); yindex++) {
			LCONST ycourse = list_course.get(yindex);
			params1.set(0, ycourse); // This is the target course
			boolean b_is_prog_req = (Boolean)s.getPVariableAssign(PROGRAM_REQUIREMENT, params1); // ycourse prog req?
			boolean b_passed      = (Boolean)s.getPVariableAssign(passed, params1); // ycourse passed?
			boolean b_taken       = (Boolean)s.getPVariableAssign(taken,  params1); // ycourse taken?
			
			String name = ycourse._sConstValue;

			Color color = Color.black;
			if (b_is_prog_req && b_passed)
				color = Color.green; // brown
			else if (b_is_prog_req && !b_passed && b_taken) 
				color = Color.red;
			else if (b_is_prog_req && !b_passed && !b_taken) 
				color = Color.orange;
			else if (!b_is_prog_req && b_passed)
				color = Color.blue;
			else if (b_taken)
				color = Color.gray;
			
			_bd.setCell(yindex, 0, color, name);
		}
		
		for (int xindex = 0; xindex < list_course.size(); xindex++) {
			LCONST xcourse = list_course.get(xindex);
			for (int yindex = 0; yindex < list_course.size(); yindex++) {
				LCONST ycourse = list_course.get(yindex);
				int col = 4 + xindex;
				int row = yindex;
				params2.set(0, xcourse);
				params2.set(1, ycourse);
				params1.set(0, ycourse); // This is the target course
				boolean b_is_prereq   = (Boolean)s.getPVariableAssign(PREREQ, params2); // xcourse prereq of ycourse?
				//boolean b_is_prog_req = (Boolean)s.getPVariableAssign(PROGRAM_REQUIREMENT, params1); // ycourse prog req?
				//boolean b_passed_y    = (Boolean)s.getPVariableAssign(passed, params1); // ycourse passed?
				//boolean b_taken_y     = (Boolean)s.getPVariableAssign(taken,  params1); // ycourse taken?
				params1.set(0, xcourse); // This is the target course
				boolean b_passed_x    = (Boolean)s.getPVariableAssign(passed, params1); // xcourse passed?
				//boolean b_taken_x     = (Boolean)s.getPVariableAssign(taken,  params1); // ycourse taken?
				
				String letter = null;
				if (b_is_prereq)
					letter = "P";
				
				Color color = Color.white;
				
				if (b_passed_x)
					color = Color.green;
				else if (b_is_prereq && !b_passed_x) 
					color = Color.red;
				
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

