/**
 * RDDL: Graphical Visualisation for Traffic Model 
 * 
 * @author Tan Nguyen (tan1889@gmail.com)
 * @version 6-May-2011
 *
 **/

package rddl.viz.trfctm;

import rddl.viz.StateViz;
import java.awt.*;
import javax.swing.*;

public class TrfGridDisplay extends JPanel {
   
	static final int MAX_CARS_PER_CELL = 200;	// Max cars/cell otherwise = charge & discharge cell

	static final int CELL_WH = 30;				// W x H of cell in px 
	static final int ARROW_WIDTH = 9; 			// arrow width in pixel
	static final Color ARROW_COLOR1 = new Color(0, 240, 255);
	static final Color ARROW_COLOR2 = new Color(0, 190, 255);
	
	static final int CAR_W = 6;					// W of square to draw car
	static final int CAR_H = 5;					// H of square to draw car
	static final int CAR_ROWS_PER_CELL = (CELL_WH -2) / CAR_H;
	static final int CAR_COLS_PER_CELL = (CELL_WH - ARROW_WIDTH -2) / CAR_W;

	private int nRows, nCols;
	public int NRows() { return nRows; }
	public int NCols() { return nCols; }
	
	public enum Direction {UP, DOWN, LEFT, RIGHT};
    public enum Signal{ALL_RED, NORTH_SOUTH, WEST_EAST};

    private Cell[][] cell;
    private Signal[][] signal;
    private JFrame frame; 

    // constructor
    public TrfGridDisplay(int cols, int rows) {
		super();
    	nRows = rows;
    	nCols = cols;
    	cell = new Cell[nCols][nRows];
    	signal = new Signal[nCols][nRows];
    	frame = new JFrame("Traffic Visualisation");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
		frame.setSize((nCols + 2) * CELL_WH, (nRows + 2) * CELL_WH);        
		frame.setContentPane(this);                  
		frame.setVisible(true);    
    }

    
	public class Cell {
		double _nCars, _maxCars;
		Direction _direction;
		int _arrowStyle;

		public Cell(double nCars, double maxCars, 
				Direction direction, int arrowStyle) {
			_nCars = nCars;
			_maxCars = maxCars;
			if (_nCars < 0) _nCars = 0;
			if (_maxCars < 1) _maxCars = 1;
			_direction = direction;
			_arrowStyle = arrowStyle;
		}

		public void setNewValues(double nCars, double maxCars, 
				Direction direction, int arrowStyle) {
			_nCars = nCars;
			_maxCars = maxCars;
			if (_nCars < 0) _nCars = 0;
			if (_maxCars < 1) _maxCars = 1;
			_direction = direction;
			_arrowStyle = arrowStyle;
		}

		public int getNCars() {
			int n = (int) Math.round(_nCars);
			return n;
		}

		public int getMaxCars() {
			return (int) Math.round(_maxCars);
		}

		public Direction getDirection() {
			return _direction;
		}
		
		public int switchArrow() {
			return _arrowStyle;
		}

		public Color fillColor() {			
			// charge & discharge cells
			if (_maxCars > MAX_CARS_PER_CELL) return new Color(0, 180, 0);			
			// other cells -> linear interpolation white -> red
			int newGB = 255 - 255 * getNCars()/getMaxCars();
			if (newGB < 0) newGB = 0;
			if (newGB > 255) newGB = 255;
			return new Color(255, newGB, newGB);
		}		

		public Color arrowColor() {			
			if (_maxCars > MAX_CARS_PER_CELL || _nCars < 1 || _arrowStyle == -1) 
				return fillColor();
			else if (_arrowStyle == 1) return ARROW_COLOR1;
			else return ARROW_COLOR2;
				
		}
	}

