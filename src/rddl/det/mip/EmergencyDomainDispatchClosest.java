package rddl.det.mip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.OBJECT_VAL;

public class EmergencyDomainDispatchClosest extends EmergencyDomainHOPTranslate {

	private static final PVAR_NAME vehicleInServicePvar = new PVAR_NAME("vehicleInService");
	private static final PVAR_NAME callMilesPvar = new PVAR_NAME("callMiles");
	private ArrayList<ArrayList<LCONST>> vehicleSubs;
	private int numVehicles;

	public EmergencyDomainDispatchClosest(List<String> args) throws Exception {
		super(args);
		vehicleSubs = new ArrayList<>();
		numVehicles = 6;
		for( int i = 1 ; i <= numVehicles; ++i ){
			vehicleSubs.add( new ArrayList<LCONST>( 
					Collections.singletonList( new OBJECT_VAL("v"+i) ) ) );
		}
	}
	
	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		s.computeIntermFluents( new ArrayList<PVAR_INST_DEF>(), rand);
		double smallest_miles = Double.MAX_VALUE;
		ArrayList<LCONST> smallest_sub = null;
		
		for( ArrayList<LCONST> vehicleSub : vehicleSubs ){
			double this_miles = ((Number)s.getPVariableAssign(callMilesPvar, vehicleSub)).doubleValue();
			boolean this_service = (boolean)s.getPVariableAssign(vehicleInServicePvar, vehicleSub);
			if( this_service && (this_miles < smallest_miles) ){
				smallest_miles = this_miles;
				smallest_sub = vehicleSub;
			}
			System.out.println(vehicleSub + " " + this_miles + " " +this_service );
		}
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<PVAR_INST_DEF>();
		if( smallest_sub != null ){
			ret.add( new PVAR_INST_DEF("dispatch", true, smallest_sub) );
		}
		return ret;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println( Arrays.toString( args ) );
		EmergencyDomainHOPTranslate planner = new EmergencyDomainDispatchClosest( Arrays.asList( args ) );
		System.out.println( planner.evaluatePlanner(30, null) );
	}
	
}
