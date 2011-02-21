package rddl.competition.generators;

/**
 *  A generator for instances of a partially observable elevators domain.
 *  
 *  @author Tom Walsh
 *  @version 2/18/11
 * 
 **/

import java.util.Random;

public class ElevatorPOMDPGen extends ElevatorMDPGen {

public static void main(String [] args){
		
		if(args.length != 7){
			System.err.println("Usage: numElevators numFloors max-arrive min_arrive horizon discount numGenerations");
			System.err.println("Example:  1 4 0.3 0.1 100 0.9 1");
			System.exit(127);
		}
		
		ElevatorMDPGen efg = new ElevatorMDPGen(args);
		//ElevatorFullGen efg = new ElevatorFullGen(numEls, numFloors, startFloors, arrParams, Double.parseDouble(args[numEls + numFloors + 2]), Integer.parseInt(args[numEls + numFloors + 3]), Double.parseDouble(args[numEls + numFloors + 4]), Integer.parseInt(args[numEls + numFloors + 5]));
		int toGen = Integer.parseInt(args[args.length-1]);
		String s = "";
		for(int i =0 ; i < toGen ; i++){
			s = efg.generate();
			System.out.println(s);
		}
	}
	
   double revealMax = 0.9;
   double revealMin = 0.7;
   double prankMax = 0.3;
   double prankMin = 0.1;
   
	public  ElevatorPOMDPGen(String [] args){
		super(args);
	}
	
	public String generate(){
		Random ran = new Random();
		String s = super.generate();
		String [] broken = s.split("non-fluents \\{");
		String mid = "";
		for(int f =0; f < floors; f++){
			mid += "REVEAL-PROB(f" +f+ ") = " + (ran.nextDouble() * (revealMax - revealMin) + revealMin) + ";\n";
			mid += "PRANK(f" +f+ ") = " + (ran.nextDouble() * (prankMax - prankMin) + prankMin) + ";\n";
		}
		return broken[0] + "non-fluents {\n" + mid + broken[1];
	}
	
	
	
}
