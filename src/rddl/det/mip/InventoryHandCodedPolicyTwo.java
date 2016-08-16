package rddl.det.mip;

/**
 * 
 * @author ashwi
 * if stock < min-demand then order more
 * if stoock > max-demand dont order
 * distribute remaining budget evenly
 */


import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.policy.Policy;
import rddl.RDDL.TYPE_NAME;

public class InventoryHandCodedPolicyTwo implements Policy {
	
	private static final double EPSILON = 0.1;
	private RandomDataGenerator rng = new RandomDataGenerator();
	private PVarHeatMap viz = new PVarHeatMap( PVarHeatMap.inventory_tags );
	private DecimalFormat df = new DecimalFormat("#.#");
	
	private final static PVAR_NAME demand_center_pvar = new PVAR_NAME("DEMAND-CENTER");
	private final static PVAR_NAME demand_width_pvar = new PVAR_NAME("DEMAND-WIDTH");
	private final static PVAR_NAME stock_pvar = new PVAR_NAME("stock");
	private final static PVAR_NAME order_pvar = new PVAR_NAME("order");
	
	public InventoryHandCodedPolicyTwo( List<String> args ) {
		df.setRoundingMode( RoundingMode.DOWN );
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		double remaining_budget = (double) s.getPVariableAssign( new PVAR_NAME("budget") , new ArrayList<LCONST>() ) - EPSILON; 
		
		ArrayList<ArrayList<LCONST>> atoms = s.generateAtoms( stock_pvar );
		HashMap< ArrayList<LCONST> , Double > orders = new HashMap<>();
		
		for( final ArrayList<LCONST> atom : atoms ){
			double stock = (double) s.getPVariableAssign(stock_pvar, atom);
			double min_demand = 0;//(double)s.getPVariableAssign( demand_center_pvar, atom) 
							//- (double)s.getPVariableAssign( demand_width_pvar, atom );
			double max_demand = (double)s.getPVariableAssign( demand_center_pvar, atom) *
					(double)s.getPVariableAssign( demand_width_pvar, atom );
			double expected_demand = max_demand / 2.0d;
			if( stock > max_demand ){
				continue;
			}else if( stock < min_demand ){
				double this_order = Math.min( remaining_budget, min_demand-stock );
				addToOrder( atom, this_order, orders );//side effect on orders
				remaining_budget -= min_demand-stock;
			}else if( remaining_budget <= 0 ){
				break;
			}else if( stock < expected_demand ){
				addToOrder( atom, ( expected_demand - stock ), orders );
			}
			
		}
		
//		if( remaining_budget >= 0 ){
//			final double per_shop_budget = Double.valueOf( df.format( 
//					remaining_budget / ( s._hmObject2Consts.get( new TYPE_NAME("item") ).size() ) ) );
//				
//			for( final ArrayList<LCONST> atom : atoms ){
//				addToOrder( atom, per_shop_budget - EPSILON, orders );
//			}
//			
//		}
		
		if( viz != null ){
			viz.display(s , 0 );
		}
		
		s.clearActions();
		s.clearIntermFluents();

		ArrayList<PVAR_INST_DEF> ret = new ArrayList<>();
		orders.forEach( (ArrayList<LCONST> a, Double b) -> ret.add( new PVAR_INST_DEF(order_pvar._sPVarName, b, a ) ) );
		
				
		return ret;
		
	}

	private void addToOrder(ArrayList<LCONST> atom, double this_order, HashMap<ArrayList<LCONST>, Double> orders) {
		double cur_level = orders.containsKey(atom) ? orders.get(atom) : 0;
		orders.put( atom, cur_level + this_order );
	}

}

