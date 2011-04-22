package rddl.competition.generators;

/**
 *  A generator for instances of a fully observable reconaissance domain.
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

public class ReconMDPGen {

	// parameters are number of elevators, number of floors, starting floor of
	// each elevator,
	// arrival parameter on each floor, horizon and discount

	//protected final int outliersLow = 0;
	//protected final int outliersHigh = 0;

	protected int size;
	protected int maxObjects;
	protected float damageProbMax;
	protected float maxHazardDensity;
	protected float lifeDensity;
	
	
	protected String output_dir;
	protected String instance_name;
	
	protected int hor;
	protected float dis;
	protected int id;

	public static void main(String[] args) throws Exception {

		if (args.length != 9)
			usage();

		ReconMDPGen efg = new ReconMDPGen(args);
		String content = efg.generate(true);
		PrintStream ps = new PrintStream(
				new FileOutputStream(efg.output_dir + File.separator + efg.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}

	public static void usage() {
		System.err.println("Usage: output-dir instance-name size maxObjects damageProbMax maxHazardDensity  lifeDensity horizon discount");
		System.err.println("Example: files/testcomp/rddl recon-2 4 5 0.9 0.5 0.7 100 0.9");
		System.exit(127);
	}

	public ReconMDPGen(String[] args) {
		// int els, int floors, int[]starts, float [] arrs, float pen, int
		// hor, float dis, int id){

		output_dir = args[0];
		if (output_dir.endsWith("/") || output_dir.endsWith("\\"))
			output_dir = output_dir.substring(0, output_dir.length() - 1);
		
		instance_name = args[1];
		
		id = 0;
		
		try {
			
		
		size = Integer.parseInt(args[2]);
		maxObjects = Integer.parseInt(args[3]);
		damageProbMax = Float.parseFloat(args[4]);
		maxHazardDensity = Float.parseFloat(args[5]);
		lifeDensity = Float.parseFloat(args[6]);
		hor = Integer.parseInt(args[7]);
		dis = Float.parseFloat(args[8]);
		} catch (Exception ex) {
			System.err.println("Error in onr of the inputs");
			System.exit(127);
		}
	}

	public String generate(boolean mdp) {
		Random ran = new Random();
		int numObjects = Math.max(2, maxObjects); //Math.max(2, ran.nextInt(maxObjects +1));
		int numHazards = Math.max(1, ran.nextInt((int) (size * size * maxHazardDensity))); 
		while(size * size -  1 - numHazards - numObjects < 0)
			numHazards--;
		if (numHazards < 1)
			numHazards = 1;
		//System.out.println("Num hazards: " + numHazards);

		//System.err.println(numObjects + "  " + numHazards);
		
		String s = "";
		s += "non-fluents nf_" + instance_name 
				+ " {\n\tdomain = recon_mdp; \n\tobjects { \n";
		s += "\t\tx_pos : {";
		for (int e = 0; e < size; e++) {
			s += "x" + e;
			if (e < size - 1)
				s += ",";
		}
		s += "};\n\t\ty_pos : {";
		for (int e = 0; e < size; e++) {
			s += "y" + e;
			if (e < size - 1)
				s += ",";
		}
		s += "};\n\t\tobj : {";
		for (int e = 0; e < numObjects; e++) {
			s += "o" + e;
			if (e < numObjects - 1)
				s += ",";
		}
		s += "};\n\t\tagent : {a1};\n\t\ttool : {l1,w1,p1};\n";
		
		s += "\n\t}; \n\tnon-fluents {\n";
		for(int x = 0; x < size; x++){
			s += "\t\tADJACENT-LEFT(x" + x + ", x" +  Math.max(0, x-1) + ");\n";
			s += "\t\tADJACENT-DOWN(y" + x + ", y" +  Math.max(0, x-1) + ");\n";
			s += "\t\tADJACENT-RIGHT(x" + x + ", x"  + Math.min(size-1, x+1) + ");\n";
			s += "\t\tADJACENT-UP(y" + x + ", y" +  Math.min(size-1, x+1) + ");\n";
		}
		s+= "\t\tWATER_TOOL(w1);\n\t\tLIFE_TOOL(l1);\n\t\tCAMERA_TOOL(p1);\n";
		//s+= "\t\tDETECT_PROB(w1) = " + (ran.nextFloat() * 0.15f + 0.85f)+";\n";
		//s+= "\t\tDETECT_PROB(l1) = " + (ran.nextFloat() * 0.25f+ 0.75f) +";\n";
		//s+= "\t\tDETECT_PROB_DAMAGED(w1) = " + ran.nextFloat() * 0.7f +";\n";
		//s+= "\t\tDETECT_PROB_DAMAGED(l1) = " + ran.nextFloat() * 0.6f +";\n";
		
		int [] filled = new int[size * size];
		for(int x = 0; x < filled.length; x++)
				filled[x] = 0;
		int baseLoc = ran.nextInt(size * size);
		s+= "\t\tBASE(x" + baseLoc % size + ",y" + baseLoc / size+ ");\n";
		filled[baseLoc] = 1;;
		int loc;
		int life =0;
		for(int o = 0; o < numObjects;o++){
			int tries = 0;
			do{
				loc = ran.nextInt(size * size);
			}while(filled[loc] == 1 && ++tries < 100);
			s+= "\t\tobjAt(o" + o +",x" + loc % size + ",y" + loc / size + ");\n";
			filled[loc] =1;
			if(ran.nextFloat()< lifeDensity || life < 1){
				if (!mdp) {
					s+= "\t\tHAS_WATER(o" + o + ");\n";
					s+= "\t\tHAS_LIFE(o" + o + ");\n";
				}
				life++;
				//System.err.println(numObjects + "  " + life);
			}
			else if(ran.nextFloat()< 0.7){
				if (!mdp)
					s+= "\t\tHAS_WATER(o" + o + ");\n";
			}
		}
		for(int x = 0; x < filled.length; x++)
			filled[x] = 0;// allowing hazards on objects
		filled[baseLoc] =1;
		for(int o = 0; o < numHazards;o++){
			int tries = 0;
			do{
				loc = ran.nextInt(size * size);
			}while(filled[loc] == 1 && ++tries < 100);
			s+= "\t\tHAZARD(x" + loc % size + ",y" + loc / size + ");\n";
			filled[loc] =1;
		}
		
		s += "\t\tDAMAGE_PROB(w1) = " + (ran.nextFloat()* (damageProbMax -0.25f) + 0.25f) + ";\n";
		s += "\t\tDAMAGE_PROB(l1) = " + (ran.nextFloat()* (damageProbMax -0.25f) + 0.25f) + ";\n";
		
		s += "\t\tGOOD_PIC_WEIGHT = " + (ran.nextFloat()*0.9f + 0.1f)  + ";\n";
		s += "\t\tBAD_PIC_WEIGHT = " + (ran.nextFloat()*0.9f + 0.1f) + ";\n";
		
		
		s += "\t};\n}\ninstance " + instance_name + " { \n\tdomain = recon_mdp; \n ";
		s += "\tnon-fluents = nf_" + instance_name + ";\n\tinit-state { \n";
		
		s += "\t\tagentAt(a1,x" + baseLoc % size + ",y" + baseLoc/size + ");\n";

		s += "\t};\n\tmax-nondef-actions = " + 1 + ";\n";
		s += "\thorizon = " + hor + ";\n";
		s += "\tdiscount = " + dis + ";\n} \n";

		id++;
		return s;
	}

}
