/**
 * Basic Graph Visualization -- code based on examples from Grappa 1.4
 *                              by John Mocenigo
 *                              http://www2.research.att.com/~john/Grappa/
 *
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 11/29/04
 *
 **/

package graph.gviz;

import att.grappa.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;


public class DotViewerApplet extends JApplet {

    // Not currently used
    public int width    = 600;
    public int height   = 400;

    public Graph graph    = null;
    public GrappaPanel gp = null;

    private Dimension DEFAULT_SIZE = new Dimension( width, height );

    /**
     * @see java.applet.Applet#init().
     */
    public void init(  ) {

		// Get Applet Param
		String filename = getParameter("filename");
		if (filename == null) {
		    Error("Illegal filename");
		    return;
		}
	
		// Get applet dimensions
		Dimension dim = DEFAULT_SIZE;
		try {
			int w = Integer.parseInt(getParameter("width"));
			int h = Integer.parseInt(getParameter("height"));
			dim = new Dimension(w, h);
			
		} catch (Exception e) { }
		
		// Setup input stream
        InputStream in;
        try {
            File ff = new File(filename);
            in = new FileInputStream(ff);
        }
        catch (Exception ignore) {
            try {
            	URL url = new URL(filename);
                in = url.openStream();
            }
            catch (Exception e) {
                Error("Graph viewer: Failed to open: " + filename + 
                	  "\n" + e);
                return;
            }
        }
        
		// Parse input stream and create graph
        getContentPane(  ).add( getWindow(in) );
        resize( dim );
        repaint();
    }

    public void Error(String msg) {
		System.err.println(msg);
		getContentPane(  ).add( "Center", new JLabel(msg, JLabel.CENTER));
		repaint();
    }

    public Component getWindow(InputStream input) {

	Parser program = new Parser(input,System.err);
	try {
	    program.parse();
	} catch(Exception ex) {
	    System.err.println("Exception: " + ex.getMessage());
	    ex.printStackTrace(System.err);
	    System.exit(1);
	}
	
	graph = null;
	graph = program.getGraph();
	
	graph.setEditable(true);
	graph.setErrorWriter(new PrintWriter(System.err,true));
	
	gp = DotViewer.DotViewerFrame.MakeGrappaPanel(graph, null);
	//gp.setPreferredSize(DEFAULT_SIZE);
	//gp.setBackground(Color.white);
	
	JScrollPane jsp = new JScrollPane();
	jsp.setSize(width,height);
	jsp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
	jsp.getViewport().setBackground(Color.white);
	jsp.setViewportView(gp);
	
	return jsp;
    }

}
