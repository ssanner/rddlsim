/**
 * RDDL: Traffic Display for Traffic Model 
 * 
 * @author Tan Nguyen (tan1889@gmail.com)
 * @version 6-May-11
 *
 **/

package rddl.viz.trfctm;

import rddl.viz.StateViz;
import java.util.ArrayList;
import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

import java.text.DecimalFormat;

public class TrfDisplay extends StateViz {

	public TrfDisplay() { }

	public TrfGridDisplay gd = null;
	static final int delayTime = 1;
	
	public void display(State s, int time) {
		try {
			System.out.println("TIME = " + time + ": " + getStateDescription(s, time));
		} catch (EvalException e) {
			System.out.println("\n\nError during visualization:\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
		//System.out.println("TIME = " + time + ": " + getStateDescription(s, time));
		//System.out.print(getStateDescription(s, time));
	}

	public String getStateDescription(State s, int time) throws EvalException {
		StringBuilder sb = new StringBuilder();

		PVAR_NAME send = new PVAR_NAME("send");
		PVAR_NAME recv = new PVAR_NAME("recv");
		PVAR_NAME l = new PVAR_NAME("l");
		PVAR_NAME k = new PVAR_NAME("k");
		PVAR_NAME flow = new PVAR_NAME("flow");
		PVAR_NAME n = new PVAR_NAME("n");

		TYPE_NAME cell_type = new TYPE_NAME("cell");
		ArrayList<LCONST> cells = s._hmObject2Consts.get(cell_type);
		
		// initiate TrfGridDisplay gd
		if (gd == null) {
			int max_row = -1;
			int max_col = -1;
			for (LCONST cell : cells) {
				String[] split = cell.toString().split("[_a]");
				int col = new Integer(split[1]);
				int row = new Integer(split[2]);
				if (row > max_row) max_row = row;
				if (col > max_col) max_col = col;
			}
			gd= new TrfGridDisplay(max_col + 2, max_row + 2);	
		}
		
		// get flow grid
		double[][] flowToCell = new double[gd.NCols()][gd.NRows()];
		try {
			ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(flow);
			for (ArrayList<LCONST> gfluent : gfluents) {
				Object assignment = s.getPVariableAssign(flow, gfluent);
				//System.out.println("getting: " + flow + ", " + gfluent + " = " + assignment);
				double dblFlow = 0.0;
				if (assignment != null)
					dblFlow = Double.parseDouble(assignment.toString());
				if (dblFlow != 0.0) {
					String str = gfluent.toString();
					String[] split = str.replaceAll("]", "").split("[_a]");
					int col = new Integer(split[3]);
					int row = new Integer(split[4]);
					flowToCell[col][row] += dblFlow;
				}
			}
		} catch (EvalException ex) {
			sb.append("ERROR while retrieving flows info\n");
		}
		
		ArrayList<LCONST> params = new ArrayList<LCONST>(1);
		params.add(null);
		
		DecimalFormat twoPlaces = new DecimalFormat("0.00");
		sb.append("\n\n");
		
		for (LCONST cell : cells) {
			params.set(0, cell);
			String[] split = cell.toString().split("[_a]");
			int col = new Integer(split[1]);			
			int row = new Integer(split[2]);
			
			// Text output display
			//sb.append(params.toString() + " n=" + twoPlaces.format(s.getPVariableAssign(n, params)) 
			//		+ "; send=" + twoPlaces.format(s.getPVariableAssign(send, params)) 
			//		+ "; recv=" + twoPlaces.format(s.getPVariableAssign(recv, params))
			//		+ "; inflow=" + twoPlaces.format(flowToCell[col][row])
			//		+ ";\n");
			
			// Update gd visualization
			double nCars = (Double) s.getPVariableAssign(n, params);
			double density = (Double) s.getPVariableAssign(k, params);
			double length = (Double) s.getPVariableAssign(l, params);			
			int arrowStyle = 0;
			if (time%2 == (row+col)%2) arrowStyle = 1;
			if (flowToCell[col][row] < 0.05) arrowStyle = -1;
			gd.setCell(col, row, nCars, density * length, cell.toString().toUpperCase().charAt(0), arrowStyle);
		}
		
		// Intersection + Signals
		PVAR_NAME signal = new PVAR_NAME("signal");
		TYPE_NAME intersection_type = new TYPE_NAME("intersection");
		ArrayList<LCONST> intersections = s._hmObject2Consts.get(intersection_type);
		
		for (LCONST intersection : intersections) {
			if (intersection.toString().startsWith("I")) {
				params.set(0, intersection);
				String[] split = intersection.toString().split("[_a]");
				int col = new Integer(split[1]);			
				int row = new Integer(split[2]);

				// Text output display
				sb.append("signal[" + params.toString() + "] = " + s.getPVariableAssign(signal, params)  + ";\n");

				gd.setSignal(col, row, s.getPVariableAssign(signal, params).toString());
			}
		}
				

		// repaint visual graph
		gd.repaint();

		try { Thread.currentThread().sleep(delayTime); } 
		catch (InterruptedException e) { }
		return sb.toString();
	}
	
	public void close() {
		gd.close();
	}

}

