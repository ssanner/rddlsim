package rddl.translate.cnf;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import rddl.RDDL.INSTANCE;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL;
import rddl.State;
import rddl.parser.parser;
import rddl.translate.RDDL2Format;
import rddl.translate.cnf.CNFClause.ClauseSources;
import util.Pair;

import dd.discrete.ADD;
import dd.discrete.ADDDNode;
import dd.discrete.ADDINode;
import dd.discrete.ADDNode;
import dd.discrete.DD;

public class CNF {
	public final static String FORMAT = "cnf".intern();
	public final static String EOCLAUSE = "0".intern();
	public final static String COMMENTH = "c ".intern();
	public final static String COMMENTH_VAR = "c cv ".intern();
	public final static String PARAMETERH = "p ".intern();
	public final static String PRO_SUFFIX = "_pro".intern();
	public final static String COST_SUFFIX = "_cost".intern();
	public final static String BLANK = " ".intern();
	public final static String NULLSTRING = "".intern();
	public final static String COLON = " : ".intern();
	public final static String NEWLINE = "\n".intern();
	public final static String OUTCOME = "_outcome_".intern();
	public final static String AT = "@".intern();
	//state , action , cost , outcome , probability
	public final static String[] VAR_TYPE = {"STA","ACT","COS","OUT","PRO"};
	
	public String _header_domain = null;
	public String _header_name	= null;
	public String _comment = null;
	public HashMap<String, Integer> _var2Integer = null;
	public HashMap<Integer, String> _integer2Var = null;
	public HashMap<String, Integer> _actionmapping = null;
	public HashMap<String, Integer> _statemapping = null;
	public HashMap<String, Integer> _obsermapping = null;
	public HashMap<String, Integer> _newvarmapping = null;
	public HashMap<String, Double> _costvalue	= null;
	public HashMap<String,Boolean> _initStates = null;
	
	public HashMap<Literal,ArrayList<Literal>> _change2outcomes = null;
	public HashMap<String, Literal> _outcomes2state = null;
	public HashMap<String, ArrayList<ArrayList<Literal>>> _action2outcomes = null;
	public HashMap<Literal, ArrayList<Literal>> _outcome2probability = null;
	
//	public HashMap<Literal, Double> _outcomes2pro = null;
	public HashMap<String, Double> _newvar2weight = null;
	
	public ArrayList<CNFClause> _clauses = null;
	
	private Integer varNumber = null;
	private Integer varNoPerTS = null;
	private Integer clauseNumber = null;
	private Integer timestep = null;
	private HashMap<String, Pair<Literal,Literal>> curLiterals = null;
	private HashMap<String, Pair<Literal,Literal>> nextLiterals = null;
	private HashMap<String, Boolean> stateDef = null;
	
	public CNF(RDDL2Format r2s)
	{
		_CNF(r2s._alAllVars, 
				r2s._alStateVars, 
				r2s._alObservVars, 
				r2s._hmActionMap, 
				r2s._var2transDD, 
				r2s._var2observDD, 
				r2s._act2rewardDD, 
				r2s._context, 
				r2s._i,
				r2s._state);
	}
	