	// set values of cell i, j to update viz 
	public void setCell(int i, int j, double nCars, double maxCars, char directionChar, int arrowStyle) {
		
		Direction direction;
		switch (directionChar) {
		case 'R': 
			direction = TrfGridDisplay.Direction.RIGHT;
			break;
		case 'U': 
			direction = TrfGridDisplay.Direction.UP;
			break;
		case 'D': 
			direction = TrfGridDisplay.Direction.DOWN;
			break;
		default: 
			direction = TrfGridDisplay.Direction.LEFT;
		}

		if (cell[i][j] == null)
			cell[i][j] = new Cell(nCars, maxCars, direction, arrowStyle);
		else
			cell[i][j].setNewValues(nCars, maxCars, direction, arrowStyle);
	}

	// set values of signal i, j to update viz 
	public void setSignal(int i, int j, String signalStr) {
		if (signalStr == "@ALL-RED" || signalStr == "@ALL-RED2") 
			signal[i][j] = Signal.ALL_RED;
		else if (signalStr == "@WEST-EAST")
			signal[i][j] = Signal.WEST_EAST;
		else if (signalStr == "@NORTH-SOUTH")
			signal[i][j] = Signal.NORTH_SOUTH;			
	}
	
	
	// draw cell i,j on the window
	public void drawCell(int i, int j, Graphics g) {	
		if (cell[i][j] == null) return;
		
		Direction direction = cell[i][j].getDirection();
		
		// draw the cell
		int x = i * CELL_WH + CELL_WH;
		int y = j * CELL_WH + CELL_WH;
		// fill cell with color
		g.setColor(cell[i][j].fillColor());
		g.fillRect(x, y, CELL_WH, CELL_WH);
		// fill arrow with color
		Polygon arrowPol = getArrowPolygon(i, j, direction);
		g.setColor(cell[i][j].arrowColor());
		g.fillPolygon(arrowPol);
		// fill cell borders
		g.setColor(Color.BLUE);
		g.drawRect(x, y, CELL_WH, CELL_WH);
		// draw cars in the cell
		if (cell[i][j].getMaxCars() < MAX_CARS_PER_CELL) {
			g.setColor(Color.BLUE);
			int nCars = CAR_COLS_PER_CELL * CAR_ROWS_PER_CELL;
			if (nCars > cell[i][j].getNCars()) nCars = cell[i][j].getNCars();
			int carRows = nCars / CAR_COLS_PER_CELL;
			if (nCars % CAR_COLS_PER_CELL >0) carRows++;

			while (nCars > 0) {
				nCars--;				
				int c = nCars % CAR_COLS_PER_CELL;
				int r = nCars / CAR_COLS_PER_CELL;
				int offsetY = (CELL_WH - carRows * CAR_H)/2 + 1;
				int offsetX = ARROW_WIDTH - 1 + (CELL_WH - ARROW_WIDTH - CAR_COLS_PER_CELL*CAR_W) / 2;

				if (direction == Direction.LEFT){
					c = CAR_COLS_PER_CELL - c - 1;
					offsetX = 2 + (CELL_WH - ARROW_WIDTH - CAR_COLS_PER_CELL * CAR_W) / 2;
				} else if (direction == Direction.DOWN){
					int tmp = r; r = c; c = tmp;
					tmp = offsetY; offsetY = offsetX; offsetX=tmp;					
				} else if (direction == Direction.UP){
					int tmp = r; r = CAR_COLS_PER_CELL - c -1; c = tmp;
					tmp = offsetY; offsetY = offsetX - ARROW_WIDTH + 3; offsetX=tmp;					
				}

				if (direction == Direction.RIGHT || direction == Direction.LEFT)  
					g.drawRect(x + offsetX + c * CAR_W + (CAR_W -3)/2, 
							y + offsetY + r * CAR_H + (CAR_H-2)/2, 3, 1);
				else
					g.drawRect(x + offsetX + c * CAR_H + (CAR_H -2)/2, 
							y + offsetY + r * CAR_W + (CAR_W-3)/2, 1, 3);
			}
		}
	}
	
