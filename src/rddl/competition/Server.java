/**
 * RDDL: Main server code for interaction with RDDLSim client
 * 
 * @author Sungwook Yoon (sungwook.yoon@gmail.com)
 * @version 10/1/10
 *
 **/

package rddl.competition;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import rddl.RDDL;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.parser.parser;
import rddl.policy.Policy;
import rddl.policy.RandomBoolPolicy;
import rddl.viz.NullScreenDisplay;
import rddl.viz.StateViz;

public class Server implements Runnable {
	
	private static final String LOG_FILE = "rddl.log";
	/**
	 * following is XML definitions
	 */
	public static final String SESSION_REQUEST = "session-request";
	public static final String CLIENT_NAME = "client-name";
	public static final String PROBLEM_NAME = "problem-name";
	public static final String SESSION_INIT = "session-init";
	public static final String SESSION_ID = "session-id";
	public static final String SESSION_END = "session-end";
	public static final String TOTAL_REWARD = "total-reward";
	public static final String TIME_SPENT = "time-spent";
	public static final String NUM_ROUNDS = "num-rounds";
	public static final String TIME_ALLOWED = "time-allowed";
	public static final String ROUNDS_USED = "rounds-used";
	
	public static final String ROUND_REQUEST = "round-request";
	public static final String ROUND_INIT = "round-init";
	public static final String ROUND_NUM = "round-num";
	public static final String ROUND_LEFT = "round-left";
	public static final String TIME_LEFT = "time-left";
	public static final String ROUND_END = "round-end";
	public static final String ROUND_REWARD = "round-reward";
	public static final String TURNS_USED = "turns-used";
	public static final String TIME_USED = "time-used";
	
	public static final String TURN = "turn";
	public static final String TURN_NUM = "turn-num";
	public static final String OBSERVED_FLUENT = "observed-fluent";
	public static final String FLUENT_NAME = "fluent-name";
	public static final String FLUENT_ARG = "fluent-arg";
	public static final String FLUENT_VALUE = "fluent-value";
	
	public static final String ACTION = "action";
	public static final String ACTION_NAME = "action-name";
	public static final String ACTION_ARG = "action-arg";
	public static final String DONE = "done";
	public static final String NOOP = "noop";
	
	public static final int PORT_NUMBER = 2323;
	public static final String HOST_NAME = "localhost";

	public static final boolean SHOW_MEMORY_USAGE = true;
	public static final Runtime RUNTIME = Runtime.getRuntime();
	private static DecimalFormat _df = new DecimalFormat("0.##");
	
	
	private Socket connection;
	private String TimeStamp;
	private RDDL rddl = null;
	private static int ID = 0;
	private static int DEFAULT_NUM_ROUNDS = 30;
	private static double DEFAULT_TIME_ALLOWED = 30;
	public int id;
	public String clientName = null;
	public String requestedInstance = null;
	public static Random rand = new Random(1);
	
	public State      state;
	public INSTANCE   instance;
	public NONFLUENTS nonFluents;
	public DOMAIN     domain;
	public StateViz   stateViz;
	
