package rddl.det.mip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.management.relation.RoleResult;

import gurobi.GRBException;
import rddl.EvalException;
import rddl.State;
import util.Pair;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.OBJECT_VAL;

public class EmergencyDomainDispatchClosest extends EmergencyDomainHOPTranslate {

	private static final PVAR_NAME vehicleInServicePvar = new PVAR_NAME("vehicleInService");
	private static final PVAR_NAME callMilesPvar = new PVAR_NAME("callMiles");
	private static final ArrayList<ArrayList<LCONST>> vehicleSubs;
	private static final int NUMVEHICLES= 8;
	
	static{
		vehicleSubs = new ArrayList<ArrayList<LCONST>>();
		for( int i = 1; i<= NUMVEHICLES; ++i ){
			vehicleSubs.add( new ArrayList<LCONST>( Arrays.<LCONST>asList( new OBJECT_VAL[]{new OBJECT_VAL("v" + i)} ) ) );
		}
	}

	private PVAR_NAME hasCapabilityPvar = new PVAR_NAME("HasCapability");
	private PVAR_NAME causeRequirementPvar = new PVAR_NAME("CauseRequirement");

	private static final ArrayList<OBJECT_VAL> roles = new ArrayList<>( Arrays.<OBJECT_VAL>asList( new OBJECT_VAL[]{
			new OBJECT_VAL("Engine"), new OBJECT_VAL("Ambulance"), 
			new OBJECT_VAL("Ladder"), new OBJECT_VAL("Command") } ) );
	
	public EmergencyDomainDispatchClosest(List<String> args) throws Exception {
		super(args);
	}
	
	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		s.computeIntermFluents( new ArrayList<PVAR_INST_DEF>(), rand);
		
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<PVAR_INST_DEF>();
		Map<String, Boolean> added = new HashMap<>();
		String this_code = EmergencyDomainDataReelElement.getCurrentCauseCode(s);
		OBJECT_VAL this_region = EmergencyDomainDataReelElement.getRegion(
				EmergencyDomainDataReelElement.getCurrentCallX(s), 
				EmergencyDomainDataReelElement.getCurrentCallY(s), this_code);
		
		for( OBJECT_VAL this_ole : roles ){
			
			int role_required = (int)s.getPVariableAssign(causeRequirementPvar , new ArrayList<LCONST>( Arrays.<LCONST>asList(
						new LCONST[]{ new OBJECT_VAL(this_code), this_region, this_ole } )));
			System.out.println(this_ole + " " + role_required );
			if( role_required == 0 ){
				continue;
			}
			
			TreeMap<Double,ArrayList<LCONST>> candidates = new TreeMap<>();
			for( ArrayList<LCONST> vehicleSub : vehicleSubs ){
				if( added.containsKey(vehicleSub.get(0)._sConstValue) ){
					continue;
				}
				
				double this_miles = ((Number)s.getPVariableAssign(callMilesPvar, vehicleSub)).doubleValue();
				boolean this_service = (boolean)s.getPVariableAssign(vehicleInServicePvar, vehicleSub);
				ArrayList<LCONST> this_params = new ArrayList<LCONST>();
				this_params.add( vehicleSub.get(0) );
				this_params.add( this_ole );
				
				boolean this_capable = (boolean)s.getPVariableAssign(hasCapabilityPvar , this_params );
				
				if( this_capable && this_service ){
					if( !candidates.containsKey(this_miles) ){
						this_miles += 0.01;
					}
					candidates.put( this_miles, this_params );
				}
				System.out.println(vehicleSub + " " + this_miles + " " +this_service + " " + this_capable );
			}
			
//			Collections.sort( candidates );
			System.out.println(this_ole + " " + candidates );
			
			for( int i = 0 ; i < role_required; ++i ){
				Entry<Double, ArrayList<LCONST>> selected = candidates.pollFirstEntry();
				if( selected == null ){
					break;
				}
				ret.add( new PVAR_INST_DEF("dispatch", true, selected.getValue() ) );
				added.put( selected.getValue().get(0)._sConstValue, true );
			}
		}
		
		s.clearIntermFluents();
		return ret;
	}
	
	@Override
	protected void cleanUp() throws GRBException {
		return;
	}
	
	@Override
	protected void handleOOM() {
		return;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println( Arrays.toString( args ) );
		EmergencyDomainHOPTranslate planner = new EmergencyDomainDispatchClosest( 
				Arrays.asList( args ).subList(0, args.length-2) );
		System.out.println( planner.evaluatePlanner(
				Integer.parseInt( args[args.length-2] ), 
				null, //new EmergencyDomainStateViz(1300,30,1500,80), 
				Boolean.parseBoolean( args[ args.length-1 ] ) ) );
	}
	
}
