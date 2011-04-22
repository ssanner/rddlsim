package rddl.competition.generators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class SkillTeachingPOMDPGen extends SkillTeachingMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 10)
			usage();

		SkillTeachingPOMDPGen efg = new SkillTeachingPOMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public SkillTeachingPOMDPGen(String [] args){
		super(args);
		noiseMax = Math.min(0.25f, Float.parseFloat(args[9]));
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name minNumSkils maxNumSkills maxPreReqs  maxMedCorrectProb maxLowCorrectProb horizon discount noiseMax");
		System.err.println("Example: files/testcomp/rddl recon-2 5 10 3  0.8 0.7 100 0.9 0.25");
		System.exit(127);
	}
	
	public String generate() {
		Random ran = new Random();
		String s = super.generate();
		s = s.replaceAll("LOSE_PROB(.*);", "");
		s = s.replaceAll("skill_teaching_mdp", "skill_teaching_pomdp");
		String[] broken = s.split("non-fluents \\{");
		String mid = "";
		
		for(int x = 0; x < curNumSkills; x++){
			float noise2 = ran.nextFloat() * noiseMax;
			if (noise2 < 0.01f)
				noise2 = 0.01f;
			//float noise2 = Math.min(noiseMax, 0.25f + ran.nextFloat() * (noiseMax - 0.25f));
			//mid += "\t\tREPORT_PROB(s" + x + ") = "
			//			+ (1.0 - noise1)
			//			+ ";\n";
			mid += "\t\tFALSE_POS(s" + x + ") = "
						+ noise2
						+ ";\n";
		}
		
		return broken[0] + "non-fluents {\n" + mid + broken[1];
	}
	
	protected float noiseMax;
}