	private void _CNF(	ArrayList<String> _alAllVars, 
			ArrayList<String> _alStateVars,
			ArrayList<String> _alObservVars,
			Map<String,ArrayList<PVAR_INST_DEF>> _hmActionMap,
			TreeMap<Pair, Integer> _var2transDD, 
			TreeMap<Pair, Integer> _var2observDD,
			TreeMap<String, ArrayList<Integer>> _act2rewardDD,
			ADD context,
			INSTANCE i, State _state)
	{
		_var2Integer = new HashMap<String, Integer>();
		_integer2Var = new HashMap<Integer, String>();
		_actionmapping = new HashMap<String, Integer>();
		_statemapping = new HashMap<String, Integer>();
		_obsermapping = new HashMap<String, Integer>();
		_newvarmapping = new HashMap<String, Integer>();
		_costvalue	= new HashMap<String, Double> ();
		_initStates = new HashMap<String, Boolean>();
		_newvar2weight = new HashMap<String, Double>();
		_outcome2probability = new HashMap<Literal, ArrayList<Literal>>();
		_clauses = new ArrayList<CNFClause>();
		curLiterals = new HashMap<String, Pair<Literal,Literal>>();
		nextLiterals = new HashMap<String, Pair<Literal,Literal>>();
		varNumber = 0;
		varNoPerTS = 0;
		clauseNumber = 0;
		timestep = i._nHorizon;
		_header_domain = i._sDomain;
		_header_name = i._sName;
		//init var mapping : var of states
		for(String state: _alStateVars)
		{
			if(!_var2Integer.containsKey(state))
			{
				mapVarInteger(state,_statemapping);

				Literal positiveL = new Literal(false,state,true);
				Literal negativeL = new Literal(true,state,true);
				
				add2CurLiteralList(positiveL, negativeL);
				

				Literal nextPosL = new Literal(false,state,false);
				Literal nextNegL = new Literal(true,state,false);
				Pair nextLiterallPair = new Pair(nextPosL, nextNegL);
				nextLiterals.put(state, nextLiterallPair);
			}
		}
		
		//init var mapping : var of actions		
		for(String action: _hmActionMap.keySet())
		{
			if(!_var2Integer.containsKey(action))
			{
				mapVarInteger(action,_actionmapping);

				Literal positiveL = new Literal(false,action,true);
				Literal negativeL = new Literal(true,action,true);

				add2CurLiteralList(positiveL, negativeL);
			}
		}
		
		//This segment of Observation is not tested
		//init var mapping : var of observ
		for(String observ: _alObservVars)
		{
			if(!_var2Integer.containsKey(observ))
			{
				mapVarInteger(observ, _obsermapping);

				Literal positiveL = new Literal(false,observ,true);
				Literal negativeL = new Literal(true,observ,true);

				add2CurLiteralList(positiveL, negativeL);
			}
		}
		
		
		stateDef = new HashMap<String, Boolean>();
		HashMap<String, Boolean> temp = new HashMap<String,Boolean>();
		//Get clauses about init states
		for (PVAR_INST_DEF def : i._alInitState) {	
			// Get the assignments for this PVAR
			temp.put(RDDL2Format.CleanFluentName(def._sPredName.toString() + def._alTerms), (Boolean)def._oValue);			
		}
		for(String state: _alStateVars)
		{
			
			CNFClause init = new CNFClause();
			Literal l = null;
			PVAR_NAME p = new PVAR_NAME(state.split("__")[0]);
			boolean defValue = (Boolean)_state.getDefaultValue(p);
			stateDef.put(state, defValue);
			Boolean bval = temp.get(state);
			if (bval == null) { // No assignment, get default value
				// This is a quick fix... a bit ugly
				bval = defValue;
			}
			Pair<Literal, Literal> pair = (Pair<Literal, Literal>)curLiterals.get(state);
			if (bval)
				l = pair._o1;
			else
				l = pair._o2;
			
			init.addLiteral(l);
			_initStates.put(state, bval);
			addClause(init);
			
		}
		
		
		//Get clauses about  action mutual exclusion: at least one be executed
		//Get clauses about  action mutual exclusion: at most one be executed
		frameAxiomActionAtLeastMost(_actionmapping.keySet());

		_change2outcomes = new HashMap<Literal,ArrayList<Literal>>();
		_outcomes2state = new HashMap<String, Literal>();
		_action2outcomes = new HashMap<String, ArrayList<ArrayList<Literal>>>();
		
//		_outcomes2pro = new HashMap<Literal, Double>();
		
		
		//Get clauses about action
		for(String action: _actionmapping.keySet())
		{
			ArrayList<ArrayList<Literal>> lists = new ArrayList<ArrayList<Literal>>();
			_action2outcomes.put(action, lists);
			for(String state : _statemapping.keySet())
			{
				int index = _var2transDD.get(new Pair(action, state));
				CNFClause clause = new CNFClause(ClauseSources.EFFECT);
				Literal l = (Literal)curLiterals.get(action)._o2;
				clause.addLiteral(l);
				ArrayList<Literal> outcomes = new ArrayList<Literal>();
				lists.add(outcomes);
				int oldVarCnt = _integer2Var.size();
				convert2Clauses(null, state, index, null, context, 2, clause, true);
				for(index = oldVarCnt+1; index <= _integer2Var.size(); index++)
				{
					String outcomeName= _integer2Var.get(index);
					if(outcomeName.contains(OUTCOME) && curLiterals.containsKey(outcomeName))
					{
						Literal outcome = curLiterals.get(_integer2Var.get(index))._o1;
						outcomes.add(outcome);
					}
				}
			}
			
			// This segment of Observation is not tested.
			//Get clauses from Observation
			for(String obser : _obsermapping.keySet())
			{
				int index = _var2observDD.get(new Pair(action, obser));
				CNFClause clause = new CNFClause(ClauseSources.OBSERVATION);
				Literal l = (Literal)curLiterals.get(action)._o2;
				clause.addLiteral(l);
				convert2Clauses(null, obser, index, null, context, 2, clause, true);
			}

			//Get clauses from cost
			ArrayList<Integer> rewards = _act2rewardDD.get(action);
			if (rewards.size() > 0) {
				for (int reward_dd : rewards) {
					int index = context.applyInt(context.getConstantNode(0d), reward_dd, DD.ARITH_MINUS);
					CNFClause clause = new CNFClause(ClauseSources.COST);
					Literal l = (Literal)curLiterals.get(action)._o2;
					clause.addLiteral(l);
					convert2Clauses(null, "", index, null, context, 2, clause, true);
				}
			}
			
		}
		
		//Get clauses from axioms
		frameAxiomChange2Outcomes();
		frameAxiomAction2Outcomes();
		frameAxiomAtMostOneOutcome();
		frameAxiomOutcome2Action();
		frameAxiomOutcome2State();
		frameAxiomOutcome2Probability();
		
		

		
		varNumber = varNoPerTS * timestep + _alStateVars.size();
		clauseNumber = (clauseNumber - _alStateVars.size()) * timestep + _alStateVars.size();
		if(_header_domain.contains("elevators_mdp") )
		{
			for(String state: _statemapping.keySet())
			{
				if(state.startsWith("person_"))
				{
					clauseNumber++;
				}
					
			}
		}
		else if(_header_domain.contains("Crossing_traffic_mdp"))
		{
			//Next line just for domain: Crossing_traffic_mdp
			clauseNumber++;
		}
		
	}

