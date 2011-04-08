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

import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBaseIterators.ChildrenIterator;


public class MinMaxEval {
	
	public static DecimalFormat df = new DecimalFormat("#.##");
	
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
		
		HashMap<String,Integer> instance2count = new HashMap<String,Integer>();
		HashMap<String,Double>  instance2minR  = new HashMap<String,Double>();
		HashMap<String,Double>  instance2maxR  = new HashMap<String,Double>();
		HashMap<String,String>  instance2minRName    = new HashMap<String,String>();
		HashMap<String,String>  instance2maxRName    = new HashMap<String,String>();
		HashMap<String,Double>  instance2minRStdErr  = new HashMap<String,Double>();
		HashMap<String,Double>  instance2maxRStdErr  = new HashMap<String,Double>();

		TreeSet<String> instances = new TreeSet<String>();
		for (Map.Entry<String, MapList> e : client2data.entrySet()) {
			String client_name = e.getKey();
			HashSet<String> instances_encountered = new HashSet<String>();
			for (Object o : e.getValue().keySet()) {
				String instance_name = (String)o;
				Integer count = instance2count.get(instance_name);
				if (count == null) {
					// This instance has never been encountered before
					count = 0;
					instance2minR.put(instance_name, Double.MAX_VALUE);
					instance2maxR.put(instance_name, -Double.MAX_VALUE);
				}
				instance2count.put(instance_name, count + 1);
				instances_encountered.add(instance_name);
				
				ArrayList<Double> rewards = new ArrayList<Double>(e.getValue().getValues(instance_name));
				
				double min_val = instance2minR.get(instance_name);
				double max_val = instance2maxR.get(instance_name);	
				double avg = Statistics.Avg(rewards);
				double stderr = Statistics.StdError95(rewards);
				if (avg < min_val) {
					instance2minR.put(instance_name, Math.min(min_val, avg));
					instance2minRName.put(instance_name, client_name);
					instance2minRStdErr.put(instance_name, stderr);
				}
				if (avg > max_val) {
					instance2maxR.put(instance_name, Math.max(max_val, avg));
					instance2maxRName.put(instance_name, client_name);
					instance2maxRStdErr.put(instance_name, stderr);
				}
			}
			instances.addAll(instances_encountered);
		}
		
		for (String instance_name : instances) {
			double count   = instance2count.get(instance_name);
			double min_val = instance2minR.get(instance_name);
			String min_val_src = instance2minRName.get(instance_name);;
			double min_val_stderr = instance2minRStdErr.get(instance_name);;
			double max_val = instance2maxR.get(instance_name);
			String max_val_src = instance2maxRName.get(instance_name);;
			double max_val_stderr = instance2maxRStdErr.get(instance_name);;
			System.out.println(instance_name + "\t" + count + "\t" + 
					min_val_src + "\t" + df.format(min_val) + "\t+/-" + df.format(min_val_stderr) + "\t" +
					max_val_src + "\t" + df.format(max_val) + "\t+/-" + df.format(max_val_stderr));
		}
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
