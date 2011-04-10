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

	public static DecimalFormat df = new DecimalFormat("#.##");
	public static DecimalFormat df4 = new DecimalFormat("#.####");

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

		HashMap<String,Double> instance2min = new HashMap<String,Double>();
		HashMap<String,String> instance2minSrc = new HashMap<String,String>();
		HashMap<String,Double> instance2max = new HashMap<String,Double>();
		
		BufferedReader br = new BufferedReader(
				new FileReader(f.getPath() + File.separator + MinMaxEval.MIN_MAX_FILE));
		String line = null;
		while ((line = br.readLine()) != null) {
			String split[] = line.split("\t");
			double min = new Double(split[2]);
			double max = new Double(split[4]);
			instance2min.put(split[0], min);
			instance2minSrc.put(split[0], split[1]);
			instance2max.put(split[0], max);
		}
		br.close();
		
		HashMap<Pair<String,String>,ArrayList<Double>> instance2allrewards = new HashMap<Pair<String,String>,ArrayList<Double>>();
		HashMap<Pair<String,String>,Integer> instance2count   = new HashMap<Pair<String,String>,Integer>();
		HashMap<Pair<String,String>,Double>  instance2minR    = new HashMap<Pair<String,String>,Double>();
		HashMap<Pair<String,String>,Double>  instance2maxR    = new HashMap<Pair<String,String>,Double>();
		HashMap<Pair<String,String>,Double>  instance2avg     = new HashMap<Pair<String,String>,Double>();
		HashMap<Pair<String,String>,Double>  instance2stderr  = new HashMap<Pair<String,String>,Double>();

		TreeSet<String> instances = new TreeSet<String>(new MinMaxEval.InstNameComparator());
		TreeSet<String> domains   = new TreeSet<String>();
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
				domains.add(GetDomainName(instance_name));
				
				Double instance_min = instance2min.get(instance_name);
				if (instance_min == null) {
					System.out.println("ERROR: could not find min for: " + instance_name);
					System.exit(1);
				}

				ArrayList<Double> rewards = new ArrayList<Double>(e.getValue().getValues(instance_name));
				
				if (MinMaxEval.BASELINE_POLICIES.contains(client_name.toLowerCase())
						&& rewards.size() != NUM_EXPECTED_TRIALS) {
						System.err.println("INCORRECT NUMBER OF TRIALS [" + rewards.size() + "/"
								+ NUM_EXPECTED_TRIALS + "] for " + client_name);
						System.exit(1);
				}
				
				Pair<String,String> key = new Pair<String,String>(client_name,instance_name);
				instance2count.put(key,  rewards.size());
				instance2allrewards.put(key, rewards);

				if (rewards.size() > NUM_EXPECTED_TRIALS) {
					// Take the last NUM_EXPECTED_TRIALS
					Object[] temp_rewards = rewards.toArray();
					rewards.clear();
					for (int i = temp_rewards.length - NUM_EXPECTED_TRIALS; i < temp_rewards.length; i++)
						rewards.add((Double)temp_rewards[i]);
					
				} else if (rewards.size() < NUM_EXPECTED_TRIALS) {
					// Use min reward if not enough trials
					for (int i = rewards.size(); i < NUM_EXPECTED_TRIALS; i++)
						rewards.add(instance_min);
				}

				if (rewards.size() != NUM_EXPECTED_TRIALS) {
					System.out.println("INCORRECT NUMBER OF TRIALS [" + rewards.size() + "/"
							+ NUM_EXPECTED_TRIALS + "] for " + client_name + " after correction.");
				}

				double avg = Statistics.Avg(rewards);
				double stderr = Statistics.StdError95(rewards);

				instance2minR.put(key,   Statistics.Min(rewards));
				instance2maxR.put(key,   Statistics.Max(rewards));
				instance2avg.put(key,    avg);
				instance2stderr.put(key, stderr);
			}
		}
		
		// TODO: Show details like variable count for domains, largest CPT, tree width, etc?
		//       Probably another domain analysis file.
		
		// TODO: Track aggregate results per instance... remove __#

		PrintStream all_results = new PrintStream(new FileOutputStream(f.getPath() + File.separator + "all_results.txt"));

		System.out.println("All results\n===");
		all_results.println("All results\n===");
		
		MapList client2normval = new MapList();
		MapList client2normvalAll = new MapList();
		MapList domain_client2normval = new MapList();
		MapList domain_client2normvalAll = new MapList();
		for (String instance_name : instances) {
			
			String domain_name = GetDomainName(instance_name);
			double instance_min = instance2min.get(instance_name);
			double instance_max = instance2max.get(instance_name);
			all_results.print(instance_name + "\t");
			System.out.print(instance_name + "\t");
			
			for (String client_name : clients) {
				Pair<String,String> key = new Pair<String,String>(client_name,instance_name);
				
				Integer count = instance2count.get(key);
				if (count == null) {
					String min_client_name = instance2minSrc.get(instance_name);
					key = new Pair<String,String>(min_client_name,instance_name);
					
					count = 0;//instance2count.get(key);
				}
				ArrayList<Double> all_rewards = instance2allrewards.get(key);
				double min_val = instance2minR.get(key);
				double max_val = instance2maxR.get(key);
				double avg     = instance2avg.get(key);
				double stderr  = instance2stderr.get(key);
				
				// Don't allow scores below instance_min
				if (avg < instance_min)
					avg = instance_min;

				double range = instance_max - instance_min;
				if (range == 0d)
					range = 1e10d;
				double norm_score = (avg - instance_min) / range;
				client2normval.putValue(client_name, norm_score);
				domain_client2normval.putValue(new Pair<String,String>(domain_name,client_name), norm_score);
				
				// Note: the "min-average rule" technically prevents us from directly normalizing individual
				//       performances -- the instance normalized avg is the result of a min function
				for (Double reward : all_rewards) {
					double norm_reward = (reward - instance_min) / range;
					client2normvalAll.putValue(client_name, norm_reward);
					domain_client2normvalAll.putValue(new Pair<String,String>(domain_name,client_name), norm_reward);
				}
				
				System.out.print(client_name + "\t" + count + "\t" + format4(norm_score) + "\t" + format(avg) + "\t+/- " + format(stderr) + "\t[ " + format(min_val) + "\t" + format(max_val) + " ]\t");
				all_results.print(client_name + "\t" + count + "\t" + format4(norm_score) + "\t" + format(avg) + "\t" + format(stderr) + "\t" + format(min_val) + "\t" + format(max_val) + "\t");
			}
			System.out.println();
			all_results.println();
		}
		
		System.out.println("\nAvg of all normalized rewards by domain\n===");
		all_results.println("\nAvg of all normalized rewards by domain\n===");
		for (String domain_name : domains) {
			System.out.print(domain_name + "\t");
			all_results.print(domain_name + "\t");
			for (String client_name : clients) {
				ArrayList<Double> norm_scoresAll = (ArrayList<Double>)domain_client2normvalAll.getValues(new Pair<String,String>(domain_name,client_name));
				double avgAll = Statistics.Avg(norm_scoresAll);
				double stderrAll = Statistics.StdError95(norm_scoresAll);
				System.out.print("\t" + client_name + "\t" + norm_scoresAll.size() + "\t" + format4(avgAll) + "\t+/- " + format4(stderrAll));
				all_results.print("\t" + client_name + "\t" + norm_scoresAll.size() + "\t" + format4(avgAll) + "\t" + format4(stderrAll));
			}
			System.out.println();
			all_results.println();
		}
		
		System.out.println("\nAvg of min(0,avg-norm-score-instance)\n===");
		all_results.println("\nAvg of min(0,avg-norm-score-instance)\n===");
		for (String domain_name : domains) {
			System.out.print(domain_name + "\t");
			all_results.print(domain_name + "\t");
			for (String client_name : clients) {
				ArrayList<Double> norm_scores = (ArrayList<Double>)domain_client2normval.getValues(new Pair<String,String>(domain_name,client_name));
				double avg = Statistics.Avg(norm_scores);
				double stderr = Statistics.StdError95(norm_scores);
				System.out.print("\t" + client_name + "\t" + norm_scores.size() + "\t" + format4(avg) + "\t+/- " + format4(stderr));
				all_results.print("\t" + client_name + "\t" + norm_scores.size() + "\t" + format4(avg) + "\t" + format4(stderr));
			}
			System.out.println();
			all_results.println();
		}

		System.out.println("\nAvg of all normalized rewards\n===");
		all_results.println("\nAvg of all normalized rewards\n===");
		for (String client_name : clients) {
			ArrayList<Double> norm_scoresAll = (ArrayList<Double>)client2normvalAll.getValues(client_name);
			double avgAll = Statistics.Avg(norm_scoresAll);
			double stderrAll = Statistics.StdError95(norm_scoresAll);
			System.out.println(client_name + "\t" + norm_scoresAll.size() + "\t" + format4(avgAll) + "\t+/- " + format4(stderrAll));
			all_results.println(client_name + "\t" + norm_scoresAll.size() + "\t" + format4(avgAll) + "\t" + format4(stderrAll));
		}
		
		all_results.println("\nAvg of min(0,avg-norm-score-instance)\n===");
		System.out.println("\nAvg of min(0,avg-norm-score-instance)\n===");
		for (String client_name : clients) {
			ArrayList<Double> norm_scores = (ArrayList<Double>)client2normval.getValues(client_name);
			double avg = Statistics.Avg(norm_scores);
			double stderr = Statistics.StdError95(norm_scores);
			System.out.println(client_name + "\t" + norm_scores.size() + "\t" + format4(avg) + "\t+/- " + format4(stderr));
			all_results.println(client_name + "\t" + norm_scores.size() + "\t" + format4(avg) + "\t" + format4(stderr));
		}
		
		all_results.close();
	}
	
	public static String format(Double d) {
		if (d == null)
			return null;
		else
			return df.format(d);
	}
	
	public static String format4(Double d) {
		if (d == null)
			return null;
		else
			return df4.format(d);
	}
	
	public static String GetDomainName(String instance_name) {
		String split[] = instance_name.split("__");
		String domain_name = split[0];
		return domain_name.replace("_inst", "");
	}
	
	public static void main(String[] args) throws Exception {
		
		String directory = "TestComp/MDP";
		if (args.length == 1)
			directory = args[0];
		else
			usage();
		
		Eval(new File(directory));
	}
	
	public static void usage() {
		System.out.println("\nUsage: <directory of RDDL .log files>");
	}
}
