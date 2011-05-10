package rddl.competition.generators;

/**
 *  A POMDP generator for navigation.
 *  
 *  @author Scott Sanner
 *  @version 4/18/11
 * 
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class NavigationPOMDPGen extends NavigationMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 7)
			usage();

		NavigationPOMDPGen g = new NavigationPOMDPGen(args);
		String content = g.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(g.output_dir + File.separator + g.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public NavigationPOMDPGen(String[] args) {
		super(args);
	}

	public String generate() {
		String s = super.generate();
		s = s.replace("navigation_mdp", "navigation_pomdp");
		s = s.replaceAll("robot-at(.*);", "min-x;");
		return s;
	}

}
