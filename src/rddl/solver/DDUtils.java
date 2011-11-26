package rddl.solver;

import java.util.ArrayList;
import java.util.Set;

import java.util.HashMap;
import java.util.Iterator;

import dd.discrete.DD;
import dd.discrete.ADDDNode;
import dd.discrete.ADDNode;
import dd.discrete.ADDINode;
import dd.discrete.ADD;

import util.CString;
import util.Pair;

public class DDUtils {
	public static class doubleOutPut {
		public double  _dWeight;
		public ArrayList<Double> _alWeights;
		public doubleOutPut(double weight, ArrayList<Double> Weights) {
			_dWeight = weight;
			_alWeights = Weights;
		}
	}
	public static class doubleOutPut2 {
		public double  _dWeight;
		public int _nWeights;
		public doubleOutPut2(double weight, Integer Weights) {
			_dWeight = weight;
			_nWeights = Weights;
		}
	}
	public static class doubleOutPut3 {
		public double _dMaxWeight;
		public double _dMinWeight;
		public doubleOutPut3(double maxWeight, double minWeight) {
			_dMaxWeight = maxWeight;
			_dMinWeight = minWeight;
		}
	}
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
						context.getVarNode((Integer)context._alOrder.get(level) /*var id*/, 
						0d /*low*/, 1d /*high*/), DD.ARITH_PROD);
			else // swap high/low branches to invert indicator function
				dd = context.applyInt(dd, 
						context.getVarNode((Integer)context._alOrder.get(level) /*var id*/, 
						1d /*low*/, 0d /*high*/), DD.ARITH_PROD);
		}
		return dd;
	}
    /*Insert a number in an ADD branch given a list of variable assignments
     *  
     */
	public static Pair<Double, ArrayList<Double>> JoinBranches(Pair<Pair<Double, ArrayList<Double>>, Pair<Double, ArrayList<Double>>> weightsNode){
		ArrayList<Double> listRequest= new ArrayList<Double>();
		ArrayList<Double> listH = weightsNode._o1._o2;
		Double weightRequest = weightsNode._o1._o1 * listH.size();
		for (Double wH: listH)
			listRequest.add(wH);
		ArrayList<Double> listL = weightsNode._o2._o2;
		for (Double wL: listL)
			if (!listRequest.contains(wL)){
				weightRequest += wL;
				listRequest.add(wL);
			}
		weightRequest /= listRequest.size();
		return new Pair<Double, ArrayList<Double>>(weightRequest, listRequest);
	}
	public static Pair<Double, Integer> JoinBranches2(Pair<Pair<Double, Integer>, Pair<Double, Integer>> weightsNode){
		Pair<Double, Integer> highPair = weightsNode._o1;
		Pair<Double, Integer> lowPair = weightsNode._o2;
		double num = (highPair._o1*highPair._o2) +(lowPair._o1*lowPair._o2);
		int den = highPair._o2+lowPair._o2;
		return new Pair<Double, Integer>(num/den, den);
	}
	public static Double JoinBranches3(Pair<Double, Double> weightsNode){
		Double high = weightsNode._o1;
		Double low = weightsNode._o2;
		return Math.max(high, low);
	}
	public static Pair<Double, Double> JoinBranches4(Pair<Pair<Double, Double>, Pair<Double, Double>> weightsNode){
		Pair<Double, Double> highPair = weightsNode._o1;
		Pair<Double, Double> lowPair = weightsNode._o2;
		double max = Math.max(highPair._o1, lowPair._o1);
		double min = Math.min(highPair._o2, lowPair._o2);
		return new Pair<Double, Double>(max, min);
	}
	public static int insertValueInDD(int F, ArrayList state, double value, Iterator<CString> it, HashMap<String,String> hmPrimeRemap, ADD _context, HashMap<Integer, Integer> iD2ADD,HashMap<Integer, Pair<Pair<Double, ArrayList<Double>>, Pair<Double, ArrayList<Double>>>> hmNodeWeight, doubleOutPut weight) {
		int Fh, Fl;
		//There are no more elements in the alStateVars 	
		if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
			weight._dWeight = value;
			weight._alWeights = new ArrayList<Double>();
			weight._alWeights.add(value);
			//int newF = _context.getConstantNode(value);
			//hmNodeWeight.put(newF, new Pair<Double, Double>(value, 0d));
			return _context.getConstantNode(value);//newF;
		}
		else{
			String varStr = ((CString)it.next())._string;
			Integer var=(Integer)_context._hmVarName2ID.get(varStr);
			Object valueVar =state.get((Integer)_context._hmGVarToLevel.get(var));
			Boolean val=(valueVar != null) && (Boolean)valueVar;
			ADDNode cur = _context.getNode(F);
			Boolean unchangedBranchLow = false;
			//If F is a leaf and there are more elements in alStateVars or the variable is not in the ADD
			// means that we need to create the node(s) with the remain variable(s)
			int FforH = F, FforL = F;
			Boolean create = true;
			//If f is a internal Node and id state variable is equal to id Node
			if(cur instanceof ADDINode && var.compareTo(((ADDINode)cur)._nTestVarID) == 0){
				FforH = ((ADDINode)cur)._nHigh;
				FforL = ((ADDINode)cur)._nLow;
				create =  false;
			}
			if (val==true){
				Fh = insertValueInDD(FforH, state, value, it, hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight);
				unchangedBranchLow = true;
				Fl = FforL;
			}
			else {
				Fh = FforH;
				Fl = insertValueInDD(FforL, state, value, it,hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight);
			}
			//Computing the weight
			int fRequest = unchangedBranchLow? Fl : Fh;
			ADDNode nodeRequest = _context.getNode(fRequest);
			double weightRequest=0;
			ArrayList<Double> listRequest=new ArrayList<Double>();
			if (nodeRequest instanceof ADDDNode){
				weightRequest = ((ADDDNode)nodeRequest)._dLower;
				listRequest.add(weightRequest);
			}
			else{
				Pair<Double, ArrayList<Double>> newBranch = JoinBranches(hmNodeWeight.get(fRequest));
				listRequest = newBranch._o2;
				weightRequest = newBranch._o1;
			}
			Double highWeight = unchangedBranchLow? weight._dWeight : weightRequest;
			ArrayList<Double> highList = unchangedBranchLow? weight._alWeights : listRequest;
			Double lowWeight = unchangedBranchLow? weightRequest: weight._dWeight;
			ArrayList<Double> lowList = unchangedBranchLow? listRequest: weight._alWeights;
			if (create)
				F = _context.getINode(var,Fl,Fh, true);
			else{
				((ADDINode)cur)._nHigh =  Fh;
				((ADDINode)cur)._nLow = Fl;
			}				
			if (hmNodeWeight.containsKey(F))
				hmNodeWeight.remove(F);
			Pair<Pair<Double, ArrayList<Double>>, Pair<Double, ArrayList<Double>>> branch = new Pair<Pair<Double, ArrayList<Double>>, Pair<Double, ArrayList<Double>>>(new Pair<Double, ArrayList<Double>>(highWeight, highList), new Pair<Double, ArrayList<Double>>(lowWeight, lowList));
			hmNodeWeight.put(F, branch);
			Pair<Double, ArrayList<Double>> newBranch = JoinBranches(branch);
			weight._alWeights = newBranch._o2;
			weight._dWeight = newBranch._o1;
			return F;
		}
	}

	public static int insertValueInDD4(int F, ArrayList state, double value, Iterator<CString> it, HashMap<String,String> hmPrimeRemap, ADD _context, HashMap<Integer, Integer> iD2ADD,HashMap<Integer, Pair<Pair<Double, Double>, Pair<Double, Double>>> hmNodeWeight, doubleOutPut3 weight) {
		int Fh, Fl;
		//There are no more elements in the alStateVars 	
		if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
			weight._dMaxWeight = value;
			weight._dMinWeight = value;
			//int newF = _context.getConstantNode(value);
			//hmNodeWeight.put(newF, new Pair<Double, Double>(value, 0d));
			return _context.getConstantNode(value);//newF;
		}
		else{
			String varStr = ((CString)it.next())._string;
			Integer var=(Integer)_context._hmVarName2ID.get(varStr);
			Object valueVar =state.get((Integer)_context._hmGVarToLevel.get(var));
			Boolean val=(valueVar != null) && (Boolean)valueVar;
			ADDNode cur = _context.getNode(F);
			Boolean unchangedBranchLow = false;
			//If F is a leaf and there are more elements in alStateVars or the variable is not in the ADD
			// means that we need to create the node(s) with the remain variable(s)
			int FforH = F, FforL = F;
			Boolean create = true;
			//If f is a internal Node and id state variable is equal to id Node
			if(cur instanceof ADDINode && var.compareTo(((ADDINode)cur)._nTestVarID) == 0){
				FforH = ((ADDINode)cur)._nHigh;
				FforL = ((ADDINode)cur)._nLow;
				create =  false;
			}
			if (val==true){
				Fh = insertValueInDD4(FforH, state, value, it, hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight);
				unchangedBranchLow = true;
				Fl = FforL;
			}
			else {
				Fh = FforH;
				Fl = insertValueInDD4(FforL, state, value, it,hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight);
			}
			//Computing the weight
			int fRequest = unchangedBranchLow? Fl : Fh;
			ADDNode nodeRequest = _context.getNode(fRequest);
			double maxWeightRequest=0;
			double minWeightRequest=0;
			if (nodeRequest instanceof ADDDNode){
				maxWeightRequest = ((ADDDNode)nodeRequest)._dLower;
				minWeightRequest = ((ADDDNode)nodeRequest)._dLower;
			}
			else{
				Pair<Double, Double> newBranch = JoinBranches4(hmNodeWeight.get(fRequest));
				maxWeightRequest = newBranch._o1;
				minWeightRequest = newBranch._o2;
			}
			Double highMaxWeight = unchangedBranchLow? weight._dMaxWeight : maxWeightRequest;
			Double highMinWeight = unchangedBranchLow? weight._dMinWeight : minWeightRequest;
			Double lowMaxWeight = unchangedBranchLow? maxWeightRequest: weight._dMaxWeight;
			Double lowMinWeight = unchangedBranchLow? minWeightRequest: weight._dMinWeight;
			if (create)
				F = _context.getINode(var,Fl,Fh, true);
			else{
				((ADDINode)cur)._nHigh =  Fh;
				((ADDINode)cur)._nLow = Fl;
			}				
			if (hmNodeWeight.containsKey(F))
				hmNodeWeight.remove(F);
			Pair<Pair<Double, Double>, Pair<Double, Double>> branch = new Pair<Pair<Double, Double>, Pair<Double, Double>>(new Pair<Double, Double>(highMaxWeight, highMinWeight), new Pair<Double, Double>(lowMaxWeight, lowMinWeight));
			hmNodeWeight.put(F, branch);
			Pair<Double, Double> newBranch = JoinBranches4(branch);
			weight._dMinWeight = newBranch._o2;
			weight._dMaxWeight = newBranch._o1;
			return F;
		}
	}

	public static int insertValueInDD2(int F, ArrayList state, double value, Iterator<CString> it, HashMap<String,String> hmPrimeRemap, ADD _context, HashMap<Integer, Integer> iD2ADD,HashMap<Integer, Pair<Pair<Double, Integer>, Pair<Double, Integer>>> hmNodeWeight, doubleOutPut2 weight) {
		int Fh, Fl;
		//There are no more elements in the alStateVars 	
		if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
			weight._dWeight = value;
			weight._nWeights = 1;
			//int newF = _context.getConstantNode(value);
			//hmNodeWeight.put(newF, new Pair<Double, Double>(value, 0d));
			return _context.getConstantNode(value);//newF;
		}
		else{
			String varStr = ((CString)it.next())._string;
			Integer var=(Integer)_context._hmVarName2ID.get(varStr);
			Object valueVar =state.get((Integer)_context._hmGVarToLevel.get(var));
			Boolean val=(valueVar != null) && (Boolean)valueVar;
			ADDNode cur = _context.getNode(F);
			Boolean unchangedBranchLow = false;
			//If F is a leaf and there are more elements in alStateVars or the variable is not in the ADD
			// means that we need to create the node(s) with the remain variable(s)
			int FforH = F, FforL = F;
			Boolean create = true;
			//If f is a internal Node and id state variable is equal to id Node
			if(cur instanceof ADDINode && var.compareTo(((ADDINode)cur)._nTestVarID) == 0){
				FforH = ((ADDINode)cur)._nHigh;
				FforL = ((ADDINode)cur)._nLow;
				create =  false;
			}
			if (val==true){
				Fh = insertValueInDD2(FforH, state, value, it, hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight);
				unchangedBranchLow = true;
				Fl = FforL;
			}
			else {
				Fh = FforH;
				Fl = insertValueInDD2(FforL, state, value, it,hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight);
			}
			//Computing the weight
			int fRequest = unchangedBranchLow? Fl : Fh;
			ADDNode nodeRequest = _context.getNode(fRequest);
			double weightRequest=0;
			int nRequest= 0;
			if (nodeRequest instanceof ADDDNode){
				weightRequest = ((ADDDNode)nodeRequest)._dLower;
				nRequest = 1;
			}
			else{
				Pair<Double, Integer> newBranch = JoinBranches2(hmNodeWeight.get(fRequest));
				nRequest = newBranch._o2;
				weightRequest = newBranch._o1;
			}
			Double highWeight = unchangedBranchLow? weight._dWeight : weightRequest;
			Integer highNum = unchangedBranchLow? weight._nWeights : nRequest;
			Double lowWeight = unchangedBranchLow? weightRequest: weight._dWeight;
			Integer lowNum = unchangedBranchLow? nRequest: weight._nWeights;
			if (create)
				F = _context.getINode(var,Fl,Fh, true);
			else{
				((ADDINode)cur)._nHigh =  Fh;
				((ADDINode)cur)._nLow = Fl;
			}				
			if (hmNodeWeight.containsKey(F))
				hmNodeWeight.remove(F);
			Pair<Pair<Double, Integer>, Pair<Double, Integer>> branch = new Pair<Pair<Double, Integer>, Pair<Double, Integer>>(new Pair<Double, Integer>(highWeight, highNum), new Pair<Double, Integer>(lowWeight, lowNum));
			hmNodeWeight.put(F, branch);
			Pair<Double, Integer> newBranch = JoinBranches2(branch);
			weight._nWeights = newBranch._o2;
			weight._dWeight = newBranch._o1;
			return F;
		}
	}

	
	public static int insertValueInDD3(int F, ArrayList state, double value, Iterator<CString> it, HashMap<String,String> hmPrimeRemap, ADD _context, HashMap<Integer, Integer> iD2ADD,HashMap<Integer, Pair<Double, Double>> hmNodeWeight, doubleOutPut2 weight,HashMap<Integer, Pair<Double, Double>> hmVarWeight) {
		int Fh, Fl;
		//There are no more elements in the alStateVars 	
		if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
			weight._dWeight = value;
			weight._nWeights = 1;
			//int newF = _context.getConstantNode(value);
			//hmNodeWeight.put(newF, new Pair<Double, Double>(value, 0d));
			return _context.getConstantNode(value);//newF;
		}
		else{
			String varStr = ((CString)it.next())._string;
			Integer var=(Integer)_context._hmVarName2ID.get(varStr);
			Object valueVar =state.get((Integer)_context._hmGVarToLevel.get(var));
			Boolean val=(valueVar != null) && (Boolean)valueVar;
			ADDNode cur = _context.getNode(F);
			Boolean unchangedBranchLow = false;
			//If F is a leaf and there are more elements in alStateVars or the variable is not in the ADD
			// means that we need to create the node(s) with the remain variable(s)
			int FforH = F, FforL = F;
			Boolean create = true;
			//If f is a internal Node and id state variable is equal to id Node
			if(cur instanceof ADDINode && var.compareTo(((ADDINode)cur)._nTestVarID) == 0){
				FforH = ((ADDINode)cur)._nHigh;
				FforL = ((ADDINode)cur)._nLow;
				create =  false;
			}
			if (val==true){
				Fh = insertValueInDD3(FforH, state, value, it, hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight, hmVarWeight);
				unchangedBranchLow = true;
				Fl = FforL;
			}
			else {
				Fh = FforH;
				Fl = insertValueInDD3(FforL, state, value, it,hmPrimeRemap, _context, iD2ADD, hmNodeWeight, weight, hmVarWeight);
			}
			//Computing the weight
			int fRequest = unchangedBranchLow? Fl : Fh;
			ADDNode nodeRequest = _context.getNode(fRequest);
			double weightRequest=0;
			if (nodeRequest instanceof ADDDNode)
				weightRequest = ((ADDDNode)nodeRequest)._dLower;
			else
				weightRequest = JoinBranches3(hmNodeWeight.get(fRequest));
			Double highWeight = unchangedBranchLow? weight._dWeight : weightRequest;
			Double lowWeight = unchangedBranchLow? weightRequest: weight._dWeight;			
			if (create)
				F = _context.getINode(var,Fl,Fh, true);
			else{
				((ADDINode)cur)._nHigh =  Fh;
				((ADDINode)cur)._nLow = Fl;
			}				
			if (hmNodeWeight.containsKey(F))
				hmNodeWeight.remove(F);
			Pair<Double, Double> branch = new Pair<Double, Double>(highWeight, lowWeight);
			hmNodeWeight.put(F, branch);
			Double newBranch = JoinBranches3(branch);
			ADDINode nodeF = (ADDINode)_context.getNode(F); 
			if (hmVarWeight!= null)
			{
				if (hmVarWeight.containsKey(nodeF._nTestVarID))
				{
					Pair<Double, Double> branchVar = hmVarWeight.get(nodeF._nTestVarID);
					branch._o1 = JoinBranches3(new Pair<Double, Double>(branch._o1, branchVar._o1));
					branch._o2 = JoinBranches3(new Pair<Double, Double>(branch._o2, branchVar._o2));
					hmVarWeight.remove(nodeF._nTestVarID);
				}
				hmVarWeight.put(nodeF._nTestVarID, branch);
			}
			weight._dWeight = newBranch;
			return F;
		}
	}

	public static int insertValueInDD(int F, ArrayList state, double value, Iterator<String> it, HashMap<String,String> hmPrimeRemap, ADD _context) {
		int Fh, Fl;
		//There are no more elements in the alStateVars 	
		if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
			return _context.getConstantNode(value);
		}
		else{
			String varStr = (String)it.next();
			Integer var = (Integer)_context._hmVarName2ID.get(varStr);
			Object valueVar =state.get((Integer)_context._hmGVarToLevel.get(var));
			Boolean val=(valueVar != null) && (Boolean)valueVar;
			Integer varPrime= (Integer)_context._hmVarName2ID.get(hmPrimeRemap.get(varStr));
			if(varPrime==null){ // this was inserted for RTDPEnum and BRTDPEnum in order to use it for states with prime or non-prime variables
				varPrime=var;
			}
			ADDNode cur = _context.getNode(F);
			//If F is a leaf and there are more elements in alStateVars or the variable is not in the ADD
			// means that we need to create the node(s) with the remain variable(s)
			int FforH = F, FforL = F;
			Boolean create = true;
			//If f is a internal Node and id state variable is equal to id Node
			if(cur instanceof ADDINode && varPrime.compareTo(((ADDINode)cur)._nTestVarID) == 0){
				FforH = ((ADDINode)cur)._nHigh;
				FforL = ((ADDINode)cur)._nLow;
				create =  false;
			}
			if (val==true){
				Fh = insertValueInDD(FforH, state, value, it, hmPrimeRemap, _context);
				Fl = FforL;
			}
			else {
				Fh = FforH;
				Fl = insertValueInDD(FforL, state, value, it,hmPrimeRemap, _context);
			}
			if (create)
				F = _context.getINode(var,Fl,Fh, true);
			else{
				((ADDINode)cur)._nHigh =  Fh;
				((ADDINode)cur)._nLow = Fl;
			}
			return F;
		}
	}

	/*
	*/
}