	private void frameAxiomOutcome2Probability() {
		for(Literal l : _outcome2probability.keySet())
		{
			ArrayList<Literal> probabilities = _outcome2probability.get(l);
			for(Literal pro: probabilities)
			{
				CNFClause clause = new CNFClause(ClauseSources.CONSTRAINT);
				clause.addLiteral(getNot(l));
				clause.addLiteral(pro);
				addClause(clause);
			}
		}
		
	}


	private void frameAxiomOutcome2State() {
		for(String outcome_name : _outcomes2state.keySet())
		{
			CNFClause clause = new CNFClause(ClauseSources.CONSTRAINT);
			clause.addLiteral(curLiterals.get(outcome_name)._o2);
			clause.addLiteral(_outcomes2state.get(outcome_name));
			addClause(clause);
			
		}
		
	}

	
	private void frameAxiomOutcome2Action() {
		
		for(String action_name : _action2outcomes.keySet())
		{
			ArrayList<ArrayList<Literal>> list = _action2outcomes.get(action_name);
			Literal action = curLiterals.get(action_name)._o1;
			for(ArrayList<Literal> outcomes: list)
			{
				if(outcomes == null)continue;
				for(Literal outcome: outcomes)
				{
					CNFClause clause = new CNFClause(ClauseSources.CONSTRAINT);
					clause.addLiteral(getNot(outcome));
					clause.addLiteral(action);
					addClause(clause);
				}
			}
		}
	}

	
	private void frameAxiomAtMostOneOutcome() {
		for(ArrayList<ArrayList<Literal>> list : _action2outcomes.values())
		{
			for(ArrayList<Literal> outcomes : list)
			{
				int size = outcomes.size();
				if(list.size() < 2)
				{
					System.out.println("Error: wrong count of outcomes" );
					continue;
				}
				for(int start = 0 ; start < (size -1) ; start++)
				{
					for(int cursor = start + 1 ; cursor < size ; cursor++)
					{

						CNFClause clause = new CNFClause(ClauseSources.CONSTRAINT);
						clause.addLiteral(getNot(outcomes.get(start)));
						clause.addLiteral(getNot(outcomes.get(cursor)));
						addClause(clause);
					}
				}
				
			}
		}
	}

	
	private void frameAxiomAction2Outcomes() {
		for(String action_name : _action2outcomes.keySet())
		{
			Literal action = curLiterals.get(action_name)._o2;
			ArrayList<ArrayList<Literal>> list = _action2outcomes.get(action_name);
			for(ArrayList<Literal> outcomes: list)
			{
				if(outcomes.size() < 2)
				{
					System.out.println("Error: wrong count of outcomes of action-" + action.getSymbol());
					continue;
				}
				CNFClause clause = new CNFClause(ClauseSources.CONSTRAINT);
				clause.addLiteral(action);
				for(Literal outcome : outcomes)
				{
					clause.addLiteral(outcome);
				}			
				addClause(clause);	
			}
		}
	}

	
	private void frameAxiomChange2Outcomes() {
		for(Literal nowstate : _change2outcomes.keySet())
		{
			if(nowstate == null)continue;
			Literal nextState = changedStateInNextTime(nowstate);
			ArrayList<Literal> outcomes = _change2outcomes.get(nowstate);
			CNFClause clause = new CNFClause(ClauseSources.CONSTRAINT);
			clause.addLiteral(getNot(nowstate));
			clause.addLiteral(getNot(nextState));
			for(Literal outcome : outcomes)
			{
				clause.addLiteral(outcome);
			}
			addClause(clause);
		}
	}

	
	private Literal getNot(Literal state) {
		Literal not = null;
		if(state == null) return not;
		String statename = state.getSymbol();
		not = state.getNegative()?curLiterals.get(statename)._o1:curLiterals.get(statename)._o2;
		return not;
	}

	
	private Literal changedStateInNextTime(Literal now) {
		Literal next = null;
		if(now == null) return next;
		String statename = now.getSymbol();
		next = now.getNegative()?nextLiterals.get(statename)._o1:nextLiterals.get(statename)._o2;
		return next;
	}

	
	public void tryGenNewVar(CNFClause c)
	{
		//For now, only support binary tree
		//act & con1 & con2 &... & probability(0,1) -> eff    ===>   
		//-act | -con1 | -con2 |...| -probability | out1  &
		//-act | -con1 | -con2 |...| probability | out2 &
		//out1 -> eff &
		//out2 -> -eff &

		ClauseSources csourse = c.getSource();
		
		if((csourse == ClauseSources.EFFECT) || (csourse == ClauseSources.OBSERVATION))
		{
			if(!c.isGend)
			{
				if(c.isProbabilistic())
				{
					// Generate new clause(clause) by copy from c (c - effect)
					/*
					 * 1. Get new clause 
					 * 2. Generate pro_var for each
					 * 3. Try generate outcome_var for each (And remove the effect Literal if has change)
					 * 4. Set true to both isGend 
					 * 5. Add clause to _clauses(by call the addClause method) */
					CNFClause clause = getOppositeClause(c);
					if(clause == null) 
					{
						System.out.println("Error: In generating opposite clause of --" + c._literals.toString()	);
					}
					else
					{
						Literal truel = genProVar(c);
						if(truel == null)
						{
							System.out.println("Error: Got null probability variable from" + c._literals.toString());
						}
						c.addLiteral(truel);
						Literal falsel = genProVar(clause);

						if(falsel == null)
						{
							System.out.println("Error: Got null probability variable(Generated one) from" + clause._literals.toString());
						}
						clause.addLiteral(falsel);
						
						tryGenOutcome(c);
						tryGenOutcome(clause);
						c.isGend = true;
						clause.isGend = true;
						addClause(clause);
						
					}		
					
				}
				else
				{
					// Generate new var of outcome if has change : for c
					/* 
					 * 1. Try generate outcome_var for c (And remove the effect Literal if has change)
					 * 2. c.isGend = true */
					tryGenOutcome(c);
					c.isGend = true;
				}
				
			}
		}
		else if(csourse == ClauseSources.COST && !c.isGend)
		{
			// Generate cost_var
			/* 1. Generate cost_var for each c 
			 * 2. c.isGend = true */
			genCostVar(c);
			c.isGend = true;
		}

	}
	
	
	private void genCostVar(CNFClause c) {

		String _newVar = null;
		_newVar = (c._literals.toString() + COST_SUFFIX + c.getWeight().toString()).intern();
		Literal l = null;
		if(curLiterals.containsKey(_newVar))
		{
			l = curLiterals.get(_newVar)._o1;
		}
		else
		{
			l = new Literal(false, _newVar, true);
			Literal falsel = new Literal(true, _newVar, true);
			add2CurLiteralList(l, falsel);
			_costvalue.put(_newVar, c.getWeight());
//			_newvar2weight.put(_newVar, c.getWeight());
			mapVarInteger(l.getSymbol(), _newvarmapping);
		}
		c.addLiteral(l);
	}