	public Polygon getArrowPolygon(int i, int j, Direction direction) {
		int xpoints[] = {0, ARROW_WIDTH, 0};
		int ypoints[] = {0, CELL_WH/2, CELL_WH};		
		if (direction == Direction.UP){
			int[] tmp = xpoints;
			xpoints = ypoints;
			ypoints = tmp;
			for (int k=0; k<3; k++)
				ypoints[k] = CELL_WH - ypoints[k];
			
		} else if (direction == Direction.DOWN){
			int[] tmp = xpoints;
			xpoints = ypoints;
			ypoints = tmp;
		} else if (direction == Direction.LEFT){
			for (int k=0; k<3; k++)
				xpoints[k] = CELL_WH - xpoints[k];
		}
		
		for (int k=0; k<3; k++) {
			xpoints[k] = i * CELL_WH + CELL_WH + xpoints[k];
			ypoints[k] = j * CELL_WH + CELL_WH + ypoints[k];
		}
		
		return new Polygon(xpoints, ypoints, 3);		
	}
	
	// draw signal i,j on the window
	public void drawSignal(int i, int j, Graphics g) {	
		if (signal[i][j] == null) return;

		int x = i * CELL_WH + CELL_WH;
		int y = j * CELL_WH + CELL_WH;
		int ms = 5; // space from square margin to symbol

		// fill cell with color
		g.setColor(Color.GRAY);
		g.fillRect(x, y, CELL_WH, CELL_WH);

		// draw cell borders
		g.setColor(Color.BLUE);
		g.drawRect(x, y, CELL_WH, CELL_WH);

		// draw the signal symbol
		if (signal[i][j] == Signal.ALL_RED) {
			g.setColor(Color.YELLOW);
			g.fillOval(x + ms, y + ms, CELL_WH - 2*ms, CELL_WH - 2*ms);
		} else if (signal[i][j] == Signal.WEST_EAST) {
			int xpoints[] = {x + ms, x + CELL_WH/2, x + CELL_WH/2, x + CELL_WH - ms, x + CELL_WH/2, x + CELL_WH/2, x + ms};
			int ypoints[] = {y + CELL_WH/3, y + CELL_WH/3, y + ms, y + CELL_WH/2, y + CELL_WH - ms, y + 2*CELL_WH/3, y + 2*CELL_WH/3};		
			g.setColor(Color.GREEN);
			g.fillPolygon(new Polygon(xpoints, ypoints, 7));
			
		} else if (signal[i][j] == Signal.NORTH_SOUTH) {
			int xpoints[] = {x + CELL_WH/3, x + CELL_WH/3, x + ms, x + CELL_WH/2, x + CELL_WH - ms, x + 2*CELL_WH/3, x + 2*CELL_WH/3};
			int ypoints[] = {y + ms, y + CELL_WH/2, y + CELL_WH/2, y + CELL_WH - ms, y + CELL_WH/2, y + CELL_WH/2, y + ms};
			g.setColor(Color.GREEN);
			g.fillPolygon(new Polygon(xpoints, ypoints, 7));			
		}		

	}
		
	
	public void paintComponent(Graphics g){        
		for (int i=0; i < nCols; i++) {
			for (int j=0; j < nRows; j++) {
				if (cell[i][j] != null) drawCell(i, j, g);
				if (signal[i][j] != null) drawSignal(i, j, g);
			}
		}
	}    

	public void close() {
    	frame.dispose();
    }

	// some tests
    public static void main(String[] args) {
		TrfGridDisplay panel = new TrfGridDisplay(20, 15);

    	panel.setCell(0, 9, 9100, 9999, 'R', 1);
    	panel.setCell(1, 9, 30, 30, 'R', 0);
    	panel.setCell(2, 9, 7, 20, 'R', 1);
    	panel.setCell(3, 9, 2, 20, 'R', 0);
    	panel.setCell(3, 10, 20, 20, 'L', 1);
    	panel.setCell(4, 8, 5, 20, 'U', 0);
    	panel.setCell(4, 7, 30, 30, 'U', 1);
    	panel.setCell(4, 10, 10, 20, 'D', 0);
    	panel.setSignal(5, 5, "@ALL-RED");
    	panel.setSignal(6, 6, "@WEST-EAST");
    	panel.setSignal(7, 7, "@NORTH-SOUTH");

    }
}
