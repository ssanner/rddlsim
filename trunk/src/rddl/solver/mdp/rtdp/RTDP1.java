package rddl.solver.mdp.rtdp;


import java.io.File;

import dd.discrete.ADD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;

//import mdp.Action;


import util.*;


import rddl.EvalException;
import rddl.RDDL;
import rddl.State;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.EXPR;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.parser.parser;
import rddl.policy.Policy;
import rddl.policy.RandomBoolPolicy;
import rddl.sim.Result;
import rddl.sim.Simulator;
import rddl.translate.RDDL2Format;
import rddl.viz.StateViz;
import dd.discrete.ADDBNode;
import dd.discrete.ADDDNode;
import dd.discrete.ADDINode;
import dd.discrete.ADDNode;

//import mdp.Action;
//import mdp.MDP;
//import add.Context;

public class RTDP1 {
		
		public RDDL2Format _rddl2format;
		public State       _state;
		public long		   _timeTrials;
		public String      _NAME_FILE_VALUE;
		private double maxUpper,minLower;
		private double maxUpperUpdated,maxLowerUpdated;
		public int   VUpper;
		public int   VLower;
		public int   VGap;
		public int contUpperUpdates;
		public ArrayList _vars;
		
		public RTDP1(RDDL rddl, String instance_name) throws Exception {
			_rddl2format = new RDDL2Format(rddl, instance_name, RDDL2Format.SPUDD_CURR, "");
			_state = new State();
			resetState();			
			_vars = new ArrayList();
			Iterator x=_rddl2format._hmPrimeRemap.keySet().iterator();
			while (x.hasNext()){
				_vars.add(x.next());
			}
			solveRTDP(instance_name, 1, 4);			
		}
		
		/*public void computeCompletedCPTs()
		{
			Iterator actions=_rddl2format._hmActionMap.keySet().iterator();
			while (actions.hasNext()){
				Iterator it =_rddl2format._hmPrimeRemap.keySet().iterator();
				String actionName= (String)actions.next();
				while (it.hasNext())
				{
					String xiprime = (String)it.next();
					int idxiprime = (Integer)_rddl2format._context._hmVarName2ID.get(xiprime);
					Pair<String, String> p = new Pair<String, String>(actionName, xiprime);
					int inv = _rddl2format._context.complement(idxiprime);
					int newCPT=_rddl2format._context.getINode(idxiprime, idxiprime, inv, true);
					_rddl2format._varPrime2transDD.put(p, newCPT);
				}
			}
		}*/
		public void resetState() {
			_state.init(_rddl2format._n != null ? _rddl2format._n._hmObjects : null, _rddl2format._i._hmObjects,  
					_rddl2format._d._hmTypes, _rddl2format._d._hmPVariables, _rddl2format._d._hmCPF,
					_rddl2format._i._alInitState, _rddl2format._n == null ? null : _rddl2format._n._alNonFluents,
					_rddl2format._d._alStateConstraints, _rddl2format._d._exprReward, _rddl2format._i._nNonDefActions);
		}
		
		private double computeQ(int V,State state, String actionName) {
			//it is not necessary to do remapIdWithPrime because V has all prime variables 
			int VPrime=V; 
			//_rddl2format._context.getGraph(VPrime).launchViewer();
			String xiprime, xi;
			Iterator x=_rddl2format._hmPrimeRemap.entrySet().iterator();
			int idxiprime = 0, idxi =0;
			while (x.hasNext()){
				Map.Entry xiprimeme = (Map.Entry)x.next();
				xiprime= (String)xiprimeme.getValue();
				xi = (String)xiprimeme.getKey();
				idxiprime = (Integer)_rddl2format._context._hmVarName2ID.get(xiprime);
				idxi = (Integer)_rddl2format._context._hmVarName2ID.get(xi);
				Pair<String, String> p = new Pair<String, String>(actionName, xi);
				int cpt_a_xiprime=_rddl2format._var2transDD.get(p);
				double probTrue,probFalse;
				probTrue=evaluateState(cpt_a_xiprime,_state._state, _vars);
				probFalse=1-probTrue;
				int Fh=_rddl2format._context.getConstantNode(probTrue);
				int Fl=_rddl2format._context.getConstantNode(probFalse);
				int newCPT=_rddl2format._context.getINode(idxiprime, Fh, Fl, true);
				VPrime = _rddl2format._context.applyInt(VPrime, newCPT, dd.discrete.DD.ARITH_PROD);				
				VPrime = _rddl2format._context.opOut(VPrime, idxiprime, dd.discrete.DD.ARITH_SUM);
			}
			double total_sum = ((ADDDNode)_rddl2format._context.getNode(VPrime))._dLower;
			double reward = getRewardState(actionName);
			return (_rddl2format._i._dDiscount*total_sum) + reward;
		}

