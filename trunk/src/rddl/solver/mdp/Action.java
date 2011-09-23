package rddl.solver.mdp;

import java.util.HashMap;
import java.util.Map;

import dd.discrete.DD;

import util.CString;

// Action class for MDP definition
public class Action {
	
	public DD _context;
	public CString _csActionName;
	public HashMap<CString,Integer> _hmStateVar2CPT = null; // Map of CPT ADDs
	public HashMap<Integer,Integer> _hmVarID2CPT    = null; // Map of CPT ADDs
	public int _reward = -1; // Action-specific reward function ADD

	public Action(DD context, CString name, 
			HashMap<CString,Integer> cpts, Integer reward) {
		_context = context;
		_csActionName = name;
		_hmStateVar2CPT = cpts;
		_reward = reward;
		
		// Create a second map by VarID (saves hash lookups in some cases)
		_hmVarID2CPT = new HashMap<Integer,Integer>();
		for (Map.Entry<CString, Integer> me : _hmStateVar2CPT.entrySet()) {
			Integer var_id = (Integer)context._hmVarName2ID.get(me.getKey()._string);
			if (var_id == null) {
				System.err.println("ERROR in Action(): could not find var ID for " + me.getKey());
				System.exit(1);
			}
			_hmVarID2CPT.put(var_id, me.getValue());
		}
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
