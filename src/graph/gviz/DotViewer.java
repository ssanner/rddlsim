/**
 * Basic Graph Visualization -- code based on examples from Grappa 1.4
 *                              by John Mocenigo
 *                              http://www2.research.att.com/~john/Grappa/
 *
 * Note: autolayout has been disabled so programs must pass the filename
 *       of a pre-formatted file (see Graph.genFormatDotFile(...))
 *
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 11/29/04
 *
 **/

package graph.gviz;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import att.grappa.*;

public abstract class DotViewer
	implements GrappaConstants
{
    public int width    = 600;
    public int height   = 400;
    public int x_offset = 100; 
    public int y_offset = 100;
    public int text_height = 75;

    static {
	// JFrame.setDefaultLookAndFeelDecorated(true);
    }
    
    public DotViewerFrame  frame  = null;

    public DotViewer() { 
	// Initially use defaults for window sizing
    }

    public void setWindowSizing(int w, int h, int x_off, int y_off, int text_h) {
	width = w;
	height = h;
	x_offset = x_off;
	y_offset = y_off;
	text_height = text_h;
    }

    // Must override
    public abstract void nodeClicked(String name);
    
    public void showWindow(String filename) {

	FileInputStream input = null;
	try {
	    input = new FileInputStream(filename);
	} catch(FileNotFoundException fnf) {
	    System.out.println(fnf.toString());
	    System.exit(1);
	}
	//System.out.println("Displaying: '" + filename + "'");
	showWindow(input);
    }

    public void showWindow(InputStream input) {

	Parser program = new Parser(input,System.err);
	try {
	    program.parse();
	} catch(Exception ex) {
	    System.err.println("Exception: " + ex.getMessage());
	    ex.printStackTrace(System.err);
	    System.exit(1);
	}

	Graph graph = null;
	graph = program.getGraph();

	graph.setEditable(true);
	graph.setErrorWriter(new PrintWriter(System.err,true));

	frame = new DotViewerFrame(graph, this, width, height, x_offset, y_offset, text_height);
	frame.show();
	frame.repaint();
    }

    public void displayText(String msg) {
	frame.displayText(msg);
    }

    public static class ZoomAdapter extends GrappaAdapter {

	GrappaPanel gp;
	DotViewerFrame dvf;
	GrappaBox last_outline = null;

	public ZoomAdapter(GrappaPanel panel, DotViewerFrame frame) {
	    gp  = panel;
	    dvf = frame;
	}

	// The method called when a single mouse click occurs on a displayed subgraph.	 
	public void grappaClicked(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, 
				  int clickCount, GrappaPanel panel) {

	    if (clickCount == 2) {
		gp.setScaleToFit(true);
		//System.out.println("Zoom reset");
		gp.resetZoom();
		gp.repaint();
	    }

	    if (elem instanceof Node && dvf != null) {
		String name = ((Node)elem).toString();
		name = name.substring(1,name.length()-1);
		dvf.dv.nodeClicked(name);
	    }
	}

	// The method called when a mouse drag occurs on a displayed subgraph.
	public void grappaDragged(Subgraph subg, GrappaPoint currentPt, int currentModifiers, Element pressedElem, 
				  GrappaPoint pressedPt, int pressedModifiers, GrappaBox outline, GrappaPanel panel) {

	    if((currentModifiers&InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
		if(currentModifiers == InputEvent.BUTTON1_MASK || currentModifiers == (InputEvent.BUTTON1_MASK|InputEvent.CTRL_MASK)) {
		    Graphics2D g2d = (Graphics2D)(panel.getGraphics());
		    AffineTransform orig = g2d.getTransform();
		    g2d.setTransform(panel.getTransform());
		    g2d.setXORMode(Color.darkGray);
		    if(outline != null) {
			g2d.draw(outline);
		    }
		GrappaBox box = GrappaSupport.boxFromCorners(pressedPt.x, pressedPt.y, currentPt.x, currentPt.y);
		g2d.draw(box);
		g2d.setPaintMode();
		g2d.setTransform(orig);
		}
	    }
	    last_outline = outline;
	}

	// The method called when a mouse press occurs on a displayed subgraph.
	public void grappaPressed(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, GrappaPanel panel) {

	}

	// The method called when a mouse release occurs on a displayed subgraph.
	public void grappaReleased(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, Element pressedElem, 
			    GrappaPoint pressedPt, int pressedModifiers, GrappaBox outline, GrappaPanel panel) {
	    //System.out.println("Zoom Outline");
	    //System.out.flush();
	    gp.setScaleToFit(false);
	    gp.setScaleToSize(null);
	    gp.zoomToOutline(outline);
	    gp.repaint();
	}

	// The method called when a element tooltip is needed.
	public String grappaTip(Subgraph subg, Element elem, GrappaPoint pt, int modifiers, GrappaPanel panel) {
	    return null; // "No tip";
	}

    }

    public static class DotViewerFrame extends JFrame implements ActionListener
    {
	DotViewer dv = null;

	JButton layout   = null;
	JTextPane jta    = null;
	GrappaPanel gp   = null;
	GrappaAdapter ga = null;
	Graph graph      = null;

	JButton printer = null;
	JButton redraw = null;
	JButton quit = null;
	JButton zoom_reset = null;
	JButton zoom_two   = null;
	JButton zoom_half  = null;
	JPanel panel = null;
  
	public DotViewerFrame(Graph graph, DotViewer dot_viewer, 
			      int width, int height, int offset_x, int offset_y,
			      int text_height) {
	    super("Graph Viewer");

	    dv = dot_viewer;

	    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    this.graph = graph; 
	    setSize(width,height);
	    setLocation(offset_x,offset_y);

	    JScrollPane jsp = new JScrollPane();
	    jsp.setSize(width,height);
	    //jsp.setMinimumSize(new Dimension(width,height));
	    //jsp.setPreferredSize(new Dimension(width,height));
	    //jsp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
	    jsp.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
	    jsp.getViewport().setBackground(Color.white);

	    gp = MakeGrappaPanel(graph, this);

	    java.awt.Rectangle bbox = graph.getBoundingBox().getBounds();
  
	    GridBagLayout gbl = new GridBagLayout();
	    GridBagConstraints gbc  = new GridBagConstraints();
	    GridBagConstraints gbcr = new GridBagConstraints();

	    gbcr.fill = gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbcr.anchor = gbc.anchor = GridBagConstraints.NORTHWEST;
	    gbcr.gridwidth = GridBagConstraints.REMAINDER;

	    panel = new JPanel();
	    panel.setLayout(gbl);

	    redraw = new JButton("Redraw");
	    gbl.setConstraints(redraw,gbc);
	    panel.add(redraw);
	    redraw.addActionListener(this);

	    zoom_reset = new JButton("Zoom Reset");
	    gbl.setConstraints(zoom_reset,gbc);
	    panel.add(zoom_reset);
	    zoom_reset.addActionListener(this);

	    zoom_two = new JButton("Zoom 2x");
	    gbl.setConstraints(zoom_two,gbc);
	    panel.add(zoom_two);
	    zoom_two.addActionListener(this);

	    zoom_half = new JButton("Zoom 1/2x");
	    gbl.setConstraints(zoom_half,gbc);
	    panel.add(zoom_half);
	    zoom_half.addActionListener(this);

	    printer = new JButton("Save");
	    gbl.setConstraints(printer,gbc);
	    panel.add(printer);
	    printer.addActionListener(this);

	    quit = new JButton("Close");
	    gbl.setConstraints(quit,gbc);
	    panel.add(quit);
	    quit.addActionListener(this);

	    JPanel empty = new JPanel();
	    gbl.setConstraints(empty,gbcr);
	    panel.add(empty);

	    jta = new JTextPane();
	    if (text_height > 0) {
		jta.setText("Click on a node for more information.");
		Dimension jta_dim = new Dimension(width, text_height);
		jta.setSize(width, text_height);
		jta.setPreferredSize(jta_dim);
		jta.setMinimumSize(jta_dim);
		//jta.setRows(5);
		jta.setEditable(false);
		jta.setBackground(Color.white);
		gbl.setConstraints(jta,gbcr);
		panel.add(jta);
		//jta.addActionListener(this);
	    }

	    //GridBagLayout gbl2 = new GridBagLayout();
	    //GridBagConstraints gbc2 = new GridBagConstraints();
	    //gbc2.fill = GridBagConstraints.VERTICAL;
	    //gbc2.anchor = GridBagConstraints.NORTHWEST;
	    //gbc2.gridwidth = GridBagConstraints.REMAINDER;

	    //JPanel p = new JPanel();
	    //p.setLayout(gbl2);
	    //gbl2.setConstraints(panel, gbc2);
	    //p.add(panel);
	    //gbl2.setConstraints(jsp, gbc2);
	    //p.add(jsp);

	    //getContentPane().add("Center", p);

	    getContentPane().add("North", panel);
	    getContentPane().add("Center", jsp);

	    //autoLayout();
	    setVisible(true);
	    jsp.setViewportView(gp);
	}

	public static GrappaPanel MakeGrappaPanel(Graph graph, DotViewerFrame dvf) {
	    GrappaPanel gp = new GrappaPanel(graph);
	    GrappaAdapter ga = new ZoomAdapter(gp, dvf);
	    gp.addGrappaListener(ga);
	    gp.setScaleToFit(true);
	    return gp;
	}

	public void displayText(String msg) {
	    jta.setText(msg);
	}

	public void actionPerformed(ActionEvent evt) {
	    if(evt.getSource() instanceof JButton) {
		JButton tgt = (JButton)evt.getSource();
		if(tgt == quit) {
		    setVisible(false);
		    dispose();
		} else if(tgt == printer) {
		    try {
			FileDialog fd = new FileDialog(this,"Save file as ...",FileDialog.SAVE);
			fd.show();
			String filename = fd.getFile();
			String dir = fd.getDirectory();
			if (filename != null && dir != null) {
			    PrintStream ps = new PrintStream(new FileOutputStream(dir + filename));
			    graph.printGraph(ps);
			    ps.close();
			}
		    } catch (FileNotFoundException ex) {
			System.err.println("File system error... could not write file.");
		    }
		    System.out.flush();
		} else if(tgt == redraw) {
		    gp.repaint();
		} else if(tgt == zoom_reset) {
		    gp.setScaleToFit(true);
		    //System.out.println("Zoom reset");
		    gp.resetZoom();
		    gp.repaint();
		} else if(tgt == zoom_half) {
		    //System.out.println("Zoom 1/2x");
		    //System.out.flush();
		    gp.setScaleToFit(false);
		    gp.setScaleToSize(null);
		    gp.multiplyScaleFactor(0.5d);
		    gp.repaint();
		} else if(tgt == zoom_two) {
		    //System.out.println("Zoom 2x");	
		    //System.out.flush();
		    gp.setScaleToFit(false);
		    gp.setScaleToSize(null);
		    gp.multiplyScaleFactor(2d);
		    gp.repaint();
		}  else {
		    System.out.println("Unhandled button: " + evt);
		}
	    }
	}

	public void autoLayout() {
		GraphFormat.LayoutGraph(graph); 
	    gp.repaint();
	}
    }
}



