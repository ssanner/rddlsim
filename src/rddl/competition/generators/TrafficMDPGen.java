package rddl.competition.generators;

/**
 *  A generator for instances of a fully observable game of life.
 *  
 *  @author Scott Sanner
 *  @version 3/1/11
 * 
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class TrafficMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int num_cells;
	protected float input_min;
	protected float input_max;
	protected int horizon;
	protected float discount;

	public static final float INIT_OCCUPANCY_PROB = 0.3f;
	
	public static void main(String [] args) throws Exception {
		
		if(args.length != 7)
			usage();
		
		TrafficMDPGen gen = new TrafficMDPGen(args);
		String content = gen.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(gen.output_dir + File.separator + gen.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name num-cells input-min input-max horizon discount");
		System.err.println("Example: files/testcomp/rddl traffic_3 3 0.3 0.5 100 0.9");
		System.exit(127);
	}
	
	public  TrafficMDPGen(String [] args){
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		num_cells = Integer.parseInt(args[2]);
		input_min =  Float.parseFloat(args[3]);
		input_max = Float.parseFloat(args[4]);
		horizon = Integer.parseInt(args[5]);
		discount = Float.parseFloat(args[6]);
		
		if (num_cells < 2) {
			System.out.println("num-cells (" + num_cells + ") must be >= 2");
			System.exit(1);
		}
		if (input_min < 0d || input_max > 1d || input_min > input_max) {
			System.out.println("num-cells range [" + input_min + "," + input_max + "] must be in [0,1]");
			System.exit(1);
		}
	}

	public String generate(){

		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
		// 3 cells would be
		
		//   1 2 3 4 5 6 7 8 9 0 1
		// 1       v       v
		// 2       v       v
		// 3       v       v
		// 4 > > > I > > > I > > >
		// 5       v       v
		// 6       v       v
		// 7       v       v
		// 8 > > > I > > > I > > >
		// 9       v       v
		// 10      v       v
		// 11      v       v
		
		// First row/col: 1
		// Last row/col: 3*(nc+1)-1
		//
		// Generate row and col: nc + 1, 2*(nc+1)
		// Don't generate {nc + 1, 2*(nc+1)} X {nc + 1, 2*(nc+1)}
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = traffic_mdp;\n");
		sb.append("\tobjects {\n");

		int min_cell = 1;
		int int_1    = num_cells + 1;
		int int_2    = 2*(num_cells + 1);
		ArrayList<Integer> intersections = new ArrayList<Integer>(Arrays.asList(int_1, int_2));
		int max_cell = 3*(num_cells + 1) - 1;
		
		sb.append("\t\tintersection : {");
		sb.append("ia" + int_1 + "a" + int_1);				
		sb.append(",ia" + int_1 + "a" + int_2);				
		sb.append(",ia" + int_2 + "a" + int_1);				
		sb.append(",ia" + int_2 + "a" + int_2);				
		sb.append("};\n");

		ArrayList<String> cells = new ArrayList<String>();
		ArrayList<String> init_cells = new ArrayList<String>();
		sb.append("\t\tcell : {");
		boolean first = true;
		for (int i : intersections) {
			for (int o = min_cell; o <= max_cell; o++) {
				if (intersections.contains(o))
					continue;
		
				String cell_name1 = "ca" + i + "a" + o;
				cells.add(cell_name1);
				sb.append((first ? "" : ",") + cell_name1);
				first = false;

				String cell_name2 = "ca" + o + "a" + i;
				cells.add(cell_name2);
				sb.append("," + cell_name2);

				if (ran.nextFloat() < INIT_OCCUPANCY_PROB)
					init_cells.add(cell_name1);
				if (ran.nextFloat() < INIT_OCCUPANCY_PROB)
					init_cells.add(cell_name2);
			}
		}
		sb.append("};\n");
		
		sb.append("\t};\n");
		
		sb.append("\tnon-fluents {\n");
		
		// Define input cells and input rates
		// PERIMETER-INPUT-CELL(ca2a1);
		// PERIMETER-INPUT-RATE(ca2a1) = 0.8;
		sb.append("\n\t\tPERIMETER-INPUT-CELL(ca" + int_1 + "a" + min_cell + ");\n");
		sb.append("\t\tPERIMETER-INPUT-CELL(ca" + int_2 + "a" + min_cell + ");\n");
		sb.append("\t\tPERIMETER-INPUT-CELL(ca" + min_cell + "a" + int_1 + ");\n");
		sb.append("\t\tPERIMETER-INPUT-CELL(ca" + min_cell + "a" + int_2 + ");\n");

		sb.append("\n\t\tPERIMETER-INPUT-RATE(ca" + int_1 + "a" + min_cell + ") = " + (ran.nextFloat() * (input_max - input_min) + input_min) + ";\n");
		sb.append("\t\tPERIMETER-INPUT-RATE(ca" + int_2 + "a" + min_cell + ") = " + (ran.nextFloat() * (input_max - input_min) + input_min) + ";\n");
		sb.append("\t\tPERIMETER-INPUT-RATE(ca" + min_cell + "a" + int_1 + ") = " + (ran.nextFloat() * (input_max - input_min) + input_min) + ";\n");
		sb.append("\t\tPERIMETER-INPUT-RATE(ca" + min_cell + "a" + int_2 + ") = " + (ran.nextFloat() * (input_max - input_min) + input_min) + ";\n");
		
		// Define exit cells
		// PERIMETER-EXIT-CELL(ca2a11);
		sb.append("\n\t\tPERIMETER-EXIT-CELL(ca" + int_1 + "a" + max_cell + ");\n");
		sb.append("\t\tPERIMETER-EXIT-CELL(ca" + int_2 + "a" + max_cell + ");\n");
		sb.append("\t\tPERIMETER-EXIT-CELL(ca" + max_cell + "a" + int_1 + ");\n");
		sb.append("\t\tPERIMETER-EXIT-CELL(ca" + max_cell + "a" + int_2 + ");\n");
		
		// Define intersection inputs
		// FLOWS-INTO-INTERSECTION-EW(ca2a3, ia2a4);
		// FLOWS-INTO-INTERSECTION-NS(ca3a3, ia2a4);
		sb.append("\n\t\tFLOWS-INTO-INTERSECTION-EW(ca" + int_1 + "a" + (int_1-1) + ",ia" + int_1 + "a" + int_1 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-EW(ca" + int_1 + "a" + (int_2-1) + ",ia" + int_1 + "a" + int_2 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-EW(ca" + int_2 + "a" + (int_1-1) + ",ia" + int_2 + "a" + int_1 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-EW(ca" + int_2 + "a" + (int_2-1) + ",ia" + int_2 + "a" + int_2 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-NS(ca" + (int_1-1) + "a" + int_1 + ",ia" + int_1 + "a" + int_1 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-NS(ca" + (int_2-1) + "a" + int_1 + ",ia" + int_2 + "a" + int_1 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-NS(ca" + (int_1-1) + "a" + int_2 + ",ia" + int_1 + "a" + int_2 + ");\n");
		sb.append("\t\tFLOWS-INTO-INTERSECTION-NS(ca" + (int_2-1) + "a" + int_2 + ",ia" + int_2 + "a" + int_2 + ");\n");

		// Define cell flow
		// FLOWS-INTO-CELL(ca5a4, ca4a4);
		sb.append("\n");
		for (int i : intersections) {
			for (int o = min_cell; o <= max_cell-1; o++) {
				if (intersections.contains(o))
					continue;
				int next_o = o + 1;
				if (intersections.contains(next_o))
					next_o = next_o + 1;
				
				sb.append("\t\tFLOWS-INTO-CELL(ca" + i + "a" + o + ",ca" + i + "a" + next_o + ");\n");
				sb.append("\t\tFLOWS-INTO-CELL(ca" + o + "a" + i + ",ca" + next_o + "a" + i + ");\n");
			}
		}
		
		sb.append("\t};\n");
		sb.append("}\n\n");
		
//		instance sysm1 {
//
//					domain = traffic_mdp;
//					
//					non-fluents = nf_traffic_inst_mdp;
//
//					init-state { 
//						running(c1); 
//						~running(c2); 
//					};
//				  
//					max-nondef-actions = 1;
//				 	horizon  = 20;
//					discount = 1.0;
//				}

		sb.append("instance " + instance_name + " {\n");
		sb.append("\tdomain = traffic_mdp;\n");
		sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
		if (init_cells.size() > 0) {
			sb.append("\tinit-state {\n");
			for (String cell : init_cells)
				sb.append("\t\toccupied(" + cell + ");\n");
			sb.append("\t};\n\n");
		}
		sb.append("\tmax-nondef-actions = 4;\n");
		sb.append("\thorizon  = " + horizon + ";\n");
		sb.append("\tdiscount = " + discount + ";\n");
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
