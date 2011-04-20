/**
 *  A generator for instances of crossing traffic.
 *  
 *  @author Sungwook Yoon
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

public class CrossingTrafficMDPGen {

	protected String output_dir;
	protected String instance_name;
	protected int size_x;
	protected int size_y;
	protected double input_rate;
	protected int horizon;
	protected double discount;
	
	public static void main(String[] args) throws Exception {

		if (args.length != 7)
			usage();

		CrossingTrafficMDPGen cr = new CrossingTrafficMDPGen(args);
		String content = cr.generate();
		PrintStream ps = new PrintStream(
				new FileOutputStream(cr.output_dir + File.separator + cr.instance_name + ".rddl"));
		ps.println(content);
		ps.close();
	}
	
	public static void usage() {
		System.err.println("Usage: output-dir instance-name size_x size_y input_rate horizon discount");
		System.err.println("Example: files/testcomp/rddl crossing-traffic-5-5 5 5 0.2 40 1.0");
		System.exit(127);
	}
	
	public CrossingTrafficMDPGen(String [] args) {
		this.output_dir = args[0];
		this.instance_name = args[1];
		this.size_x = Integer.parseInt(args[2]);
		this.size_y = Integer.parseInt(args[3]);
		this.input_rate = Double.parseDouble(args[4]);
		this.horizon = Integer.parseInt(args[5]);
		this.discount = Double.parseDouble(args[6]);
	}
	
	public String generate(){
		Random ran = new Random();
		StringBuilder sb = new StringBuilder();
		
		sb.append("non-fluents nf_" + instance_name + " {\n");
		sb.append("\tdomain = crossing_traffic_mdp;\n");
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

//		INPUT-RATE = 0.2;
		sb.append("\t\tINPUT-RATE = " + input_rate + ";\n");
 
		sb.append("\t};\n}");
		
		// Now generate domain
		
		sb.append("\n\ninstance " + this.instance_name + " {\n"); 
		sb.append("\tdomain = crossing_traffic_mdp;\n");
		sb.append("\tnon-fluents = nf_" + this.instance_name + ";\n" );
		sb.append("\tinit-state {\n");
		
//        robot-at(x1,y1);
		sb.append("\t\trobot-at(x" + size_x + ",y1);\n");
		
//        obstacle-at(x1,y2);
		for (int i = 1; i <= size_x; i++)
			for (int j = 2; j < size_y; j++)
				if (ran.nextBoolean())
					sb.append("\t\tobstacle-at(x" + i + ",y" + j + ");\n");

		sb.append("\t};\n");
		sb.append("\tmax-nondef-actions = 1;\n");
		sb.append("\thorizon = " + this.horizon + ";\n");
		sb.append("\tdiscount = " + this.discount + ";\n");
		sb.append("}\n");
		return sb.toString();
	}
}

	