	public static void main(String[] args) {
		RDDL rddl;
		int port = PORT_NUMBER;
		if ( args.length < 1 ) {
			System.out.println("usage: rddlfilename (optional) portnumber");
			System.exit(1);
		}
		try {
			rddl = parser.parse(new File(args[0]));
			// Get first instance name in file and create a simulator
			if ( args.length > 1) {
				port = Integer.valueOf(args[1]);
			}
			ServerSocket socket1 = new ServerSocket(port);
			System.out.println("RDDL Test Server Initialized");
			while (true) {
				Socket connection = socket1.accept();
				Runnable runnable = new Server(connection, ++ID, rddl);
				Thread thread = new Thread(runnable);
				thread.start();
			}
		}
		catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	Server (Socket s, int i, RDDL rddl) {
		this.connection = s;
		this.id = i;
		this.rddl = rddl;
	}
	public void run() {
		DOMParser p = new DOMParser();
		Policy policy = new RandomBoolPolicy("");
		int numRounds = DEFAULT_NUM_ROUNDS;
		double timeAllowed = DEFAULT_TIME_ALLOWED;
		double timeUsed = 0;
		int sessionId = 0;
		try {
			BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
			InputStreamReader isr = new InputStreamReader(is);
			InputSource isrc = readOneMessage(isr);
			processXMLSessionRequest(p, isrc, this);
			System.out.println(requestedInstance);
	
			BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
			OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
			String msg = createXMLSessionInit(numRounds, timeAllowed, this);
			sendOneMessage(osw,msg);			

			initializeState(requestedInstance);
			stateViz = new NullScreenDisplay(false); // GenericScreenDisplay(true)
			
			double accum_total_reward = 0;
			ArrayList<Double> rewards = new ArrayList<Double>(DEFAULT_NUM_ROUNDS * instance._nHorizon);
			int r = 0;
			for( ; r < numRounds; r++ ) {			
				isrc = readOneMessage(isr);
				if ( !processXMLRoundRequest(p, isrc) ) {
					break;
				}
				resetState();
				msg = createXMLRoundInit(r+1, numRounds, timeUsed, timeAllowed);
				sendOneMessage(osw,msg);
				
				if (SHOW_MEMORY_USAGE)
					System.out.print("[ Memory usage: " + 
							_df.format((RUNTIME.totalMemory() - RUNTIME.freeMemory())/1e6d) + "Mb / " + 
							_df.format(RUNTIME.totalMemory()/1e6d) + "Mb" + 
							" = " + _df.format(((double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 
											   (double) RUNTIME.totalMemory())) + " ]\n");
				
				// NOTE: should pre-initialize arrays when constantly adding to them
				
				double accum_reward = 0.0d;
				double cur_discount = 1.0d;
				int h = 0;
				for( ; h < instance._nHorizon; h++ ) {
					
					msg = createXMLTurn(state, h+1, domain);
					sendOneMessage(osw,msg);

					isrc = readOneMessage(isr);		
					PVAR_INST_DEF d = processXMLAction(p,isrc);
					System.out.println(d);
					if ( d == null ) {
						break;
					}
					ArrayList<PVAR_INST_DEF> ds = new ArrayList<PVAR_INST_DEF>();
					ds.add(d);
//					ds = policy.getActions(state);
					state.computeNextState(ds, rand);
					// Calculate reward / objective and store
					double reward = ((Number)domain._exprReward.sample(new HashMap<LVAR,LCONST>(), 
							state, rand)).doubleValue();
					rewards.add(reward);
					accum_reward += cur_discount * reward;
					cur_discount *= instance._dDiscount;

					stateViz.display(state, h);			
					state.advanceNextState();
				}
				accum_total_reward += accum_reward;
				msg = createXMLRoundEnd(r, accum_reward, h, 0);
				sendOneMessage(osw, msg);
			}
			msg = createXMLSessionEnd(accum_total_reward, r, 0, this.clientName, this.id);
			sendOneMessage(osw, msg);

			BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true));
			bw.write(msg);
			bw.newLine();
			bw.flush();

			//need to wait 10 seconds to pretend that we're processing something
//			try {
//				Thread.sleep(10000);
//			}
//			catch (Exception e){}
//			TimeStamp = new java.util.Date().toString();
//			String returnCode = "MultipleSocketServer repsonded at "+ TimeStamp + (char) 3;
		}
		catch (Exception e) {
			System.out.println(e);
		}
		finally {
			try {
				connection.close();
			}
			catch (IOException e){}
		}
	}
	
	void initializeState (String requestedInstance) {
		state = new State();
		instance = rddl._tmInstanceNodes.get(requestedInstance);
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
	}
	
	void resetState () {
		state.init(nonFluents != null ? nonFluents._hmObjects : null, instance._hmObjects, 
				domain._hmTypes, domain._hmPVariables, domain._hmCPF,
				instance._alInitState, nonFluents == null ? null : nonFluents._alNonFluents);
	}
	
