/**
 * RDDL: Main client code for interaction with RDDLSim server
 * 
 * @author Sungwook Yoon (sungwook.yoon@gmail.com)
 * @version 10/1/10
 *
 **/

package rddl.competition;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import rddl.EvalException;
import rddl.RDDL;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.LCONST;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.parser.parser;
import rddl.viz.StateViz;
/** The SocketClient class is a simple example of a TCP/IP Socket Client.
 *
 */

public class Client {
	
	public static final boolean SHOW_MEMORY_USAGE = true;
	public static final Runtime RUNTIME = Runtime.getRuntime();
	private static DecimalFormat _df = new DecimalFormat("0.##");
	
	private static int PORT_NUMBER = 2323;
	
	enum XMLType {
		ROUND,TURN,ROUND_END,END_TEST,NONAME
	}
	
	public static void main(String[] args) {
		RDDL rddl;
		/** Define a host server */
		String host = "localhost";
		/** Define a port */
		int port = PORT_NUMBER;

		State      state;
		INSTANCE   instance;
		NONFLUENTS nonFluents = null;
		DOMAIN     domain;
		StateViz   stateViz;
		
		StringBuffer instr = new StringBuffer();
		String TimeStamp;
		System.out.println("RDDL client initialized");

		try {
			rddl = parser.parse(new File("files/rddl/test/sysadmin.rddl"));
			state = new State();
			// just pick the first
			String problemInstance = rddl._tmInstanceNodes.firstKey();
			instance = rddl._tmInstanceNodes.get(problemInstance);
			if (instance._sNonFluents != null) {
				nonFluents = rddl._tmNonFluentNodes.get(instance._sNonFluents);
			}
			domain = rddl._tmDomainNodes.get(instance._sDomain);
			if (nonFluents != null && !instance._sDomain.equals(nonFluents._sDomain)) {
				try {
					throw new Exception("Domain name of instance and fluents do not match: " + 
							instance._sDomain + " vs. " + nonFluents._sDomain);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			state.init(nonFluents != null ? nonFluents._hmObjects : null, instance._hmObjects,
					domain._hmTypes, domain._hmPVariables, domain._hmCPF,
					instance._alInitState, nonFluents == null ? null : nonFluents._alNonFluents);
			
			/** Obtain an address object of the server */
			InetAddress address = InetAddress.getByName(host);
			/** Establish a socket connetion */
			Socket connection = new Socket(address, port);
			/** Instantiate a BufferedOutputStream object */
			BufferedOutputStream bos = new BufferedOutputStream(connection.
					getOutputStream());

			/** Instantiate an OutputStreamWriter object with the optional character
			 * encoding.
			 */
			OutputStreamWriter osw = new OutputStreamWriter(bos, "US-ASCII");
			
			/** Write across the socket connection and flush the buffer */
			String msg = createXMLProblemInstance(problemInstance) + (char)3;
			osw.write( msg);
			osw.flush();
			BufferedInputStream bis = new BufferedInputStream(connection.
					getInputStream());
			/**Instantiate an InputStreamReader with the optional
			 * character encoding.
			 */

			InputStreamReader isr = new InputStreamReader(bis, "US-ASCII");
			DOMParser p = new DOMParser();
			/**Read the socket's InputStream and append to a StringBuffer */

			StringBuffer message = new StringBuffer();
			int character;

			while (true) {
				message = new StringBuffer();
				while((character = isr.read()) != (char)3) {
					message.append((char)character);
				}
				ByteArrayInputStream bais = new ByteArrayInputStream(message.toString().getBytes());
				InputSource isrc = new InputSource();
				isrc.setByteStream(bais);
				p.parse(isrc);
				XMLType msgType = getXMLType(p,isrc);
				if ( msgType != XMLType.ROUND ) {
					break;
				}

				int totalRoundNum = getANumber(p,isrc,"round", "round-total-num");
				int roundnum = getANumber(p,isrc,"round", "round-num");
				System.out.println("Round " + roundnum + " Among " + totalRoundNum);
				
				if (SHOW_MEMORY_USAGE)
					System.out.print("[ Memory usage: " + 
							_df.format((RUNTIME.totalMemory() - RUNTIME.freeMemory())/1e6d) + "Mb / " + 
							_df.format(RUNTIME.totalMemory()/1e6d) + "Mb" + 
							" = " + _df.format(((double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 
											   (double) RUNTIME.totalMemory())) + " ]\n");
				
				while (true) {
					message = new StringBuffer();
					while((character = isr.read()) != (char)3) {
						message.append((char)character);
					}
					bais = new ByteArrayInputStream(message.toString().getBytes());
					isrc = new InputSource();
					isrc.setByteStream(bais);
					p.parse(isrc);
					msgType = getXMLType(p,isrc);
					if (msgType == XMLType.ROUND_END) {
						break;
					}
					if (msgType != XMLType.TURN ) {
						break;
					}

					int turn = getANumber(p,isrc,"turn","turn-num");
					System.out.println("Turn " + turn);
					ArrayList<PVAR_INST_DEF> obs = getXMLObservation(p,isrc);
					osw = new OutputStreamWriter(bos, "US-ASCII");
					String returnString = createXMLAction(state);
					osw.write(returnString + (char)3);
					osw.flush();

				}
				if ( ++roundnum >= totalRoundNum ) {
					break;
				}
			}	

			/** Close the socket connection. */
			connection.close();
			System.out.println(instr);
		}
		catch (IOException f) {
			System.out.println("IOException: " + f);
		}
		catch (Exception g) {
			System.out.println("Exception: " + g);
		}
	}
	
	static String serialize(Document dom) {
		OutputFormat format = new OutputFormat(dom);
//		format.setIndenting(true);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		XMLSerializer xmls = new XMLSerializer(baos, format);
		try {
			xmls.serialize(dom);
			return baos.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	static XMLType getXMLType(DOMParser p,InputSource isrc) {

		Element e = p.getDocument().getDocumentElement();
		if ( e.getNodeName().equals("turn") ) {
			return XMLType.TURN;
		} else if (e.getNodeName().equals("round")) {
			return XMLType.ROUND;
		} else if (e.getNodeName().equals("round-end")) {
			return XMLType.ROUND_END;
		} else if (e.getNodeName().equals("end-test")) {
			return XMLType.END_TEST;
		} else {
			return XMLType.NONAME;
		}
	}
	
	static String createXMLProblemInstance (String problemName) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			//get an instance of builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//create an instance of DOM
			Document dom = db.newDocument();
			Element rootEle = dom.createElement("request-test");
			dom.appendChild(rootEle);
			Element pEle = dom.createElement("problem-name");
			Text pText = dom.createTextNode(problemName); 
			pEle.appendChild(pText);
			rootEle.appendChild(pEle);
			return serialize(dom);
		} catch (Exception e) {
			return null;
		}
	}
	
	static String createXMLAction(State state) {
		PVAR_NAME p = state._alActionNames.get(0);
		ArrayList<ArrayList<LCONST>> inst;
		try {
			inst = state.generateAtoms(p);
			ArrayList<LCONST> terms = inst.get(Server.rand.nextInt(inst.size()));
			// Generate the action list
			PVAR_INST_DEF d = new PVAR_INST_DEF(p._sPVarName, new Boolean(true), terms); 
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element action = dom.createElement("action");
			dom.appendChild(action);
			Element name = dom.createElement("name");
			action.appendChild(name);
			Text textName = dom.createTextNode(d._sPredName.toString());
			name.appendChild(textName);
			for( LCONST lc : d._alTerms ) {
				Element arg = dom.createElement("arg");
				Text textArg = dom.createTextNode(lc.toString());
				arg.appendChild(textArg);
				action.appendChild(arg);
			}
			return serialize(dom);
		} catch (EvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	static int getANumber (DOMParser p, InputSource isrc, 
			String parentName, String name) {
		Element e = p.getDocument().getDocumentElement();
		if ( e.getNodeName().equals(parentName) ) {
			String turnnum = Server.getTextValue(e, name).get(0);
			return Integer.valueOf(turnnum);
		}
		return -1;
	}

	
	static ArrayList<PVAR_INST_DEF> getXMLObservation (DOMParser p, InputSource isrc) {

		Element e = p.getDocument().getDocumentElement();
		if ( e.getNodeName().equals("turn") ) {
			NodeList nl = e.getElementsByTagName("observation-fluent");
			if(nl != null && nl.getLength() > 0) {
				ArrayList<PVAR_INST_DEF> ds = new ArrayList<PVAR_INST_DEF>();
				for(int i = 0 ; i < nl.getLength();i++) {
					Element el = (Element)nl.item(i);
					String name = Server.getTextValue(el, "fluent-name").get(0);
					ArrayList<String> args = Server.getTextValue(el, "fluent-arg");
					ArrayList<LCONST> lcArgs = new ArrayList<LCONST>();
					for( String arg : args ) {
						lcArgs.add(new LCONST(arg));
					}
					String value = Server.getTextValue(el, "fluent-value").get(0);
					PVAR_INST_DEF d = new PVAR_INST_DEF(name, Double.valueOf(value), lcArgs);
					ds.add(d);
				}
				return ds;
			}
			return null;
		}

		return null;
	}
}

