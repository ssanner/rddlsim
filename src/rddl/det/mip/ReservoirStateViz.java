package rddl.det.mip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.ToDoubleFunction;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.viz.StateViz;

public class ReservoirStateViz extends PVarHeatMap {

	public ReservoirStateViz(String[] tags) {
		super(tags);
		// TODO Auto-generated constructor stub
	}

	private final static String OUTPUT_FILE_LEVEL = "reservoir_levels_last.viz";
	private final static String OUTPUT_FILE_RAIN = "reservoir_rain_last.viz";

	private BufferedWriter outfile_level = null;;
	private BufferedWriter outfile_rain = null;;

	@Override
	protected void finalize() throws Throwable {
		outfile_level.close(); outfile_rain.close();
	}

	@Override
	public void display(State s, int time) {
		if( outfile_level == null ){
			try {
				outfile_level = new BufferedWriter( new FileWriter( new File( OUTPUT_FILE_LEVEL ) ) );
				outfile_rain = new BufferedWriter( new FileWriter( new File( OUTPUT_FILE_RAIN ) ) );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
			
		try {
			PVAR_NAME pvar = new PVAR_NAME("rlevel");						
			double[] levels = s.generateAtoms( pvar ).stream()
					.mapToDouble( new ToDoubleFunction< ArrayList<LCONST> > () {
						public double applyAsDouble( ArrayList<LCONST> value) {
							try {
								return (Double)s.getPVariableAssign(pvar, value);
							} catch (EvalException e) {
								e.printStackTrace();
							}
							return Double.NaN;
						}
					} ).toArray();
			outfile_level.write( Arrays.toString( levels ) );
			outfile_level.write( "\n" );
			outfile_level.flush();
			
			PVAR_NAME rainvar = new PVAR_NAME("rain");
			double[] rain = s.generateAtoms( rainvar ).stream()
					.mapToDouble( new ToDoubleFunction< ArrayList<LCONST> > () {
						public double applyAsDouble( ArrayList<LCONST> value) {
							try {
								return (Double)s.getPVariableAssign( rainvar, value);
							} catch (EvalException e) {
								e.printStackTrace();
							}
							return Double.NaN;
						}
					} ).toArray();
			outfile_rain.write( Arrays.toString( rain ) );
			outfile_rain.write( "\n" );
			outfile_rain.flush();
				
		} catch (EvalException | IOException e) {
			e.printStackTrace();
		}

	}

}
