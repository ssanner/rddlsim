package rddl.competition.generators;

/**
 *  A generator for instances of a partially observable elevators domain.
 *  
 *  @author Tom Walsh
 *  @version 2/18/11
 * 
 **/

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class ElevatorPOMDPGen extends ElevatorMDPGen {

	public static void main(String[] args) throws Exception {

		if (args.length != 8)
			usage();

		ElevatorPOMDPGen efg = new ElevatorPOMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	protected double revealMax = 0.9;
	protected double revealMin = 0.7;
	protected double prankMax = 0.3;
	protected double prankMin = 0.1;

	public ElevatorPOMDPGen(String[] args) {
		super(args);
	}

	public String generate() {
		Random ran = new Random();
		String s = super.generate();
//		String[] broken = s.split("non-fluents \\{");
//		String mid = "";
//		for (int f = 0; f < floors; f++) {
//			mid += "REVEAL-PROB(f" + f + ") = "
//					+ (ran.nextDouble() * (revealMax - revealMin) + revealMin)
//					+ ";\n";
//			mid += "PRANK(f" + f + ") = "
//					+ (ran.nextDouble() * (prankMax - prankMin) + prankMin)
//					+ ";\n";
//		}
//		return broken[0] + "non-fluents {\n" + mid + broken[1];
		
		// For now, we don't assume any stochasticity with observations
		s = s.replace("elevators_mdp", "elevators_pomdp");
		return s;
	}

}
