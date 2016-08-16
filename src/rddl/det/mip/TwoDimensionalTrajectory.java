package rddl.det.mip;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;

import rddl.RDDL.OBJECT_VAL;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.viz.StateViz;

public class TwoDimensionalTrajectory implements StateViz {

	private final static String VIZ_FILE = "trajectory2d.pbm";
	private boolean[][] bw_image;
	private int blowup = 10;
	private Double _maxX;
	private Double _maxY;
	private ArrayList<LCONST> center_point;
	private int prev_row = -1, prev_col = -1;
	
	public TwoDimensionalTrajectory() {
		center_point = new ArrayList<LCONST>();
//		center_point.add( new OBJECT_VAL("p_center") );
	}
	
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
				
				int min_col = (int)(blowup*getInnerMinX( s )) + (int)(blowup*0.1);
				int max_col =  (int)(blowup*getInnerMaxX( s )) - (int)(blowup*0.1);
				int min_row = (int)( blowup*( getMaxY(s)-getInnerMaxY( s ) )) + (int)(blowup*0.1);;
				int max_row = (int)(blowup*(getMaxY(s)-getInnerMinY( s )) ) - (int)(blowup*0.1);;
				for( int row = min_row; row <= max_row; ++row ){
					for( int col = min_col; col <= max_col; ++col ){
						if( row == min_row || row == max_row || col == min_col || col == max_col ){
							bw_image[row][col] = false; //inner black	
						}
					}
				}
			}
			
			int base_row = Math.min( (int)(blowup*getMaxY(s)), Math.max( 0, (int)(blowup*(getMaxY(s) - getY(s))) ) ); 
			int base_col = Math.min( (int)( blowup*getMaxX(s) ), Math.max( 0 , (int)(blowup*getX(s)) ) );
			addPoint( base_row, base_col, prev_row, prev_col );
			prev_row = base_row;
			prev_col = base_col;
			
		}catch( Exception exc ){
			exc.printStackTrace();
			System.exit(1);
		}
	}
	
	private void addPoint( int base_row, int base_col, int prev_row, int prev_col ){
		if( prev_col != -1 && prev_row != -1 ){
			//line
			int x1 = prev_col, y1 = (int) (blowup*_maxY - prev_row), x2 = base_col, y2 = (int) (blowup*_maxY - base_row);
//			System.out.println( "(" + x1 + "," + y1 + ") -> " + "(" + x2 + "," + y2 + ")" );
			
			if( x1 == x2 ){
				for( int next_y = y1; (y1 <= y2) ? (next_y <= y2) : (next_y >= y2); next_y = ((y1 <= y2) ? next_y+1 : next_y-1)  ){
					bw_image[ (int)(blowup*_maxY - next_y) ][ x1 ] = false;
				}
			}else{
				double slope = ((double)(y2 - y1))/(x2 - x1);
				double intercept = (y1 - slope*x1);
 				for( int next_x = x1 ; (x1 <= x2) ? ( next_x <= x2) : (next_x >= x2); 
						next_x = ((x1 <= x2) ? next_x+1 : next_x-1) ){
// 					System.out.println(next_x);
					bw_image[ (int)(blowup*_maxY - (slope*next_x+intercept)) ][ next_x ] = false;
				}				
			}
		}
		
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
		
	}
	
	private double getX(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("x"), center_point );
	}

	private double getY(State s) throws EvalException {
		return (double) s.getPVariableAssign( new PVAR_NAME("y"), center_point );
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
		
		System.out.println("Writing to file " + VIZ_FILE );
		try{
			BufferedWriter writer = new BufferedWriter( new FileWriter(  VIZ_FILE ) );
			writer.write("P1\n");
			writer.write( bw_image[0].length + " " + bw_image.length + "\n" );
			
			for( int row = 0 ; row < bw_image.length; ++row ){
				for( int col = 0; col < bw_image[row].length ; ++col ){
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
	
	 public static void main(String[] args) {
		new TwoDimensionalTrajectory().vizFromClientLog( args[0] );
	}

	private void vizFromClientLog(final String filename) {
		try {
			BufferedReader fw = new BufferedReader( new FileReader(filename) );
			String line;
			double x_center = -1 , y_center = -1;
			int w = -1,h = -1;
			int min_col = -1, max_col = -1, min_row = -1, max_row = -1;
			double max_x = 0, max_y = 0;
			int round_num = 0;
			int inner_max_y = -1, inner_min_y = -1;
			
			while( (line = fw.readLine()) != null ){
				
				if( bw_image == null ){
					if( line.startsWith("OUTER-MAX-X :") ){
						max_x = Double.parseDouble( line.substring( line.indexOf('=') + 1 , line.length()-1 ) );
						w = (int)( blowup* max_x ) + 1;
					}else if( line.startsWith("OUTER-MAX-Y :") ){
						max_y = Double.parseDouble( line.substring( line.indexOf('=') + 1 , line.length()-1 ) );
						h = (int)( blowup* max_y ) + 1;
						_maxY = max_y;
					}else if( line.startsWith("INNER-MIN-X :") ){
						min_col = (int)( blowup* Double.parseDouble( line.substring( line.indexOf('=') + 1 , line.length()-1 ) ) ) + (int)(blowup*0.1);
					}else if( line.startsWith("INNER-MAX-X :") ){
						max_col = (int)( blowup* Double.parseDouble( line.substring( line.indexOf('=') + 1 , line.length()-1 ) ) ) - (int)(blowup*0.1);
					}else if( line.startsWith("INNER-MAX-Y :") ){
						inner_max_y = (int)( blowup* Double.parseDouble( line.substring( line.indexOf('=') + 1 , line.length()-1 ) ) );
					}else if( line.startsWith("INNER-MIN-Y :") ){
						inner_min_y = (int)( blowup* Double.parseDouble( line.substring( line.indexOf('=') + 1 , line.length()-1 ) ) );
					}	
					
					if( h != -1 ){
						min_row = h - inner_max_y;
						max_row = h - inner_min_y;
					}
					 
					if( w != -1 && h != -1 && min_row != -1 && min_col != -1 && max_row != -1 && max_col != -1 ){
						
						bw_image = new boolean[h][w];
						for( int row = 0 ; row < h; ++row ){
							for( int col = 0 ; col < w; ++ col ){
								bw_image[row][col] = true;//bg white
							}
						}
						
						for( int row = min_row; row <= max_row; ++row ){
							for( int col = min_col; col <= max_col; ++col ){
								if( row == min_row || row == max_row || col == min_col || col == max_col ){
									bw_image[row][col] = false; //inner black	
								}
							}
						}
					}
				}else {
				
					if( line.startsWith("- states: x := " ) ){
						x_center = Double.parseDouble( line.substring( 1+line.indexOf("=") ) );
					}else if( line.startsWith("- states: y := " ) ){
						y_center = Double.parseDouble( line.substring( 1+line.indexOf("=") ) );
					}
					
					if( x_center != -1 && y_center != -1 ){
						int base_row = Math.min( (int)(blowup*max_y), Math.max( 0, (int)(blowup*(max_y - y_center) ) ) ); 
						int base_col = Math.min( (int)( blowup*max_x ), Math.max( 0 , (int)(blowup*x_center) ) );
//						System.out.println( base_col + " , " + base_row );
						addPoint( base_row, base_col , prev_row, prev_col );
						prev_row = base_row;
						prev_col = base_col;
						x_center = y_center = -1;
					}
				}
				
				if( line.startsWith(">>> ROUND END, ") ){
					System.out.println(line);
					close();
					System.out.println("Moving " + VIZ_FILE + " to " + filename + "." + round_num + ".pbm" );
					Files.move( FileSystems.getDefault().getPath(VIZ_FILE), 
							FileSystems.getDefault().getPath( filename + "." + round_num + ".pbm" ), StandardCopyOption.REPLACE_EXISTING );
					++round_num;
					bw_image = null;
				}
			}
			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	 
	 
}
