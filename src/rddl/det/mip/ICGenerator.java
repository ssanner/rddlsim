package rddl.det.mip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;

public class ICGenerator {

	private String FILE_NAME;

	public ICGenerator( int numShops, double demandMean, double demandVar, 
			double payoffMean, double payoffVar, double costMean, double costVar,
			final long seed ) throws IOException {
		FILE_NAME = "files/inventory_control_" + numShops +".rddl";
		NormalDistribution demandDist = new NormalDistribution( new MersenneTwister(seed), demandMean, demandVar );
		NormalDistribution payoffDist = new NormalDistribution( new MersenneTwister(seed), payoffMean, payoffVar );
		NormalDistribution costDist = new NormalDistribution( new MersenneTwister(seed), costMean, costVar );

		FileWriter outfile = new FileWriter( new File( FILE_NAME ) );
		outfile.write("non-fluents nf_inventory_control_"+numShops+" {\n");
		outfile.write("\t domain = inventory_control_continuous;\n" );
		outfile.write("\t objects {\n");
		outfile.write("\t\t item : {");
		for( int i = 0 ; i < numShops; ++i ){
			outfile.write("t" + i + (( i == numShops - 1 ) ? "};" : ",") );
		}
		outfile.write("\n");
		outfile.write("\t };\n");
		
				
		outfile.write("\t non-fluents {\n");
		double max_demand = Double.NEGATIVE_INFINITY;
		for( int i = 0 ; i < numShops; ++i ){
			double this_demand = demandDist.sample();
			max_demand = Math.max( max_demand, this_demand );
			outfile.write("\t\t demand(t" + i + ") = " + this_demand + ";\n");
		}
		outfile.write("\n");
		for( int i = 0 ; i < numShops; ++i ){
			outfile.write("\t\t payoff(t" + i + ") = " + payoffDist.sample() + ";\n");
		}
		
		outfile.write("\n");
		for( int i = 0 ; i < numShops; ++i ){
			outfile.write("\t\t cost(t" + i + ") = " + costDist.sample() + ";\n");
		}
		
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
		new ICGenerator(10, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(15, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(20, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(25, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(30, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(35, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(40, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(45, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		new ICGenerator(50, 5, 0.5, 1.5, 0.3, 0.8, 0.1, 42 );
		
//		new ICGenerator(10, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(15, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(20, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(25, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(30, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(35, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(40, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(45, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ICGenerator(50, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
		
	}
}
