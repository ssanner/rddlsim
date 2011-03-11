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

public class TrafficPOMDPGen extends TrafficMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 7)
			usage();

		TrafficPOMDPGen efg = new TrafficPOMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public TrafficPOMDPGen(String[] args) {
		super(args);
	}

	public String generate() {
		String s = super.generate();
		s = s.replace("traffic_mdp", "traffic_pomdp");
		return s;
	}

}
