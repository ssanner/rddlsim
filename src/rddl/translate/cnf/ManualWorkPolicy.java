/**
 * RDDL: Implements a policy for manually choose action.
 * 
 * @author Xavier Kong (wizardkxh@gmail.com)
 * @version 13/01/08
 *
 **/

package rddl.translate.cnf;

import java.util.*;

import rddl.*;
import rddl.RDDL.*;
import rddl.policy.Policy;

import java.io.Console;

public class ManualWorkPolicy extends Policy {
	

	public ManualWorkPolicy () {
		
	}
	
	public ManualWorkPolicy(String instance_name) {
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

		// Get a map of { legal action names -> RDDL action definition }  
		Map<String,ArrayList<PVAR_INST_DEF>> action_map = 
			ActionGenerator.getLegalBoolActionMap(s);
		Console console = System.console();
		// Return a random action selection
		ArrayList<String> actions = new ArrayList<String>(action_map.keySet());
		//String action_taken = actions.get(_rand.nextInt(action_map.size()));
		//System.out.println("\n--> Action taken: " + action_taken);
		int i = 1;
		for(String act: actions)
		{
			//list all actions
			if(console != null)
				console.printf("\n %d \t:" + act,i);
			else
			{
				System.out.println("\n " + i + " \t:" + act);
			}
			i++;
		}
		int choose = 0 ;
		String action_taken = null;
		
		do{
			String temp = null;
			if(console != null)
				temp = console.readLine("\nAction: ");
			else 
			{
				System.out.println("Action:");
				Scanner input = new Scanner(System.in);
				temp = input.nextLine();
				
			}
			try{
				choose = Integer.parseInt( temp);
				action_taken = actions.get(choose -1 );
			}catch(NumberFormatException e)
			{
				continue;
			}
		}while(!actions.contains(action_taken));	
		return action_map.get(action_taken);
	}
}
