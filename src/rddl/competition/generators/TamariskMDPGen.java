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

public class TamariskMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int num_slots;
	protected int num_reaches;
	protected float prob_native;
	protected float prob_tamarisk;
	protected int horizon;
	protected float discount;

	public static void main(String [] args) throws Exception {
		
		if(args.length != 8) // max index + 1
			usage();
		
		TamariskMDPGen gen = new TamariskMDPGen(args);
		String content = gen.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(gen.output_dir + File.separator + gen.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name num_reaches num_slots prob_native prob_tamarisk horizon discount");
		System.err.println("Example: files/testcomp/rddl tamarisk_mdp__1 3 2 0.2 0.1 40 1.0");
		System.exit(127);
	}
	
	public  TamariskMDPGen(String [] args){
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		num_reaches = Integer.parseInt(args[2]);
		num_slots = Integer.parseInt(args[3]);
		prob_native = Float.parseFloat(args[4]);
		prob_tamarisk = Float.parseFloat(args[5]);
		horizon = Integer.parseInt(args[6]);
		discount = Float.parseFloat(args[7]);
	}

	public String generate(){

		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
//		non-fluents nf_tamarisk_inst_mdp__1 {
//			  domain = tamarisk_mdp;
//			  
//			  objects {
//			    slot  : {s11, s12, s21, s22, s31, s32};
//			    reach : {r1, r2, r3};
//			  };
//
//			  // Note that nonfluents are separated from fluents  
//			  non-fluents {
//			    SLOT-AT-REACH(s11,r1);
//			    SLOT-AT-REACH(s12,r1);
//				DOWNSTREAM-REACH(r2,r1); // r1 is stream head
//				DOWNSTREAM-REACH(r3,r2);
//			  };
//			}

		ArrayList<String> reaches = new ArrayList<String>();
		ArrayList<String> slots   = new ArrayList<String>();
		for (int reach = 1; reach <= num_reaches; reach++) {
			String new_reach = "r" + reach;
			reaches.add(new_reach);
			for (int slot = 1; slot <= num_slots; slot++) {
				String new_slot = "s" + reach + "s" + slot;
				slots.add(new_slot);
			}
		}
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = tamarisk_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\tslot : {");
		boolean first = true; 
		for (String slot  : slots) {
			sb.append((first ? "" : ", ") + slot);
			first = false;
		}
		sb.append("};\n");

		sb.append("\t\treach : {");
		first = true; 
		for (String reach  : reaches) {
			sb.append((first ? "" : ", ") + reach);
			first = false;
		}
		sb.append("};\n\t};\n\n");
				
		sb.append("\tnon-fluents {\n");

		String prev_reach = null;
		for (int reach = 1; reach <= num_reaches; reach++) {
			String new_reach = "r" + reach;
			reaches.add(new_reach);
			
			if (prev_reach != null)
				sb.append("\t\tDOWNSTREAM-REACH(" + new_reach + "," + prev_reach + ");\n");
			
			for (int slot = 1; slot <= num_slots; slot++) {
				String new_slot = "s" + reach + "s" + slot;
				sb.append("\t\tSLOT-AT-REACH(" + new_slot + "," + new_reach + ");\n");
			}
			
			prev_reach = new_reach;
		}
								
		sb.append("\t};\n");
		sb.append("}\n\n");
		
//		instance tamarisk_inst_mdp__1 {
//			  domain = tamarisk_mdp;
//			  non-fluents = nf_tamarisk_inst_mdp__1;
//
//			  init-state {
//			    tamarisk-at(s11);
//			    native-at(s12);
//				// empty: (s21);
//				// empty: (s22);
//			    native-at(s31);
//			    tamarisk-at(s32);
//			  };
//			  
//			  max-nondef-actions = 1;
//			  horizon = 40;
//			  discount = 1.0;
//			}

		sb.append("instance " + instance_name + " {\n");
		sb.append("\tdomain = tamarisk_mdp;\n");
		sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
		sb.append("\tinit-state {\n");
		boolean any_native   = false;
		boolean tamarisk_at_s1s1 = false;
		sb.append("\t\ttamarisk-at(s1s1);\n");
		for (String slot : slots) {
			if (ran.nextFloat() < prob_native) {
				sb.append("\t\tnative-at(" + slot + ");\n");
				any_native = true;
			}
			if (slot.equals("s1s1"))
				continue; // already added tamarisk
			if (ran.nextFloat() < prob_tamarisk) {
				sb.append("\t\ttamarisk-at(" + slot + ");\n");
			}
		}
		if (!any_native) // Must be at least one native
			sb.append("\t\tnative-at(s1s1);\n");
		// Tamarisk always at s1s1
		sb.append("\t};\n\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon  = " + horizon + ";\n");
		sb.append("\tdiscount = " + discount + ";\n");
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
