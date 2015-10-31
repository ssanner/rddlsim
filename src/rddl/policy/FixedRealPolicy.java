package rddl.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.det.mip.PVarHeatMap;
import rddl.viz.StateViz;

public class FixedRealPolicy implements Policy {

	protected double val = Double.NaN;
	public FixedRealPolicy ( List<String> args ) { 
		if( args.get(0).equalsIgnoreCase("reservoir") ){
			viz = new PVarHeatMap( PVarHeatMap.reservoir_tags );			
		}else if( args.get(0).equalsIgnoreCase("inventory") ){
			viz = new PVarHeatMap( PVarHeatMap.inventory_tags );
		}
		val = Double.parseDouble( args.get(1) );
	}
	
	private StateViz viz = null;
	
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<PVAR_INST_DEF>();

		s._alActionNames.forEach( new Consumer<PVAR_NAME>() {
			public void accept(PVAR_NAME t) {
				try {
					s.generateAtoms(t).forEach( m -> 
						ret.add( new PVAR_INST_DEF( t._sPVarName, val, m ) ) );
				} catch (EvalException e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
		});
		s.computeIntermFluents( ret, new RandomDataGenerator() );
		if( viz != null ){
			viz.display(s , 0 );
		}
		s.clearIntermFluents();
		return ret;
		
	}
	
}