	private void tryGenOutcome(CNFClause c) {
		Literal eff = getEff(c._literals);
		if(eff == null)
		{
			System.out.println("Error: Failed to get effect for Outcome===>" + c._literals.toString());
		}
		if(c._literals.remove(eff))
		{
			
			// Generate new var of outcome if has change : for c and cc
			Literal changedS = findChange(eff, c._literals);

			//Get action name: the first Literal in the clause
			String action  = c._literals.get(0).getSymbol();
			
			//Generate outcomes in format: action_outcome_Number
			//Get Number
			ArrayList<ArrayList<Literal>> lists = _action2outcomes.get(action);
			if(lists == null)
			{
				lists = new ArrayList<ArrayList<Literal>>();
				_action2outcomes.put(action, lists);
			}
			Integer outcomeNumber = 0;
			for(ArrayList<Literal> outcomes : lists)
			{
				outcomeNumber += outcomes.size();
			}
			outcomeNumber++;
			String outcome_name = (c._literals.toString() +OUTCOME + outcomeNumber.toString()).intern();;//(action + c._literals.toString() +OUTCOME + outcomeNumber.toString()).intern();
			
			Literal outcomet = new Literal(false, outcome_name, true);
			Literal outcomef = new Literal(true, outcome_name, true);
			
			
			add2CurLiteralList(outcomet,outcomef);	
			
			
			//Add to var2Integer & integer2Var mapping adn add to new varmapping
			mapVarInteger(outcome_name, _newvarmapping);

			//Map action 2 outcome
//				outcomes.add(outcomet);
			
			
			
			//Map outcome 2 state
			if(_outcomes2state.containsKey(outcome_name))
			{
				Literal o2s =  (Literal)_outcomes2state.get(outcome_name);
				if( o2s != eff)
				{
					System.out.println("Error: Wrong mapping of outcome-1-" + outcome_name + " to " + eff.toString());
					System.out.println("Error: Wrong mapping of outcome-2-" + outcome_name + " to " + o2s.toString());
				}
			}
			else
			{
				_outcomes2state.put(outcome_name, eff);
			}
			

			//Mapping change to outcome
			if(_change2outcomes.containsKey(changedS))
			{
				_change2outcomes.get(changedS).add(outcomet);
			}
			else
			{
				ArrayList<Literal> changesOutcomes	= new ArrayList<Literal>();
				changesOutcomes.add(outcomet);
				_change2outcomes.put(changedS, changesOutcomes);
			}
			
			c.addLiteral(outcomet);	
			if(outcomet.getSymbol().contains(PRO_SUFFIX))
				_outcome2probability.put(outcomet, getProbabilities(c));
		}
		else
		{
			System.out.println("Error: Clause from effect without eff--" + c._literals.toString());
		}
		
	}


