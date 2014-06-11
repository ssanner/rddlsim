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

public class AcademicAdvisingPOMDPGen extends AcademicAdvisingMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 10)
			usage();

		AcademicAdvisingPOMDPGen efg = new AcademicAdvisingPOMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public AcademicAdvisingPOMDPGen(String[] args) {
		super(args);
	}

	public String generate() {
		String s = super.generate();
		s = s.replace("academic_advising_mdp", "academic_advising_pomdp");
		return s;
	}

}
