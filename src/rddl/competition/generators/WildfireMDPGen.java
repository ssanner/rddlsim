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

import util.Permutation;

public class WildfireMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int size_x;
	protected int size_y;
	protected int num_targets;
	protected int horizon;
	protected float prob_drop_neighbor;
	protected float burn_prob;
	protected float discount;

	public static void main(String [] args) throws Exception {
		
		if(args.length != 9) // max index + 1
			usage();
		
		WildfireMDPGen gen = new WildfireMDPGen(args);
		String content = gen.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(gen.output_dir + File.separator + gen.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name size_x size_y num_targets prob_drop_neighbor burn_prob horizon discount");
		System.err.println("Example: files/testcomp/rddl wildfire_inst_mdp__1 3 3 4 0.1 0.1 40 1.0");
		System.exit(127);
	}
	
	public  WildfireMDPGen(String [] args){
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		size_x = Integer.parseInt(args[2]);
		size_y = Integer.parseInt(args[3]);
		num_targets = Integer.parseInt(args[4]);
		prob_drop_neighbor = Float.parseFloat(args[5]);
		burn_prob = Float.parseFloat(args[6]);
		horizon = Integer.parseInt(args[7]);
		discount = Float.parseFloat(args[8]);
	}

	public String generate(){

		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
//		non-fluents nf_wildfire_inst_mdp__1 {
//			domain = wildfire_mdp;
//			objects {
//				x_pos : {x1,x2,x3};
//				y_pos : {y1,y2,y3};
//			};
//			non-fluents {
//				NEIGHBOR(x1,y1,x1,y2);
//				NEIGHBOR(x1,y1,x2,y1);
//				TARGET(x1, y1);
//				TARGET(x3, y3);
//			};
//		}

		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = wildfire_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\tx_pos : {");
		for (int i = 1; i <= size_x; i++)
			sb.append(((i > 1) ? "," : "") + "x" + i);
		sb.append("};\n");
		
		sb.append("\t\ty_pos : {");
		for (int j = 1; j <= size_y; j++)
			sb.append(((j > 1) ? "," : "") + "y" + j);
		sb.append("};\n");
		
		sb.append("\t};\n");
		
		sb.append("\tnon-fluents {\n");
		
		for (int i = 1; i <= size_x; i++)
			for (int j = 1; j <= size_y; j++) {
				for (int io = -1; io <= 1; io++)
					for (int jo = -1; jo <= 1; jo++) {
						if (io == 0 && jo == 0)
							continue;
						int xn = i + io;
						int yn = j + jo;
						if (xn < 1 || xn > size_x || yn < 1 || yn > size_y)
							continue;
						if (ran.nextFloat() < prob_drop_neighbor)
							sb.append("\t\t// Omitted: NEIGHBOR(x" + i + ",y" + j + ",x" + xn + ",y" + yn + ");\n");
						else
							sb.append("\t\tNEIGHBOR(x" + i + ",y" + j + ",x" + xn + ",y" + yn + ");\n");
					}
			}

		int[] indices = Permutation.permute(size_x * size_y, ran); // permutation of {0..size_x * size_y}
		HashSet<Integer> target_indices = new HashSet<Integer>();
		for (int t = 0; t < num_targets; target_indices.add(indices[t++])); // Add first num_targets indices
		
		int index_counter = 0;
		for (int i = 1; i <= size_x; i++) {
			for (int j = 1; j <= size_y; j++) {
				if (target_indices.contains(index_counter++))
					sb.append("\t\tTARGET(x" + i + ",y" + j + ");\n");
			}
		}	
		
		sb.append("\t};\n");
		sb.append("}\n\n");
		
//		instance wildfire_inst_mdp__1 {
//			domain = wildfire_mdp;
//			non-fluents = nf_wildfire_inst_mdp__1;
//			init-state {
//				burning(x1, y3);
//				burning(x1, y2);
//				burning(x2, y3);
//			};
//			max-nondef-actions = 1;
//			horizon = 40;
//			discount = 1.0;
//		}

		sb.append("instance " + instance_name + " {\n");
		sb.append("\tdomain = wildfire_mdp;\n");
		sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
		sb.append("\tinit-state {\n");
		boolean any_burning = false;
		for (int i = 1; i <= size_x; i++)
			for (int j = 1; j <= size_y; j++)
				if (ran.nextFloat() < burn_prob) {
					sb.append("\t\tburning(x" + i + ",y" + j + ");\n");
					any_burning = true;
				}
		if (!any_burning) // Must be at least one burning
			sb.append("\t\tburning(x1,y1);\n");
		sb.append("\t};\n\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon  = " + horizon + ";\n");
		sb.append("\tdiscount = " + discount + ";\n");
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
