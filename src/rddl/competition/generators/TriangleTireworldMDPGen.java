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

import ppddl.PPDDL;

import util.Permutation;

public class TriangleTireworldMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected String ppddl_file;
	protected float prob_flat;
	protected int horizon;
	protected float discount;

	public static void main(String [] args) throws Exception {
		
		if(args.length != 6) // max index + 1
			usage();
		
		TriangleTireworldMDPGen gen = new TriangleTireworldMDPGen(args);
		String content = gen.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(gen.output_dir + File.separator + gen.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
		//System.out.println("\n" + content + "\n");
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name ppddl_file prob_flat horizon discount");
		System.err.println("Example: files/testcomp/rddl triangle_tireworld_mdp__1 3 2 0.2 0.1 40 1.0");
		System.exit(127);
	}
	
	public  TriangleTireworldMDPGen(String [] args){
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		ppddl_file = args[2];
		prob_flat = Float.parseFloat(args[3]);
		horizon = Integer.parseInt(args[4]);
		discount = Float.parseFloat(args[5]);
	}

	public String generate(){

		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
		// Load source PPDDL file
		PPDDL src = new PPDDL(ppddl_file);
		//System.out.println("Domains" + src._alDomains);
		//System.out.println("Problems" + src._alProblems.get(0));
		PPDDL.Problem p = (PPDDL.Problem)src._alProblems.get(0);
		
//		Problem: triangle-tire-1
//		  - Domain:  triangle-tire
//		  - Objects: [l-1-1, l-1-2, l-1-3, l-2-1, l-2-2, l-2-3, l-3-1, l-3-2, l-3-3]
//		  - Types:   [location, location, location, location, location, location, location, location, location]
//		  - Init:    [[vehicle-at, l-1-1], [road, l-1-1, l-1-2], [road, l-1-2, l-1-3], [road, l-1-1, l-2-1], [road, l-1-2, l-2-2], [road, l-2-1, l-1-2], [road, l-2-2, l-1-3], [spare-in, l-2-1], [spare-in, l-2-2], [road, l-2-1, l-3-1], [road, l-3-1, l-2-2], [spare-in, l-3-1], [spare-in, l-3-1], [not-flattire]]
//		  - Goal:    [vehicle-at, l-1-3]
//		  - Metric:  [:metric, maximize, [reward]]
//		  - Reward:  100.0
		  
//		non-fluents nf_triangle_tireworld_inst_mdp__1 {
//			  domain = triangle_tireworld_mdp;
//			  
//			  objects {
//			    location : {la1a1, la1a2, la1a3, la2a1, la2a2, la3a1};
//			  };
//
//			  // Note that nonfluents are separated from fluents  
//			  non-fluents {
//			    road(la1a1,la1a2); // 1a1 1a2 1a3
//			    road(la1a2,la1a3); // 2a1 2a2
//			    road(la1a1,la2a1); // 3a1          row = 3, col = 1, col <= max_row + 1 - row
//			    road(la1a2,la2a2);
//			    road(la2a1,la1a2);
//			    road(la2a2,la1a3);
//			    road(la2a1,la3a1);
//			    road(la3a1,la2a2);
//
//			    goal-location(la1a3);
//			  };
//			}

		int max_row = -1;
		for (Object o : p._alObjects) {
			String s = (String)o;
			String[] split = s.split("-");
			int row = new Integer(split[1]);
			//int col = new Integer(split[2]);
			max_row = Math.max(row, max_row);
		}
		
		ArrayList<String> locations = new ArrayList<String>();
		for (Object o : p._alObjects) {
			String s = (String)o;
			String[] split = s.split("-");
			int row = new Integer(split[1]);
			int col = new Integer(split[2]);
			if (col > max_row + 1 - row)
				continue; // never used
			String location = "la" + row + "a" + col;
			locations.add(location);
		}
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = triangle_tireworld_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\tlocation : {");
		boolean first = true; 
		for (String location  : locations) {
			sb.append((first ? "" : ", ") + location);
			first = false;
		}
		sb.append("};\n\t};\n\n");
				
		sb.append("\tnon-fluents {\n");

		sb.append("\t\tFLAT-PROB = " + prob_flat + ";\n");
		
		for (Object o : p._alInit) {
			ArrayList pred = (ArrayList)o;
			String pred_name = (String)pred.get(0);
			if (pred_name.equals("road")) {
				String location1 = ((String)pred.get(1)).replace("-", "a");
				String location2 = ((String)pred.get(2)).replace("-", "a");
				sb.append("\t\troad(" + location1 + "," + location2 + ");\n");
			}
		}
		
		ArrayList goal = (ArrayList)p._alGoal;
		String location = ((String)goal.get(1)).replace("-", "a");
		sb.append("\n\t\tgoal-location(" + location + ");\n");
								
		sb.append("\t};\n");
		sb.append("}\n\n");
		
//		instance triangle_tireworld_inst_mdp__1 {
//			  domain = triangle_tireworld_mdp;
//			  non-fluents = nf_triangle_tireworld_inst_mdp__1;
//
//			  // Note that nonfluents are specified above, the initial state is only for fluents  
//			  init-state {
//			    vehicle-at(la1a1);
//			    spare-in(la2a1);
//			    spare-in(la2a2);
//			    spare-in(la3a1);
//			    spare-in(la3a1);
//			    not-flattire;
//			  };
//			  
//			  max-nondef-actions = 1;
//			  horizon = 40;
//			  discount = 1.0;
//			}


		sb.append("instance " + instance_name + " {\n");
		sb.append("\tdomain = triangle_tireworld_mdp;\n");
		sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
		sb.append("\tinit-state {\n");
		for (Object o : p._alInit) {
			ArrayList pred = (ArrayList)o;
			String pred_name = (String)pred.get(0);
			if (pred_name.equals("vehicle-at")) {
				String location1 = ((String)pred.get(1)).replace("-", "a");
				sb.append("\t\tvehicle-at(" + location1 + ");\n");
			} else if (pred_name.equals("spare-in")) {
				String location1 = ((String)pred.get(1)).replace("-", "a");
				sb.append("\t\tspare-in(" + location1 + ");\n");
			} else if (pred_name.equals("not-flattire")) {
				sb.append("\t\tnot-flattire;\n");
			}
		}
		sb.append("\t};\n\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon  = " + horizon + ";\n");
		sb.append("\tdiscount = " + discount + ";\n");
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
