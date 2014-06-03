/** Derives min and max normalizing constants for use by FinalEval in determining
 *  competitors final normalized scores.  Outputs 'min_max_norm_constants.txt'.
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

import util.DocUtils;
import util.MapList;

public class MinMaxEval {
	
	public static final String MIN_MAX_FILE = "min_max_norm_constants.txt";
	
	public static HashSet<String> BASELINE_POLICIES = new HashSet<String>();
	static {
		BASELINE_POLICIES.add("RandomBoolPolicy".toLowerCase());
		BASELINE_POLICIES.add("RandomPolicy".toLowerCase());
		BASELINE_POLICIES.add("NoopPolicy".toLowerCase());
	}
	
	public static final String IGNORE_CLIENT_LIST_FILE = "IGNORE_CLIENT_LIST.txt";
	public static HashSet<String> IGNORE_POLICIES = new HashSet<String>();
	
	public static DecimalFormat df = new DecimalFormat("#.##");
	
	/**
	 * @param args
	 */
	public static void Eval(File f) throws Exception {
		
		HashMap<String,MapList> client2data = new HashMap<String,MapList>();
		HashSet<String> client_names = new HashSet<String>();
		
		if (f.isDirectory()) {
			
			// Add additional client names to ignore from IGNORE_LIST_FILE
			String s_ignore = DocUtils.ReadFile(new File(f.getPath() + File.separator + IGNORE_CLIENT_LIST_FILE));
			for (String s : s_ignore.split("[\\s]")) {
				IGNORE_POLICIES.add(s.trim());
				System.out.println("Ignoring: '" + s.trim() + "'");
			}

			// Read all log files
			for (File f2 : f.listFiles())
				if (f2.getName().endsWith(".log")) {
					System.out.println("Loading log file: " + f2 + "...");
					LogReader lr = new LogReader(f2);
					System.out.println(lr._client2data);
					client2data.putAll(lr._client2data);
				}
		} else
			FinalEval.usage();
		
		HashMap<String,Integer> instance2count = new HashMap<String,Integer>();
		HashMap<String,Double>  instance2minR  = new HashMap<String,Double>();
		HashMap<String,Double>  instance2minRNoopRandom = new HashMap<String,Double>();
		HashMap<String,Double>  instance2maxR  = new HashMap<String,Double>();
		HashMap<String,String>  instance2minRName    = new HashMap<String,String>();
		HashMap<String,String>  instance2minRNameNoopRandom    = new HashMap<String,String>();
		HashMap<String,String>  instance2maxRName    = new HashMap<String,String>();
		HashMap<String,Double>  instance2minRStdErr  = new HashMap<String,Double>();
		HashMap<String,Double>  instance2minRStdErrNoopRandom  = new HashMap<String,Double>();
		HashMap<String,Double>  instance2maxRStdErr  = new HashMap<String,Double>();

		TreeSet<String> instances = new TreeSet<String>(new InstNameComparator());
		for (Map.Entry<String, MapList> e : client2data.entrySet()) {
			String client_name = e.getKey();
			
			if (IGNORE_POLICIES.contains(client_name)) {
				continue;
			}
			client_names.add(client_name);
			
			HashSet<String> instances_encountered = new HashSet<String>();
			for (Object o : e.getValue().keySet()) {
				String instance_name = (String)o;
				if (instance_name.endsWith("__trial_time"))
					continue;
				Integer count = instance2count.get(instance_name);
				if (count == null) {
					// This instance has never been encountered before
					count = 0;
					instance2minR.put(instance_name, Double.MAX_VALUE);
					instance2maxR.put(instance_name, -Double.MAX_VALUE);
					instance2minRNoopRandom.put(instance_name, -Double.MAX_VALUE);
				}
				instance2count.put(instance_name, count + 1);
				instances_encountered.add(instance_name);
				
				ArrayList<Double> rewards = new ArrayList<Double>(e.getValue().getValues(instance_name));
				ArrayList<Long>   times   = new ArrayList<Long>((e.getValue().getValues(instance_name + "__trial_time")));
				
				///////////////////////////////////////////////////////////////////////////////////////////////////////
				
				// Get up to last FinalEval.NUM_EXPECTED_TRIALS within cumulative time limit of FinalEval.TIME_ALLOWED
				ArrayList<Double> last_rewards_in_trial_and_time_limit = new ArrayList<Double>();
				ArrayList<Long> last_times_in_trial_and_time_limit = new ArrayList<Long>();
				long cumulative_time = 0; 
				for (int i = rewards.size() - 1 /*end*/; i >= Math.max(0, rewards.size() - FinalEval.NUM_EXPECTED_TRIALS) /*e.g., max(end-30,0)*/; i--) {
					
					double rew = rewards.get(i);
					long time  = times.get(i);
					if (FinalEval.ENFORCE_TIME_LIMIT && cumulative_time + time > FinalEval.TIME_ALLOWED) {
						System.err.println("TIME LIMIT (" + (cumulative_time + time) + "/" + FinalEval.TIME_ALLOWED + ") EXCEEDED on " + instance_name + 
								           " by " + client_name + ", using last " + last_rewards_in_trial_and_time_limit.size() + " / " + rewards.size() + " trials.");
						break;
					}
					
					last_rewards_in_trial_and_time_limit.add(rew);
					last_times_in_trial_and_time_limit.add(time);
					cumulative_time += time;
				}
				rewards.clear(); // Need to modify in place since external references to this object 
				rewards.addAll(last_rewards_in_trial_and_time_limit); // Replace with the subset within the time limit
				times.clear(); // Need to modify in place since external references to this object
				times.addAll(last_times_in_trial_and_time_limit);
				
//				if (rewards.size() > FinalEval.NUM_EXPECTED_TRIALS) {
//					// Take the last NUM_EXPECTED_TRIALS
//					Object[] temp_rewards = rewards.toArray();
//					rewards.clear();
//					for (int i = temp_rewards.length - FinalEval.NUM_EXPECTED_TRIALS; i < temp_rewards.length; i++)
//						rewards.add((Double)temp_rewards[i]);
//					
//				} 

				if (rewards.size() != FinalEval.NUM_EXPECTED_TRIALS) {
					System.err.println("INCORRECT NUMBER OF TRIALS [" + rewards.size() + " / expected: "
							+ FinalEval.NUM_EXPECTED_TRIALS + "] for " + client_name + " on " +
							instance_name + ": continuing with average on these trials.");
				}		
				///////////////////////////////////////////////////////////////////////////////////////////////////////
				
				double min_val = instance2minR.get(instance_name);
				double max_val = instance2maxR.get(instance_name);	
				double min_valNoopRandom = instance2minRNoopRandom.get(instance_name);
				double avg = Statistics.Avg(rewards);
				double stderr = Statistics.StdError95(rewards);
				if (avg < min_val) {
					instance2minR.put(instance_name, avg);
					instance2minRName.put(instance_name, client_name);
					instance2minRStdErr.put(instance_name, stderr);
				}
				if (avg > max_val) {
					instance2maxR.put(instance_name, avg);
					instance2maxRName.put(instance_name, client_name);
					instance2maxRStdErr.put(instance_name, stderr);
				}
				if (avg > min_valNoopRandom && 
						(BASELINE_POLICIES.contains(client_name.toLowerCase()))) {
					instance2minRNoopRandom.put(instance_name, avg);
					instance2minRNameNoopRandom.put(instance_name, client_name);
					instance2minRStdErrNoopRandom.put(instance_name, stderr);
				}
			}
			instances.addAll(instances_encountered);
		}
		
		PrintStream ps = new PrintStream(f.getPath() + File.separator + MIN_MAX_FILE);
		for (String instance_name : instances) {
			
			int count   = instance2count.get(instance_name);
			double min_val = instance2minR.get(instance_name);
			String min_val_src = instance2minRName.get(instance_name);;
			double min_val_stderr = instance2minRStdErr.get(instance_name);;
			double min_valNoopRandom = instance2minRNoopRandom.get(instance_name);
			String min_val_srcNoopRandom = instance2minRNameNoopRandom.get(instance_name);;
			Double min_val_stderrNoopRandom = instance2minRStdErrNoopRandom.get(instance_name);;
			if (min_val_stderrNoopRandom == null) {
				System.out.println("ERROR: could not find min for: " + instance_name);
				System.exit(1);
			}
			
			double max_val = instance2maxR.get(instance_name);
			String max_val_src = instance2maxRName.get(instance_name);;
			double max_val_stderr = instance2maxRStdErr.get(instance_name);;
			System.out.println(instance_name + "\t" + count + "\t" + 
					min_val_src + "\t" + df.format(min_val) + "\t+/- " + df.format(min_val_stderr) + "\t" +
					min_val_srcNoopRandom + "\t" + df.format(min_valNoopRandom) + "\t+/- " + df.format(min_val_stderrNoopRandom) + "\t" +
					max_val_src + "\t" + df.format(max_val) + "\t+/- " + df.format(max_val_stderr));
			
			ps.println(instance_name + "\t" + min_val_srcNoopRandom + "\t" + df.format(min_valNoopRandom) + "\t" + max_val_src + "\t" + df.format(max_val));
		}
		ps.close();
		
		System.out.println("\nClients evaluated: " + client_names);
	}
	
	public static class InstNameComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			String[] parts1 = ((String)o1).split("__");
			String[] parts2 = ((String)o2).split("__");
			
			int comp1 = parts1[0].compareTo(parts2[0]);
			if (comp1 != 0)
				return comp1;
			
			int id1 = new Integer(parts1[1]);
			int id2 = new Integer(parts2[1]);
			return id1 - id2;
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		
		String directory = null;
		int index = FinalEval.ProcessArgs(args, 0);
		if (index != args.length - 1)
			FinalEval.usage();
		directory = args[index];
		
		Eval(new File(directory));
	}
}