		//////////////////////////////////////////////////////////////////////////////
		public void ResetTimer() {
			_timeTrials = System.currentTimeMillis();
		}
		
		private String updateVUpper() {
			double max=Double.NEGATIVE_INFINITY;
			String actionGreedy=null;
			Iterator actions=_rddl2format._hmActionMap.keySet().iterator();
			while (actions.hasNext()){
				String actionName = (String)actions.next();
				double Qt = computeQ(VUpper, _state,actionName);
				max=Math.max(max,Qt);
				if(max==Qt){
					actionGreedy=actionName;
				}
			}
			double maxTotal=max;
	        //update the ADD VUpper
			updateValueBranch('u',maxTotal);
			maxUpperUpdated=maxTotal; //Error, maxUpper must not be updated, we need to use other variable
			return actionGreedy;
		}
		
		private void updateValueBranch(char c, double value) {
			if (c=='u'){
				VUpper = insertValueInDD(VUpper, _state._state,  value, _rddl2format._hmPrimeRemap);		
			}
			else if(c=='l'){
				VLower = insertValueInDD(VLower, _state._state,  value, _rddl2format._hmPrimeRemap);		
			}
			else if(c=='g'){
				VGap = insertValueInDD(VGap, _state._state,  value, _rddl2format._hmPrimeRemap);
			}
	    }
		
		public double evaluateState(int id, HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> state, ArrayList vars) {
			ArrayList vars2 = new ArrayList();
			for (int i=0;i<vars.size();i++)
				vars2.add(vars.get(i));
			ArrayList assign = new ArrayList();
			for (int i = 0; i <= _rddl2format._context._alOrder.size(); i++)
				assign.add(null);
			for (Object o : state.entrySet()) {
				Map.Entry me = (Map.Entry)o;
				HashMap<ArrayList<LCONST>,Object>  value = (HashMap<ArrayList<LCONST>,Object>)me.getValue();
				String prefix = ((PVAR_NAME)me.getKey()).toString();
				for (Object ob : value.entrySet()) {
					Map.Entry mev = (Map.Entry)ob;
					String var = prefix.replace("-", "_")+"__"+mev.getKey().toString().replace("[", "").replace("]", "").replace(" ", "").replace(",", "_").replace("-", "_");
					int index = (Integer)_rddl2format._context._hmVarName2ID.get(var); // if null, var not in var2ID
					int level = (Integer)_rddl2format._context._hmGVarToLevel.get(index);
					Boolean b = (Boolean)mev.getValue() != null;
					vars2.remove(var);
					assign.set(level, b);
				}
			}
			for (int i=0;i<vars2.size();i++)
			{
				int index = (Integer)_rddl2format._context._hmVarName2ID.get((String)vars2.get(i)); // if null, var not in var2ID
				int level = (Integer)_rddl2format._context._hmGVarToLevel.get(index);
				assign.set(level, false);
			}
			return _rddl2format._context.evaluate(id, assign);
		}
		public int insertValueInDD(int F, HashMap<String, Boolean> state, double value, Iterator it, HashMap<String,String> hmPrimeRemap) {
			int Fh, Fl;
			if (!it.hasNext()){//means that we are in a leaf then we need to replace the value
				return _rddl2format._context.getConstantNode(value);
			}
			String varStr = (String)it.next();
			Integer var=(Integer)_rddl2format._context._hmVarName2ID.get(varStr);
			Boolean val=state.get(varStr);
			val = (val != null);			
			Integer varPrime= (Integer)_rddl2format._context._hmVarName2ID.get(hmPrimeRemap.get(varStr));
			if(varPrime==null){ // this if was inserted for RTDPEnum and BRTDPEnum because we want to serve for states with prime or non-prime variables
				varPrime=var;
			}
			ADDNode cur = _rddl2format._context.getNode(F);
			if((cur instanceof ADDDNode) || (cur instanceof ADDBNode)){
				// means that we need to create the nodes with the remain variables
		
				if (val==true){
					Fh = insertValueInDD(F, state, value, it, hmPrimeRemap);
					Fl = F;
				}
				else {//val=false
					Fh = F;
					Fl = insertValueInDD(F, state, value, it,hmPrimeRemap);
				}
				return _rddl2format._context.getINode(varPrime, Fl, Fh, true);
			}
			ADDINode cur1 =  (ADDINode)_rddl2format._context.getNode(F);
			Integer Fvar= cur1._nTestVarID;
			if(Fvar.compareTo(varPrime)==0){
				if (val==true){
					Fh = insertValueInDD(cur1._nHigh, state, value, it,hmPrimeRemap);
					Fl = cur1._nLow;
				}
				else {//val=false
					Fh =cur1._nHigh;
					Fl = insertValueInDD(cur1._nLow, state, value, it,hmPrimeRemap);
				}	
				return _rddl2format._context.getINode(varPrime,Fh,Fl, true);

			}
			//Fvar.compareTo(var)!=0
			if (val==true){
				Fh = insertValueInDD(F, state, value, it,hmPrimeRemap);
				Fl = F;
			}
			else {//val=false
				Fh = F;
				Fl = insertValueInDD(F, state, value, it,hmPrimeRemap);
			}
			return _rddl2format._context.getINode(varPrime,Fh,Fl, true);
		}

