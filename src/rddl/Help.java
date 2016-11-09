package rddl;

import java.util.Scanner;

public class Help {
	/**
	 * Define arguments
	 * -R: rddle files
	 * -P: policy name
	 * -I: instance name
	 * -V: visualizer name
	 * -S: random seed for simulator
	 * -X: random seed for policy
	 * -K: number of rounds
	 * -D: output data path
	 * -L: output label path
	 */
	public static String getSimulatorParaDescription(){
		StringBuilder sb = new StringBuilder("\n*********************************************************");		
		sb.append("\n>>> Parameter Description");
		sb.append("\n*********************************************************");
		sb.append("\n[1]: -R: RDDL file or directory that contains RDDL file");
		sb.append("\n[2]: -P: Policy name e.g. rddl.policy.RandomBoolPolicy");
		sb.append("\n[3]: -I: Instance name e.g. elevators_inst_mdp__9");
		sb.append("\n[4]: -V: Visualization Method e.g. rddl.viz.GenericScreenDisplay");
		sb.append("\n[5]: -S: (Optional) Random seed for simulator to sample output.");
		sb.append("\n[6]: -X: (Optional) Random seed for policy to take random actions.");
		sb.append("\n[7]: -K: (Optional) Number of rounds. Default:1");		
		sb.append("\n[8]: -D: (Optional) Output file address for state-action pair");
		sb.append("\n[9]: -L: (Optional) Output file address for state label");
		sb.append("\n*********************************************************");
		return sb.toString();
	}
	
	public static String getGraphParaDescription(){
		StringBuilder sb = new StringBuilder("\n*********************************************************");		
		sb.append("\n>>> Parameter Description");
		sb.append("\n*********************************************************");
		sb.append("\n[1]: -R: RDDL file or directory that contains RDDL file");
		sb.append("\n[2]: -P: Policy name e.g. rddl.policy.RandomBoolPolicy");
		sb.append("\n[3]: -D: (Optional) Directed, 'true' or 'false'");
		sb.append("\n[4]: -L: (Optional) Strict-levels, 'true' or 'false'");
		sb.append("\n[5]: -G: (Optional) Strict-grouping, 'true' or 'false'");
		sb.append("\n*********************************************************");
		return sb.toString();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(getSimulatorParaDescription());
		System.out.print("Type 'exit' to leave the help program: ");
		Scanner sc = new Scanner (System.in);
		
		while(sc.hasNext()) {
		    String input = sc.next();
		    if(input.equals("exit")) {
		        break;
		    }
		    
		}
	}
}
