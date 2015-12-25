package rddl.det.mip;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.ToDoubleFunction;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.viz.StateViz;

public class PVarHeatMap implements StateViz {
	
	public class PVarElement {
		
		private final String varName;
		private BufferedWriter bw;
		private PVAR_NAME pvar;
		
		public PVarElement( String pvar_name ){
			this.varName = pvar_name;
			if( !varName.equalsIgnoreCase("reward") ){
				this.pvar = new PVAR_NAME( pvar_name );
			}
			try {
				open_bw();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		protected String extractLine( State s ) throws EvalException{
			if( varName.equalsIgnoreCase("reward") ){
				return s._reward.sample( new HashMap<>(), s, new RandomDataGenerator() ).toString();
			}
			
			return Arrays.toString( s.generateAtoms( pvar ).stream().mapToDouble( new ToDoubleFunction< ArrayList<LCONST> > () {
				public double applyAsDouble( ArrayList<LCONST> value) {
					try {
						return (Double)s.getPVariableAssign(pvar, value);
					} catch (EvalException e) {
						e.printStackTrace();
					}
					return Double.NaN;
				}
			} ).toArray() );
		}
		
		public void doState( State s ){
			try {
				bw.write( extractLine( s ) );
				bw.write("\n"); bw.flush();
			} catch (IOException | EvalException e) {
				e.printStackTrace();
				System.exit(1);
			} 
		}
		
		@Override
		protected void finalize() throws Throwable {
			close_bw();
		}
		
		protected void open_bw() throws IOException{
			bw = new BufferedWriter( new FileWriter( varName + ".viz" ) );
		}
		protected void close_bw() throws IOException{
			bw.flush(); bw.close();
		}

		ArrayList<String> lines = null;
		
		public void processLine(String line) throws IOException {
			
			if( line.contains( "- states: " + varName ) ){
				if( lines == null ){
					lines = new ArrayList<String>();
				}
				lines.add( line.substring( line.indexOf('=') + 1 ) );
			}else{
				if( lines != null ){
					bw.write( Arrays.toString( lines.toArray() ) + "\n" );
					lines.clear();
					lines = null;
				}
			}
		}
		
	}
	
	protected PVarElement[] maps;
	public final static String[] reservoir_tags = { "rlevel", "rain"};
	public final static String[] inventory_tags = { "stock", "order" };
	
	public PVarHeatMap( String[] tags ) {
		maps = new PVarElement[ tags.length ];
		for( int i = 0 ; i < tags.length; ++i ){
			maps[i] = new PVarElement(tags[i]);
		}
	}

	@Override
	public void display(State s, int time) {
		for( final PVarElement p : maps ){
			p.doState(s);
		}
	}
	
	public void vizFromFile( final String filename ){
		try{
			BufferedReader bw = new BufferedReader( new FileReader( filename ) );
			String line;
			while( (line = bw.readLine() ) != null ){
				for( final PVarElement m : maps ){
					m.processLine( line );
				}
			}
			
			for( final PVarElement m : maps ){
				m.close_bw();
			}
			
			bw.close();
			
			for( final PVarElement m : maps ){
				Files.move( FileSystems.getDefault().getPath( m.varName + ".viz" ), 
						FileSystems.getDefault().getPath( filename + "." + m.varName + ".viz" ),
						StandardCopyOption.REPLACE_EXISTING );
			}
					
		}catch( IOException exc ){
			exc.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new PVarHeatMap( Arrays.copyOfRange( args, 1, args.length ) ).vizFromFile( args[0] );
	}

}