		public int insertValueInDD(int id, HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> state, double value, HashMap<String,String> hmPrimeRemap) {
			HashMap<String, Boolean> assign = new HashMap<String, Boolean>();
			for (Object o : state.entrySet()) {
				Map.Entry me = (Map.Entry)o;
				HashMap<ArrayList<LCONST>,Object>  val = (HashMap<ArrayList<LCONST>,Object>)me.getValue();
				String prefix = ((PVAR_NAME)me.getKey()).toString();
				for (Object ob : val.entrySet()) {
					Map.Entry mev = (Map.Entry)ob;
					String var = prefix+"__"+mev.getKey().toString().replace("[", "").replace("]", "").replace(" ", "").replace(",", "_"); // if null, var not in var2ID
					assign.put(var, (Boolean)mev.getValue());
				}
			}
			Iterator it = hmPrimeRemap.keySet().iterator();
			return insertValueInDD(id, assign, value, it, hmPrimeRemap);
		}

		
		public double getRewardState(String actionName)
		{
			return evaluateState(sum_reward(actionName), _state._state, _vars);
		}
		//TODO
		public ArrayList<LCONST> parameters()
		{
			return new ArrayList<LCONST>();
		}
		
		public int sum_reward(String ActionName)
		{
			ArrayList<Integer> lista = _rddl2format._act2rewardDD.get(ActionName);
			int vsum = _rddl2format._context.getConstantNode(0.0);
			for (int i = 0;i<lista.size();i++){
				vsum = _rddl2format._context.applyInt(vsum, lista.get(i), ADD.ARITH_SUM);
			}
			return vsum;
		}

		public ArrayList terms(String termsStr)
		{
			ArrayList Result = new ArrayList();
			String[] ArrTerms = termsStr.split("_");			
			for (int i=0;i<ArrTerms.length;i++)
			{
				LCONST a = new LCONST(ArrTerms[i]) {
				};
				Result.add(a);
			}
			return Result;
		}
		
		
		private State chooseNextStateRTDP(State state, String actionGreedy, Random randomGenerator) {
			State nextState=new State();
			Iterator x=_rddl2format._hmPrimeRemap.keySet().iterator();
			ArrayList<PVAR_INST_DEF> newlist = new ArrayList<PVAR_INST_DEF>();
			while (x.hasNext()){
				String xiprime=(String)x.next(); //xiprimeme.getValue();
				int idxiprime = (Integer)_rddl2format._context._hmVarName2ID.get(xiprime);
				Pair<String, String> p = new Pair<String, String>(actionGreedy, xiprime);
				int cpt_a_xiprime=_rddl2format._var2transDD.get(p);//ver despues el complemento
				double probTrue;
				probTrue=evaluateState(cpt_a_xiprime,_state._state, _vars);
				double ran = randomGenerator.nextDouble();
				String predname = xiprime.substring(0,xiprime.indexOf("__"));
				newlist.add(new PVAR_INST_DEF(predname, ran<=probTrue, terms(xiprime.replace(predname+"__", ""))));
			}
			nextState.init(_rddl2format._n != null ? _rddl2format._n._hmObjects : null, _rddl2format._i._hmObjects,  
					_rddl2format._d._hmTypes, _rddl2format._d._hmPVariables, _rddl2format._d._hmCPF,
					newlist, _rddl2format._n == null ? null : _rddl2format._n._alNonFluents,
					_rddl2format._d._alStateConstraints, _rddl2format._d._exprReward, _rddl2format._i._nNonDefActions);
			return nextState;
		}
		
