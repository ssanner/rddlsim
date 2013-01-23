package rddl.translate.cnf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import rddl.ActionGenerator;
import rddl.EvalException;
import rddl.RDDL;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.parser.parser;
import rddl.policy.Policy;
import rddl.translate.RDDL2Format;

public class TestElevatorPolicy extends Policy{	
	//Only for Elevator 
	public final static String ROOT_PATH = System.getProperty("user.dir");
	public final static String RDDL_FILE_NAME = ROOT_PATH+"/test_input/elevators_mdp_testinput3.rddl";
//	public final static String RDDL_FILE_NAME = "E:\\Dropbox\\project\\test_input\\elevators_mdp_testinput3.rddl";
	public final static String CNF_FILE_NAME = ROOT_PATH+"/result/elevators_inst_mdp__3.cnf";
//	public final static String CNF_FILE_NAME = "E:\\Dropbox\\result\\elevators_inst_mdp__3.cnf";
	public final static String INST_NAME = "elevators_inst_mdp__3";
	public final static String OUTPUT_DIR = ROOT_PATH+"/result/";
//	public final static String OUTPUT_DIR = "E:\\Dropbox\\result\\";
	public final static String FILE_FORMAT = "cnf";
	public final static String PLAN_NAME_PREFIX = "plan_";
	public final static String PLAN_NAME_SURFIX = ".txt";
	public final static String[] command=  {
		ROOT_PATH+"/rsat_2.02_cost_simple/rsat",
		ROOT_PATH+"/result/elevators_inst_mdp__3.cnf",
//		"E:\\Dropbox\\rsat_2.02_cost_simple\\rsat.exe",
//		"E:\\Dropbox\\result\\elevators_inst_mdp__3.cnf",
		"-1",
		"-s" };
//	private RDDL2Format r2f = null;
	
	public final static String CHECK_PROC = "ps gx -e";
	public final static String KILL_RSAT = "killall -9 rsat";
	public final static String RSAT_NAME = "rsat";
	
	private HashMap<String,Boolean> previousStartStates = new HashMap<String, Boolean>();
	private ArrayList<String> curPlan = null;
	private int timestepInCurPlan = 0;
	
	private int timestep = 0;
	private CNF cnf = null;
	private SolutionReader sr = null;
	private File cnffile = null;
	public TestElevatorPolicy () {
	}
	
	public TestElevatorPolicy(String instance_name) {
		super(instance_name);

	}

	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		
		if (s == null) {
			// This should only occur on the **first step** of a POMDP trial
			// when no observations have been generated, for now, we simply
			// send a noop.  An approach that actually reads the domain
			// should probably execute a different legal action since the
			// initial state is assumed known.
			return new ArrayList<PVAR_INST_DEF>();
		}
		//use given rddl file initialize the cnf
		if(cnf == null)
		{
			// If RDDL file is a directory, add all files
			ArrayList<File> rddl_files = new ArrayList<File>();
			ArrayList<File> rddl_error = new ArrayList<File>();
			File file = new File(RDDL_FILE_NAME);
			if (file.isDirectory())
				rddl_files.addAll(Arrays.asList(file.listFiles()));
			else
				rddl_files.add(file);
			
			// Load RDDL files
			RDDL rddl = new RDDL();
			HashMap<File,RDDL> file2rddl = new HashMap<File,RDDL>();
			if(file.exists())
			for (File f : (ArrayList<File>)rddl_files.clone()) {
				try {
					if (f.getName().endsWith(".rddl")) {
						RDDL r = parser.parse(f);
						file2rddl.put(f, r);
						rddl.addOtherRDDL(r);
					}
				} catch (Exception e) {
					System.out.println(e);
					System.out.println("Error processing: " + f + ", skipping...");
					rddl_files.remove(f);
					continue;
				}
			}
			
			for (File f : (ArrayList<File>)rddl_files.clone()) {
							
				try {	
					if (!file2rddl.containsKey(f))
						continue;
					for (String instance_name : file2rddl.get(f)._tmInstanceNodes.keySet()) {
						RDDL2Format r2f = new RDDL2Format(rddl, INST_NAME, FILE_FORMAT, OUTPUT_DIR);
						File cnffile = new File(CNF_FILE_NAME);
//						if(!cnffile.exists())cnffile.createNewFile();
						cnf = new CNF(r2f);
						PrintWriter pw = new PrintWriter(new FileWriter(CNF_FILE_NAME));
						cnf.exportCNF(pw);
						pw.close();
					}
				} catch (Exception e) {
					System.err.println("Error processing: " + f);
//					System.err.println(e);
					e.printStackTrace(System.err);
					System.err.flush();
					rddl_files.remove(f);
					rddl_error.add(f);
				}
			}
		}
		
