package rddl.det.mip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;

public class ReservoirGenerator {

	private String FILE_NAME;

	public ReservoirGenerator( int numReserv, double rainMean, double rainVar, 
			double minMean, double minVar, double maxMean, double maxVar,
			final long seed ) throws IOException {
		FILE_NAME = "files/reservoir_control_" + numReserv +".rddl";
		
		//iid
		NormalDistribution rainDist = new NormalDistribution( new MersenneTwister(seed), rainMean, rainVar );
		NormalDistribution minDist = new NormalDistribution( new MersenneTwister(seed), minMean, minVar );
		NormalDistribution maxDist = new NormalDistribution( new MersenneTwister(seed), maxMean, maxVar );

		FileWriter outfile = new FileWriter( new File( FILE_NAME ) );
		outfile.write("non-fluents nf_reservoir_control_"+numReserv+" {\n");
		outfile.write("\t domain = reservoir_control;\n" );
		outfile.write("\t objects {\n");
		
		outfile.write("\t\t reservoir : {");
		for( int i = 0 ; i < numReserv; ++i ){
			outfile.write("t" + i + (( i == numReserv - 1 ) ? "};\n" : ",") );
		}
		outfile.write("\t\t pipe : {");
		for( int i = 0 ; i < numReserv -1 ; ++i ){
			outfile.write("p" + i + (( i == numReserv - 2 ) ? "};\n" : ",") );
		}

		outfile.write("\t };\n");
				
		outfile.write("\t non-fluents {\n");
		for( int i = 0 ; i < numReserv; ++i ){
			double this_rain = rainDist.sample();
			outfile.write("\t\t RAIN(t" + i + ") = " + this_rain + ";\n");
		}
		outfile.write("\n");
		for( int i = 0 ; i < numReserv; ++i ){
			outfile.write("\t\t LEVEL_MIN(t" + i + ") = " + minDist.sample() + ";\n");
		}
		
		outfile.write("\n");
		for( int i = 0 ; i < numReserv; ++i ){
			outfile.write("\t\t LEVEL_MAX(t" + i + ") = " + maxDist.sample() + ";\n");
		}
		
		for( int i = 0 ; i < numReserv-1; ++i ){
			outfile.write("\t\t CONNECT( p" + i + ", t" + i + ", t" + (i+1) + " ) = true;\n" );
		}
		
		outfile.write("\t };\n");
		outfile.write("}\n");
		
		outfile.write("instance reservoir_control_" + numReserv + " { \n");
		outfile.write("\t domain = reservoir_control;\n");
		outfile.write("\t non-fluents = nf_reservoir_control_" + numReserv +";\n" );
		outfile.write("\t horizon = 100;\n" );
		outfile.write("\t discount = 1.0;\n" );
		outfile.write("}");
		outfile.close();
		
		System.out.println( FILE_NAME );
	}
	
	public static void main(String[] args) throws IOException {
		new ReservoirGenerator(50, 5, 0.01, 1, 0.01, 10, 0.01, 42 );
//		new ReservoirGenerator(15, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(20, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(25, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(30, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(35, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(40, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(45, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
//		new ReservoirGenerator(50, 5, 0.01, 1.5, 0.01, 0.8, 0.01, 42 );
		
	}
}
