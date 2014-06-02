/**
 * RDDL: Simple Block Graphics Display for Visualization
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 3/16/05
 *
 **/

package rddl.viz;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

/**
 * A simple graphics demonstration.
 */
public class BlockDisplay extends JPanel implements ActionListener {

	// //////////////////////////////////////////////////////////////////////////////
	// Class definition
	// //////////////////////////////////////////////////////////////////////////////

	// Potential colors
	public Color[] _colors = new Color[] { Color.cyan, Color.pink,
			Color.orange, Color.yellow, Color.magenta, Color.blue, Color.green,
			Color.black, Color.gray, Color.darkGray, Color.white, Color.red };

	private static final int _nBlockSize = 15;
	
	public float FONT_SIZE = 16.0f;
	public int   LINE_THICKNESS = 2;

	private String _title;
	private int _nRows;
	private int _nCols;
	private Font _font;
	private JFrame _frame;
	private JLabel _status;
	private String _msg;
	private Cell[][] _grid;
	private ArrayList _lines;
	private ArrayList _circles;
	private ArrayList _text;
	
	// Constructor
	public BlockDisplay(String title, String status, int rows, int cols) {

		// Initialize local members
		_title = title;
		_nRows = rows;
		_nCols = cols;
		_grid = new Cell[rows][];
		_font = new Font("System", Font.BOLD, 16);
		_lines = new ArrayList();
		_circles = new ArrayList();
		_text    = new ArrayList();
		_msg = status;
		for (int i = 0; i < rows; i++) {
			_grid[i] = new Cell[cols];
			for (int j = 0; j < cols; j++) {
				_grid[i][j] = null;
			}
		}

		// Set subpanels
		setPreferredSize(new Dimension(/* width - cols */(cols + 2)
				* _nBlockSize,
		/* height - rows */(int) ((rows + 2) * _nBlockSize)));
		_status = new JLabel(_msg, JLabel.CENTER);
		JPanel subpanel = new JPanel();
		subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
		subpanel.add(_status);

		// setBackground(Color.white);
		// _status.setBackground(Color.white);
		_status.setPreferredSize(new Dimension(/* width - cols */(cols + 1)
				* _nBlockSize,
		/* height - rows */(int) ((2.5) * _nBlockSize)));
		Border bevel_border = BorderFactory
				.createEtchedBorder(EtchedBorder.LOWERED);
		Border empty_border1 = BorderFactory.createEmptyBorder(8, 8, 8, 8);
		Border empty_border2 = BorderFactory.createEmptyBorder(0, 8, 8, 8);
		Border compound1 = /* BorderFactory.createCompoundBorder( */empty_border1/*
																				 * ,
																				 * bevel_border
																				 * )
																				 */;
		Border compound2 = BorderFactory.createCompoundBorder(empty_border2,
				bevel_border);
		_status.setBorder(compound1);
		setBorder(compound2); // Can have title as well

		// Initialize window frame
		JFrame.setDefaultLookAndFeelDecorated(true);
		_frame = new JFrame(title);
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container content_pane = _frame.getContentPane();
		content_pane.setLayout(new BoxLayout(content_pane, BoxLayout.Y_AXIS));
		content_pane.add(subpanel); // was CENTER
		// content_pane.add(_status); // was CENTER
		// content_pane.add(Box.createRigidArea(new Dimension(0,5)));
		content_pane.add(this);
		// _frame.setSize(400,400);
		_frame.pack();
		_frame.show();

		// Initiate keyboard event handling
		registerKeyboardAction(this, "left",
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
				WHEN_IN_FOCUSED_WINDOW);
		registerKeyboardAction(this, "right",
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
				WHEN_IN_FOCUSED_WINDOW);
	}

	public static class Cell {
		Color _color;
		String _text;

		public Cell(Color col /* null = empty */, String text /* null for rect */) {
			_color = col;
			_text = text;
		}
	}

	public static class Line {
		Color _color;
		double _x1, _y1, _x2, _y2;

		public Line(Color col, double x1, double y1, double x2, double y2) {
			_color = col;
			_x1 = x1;
			_y1 = y1;
			_x2 = x2;
			_y2 = y2;
		}
	}

	public static class Circle {
		Color _color;
		double _x, _y, _radius;
		boolean _fill;

		public Circle(Color col, double x, double y, double r, boolean fill) {
			_color = col;
			_x = x;
			_y = y;
			_radius = r;
			_fill = fill;
		}
	}

	public static class Text {
		Color _color;
		double _x, _y;
		String _text;

		public Text(Color col, double x, double y, String text) {
			_color = col;
			_x = x;
			_y = y;
			_text = text;
		}
	}

	public void clearAllLines() {
		_lines.clear();
	}

	public void addLine(Color col, double x1, double y1, double x2, double y2) {
		_lines.add(new Line(col, x1, y1, x2, y2));
	}

	public void clearAllCircles() {
		_circles.clear();
	}

	public void addCircle(Color col, double x, double y, double r) {
		_circles.add(new Circle(col, x, y, r, false));
	}

	public void addFillCircle(Color col, double x, double y, double r) {
		_circles.add(new Circle(col, x, y, r, true));
	}

	public void clearAllText() {
		_text.clear();
	}

	public void addText(Color col, double x, double y, String text) {
		_text.add(new Text(col, x, y, text));
	}

	public void clearAllCells() {
		// Draw the currently active rectangles
		for (int i = 0; i < _nRows; i++) {
			for (int j = 0; j < _nCols; j++) {
				_grid[i][j] = null;
			}
		}
	}

	public void setCell(int i, int j, Color col /* null to unset */, String text) {
		if (i < _nRows && j < _nCols) {
			_grid[i][j] = new Cell(col, text);
		} else {
			System.err.println("Cell out of range: <" + i + "," + j + "> / "
					+ "<" + _nRows + "," + _nCols + ">");
		}
	}

