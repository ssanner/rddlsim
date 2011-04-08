/** Document implementation for TREC Interactive track data (TREC Disks 4-5)
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package rddl.evaluate;

import java.io.*;
import java.text.DecimalFormat;
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

import rddl.parser.parser;

import util.MapList;
import util.Pair;

import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBaseIterators.ChildrenIterator;

public class FinalEval {
	
	public final static int NUM_EXPECTED_TRIALS = 30;
	public final static String RANDOM_POLICY_NAME = "RandomBoolPolicy";
	public final static String NOOP_POLICY_NAME   = "NoopBoolPolicy";
	
	public static DecimalFormat df = new DecimalFormat("#.##");

	public static HashMap<String,Double> _minAvg       = new HashMap<String,Double>();
	public static HashMap<String,String> _minAvgName   = new HashMap<String,String>();
	public static HashMap<String,Double> _minAvgStdErr = new HashMap<String,Double>();
	
	/**
	 * @param args
	 */
	public static void Eval(File f) throws Exception {
		
		HashMap<String,MapList> client2data = new HashMap<String,MapList>();
		
		if (f.isDirectory()) {
			for (File f2 : f.listFiles())
				if (f2.getName().endsWith(".log")) {
					System.out.println("Loading log file: " + f2 + "...");
					LogReader lr = new LogReader(f2);
					System.out.println(lr._client2data);
					client2data.putAll(lr._client2data);
				}
		} else
			usage();
		
		HashMap<Pair<String,String>,Integer> instance2count   = new HashMap<Pair<String,String>,Integer>();
		HashMap<Pair<String,String>,Double>  instance2minR    = new HashMap<Pair<String,String>,Double>();
		HashMap<Pair<String,String>,Double>  instance2maxR    = new HashMap<Pair<String,String>,Double>();
		HashMap<Pair<String,String>,Double>  instance2avg     = new HashMap<Pair<String,String>,Double>();
		HashMap<Pair<String,String>,Double>  instance2stderr  = new HashMap<Pair<String,String>,Double>();

		TreeSet<String> instances = new TreeSet<String>();
		TreeSet<String> clients   = new TreeSet<String>();

		for (Map.Entry<String, MapList> e : client2data.entrySet()) {
			String client_name = e.getKey();
			if (client_name == null) {
				System.err.println("Client name was null for " + e + "... skipping");
				continue;
			}
			clients.add(client_name);
			
			for (Object o : e.getValue().keySet()) {
				String instance_name = (String)o;
				instances.add(instance_name);

				ArrayList<Double> rewards = new ArrayList<Double>(e.getValue().getValues(instance_name));
				double avg = Statistics.Avg(rewards);
				double stderr = Statistics.StdError95(rewards);
				
				if (client_name.equalsIgnoreCase(RANDOM_POLICY_NAME)
						|| client_name.equalsIgnoreCase(NOOP_POLICY_NAME)) {
					
					if (rewards.size() != NUM_EXPECTED_TRIALS) {
						System.err.println("INCORRECT NUMBER OF TRIALS [" + rewards.size() + "/"
								+ NUM_EXPECTED_TRIALS + "]!!!");
						System.exit(1);
					}
				
					Double min_avg = _minAvg.get(instance_name);
				 
					if (min_avg == null || avg < min_avg) {
						_minAvg.put(instance_name, avg);
						_minAvgName.put(instance_name, client_name);
						_minAvgStdErr.put(instance_name, stderr);
					}
				}
				
				Pair<String,String> key = new Pair<String,String>(client_name,instance_name);
				instance2count.put(key,  rewards.size());
				instance2minR.put(key,   Statistics.Min(rewards));
				instance2maxR.put(key,   Statistics.Max(rewards));
				instance2avg.put(key,    avg);
				instance2stderr.put(key, stderr);
			}
		}
		
		// TODO: Show details like variable count for domains, largest CPT, tree width, etc?
		//       Probably another analysis file.
		
		// TODO: Pad results for less than NUM_EXPECTED_TRIALS
		
		// TODO: Check for an average lower than min and change to min
		
		// TODO: Raw and normalized scores
		
		// TODO: SimpleEval should restrict minimum to NOOP and Random... rename to ComputeNorm
		
		// TODO: Read normalization constants from ComputeNorm
		
		// Track results for
		for (String instance_name : instances) {
			System.out.print(instance_name + "\t");
			for (String client_name : clients) {
				Pair<String,String> key = new Pair<String,String>(client_name,instance_name);
				Integer count   = instance2count.get(key);
				Double min_val = instance2minR.get(key);
				Double max_val = instance2maxR.get(key);
				Double avg     = instance2maxR.get(key);
				Double stderr  = instance2stderr.get(key);
				System.out.print(client_name + "\t" + count + "\t" + format(avg) + "\t+/- " + format(stderr) + "\t[ " + format(min_val) + "\t" + format(max_val) + " ]\t");
			}
			System.out.println();
		}
		
		// New comparator for instance sorting by __#
		
		// Track aggregate results per instance... remove __#
	}
	
	public static String format(Double d) {
		if (d == null)
			return null;
		else
			return df.format(d);
	}
	
	public static void main(String[] args) throws Exception {
		
		//if (args.length != 1)
		//	usage();
		
		Eval(new File("TestComp"));
	}
	
	public static void usage() {
		System.out.println("\nUsage: <directory of RDDL .log files>");
		System.exit(1);
	}
}
