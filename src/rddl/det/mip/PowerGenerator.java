package rddl.det.mip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;

public class PowerGenerator {

	private String FILE_NAME;

	public PowerGenerator( int numStations, double demandMean, double demandVar, 
			double payoffMean, double payoffVar, double costMean, double costVar,
			final long seed ) throws IOException {
		FILE_NAME = "files/power_gen_" + numStations +".rddl";
		NormalDistribution demandDist = new NormalDistribution( new MersenneTwister(seed), demandMean, demandVar );
		NormalDistribution payoffDist = new NormalDistribution( new MersenneTwister(seed), payoffMean, payoffVar );
		NormalDistribution costDist = new NormalDistribution( new MersenneTwister(seed), costMean, costVar );

		FileWriter outfile = new FileWriter( new File( FILE_NAME ) );
		outfile.write("non-fluents nf_inventory_control_"+ numStations +" {\n");
		outfile.write("\t domain = inventory_control_continuous;\n" );
		outfile.write("\t objects {\n");
		outfile.write("\t\t item : {");
		for( int i = 0 ; i < numStations; ++i ){
			outfile.write("t" + i + (( i == numStations - 1 ) ? "};" : ",") );
		}
		outfile.write("\n");
		outfile.write("\t };\n");
		
				
		outfile.write("\t\t capacity = " + 5*numShops*max_demand + ";\n" );
		outfile.write("\t };\n");
		outfile.write("}\n");
		
		outfile.write("instance inst_inventory_control_" + numShops + " { \n");
		outfile.write("\t domain = inventory_control_continuous;\n");
		outfile.write("\t non-fluents = nf_inventory_control_" + numShops +";\n" );
		outfile.write("\t horizon = 100;\n" );
		outfile.write("\t discount = 1.0;\n" );
		outfile.write("}");
		outfile.close();
		
		System.out.println( FILE_NAME );
	}
	
	public static void main(String[] args) throws IOException {
		
	}
}
