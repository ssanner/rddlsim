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
import java.util.List;
import java.util.Random;

public class ElevatorMDPGen {

	// parameters are number of elevators, number of floors, starting floor of
	// each elevator,
	// arrival parameter on each floor, horizon and discount

	protected final int outliersLow = 0;
	protected final int outliersHigh = 0;

	protected String output_dir;
	protected String instance_name;
	protected int els;
	protected int floors;
	protected float maxA;
	protected float minA;
	protected int hor;
	protected float dis;
	protected int id;

	public static void main(String[] args) throws Exception {

		if (args.length != 8)
			usage();

		ElevatorMDPGen efg = new ElevatorMDPGen(args);
		String content = efg.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public static void usage() {
		System.err.println("Usage: output-dir instance-name numElevators numFloors max-arrive min_arrive horizon discount");
		System.err.println("Example: files/testcomp/rddl elevator-1-4 1 4 0.3 0.1 100 0.9");
		System.exit(127);
	}

	public ElevatorMDPGen(String[] args) {
		// int els, int floors, int[]starts, float [] arrs, float pen, int
		// hor, float dis, int id){

		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		
		id = 0;
		els = 0;
		floors = 0;
		try {
			els = Integer.parseInt(args[2]);
		} catch (Exception ex) {
			System.err.println("Number of elevators must be an integer");
			System.exit(127);
		}
		try {
			floors = Integer.parseInt(args[3]);
		} catch (Exception ex) {
			System.err.println("Number of floors must be an integer");
			System.exit(127);
		}

		maxA = Float.parseFloat(args[4]);
		minA = Float.parseFloat(args[5]);
		hor = Integer.parseInt(args[6]);
		dis = Float.parseFloat(args[7]);
	}

	public String generate() {
		Random ran = new Random();
		int[] starts = new int[els];
		for (int x = 0; x < els; x++)
			starts[x] = 0; //ran.nextInt(floors);

		List<Integer> lows = new ArrayList<Integer>();
		List<Integer> highs = new ArrayList<Integer>();
		while (lows.size() < outliersLow) {
			int r = ran.nextInt(floors);
			if (!lows.contains(r))
				lows.add(r);
		}

		while (highs.size() < outliersHigh) {
			int r = ran.nextInt(floors);
			if (!highs.contains(r) && !lows.contains(r))
				highs.add(r);
		}

		float[] arrs = new float[floors];
		try {
			for (int x = 0; x < floors; x++) {
				if (highs.contains(x))
					arrs[x] = 0.2f + ((ran.nextFloat() * (maxA - minA) + minA) / (floors));
				else if (lows.contains(x))
					arrs[x] = 0.0f;
				else
					arrs[x] = (ran.nextFloat() * (maxA - minA) + minA)
							/ (floors);
				if (arrs[x] > 1.0)
					arrs[x] = 1.0f;
			}
		} catch (Exception ex) {
			System.err.println("Arrival params must be floats");
			System.exit(127);
		}

		String s = "";
		s += "non-fluents nf_" + instance_name 
				+ " {\n\tdomain = elevators_mdp; \n\tobjects { \n";
		s += "\t\televator : {";
		for (int e = 0; e < els; e++) {
			s += "e" + e;
			if (e < els - 1)
				s += ",";
		}
		s += "};\n\t\tfloor : {";
		for (int e = 0; e < floors; e++) {
			s += "f" + e;
			if (e < floors - 1)
				s += ",";
		}
		s += " }; \n\t}; \n\tnon-fluents {\n";
		s += "\t\tELEVATOR-PENALTY-RIGHT-DIR = " + 0.75 + ";\n";
		s += "\t\tELEVATOR-PENALTY-WRONG-DIR = " + 3.00 + ";\n";
		for (int e = 0; e < floors; e++) {
			if (e != 0 && e != (floors-1))
				s += "\t\tARRIVE-PARAM(f" + e + ") = " + arrs[e] + ";\n";
			if (e < floors - 1)
				s += "\t\tADJACENT-UP(f" + e + ",f" + (e + 1) + ") = true;\n";
		}
		s += "\t\tTOP-FLOOR(f" + (floors - 1) + ") = true;\n";
		s += "\t\tBOTTOM-FLOOR(f0) = true;\n \t}; \n }\n";

		s += "instance " + instance_name + " { \n\tdomain = elevators_mdp; \n ";
		s += "\tnon-fluents = nf_" + instance_name + ";\n\tinit-state { \n";
		for (int e = 0; e < els; e++) {
			s += "\t\televator-at-floor(e" + e + ",f" + starts[e] + ");\n";
		}

		s += "\t};\n\tmax-nondef-actions = " + els + ";\n";
		s += "\thorizon = " + hor + ";\n";
		s += "\tdiscount = " + dis + ";\n} \n";

		id++;
		return s;
	}

}
