/** Document implementation for TREC Interactive track data (TREC Disks 4-5)
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package rddl.evaluate;

import java.io.*;
import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.MapList;


public class LogReader {

	public static final boolean ALLOW_MULTIPLE_CLIENT_NAMES = true;
	
	public XPath _xpath = XPathFactory.newInstance().newXPath();
	public Document _doc = null;
	public HashMap<String,MapList> _client2data = null;
	
	public LogReader(File f) {
		
		File f2 = new File(f.toString() + ".clean");
		if (!f2.exists()) {
			CleanFile(f, f2);
		}
		
		_client2data = new HashMap<String,MapList>();
 
		/**
         * Parse the input url
         */
		try {
	        DOMParser parser = new DOMParser();
	        InputStream byteStream = new FileInputStream(f2);
	        parser.parse(new org.xml.sax.InputSource(byteStream));
	        _doc = parser.getDocument();
		} catch (Exception e) {
			System.out.println(e);
		}

        //PrintNode(_doc, "", 0);

//        NodeList nodes = (NodeList)XPathQuery(_doc, "//client-name", XPathConstants.NODESET);
//        _sClientName = nodes.item(0).getFirstChild().getNodeValue();
//		if (!ALLOW_MULTIPLE_CLIENT_NAMES) {
//	        // Verify that only a single client name was used
//	        for (int i = 1; i < nodes.getLength(); i++) {
//	        	String node_name = nodes.item(i).getFirstChild().getNodeValue();
//	        	if (!_sClientName.equals(node_name)) {
//	        		System.out.println("\n\n*** LOG ERROR [" + f + "]: " + _sClientName + " != " + node_name);
//	        		System.exit(1);
//	        	}
//	        }
//		}
        
		//NodeList nodes = (NodeList)XPathQuery(_doc, "//round-end", XPathConstants.NODESET);		
		//System.out.println("\n\nQuery result: " + result.getClass());
		NodeList nodes = _doc.getChildNodes().item(0).getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {

			Node n = nodes.item(i);
			if (!n.getNodeName().equals("round-end"))
				continue;
			
			// Note: use . for the current node: nodes.item(i)
			String client_name = null; //(String)XPathQuery(n, ".//client-name", XPathConstants.STRING);		
			String instance_name = null; //(String)XPathQuery(n, ".//instance-name", XPathConstants.STRING);		
			double reward = Double.NaN; //(Double)XPathQuery(n, ".//round-reward", XPathConstants.NUMBER);
			long time_used = -1;
			
			NodeList children = n.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node c = children.item(j);
				
				if (c.getNodeName().equals("client-name")) {
					client_name = c.getFirstChild().getNodeValue();		
				}
				if (c.getNodeName().equals("instance-name")) {
					instance_name = c.getFirstChild().getNodeValue(); 
				}
				if (c.getNodeName().equals("round-reward")) {
					//PrintNode(c.getFirstChild(), "", 0);
					//System.out.println("C:" + c.getFirstChild().getNodeValue());
					reward = new Double(c.getFirstChild().getNodeValue());
				}
				if (c.getNodeName().equals("time-used")) {
					time_used = new Long(c.getFirstChild().getNodeValue());
				}
			}

			if (client_name == null) {
				System.err.println("Client name null... skipping");
				PrintNode(n, "", 0);
				continue;
				//System.exit(1);
			}
			
			//System.out.println(_sClientName + ": " + instance_name + " -> " + reward);
			MapList ml = _client2data.get(client_name);
			if (ml == null) {
				ml = new MapList();
				_client2data.put(client_name, ml);
			}
			ml.putValue(instance_name, reward);
			ml.putValue(instance_name + "__trial_time", time_used);
		}
		
		//System.out.println(_client2data);
	}
	
	public static void CleanFile(File f, File f2) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			PrintStream ps = new PrintStream(new FileOutputStream(f2));
			String line = br.readLine(); // Discard first line
			ps.println("<root>");
			while ((line = br.readLine()) != null) {
				if (line.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
					continue;
				ps.println(line);
			}
			ps.println("</root>");
			br.close();
			ps.close();
		} catch (Exception e) {
			System.out.println("Cannot process file: '" + f + "'\n" + e);
			System.exit(1);
		}
	}

	/**
     * Creates a string buffer of spaces
     * @param depth the number of spaces
     * @return string of spaces
     */
    public static StringBuffer Pad(int depth) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < depth; i++)
			sb.append("  ");
		return sb;
	}

    /**
     * Print the DOM tree on stdout
     * @param n root node of a document
     * @param prefix
     * @param depth
     */
	public static void PrintNode(Node n, String prefix, int depth) {
		
		try {			
			System.out.print("\n" + Pad(depth) + "[" + n.getNodeName());
			NamedNodeMap m = n.getAttributes();
			for (int i = 0; m != null && i < m.getLength(); i++) {
				Node item = m.item(i);
				System.out.print(" " + item.getNodeName() + "=" + item.getNodeValue());
			}
			System.out.print("] ");
			
			NodeList cn = n.getChildNodes();
			
			for (int i = 0; cn != null && i < cn.getLength(); i++) {
				Node item = cn.item(i);
				if (item.getNodeType() == Node.TEXT_NODE) {
					String val = item.getNodeValue().trim();
					if (val.length() > 0) System.out.print(" \"" + item.getNodeValue().trim() + "\"");
				} else
					PrintNode(item, prefix, depth+2);
			}
		} catch (Exception e) {
			System.out.println(Pad(depth) + "Exception e: ");
		}
	}
	
	public Object XPathQuery(Node doc, String query, QName query_type) {
		
		try {
			XPathExpression xPathExpression = _xpath.compile(query);
			return xPathExpression.evaluate(doc, query_type);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.err.println("Must specify log file to read.");
			System.exit(1);
		}
		
		LogReader d = new LogReader(new File(args[0]));
		System.out.println(d._client2data);
	}
}