	private ArrayList<Literal> getProbabilities(CNFClause c) {
		ArrayList<Literal> probobilities = new ArrayList<Literal>();
		for(Literal l : c._literals)
		{
			if(l.getSymbol().contains(PRO_SUFFIX) 
					&& !l.getSymbol().contains(COST_SUFFIX)
					&& !l.getSymbol().contains(OUTCOME))
			{
				probobilities.add(getNot(l));
			}
		}
		
		return probobilities;
	}


	private Literal genProVar(CNFClause c) {
		Literal eff = getEff(c._literals);
		
		Literal proVar = null;
		String _newVar = null;
		if(eff == null)
		{
			return proVar;
		}
		c._literals.remove(eff);		
		_newVar = (c._literals.toString() + PRO_SUFFIX + c.getProbability()).intern();
		c._literals.add(eff);
		
		if(_var2Integer.containsKey(_newVar))
		{
			proVar = eff.getNegative()?curLiterals.get(_newVar)._o1:curLiterals.get(_newVar)._o2;
		}
		else
		{
			Literal truel = new Literal(false, _newVar, true);
			Literal falsel = new Literal(true, _newVar, true);					
			add2CurLiteralList(truel, falsel);
			_newvar2weight.put(_newVar, Math.ceil(Math.log(c.getProbability())*(-10)));
			mapVarInteger(truel.getSymbol(), _newvarmapping);
			proVar = eff.getNegative()?truel:falsel;
		}
		
		return proVar;
	}


	private CNFClause getOppositeClause(CNFClause c) {
		Literal eff = getEff(c._literals);
		CNFClause clause = null;
		if(eff != null && !eff._currentTS)
		{
			if(c._literals.remove(eff))
			{
				clause = c.clone();
				boolean negative = eff.getNegative();
				String varName = eff.getSymbol();
				Literal nonEff = negative?nextLiterals.get(varName)._o1:nextLiterals.get(varName)._o2;
				c.addLiteral(eff);
				clause.addLiteral(nonEff);
				//We do need 1 - probability: only one side of possible/impossible is need
				clause.setProbability(1-c.getProbability());
//				clause.setProbability(c.getProbability());
			}
			
		}
		else
		{
			System.out.println("Error: Find clause with incorrect eff from effect==>" + c._literals.toString());
		}
		return clause;
	}


	private Literal getEff(ArrayList<Literal> _literals) {
		Literal eff = null; 
		for(Literal l : _literals)
		{
			if(!l._currentTS)
			{
				eff = l;
				break;
			}
		}
		return eff;
	}