	static PVAR_INST_DEF processXMLAction(DOMParser p, InputSource isrc) {
		try {
			p.parse(isrc);
			Element e = p.getDocument().getDocumentElement();
			if ( e.getNodeName().equals("action") ) {
				String name = getTextValue(e, "name").get(0);
				ArrayList<String> args = getTextValue(e, "arg");
				ArrayList<LCONST> lcArgs = new ArrayList<LCONST>();
				for( String arg : args ) {
					lcArgs.add(new LCONST(arg));
				}
				PVAR_INST_DEF d = new PVAR_INST_DEF(name, new Boolean(true), lcArgs);
				return d;
			}
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
	
	public static void sendOneMessage (OutputStreamWriter osw, String msg) throws IOException {
		osw.write(msg + (char)3);
		osw.flush();
	}
	
	public static InputSource readOneMessage(InputStreamReader isr) {
		StringBuffer message = new StringBuffer();
		int character;
		try {
			while((character = isr.read()) != (char)3) {
				message.append((char)character);
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(message.toString().getBytes());
			InputSource isrc = new InputSource();
			isrc.setByteStream(bais);
			return isrc;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	static String createXMLSessionInit (int numRounds, double timeAllowed, Server server) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(SESSION_INIT);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,SESSION_ID, server.id + "");
			addOneText(dom,rootEle,NUM_ROUNDS, numRounds + "");
			addOneText(dom,rootEle,TIME_ALLOWED, timeAllowed + "");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static void processXMLSessionRequest (DOMParser p, InputSource isrc,
			Server server) {
        try {
			p.parse(isrc);
			Element e = p.getDocument().getDocumentElement();
			if ( e.getNodeName().equals(SESSION_REQUEST) ) {
				server.requestedInstance = getTextValue(e, PROBLEM_NAME).get(0);
				server.clientName = getTextValue(e,CLIENT_NAME).get(0);
			}
			return;
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return;
	}
	
	static boolean processXMLRoundRequest (DOMParser p, InputSource isrc) {
        try {
			p.parse(isrc);
			Element e = p.getDocument().getDocumentElement();
			if ( e.getNodeName().equals(ROUND_REQUEST) ) {
				return true;			
			}
			return false;
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
	
	public static ArrayList<String> getTextValue(Element ele, String tagName) {
		ArrayList<String> returnVal = new ArrayList<String>();
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			for ( int i= 0; i < nl.getLength(); i++ ) {
				Element el = (Element)nl.item(i);
				returnVal.add(el.getFirstChild().getNodeValue());
			}
			return returnVal;
		}
		return null;
	}
	
	static String createXMLTurn (State state, int turn, DOMAIN domain) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(TURN);
			dom.appendChild(rootEle);
			Element turnNum = dom.createElement(TURN_NUM);
			Text textTurnNum = dom.createTextNode(turn + "");
			turnNum.appendChild(textTurnNum);
			rootEle.appendChild(turnNum);
			
			
			for ( PVAR_NAME pn :  domain._bPartiallyObserved? state._observ.keySet() 
					: state._state.keySet() ) {
				for ( ArrayList<LCONST> lcs : state._observ.get(pn).keySet() ) {
					Element ofEle = dom.createElement(OBSERVED_FLUENT);
					rootEle.appendChild(ofEle);

					Element pName = dom.createElement(FLUENT_NAME);
					Text pTextName = dom.createTextNode(pn.toString());
					pName.appendChild(pTextName);
					ofEle.appendChild(pName);
					for ( LCONST lc : lcs ) {
						Element pArg = dom.createElement(FLUENT_ARG);
						Text pTextArg = dom.createTextNode(lc.toString());
						pArg.appendChild(pTextArg);
						ofEle.appendChild(pArg);
					}
					Element pValue = dom.createElement(FLUENT_VALUE);
					Text pTextValue = dom.createTextNode(state._observ.get(pn).get(lcs).toString());
					pValue.appendChild(pTextValue);
					ofEle.appendChild(pValue);
				}
			}
			return(Client.serialize(dom));
			
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static String createXMLRoundInit (int round, int numRounds, double timeUsed,
			double timeAllowed) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(ROUND_INIT);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,ROUND_NUM, round + "");
			addOneText(dom,rootEle,ROUND_LEFT, (numRounds - round) + "");
			addOneText(dom,rootEle,TIME_LEFT, (timeAllowed-timeUsed) + "");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static String createXMLRoundEnd (int round, double reward,
			int turnsUsed, double timeUsed) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(ROUND_END);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,	ROUND_NUM, round + "");
			addOneText(dom,rootEle, ROUND_REWARD, reward + "");			
			addOneText(dom,rootEle, TURNS_USED, turnsUsed + "");
			addOneText(dom,rootEle, TIME_USED, timeUsed + "");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	public static void addOneText(Document dom, Element p, 
			String name, String value) {
		Element e = dom.createElement(name);
		Text text = dom.createTextNode(value);
		e.appendChild(text);
		p.appendChild(e);
	}
	
	static String createXMLSessionEnd(double reward, int roundsUsed, double timeUsed,
			String clientName, int sessionId) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(SESSION_END);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,TOTAL_REWARD, reward + "");			
			addOneText(dom,rootEle,ROUNDS_USED, roundsUsed + "");
			addOneText(dom,rootEle,TIME_USED, timeUsed + "");
			addOneText(dom,rootEle,CLIENT_NAME, clientName + "");
			addOneText(dom,rootEle,SESSION_ID, sessionId + "");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
}

