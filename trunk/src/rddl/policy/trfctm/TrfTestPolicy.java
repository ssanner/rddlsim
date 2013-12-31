/**
 * RDDL: Fixed Time Policy for Signal Control of The Traffic Model.
 * 
 * @author Tan Nguyen (tan1889@gmail.com)
 * @version 6-May-2011
 *
 **/

package rddl.policy.trfctm;

import rddl.policy.Policy;
import java.util.*;

import rddl.*;
import rddl.RDDL.*;

public class TrfTestPolicy extends Policy {

	static int time = -1;
	static final int RED_TIME = 3;
	static final int OFFSET = 20 - RED_TIME; // offset between traffic lights
	
	static int[][] signalTimeOut;
	static int[][] signal;
	
	public TrfTestPolicy () {
		
	}
	
	public TrfTestPolicy(String instance_name) {
		super(instance_name);
	}
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		PVAR_NAME p = new PVAR_NAME("signal");
		ArrayList<ArrayList<LCONST>> intersections = s.generateAtoms(p);
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
		
		// initialize signal arrays
		if (time < 0) {
			int maxCol = 0, maxRow = 0; 
			for(ArrayList<LCONST> intersection: intersections)
				if (intersection.toString().startsWith("[I_")) {
					String[] split = intersection.toString().replace("]", "").split("[_a]");
					int col = new Integer(split[1]);			
					int row = new Integer(split[2]);
					if (col > maxCol) maxCol = col;
					if (row > maxRow) maxRow = row;
				}
			signal = new int[maxCol + 1][maxRow + 1];
			signalTimeOut = new int[maxCol + 1][maxRow + 1];
			signal[5][5] = 1;
			signalTimeOut[5][5] = 2*OFFSET;
			signal[5][11] = 1;
			signalTimeOut[5][11] = 3*OFFSET;
			signal[5][17] = 1;
			signalTimeOut[5][17] = 4*OFFSET;
			
			signal[11][5] = 1;
			signalTimeOut[11][5] = 2*OFFSET;
			signal[11][11] = 1;
			signalTimeOut[11][11] = 1*OFFSET;
			signal[11][17] = 1;
			signalTimeOut[11][17] = 2*OFFSET;

			signal[17][5] = 1;
			signalTimeOut[17][5] = 3*OFFSET;
			signal[17][11] = 1;
			signalTimeOut[17][11] = 2*OFFSET;
			signal[17][17] = 1;
			signalTimeOut[17][17] = 3*OFFSET;
			
		} 
		
		// apply signal control policy
		else {		
			// Get the list of enum values for this action
			PVARIABLE_DEF pdef = s._hmPVariables.get(p);
			ENUM_TYPE_DEF tdef = (ENUM_TYPE_DEF)s._hmTypes.get(pdef._typeRange);
			ArrayList<ENUM_VAL> enums = new ArrayList<ENUM_VAL>((ArrayList)tdef._alPossibleValues);

			for(ArrayList<LCONST> intersection: intersections){
				if (intersection.toString().startsWith("[I_")) {
					String[] split = intersection.toString().replace("]", "").split("[_a]");
					int col = new Integer(split[1]);			
					int row = new Integer(split[2]);
					ENUM_VAL r = enums.get(signal[col][row]);					
/*
					// time-out -> change the signal to next phase + set new time-out 
					if (signalTimeOut[col][row] <= time)
					{				
						signal[col][row] = (signal[col][row] + 1) % enums.size();
						r = enums.get(signal[col][row]);					
						if (r.toString().contains("RED"))
							signalTimeOut[col][row] = time + RED_TIME;
						else
							signalTimeOut[col][row] = time + OFFSET;
					}
*/					
					PVAR_INST_DEF d = new PVAR_INST_DEF(p._sPVarName, r, intersection);
					actions.add(d);
//					System.out.println("Action: {signal " + intersection + " = " + r + "} ...until TIME=" + signalTimeOut[col][row]);
					
				}
			}
		}

		time++;				
		// return the action list
		return actions;
	}

}
