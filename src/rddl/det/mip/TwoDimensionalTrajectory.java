package rddl.det.mip;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import rddl.EvalException;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.viz.StateViz;

public class TwoDimensionalTrajectory implements StateViz {

	private final static String VIZ_FILE = "trajectory2d.pbm";
	private boolean[][] bw_image;
	private int blowup = 10;
	private Double _maxX;
	private Double _maxY;
	
	@Override
	public void display(State s, int time) {
		try{
			
			if( bw_image == null ){
				int w = (int)( blowup*getMaxX( s ) ) + 1;
				int h = (int)( blowup*getMaxY( s ) ) + 1;
				bw_image = new boolean[h][w];
				for( int row = 0 ; row < h; ++row ){
					for( int col = 0 ; col < w; ++ col ){
						bw_image[row][col] = true;//bg white
					}
				}
				
				int min_col = (int)(blowup*getInnerMinX( s ));
				int max_col =  (int)(blowup*getInnerMaxX( s ));
				int min_row = (int)( blowup*( getMaxY(s)-getInnerMaxY( s ) ));
				int max_row = (int)(blowup*(getMaxY(s)-getInnerMinY( s )) );
				for( int row = min_row; row <= max_row; ++row ){
					for( int col = min_col; col <= max_col; ++col ){
						bw_image[row][col] = false; //inner black
					}
				}
			}
			
			int base_row = Math.min( (int)(blowup*getMaxY(s)), Math.max( 0, (int)(blowup*(getMaxY(s) - getY(s))) ) ); 
			int base_col = Math.min( (int)( blowup*getMaxX(s) ), Math.max( 0 , (int)(blowup*getX(s)) ) );
			
			bw_image[ base_row ][ base_col ] = false;
			//add more points 
			try{
				bw_image[ base_row + 1 ][ base_col ] = false;
			}catch( ArrayIndexOutOfBoundsException exc ){
			}
			
			try{
				bw_image[ base_row - 1 ][ base_col ] = false;
			}catch( ArrayIndexOutOfBoundsException exc ){
			}
			
			try{
				bw_image[ base_row ][ base_col + 1 ] = false;
			}catch( ArrayIndexOutOfBoundsException exc ){
			}
			
			try{
				bw_image[ base_row ][ base_col - 1 ] = false;
			}catch( ArrayIndexOutOfBoundsException exc ){
			}
			
		}catch( Exception exc ){
			exc.printStackTrace();
			System.exit(1);
		}
	}
	
	private double getX(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("x"), new ArrayList<>() );
	}

	private double getY(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("y"), new ArrayList<>() );
	}

	private double getInnerMinY(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("INNER-MIN-Y"), new ArrayList<>() );
	}

	private double getInnerMaxY(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("INNER-MAX-Y"), new ArrayList<>() );
	}

	private double getInnerMaxX(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("INNER-MAX-X"), new ArrayList<>() );
	}

	private double getInnerMinX(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("INNER-MIN-X"), new ArrayList<>() );
	}

	private double getMaxY(State s) throws EvalException {
		if ( _maxY == null ){
			_maxY = (double) s.getPVariableAssign( new PVAR_NAME("OUTER-MAX-Y"), new ArrayList<>() );
		}
		return _maxY;
	}

	private double getMaxX( State s ) throws EvalException {
		if( _maxX == null ){
			_maxX = (double) s.getPVariableAssign( new PVAR_NAME("OUTER-MAX-X"), new ArrayList<>() );
		}
		return _maxX;
	}

	@Override
	public void close() {
		
		System.out.println("Writing to file" );
		try{
			BufferedWriter writer = new BufferedWriter( new FileWriter(  VIZ_FILE ) );
			writer.write("P1\n");
			writer.write( (int)(blowup*getMaxX( null ) + 1) + " " + (int)(blowup*getMaxY( null ) + 1) + "\n" );
			
			for( int row = 0 ; row < (int)( blowup*getMaxY( null ) ) + 1; ++row ){
				for( int col = 0; col < (int)( blowup*getMaxX( null ) ) + 1 ; ++col ){
					writer.write( ( bw_image[ row ][ col ] ? "0" : "1" ) + " "  );
				}
				writer.write("\n");
			}
			writer.flush();
			writer.close();
		}catch( Exception exc ){
			exc.printStackTrace();
			System.exit(1);
		}
	}

}
