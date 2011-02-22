package rddl.competition.generators;

/**
 *  A generator for instances of a fully observable elevators domain.
 *  
 *  @author Tom Walsh
 *  @version 2/18/11
 * 
 **/


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ElevatorMDPGen {

	//parameters are number of elevators, number of floors, starting floor of each elevator,
	//arrival parameter on each floor, horizon and discount
	
	protected int outliersLow = 1;
	protected int outliersHigh = 1;
	
	public static void main(String [] args){
		
		if(args.length != 7){
			System.err.println("Usage: numElevators numFloors max-arrive min_arrive horizon discount numInstances");
			System.err.println("Example:  1 4 0.3 0.1 100 0.9 1");
			System.exit(127);
		}
		
		
//		for(int x = 1; x < 200; x++ ){
//			args[1] = x +"";
			ElevatorMDPGen efg = new ElevatorMDPGen(args);
			//ElevatorFullGen efg = new ElevatorFullGen(numEls, numFloors, startFloors, arrParams, Double.parseDouble(args[numEls + numFloors + 2]), Integer.parseInt(args[numEls + numFloors + 3]), Double.parseDouble(args[numEls + numFloors + 4]), Integer.parseInt(args[numEls + numFloors + 5]));
			int toGen = Integer.parseInt(args[args.length-1]);
			String s = "";
			for(int i =0 ; i < toGen ; i++){
				s = efg.generate();
				System.out.println(s);
			}
		//}
	}
	
	public  ElevatorMDPGen(String [] args){
		//int els, int floors, int[]starts, double [] arrs, double pen, int hor, double dis, int id){
		id = 0;
		if(args.length < 2){
			System.err.println("Usage: numElevators numFloors max-arrive, min_arrive   horizon  discount numInstances");
			System.err.println("Example:  1 4 0.3 0.1 100 0.9 1");
			System.exit(127);
		}
		els =0;
		floors=0;
		try{
		els = Integer.parseInt(args[0]);
		}catch(Exception ex){
			System.err.println("Number of elevators must be an integer");
			System.exit(127);
		}
		try{
			floors = Integer.parseInt(args[1]);
			}catch(Exception ex){
				System.err.println("Number of floors must be an integer");
				System.exit(127);
			}
		if(args.length < 7){
			System.err.println("Usage: numElevators numFloors max-arrive, min_arrive  horizon  discount numGenerations");
			System.err.println("Example:  1 4 0.3 0.1 1.2 100 0.9 1");
			System.exit(127);
		}
		
		
		
		maxA = Double.parseDouble(args[2]);
		minA = Double.parseDouble(args[3]);
		hor = Integer.parseInt(args[4]);
		dis = Double.parseDouble(args[5]);
	
		
	}
	
	protected int els;
	protected int floors;
	protected double  maxA;
	protected double  minA;
	protected int hor;
	protected double dis;
	protected int id;
	
	public String generate(){
		Random ran = new Random();
		int [] starts = new int[els];
		try{
		for(int x =0;  x < els; x++)
			starts[x] = ran.nextInt(floors);
		}catch(Exception ex){
			System.err.println("Starting floors must be integers");
			System.exit(127);
		}
		
		List<Integer> lows = new ArrayList<Integer>();
		List<Integer> highs = new ArrayList<Integer>();
		while(lows.size() < outliersLow){
			int r = ran.nextInt(floors);
			if(!lows.contains(r))
				lows.add(r);
		}
		
		while(highs.size() < outliersHigh){
			int r = ran.nextInt(floors);
			if(!highs.contains(r) && !lows.contains(r))
				highs.add(r);
		}
		
		
		double [] arrs = new double[floors];
		try{
		for(int x =0;  x < floors; x++){
			if(highs.contains(x))
				arrs[x] = 0.2 + ((ran.nextDouble() * (maxA- minA) + minA)  / (floors));
			else if(lows.contains(x))
				arrs[x] = 0.0;
			else
				arrs[x] = (ran.nextDouble() * (maxA- minA) + minA)  / (floors);
			if(arrs[x] > 1.0)
				arrs[x] = 1.0;
		}
		}catch(Exception ex){
			System.err.println("Arrival params must be doubles");
			System.exit(127);
		}
		
		
		
		String s = "";
		s += "non-fluents elComp" + id + " {\n\tdomain = elevators_mdp; \n\tobjects { \n";
		s += "\t\televator : {";
		for(int e = 0; e < els; e++){
			s+= "e" + e;
			if(e < els -1)
				s+= ",";
		}
		s+= "};\n\t\tfloor : {";
		for(int e = 0; e < floors; e++){
			s+= "f" + e;
			if(e < floors -1)
				s+= ",";
		}
		s+= "\t}; \n }; \n\tnon-fluents {\n";
		s+= "\t\tELEVATOR-PENALTY-RIGHT-DIR = "+ 0.50 + ";\n";
		s+= "\t\tELEVATOR-PENALTY-WRONG-DIR = "+ 3.00 + ";\n";
		for(int e = 0; e < floors; e++){
			s+= "\t\tARRIVE-PARAM(f" + e + ") = " + arrs[e] + ";\n"; 
			if(e < floors - 1)
				s+= "\t\tADJACENT-UP(f" + e + ",f" + (e+1) +") = true;\n";
		}
		s+= "\t\tTOP-FLOOR(f"+ (floors-1) + ") = true;\n";  
		s+= "\t\tBOTTOM-FLOOR(f0) = true;\n \t}; \n }\n";
				
		s += "instance ieComp" + id + " { \n\tdomain = elevators_mdp; \n ";
		s += "\tnon-fluents = elComp" + id + ";\n\tinit-state { \n";
		for(int e =0; e < els; e++){
			s+= "\t\televator-at-floor(e" + e + ",f" + starts[e] + ");\n";  
		}
			
		s+="\t};\n\tmax-nondef-actions = 1;\n";
		s+= "\thorizon = " + hor + ";\n";
		s+= "\tdiscount = " + dis + ";\n} \n";
		
		id++;
		return s;
	}
	
}
