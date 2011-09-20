package rddl.solver;

import java.util.ArrayList;
import java.util.Set;

import dd.discrete.DD;

import util.CString;

public class DDUtils {
	
	// Converts the list of true state fluents to an ArrayList of assignments
	// that can be passed to ADD evaluate (avoids the need to constantly pass
	// a HashMap of Strings -> assignments)
	public static ArrayList ConvertTrueVars2DDAssign(
			DD context, 
			Set<CString> true_vars, 
			ArrayList<CString> all_vars) {
		
		ArrayList assign = new ArrayList();
		
		// Initialize assignments
		for (int i = 0; i <= context._alOrder.size(); i++)
			assign.add(null);
		
		// Now set all assignments to true or false as required
		for (CString s : all_vars) {
			Integer index = (Integer)context._hmVarName2ID.get(s._string); // if null, var not in var2ID
			Integer level = (Integer)context._hmGVarToLevel.get(index);
			if (index == null || level == null) {
				System.err.println("ERROR: could not find ADD index/level for " + s);
				System.exit(1);
			}
			assign.set(level, (Boolean)true_vars.contains(s));
		}
		return assign;
	}
}
