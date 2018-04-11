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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.math3.random.RandomDataGenerator;
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
	
	/**
	 * following is XML definitions
	 */
	public static final String SESSION_REQUEST = "session-request";
	public static final String CLIENT_NAME = "client-name";
	public static final String INSTANCE_NAME = "instance-name";
    public static final String INPUT_LANGUAGE = "input-language";
	public static final String PROBLEM_NAME = "problem-name";
	public static final String SESSION_INIT = "session-init";
	public static final String SESSION_ID = "session-id";
    public static final String TASK_DESC = "task";
	public static final String SESSION_END = "session-end";
	public static final String TOTAL_REWARD = "total-reward";
	public static final String IMMEDIATE_REWARD = "immediate-reward";
	public static final String NUM_ROUNDS = "num-rounds";
	public static final String TIME_ALLOWED = "time-allowed";
	public static final String ROUNDS_USED = "rounds-used";
	
	public static final String CLIENT_INFO = "client-info";
	public static final String CLIENT_HOSTNAME = "client-hostname";
	public static final String CLIENT_IP = "client-ip";
	
	public static final String ROUND_REQUEST = "round-request";
    public static final String EXECUTE_POLICY = "execute-policy";
	public static final String ROUND_INIT = "round-init";
	public static final String ROUND_NUM = "round-num";
	public static final String ROUND_LEFT = "round-left";
	public static final String TIME_LEFT = "time-left";
	public static final String ROUND_END = "round-end";
	public static final String ROUND_REWARD = "round-reward";
	public static final String TURNS_USED = "turns-used";
	public static final String TIME_USED = "time-used";

	public static final String RESOURCE_REQUEST = "resource-request";
	public static final String RESOURCE_NOTIFICATION = "resource-notification";
	public static final String MEMORY_LEFT = "memory-left";

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
	private RDDL rddl = null;
	private static int ID = 0;
	private static int DEFAULT_NUM_ROUNDS = 30;
	private static long DEFAULT_TIME_ALLOWED = 1080000; // milliseconds = 18 minutes
	private static boolean USE_TIMEOUT = true;
	private static boolean INDIVIDUAL_SESSION = false;
	private static String LOG_FILE = "rddl";
	private static boolean MONITOR_EXECUTION = false;
	private static String SERVER_FILES_DIR = "";
	private static String CLIENT_FILES_DIR = "";
    
	public int port;
	public int id;
	public String clientName = null;
	public String requestedInstance = null;
	public RandomDataGenerator rand;
	public boolean executePolicy = true;
	public String inputLanguage = "rddl";
	public int numSimulations = 0;
	
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
			System.out.println("usage: rddlfilename-or-dir (optional) portnumber num-rounds random-seed use-timeout individual-session log-folder monitor-execution state-viz-class-name");
			System.out.println("\nexample 1: Server rddlfilename-or-dir");
			System.out.println("example 2: Server rddlfilename-or-dir 2323");
			System.out.println("example 3: Server rddlfilename-or-dir 2323 100 0 0 1 experiments/experiment23/ 1 rddl.viz.GenericScreenDisplay");
			System.exit(1);
		}
				
		try {
			// Load RDDL files
			SERVER_FILES_DIR = new String(args[0]);
			CLIENT_FILES_DIR = new String(args[0]);

			File[] subDirs = new File(args[0]).listFiles(File::isDirectory);
			// Check if there are subdirectories called "client" and "server"
			for (File subDir : subDirs) {
				if (subDir.getName().equals("server")) {
				SERVER_FILES_DIR =  new String(subDir.getPath());
				} else if (subDir.getName().equals("client")) {
					CLIENT_FILES_DIR =  new String(subDir.getPath());
				}
			}

			RDDL rddl = new RDDL(SERVER_FILES_DIR);

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
				if (args[4].equals("1")) {
					INDIVIDUAL_SESSION = true;
				}
			}
			if (args.length > 5) {
				if (args[5].equals("0")) {
					USE_TIMEOUT = false;
				} else {
					USE_TIMEOUT = true;
					DEFAULT_TIME_ALLOWED = Integer.valueOf(args[5]) * 1000;
				}
			}
			if (args.length > 6) {
				LOG_FILE = args[6] + "/logs";
			}
			if (args.length > 7) {
				assert(args[7].equals("0") || args[7].equals("1"));
				if (args[7].equals("1")) {
					MONITOR_EXECUTION = true;
				}
			}
			if (args.length > 8) {
				state_viz = (StateViz)Class.forName(args[8]).newInstance();
			}
			System.out.println("RDDL Server Initialized");
			while (true) {
				Socket connection = socket1.accept();
				RandomDataGenerator rdg = new RandomDataGenerator();
				rdg.reSeed(rand_seed + ID); // Ensures predictable but different seed on every session if a single client connects and all session requests run in same order
				Runnable runnable = new Server(connection, ++ID, rddl, state_viz, port, rdg);
				Thread thread = new Thread(runnable);
				thread.start();
				if (INDIVIDUAL_SESSION) {
					break;
				}
			}
			System.out.println("Single client has connected, no more are accepted.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
	}
	Server (Socket s, int i, RDDL rddl, StateViz state_viz, int port, RandomDataGenerator rgen) {
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
		long timeAllowed = DEFAULT_TIME_ALLOWED;
				
		try {
			
			// Log client host name and IP address
			InetAddress ia = connection.getInetAddress();
			String client_hostname = ia.getCanonicalHostName();
			String client_IP = ia.getHostAddress();
			long start_time = System.currentTimeMillis();
			System.out.println("Connection from client at address " + client_hostname + " / " + client_IP);
			writeToLog(createClientHostMessage(client_hostname, client_IP));
			
			// Begin communication protocol from PROTOCOL.txt
			BufferedInputStream isr = new BufferedInputStream(connection.getInputStream());
			InputSource isrc = readOneMessage(isr);
			requestedInstance = null;
			processXMLSessionRequest(p, isrc, this);
			System.out.println("Client name: " + clientName);
			System.out.println("Instance requested: " + requestedInstance);
	
			if (!rddl._tmInstanceNodes.containsKey(requestedInstance)) {
				System.out.println("Instance name '" + requestedInstance + "' not found.");
				return;
			}

			BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
			OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
			String msg = createXMLSessionInit(numRounds, timeAllowed, this);
			boolean OUT_OF_TIME = false;
			sendOneMessage(osw,msg);			

			initializeState(rddl, requestedInstance);
			//System.out.println("STATE:\n" + state);
			
			double accum_total_reward = 0d;
			ArrayList<Double> rewards = new ArrayList<Double>();
			int r = 0;
			for( ; r < numRounds && !OUT_OF_TIME; r++ ) {
			boolean roundRequested = false;
			while (!roundRequested) {
				isrc = readOneMessage(isr);
				roundRequested =  processXMLRoundRequest(p, isrc, this);
				if (!roundRequested) {
					msg = createXMLResourceNotification(timeAllowed - System.currentTimeMillis() + start_time);
					sendOneMessage(osw,msg);
				}
			}

			if (!executePolicy) {
					r--;
				}
				
				resetState();
				msg = createXMLRoundInit(r+1, numRounds, timeAllowed - System.currentTimeMillis() + start_time, timeAllowed);
			sendOneMessage(osw,msg);
				
				if (executePolicy) {
					System.out.println("Round " + (r+1) + " / " + numRounds + ", time remaining: " + (timeAllowed - System.currentTimeMillis() + start_time));
					if (SHOW_MEMORY_USAGE) {
						System.out.print("[ Memory usage: " + 
							_df.format((RUNTIME.totalMemory() - RUNTIME.freeMemory())/1e6d) + "Mb / " + 
							_df.format(RUNTIME.totalMemory()/1e6d) + "Mb" + 
							" = " + _df.format(((double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 
											   (double) RUNTIME.totalMemory())) + " ]\n");
					}
				}

				double immediate_reward = 0.0d;
				double accum_reward = 0.0d;
				double cur_discount = 1.0d;
				int h = 0;
				HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> observStore =null;
				while (true) {
					Timer timer = new Timer();
				
					//if ( observStore != null) {
					//	for ( PVAR_NAME pn : observStore.keySet() ) {
					//		System.out.println("check3 " + pn);
					//		for( ArrayList<LCONST> aa : observStore.get(pn).keySet()) {
					//			System.out.println("check3 :" + aa + ": " + observStore.get(pn).get(aa));
					//		}
					//	}
					//}
					msg = createXMLTurn(state, h+1, domain, observStore, timeAllowed - System.currentTimeMillis() + start_time, immediate_reward);
					
					if (SHOW_TIMING)
						System.out.println("**TIME to create XML turn: " + timer.GetTimeSoFarAndReset());
					
					if (SHOW_MSG)
						System.out.println("Sending msg:\n" + msg);

					sendOneMessage(osw,msg);

					ArrayList<PVAR_INST_DEF> ds = null;
					while (ds == null) {
						isrc = readOneMessage(isr);
						if (isrc == null) {
							throw new Exception("FATAL SERVER EXCEPTION: EMPTY CLIENT MESSAGE");
						}

						ds = processXMLAction(p,isrc,state);
						if ( ds == null ) {
							msg = createXMLResourceNotification(timeAllowed - System.currentTimeMillis() + start_time);
							sendOneMessage(osw,msg);
						}
					}

					// Check action preconditions (also checks maxNonDefActions)
					try {
						state.checkStateActionConstraints(ds);
					} catch (Exception e) {
						System.out.println("TRIAL ERROR -- ACTION NOT APPLICABLE:\n" + e);
						if (INDIVIDUAL_SESSION) {
							try {
								connection.close();
							} catch (IOException ioe){}
							System.exit(1);
						}
						break;
					}

					//Sungwook: this is not required.  -Scott
					//if ( h== 0 && domain._bPartiallyObserved && ds.size() != 0) {
					//	System.err.println("the first action for partial observable domain should be noop");
					//}
					if (SHOW_ACTIONS && executePolicy) {
						boolean suppress_object_cast_temp = RDDL.SUPPRESS_OBJECT_CAST;
						RDDL.SUPPRESS_OBJECT_CAST = true;
						System.out.println("** Actions received: " + ds);
						RDDL.SUPPRESS_OBJECT_CAST = suppress_object_cast_temp;
					}

					try {
						state.computeNextState(ds, rand);
					} catch (Exception ee) {
						System.out.println("FATAL SERVER EXCEPTION:\n" + ee);
						//ee.printStackTrace();
						if (INDIVIDUAL_SESSION) {
							try {
								connection.close();
							} catch (IOException ioe){}
							System.exit(1);
						}
						throw ee;
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
					immediate_reward = ((Number)domain._exprReward.sample(new HashMap<LVAR,LCONST>(), 
							state, rand)).doubleValue();
					rewards.add(immediate_reward);
					accum_reward += cur_discount * immediate_reward;
					//System.out.println("Accum reward: " + accum_reward + ", instance._dDiscount: " + instance._dDiscount + 
					//   " / " + (cur_discount * reward) + " / " + reward);
					cur_discount *= instance._dDiscount;
					
					if (SHOW_TIMING)
						System.out.println("**TIME to copy observations & update rewards: " + timer.GetTimeSoFarAndReset());

					stateViz.display(state, h);			
					state.advanceNextState();
					
					if (SHOW_TIMING)
						System.out.println("**TIME to advance state: " + timer.GetTimeSoFarAndReset());
										
					// Scott: Update 2014 to check for out of time... this can trigger
					//        an early round end
					OUT_OF_TIME = ((System.currentTimeMillis() - start_time) > timeAllowed) && USE_TIMEOUT;
					h++;

					// Thomas: Update 2018 to allow simulation of SSPs
					if (OUT_OF_TIME) {
						// System.out.println("OUT OF TIME!");
						break;
					}
					if ((instance._termCond == null) && (h == instance._nHorizon)) {
						// System.out.println("Horizon reached");
						break;
					}
					if ((instance._termCond != null) && state.checkTerminationCondition(instance._termCond)) {
						// System.out.println("Terminal state reached");
						break;
					}
				}
				if (executePolicy) {
					accum_total_reward += accum_reward;
					System.out.println("** Round reward: " + accum_reward);
				}
				msg = createXMLRoundEnd(requestedInstance, r, accum_reward, h,
							timeAllowed - System.currentTimeMillis() + start_time,
                                                        clientName, immediate_reward);
				if (SHOW_MSG)
					System.out.println("Sending msg:\n" + msg);
				sendOneMessage(osw, msg);
				
				writeToLog(msg);
			}
			msg = createXMLSessionEnd(requestedInstance, accum_total_reward, r,
						  timeAllowed - System.currentTimeMillis() + start_time, this.clientName, this.id);
			if (SHOW_MSG)
				System.out.println("Sending msg:\n" + msg);
			sendOneMessage(osw, msg);

			writeToLog(msg);

			System.out.println("Session finished successfully: " + clientName);
			System.out.println("Time left: " + (timeAllowed - System.currentTimeMillis() + start_time));
			System.out.println("Number of simulations: " + numSimulations);
			System.out.println("Number of runs: " + numRounds);
			System.out.println("Accumulated reward: " + (accum_total_reward));
			System.out.println("Average reward: " + (accum_total_reward / numRounds));

			if (INDIVIDUAL_SESSION) {
				try {
					connection.close();
				} catch (IOException ioe){}
				System.exit(0);
			}

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
			if (INDIVIDUAL_SESSION) {
				try {
					connection.close();
				} catch (IOException ioe){}
				System.exit(1);
			}
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
                                if (INDIVIDUAL_SESSION) {
                        		try {
                        			connection.close();
                        		}
                        		catch (IOException ioe){}
                        		System.exit(1);
                        	}
			}
		}
	}
	
	void resetState () {
		state.init(domain._hmObjects, nonFluents != null ? nonFluents._hmObjects : null, instance._hmObjects, 
				domain._hmTypes, domain._hmPVariables, domain._hmCPF,
				instance._alInitState, nonFluents == null ? new ArrayList<PVAR_INST_DEF>() : nonFluents._alNonFluents, instance._alNonFluents,
				domain._alStateConstraints, domain._alActionPreconditions, domain._alStateInvariants, 
				domain._exprReward, instance._nNonDefActions);
		
		if ((domain._bPartiallyObserved && state._alObservNames.size() == 0)
				|| (!domain._bPartiallyObserved && state._alObservNames.size() > 0)) {
			boolean observations_present = (state._alObservNames.size() > 0);
			System.err.println("WARNING: Domain '" + domain._sDomainName
							+ "' partially observed (PO) flag and presence of observations mismatched.\nSetting PO flag = " + observations_present + ".");
			domain._bPartiallyObserved = observations_present;
		}

	}
	
	static Object getValue(String pname, String pvalue, State state) {
		
		// Get the fluent value's range
		TYPE_NAME tname = state._hmPVariables.get(new PVAR_NAME(pname))._typeRange;
		
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
		
		// TODO: handle vectors <>
		// TODO: are enum int values handled correctly?  need an @ 
		
		// This allows object vals
		// TODO: should really verify tname is an enum val here by looking up it's definition
		if ( pvalue.startsWith("@") ) {
			// Must be an enum
			return new ENUM_VAL(pvalue);
		} else {			
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
			if ( e.getNodeName().equals(RESOURCE_REQUEST) ) {
				return null;
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
						else // TODO $ <> (forgiving)... done$
							lcArgs.add(new RDDL.OBJECT_VAL(arg));
					}
					String pvalue = getTextValue(el, ACTION_VALUE).get(0);
					Object value = getValue(name, pvalue, state); // TODO $ <> (forgiving)... done$
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
	
	public static final int MAX_BYTES = 10485760;
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
	
	static public String createClientHostMessage(String client_hostname, String client_IP) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(CLIENT_INFO);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,CLIENT_HOSTNAME, client_hostname);
			addOneText(dom,rootEle,CLIENT_IP, client_IP);
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
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

			INSTANCE instance = server.rddl._tmInstanceNodes.get(server.requestedInstance);
			DOMAIN domain = server.rddl._tmDomainNodes.get(instance._sDomain);

			String domainFile = CLIENT_FILES_DIR + "/" + domain._sFileName + "." + server.inputLanguage;
			String instanceFile = CLIENT_FILES_DIR + "/" + instance._sFileName + "." + server.inputLanguage;

			// NONFLUENTS nonFluents = null;
			// if (instance._sNonFluents != null) {
			//     nonFluents = server.rddl._tmNonFluentNodes.get(instance._sNonFluents);
			// }
			StringBuilder task = new StringBuilder(new String(Files.readAllBytes(Paths.get(domainFile))));
			// if (nonFluents != null) {
			// task.append(System.getProperty("line.separator"));
			// task.append(System.getProperty("line.separator"));
 			// task.append(new String(Files.readAllBytes(Paths.get(nonFluents._sFileName))));
			// }
			task.append(System.getProperty("line.separator"));
			task.append(System.getProperty("line.separator"));
			task.append(new String(Files.readAllBytes(Paths.get(instanceFile))));
			task.append(System.getProperty("line.separator"));

			// We have to send the description encoded to Base64 as "<"
			// and ">" signs are replaced in XML text by &lt; and &gt;,
			// respectively. This seems the cleanest solution, even
			// though it requires the client to decode the description.
			byte[] encodedBytes = Base64.getEncoder().encode(task.toString().getBytes());

			addOneText(dom, rootEle, TASK_DESC, new String(encodedBytes));
			addOneText(dom, rootEle, SESSION_ID, server.id + "");
			addOneText(dom, rootEle, NUM_ROUNDS, numRounds + "");
			addOneText(dom, rootEle, TIME_ALLOWED, timeAllowed + "");
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
				ArrayList<String> lang = getTextValue(e, INPUT_LANGUAGE);
				if (lang != null && lang.size() > 0) {
					if (lang.get(0).trim().equals("pddl")) {
						server.inputLanguage = "pddl";
 					}
				}
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
	
	static boolean processXMLRoundRequest (DOMParser p, InputSource isrc,
                                           Server server) {
        try {
			p.parse(isrc);
			Element e = p.getDocument().getDocumentElement();
			if ( e.getNodeName().equals(ROUND_REQUEST) ) {
				if (MONITOR_EXECUTION) {
					// System.out.println("Monitoring execution!");
					String executePolicyString = "no";
					ArrayList<String> exec_pol = getTextValue(e, EXECUTE_POLICY);
					if (exec_pol != null && exec_pol.size() > 0) {
						executePolicyString = exec_pol.get(0).trim();
					}
					if (executePolicyString.equals("yes")) {
						server.executePolicy = true;
					} else {
						assert(executePolicyString.equals("no"));
						server.executePolicy = false;
						server.numSimulations++;
						// System.out.println("Do not execute the policy!");
					}
				}
				return true;			
			} else if ( e.getNodeName().equals(RESOURCE_REQUEST) ) {
				return false;
			}
			System.out.println("Illegal message from server: " + e.getNodeName());
			System.out.println("round request or time left request expected");
			System.exit(1);
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
                                     HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> observStore,
                                     double timeLeft, double immediateReward) throws Exception {
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
                        Element timeElem = dom.createElement(TIME_LEFT);
                        Text textTimeElem = dom.createTextNode(timeLeft + "");
                        timeElem.appendChild(textTimeElem);
                        rootEle.appendChild(timeElem);
                        Element immediateRewardElem = dom.createElement(IMMEDIATE_REWARD);
                        Text textImmediateRewardElem = dom.createTextNode(immediateReward + "");
                        immediateRewardElem.appendChild(textImmediateRewardElem);
                        rootEle.appendChild(immediateRewardElem);

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
							Text pTextArg = dom.createTextNode(lc.toSuppString()); // TODO $ <>... done$
							pArg.appendChild(pTextArg);
							ofEle.appendChild(pArg);
						}
						Element pValue = dom.createElement(FLUENT_VALUE);
						Object value = state.getPVariableAssign(pn, gfluent);
						if (value == null) {
							System.out.println("STATE:\n" + state);
							throw new Exception("ERROR: Could not retrieve value for " + pn + gfluent.toString());
						}

						Text pTextValue = value instanceof LCONST 
								? dom.createTextNode( ((LCONST)value).toSuppString())
								: dom.createTextNode( value.toString() ); // TODO $ <>... done$
						// dom.createTextNode(value.toString()); // TODO $ <>
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

	static String createXMLResourceNotification(double timeLeft) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(RESOURCE_NOTIFICATION);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,TIME_LEFT, timeLeft + "");
			// TODO: memory left is not implemented yet
			addOneText(dom,rootEle,MEMORY_LEFT, "enough");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static String createXMLRoundInit (int round, int numRounds, double timeLeft,
			double timeAllowed) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement(ROUND_INIT);
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,ROUND_NUM, round + "");
			addOneText(dom,rootEle,ROUND_LEFT, (numRounds - round) + "");
			addOneText(dom,rootEle,TIME_LEFT, timeLeft + "");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static String createXMLRoundEnd (String requested_instance, int round, double reward,
					 int turnsUsed, long timeLeft, String client_name, double immediateReward) {
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
			addOneText(dom,rootEle, TIME_LEFT, timeLeft + "");
                        addOneText(dom,rootEle, IMMEDIATE_REWARD, immediateReward + "");
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
					  double reward, int roundsUsed, long timeLeft, 
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
			addOneText(dom,rootEle,CLIENT_NAME, clientName + "");
			addOneText(dom,rootEle,SESSION_ID, sessionId + "");
			addOneText(dom,rootEle,TIME_LEFT, timeLeft + "");
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
		} catch (IOException e) {
			System.out.println(">>> Inputstream error");
			e.printStackTrace();
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

