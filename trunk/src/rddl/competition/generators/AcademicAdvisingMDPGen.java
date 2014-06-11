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

public class AcademicAdvisingMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int num_levels;
	protected int num_courses_per_level;
	protected int num_prereqs;
	protected float prob_more_less_prereqs;
	protected float prob_prog_req;
	public    int num_conc_actions;
	protected int horizon;
	protected float discount;

	public static void main(String [] args) throws Exception {
		
		if(args.length != 10) // max index + 1
			usage();
		
		AcademicAdvisingMDPGen gen = new AcademicAdvisingMDPGen(args);
		String content = gen.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(gen.output_dir + File.separator + gen.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name num_levels num_courses_per_level num_prereqs prob_more_less_prereqs prob_prog_req num_conc_actions horizon discount");
		System.err.println("Example: files/testcomp/rddl academic_advising_mdp__1 5 2 2 0.5 0.5 2 40 1.0");
		System.exit(127);
	}
	
	public  AcademicAdvisingMDPGen(String [] args){
		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		num_levels = Integer.parseInt(args[2]);
		num_courses_per_level = Integer.parseInt(args[3]);
		num_prereqs = Integer.parseInt(args[4]);
		prob_more_less_prereqs = Float.parseFloat(args[5]);
		prob_prog_req = Float.parseFloat(args[6]);
		num_conc_actions = Integer.parseInt(args[7]);
		horizon = Integer.parseInt(args[8]);
		discount = Float.parseFloat(args[9]);
	}

	public String generate(){

		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
//		non-fluents nf_academic_advising_inst_mdp__1 {
//			  domain = academic_advising_mdp;
//			  objects {
//			    course : {CS11, CS21, CS22, CS31, CS32, CS33, CS41, CS42, CS51};
//			  };
//
//			  non-fluents {
//			    PREREQ(CS11, CS21);
//			    PREREQ(CS21, CS31);
//			    PROGRAM_REQUIREMENT(CS51);
//			    PROGRAM_REQUIREMENT(CS42);
//			  };
//			}

		ArrayList<String> courses = new ArrayList<String>();
		for (int level = 1; level <= num_levels; level++)
			for (int course_num = 1; course_num <= num_courses_per_level; course_num++) {
				String new_course = "CS" + level + course_num;
				if (courses.contains(new_course)) {
					// Could occur if level=1,course_num=11 and level=11,course_num=1, but unlikely to see these dimensions
					System.err.println("ERROR: Duplicate course ID: " + new_course);
					System.exit(1);
				}
				courses.add(new_course);
			}
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = academic_advising_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\tcourse : {");
		boolean first = true; 
		for (String course  : courses) {
			sb.append((first ? "" : ", ") + course);
			first = false;
		}
		sb.append("};\n\t};\n\n");
				
		sb.append("\tnon-fluents {\n");

		ArrayList<String> courses_so_far = new ArrayList<String>();
		for (int level = 1; level <= num_levels; level++)
			for (int course_num = 1; course_num <= num_courses_per_level; course_num++) {
				String course_name = "CS" + level + course_num;
				if (level > 1) {
					// Generate PREREQS
					int[] indices = Permutation.permute(courses_so_far.size(), ran); // permutation of {0..courses_so_far.size()}
					boolean plus_or_minus =  ran.nextFloat() < prob_more_less_prereqs;
					boolean plus = ran.nextBoolean();
					int num_prereqs_for_course = num_prereqs + (plus_or_minus ? (plus ? 1 : -1) : 0);
					for (int index = 0; index < num_prereqs_for_course && index < indices.length; index++)
						sb.append("\t\tPREREQ(" + courses_so_far.get(indices[index]) + "," + course_name + ");\n");
				}
				
				// Generate REQ?
				float rnum = ran.nextFloat();
				//System.err.println(rnum + " < " + prob_prog_req + " = " + (rnum < prob_prog_req));
				if (rnum < prob_prog_req) {
					sb.append("\t\tPROGRAM_REQUIREMENT(" + course_name + ");\n");
				}
				
				courses_so_far.add(course_name);
			}
				
		sb.append("\t};\n");
		sb.append("}\n\n");
		
//		instance academic_advising_inst_mdp__1 {
//			  domain = academic_advising_mdp;
//			  non-fluents = nf_academic_advising_inst_mdp__1;
//			  max-nondef-actions = 2;
//			  horizon = 40;
//			  discount = 1.0;
//		}

		sb.append("instance " + instance_name + " {\n");
		sb.append("\tdomain = academic_advising_mdp;\n");
		sb.append("\tnon-fluents = nf_" + instance_name + ";\n");
		sb.append("\tmax-nondef-actions = " + num_conc_actions + ";\n");
		sb.append("\thorizon  = " + horizon + ";\n");
		sb.append("\tdiscount = " + discount + ";\n");
		
		sb.append("}");
		
		return sb.toString();
	}
	
}
