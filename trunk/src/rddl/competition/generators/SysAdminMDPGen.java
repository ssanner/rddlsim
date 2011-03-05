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
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class SysAdminMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int num_comp;
	protected int num_neighbors;
	protected float reboot_prob;
	protected int horizon;
	protected float discount;

	public static void main(String [] args) throws Exception {
		
		if(args.length != 7)
			usage();
		
		SysAdminMDPGen gen = new SysAdminMDPGen(args);
		String content = gen.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(gen.output_dir + File.separator + gen.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name num-comp num-neighbors reboot-prob horizon discount");
		System.err.println("Example: files/testcomp/rddl sysadmin_10_3 10 3 0.05 100 0.9");
		System.exit(127);
	}
	
	public  SysAdminMDPGen(String [] args){
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		num_comp = Integer.parseInt(args[2]);
		num_neighbors = Integer.parseInt(args[3]);
		reboot_prob = Float.parseFloat(args[4]);
		horizon = Integer.parseInt(args[5]);
		discount = Float.parseFloat(args[6]);
	}

	public String generate(){

		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
//		non-fluents ring8 {
//
//			domain = sysadmin_mdp;
//			
//			objects { 
//				computer : {c1, c2, c3, c4, c5, c6, c7, c8 };
//			};
//		  
//			// Only need to specify non-default values
//			non-fluents { 
//				REBOOT-PROB = 0.05; 
//				CONNECTED(c1,c2);
//				CONNECTED(c2,c3);
//				CONNECTED(c3,c4);
//				CONNECTED(c4,c5);
//				CONNECTED(c5,c6);
//				CONNECTED(c6,c7);
//				CONNECTED(c7,c8);
//				CONNECTED(c8,c1);
//			};
//		}

		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = sysadmin_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\tcomputer : {");
		for (int i = 1; i <= num_comp; i++)
			sb.append(((i > 1) ? "," : "") + "c" + i);
		sb.append("};\n");
		
		sb.append("\t};\n");
		
		sb.append("\tnon-fluents {\n");
		
		sb.append("\t\tREBOOT-PROB = " + reboot_prob + ";\n");
		
		for (int i = 1; i <= num_comp; i++) {
			HashSet<Integer> neighbors = new HashSet<Integer>();
			
			// Duplicates could be generated, so will contain <= num_neighbors
			for (int j = 1; j <= num_neighbors; j++) {
				int neighbor = 1 + ran.nextInt(num_comp);
				if (neighbor != i)
					neighbors.add(neighbor);
			}
			
			for (Integer n : neighbors) {
				sb.append("\t\tCONNECTED(c" + i + ",c" + n + ");\n");
			}
		}
		
		sb.append("\t};\n");
		sb.append("}\n\n");
		
//		instance sysm1 {
//
//					domain = sysadmin_mdp;
//					
//					non-fluents = ring4;
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
		sb.append("\tdomain = sysadmin_mdp;\n");
		sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
		sb.append("\tinit-state {\n");
		
		// Always start with all computers running
		for (int i = 1; i <= num_comp; i++)
			sb.append("\t\trunning(c" + i + ");\n");
		sb.append("\t};\n\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon  = " + horizon + ";\n");
		sb.append("\tdiscount = " + discount + ";\n");
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
