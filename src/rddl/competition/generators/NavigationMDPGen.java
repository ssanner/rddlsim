/**
 *  A generator for navigation problems.  The notation is intentionally obscured
 *  to make this domain hard to interpret.
 *  
 *  @author Scott Sanner
 *  @version 2/18/11
 * 
 **/
package rddl.competition.generators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.random.RandomDataGenerator;

import util.Permutation;

public class NavigationMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int size_x;
	protected int size_y;
	protected boolean obfuscate;
	protected int horizon;
	protected double discount;
	
	public static void main(String[] args) throws Exception {

		if (args.length != 7)
			usage();

		NavigationMDPGen cr = new NavigationMDPGen(args);
		String content = cr.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(cr.output_dir + File.separator + cr.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name size_x size_y {obfuscate,normal} horizon discount");
		System.err.println("Example: files/testcomp/rddl crossing-traffic-5-5 5 5 40 1.0");
		System.exit(127);
	}
	
	public NavigationMDPGen(String [] args) {
		this.output_dir = args[0];
		this.instance_name = args[1];
		this.size_x = Integer.parseInt(args[2]);
		this.size_y = Integer.parseInt(args[3]);
		if (args[4].equals("obfuscate"))
			this.obfuscate = true;
		else if (args[4].equals("normal"))
			this.obfuscate = false;
		else {
			System.out.println("Expected one of {obfuscate,normal}, got '" + args[4] + "'"); 
			usage();
		}
		this.horizon = Integer.parseInt(args[5]);
		this.discount = Double.parseDouble(args[6]);
	}
	
	public String generate() {
		if (obfuscate)
			return generateObfuscate();
		else
			return generateNormal();
	}
	
	public String generateNormal(){
		RandomDataGenerator ran = new RandomDataGenerator();
		StringBuilder sb = new StringBuilder();
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = navigation_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\txpos : {");
		for (int i = 1; i <= size_x; i++)
			sb.append(((i > 1) ? "," : "") + "x" + i);
		sb.append("};\n");
		
		sb.append("\t\typos : {");
		for (int j = 1; j <= size_y; j++)
			sb.append(((j > 1) ? "," : "") + "y" + j);
		sb.append("};\n");
		
		sb.append("\t};\n");
		
//		NORTH(y1,y2);
//		SOUTH(y2,y1);
		sb.append("\tnon-fluents {\n");
		for (int j = 2; j <= size_y; j++) {
			sb.append("\t\tNORTH(y" + (j-1) + ",y" + j + ");\n");
			sb.append("\t\tSOUTH(y" + j + ",y" + (j-1) + ");\n");
		}
		sb.append("\n");
		
//		EAST(x1,x2);
//		WEST(x2,x1);
		for (int i = 2; i <= size_x; i++) {
			sb.append("\t\tEAST(x" + (i-1) + ",x" + i + ");\n");
			sb.append("\t\tWEST(x" + i + ",x" + (i-1) + ");\n");
		}
		sb.append("\n");
		
//		MIN-XPOS(x1);
//		MAX-XPOS(x4);
//		MIN-YPOS(y1);
//		MAX-YPOS(y5);
		sb.append("\t\tMIN-XPOS(x1);\n");
		sb.append("\t\tMAX-XPOS(x" + size_x + ");\n");
		sb.append("\t\tMIN-YPOS(y1);\n");
		sb.append("\t\tMAX-YPOS(y" + size_y + ");\n\n");

//		GOAL(x4,y5);
		sb.append("\t\tGOAL(x" + size_x + ",y" + size_y + ");\n\n");

//      obstacle-at(x1,y2);
		ArrayList<String> dp = new ArrayList<String>();
		for (int i = 1; i <= size_x; i++)
			for (int j = 2; j < size_y; j++)
				dp.add("\t\tP(x" + i + ",y" + j + ") = " + 
						((.01f + ((.9f*(i-1))/(double)(size_x - 1))) + 0.05f*ran.nextUniform(0d,1d)) + ";\n");
		int[] indices = Permutation.permute(dp.size(), ran);
		for (int i = 0; i < dp.size(); i++)
			sb.append(dp.get(/*indices[i]*/i));
		
		sb.append("\t};\n}");
		
		// Now generate domain
		
		sb.append("\n\ninstance " + this.instance_name + " {\n"); 
		sb.append("\tdomain = navigation_mdp;\n");
		sb.append("\tnon-fluents = nf_" + this.instance_name + ";\n" );
		sb.append("\tinit-state {\n");
		
//        robot-at(x1,y1);
		sb.append("\t\trobot-at(x" + size_x + ",y1);\n");
		
		sb.append("\t};\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon = " + this.horizon + ";\n");
		sb.append("\tdiscount = " + this.discount + ";\n");
		sb.append("}\n");
		return sb.toString();
	}
	
	public String generateObfuscate(){
		RandomDataGenerator ran = new RandomDataGenerator();
		StringBuilder sb = new StringBuilder();
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = navigation_mdp;\n");
		sb.append("\tobjects {\n");

		sb.append("\t\txpos : {");
		ArrayList<String> xp = new ArrayList<String>();
		for (int i = 1; i <= size_x; i++)
			xp.add("x" + (i*i+5));
		int[] indices = Permutation.permute(xp.size(), ran);
		for (int i = 0; i < indices.length; i++)
			sb.append(((i > 0) ? "," : "") + xp.get(indices[i]));
		sb.append("};\n");
		
		sb.append("\t\typos : {");
		ArrayList<String> yp = new ArrayList<String>();
		for (int j = 1; j <= size_y; j++)
			yp.add("y" + (j*j+11));
		indices = Permutation.permute(yp.size(), ran);
		for (int j = 0; j < indices.length; j++)
			sb.append(((j > 0) ? "," : "") + yp.get(indices[j]));
		sb.append("};\n");
		
		sb.append("\t};\n");
		
//		NORTH(y1,y2);
//		SOUTH(y2,y1);
		sb.append("\tnon-fluents {\n");
		
		ArrayList<String> dp = new ArrayList<String>();
		for (int j = 2; j <= size_y; j++) {
			dp.add("\t\tNORTH(y" + ((j-1)*(j-1)+11) + ",y" + (j*j+11) + ");\n");
			dp.add("\t\tSOUTH(y" + (j*j+11) + ",y" + ((j-1)*(j-1)+11) + ");\n");
		}
		//sb.append("\n");
		
//		EAST(x1,x2);
//		WEST(x2,x1);
		for (int i = 2; i <= size_x; i++) {
			dp.add("\t\tEAST(x" + ((i-1)*(i-1)+5) + ",x" + (i*i+5) + ");\n");
			dp.add("\t\tWEST(x" + (i*i+5) + ",x" + ((i-1)*(i-1)+5) + ");\n");
		}
		//sb.append("\n");
		
//		MIN-XPOS(x1);
//		MAX-XPOS(x4);
//		MIN-YPOS(y1);
//		MAX-YPOS(y5);
		dp.add("\t\tMIN-XPOS(x6);\n");
		dp.add("\t\tMAX-XPOS(x" + (size_x*size_x+5) + ");\n");
		dp.add("\t\tMIN-YPOS(y12);\n");
		dp.add("\t\tMAX-YPOS(y" + (size_y*size_y+11) + ");\n");

//		GOAL(x4,y5);
		dp.add("\t\tGOAL(x" + (size_x*size_x+5) + ",y" + (size_y*size_y+11) + ");\n");

//      obstacle-at(x1,y2);
		for (int i = 1; i <= size_x; i++)
			for (int j = 2; j < size_y; j++)
				dp.add("\t\tP(x" + (i*i+5) + ",y" + (j*j+11) + ") = " + 
						((.01f + ((.9f*(i-1))/(double)(size_x - 1))) + 0.05f*ran.nextUniform(0d,1d)) + ";\n");
		indices = Permutation.permute(dp.size(), ran);
		
		// Now export everything
		for (int i = 0; i < dp.size(); i++) {
			//sb.append(dp.get(i));
			sb.append(dp.get(indices[i]));
		}
		
		sb.append("\t};\n}");
		
		// Now generate domain
		
		sb.append("\n\ninstance " + this.instance_name + " {\n"); 
		sb.append("\tdomain = navigation_mdp;\n");
		sb.append("\tnon-fluents = nf_" + this.instance_name + ";\n" );
		sb.append("\tinit-state {\n");
		
//        robot-at(x1,y1);
		sb.append("\t\trobot-at(x" + (size_x*size_x+5) + ",y12);\n");
		
		sb.append("\t};\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon = " + this.horizon + ";\n");
		sb.append("\tdiscount = " + this.discount + ";\n");
		sb.append("}\n");
		return sb.toString();
	}
}

	