package rddl.det.mip;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.viz.BlockDisplay;
import rddl.viz.StateViz;
import util.Pair;

public class EmergencyDomainStateViz extends BlockDisplay implements StateViz {

	private static final PVAR_NAME vehicleHomePvar = new PVAR_NAME("vehicleHome");
	private PVAR_NAME lastVehicleServicePvar = new PVAR_NAME("lastVehicleService");
	
	private double map_lower_x;
	private double map_lower_y;
	private double map_upper_x;
	private double map_upper_y;

	public EmergencyDomainStateViz(final double map_lower_x, final double map_lower_y, 
			final double map_upper_x, final double map_upper_y ) {
		super("EmergencyDomain", "Status", (int)(map_upper_y-map_lower_y), (int)(map_upper_x-map_lower_x) );
		this.map_lower_x = map_lower_x;
		this.map_lower_y = map_lower_y;
		this.map_upper_x = map_upper_x;
		this.map_upper_y = map_upper_y;
	}
	
	private double relativeX(final double absX){
		return (absX-map_lower_x);
	}
	
	private double relativeY(final double absY){
		return (absY-map_lower_y);
	}

	private double toRow(final double relY){
		return (map_upper_y-map_lower_y)-relY;
	}
	
	@Override
	public void display(State s, int time) {
		try {
			this.clearAllCells();
			this.showVehicles(s);
			this.showHomes(s);
			this.showTime(s);
			
			this.showCall(s);
			this.showStats(s);
			
			this.repaint();
//			Thread.sleep(1000);
		} catch (EvalException e) {
			e.printStackTrace();
		} 
//		catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
	}

	private void showStats(State s) throws EvalException {
		this.setMessage( this._msg + "," + ( EmergencyDomainHOPTranslate.getFirstResponse(s) + "," + 
				EmergencyDomainHOPTranslate.getFullResponse(s) + "," +
				EmergencyDomainHOPTranslate.getOverwhelm(s) ) );
	}

	private void showCall(State s) throws EvalException {
		final double callX = EmergencyDomainDataReelElement.getCurrentCallX(s);
		final double callY = EmergencyDomainDataReelElement.getCurrentCallY(s);
		double relX = relativeX(callX);
		double relY = relativeY(callY);
		
		this.setCell( (int)toRow( relY ), (int)relX, Color.RED, "X" );		
	}

	private void showVehicles(State s) throws EvalException {
		showPvarXY(s, lastVehicleServicePvar, Color.BLUE );
//		this.setMessage( this._status + " " + getPvarXY(s, ) );
	}

	private void showTime(State s) throws EvalException {
		this.setMessage( EmergencyDomainDataReelElement.getCurrentCallTime(s).toString() );
	}

	private void showHomes(State s) throws EvalException {
		showPvarXY(s, vehicleHomePvar, Color.BLACK );
	}

	private void showPvarXY(State s, PVAR_NAME pvar, Color col) throws EvalException {
		Map<String, Pair<Integer, Integer>> homes = getPvarXY(s, pvar);
		
		for( Entry<String, Pair<Integer, Integer>> entry : homes.entrySet() ){
			final int target_row = (int) toRow( relativeY(entry.getValue()._o2));
			final int target_col =(int)relativeX(entry.getValue()._o1);
			this.setCell( target_row, target_col, col, entry.getKey().charAt(1)+"" );
		}		
	}

	protected Map<String, Pair<Integer, Integer>> getPvarXY(State s, PVAR_NAME pvar) throws EvalException {
		ArrayList<ArrayList<LCONST>> subs = s.generateAtoms(pvar);
		Map<String, Pair<Integer,Integer>> homes = new HashMap<>();
		
		for( ArrayList<LCONST> assign :  subs ){
			double val = (double)s.getPVariableAssign(pvar, assign);
			if( homes.containsKey( assign.get(0)._sConstValue ) ) {
				Pair<Integer, Integer> cur = homes.get( assign.get(0)._sConstValue );
				assert( cur._o1 == null || cur._o2 == null );
				if( assign.get(1)._sConstValue.equals("xpos") && cur._o1 == null ){
					cur._o1 = (int)val;
				}else if( assign.get(1)._sConstValue.equals("ypos") && cur._o2 == null ){
					cur._o2 = (int)val;
				}
				homes.put( assign.get(0)._sConstValue, cur );
			}else{
				if(  assign.get(1)._sConstValue.equals("xpos") ){
					homes.put( assign.get(0)._sConstValue,  new Pair<Integer,Integer>((int)val,null) );
				}else if(  assign.get(1)._sConstValue.equals("ypos") ){
					homes.put( assign.get(0)._sConstValue,  new Pair<Integer,Integer>(null, (int)val) );
				}
			}
		}
		return homes;
	}
	


}