	public void addClause(CNFClause c)
	{
		tryGenNewVar(c);
		_clauses.add(c);
		clauseNumber++;

	}

	
	public void exportCNF(PrintWriter pw)
	{
		pw.println(COMMENTH + _header_domain + BLANK + _header_name);
		pw.println(COMMENTH + _comment);

		Integer temp = 0;

		for(int i = 0 ; i < timestep ; i++)
		{
			for(int index = 0; index < varNoPerTS ; index++)

			{
				//comment format: c index : type : variable_name@timestep
				String var = _integer2Var.get(index+1);
				pw.print(COMMENTH);
				String type = varType(var);
				temp = i * varNoPerTS + (index + 1);
				pw.print(temp.toString() + COLON + type + COLON + var + AT + String.valueOf(i));
				pw.print(NEWLINE);
			}
		}
		for(int index = 0; index < _statemapping.size() ; index++)
			{
				String var = _integer2Var.get(index+1);
				pw.print(COMMENTH);
				temp = timestep* varNoPerTS + (index + 1);
				pw.print(temp.toString() + COLON  + VAR_TYPE[0] + COLON + var + AT + String.valueOf(timestep));
				pw.print(NEWLINE);
			}
		
		//List all weighted variables
		for(int i = 0 ; i < timestep ; i++)
		{
			for(int index = 0; index < varNoPerTS ; index++)

			{
				String var = _integer2Var.get(index+1);
				if(!curLiterals.containsKey(var))
					continue;
//				Literal l = curLiterals.get(var)._o1;
//				if(_outcomes2pro.containsKey(l))
//				{
//					pw.print(COMMENTH_VAR);
//					temp = i * varNoPerTS + (index + 1);
//					Double weight = Math.ceil(Math.log(_outcomes2pro.get(l))*(-10));
//					pw.print(temp.toString() + BLANK + weight.intValue());
//					pw.print(NEWLINE);
//				}else 
				//if(_costvalue.containsKey(var))
				//{
				//	pw.print(COMMENTH_VAR);
				//	temp = i * varNoPerTS + (index + 1);
				//	Double weight = _costvalue.get(var) * 100;
				//	pw.print(temp.toString() + BLANK + weight.intValue());
				//	pw.print(NEWLINE);					
				//}else
				if(_newvar2weight.containsKey(var))
				{
					pw.print(COMMENTH_VAR);
					temp = i * varNoPerTS + (index + 1);
					Double weight = _newvar2weight.get(var);//Math.ceil(Math.log(_newvar2weight.get(var))*(-10));
					pw.print(temp.toString() + BLANK + weight.intValue());
					pw.print(NEWLINE);
//				}else if(!var.contains("noop")&&_actionmapping.containsKey(var))
//				{
//					pw.print(COMMENTH_VAR);
//					temp = i * varNoPerTS + (index + 1);
//					Double weight = 1.0d;//Math.ceil(Math.log(_newvar2weight.get(var))*(-10));
//					pw.print(temp.toString() + BLANK + weight.intValue());
//					pw.print(NEWLINE);
				}else
				{
					pw.print(COMMENTH_VAR);
					temp = i * varNoPerTS + (index + 1);
					pw.print(temp.toString() + BLANK + 0);
					pw.print(NEWLINE);
				}
			}
		}
		
		pw.println(PARAMETERH + FORMAT + BLANK + varNumber.toString() + BLANK + clauseNumber.toString());
		
		
		for(int i = 0 ; i < timestep ; i++)
		{
			for(CNFClause clause: _clauses)
			{
				if(clause.getSource() == CNFClause.ClauseSources.INIT
						&& i > 0)continue;
				clause.printCNFClause(pw, _var2Integer, i, varNoPerTS);
				pw.print(EOCLAUSE+NEWLINE);
			}
		}
		
		if(_header_domain.contains("elevators_mdp"))
		{
			for(String state: _statemapping.keySet())
			{
				if(state.startsWith("person_"))
				{
					int index = _var2Integer.get(state);
					int no = index + timestep* varNoPerTS;
					pw.println("-" + no + BLANK + EOCLAUSE);
				}
					
			}
		}
		else
		{
			//Next line just for domain: Crossing_traffic_mdp
			pw.print(varNumber.toString() + BLANK + 0);
		}
		pw.flush();
	}

	
	private String varType(String var) {
		// TODO Auto-generated method stub
		if(this._statemapping.containsKey(var))return VAR_TYPE[0];
		if(this._actionmapping.containsKey(var))return VAR_TYPE[1];
		if(this._costvalue.containsKey(var))return VAR_TYPE[2];
		if(this._outcomes2state.containsKey(var))return VAR_TYPE[3];
//		if(this._statemapping.containsKey(var))return VAR_TYPE[4];
		return VAR_TYPE[4];
	}

