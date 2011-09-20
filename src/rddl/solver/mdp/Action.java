package rddl.solver.mdp;

import java.util.HashMap;

import dd.discrete.DD;

import util.CString;

// Action class for MDP definition
public class Action {
	
	public DD _context;
	public CString _csActionName;
	public HashMap<CString,Integer> _hmStateVar2CPT = null; // Map of CPT ADDs
	public int _reward = -1; // Action-specific reward function ADD

	public Action(DD context, CString name, 
			HashMap<CString,Integer> cpts, Integer reward) {
		_context = context;
		_csActionName = name;
		_hmStateVar2CPT = cpts;
		_reward = reward;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(_csActionName + ":\n");
		for (CString var : _hmStateVar2CPT.keySet())
			sb.append("- CPT for  next state var '" + var + "'\n" + 
					_context.printNode(_hmStateVar2CPT.get(var)) + "\n");
		sb.append("- Reward: " + _context.printNode(_reward) + "\n");
		return sb.toString();
	}
}