		//Update previousStartStates
		previousStartStates.clear();
		for(PVAR_NAME state : s._state.keySet())
		{
			HashMap<ArrayList<LCONST>,Object> value = s._state.get(state);
			String valName = null;
			Boolean valValue = null;
			for (ArrayList<LCONST> assign : value.keySet()) {
				valName = RDDL2Format.CleanFluentName(state.toString() + assign);
				valValue = (Boolean) value.get(assign);
				previousStartStates.put(valName, valValue);
			}
		}
		
		//Update start states in cnf
		Boolean needReplan = cnf.needReplan(previousStartStates);
		try {
			if(timestep > 0)
			{
				cnffile = new File(CNF_FILE_NAME);
				if(cnffile.exists())cnffile.renameTo(new File(CNF_FILE_NAME+timestep));
				if( needReplan || timestepInCurPlan > 14)
				{
					cnf.SetStartState(previousStartStates);
					PrintWriter pw = new PrintWriter(new FileWriter(CNF_FILE_NAME));
					cnf.exportCNF(pw);
	
					pw.close();
				}
				else
				{
//					timestepInCurPlan++;
				}
			}
		} catch (Exception e) {			
			e.printStackTrace();
			System.out.println("Error: CNF file output failed!");
		}
		//Use ProcessBuilder execute RSAT and redirect output to given file
		if(needReplan || timestepInCurPlan > 14 || curPlan == null)
		{
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(command);
			File newplan = new File( OUTPUT_DIR + PLAN_NAME_PREFIX + timestep + PLAN_NAME_SURFIX);
			pb.redirectOutput(newplan);
			try {
				pb.start();
			} catch (IOException e) {
				
				e.printStackTrace();
				System.out.println("Error: Failed to execute RSAT");
			}
			int time_up_cnt = 0;
			while(RSATRunning()){
				if(	time_up_cnt == 10)
				{
					giveUp();
					break;
				}
				try{
					Thread.sleep(400);
				}catch(Exception e){
					System.out.println("Error: sleep failed");
				}
				time_up_cnt++;
				
			}
			
			/*
			 * Try to find a new plan with new starting state
			 * If can not find one, go on the old plan*/
			try{
				sr = new SolutionReader(CNF_FILE_NAME);
//				sr._actList = SolutionReader.ACTIONS_ELEVATOR;
				ArrayList<String> tempPlan = sr.SerialActions(OUTPUT_DIR + PLAN_NAME_PREFIX + timestep + PLAN_NAME_SURFIX);
				if(tempPlan.size() > 0)
				{
					curPlan = tempPlan;
					timestepInCurPlan = 0;
				}
			}
			catch(NullPointerException e)
			{
				e.printStackTrace(System.err);
				System.err.flush();
			}
		}
		// Get first action
		String action_taken = null;
		action_taken = curPlan.get(timestepInCurPlan);
		timestep++;
		timestepInCurPlan++;
		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);
		// Return a random action selection
		
		
		
			
		return action_map.get(action_taken);
	}

	private void giveUp() {
		// TODO Auto-generated method stub
		Process proc = null;
		try
		{
			proc = Runtime.getRuntime().exec(KILL_RSAT);
		}catch(IOException e)
		{
			return;
		}
		proc.destroy();
				
	}

	private boolean RSATRunning() {

		Process proc = null;
		BufferedReader input;
		StringBuffer sb = new StringBuffer();
		try
		{
			proc = Runtime.getRuntime().exec(CHECK_PROC);
		}catch(IOException e)
		{
			e.printStackTrace(System.err);
			return true;
		}
		try
		{
			proc.waitFor();
		}catch(InterruptedException e)
		{
			e.printStackTrace(System.err);
		}
		if(proc != null && proc.getInputStream() != null)
		{
			input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			try
			{
				while((line = input.readLine()) != null)
				{
					sb.append(line);
				}
			}catch(IOException e)
			{
				e.printStackTrace(System.err);
				return true;
			}
		}
		proc.destroy();
		int firstRsat = sb.indexOf(RSAT_NAME);
		int lastRsat = sb.lastIndexOf(RSAT_NAME);
		if(firstRsat == lastRsat)
			return false;
		return true;
	}
}
