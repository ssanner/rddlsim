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

public class InventoryHandCodedPolicy implements Policy {
	
	private static final double EPSILON = 0.1;
	private RandomDataGenerator rng = new RandomDataGenerator();
	private PVarHeatMap viz = new PVarHeatMap( PVarHeatMap.inventory_tags );
	private DecimalFormat df = new DecimalFormat("#.#");
	
	public InventoryHandCodedPolicy( List<String> args ) {
		df.setRoundingMode( RoundingMode.DOWN );
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		//compute total budget
		//does not depend on action
		//assumes interm are determinstic
		s.computeIntermFluents( new ArrayList<PVAR_INST_DEF>(), new RandomDataGenerator() );
		
		final double budget = (double) s.getPVariableAssign( new PVAR_NAME("budget") , new ArrayList<LCONST>() ) - EPSILON; 
		
		final double per_shop_budget = Double.valueOf( df.format( 
				budget / ( s._hmObject2Consts.get( new TYPE_NAME("item") ).size() ) ) );
		
		//distribute budget evenly
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<PVAR_INST_DEF>();

		s._alActionNames.forEach( new Consumer<PVAR_NAME>() {
			public void accept(PVAR_NAME t) {
				try {
					s.generateAtoms(t).forEach( m -> 
						ret.add( new PVAR_INST_DEF( t._sPVarName, per_shop_budget, m ) ) );
				} catch (EvalException e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
		});
		
		s.setActions(ret);
		
		if( viz != null ){
			viz.display(s , 0 );
		}
		
		s.clearActions();
		s.clearIntermFluents();
		
		return ret;
		
	}

}
