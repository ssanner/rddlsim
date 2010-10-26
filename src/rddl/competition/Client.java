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
import org.xml.sax.SAXException;

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
import rddl.policy.Policy;
import rddl.policy.RandomEnumPolicy;
import rddl.viz.StateViz;
/** The SocketClient class is a simple example of a TCP/IP Socket Client.
 *
 */

public class Client {
	
	public static final boolean SHOW_MEMORY_USAGE = true;
	public static final Runtime RUNTIME = Runtime.getRuntime();
	private static DecimalFormat _df = new DecimalFormat("0.##");	
	enum XMLType {
		ROUND,TURN,ROUND_END,END_TEST,NONAME
	}
	
	int numRounds;
	double timeAllowed;
	int curRound;
	double reward;
	int id;

	Client () {
		numRounds = 0;
		timeAllowed = 0;
		curRound = 0;
		reward = 0;
		id = 0;
	}
	
	/**
	 * 
	 * @param args
	 * 1. rddl description file name with RDDL syntax, with complete path (sysadmin.rddl)
	 * 2. host name (local host)
	 * 3. client name (for record keeping purpose on server side. identify yourself with name.
	 * 4. (optional) port number
	 */
	public static void main(String[] args) {
		RDDL rddl;
		/** Define a host server */
		String host = Server.HOST_NAME;
		/** Define a port */
		int port = Server.PORT_NUMBER;
		String clientName = "random";
		
		State      state;
		INSTANCE   instance;
		NONFLUENTS nonFluents = null;
		DOMAIN     domain;
		StateViz   stateViz;
		
		StringBuffer instr = new StringBuffer();
		String TimeStamp;
		
		if ( args.length < 3 ) {
			System.out.println("usage: rddlfilename hostname clientname (optional) portnumber");
			System.exit(1);
		}
		host = args[1];
		clientName = args[2];
		double timeLeft = 0;
		try {
			rddl = parser.parse(new File(args[0]));
			if ( args.length > 3 ) {
				port = Integer.valueOf(args[1]);
			}
			state = new State();
			// just pick the first
			String problemInstance = rddl._tmInstanceNodes.firstKey();
//			Policy policy = new RandomBoolPolicy(problemInstance);
			Policy policy = new RandomEnumPolicy(problemInstance);
			
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
			System.out.println("RDDL client initialized");
			
			/** Instantiate a BufferedOutputStream object */
			BufferedOutputStream bos = new BufferedOutputStream(connection.
					getOutputStream());
			/** Instantiate an OutputStreamWriter object with the optional character
			 * encoding.
			 */
			OutputStreamWriter osw = new OutputStreamWriter(bos, "US-ASCII");
			/** Write across the socket connection and flush the buffer */
			String msg = createXMLSessionRequest(problemInstance, clientName);
			Server.sendOneMessage(osw, msg);
			BufferedInputStream bis = new BufferedInputStream(connection.
					getInputStream());
			/**Instantiate an InputStreamReader with the optional
			 * character encoding.
			 */
			InputStreamReader isr = new InputStreamReader(bis, "US-ASCII");
			DOMParser p = new DOMParser();
			
			/**Read the socket's InputStream and append to a StringBuffer */

			InputSource isrc = Server.readOneMessage(isr);
			Client client = processXMLSessionInit(p, isrc);
			System.out.println(client.id + ":" + client.numRounds);
			int r = 0;
			for( ; r < client.numRounds; r++ ) {
				if (SHOW_MEMORY_USAGE)
					System.out.print("[ Memory usage: " + 
							_df.format((RUNTIME.totalMemory() - RUNTIME.freeMemory())/1e6d) + "Mb / " + 
							_df.format(RUNTIME.totalMemory()/1e6d) + "Mb" + 
							" = " + _df.format(((double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 
											   (double) RUNTIME.totalMemory())) + " ]\n");
				state.init(nonFluents != null ? nonFluents._hmObjects : null, instance._hmObjects,
						domain._hmTypes, domain._hmPVariables, domain._hmCPF,
						instance._alInitState, nonFluents == null ? null : nonFluents._alNonFluents);
				msg = createXMLRoundRequest();
				Server.sendOneMessage(osw, msg);
				isrc = Server.readOneMessage(isr);
				timeLeft = processXMLRoundInit(p, isrc, r+1);
				if ( timeLeft < 0 ) {
					break;
				}
				int h =0;
				System.out.println(instance._nHorizon);
				for(; h < instance._nHorizon; h++ ) {
					isrc = Server.readOneMessage(isr);
					ArrayList<PVAR_INST_DEF> obs = processXMLTurn(p,isrc,state);
					if ( obs == null ) {
					} else  {
						state.setPVariables(state._state, obs);
					}
					msg = createXMLAction(state, policy, 0);
					Server.sendOneMessage(osw, msg);
				}
				if ( h < instance._nHorizon ) {
					break;
				}
				isrc = Server.readOneMessage(isr);
				processXMLRoundEnd(p, isrc);
			}
			isrc = Server.readOneMessage(isr);
			processXMLSessionEnd(p, isrc);
			
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
	
	static String createXMLSessionRequest (String problemName, String clientName) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			//get an instance of builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//create an instance of DOM
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(Server.SESSION_REQUEST);
			dom.appendChild(rootEle);
			Server.addOneText(dom, rootEle, Server.PROBLEM_NAME, problemName);
			Server.addOneText(dom, rootEle, Server.CLIENT_NAME, clientName);
			return serialize(dom);
		} catch (Exception e) {
			return null;
		}
	}
	
	static String createXMLRoundRequest() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(Server.ROUND_REQUEST);
			dom.appendChild(rootEle);
			return serialize(dom);
		} catch (Exception e) {
			return null;
		}
	}
	
	static Client processXMLSessionInit(DOMParser p, InputSource isrc) throws RDDLXMLException {
		try {
			p.parse(isrc);
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("sax exception");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("io exception");
		}
		Client c = new Client();
		Element e = p.getDocument().getDocumentElement();
		
		if ( !e.getNodeName().equals(Server.SESSION_INIT) ) {
			throw new RDDLXMLException("not session init");
		}
		ArrayList<String> r = Server.getTextValue(e, Server.SESSION_ID);
		if ( r != null ) {
			c.id = Integer.valueOf(r.get(0));
		}
		r = Server.getTextValue(e, Server.NUM_ROUNDS);
		if ( r != null ) {
			c.numRounds = Integer.valueOf(r.get(0));
		}
		r = Server.getTextValue(e, Server.TIME_ALLOWED);
		if ( r!= null ) {
			c.timeAllowed = Double.valueOf(r.get(0));
		}
		return c;
	}
	
	
	static String createXMLAction(State state, Policy policy, int nth) {
		PVAR_NAME p = state._alActionNames.get(0);
		ArrayList<ArrayList<LCONST>> inst;
		try {
			PVAR_INST_DEF d = policy.getActions(state).get(nth);  
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element action = dom.createElement(Server.ACTION);
			dom.appendChild(action);
			Element name = dom.createElement(Server.ACTION_NAME);
			action.appendChild(name);
			Text textName = dom.createTextNode(d._sPredName.toString());
			name.appendChild(textName);
			for( LCONST lc : d._alTerms ) {
				Element arg = dom.createElement(Server.ACTION_ARG);
				Text textArg = dom.createTextNode(lc.toString());
				arg.appendChild(textArg);
				action.appendChild(arg);
			}
			Element value = dom.createElement(Server.ACTION_VALUE);
			Text textValue = dom.createTextNode(d._oValue.toString());
			value.appendChild(textValue);
			action.appendChild(value);
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

	static double processXMLRoundInit(DOMParser p, InputSource isrc,
			int curRound) throws RDDLXMLException {
		try {
			p.parse(isrc);
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("sax exception");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("io exception");
		}
		Element e = p.getDocument().getDocumentElement();
		if ( !e.getNodeName().equals(Server.ROUND_INIT)) {
			return -1;
		}
		ArrayList<String> r = Server.getTextValue(e, Server.ROUND_NUM);
		if ( r == null || curRound != Integer.valueOf(r.get(0))) {
			return -1;
		}
		r =	Server.getTextValue(e, Server.TIME_LEFT);
		if ( r == null ) {
			return -1;
		}
		return Double.valueOf(r.get(0));
	}
	
	static ArrayList<PVAR_INST_DEF> processXMLTurn (DOMParser p, InputSource isrc,
			State state) throws RDDLXMLException {
		try {
			p.parse(isrc);
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("sax exception");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("io exception");
		}
		Element e = p.getDocument().getDocumentElement();
		if ( e.getNodeName().equals(Server.TURN) ) {
			NodeList nl = e.getElementsByTagName(Server.OBSERVED_FLUENT);
			if(nl != null && nl.getLength() > 0) {
				ArrayList<PVAR_INST_DEF> ds = new ArrayList<PVAR_INST_DEF>();
				for(int i = 0 ; i < nl.getLength();i++) {
					Element el = (Element)nl.item(i);
					String name = Server.getTextValue(el, Server.FLUENT_NAME).get(0);
					ArrayList<String> args = Server.getTextValue(el, Server.FLUENT_ARG);
					ArrayList<LCONST> lcArgs = new ArrayList<LCONST>();
					for( String arg : args ) {
						if (arg.startsWith("@"))
							lcArgs.add(new RDDL.ENUM_VAL(arg));
						else 
							lcArgs.add(new RDDL.OBJECT_VAL(arg));
					}
					String value = Server.getTextValue(el, Server.FLUENT_VALUE).get(0);
					Object r = Server.getValue(name, value, state);
					PVAR_INST_DEF d = new PVAR_INST_DEF(name, r, lcArgs);
					ds.add(d);
				}
				return ds;
			}
			return null;
		}
		return null;
	}
	
	static double processXMLRoundEnd(DOMParser p, InputSource isrc) throws RDDLXMLException {
		try {
			p.parse(isrc);
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("sax exception");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("io exception");
		}
		Element e = p.getDocument().getDocumentElement();
		if ( e.getNodeName().equals(Server.ROUND_END) ) {
			ArrayList<String> text = Server.getTextValue(e, Server.ROUND_REWARD);
			if ( text == null ) {
				return -1;
			}
			return Double.valueOf(text.get(0));
		}
		return -1;
	}
			
	static double processXMLSessionEnd(DOMParser p, InputSource isrc) throws RDDLXMLException {
		try {
			p.parse(isrc);
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("sax exception");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RDDLXMLException("io exception");
		}
		Element e = p.getDocument().getDocumentElement();
		if ( e.getNodeName().equals(Server.SESSION_END) ) {
			ArrayList<String> text = Server.getTextValue(e, Server.TOTAL_REWARD);
			if ( text == null ) {
				return -1;
			}
			return Double.valueOf(text.get(0));
		}
		return -1;
	}
}

