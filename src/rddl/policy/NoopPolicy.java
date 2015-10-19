/**
 * RDDL: NOOP Policy.
 * 
 * @author Scott Sanner (ssanner [at] gmail.com)
 * @version 4/1/11
 *
 **/

package rddl.policy;

import java.util.*;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.*;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.*;
import rddl.det.mip.PVarHeatMap;
import rddl.viz.StateViz;

public class NoopPolicy implements Policy {
	
	public NoopPolicy ( List<String> args ) { 
		if( args.get(0).equalsIgnoreCase("reservoir") ){
			viz = new PVarHeatMap( PVarHeatMap.reservoir_tags );			
		}else if( args.get(0).equalsIgnoreCase("inventory") ){
			viz = new PVarHeatMap( PVarHeatMap.inventory_tags );
		}
	}
	
	private StateViz viz = null;
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> noop = new ArrayList<PVAR_INST_DEF>();
		s.computeIntermFluents( noop, new RandomDataGenerator() );
		if( viz != null ){
			viz.display(s , 0 );
		}
		s.clearIntermFluents();
		return noop; // NOOP

	}
}
