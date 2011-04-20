package rddl.competition.generators;

/**
 *  A generator for instances of a fully observable elevators domain.
 *  
 *  @author Tom Walsh
 *  @version 2/18/11
 * 
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class SkillTeachingMDPGen {

	// parameters are number of elevators, number of floors, starting floor of
	// each elevator,
	// arrival parameter on each floor, horizon and discount

	
	protected double MAX_LOSS = 0.05;
	
	
	protected int minNumSkills;
	protected int maxNumSkills;
	protected int maxPreReqs;
	//protected int numOutliers; //some skills will be really hard to learn
	protected float maxMedCorrectProb;
	protected float maxLowCorrectProb;
	
	protected int curNumSkills;
	
	
	protected String output_dir;
	protected String instance_name;
	
	protected int hor;
	protected float dis;
	protected int id;

	public static void main(String[] args) throws Exception {

		if (args.length != 9)
			usage();

		SkillTeachingMDPGen efg = new SkillTeachingMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public static void usage() {
		System.err.println("Usage: output-dir instance-name minNumSkils maxNumSkills maxPreReqs  maxMedCorrectProb maxLowCorrectProb horizon discount");
		System.err.println("Example: files/testcomp/rddl recon-2 5 10 3  0.8 0.7 100 0.9");
		System.exit(127);
	
		
	}

	public SkillTeachingMDPGen(String[] args) {
		// int els, int floors, int[]starts, float [] arrs, float pen, int
		// hor, float dis, int id){

		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		
		id = 0;
		
		try {
			
			
		minNumSkills = Integer.parseInt(args[2]);
		maxNumSkills = Integer.parseInt(args[3]);
		maxPreReqs = Integer.parseInt(args[4]);
		//numOutliers = Integer.parseInt(args[5]);
		maxMedCorrectProb = Float.parseFloat(args[5]);
		maxLowCorrectProb = Float.parseFloat(args[6]);
		hor = Integer.parseInt(args[7]);
		dis = Float.parseFloat(args[8]);
		} catch (Exception ex) {
			System.err.println("Error in onr of the inputs");
			System.exit(127);
		}
	}

	public String generate() {
		Random ran = new Random();
		int numSkills = ran.nextInt(maxNumSkills - minNumSkills +1) + minNumSkills;
		curNumSkills = numSkills;
		
		String s = "";
		s += "non-fluents nf_" + instance_name 
				+ " {\n\tdomain = skill_teaching_mdp; \n\tobjects { \n";
		s += "\t\tskill : {";
		for (int e = 0; e < numSkills; e++) {
			s += "s" + e;
			if (e < numSkills - 1)
				s += ",";
		}
		s += "};\n";
		
		s += "\n\t}; \n\tnon-fluents {\n";
		//generate pre-reqs for each skill, store number of pre-reqs
		int [] numPreReqs = new int[numSkills];
		int [] maxDepth = new int[numSkills];
		for(int x=0; x < numSkills; x++)
			maxDepth[x] = 1;
		for(int x = 0; x < numSkills; x++){
			if(x< 2){
				numPreReqs[x] = 0;
			}
			else{
			numPreReqs[x] = Math.min(x, Math.max(1, ran.nextInt(maxPreReqs + 1)));
			}
			HashSet<Integer> myPres = new HashSet<Integer>();
			for(int j =0; j < numPreReqs[x]; j++){
				int candidate;
				do{
					candidate = ran.nextInt(x);
				}while(myPres.contains(candidate));
				s += "\t\tPRE_REQ(s" + candidate + ", s" + x + ");\n";
				maxDepth[x] = Math.max(maxDepth[x], maxDepth[candidate] + 1);
				myPres.add(candidate);
			}
			
			float low = (ran.nextFloat() * (maxLowCorrectProb - 0.5f) + 0.5f);
			float med = Math.max(Math.max(low, 0.65f), (ran.nextFloat() * (maxMedCorrectProb - 0.65f) + 0.65f));
			
			s+= "\t\tPROB_ALL_PRE(s" +x + ") = " + low +";\n";
			if(numPreReqs[x] > 0)
				s+= "\t\tPROB_PER_PRE(s" +x + ") = " + Math.max(0.0f, (low / (float) numPreReqs[x] - ran.nextFloat() * 0.1)) +";\n";
			s+= "\t\tPROB_ALL_PRE_MED(s" +x + ") = " + med +";\n";
			if(numPreReqs[x] > 0)
				s+= "\t\tPROB_PER_PRE_MED(s" +x + ") = "+ Math.max(0.0f,(med / (float) numPreReqs[x] - ran.nextFloat() * 0.1))+";\n";
			s+= "\t\tPROB_HIGH(s" +x + ") = " + (ran.nextFloat() * 0.15f + 0.85f)+";\n";
				
			//skill weight based on how far into the chain you are
			s+= "\t\tSKILL_WEIGHT(s" +x + ") = " + (maxDepth[x] + ran.nextFloat() * 0.5f) +";\n";
			double lose_prob = ran.nextFloat() * MAX_LOSS;
			if (lose_prob < 0.01d)
				lose_prob = 0.01d;
			s+= "\t\tLOSE_PROB(s" +x + ") = " + lose_prob +";\n";
		}
		
		
		s += "\t};\n}\ninstance " + instance_name + " { \n\tdomain = skill_teaching_mdp; \n ";
		s += "\tnon-fluents = nf_" + instance_name + ";\n";//\tinit-state { \n";
		
		//for(int x = 0; x < numSkills; x++){
		//	s += "\t\tproficiencyLow(s" + x+ ");\n";
		//}
		//s += "\t};\n";
		s += "\tmax-nondef-actions = " + 1 + ";\n";
		s += "\thorizon = " + hor + ";\n";
		s += "\tdiscount = " + dis + ";\n} \n";

		id++;
		return s;
	}

}
