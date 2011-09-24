package rddl.solver;

import java.util.ArrayList;
import java.util.Set;

import java.util.HashMap;
import java.util.Iterator;

import dd.discrete.DD;
import dd.discrete.ADDDNode;
import dd.discrete.ADDNode;
import dd.discrete.ADDINode;
import dd.discrete.ADDBNode;
import dd.discrete.ADD;

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
	public static int UpdateValue(DD context, int dd, ArrayList assign, double new_value, HashMap<String, String> hmPrimeRemap) {
		ArrayList assignprime = new ArrayList();
		for (int j = 0; j< assign.size();j++)
			assignprime.add(null);
		for (int i = 0; i< assign.size();i++){        			
			Object val = assign.get(i);
			if(val != null)	{
				int id = (Integer)context._alOrder.get(i);
				int idprime = (Integer)context._hmVarName2ID.get(hmPrimeRemap.get(context._hmID2VarName.get(id)))-1;
				assignprime.set(idprime, val);
			}
		}
        Double cur_value = context.evaluate(dd, assignprime);
		if (Double.isNaN(cur_value)) {
			System.err.println("ERROR in DDUtils.UpdateValue: Expected single value when evaluating: " + assignprime);
			//System.err.println("in " + context.printNode(dd));
			System.exit(1);
		}
		double diff = new_value - cur_value;
		int indicator = BuildIndicator(context, assign, hmPrimeRemap);
		indicator = context.scalarMultiply(indicator, diff);		
		return context.applyInt(dd, indicator, DD.ARITH_SUM);
	}
	
	// Build the indicator DD that gives 1 for assign and 0 elsewhere
	public static int BuildIndicator(DD context, ArrayList assign,HashMap hmPrimeRemap) {
		int dd = context.getConstantNode(1d);
		for (int level = 0; level < assign.size(); level++) {
			Boolean is_true = (Boolean)assign.get(level);
			if (is_true == null)
				continue;
			int idprime = (Integer)context._hmVarName2ID.get(hmPrimeRemap.get(context._hmID2VarName.get((Integer)context._alOrder.get(level))));
			if (is_true)
				dd = context.applyInt(dd, 
						((ADD)context).getINode(idprime/*(Integer)context._alOrder.get(level) /*var id*/, 
						0 /*low*/, 1 /*high*/, true), DD.ARITH_PROD);
			else // swap high/low branches to invert indicator function
				dd = context.applyInt(dd, 
						((ADD)context).getINode(idprime/*(Integer)context._alOrder.get(level) /*var id*/, 
						1 /*low*/, 0 /*high*/, true), DD.ARITH_PROD);
		}
		return dd;
	}
	
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
						((ADD)context).getINode((Integer)context._alOrder.get(level) /*var id*/, 
						0 /*low*/, 1 /*high*/, true), DD.ARITH_PROD);
			else // swap high/low branches to invert indicator function
				dd = context.applyInt(dd, 
						((ADD)context).getINode((Integer)context._alOrder.get(level) /*var id*/, 
						1 /*low*/, 0 /*high*/, true), DD.ARITH_PROD);
		}
		return dd;
	}
    /*Insert a number in an ADD branch given a list of variable assignments
     *  
     */
	public static int insertValueInDD(int F, ArrayList state, double value, Iterator it, HashMap<String,String> hmPrimeRemap, ADD _context) {
		int Fh, Fl;
		if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
			return _context.getConstantNode(value);
		}
		String varStr = (String)it.next();
		Integer var=(Integer)_context._hmVarName2ID.get(varStr);
		Object valueVar =state.get((Integer)_context._hmGVarToLevel.get(var));
		Boolean val=(valueVar != null) && (Boolean)valueVar;
		Integer varPrime= (Integer)_context._hmVarName2ID.get(hmPrimeRemap.get(varStr));
		if(varPrime==null){ // this was inserted for RTDPEnum and BRTDPEnum in order to use it for states with prime or non-prime variables
			varPrime=var;
		}
		ADDNode cur = _context.getNode(F);
		if((cur instanceof ADDDNode) || (cur instanceof ADDBNode)){
			// means that we need to create the nodes with the remain variables
			if (val==true){
				Fh = insertValueInDD(F, state, value, it, hmPrimeRemap, _context);
				Fl = F;
			}
			else {
				Fh = F;
				Fl = insertValueInDD(F, state, value, it,hmPrimeRemap, _context);
			}
			return _context.getINode(varPrime, Fl, Fh, true);
		}
		ADDINode cur1 =  (ADDINode)_context.getNode(F);
		Integer Fvar= cur1._nTestVarID;
		if(Fvar.compareTo(varPrime)==0){
			if (val==true){
				Fh = insertValueInDD(cur1._nHigh, state, value, it,hmPrimeRemap, _context);
				Fl = cur1._nLow;
			}
			else {
				Fh =cur1._nHigh;
				Fl = insertValueInDD(cur1._nLow, state, value, it,hmPrimeRemap, _context);
			}			
			return _context.getINode(varPrime,Fl,Fh, true);

		}
		if (val==true){
			Fh = insertValueInDD(F, state, value, it,hmPrimeRemap, _context);
			Fl = F;
		}
		else {
			Fh = F;
			Fl = insertValueInDD(F, state, value, it,hmPrimeRemap, _context);
		}
		return _context.getINode(varPrime,Fl,Fh, true);
	}

}