	public void setMessage(String status) {
		_msg = status;
	}

	private void drawCell(Graphics g, int row, int col, Cell c) {
		if (_grid[row][col]._color == null) {
			return;
		}
		g.setColor(c._color);
		if (c._text == null) {
			g.draw3DRect((col + 1) * _nBlockSize, (row + 1) * _nBlockSize,
					_nBlockSize - 1, _nBlockSize - 1, true);
			g.fill3DRect((col + 1) * _nBlockSize, (row + 1) * _nBlockSize,
					_nBlockSize - 1, _nBlockSize - 1, true);
		} else {
			g.setFont(_font);
			g.drawChars(c._text.toCharArray(), 0, c._text.length(), (col + 1)
					* _nBlockSize + 2, (row + 2) * _nBlockSize - 2);
		}
	}

	private void drawLine(Graphics g, Line l) {

		g.setColor(l._color);
		g.drawLine((int)Math.floor((l._x1 + 1) * _nBlockSize), (int)Math.floor((l._y1 + 1) * _nBlockSize),
				(int)Math.floor((l._x2 + 1) * _nBlockSize), (int)Math.floor((l._y2 + 1) * _nBlockSize));
	}

	private void drawCircle(Graphics g, Circle c) {

		// (x,y)
		//  |
		//  |  y
		// \ / +
		//
		//    ------>
		//       x +
		//
		// (x,y) specify upper-left corner of square with height/width 2*r
		// so to get center at (x,y), upper left corner is (x-r,y-r).
		g.setColor(c._color);
		int r = (int)Math.floor((c._radius) * _nBlockSize);
		int x = (int)Math.floor((c._x + 1) * _nBlockSize) - r;
		int y = (int)Math.floor((c._y + 1) * _nBlockSize) - r;
		int diameter = 2*r; 
		if (c._fill)
			g.fillArc(x, y, diameter, diameter, 0, 360);
		else
			g.drawArc(x, y, diameter, diameter, 0, 360);
	}

	private void drawText(Graphics g, Text t) {

		g.setColor(t._color);
		int x = (int)Math.floor((t._x + 1) * _nBlockSize);
		int y = (int)Math.floor((t._y + 1) * _nBlockSize);
		g.drawString(t._text, x, y);
	}
	
	// Paint function
	public void paint(Graphics g) {
		super.paint(g);

	    Graphics2D g2 = (Graphics2D) g;
	    g2.setStroke(new BasicStroke(LINE_THICKNESS));
	    Font f = getFont();
	    g2.setFont(f.deriveFont(FONT_SIZE));
		
		// Show msg
		_status.setText(_msg);

		// Draw the currently active rectangles
		for (int i = 0; i < _nRows; i++) {
			for (int j = 0; j < _nCols; j++) {
				if (_grid[i][j] != null) {
					drawCell(g, i, j, _grid[i][j]);
				}
			}
		}

		// Draw all lines
		for (int i = 0; i < _lines.size(); i++) {
			drawLine(g, (Line) _lines.get(i));
		}
		
		// Draw all circles
		for (int i = 0; i < _circles.size(); i++) {
			drawCircle(g, (Circle) _circles.get(i));
		}
		
		// Draw all text
		for (int i = 0; i < _text.size(); i++) {
			drawText(g, (Text) _text.get(i));
		}
}

	// Close function
	public void close() {
		_frame.dispose();
	}

	// Keyboard response
	public void actionPerformed(ActionEvent e) {
		// if ("left".equals(e.getActionCommand()))
		repaint();
	}

	// //////////////////////////////////////////////////////////////////////////////
	// A simple test routine
	// //////////////////////////////////////////////////////////////////////////////
	public static void main(String[] args) {

		try {

			// Initialize the frame
			BlockDisplay bd = new BlockDisplay("Test Display", "No Message",
					30, 20);

			// Draw a block of cells
			bd.clearAllCells();
			bd.setCell(10, 10, Color.blue, null);
			bd.setCell(10, 15, Color.red, null);
			bd.setCell(15, 10, Color.yellow, null);
			bd.setCell(20, 15, Color.green, null);
			bd.setCell(20, 20, Color.magenta, null);
			bd.repaint();

			// Sleep
			Thread.currentThread().sleep(1000);

			// Draw a different block of cells
			bd.clearAllCells();
			bd.addLine(Color.black, 6, 6, 6, 20);
			bd.addLine(Color.black, 6, 6, 20, 6);
			bd.setCell(0, 0, Color.blue, null);
			bd.setCell(1, 13, Color.red, null);
			bd.setCell(13, 0, Color.yellow, null);
			bd.setCell(17, 19, Color.green, null);
			bd.setCell(20, 20, Color.magenta, null);
			bd.repaint();

			// Sleep
			Thread.currentThread().sleep(1000);

			// Draw a different block of cells
			bd.clearAllLines();
			bd.addLine(Color.blue, 6, 6, 20, 20);
			bd.addLine(Color.red, 10, 10, 10, 20);
			bd.addLine(Color.red, 10, 10, 20, 10);
			bd.setCell(0, 0, Color.orange, null);
			bd.setCell(10, 10, Color.blue, null);
			bd.setCell(10, 15, Color.red, null);
			bd.setCell(15, 10, Color.yellow, "A");
			bd.setCell(29, 19, Color.green, "B");
			bd.setCell(30, 20, Color.magenta, "C");
			bd.setMessage("A Longer Message on the Final Screen");
			bd.repaint();

			// Sleep
			Thread.currentThread().sleep(1000);

			// Kill window
			// bd.close();

		} catch (InterruptedException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
}