		public void solveRTDP(String instance, int maxTrials, int maxDepth) throws EvalException {
			
			double discount = _rddl2format._i._dDiscount;
			// Reset to initial state
			_NAME_FILE_VALUE=_NAME_FILE_VALUE+"_"+instance+".net";//NAME_FILE_VALUE is inicializated in MDP_Fac(...)
			// Set random seed for repeatability
			Random _rand = new Random();
			// Keep track of reward
			Stack<State> visited=new Stack<State>();
			ArrayList<LCONST> params = parameters();
			PVAR_NAME GOAL = new PVAR_NAME("GOAL");
			ResetTimer();
			State state;
			String actionGreedy=null;
			//Initialize Vu with admissible value function //////////////////////////////////
			//create an ADD with  V_u=Rmax/1-gamma //////////////////////////////////
			Iterator it= _rddl2format._act2rewardDD.keySet().iterator();
			double Rmax=Double.NEGATIVE_INFINITY;
			while(it.hasNext())
			{
				ArrayList<Integer> lista = _rddl2format._act2rewardDD.get(it.next());
				double max = 0;
				for (int i = 0;i<lista.size();i++){
					max += _rddl2format._context.getMax(lista.get(i));
				}
				Rmax = Math.max(Rmax, max);
			}
			if(discount == 1){
				maxUpper=Rmax*maxDepth;
			}
			else{
				maxUpper=Rmax/(1-discount);
			}
			VUpper=_rddl2format._context.getConstantNode(maxUpper);
			contUpperUpdates=0;
			// Run problem for specified horizon
			for(int trial=1; trial <= maxTrials;trial++){	
				int depth=0;
				visited.clear();// clear visited states stack
				//draw initial state //////////////////////////////////
				resetState();
				//do trial //////////////////////////////////
				while((params.size()==0 || !(Boolean)_state.getPVariableAssign(GOAL, params)) && (_state !=null)&& depth<maxDepth){ //contUpperUpdates<maxUpdates){//not update more than maxUpdates in the last iteration
					depth++;
					visited.push(_state);
					//this compute maxUpperUpdated and actionGreedy
					actionGreedy=updateVUpper();
					contUpperUpdates++;
					State newState = chooseNextStateRTDP(_state, actionGreedy, _rand);
					_state = newState;
					//flushCachesRTDP(false);
				}
				_rddl2format._context.getGraph(VUpper).launchViewer();
				//do optimization
				while(!visited.empty()){
					_state=visited.pop();
					updateVUpper();
					contUpperUpdates++;
				}
				////////////////////////////////////////////////////////////////////
				//ResetTimer();
			}
			// Problem over, return objective and list of rewards (e.g., for std error calc)		
			//return new Result(accum_reward, rewards);
		}

    public static void main(String[] args) throws Exception {
    	if (args.length < 2) {
			System.out.println("usage: RDDL-file Instance-name");
			System.exit(1);
		}
		String rddl_file = args[0];
		String policy_class_name = "rddl.policy.RandomBoolPolicy";
		String instance_name = args[1];
    	RDDL rddl = new RDDL();
		File f = new File(rddl_file);
		if (f.isDirectory()) {
			for (File f2 : f.listFiles())
				if (f2.getName().endsWith(".rddl")) {
					System.out.println("Loading: " + f2);
					rddl.addOtherRDDL(parser.parse(f2));
				}
		} else
			rddl.addOtherRDDL(parser.parse(f));		
		RTDP1 solv = new RTDP1(rddl, instance_name);
		
		/*Policy pol = (Policy)Class.forName(policy_class_name).newInstance();
		//StateViz viz = (StateViz)Class.forName(state_viz_class_name).newInstance();
	
		Result r = solv.run(pol);//, viz, rand_seed);
		System.out.println("==> " + r);
		if (RDDL.DEBUG_PVAR_NAMES)
			System.out.println(RDDL.PVAR_SRC_SET);*/
    }
}