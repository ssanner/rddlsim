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
import java.util.HashMap;
import java.util.Map;

import rddl.EvalException;
import rddl.RDDL.ENUM_VAL;
import rddl.RDDL.OBJECT_VAL;
import rddl.RDDL.STRUCT_VAL;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class MarsRoverDisplay extends StateViz {

	public static final int NUM_SCREEN_BLOCKS = 42;
	
	public MarsRoverDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public MarsRoverDisplay(int time_delay_per_frame) {
		_nTimeDelay = time_delay_per_frame; // in milliseconds
	}
	
	public boolean _bFirstPaint = true;
	public boolean _bSuppressNonFluents = false;
	public BlockDisplay _bd = null;
	public int _nTimeDelay;
	
	public HashMap<LCONST,STRUCT_VAL> robot2oldpos = new HashMap<LCONST,STRUCT_VAL>();
	
	public void display(State s, int time) {
		System.out.println("TIME = " + time + ": " + getStateDescription(s));
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) {
		StringBuilder sb = new StringBuilder();

		TYPE_NAME robot_type = new TYPE_NAME("robot");
		ArrayList<LCONST> list_robots = s._hmObject2Consts.get(robot_type);

		TYPE_NAME picture_point = new TYPE_NAME("picture-point");
		ArrayList<LCONST> list_picture_points = s._hmObject2Consts.get(picture_point);

		TYPE_NAME int_1_10 = new TYPE_NAME("int-1-10");
		ArrayList<LCONST> list_int_1_10 = s._hmObject2Consts.get(int_1_10);

		PVAR_NAME PICT_POS = new PVAR_NAME("PICT_POS");
		PVAR_NAME PICT_VALUE = new PVAR_NAME("PICT_VALUE");
		PVAR_NAME PICT_ERROR_ALLOW = new PVAR_NAME("PICT_ERROR_ALLOW");
		PVAR_NAME POS = new PVAR_NAME("pos");
		
		PVAR_NAME SNAP_PICTURE = new PVAR_NAME("snapPicture");
		PVAR_NAME PICT_REWARD_POSSIBLE = new PVAR_NAME("pictureRewardPossible");

		PVAR_NAME CUR_PICT  = new PVAR_NAME("curPict");
		PVAR_NAME RAND_INT  = new PVAR_NAME("randInt");
		PVAR_NAME RAND_MULT = new PVAR_NAME("randMult");
		PVAR_NAME RAND_DIR  = new PVAR_NAME("randDir");
		
		if (_bd == null) {
			_bd= new BlockDisplay("Multiagent Mars Rover", "Simulation", NUM_SCREEN_BLOCKS, NUM_SCREEN_BLOCKS);	
		}
		
		// Set up arity-1 and arity-2 parameter lists
		ArrayList<LCONST> params = new ArrayList<LCONST>(1);
		params.add(null);

		ArrayList<LCONST> params2 = new ArrayList<LCONST>(2);
		params2.add(null);
		params2.add(null);

		_bd.clearAllCells();
		_bd.clearAllText();
		//_bd.clearAllLines();
		int col_index = 0;
		for (LCONST robot : list_robots) {
			params.set(0, robot);
			STRUCT_VAL old_rpos = robot2oldpos.get(robot);
			STRUCT_VAL new_rpos = (STRUCT_VAL)s.getPVariableAssign(POS, params);
			Color col = _bd._colors[col_index++ % _bd._colors.length];
			
			double new_x = ((Number)new_rpos._alMembers.get(0)._oVal).doubleValue() + (NUM_SCREEN_BLOCKS/2);
			double new_y = ((Number)new_rpos._alMembers.get(1)._oVal).doubleValue() + (NUM_SCREEN_BLOCKS/2);
			_bd.addText(col, new_x, new_y, ((LCONST)robot)._sConstValue);
			if (old_rpos != null) {
				double old_x = ((Number)old_rpos._alMembers.get(0)._oVal).doubleValue() + (NUM_SCREEN_BLOCKS/2);
				double old_y = ((Number)old_rpos._alMembers.get(1)._oVal).doubleValue() + (NUM_SCREEN_BLOCKS/2);
				_bd.addLine(col, old_x, old_y, new_x, new_y);
			}
				
			if ((Boolean)s.getPVariableAssign(SNAP_PICTURE, params)) {
				boolean picture_reward_possible = false;
				for (LCONST pic_point : list_picture_points) {
					params2.set(0, robot);
					params2.set(1, pic_point);
					picture_reward_possible = picture_reward_possible ||
							(Boolean)s.getPVariableAssign(PICT_REWARD_POSSIBLE, params2);
				}
				_bd.addFillCircle(picture_reward_possible ? Color.RED : Color.BLACK, new_x, new_y, 0.5);
			}
				
			System.out.println("Robot " + robot + " @ " + new_rpos + ", curPict: " + 
					(OBJECT_VAL)s.getPVariableAssign(CUR_PICT, params));
			
			robot2oldpos.put(robot, new_rpos);
		}
		
		for (LCONST pic_point : list_picture_points) {
			params.set(0, pic_point);
			STRUCT_VAL ppos   = (STRUCT_VAL)s.getPVariableAssign(PICT_POS, params);
			double     pval   = ((Number)s.getPVariableAssign(PICT_VALUE, params)).doubleValue();
			double     perror = ((Number)s.getPVariableAssign(PICT_ERROR_ALLOW, params)).doubleValue();
			Color col = _bd._colors[col_index++ % _bd._colors.length];
			
			// Text is erased
			double pict_x = ((Number)ppos._alMembers.get(0)._oVal).doubleValue() + (NUM_SCREEN_BLOCKS/2);
			double pict_y = ((Number)ppos._alMembers.get(1)._oVal).doubleValue() + (NUM_SCREEN_BLOCKS/2);
			_bd.addText(col, pict_x, pict_y, ((LCONST)pic_point)._sConstValue + "[" + pval + "]");
			
			// Basic pic info drawing
			if (_bFirstPaint)
				_bd.addCircle(col, pict_x, pict_y, perror);
			
			System.out.println("Picture point " + pic_point + " @ " + ppos + " +/- " + pval + " [" + pval + "]");
		}
			
		for (LCONST i : list_int_1_10) {
			params.set(0, i);
			System.out.println("randInt[" + i + "] = "  + s.getPVariableAssign(RAND_INT, params));
			System.out.println("randMult[" + i + "] = " + s.getPVariableAssign(RAND_MULT, params));
			System.out.println("randDir[" + i + "] = "  + s.getPVariableAssign(RAND_DIR, params));
		}
		
		_bd.repaint();
		
		// Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
	    try {
			Thread.currentThread().sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}
				
	    _bFirstPaint = false;
	    
		return sb.toString();
	}
	
	public void close() {
		_bd.close();
	}
}

