/**
 * RDDL: A simple graphics display for the Elevators domain. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class ElevatorDisplay extends StateViz {

	public ElevatorDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public ElevatorDisplay(int time_delay_per_frame) {
		_nTimeDelay = time_delay_per_frame; // in milliseconds
	}
	
	public boolean _bSuppressNonFluents = true;
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

		// Get constant lists for floor and elevator
		TYPE_NAME floor_type = new TYPE_NAME("floor");
		ArrayList<LCONST> floors = s._hmObject2Consts.get(floor_type);

		TYPE_NAME elev_type = new TYPE_NAME("elevator");
		ArrayList<LCONST> elevators = s._hmObject2Consts.get(elev_type);

		// Name defs
		PVAR_NAME pw_up = new PVAR_NAME("person-waiting-up"); // floor
		PVAR_NAME pw_dn = new PVAR_NAME("person-waiting-down"); // floor
		PVAR_NAME pe_up = new PVAR_NAME("person-in-elevator-going-up"); // elevator
		PVAR_NAME pe_dn = new PVAR_NAME("person-in-elevator-going-down"); // elevator
		PVAR_NAME e_dir_up = new PVAR_NAME("elevator-dir-up"); // elevator
		PVAR_NAME e_cl     = new PVAR_NAME("elevator-closed"); // elevator
		PVAR_NAME e_at_fl  = new PVAR_NAME("elevator-at-floor"); // elevator
		
		int max_row = floors.size();
		int max_col = 5 + 5*elevators.size();
		if (_bd == null) {
			_bd= new BlockDisplay("RDDL Elevator Simulation", "RDDL Elevator Simulation", max_row + 2, max_col + 2);	
		}
		
		// Set up an arity-1 parameter list
		ArrayList<LCONST> params1 = new ArrayList<LCONST>(1);
		params1.add(null);

		ArrayList<LCONST> params2 = new ArrayList<LCONST>(1);
		params2.add(null);
		params2.add(null);

		// Get HashMap from elevator to floor
		HashMap<LCONST, Integer> elev2floor = new HashMap<LCONST, Integer>();
		for (LCONST elev : elevators) {
			for (int floor_index = 1; floor_index <= floors.size(); floor_index++) {
				LCONST floor = floors.get(floor_index-1);
				params2.set(0, elev);
				params2.set(1, floor);
				if ( (Boolean)s.getPVariableAssign(e_at_fl, params2) )
					elev2floor.put(elev, floor_index);
			}
		}

		_bd.clearAllCells();
		_bd.clearAllLines();
		
		///////////////////////////////////
		//      f3    3
		//      f2    2
		// ud^o f1 ud 1
		// 1234567890 
		///////////////////////////////////
		for (int floor_index = 1; floor_index <= floors.size(); floor_index++) {
			
			LCONST floor = floors.get(floor_index-1);
			int row = floors.size() - floor_index + 1;
			
			// Go through elevators, outputting elevator info where needed
			for (int elev_index = 1; elev_index <= elevators.size(); elev_index++) {
				
				LCONST elev = elevators.get(elev_index-1);
				if (elev2floor.get(elev) == floor_index) {
					int col_offset = 1 + (elev_index-1)*5;
					params1.set(0, elev);
					
					boolean person_dn = (Boolean)s.getPVariableAssign(pe_dn, params1);
					boolean person_up = (Boolean)s.getPVariableAssign(pe_up, params1);
					boolean elev_closed = (Boolean)s.getPVariableAssign(e_cl, params1);
					boolean elev_up = (Boolean)s.getPVariableAssign(e_dir_up, params1);
					
					if (person_up)
						_bd.setCell(row, col_offset, elev_up ? Color.black : Color.red, "u");
					if (person_dn)
						_bd.setCell(row, col_offset + 1, elev_up ? Color.red : Color.black, "d");
					_bd.setCell(row, col_offset + 2, Color.black, elev_up ? "^" : "v");
					_bd.setCell(row, col_offset + 3, Color.black, elev_closed ? "|" : "<");
				}
			}
			
			params1.set(0, floor);
			
			int col_offset = 1 + elevators.size() * 5;
			boolean person_dn = (Boolean)s.getPVariableAssign(pw_dn, params1);
			boolean person_up = (Boolean)s.getPVariableAssign(pw_up, params1);
			
			if (person_dn)
				_bd.setCell(row, col_offset, Color.blue, "d");
			if (person_up)
				_bd.setCell(row, col_offset + 1, Color.blue, "u");
			_bd.addLine(Color.black, 1, 1, max_col, 1);
			_bd.addLine(Color.black, 1, floors.size() + 1, max_col, floors.size() + 1);
		}
		_bd.repaint();
		
		// Go through all variable types (state, interm, observ, action, nonfluent)
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : s._hmTypeMap.entrySet()) {
			
			if (_bSuppressNonFluents && e.getKey().equals("nonfluent"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) 
				try {
					// Go through all term groundings for variable p
					ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);										
					for (ArrayList<LCONST> gfluent : gfluents)
						sb.append("- " + e.getKey() + ": " + p + 
								(gfluent.size() > 0 ? gfluent : "") + " := " + 
								s.getPVariableAssign(p, gfluent) + "\n");
						
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment" + s + " for " + p + "\n");
				}
		}
		
		// Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
	    try {
			Thread.currentThread().sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}
				
		return sb.toString();
	}
	
	/*
	public drawElevators(State s) {
		
		if (_bd != null) {

		    _bd.clearAllCells();
		    _bd.clearAllLines();

		    // Format: Elevator in col 0, 
		    _bd.setCell(_nFloors - _nElevPos - 1, 0, 
				"Open".equalsIgnoreCase(_sCurAction) ? Color.white : Color.gray, null);

		    // Vert Line b/w 0,1...0,_nFloors
		    _bd.addLine(Color.black, 0, 0, 0, _nFloors);
		    _bd.addLine(Color.black, 1, 0, 1, _nFloors);

		    // Horiz Line b/w 0,_nFloors...MAX_WIDTH,_nFloors
		    _bd.addLine(Color.black, 0, 0, 0, 1);
		    _bd.addLine(Color.black, 0, _nFloors, MAX_WIDTH, _nFloors);

		    // People go from col 1... MAX_WIDTH
		    int i,j;
		    for (i = 0; i < _nFloors; i++) {
			for (j = 0; j < _aFloors[i].size() && j < MAX_WIDTH - 4; j++) {
			    Person p = (Person)_aFloors[i].get(j);
			    _bd.setCell(_nFloors - i - 1, j + 1, _colors[p._nGroup], p.getLetter());
			    //System.out.println("Floor[" + i + "] = " + j + ": " + p);
			}
			if (_aFloors[i].size() >= (MAX_WIDTH - 4)) {
			    _bd.setCell(_nFloors - i - 1, MAX_WIDTH - 3, Color.black, 
					"+"+(_aFloors[i].size()-(MAX_WIDTH - 4)));
			}
		    }
		    // Floors go from (fl-_nFloors)..._nFloors

		    // Elev people go on bottom
		    _bd.setCell(_nFloors + 1, 0, Color.gray, null);
		    Iterator pi = _hmPerson2Floor.keySet().iterator();
		    int elev_people = 1;
		    while (pi.hasNext()) {
			Person p = (Person)pi.next();
			if (p._nCurFloor == -1) {
			    elev_people++;
			    if (elev_people < (MAX_WIDTH-2)) {
				_bd.setCell(_nFloors + 1, elev_people, _colors[p._nGroup], p.getLetter());
			    }
			}
		    }
		    if (elev_people >= (MAX_WIDTH - 4)) {
			 _bd.setCell(_nFloors + 1, MAX_WIDTH - 3, Color.black, 
				     ""+(elev_people-(MAX_WIDTH - 4)));
		    }
		    
		    // Show current action
		    _bd.setMessage("Step = " + _nStep + "/" + _nStepsMax + 
				   ",  Action=" + _sCurAction + 
				   ",  Immed/Accum Value = " + _df.format(_dImmedValue) +
				   "/" + _df.format(_dAccumValue));

		    _bd.repaint();
		}
	}
	*/
	
	public void close() {
		_bd.close();
	}
}

