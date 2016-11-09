package rddl.translate.cnf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SolutionReader {
	private final static String COMMENTH = "c ".intern();
	private final static String VAR_INTEGER_REGULAR = "^(c \\d+ : [A-Z]{3} :)".intern();
	private final static String NOOP_ACTION = "noop".intern();
	private final static String COLON_SPLITTER = " : ".intern();
	private static final String BLANK = " ".intern();
	private static final String NULLSTRING = "".intern();
	private static final String LINE1_RESULT = "time \\d+".intern();
	private static final String LINE2_RESULT = "ans ".intern();
	private static final String LINE3_RESULT = "cost \\d+".intern();
	private static final String LINE4_RESULT = "v ".intern();
	private HashMap<Integer, String> actList = null;
	private Vector<Integer> trueVars = null;
	private String outputName = null;
	
	public SolutionReader(String cnfFileName, String solutionFileName)
	{
		File cnf = new File(cnfFileName);
		File solution = new File(solutionFileName);
		
		if(!cnf.exists() || !solution.exists())
		{
			System.out.println("Error: Input files are not exist!");
			return;
		}
		else if(!cnf.isFile() || !solution.isFile())
		{
			System.out.println("Error: Input are not file!");
			return;
		}
		outputName = cnf.getParentFile().getPath() + cnf.getName()+solution.getName();
		actList = ReadCNF(cnf);
		trueVars = ReadSolution(solution);		
	}
	
	public SolutionReader(String cnfFileName, String solutionFileName, String outputDir)
	{
		File cnf = new File(cnfFileName);
		File solution = new File(solutionFileName);
		
		if(!cnf.exists() || !solution.exists())
		{
			System.out.println("Error: Input files are not exist!");
			return;
		}
		else if(!cnf.isFile() || !solution.isFile())
		{
			System.out.println("Error: Input are not file!");
			return;
		}
		outputName = outputDir + cnf.getName()+solution.getName();
		actList = ReadCNF(cnf);
		trueVars = ReadSolution(solution);		
	}
	
	public SolutionReader(String cnfFileName)
	{
		File cnf = new File(cnfFileName);
		
		if(!cnf.exists() )
		{
			System.out.println("Error: Input files are not exist!");
			return;
		}
		else if(!cnf.isFile() )
		{
			System.out.println("Error: Input are not file!");
			return;
		}
		actList = ReadCNF(cnf);
	}


	/*
	 * Read RSAT output:
	 * if is not a RSAT output or there is no solution return 
	 * new Vector<Integer>()
	 * */
	private Vector<Integer> ReadSolution(File solution) {
		Vector<Integer> context = new Vector<Integer>();
		try {
			FileInputStream fis = new FileInputStream(solution);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			Pattern p1 = Pattern.compile(LINE1_RESULT);
			Pattern p2 = Pattern.compile(LINE2_RESULT);
			Pattern p3 = Pattern.compile(LINE3_RESULT);
			Pattern p4 = Pattern.compile(LINE4_RESULT);
			Matcher m;
			line = br.readLine();
			m = p1.matcher(line);
			if(!m.find())
			{
				System.out.println("Error: Result file is not correct.1");
				return context;
			}
			line = br.readLine();
			m = p2.matcher(line);
			if(!m.find())
			{
				System.out.println("Error: Result file is not correct.2");
				return context;
			}
			line = br.readLine();
			m = p3.matcher(line);
			if(!m.find())
			{
				System.out.println("Error: Result file is not correct.3");
				return context;
			}
			line = br.readLine();
			m = p4.matcher(line);
			if(!m.find())
			{
				System.out.println("Error: Result file is not correct.4");
				return context;
			}
			line = line.replace(LINE4_RESULT,NULLSTRING);
			while(line !=  null)
			{
				String[] indexs = line.split(BLANK);
				for(String s : indexs)
				{
					if(s != null && s != NULLSTRING)
					{
						Integer value = Integer.valueOf(s);
						if(value > 0)
						{
							context.add(value);
						}
					}
				}
				line = br.readLine();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return new Vector<Integer>();
		} catch (IOException e) {
			e.printStackTrace();
			return new Vector<Integer>();
		} catch(NullPointerException e)
		{
			e.printStackTrace();
			return new Vector<Integer>();
		}
		return context;
	}

	// Find all ordered action variables
	public ArrayList<String> SerialActions(String solutionName)
	{
		File solution = new File(solutionName);
		int cntNoop = 0;
		ArrayList<String> actions = new ArrayList<String>();
		// Find all positive variables
		if(solution != null && solution.isFile())
		{
			trueVars = ReadSolution(solution);
		}
		else 
		{
			System.out.println("Error: Wrong solution file!");
			return null;
			
		}
		if(trueVars.size() > 0)
		{
			int index = 0;
			// choose action variable to output &&
			// Reorder actions by postpone noop
			for(int i = 0 ; i < trueVars.size(); i ++)
			{
				index = trueVars.get(i);
				String action = actList.get(index);
				if(action == null)
				{
					continue;
				}
				else if(action.contains(NOOP_ACTION))
				{
					cntNoop++;
				}
				else
				{
					action = removeTimestep(action);
					actions.add(action) ;
				}
			}
			while(cntNoop>0)
			{
				actions.add(NOOP_ACTION);
				cntNoop--;
			}
		}
		return actions;
	}
	
	// format action variable name by remove "@timestep"
	private String removeTimestep(String action) {
		int pos = action.indexOf(CNF.AT);
		if(pos > 0)
		{
			return action.substring(0, pos);
		}
		return action;
	}

	//Read cnf file and list all action variables
	private HashMap<Integer, String> ReadCNF(File cnf) {
		HashMap<Integer, String> context = new HashMap<Integer, String>();
		try {
			FileInputStream fis = new FileInputStream(cnf);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			Boolean inMappingSection = false;
			line = br.readLine();
			Pattern p = Pattern.compile(VAR_INTEGER_REGULAR);
			Matcher m = null;
			while(line !=  null)
			{
				while(line.startsWith(BLANK))
				{
					line = line.replaceFirst(BLANK, NULLSTRING);
				}
				if(line.startsWith(COMMENTH))
				{
					m = p.matcher(line);
					if(m.find())
					{
						// find a new variable and its index
						inMappingSection = true;
						line = line.replace(COMMENTH, NULLSTRING);
						String[] temp = line.split(COLON_SPLITTER);
						if(temp.length != 3)
						{
							System.out.println("Error: failed to read cnf, wrong match of mapping");
							break;
						}else
						{
							if(temp[1].contains(CNF.VAR_TYPE[1]))
							{
								// find an action variable, save it
								int index = Integer.parseInt(temp[0]);
								String actionName = temp[2].replace(BLANK, "");
								context.put(index, actionName);
							}
							
						}
					}
					else if(inMappingSection)
					{// exit of var2int mapping section
						break;
					}else
					{
						line = br.readLine();
						continue;
					}
				}
				else
				{
					System.out.println("Error: Wrong format!");
					return new HashMap<Integer, String>();
				}
				line = br.readLine();
			}
			br.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return new HashMap<Integer, String>();
		} catch (IOException e) {
			e.printStackTrace();
			return new HashMap<Integer, String>();
		}
		return context;
	}
	
	public void PrintActions()
	{
		PrintWriter pw;
		if(actList == null || actList.size() == 0 )
		{
			System.out.println("Error: Need init action list.");
			return;
		}
			
		try {
			pw = new PrintWriter(new FileWriter(outputName));
			int cnt = 0;
			for(int i = 0 ; i < trueVars.size(); i++)
			{
				int index = trueVars.get(i);
				String action = actList.get(index);
				if(action != null)
				{
					
					pw.print(index + " : " + action + "\n");
					System.out.println(index + " : " + action);
					cnt++;
				}
			}
			pw.print("Finished! Total count: " + cnt);
			System.out.println("Finished! Total count: " + cnt);
			pw.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Error: Can not find output file");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error: Can not create output file");
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args.length != 3) {
			System.out.println("\nusage: CNF-File Solution-File\n");
			System.out.println("\n   or  CNF-File Solution-File Output-Dir\n");
			System.exit(1);
		}
		SolutionReader sr ;
		if(args.length == 2)
			sr = new SolutionReader(args[0], args[1]);
		else sr = new SolutionReader(args[0], args[1], args[2]);
		//all positive variables will be printed out.
		sr.PrintActions();
	}
}
