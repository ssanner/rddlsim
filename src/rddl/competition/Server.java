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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import rddl.EvalException;
import rddl.RDDL;
import rddl.RDDL.*;
import rddl.State;
import rddl.parser.parser;
import rddl.policy.Policy;
import rddl.policy.RandomBoolPolicy;
import rddl.viz.GenericScreenDisplay;
import rddl.viz.NullScreenDisplay;
import rddl.viz.StateViz;

import util.Timer;

public class Server implements Runnable {
	
	public static final boolean SHOW_ACTIONS = true;
	public static final boolean SHOW_XML = false;
	public static final boolean SHOW_MSG = false;
	public static final boolean SHOW_TIMING = false;
	
	private static final String LOG_FILE = "rddl";
	/**
	 * following is XML definitions
	 */
	public static final String SESSION_REQUEST = "session-request";
	public static final String CLIENT_NAME = "client-name";
	public static final String INSTANCE_NAME = "instance-name";
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
	public static final String NULL_OBSERVATIONS = "no-observed-fluents";
	public static final String FLUENT_NAME = "fluent-name";
	public static final String FLUENT_ARG = "fluent-arg";
	public static final String FLUENT_VALUE = "fluent-value";
	
	public static final String ACTIONS = "actions";
	public static final String ACTION = "action";
	public static final String ACTION_NAME = "action-name";
	public static final String ACTION_ARG = "action-arg";
	public static final String ACTION_VALUE = "action-value";
	public static final String DONE = "done";
	
	public static final int PORT_NUMBER = 2323;
	public static final String HOST_NAME = "localhost";
	public static final int DEFAULT_SEED = 0;

	public static final String NO_XML_HEADER = "no-header";
	public static boolean NO_XML_HEADING = false;
	public static final boolean SHOW_MEMORY_USAGE = true;
	public static final Runtime RUNTIME = Runtime.getRuntime();
	private static DecimalFormat _df = new DecimalFormat("0.##");
	
	
	private Socket connection;
	private String TimeStamp;
	private RDDL rddl = null;
	private static int ID = 0;
	private static int DEFAULT_NUM_ROUNDS = 30;
	private static double DEFAULT_TIME_ALLOWED = 30;
	public int port;
	public int id;
	public String clientName = null;
	public String requestedInstance = null;
	public Random rand;
	
	public State      state;
	public INSTANCE   instance;
	public NONFLUENTS nonFluents;
	public DOMAIN     domain;
	public StateViz   stateViz;
	