	private void convert2Clauses(Literal lastNode, String nextState, int index, 
			String branch_label, ADD context, int level, CNFClause clause, boolean branch) {

		ADDNode cur = context.getNode(index);

		if (cur instanceof ADDINode) {
			ADDINode i = (ADDINode) cur;
			CNFClause branchClause = clause.clone();
			if(lastNode != null)
				branchClause.addLiteral(lastNode);
			//Find one Literal if current node is a state.
			//conjunction to disjunction here, by set var to -var
			Literal truel = (Literal)curLiterals.get(context._hmID2VarName.get(i._nTestVarID))._o2;
			Literal falsel = (Literal)curLiterals.get(context._hmID2VarName.get(i._nTestVarID))._o1;

			convert2Clauses(truel,  nextState, i._nHigh, 
					 branch_label,  context,  level + 1,  branchClause,true);
			convert2Clauses(falsel,  nextState, i._nLow, 
					 branch_label,  context,  level + 1,  branchClause,false);
		} else {
			ADDDNode d = (ADDDNode) cur;

			CNFClause branchClause = clause.clone();

			if(lastNode != null)
				branchClause.addLiteral(lastNode);
			Literal l = null;
			if(branchClause.getSource() == ClauseSources.EFFECT ||
					branchClause.getSource() == ClauseSources.OBSERVATION)
			{
				if(nextState != null && nextState != NULLSTRING)
				{

					branchClause.setProbability(d._dLower);
					Pair<Literal,Literal> pair = nextLiterals.get(nextState);

					if(d._dLower > 0)
					{
						l = (Literal)pair._o1;
					}
					else
					{
						l = (Literal)pair._o2;
					}

					branchClause.addLiteral(l);
					
				}
				else
				{
					System.out.println("Error: Without nextstate in effect/observation ===>" + branchClause._literals.toString());
					return;
				}
			}
			else if(branchClause.getSource() == ClauseSources.COST)
			{
				branchClause.setWeight(d._dLower);
			}
			
			//Final check of this clause: 1.have correct effect if it come from effect 2.have correct Literal number
			if((branchClause.getSource() == ClauseSources.EFFECT ||
					branchClause.getSource() == ClauseSources.OBSERVATION)
				&& getEff(branchClause._literals) == null )//&& getEff(branchClause._literals)._currentTS)
			{
				System.out.println("Error: Wrong clause in effect :" + branchClause._literals.toString() + " ===="+nextState);
			
			}
			else if(branchClause._literals.size() <= 1)
			{
				System.out.println("Error: Wrong clause with single Literal from action --->" + branchClause._literals.toString());
			}
			else
			{
				RedundantClause(branchClause);
				addClause(branchClause);
			}

		}
	}

	
	private void RedundantClause(CNFClause clause) {
		/*
		 * 1. Check: a. clause from action
		 * 			 b. clause without condition
		 * 2. Copy: get a copy of clause
		 * 3. Find effect: get effect of clause
		 * 4. Get condition: get condition literal
		 * 5. Add condition: add condition/negation-condition into clause/copy
		 * 6. Call addClause for copy*/
		if(clause.getSource() != ClauseSources.EFFECT)
		{
			return;
		}
		Literal eff = getEff(clause._literals);
		
		if(eff != null && clause._literals.size() == 2)
		{
			if(clause._literals.remove(eff))
			{
				CNFClause copy = clause.clone();
				String conditionName = eff.getSymbol();
				copy.addLiteral(curLiterals.get(conditionName)._o2);
				clause.addLiteral(curLiterals.get(conditionName)._o1);
				copy.addLiteral(eff);
				clause.addLiteral(eff);
				addClause(copy);
			}
			else
			{
				System.out.println("Error: Failed to add redundant clause-" + clause._literals.toString());
			}
			
		}
		
		
	}


	private void frameAxiomActionAtLeastMost(Set<String> variables)
	{
		CNFClause atLeast = new CNFClause(ClauseSources.CONSTRAINT);
		for(String key: variables)
		{
			Literal posl = ((Pair<Literal, Literal>)curLiterals.get(key))._o1;
			Literal negl = ((Pair<Literal, Literal>)curLiterals.get(key))._o2;
			atLeast.addLiteral(posl);
			
			Integer v = (Integer)_actionmapping.get(key);
			for(String opponent:_actionmapping.keySet())
			{
				if((Integer)_actionmapping.get(opponent) > v)
				{
					CNFClause atMost = new CNFClause(ClauseSources.CONSTRAINT);
					atMost.addLiteral(negl);
					atMost.addLiteral( ((Pair<Literal, Literal>)curLiterals.get(opponent))._o2);
//					atMost.setProbability(CNFClause.DETERMINISTIC1);
//					if(atMost._literals.size() > 1)
					addClause(atMost);
				}
				else continue;
			}
		}
		atLeast.setProbability(CNFClause.DETERMINISTIC1);
		addClause(atLeast);	
	}
	
	
	private void mapVarInteger(String var, HashMap<String, Integer> mapping)
	{
		_var2Integer.put(var, ++varNumber);
		_integer2Var.put(varNumber, var);
		varNoPerTS++;
		mapping.put(var, varNumber);
	}
	
	
	private void add2CurLiteralList(Literal positive, Literal negative)
	{
		curLiterals.put(positive.getSymbol(), new Pair<Literal, Literal>(positive, negative));
	}

	
	private Literal findChange(Literal nextTimestep, ArrayList<Literal> varList)
	{
		Literal curTimestep = null;
		if(nextTimestep._currentTS)
		{
			System.out.println("Error: Wrong n+1 State: " + nextTimestep.toString());
			return curTimestep;
		}
		String state = nextTimestep.getSymbol();
		for(Literal l : varList)
		{
			if(l.getSymbol() == state && 
					l._currentTS && l.getSign() == nextTimestep.getSign())
			{
				curTimestep = l;
				break;
			}
		}		
		return curTimestep;
	}

