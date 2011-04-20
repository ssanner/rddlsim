package rddl.competition.generators;

/**
 *  A POMDP generator for crossing traffic.
 *  
 *  @author Scott Sanner
 *  @version 4/16/11
 * 
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class CrossingTrafficPOMDPGen extends CrossingTrafficMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 7)
			usage();

		CrossingTrafficPOMDPGen g = new CrossingTrafficPOMDPGen(args);
		String content = g.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(g.output_dir + File.separator + g.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public CrossingTrafficPOMDPGen(String[] args) {
		super(args);
	}

	public String generate() {
		String s = super.generate();
		s = s.replace("crossing_traffic_mdp", "crossing_traffic_pomdp");
		return s;
	}

}