	/**
	 * 
	 * @param args
	 * 1. rddl description file name (can be directory), in RDDL format, with complete path
	 * 2. (optional) port number
	 * 3. (optional) random seed
	 */
	public static void main(String[] args) {
		
		// StateViz state_viz = new GenericScreenDisplay(true); 
		StateViz state_viz = new NullScreenDisplay(false);

		ArrayList<RDDL> rddls = new ArrayList<RDDL>();
		int port = PORT_NUMBER;
		if ( args.length < 1 ) {
			System.out.println("usage: rddlfilename (optional) portnumber num-rounds random-seed state-viz-class-name");
			System.out.println("\nexample 1: Server rddlfilename");
			System.out.println("example 2: Server rddlfilename 2323");
			System.out.println("example 3: Server rddlfilename 2323 100 0 rddl.viz.GenericScreenDisplay");
			System.exit(1);
		}
				
		try {
			// Load RDDL files
			RDDL rddl = new RDDL();
			File f = new File(args[0]);
			if (f.isDirectory()) {
				for (File f2 : f.listFiles())
					if (f2.getName().endsWith(".rddl")) {
						System.out.println("Loading: " + f2);
						rddl.addOtherRDDL(parser.parse(f2));
					}
			} else
				rddl.addOtherRDDL(parser.parse(f));

			if ( args.length > 1) {
				port = Integer.valueOf(args[1]);
			}
			ServerSocket socket1 = new ServerSocket(port);
			if (args.length > 2) {
				DEFAULT_NUM_ROUNDS = Integer.valueOf(args[2]);
			}
			int rand_seed = -1;
			if ( args.length > 3) {
				rand_seed = Integer.valueOf(args[3]);
			} else {
				rand_seed = DEFAULT_SEED;
			}
			if (args.length > 4) {
				state_viz = (StateViz)Class.forName(args[4]).newInstance();
			}
			System.out.println("RDDL Server Initialized");
			while (true) {
				Socket connection = socket1.accept();
				Runnable runnable = new Server(connection, ++ID, rddl, state_viz, port, new Random(rand_seed));
				Thread thread = new Thread(runnable);
				thread.start();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}
	Server (Socket s, int i, RDDL rddl, StateViz state_viz, int port, Random rgen) {
		this.connection = s;
		this.id = i;
		this.rddl = rddl;
		this.stateViz = state_viz;
		this.port = port;
		this.rand = rgen;
	}
	public void run() {
		DOMParser p = new DOMParser();
		int numRounds = DEFAULT_NUM_ROUNDS;
		double timeAllowed = DEFAULT_TIME_ALLOWED;
		double timeUsed = 0;
		try {
			BufferedInputStream isr = new BufferedInputStream(connection.getInputStream());
			//InputStreamReader isr = new InputStreamReader(is);
			InputSource isrc = readOneMessage(isr);
			requestedInstance = null;
			processXMLSessionRequest(p, isrc, this);
			System.out.println(requestedInstance);
	
			if (!rddl._tmInstanceNodes.containsKey(requestedInstance)) {
				System.out.println("Instance name '" + requestedInstance + "' not found.");
				return;
			}

			BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
			OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
			String msg = createXMLSessionInit(numRounds, timeAllowed, this);
			sendOneMessage(osw,msg);			

			initializeState(rddl, requestedInstance);
			//System.out.println("STATE:\n" + state);
			
			double accum_total_reward = 0d;
			ArrayList<Double> rewards = new ArrayList<Double>(DEFAULT_NUM_ROUNDS * instance._nHorizon);
			int r = 0;
			long session_elapsed_time = 0l;
			for( ; r < numRounds; r++ ) {			
				isrc = readOneMessage(isr);
				if ( !processXMLRoundRequest(p, isrc) ) {
					break;
				}
				resetState();
				msg = createXMLRoundInit(r+1, numRounds, timeUsed, timeAllowed);
				sendOneMessage(osw,msg);
				
				long start_round_time = System.currentTimeMillis();
				System.out.println("Round " + (r+1) + " / " + numRounds);
				if (SHOW_MEMORY_USAGE)
					System.out.print("[ Memory usage: " + 
							_df.format((RUNTIME.totalMemory() - RUNTIME.freeMemory())/1e6d) + "Mb / " + 
							_df.format(RUNTIME.totalMemory()/1e6d) + "Mb" + 
							" = " + _df.format(((double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 
											   (double) RUNTIME.totalMemory())) + " ]\n");
				
				double accum_reward = 0.0d;
				double cur_discount = 1.0d;
				int h = 0;
				HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> observStore =null;
				for( ; h < instance._nHorizon; h++ ) {
					
					Timer timer = new Timer();
				
					//if ( observStore != null) {
					//	for ( PVAR_NAME pn : observStore.keySet() ) {
					//		System.out.println("check3 " + pn);
					//		for( ArrayList<LCONST> aa : observStore.get(pn).keySet()) {
					//			System.out.println("check3 :" + aa + ": " + observStore.get(pn).get(aa));
					//		}
					//	}
					//}
					msg = createXMLTurn(state, h+1, domain, observStore);
					
					if (SHOW_TIMING)
						System.out.println("**TIME to create XML turn: " + timer.GetTimeSoFarAndReset());
					
					if (SHOW_MSG)
						System.out.println("Sending msg:\n" + msg);
					sendOneMessage(osw,msg);

					isrc = readOneMessage(isr);	
					if (isrc == null)
						throw new Exception("FATAL SERVER EXCEPTION: EMPTY CLIENT MESSAGE");

					if (SHOW_TIMING)
						System.out.println("**TIME to send/read msg: " + timer.GetTimeSoFarAndReset());
						
					ArrayList<PVAR_INST_DEF> ds = processXMLAction(p,isrc,state);
					if ( ds == null ) {
						break;
					}
					
					if (SHOW_TIMING)
						System.out.println("**TIME to process XML action: " + timer.GetTimeSoFarAndReset());
					
					//Sungwook: this is not required.  -Scott
					//if ( h== 0 && domain._bPartiallyObserved && ds.size() != 0) {
					//	System.err.println("the first action for partial observable domain should be noop");
					//}
					if (SHOW_ACTIONS)
						System.out.println("** Actions received: " + ds);
					
					// Check state-action constraints (also checks maxNonDefActions)
					try {
						state.checkStateActionConstraints(ds);
					} catch (Exception e) {
						System.out.println("TRIAL ERROR -- STATE-ACTION CONSTRAINT VIOLATION:\n" + e);
						break;
					}
					
					try {
						state.computeNextState(ds, rand);
					} catch (Exception ee) {
						System.out.println("FATAL SERVER EXCEPTION:\n" + ee);
						//ee.printStackTrace();
						throw ee;
						//System.exit(1);
					}
					//for ( PVAR_NAME pn : state._observ.keySet() ) {
					//	System.out.println("check1 " + pn);
					//	for( ArrayList<LCONST> aa : state._observ.get(pn).keySet()) {
					//		System.out.println("check1 :" + aa + ": " + state._observ.get(pn).get(aa));
					//	}
					//}
					
					if (SHOW_TIMING)
						System.out.println("**TIME to compute next state: " + timer.GetTimeSoFarAndReset());
					
					if (domain._bPartiallyObserved)
						observStore = copyObserv(state._observ);
					
					// Calculate reward / objective and store
					double reward = ((Number)domain._exprReward.sample(new HashMap<LVAR,LCONST>(), 
							state, rand)).doubleValue();
					rewards.add(reward);
					accum_reward += cur_discount * reward;
					//System.out.println("Accum reward: " + accum_reward + ", instance._dDiscount: " + instance._dDiscount + 
					//   " / " + (cur_discount * reward) + " / " + reward);
					cur_discount *= instance._dDiscount;
					
					if (SHOW_TIMING)
						System.out.println("**TIME to copy observations & update rewards: " + timer.GetTimeSoFarAndReset());

					stateViz.display(state, h);			
					state.advanceNextState();
					
					if (SHOW_TIMING)
						System.out.println("**TIME to advance state: " + timer.GetTimeSoFarAndReset());
				}
				accum_total_reward += accum_reward;
				long elapsed_time = System.currentTimeMillis() - start_round_time;
				session_elapsed_time += elapsed_time;
				msg = createXMLRoundEnd(requestedInstance, r, accum_reward, h, elapsed_time, clientName);
				if (SHOW_MSG)
					System.out.println("Sending msg:\n" + msg);
				sendOneMessage(osw, msg);
				
				writeToLog(msg);
			}
			msg = createXMLSessionEnd(requestedInstance, accum_total_reward, r, session_elapsed_time, this.clientName, this.id);
			if (SHOW_MSG)
				System.out.println("Sending msg:\n" + msg);
			sendOneMessage(osw, msg);

			writeToLog(msg);

			//need to wait 10 seconds to pretend that we're processing something
//			try {
//				Thread.sleep(10000);
//			}
//			catch (Exception e){}
//			TimeStamp = new java.util.Date().toString();
//			String returnCode = "MultipleSocketServer repsonded at "+ TimeStamp + (char) 3;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("\n>> TERMINATING TRIAL.");
		}
		finally {
			try {
				connection.close();
			}
			catch (IOException e){}
		}
	}
	
	public void writeToLog(String msg) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE + "-" + this.port + ".log" , true));
		bw.write(msg);
		bw.newLine();
		bw.flush();
		bw.close();
	}
	
	HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> copyObserv(
			HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> observ) {
		HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> r = new
		HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>>();
	
		//System.out.println("Observation pvars: " + observ);
		for ( PVAR_NAME pn : observ.keySet() ) {
			HashMap<ArrayList<LCONST>, Object> v = 
				new HashMap<ArrayList<LCONST>, Object>();
			for ( ArrayList<LCONST> aa : observ.get(pn).keySet()) {
				ArrayList<LCONST> raa = new ArrayList<LCONST>();
				for( LCONST lc : aa ) {
					raa.add(lc);
				}
				v.put(raa, observ.get(pn).get(aa));
			}
			r.put(pn, v);
		}
		return r;
	}
	
	void initializeState (RDDL rddl, String requestedInstance) {
		state = new State();
		instance = rddl._tmInstanceNodes.get(requestedInstance);
		nonFluents = null;
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
				instance._alInitState, nonFluents == null ? null : nonFluents._alNonFluents,
				domain._alStateConstraints, domain._exprReward, instance._nNonDefActions);
		
		if ((domain._bPartiallyObserved && state._alObservNames.size() == 0)
				|| (!domain._bPartiallyObserved && state._alObservNames.size() > 0))
			System.err.println("Domain '" + domain._sDomainName
							+ "' partially observed flag and presence of observations mismatched.");

	}
	
	static Object getValue(String pname, String pvalue, State state) {
		TYPE_NAME tname = state._hmPVariables.get(new PVAR_NAME(pname))._sRange;
		
		// TYPE_NAMES are interned so that equality can be tested directly
		// (also helps enforce better type safety)
		if ( TYPE_NAME.INT_TYPE.equals(tname)) {
			return Integer.valueOf(pvalue);
		}
		
		if ( TYPE_NAME.BOOL_TYPE.equals(tname)) {
			return Boolean.valueOf(pvalue);
		}
		
		if ( TYPE_NAME.REAL_TYPE.equals(tname)) {
			return Double.valueOf(pvalue);
		}	
		
		// TODO: should really verify tname is an enum val here by looking up
		// it's definition
		if ( pvalue.startsWith("@") ) {
			// Must be an enum
			return new ENUM_VAL(pvalue);
		} else {			
			// TODO: who calls this method?  Is it only for fluent values?  Or also args?
			// for now we'll allow objects
			return new OBJECT_VAL(pvalue);
		}
		
		//if ( state._hmObject2Consts.containsKey(tname)) {
		//	return new OBJECT_VAL(pvalue);
			//for( LCONST lc : state._hmObject2Consts.get(tname)) {
			//	if ( lc.toString().equals(pvalue)) {
			//		return lc;
			//	}
			//}
		//}
		
		//if ( state._hmTypes.containsKey(tname)) {
		//	return new ENUM_VAL(pvalue);
			//if ( state._hmTypes.get(tname) instanceof ENUM_TYPE_DEF ) {
			//	ENUM_TYPE_DEF etype = (ENUM_TYPE_DEF)state._hmTypes.get(tname);
			//	for ( ENUM_VAL ev : etype._alPossibleValues) {
			//		if ( ev.toString().equals(pvalue)) {
			//			return ev;
			//		}
			//	}
			//}
		//}
		
		//return null;
	}
	
	static ArrayList<PVAR_INST_DEF> processXMLAction(DOMParser p, InputSource isrc,
			State state) throws Exception {
		try {
			//showInputSource(isrc); System.exit(1); // TODO
			p.parse(isrc);
			Element e = p.getDocument().getDocumentElement();
			if (SHOW_XML) {
				System.out.println("Received action msg:");
				printXMLNode(e);
			}
			if ( !e.getNodeName().equals(ACTIONS) ) {
				System.out.println("ERROR: NO ACTIONS NODE");
				System.out.println("Received action msg:");
				printXMLNode(e);
				throw new Exception("ERROR: NO ACTIONS NODE");
			}
			NodeList nl = e.getElementsByTagName(ACTION);
//			System.out.println(nl);
			if(nl != null) { // && nl.getLength() > 0) { // TODO: Scott
				ArrayList<PVAR_INST_DEF> ds = new ArrayList<PVAR_INST_DEF>();
				for(int i = 0 ; i < nl.getLength();i++) {
					Element el = (Element)nl.item(i);
					String name = getTextValue(el, ACTION_NAME).get(0);
					ArrayList<String> args = getTextValue(el, ACTION_ARG);
					ArrayList<LCONST> lcArgs = new ArrayList<LCONST>();
					for( String arg : args ) {
						//System.out.println("arg: " + arg);
						if (arg.startsWith("@"))
							lcArgs.add(new RDDL.ENUM_VAL(arg));
						else 
							lcArgs.add(new RDDL.OBJECT_VAL(arg));
					}
					String pvalue = getTextValue(el, ACTION_VALUE).get(0);
					Object value = getValue(name, pvalue, state);
					PVAR_INST_DEF d = new PVAR_INST_DEF(name, value, lcArgs);
					ds.add(d);
				}
				return ds;
			} else
				return new ArrayList<PVAR_INST_DEF>(); // FYI: May be unreachable. -Scott
			//} else { // TODO: Removed by Scott, NOOP should not be handled differently
			//	nl = e.getElementsByTagName(NOOP);
			//	if ( nl != null && nl.getLength() > 0) {
			//		ArrayList<PVAR_INST_DEF> ds = new ArrayList<PVAR_INST_DEF>();
			//		return ds;
			//	}
			//}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("FATAL SERVER ERROR:\n" + e);
			//t.printStackTrace();
			throw e;
			//System.exit(1);
		}
	}
	
	public static void sendOneMessage (OutputStreamWriter osw, String msg) throws IOException {
//		System.out.println(msg);
		if (NO_XML_HEADING) {
//			System.out.println(msg.substring(39));
			osw.write(msg.substring(39));
		} else {
			osw.write(msg + '\0');
		}
		osw.flush();
	}
	
	public static final int MAX_BYTES = 1048576;
	public static byte[] bytes = new byte[MAX_BYTES];
	
	// Synchronize because this uses a global bytes[] buffer
	public static synchronized InputSource readOneMessage(InputStream isr) {

		try {
		
			int cur_pos = 0; 
			//System.out.println("\n===\n");
			while (true && cur_pos < MAX_BYTES) {
				cur_pos += isr.read( bytes, cur_pos, 1 );
				if (/* Socket closed  */ cur_pos == -1 || 
					/* End of message */ bytes[cur_pos - 1] == '\0')
					break;
				//System.out.print(cur_pos + "[" + Byte.toString(bytes[cur_pos - 1]) + "]");
			}
			//System.out.println("\n===\n");
			
			//while((character = isr.read()) != '\0' && character != -1) { 
			//	message.append((char)character);
			//}
			if (SHOW_MSG) {
				System.out.println("Received message [" + (cur_pos - 1) + "]: **" + new String(bytes, 0, cur_pos - 1) + "**");
			}
			//ByteArrayInputStream bais = new ByteArrayInputStream(message.toString().getBytes());
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes, 0, cur_pos - 1); // No '\0'
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
				NodeList nl = e.getElementsByTagName(NO_XML_HEADER);
				if ( nl.getLength() > 0 ) {
					NO_XML_HEADING = true;
				}
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
//		NodeList nll = ele.getElementsByTagName("*");
		
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			for ( int i= 0; i < nl.getLength(); i++ ) {
				Element el = (Element)nl.item(i);
				returnVal.add(el.getFirstChild().getNodeValue());
			}
		}
		return returnVal;
	}
	
	static String createXMLTurn (State state, int turn, DOMAIN domain,
			HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> observStore) throws Exception {
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

			//System.out.println("PO: " + domain._bPartiallyObserved);
			if( !domain._bPartiallyObserved || observStore != null) {
				for ( PVAR_NAME pn : 
					(domain._bPartiallyObserved 
							? observStore.keySet() 
									: state._state.keySet()) ) {
					//System.out.println(turn + " check2 Partial Observ " + pn +" : "+ domain._bPartiallyObserved);
					
					// No problem to overwrite observations, only ever read from
					if (domain._bPartiallyObserved && observStore != null)
						state._observ.put(pn, observStore.get(pn));
					
					ArrayList<ArrayList<LCONST>> gfluents = state.generateAtoms(pn);			
					for (ArrayList<LCONST> gfluent : gfluents) { 
					//for ( Map.Entry<ArrayList<LCONST>,Object> gfluent : 
					//	(domain._bPartiallyObserved
					//			? observStore.get(pn).entrySet() 
					//					: state._state.get(pn).entrySet())) {
						Element ofEle = dom.createElement(OBSERVED_FLUENT);
						rootEle.appendChild(ofEle);
						Element pName = dom.createElement(FLUENT_NAME);
						Text pTextName = dom.createTextNode(pn.toString());
						pName.appendChild(pTextName);
						ofEle.appendChild(pName);
						for ( LCONST lc : gfluent ) {
							Element pArg = dom.createElement(FLUENT_ARG);
							Text pTextArg = dom.createTextNode(lc.toString());
							pArg.appendChild(pTextArg);
							ofEle.appendChild(pArg);
						}
						Element pValue = dom.createElement(FLUENT_VALUE);
						Object value = state.getPVariableAssign(pn, gfluent);
						if (value == null) {
							System.out.println("STATE:\n" + state);
							throw new Exception("ERROR: Could not retrieve value for " + pn + gfluent.toString());
						}

						Text pTextValue = dom.createTextNode(value.toString());
						pValue.appendChild(pTextValue);
						ofEle.appendChild(pValue);
					}
				}
			} else {
				// No observations (first turn of POMDP)
				Element ofEle = dom.createElement(NULL_OBSERVATIONS);
				rootEle.appendChild(ofEle);
			}
			if (SHOW_XML) {
				printXMLNode(dom);
				System.out.println();
				System.out.flush();
			}
			return(Client.serialize(dom));
			
		} catch (Exception e) {
			System.out.println("FATAL SERVER EXCEPTION: " + e);
			e.printStackTrace();
			throw e;
			//System.exit(1);
			//return null;
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
	
	static String createXMLRoundEnd (String requested_instance, int round, double reward,
			int turnsUsed, long timeUsed, String client_name) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(ROUND_END);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,INSTANCE_NAME, requested_instance);			
			addOneText(dom,rootEle,CLIENT_NAME, client_name + "");
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
	
	static String createXMLSessionEnd(String requested_instance, 
			double reward, int roundsUsed, long timeUsed,
			String clientName, int sessionId) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(SESSION_END);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,INSTANCE_NAME, requested_instance);			
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
	
	///////////////////////////////////////////////////////////////////////
	//                              DEBUG
	///////////////////////////////////////////////////////////////////////
	
	public static void showInputSource(InputSource isrc) {
		InputStream is = isrc.getByteStream();
		byte[] bytes;
		try {
			int size = is.available();
			bytes = new byte[size];
			is.read(bytes);
			System.out.println("==BEGIN IS==");
			System.out.write(bytes, 0, size);
			System.out.println("\n==END IS==");
		} catch (IOException e2) {
			System.out.println(">>> Inputstream error");
			e2.printStackTrace();
		}
	}
	
	public static void printXMLNode(Node n) {
		printXMLNode(n, "", 0);
	}
	public static void printXMLNode(Node n, String prefix, int depth) {
		
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
					printXMLNode(item, prefix, depth+2);
			}
		} catch (Exception e) {
			System.out.println(Pad(depth) + "Exception e: ");
		}
	}
	public static StringBuffer Pad(int depth) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < depth; i++)
			sb.append("  ");
		return sb;
	}
}

