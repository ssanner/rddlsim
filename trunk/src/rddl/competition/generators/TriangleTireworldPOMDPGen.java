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
import java.util.Random;

public class TriangleTireworldPOMDPGen extends TriangleTireworldMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 6)
			usage();

		TriangleTireworldPOMDPGen efg = new TriangleTireworldPOMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public TriangleTireworldPOMDPGen(String[] args) {
		super(args);
	}

	public String generate() {
		String s = super.generate();
		s = s.replace("triangle_tireworld_mdp", "triangle_tireworld_pomdp");
		return s;
	}

}
