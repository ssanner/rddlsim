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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import att.grappa.Graph;
import att.grappa.GrappaSupport;

import util.WinUNIX;

public class GraphFormat {

    // Note 1: To redirect output from a Java Process use CMD on Widnows and /bin/sh on UNIX
	
	// Note 2: If providing direct I/O to Process, make sure to close input stream when done,
	//         otherwise process never exits!
    
    public static boolean FormatGraphRedirectIO(String file_in, String file_out) {
    	try {
     		
    		System.out.println("Executing: '" + WinUNIX.GVIZ_CMD + " " + file_in + " > " + file_out + WinUNIX.GVIZ_CMD_CLOSE + "'");
			Process p = Runtime.getRuntime().exec(WinUNIX.GVIZ_CMD + " " + file_in + " > " + file_out + WinUNIX.GVIZ_CMD_CLOSE);
			System.out.print("Waiting for graphviz to finish... ");
			p.waitFor();
			System.out.println("done.");
			
		    return true;
    		
    	} catch (InterruptedException ie) {
    		System.out.println(ie);
    		return false;
    	} catch (IOException ioe) {
    		System.out.println(ioe);
    		return false;
    	}
    }
    
    // Make sure to close input stream so process exits!  Could use readLine()
    // in place of byte buffer but need to make sure final output is a newline!
    public static boolean FormatGraphStreams(String file_in, String file_out) {
    	try {
    		
    		byte[] buffer = new byte[65536];
    		
    		// Open files for reading and writing
    		FileInputStream  fis = new FileInputStream(file_in);
    		FileOutputStream fos = new FileOutputStream(file_out);
    		BufferedReader fis_reader = new BufferedReader(new InputStreamReader(fis));
    		PrintWriter    fos_writer = new PrintWriter(fos, true);
    		
			Process p = Runtime.getRuntime().exec(WinUNIX.GVIZ_EXE);
			InputStream process_out = p.getInputStream();
			PrintWriter process_in = new PrintWriter(p.getOutputStream(), true);
    					
			// Provide input to process (could come from any stream)
			String line = null;
			while ((line = fis_reader.readLine()) != null) {
				process_in.println(line);
			}
			fis_reader.close();
			process_in.close(); // Need to close input stream so process exits!!!
			
			// Get output from process (can also be used by BufferedReader to get
			// line-by-line... see how fis_reader is constructed).
			int bytes_read = -1;
			while ((bytes_read = process_out.read(buffer)) >= 0) {
				String temp_str = new String(buffer, 0, bytes_read);
				fos_writer.print(temp_str);
				System.out.print(temp_str);
			}
			process_out.close();
			fos_writer.close();
			
			System.out.print("Waiting for graphviz to finish... ");
			p.waitFor();
			System.out.println("done.");
					    
		    return true;
    		
    	} catch (InterruptedException ie) {
    		System.out.println(ie);
    		return false;
    	} catch (IOException ioe) {
    		System.out.println(ioe);
    		return false;
    	}
    }
    
    // Note: This is really convoluted... should note how the Applet
    // directly loads the formatted graph and do something similar
    // here. -SPS
    public static void LayoutGraph(Graph g) {

	System.out.print("\nPerforming graph layout... ");
	    Object connector = null;
	    try {
			//System.out.println("Trying local");
	    	if (g.isDirected())
	    		connector = Runtime.getRuntime().exec(WinUNIX.GVIZ_EXE);
	    	else
	    		connector = Runtime.getRuntime().exec(WinUNIX.GVIZ2_EXE);
	    } catch(Exception ex) {
			System.err.println("Exception while setting up Process: " + 
					     ex.getMessage() + 
					   "\nTrying format via URLConnection...");
			connector = null;
	    }
	    if(connector == null) {
		try {
		    //System.out.println("Trying URL");
		    connector = (new URL("http://www.research.att.com/~john/cgi-bin/format-graph")).openConnection();
		    URLConnection urlConn = (URLConnection)connector;
		    urlConn.setDoInput(true);
		    urlConn.setDoOutput(true);
		    urlConn.setUseCaches(false);
		    urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		} catch(Exception ex) {
		    System.err.println("Exception while setting up URLConnection: " + ex.getMessage() + "\nLayout not performed.");
		    connector = null;
		}
	    }
	    if(connector != null) {
			if(!GrappaSupport.filterGraph(g,connector)) {
			    System.err.println("ERROR: somewhere in filterGraph");
			}
		if(connector instanceof Process) {
		    try {
				int code = ((Process)connector).waitFor();
				if(code != 0) {
				    System.err.println("WARNING: proc exit code is: " + code);
				}
		    } catch(InterruptedException ex) {
				System.err.println("Exception while closing down proc: " + ex.getMessage());
				ex.printStackTrace(System.err);
		    }
		}
		connector = null;
	    } else {
			System.out.println("Could not access local or web-based graph layout.");
			System.exit(1);
	    }
	    System.out.println("DONE");
    }

    public static void main(String[] args) {
    	GraphFormat.FormatGraphRedirectIO("ssanner_graph.dot.unformat", "ssanner_graph.dot");
    }
}
