package rddl.competition.generators;

/**
 *  A generator for instances of a partially observable reconaissance domain.
 *  
 *  @author Tom Walsh
 *  @version 2/18/11
 * 
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class ReconPOMDPGen extends ReconMDPGen {
	
	public static void main(String[] args) throws Exception {

		if (args.length != 10)
			usage();

		ReconPOMDPGen efg = new ReconPOMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public static void usage() {
		System.err.println("Usage: output-dir instance-name size maxObjects damageProbMax maxHazardDensity  lifeDensity horizon discount noiseMax");
		System.err.println("Example: files/testcomp/rddl recon-2 4 3 0.9 0.5 0.7 0.8 100 0.9 0.2");
		System.exit(127);
	}
	
	protected float noiseMax;
	
	public ReconPOMDPGen(String [] args){
		super(args);
		noiseMax = Float.parseFloat(args[9]);
	}
	
	public String generate() {
		Random ran = new Random();
		String s = super.generate(false);
		s = s.replaceAll("recon_mdp", "recon_pomdp");
		String[] broken = s.split("non-fluents \\{");
		String mid = "";
		
		mid += "\t\tLIFE_PROB = " + lifeDensity + ";\n";
		mid += "\t\tWATER_PROB = " + Math.min(lifeDensity + 0.4, 0.8) + ";\n";
		
		String[] tools = {"w1","l1"};
		
		for (String t : tools){
			float noise1 = (ran.nextFloat() * noiseMax * 0.99f) + 0.01f;
			float noise2 = (ran.nextFloat() * noiseMax * 0.99f) + 0.01f;
			mid += "\t\tDAMAGE_OBS(" + t + ") = "
						+ (1.0 - noise1)
						+ ";\n";
			mid += "\t\tNOISE_DAMAGE_OBS(" + t + ") = "
						+ (noise2)
						+ ";\n";
		}
		String [] byLine = broken[1].split("\n");
		String second ="";
		for(String l : byLine)
			if(!l.contains("HAS"))
				second += l + "\n";
		
		return broken[0] + "non-fluents {\n" + mid + second;
	}
	
}
