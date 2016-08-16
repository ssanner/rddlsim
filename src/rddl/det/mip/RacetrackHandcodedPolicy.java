package rddl.det.mip;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.policy.Policy;
import rddl.RDDL.TYPE_NAME;

/**
 * (x0,y0) -> (x0,yg)
 * assume (x0,y0) -> (xt, y0) -> (xt,yg) -> (x0,yg)
 * 
 * to go from (x1,y1) -> (x1,y2)
 *  |
 * v|  /\
 *  | /  \
 *  |/____\____t__
 *  
 * a|_____
 *  |     | 
 *  |_____|___t__
 *  |     |    |
 *  |     |____|
 *  
 *  area = 0.5vt = (y2-y1) => 0.5 (0.5 MAX-GAS t) t = (y2-y1) 
 *  slope = 2v/t = MAX-GAS => v = 0.5 MAX-GAS t
 *  
 *  t even < = 20
 *  
 */
public class RacetrackHandcodedPolicy implements Policy {
	
	public RacetrackHandcodedPolicy( List<String> args ) {

	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
	}

}
