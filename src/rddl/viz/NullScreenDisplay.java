/**
 * RDDL: Does not display any state information (exception for optimal time).
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;

public class NullScreenDisplay extends StateViz {

	public NullScreenDisplay() {
		_bShowTime = false;
	}

	public NullScreenDisplay(boolean show_time) {
		_bShowTime = show_time;
	}
	
	public boolean _bShowTime = false;
	
	public void display(State s, int time) {
		
		if (_bShowTime)
			System.out.println("\n============================\n" + 
			  	               "TIME = " + time + 
				           	   "\n============================\n");
	}

}
