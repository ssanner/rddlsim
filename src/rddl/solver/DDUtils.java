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

	
	// Update the value for path 
	public static int UpdateValue(DD context, int dd, ArrayList assign, double new_value) {
		Double cur_value = context.evaluate(dd, assign);
		if (Double.isNaN(cur_value)) {
			System.err.println("ERROR in DDUtils.UpdateValue: Expected single value when evaluating: " + assign);
			//System.err.println("in " + context.printNode(dd));
			System.exit(1);
		}
		double diff = new_value - cur_value;
		int indicator = BuildIndicator(context, assign);
		indicator = context.scalarMultiply(indicator, diff);
		return context.applyInt(dd, indicator, DD.ARITH_SUM);
	}
	
	// Build the indicator DD that gives 1 for assign and 0 elsewhere
	public static int BuildIndicator(DD context, ArrayList assign) {
		int dd = context.getConstantNode(1d);
		for (int level = 0; level < assign.size(); level++) {
			Boolean is_true = (Boolean)assign.get(level);
			if (is_true == null)
				continue;
			if (is_true)
				dd = context.applyInt(dd, 
						context.getVarNode((Integer)context._alOrder.get(level) /*var id*/, 
						0d /*low*/, 1d /*high*/), DD.ARITH_PROD);
			else // swap high/low branches to invert indicator function
				dd = context.applyInt(dd, 
						context.getVarNode((Integer)context._alOrder.get(level) /*var id*/, 
						1d /*low*/, 0d /*high*/), DD.ARITH_PROD);
		}
		return dd;
	}
}
