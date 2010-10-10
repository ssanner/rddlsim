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
import java.io.ByteArrayInputStream;
import java.io.File;
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
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;
import rddl.State;
import rddl.parser.parser;
import rddl.policy.Policy;
import rddl.policy.RandomBoolPolicy;
import rddl.viz.GenericScreenDisplay;
import rddl.viz.NullScreenDisplay;
import rddl.viz.StateViz;

public class Server implements Runnable {
	
	public static final boolean SHOW_MEMORY_USAGE = true;
	public static final Runtime RUNTIME = Runtime.getRuntime();
	private static DecimalFormat _df = new DecimalFormat("0.##");
	
	private static int PORT_NUMBER = 2323;
	private Socket connection;
	private String TimeStamp;
	private RDDL rddl = null;
	private static int ID = 0;
	private static int NUM_ROUND = 30;
	private int id;
	public static Random rand = new Random(1);
	
	public State      state;
	public INSTANCE   instance;
	public NONFLUENTS nonFluents;
	public DOMAIN     domain;
	public StateViz   stateViz;
	
	public static void main(String[] args) {
		RDDL rddl;
		try {
			rddl = parser.parse(new File("files/rddl/test/sysadmin.rddl"));
			// Get first instance name in file and create a simulator

			ServerSocket socket1 = new ServerSocket(PORT_NUMBER);
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
		try {
			BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
			InputStreamReader isr = new InputStreamReader(is);
			int character;
			StringBuffer message = new StringBuffer();
			while((character = isr.read()) != (char)3) {
				message.append((char)character);
			}
			String requestedInstance = getXMLInstanceName(p, message);
			System.out.println(requestedInstance);
			initializeState(requestedInstance);
			stateViz = new NullScreenDisplay(false); // GenericScreenDisplay(true)
			
			BufferedOutputStream os = new BufferedOutputStream(connection.getOutputStream());
			OutputStreamWriter osw = new OutputStreamWriter(os, "US-ASCII");
			double accum_total_reward = 0;
			for( int r = 0; r < NUM_ROUND; r++ ) {
				resetState();
				osw.write(createXMLRound(r) + (char)3);
				osw.flush();
				
				if (SHOW_MEMORY_USAGE)
					System.out.print("[ Memory usage: " + 
							_df.format((RUNTIME.totalMemory() - RUNTIME.freeMemory())/1e6d) + "Mb / " + 
							_df.format(RUNTIME.totalMemory()/1e6d) + "Mb" + 
							" = " + _df.format(((double) (RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 
											   (double) RUNTIME.totalMemory())) + " ]\n");
				
				// NOTE: should pre-initialize arrays when constantly adding to them
				ArrayList<Double> rewards = new ArrayList<Double>(NUM_ROUND * instance._nHorizon);
				double accum_reward = 0.0d;
				double cur_discount = 1.0d;
				for( int h = 0; h < instance._nHorizon; h++ ) {
					
					String returnString = createXMLTurn(state, h+1);
					osw.write(returnString + (char)3);
					osw.flush();				

					message = new StringBuffer();
					while((character = isr.read()) != (char)3) {
						message.append((char)character);
					}
					PVAR_INST_DEF d = getXMLAction(p,message);
					System.out.println(d);
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
				osw.write(createXMLEndRound(r, accum_reward) + (char)3);
				osw.flush();
			}
			osw.write(createXMLEndTest(accum_total_reward) + (char)3);
			osw.flush();
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
	
	static PVAR_INST_DEF getXMLAction(DOMParser p, StringBuffer message) {
		ByteArrayInputStream bais = new ByteArrayInputStream(message.toString().getBytes());
		InputSource isrc = new InputSource();
		isrc.setByteStream(bais);
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
	
	static String getXMLInstanceName (DOMParser p, StringBuffer message) {
		ByteArrayInputStream bais = new ByteArrayInputStream(message.toString().getBytes());
		InputSource isrc = new InputSource();
        isrc.setByteStream(bais);
        try {
			p.parse(isrc);
			Element e = p.getDocument().getDocumentElement();
			if ( e.getNodeName().equals("request-test") ) {
				return getTextValue(e, "problem-name").get(0);			
			}
			return null;
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
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
	
	static String createXMLTurn (State state, int turn) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement("turn");
			dom.appendChild(rootEle);
			Element turnNum = dom.createElement("turn-num");
			Text textTurnNum = dom.createTextNode(turn + "");
			turnNum.appendChild(textTurnNum);
			rootEle.appendChild(turnNum);
			
			for ( PVAR_NAME pn : state._observ.keySet() ) {
				for ( ArrayList<LCONST> lcs : state._observ.get(pn).keySet() ) {
					Element ofEle = dom.createElement("observation-fluent");
					rootEle.appendChild(ofEle);

					Element pName = dom.createElement("fluent-name");
					Text pTextName = dom.createTextNode(pn.toString());
					pName.appendChild(pTextName);
					ofEle.appendChild(pName);
					for ( LCONST lc : lcs ) {
						Element pArg = dom.createElement("fluent-arg");
						Text pTextArg = dom.createTextNode(lc.toString());
						pArg.appendChild(pTextArg);
						ofEle.appendChild(pArg);
					}
					Element pValue = dom.createElement("fluent-value");
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
	
	static String createXMLRound (int round) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement("round");
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,"round-num", round + "");
			addOneText(dom,rootEle,"round-total-num", NUM_ROUND + "");
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static String createXMLEndRound (int round, double reward) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement("round-end");
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,"round-num", round + "");
			addOneText(dom,rootEle,"round-reward", reward + "");			
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	static void addOneText(Document dom, Element p, 
			String name, String value) {
		Element e = dom.createElement(name);
		Text text = dom.createTextNode(value);
		e.appendChild(text);
		p.appendChild(e);
	}
	
	static String createXMLEndTest(double reward) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.newDocument();
			Element rootEle = dom.createElement("end-test");
			dom.appendChild(rootEle);
			addOneText(dom,rootEle,"reward", reward + "");			
			return Client.serialize(dom);
		}
		catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
}