	public void SetStartState(HashMap<String,Boolean> newStates)
	{
		_initStates.clear();
		for(String stateName: _statemapping.keySet())
		{
			Pair<Literal,Literal> pair = curLiterals.get(stateName);
			Literal l = null;
			if(newStates.containsKey(stateName))
			{
				if(newStates.get(stateName))
				{
					l = pair._o1;
				}else
				{
					l = pair._o2;
				}
			}
			else
			{
				l = stateDef.get(stateName)?pair._o1:pair._o2;
			}
			_initStates.put(stateName, l.getSign().contains("-")?false:true);
			CNFClause clause = null;
			for(int i = 0 ; i < _statemapping.size() ; i++)
			{
				clause = _clauses.get(i);
//				if(clause._literals.size() != 1)break;
				if(clause._literals.get(0).getSymbol().contains(stateName) &&
					stateName.contains(clause._literals.get(0).getSymbol()) )
				{
					clause._literals.remove(0);
					clause.addLiteral(l);
				}
				
				
			}
		}
	}
	public Boolean needReplan(HashMap<String,Boolean> newStates)
	{
		Boolean result = false;
		for(String state: _initStates.keySet())
		{
			if(newStates.containsKey(state))
			{
				if( Boolean.valueOf(newStates.get(state)) != Boolean.valueOf(_initStates.get(state)) )
				{
					result = true;
					break;
				}
			}else
			{
				if( Boolean.valueOf(stateDef.get(state) )!= Boolean.valueOf(_initStates.get(state)) )
				{
					result = true;
					break;
				}
			}
		}
		
		return result;
	}
//	private boolean isSameChange(Pair<Literal, Literal> one, Pair<Literal, Literal> two)
//	{
//		boolean isChange = false;
//		
//		if(one._o1 == two._o1 && one._o2 == two._o2)
//		{
//			isChange = true;
//		}	
//		
//		return isChange;
//	}
	public static void main(String[] args) throws Exception {
		
		// Parse file

		if (args.length != 2) {
			System.out.println("\nusage: RDDL-file/directory output-dir\n");
			RDDL2Format.ShowFileFormats();
			System.exit(1);
		}
		
		String rddl_file = args[0];
		String output_dir = args[1];
		if (output_dir.endsWith(File.separator))
			output_dir = output_dir.substring(output_dir.length() - 1);
		
		// If RDDL file is a directory, add all files
		ArrayList<File> rddl_files = new ArrayList<File>();
		ArrayList<File> rddl_error = new ArrayList<File>();
		File file = new File(rddl_file);
		if (file.isDirectory())
			rddl_files.addAll(Arrays.asList(file.listFiles()));
		else
			rddl_files.add(file);
		
		// Load RDDL files
		RDDL rddl = new RDDL();
		HashMap<File,RDDL> file2rddl = new HashMap<File,RDDL>();
		for (File f : (ArrayList<File>)rddl_files.clone()) {
			try {
				if (f.getName().endsWith(".rddl")) {
					RDDL r = parser.parse(f);
					file2rddl.put(f, r);
					rddl.addOtherRDDL(r);
				}
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Error processing: " + f + ", skipping...");
				rddl_files.remove(f);
				continue;
			}
		}
		
		for (File f : (ArrayList<File>)rddl_files.clone()) {
						
			try {	
				if (!file2rddl.containsKey(f))
					continue;
				for (String instance_name : file2rddl.get(f)._tmInstanceNodes.keySet()) {
					RDDL2Format r2s = new RDDL2Format(rddl, instance_name, null, output_dir);
					CNF cnf = new CNF(r2s);
					String separator = (output_dir.endsWith("\\") || output_dir.endsWith("/")) ? "" : File.separator;
					String _filename = output_dir + separator + r2s._i._sName;
					_filename = _filename + ".cnf";
					File cnfFile = new File(_filename);
					if (cnfFile.exists()) {
						System.err.println(">> File '" + _filename + "' already exists... skipping");
						_filename = null;
						return;
					}
					if (_filename == null)
						return;
					
					PrintWriter pw = new PrintWriter(new FileWriter(_filename));
					cnf.exportCNF(pw);
					pw.close();
				}
			} catch (Exception e) {
				System.err.println("Error processing: " + f);
				System.err.println(e);
				e.printStackTrace(System.err);
				System.err.flush();
				rddl_files.remove(f);
				rddl_error.add(f);
			}
		}
		
		System.out.println("\n\n===\n");
		for (File f : rddl_files)
			System.out.println("Processed: " + f);
		for (File f : rddl_error)
			System.out.println("Error processing: " + f);
	}

}
