/**
 * RDDL: Visualizes an RDDL instance as a DBN.
 * 
 * Requires GraphViz to be installed and in path, see README.txt.
 * 
 * TODO: Update to get dependencies from RDDL2Format.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/16/10
 *
 **/
package rddl.viz;

import graph.Graph;
import graph.gviz.DotViewer;

import java.io.*;
import java.util.*;

import rddl.*;
import rddl.RDDL.*;
import rddl.parser.*;
import util.*;

public class RDDL2Graph {

	public static final String VIEWER_FILE = "tmp_rddl_graphviz.dot";
	public static final int DEFAULT_VIEWER_TEXT_LINES = 5;
	public static final int VIEWER_TEXT_HEIGHT = 20; // pixels

	public State      _state;
	public INSTANCE   _i;
	public NONFLUENTS _n;
	public DOMAIN     _d;
	public Graph      _graph;
	
	// Display cur state, actions, interm, observations, next state occur in fixed order?
	public boolean    _bStrictLevels;   
	
	// Display cur state, actions, interm, observations, next state occur at same rank?
	// if this is false, then strict levels can have no effect
	public boolean    _bStrictGrouping;

	// Maintain mappings from names to (a) lists of dependent nodes and (b) formulae
	public HashMap<String, String> _hmName2Dependents = new HashMap<String, String>();
	public HashMap<String, String> _hmName2Formula    = new HashMap<String,String>();
	public HashMap<String, String> _hmName2Type       = new HashMap<String,String>();
	public HashMap<String, String> _hmName2ParamName  = new HashMap<String,String>();
	
	public RDDL2Graph(RDDL rddl, String instance_name, 
			boolean strict_levels, boolean strict_grouping) throws Exception {
		_state = new State();

		// Set up instance, nonfluent, and domain information
		_i = rddl._tmInstanceNodes.get(instance_name);
		if (_i == null)
			throw new Exception("Instance '" + instance_name + 
					"' not found, choices are " + rddl._tmInstanceNodes.keySet());

		_n = null;
		if (_i._sNonFluents != null)
			_n = rddl._tmNonFluentNodes.get(_i._sNonFluents);
		_d = rddl._tmDomainNodes.get(_i._sDomain);
		if (_n != null && !_i._sDomain.equals(_n._sDomain))
			throw new Exception("Domain name of instance and fluents do not match: " + 
					_i._sDomain + " vs. " + _n._sDomain);
		
		// Create the initial state which well setup all the data structures
		// in State that we'll need to produce the DBN
		_bStrictLevels = strict_levels;
		_bStrictGrouping = strict_grouping;
		_state.init(_n != null ? _n._hmObjects : null, _i._hmObjects,  
				_d._hmTypes, _d._hmPVariables, _d._hmCPF,
				_i._alInitState, _n == null ? null : _n._alNonFluents, 
				_d._alStateConstraints, _d._exprReward, _i._nNonDefActions);
		
		// Build the graph
		_graph = rddl2graph();
	}
	
	public Graph rddl2graph() throws Exception {
		
		// lightblue2, gold1, plum, ivory3, lightblue, salmon
		Graph g = new Graph(/*directed*/true, false, /*left-to-right*/true, false);

		// Can still allow common rank if not strict levels, just don't enforce
		// that each variable type goes at a separate level (with unilinks below)
		if (!_bStrictGrouping)
			g.setSuppressRank(true);
		
		// Set the actions
		//for (PVAR_NAME p : _state._actions.keySet())
		//	_state._actions.get(p).clear();
		//setPVariables(_state._actions, actions);
		
		//System.out.println("Starting state: " + _state + "\n");
		//System.out.println("Starting nonfluents: " + _nonfluents + "\n");
		
		// Store node parents temporarily
		HashSet<Pair> parents = new HashSet<Pair>();
				
		// Go through all action types
		// Go through all variable types (state, interm, observ, action, nonfluent)
		ArrayList<String> actions = new ArrayList<String>();
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : _state._hmTypeMap.entrySet()) {
			
			if (!e.getKey().equals("action"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) {
				ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
				for (ArrayList<LCONST> gfluent : gfluents) {
					String action_name = CleanFluentName(p + gfluent.toString());
					g.addNode(action_name, 1, "olivedrab1", "box", "filled");
					actions.add(action_name);
					_hmName2Dependents.put(action_name, "None");
					_hmName2Formula.put(action_name, "agent controlled");
					_hmName2Type.put(action_name, "Action");
					_hmName2ParamName.put(
						action_name, CleanFluentName("" + p + _state._hmPVariables.get(p)._alParamTypes)); 
				}
			}
		}
		// Add actions to rank list once we get current state
		
		// First compute *intermediate* variables, level-by-level
		HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
		MapList m = new MapList();
		for (Map.Entry<Pair, PVAR_NAME> e : _state._tmIntermNames.entrySet()) {
			int level   = (Integer)e.getKey()._o1;
			PVAR_NAME p = e.getValue();
			
			// Generate updates for each ground fluent
			//System.out.println("Updating interm var " + p + " @ level " + level + ":");
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				CPF_DEF cpf = _state._hmCPFs.get(p);
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				String interm_name = CleanFluentName(p + gfluent.toString());
				g.addNode(interm_name, 1, "sandybrown", "ellipse", "filled");
				m.putValue("Intermediate @ Level " + level, interm_name);
				
				parents.clear();
				cpf._exprEquals.collectGFluents(subs, _state, parents);
				for (Pair par : parents)
					g.addUniLink(CleanFluentName(par._o1.toString() + par._o2.toString()), interm_name);
				
				_hmName2Dependents.put(interm_name, CleanFluentName(parents.toString()));
				_hmName2Formula.put(interm_name, cpf._exprEquals.toString());
				_hmName2Type.put(interm_name, "Intermediate @ Level " + level);
				_hmName2ParamName.put(
						interm_name, CleanFluentName("" + p + cpf._exprVarName._alTerms)); 
				
				//Object value = cpf._exprEquals.sample(subs, this, r);
				//if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value
				//HashMap<ArrayList<LCONST>,Object> pred_assign = _state._interm.get(p);
				//pred_assign.put(gfluent, value);
			}
		}
		for (Object o : m.keySet()) {
			ArrayList l = m.getValues(o);
			// Add "Intermediate @ level"
			l.add(0, o.toString());
			// Add all node names at this level
			g.addSameRank(l);
			System.out.println("At same rank: " + l);
		}
		
		// Do same for *observations*
		ArrayList<String> observations = new ArrayList<String>();
		for (PVAR_NAME p : _state._alObservNames) {
			
			// Generate updates for each ground fluent
			//System.out.println("Updating observation var " + p);
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				CPF_DEF cpf = _state._hmCPFs.get(p);
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				String observ_name = CleanFluentName(p + gfluent.toString());
				g.addNode(observ_name, 1, "orangered", "ellipse", "filled");
				observations.add(observ_name);
				
				parents.clear();
				cpf._exprEquals.collectGFluents(subs, _state, parents);
				for (Pair par : parents)
					g.addUniLink(CleanFluentName(par._o1.toString() + par._o2.toString()), observ_name, "black", "dashed", null);

				_hmName2Dependents.put(observ_name, CleanFluentName(parents.toString()));
				_hmName2Formula.put(observ_name, cpf._exprEquals.toString());
				_hmName2Type.put(observ_name, "Observation");
				_hmName2ParamName.put(
						observ_name, CleanFluentName("" + p + cpf._exprVarName._alTerms)); 
				
				//Object value = cpf._exprEquals.sample(subs, this, r);
				//if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value
				//HashMap<ArrayList<LCONST>,Object> pred_assign = _state._observ.get(p);
				//pred_assign.put(gfluent, value);
			}
		}
		observations.add(0, "Observations");
		if (observations.size() > 1) // will always contain default "Observations" node
			g.addSameRank(observations);
		System.out.println("At same rank: " + observations);
		
		// Do same for next-state (keeping in mind primed variables)
		ArrayList cur_state = new ArrayList();
		ArrayList next_state = new ArrayList();
		for (PVAR_NAME p : _state._alStateNames) {
						
			// Get default value
			Object def_value = null;
			PVARIABLE_DEF pvar_def = _state._hmPVariables.get(p);
			if (!(pvar_def instanceof PVARIABLE_STATE_DEF) ||
				((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
				throw new EvalException("Expected state variable, got nonfluent: " + p);
			def_value = ((PVARIABLE_STATE_DEF)pvar_def)._oDefValue;
			
			// Generate updates for each ground fluent
			PVAR_NAME primed = new PVAR_NAME(p._sPVarName + "'");
			//System.out.println("Updating next state var " + primed + " (" + p + ")");
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				CPF_DEF cpf = _state._hmCPFs.get(primed);
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				String unprimed_name = CleanFluentName(p + gfluent.toString());
				String primed_name = CleanFluentName(primed + gfluent.toString());
				g.addNode(unprimed_name, 1, "lightblue", "ellipse", "filled");
				g.addNode(primed_name, 1, "gold1", "ellipse", "filled");
				cur_state.add(unprimed_name);
				next_state.add(primed_name);
				//g.addUniLink(unprimed_name, primed_name);
				
				parents.clear();
				cpf._exprEquals.collectGFluents(subs, _state, parents);
				for (Pair par : parents)
					g.addUniLink(CleanFluentName(par._o1.toString() + par._o2.toString()), primed_name);

				_hmName2Dependents.put(unprimed_name, "None");
				_hmName2Formula.put(unprimed_name, "fully observed");
				_hmName2Type.put(unprimed_name, "Current State");
				_hmName2ParamName.put(
						unprimed_name, CleanFluentName("" + p + _state._hmPVariables.get(p)._alParamTypes)); 

				_hmName2Dependents.put(primed_name, CleanFluentName(parents.toString()));
				_hmName2Formula.put(primed_name, cpf._exprEquals.toString());
				_hmName2Type.put(primed_name, "Next State");
				_hmName2ParamName.put(
						primed_name, CleanFluentName("" + p + "'" + cpf._exprVarName._alTerms
						/*_state._hmPVariables.get(p)._alParamTypes*/)); 

				//Object value = cpf._exprEquals.sample(subs, this, r);
				//if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value if not default
				//if (!value.equals(def_value)) {
				//	HashMap<ArrayList<LCONST>,Object> pred_assign = _state._nextState.get(p);
				//	pred_assign.put(gfluent, value);
				//}
			}
		}
		
		// Add all current state and action nodes at same rank
		actions.addAll(cur_state);
		actions.add(0, "Current State and Actions");
		//cur_state.add(0, "Current State");
		//g.addSameRank(cur_state);
		//System.out.println("At same rank: " + cur_state);
		g.addSameRank(actions);
		System.out.println("At same rank: " + actions);
		
		// Create reward node
		g.addNode("Reward Function", 1, "firebrick1", "diamond", "filled");
		parents.clear();
		subs.clear();
		_d._exprReward.collectGFluents(subs, _state, parents);
		for (Pair par : parents)
			g.addUniLink(CleanFluentName(par._o1.toString() + par._o2.toString()), "Reward Function");
		_hmName2Dependents.put("Reward Function", CleanFluentName(parents.toString()));
		_hmName2Formula.put("Reward Function", _d._exprReward.toString());
		_hmName2Type.put("Reward Function", "Reward");
		_hmName2ParamName.put("Reward Function", "Reward");
		
		// Add Next State and Reward nodes at same rank
		next_state.add(0, "Next State and Reward");
		next_state.add(0, "Reward Function");
		g.addSameRank(next_state);
		System.out.println("At same rank: " + next_state);
		
		// Setup some stratification nodes and links if strict levels
		if (_bStrictGrouping) {
			TreeSet<Integer> levels = new TreeSet<Integer>();
			for (Map.Entry<Pair, PVAR_NAME> e : _state._tmIntermNames.entrySet()) {
				int level = (Integer)e.getKey()._o1;
				levels.add(level);
			}
			
			//g.addNode("Current State", 1, "white", "plaintext", "bold");
			//if (_bStrictLevels) 
			//	g.addUniLink("Current State", "Current State and Actions", "black", "invis", "");
			g.addNode("Current State and Actions", 1, "white", "plaintext", "bold");
			String prev_level = "Current State and Actions";
			for (Integer level : levels) {
				if (_bStrictLevels) 
					g.addUniLink(prev_level, "Intermediate @ Level " + level, "black", "invis", "");
				g.addNode("Intermediate @ Level " + level, 1, "white", "plaintext", "bold");
				prev_level = "Intermediate @ Level " + level;
			}
			if (observations.size() > 1) { // will always contain default "Observations" node
				g.addNode("Observations", 1, "white", "plaintext", "bold");		
				if (_bStrictLevels) 
					g.addUniLink(prev_level, "Observations", "black", "invis", "");
				prev_level = "Observations";
			}
			g.addNode("Next State and Reward", 1, "white", "plaintext", "bold");
			if (_bStrictLevels) 
				g.addUniLink(prev_level, "Next State and Reward", "black", "invis", "");
			//g.addNode("Reward Function", 1, "white", "plaintext", "bold");
			//if (_bStrictLevels) 
			//	g.addUniLink("Next State and Reward", "Reward Function", "black", "invis", "");
		}

		return g;
	}
	
	public static String CleanFluentName(String s) {
		s = s.replace('[', '(');
		s = s.replace(']', ')');
		s = s.replace("()","");
		return s;
	}
	
	public void launchViewer(int w, int h) throws Exception {
		//_graph.genDotFile(System.out);
		_graph.genFormatDotFile(VIEWER_FILE);
		DotViewer dv = new DotViewer() {
			public void nodeClicked(String name) {
				System.out.println("Lookup: '" + name + "'");
				String dependents = _hmName2Dependents.get(name);
				String formula    = _hmName2Formula.get(name);
				String type       = _hmName2Type.get(name);
				String param_name = _hmName2ParamName.get(name);
				
				displayText("*" + type + " PVariable: " + name + "\n\n" 
						+ "*CPF: " + param_name + " = " + formula + "\n\n" 
						+ "*Dependent Variables: " + dependents);
			}
		};
		dv.setWindowSizing(w, h, 0, 0, DEFAULT_VIEWER_TEXT_LINES * VIEWER_TEXT_HEIGHT);
		dv.showWindow(VIEWER_FILE);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		// Parse file
		//RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin_test.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/game_of_life.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/game_of_life_stoch.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/sidewalk.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/dbn_prop.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/dbn_types_interm_po.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/sysadmin.rddl"));
		//RDDL rddl = parser.parse(new File("files/rddl/test/traffic_binary_ctm.rddl"));

		// Get first instance name in file and create a simulator
		//String instance_name = rddl._tmInstanceNodes.firstKey();
	
		if (args.length != 2) {
			System.out.println("usage: RDDL-file instance-name");
			System.exit(1);
		}
		String rddl_file = args[0];
		String instance_name = args[1];
		RDDL rddl = parser.parse(new File(rddl_file));
		
		RDDL2Graph r2g = new RDDL2Graph(rddl, instance_name, 
//				/*strict levels*/false, /*strict grouping*/false);
//				/*strict levels*/true, /*strict grouping*/true);
				/*strict levels*/false, /*strict grouping*/true);
		
		// Reset, pass a policy, a visualization interface, a random seed, and simulate!
		r2g.launchViewer(1024, 768);
		
		// Can also use graph to get a low-treewidth ordering for this RDDL domain
		System.out.println();
		List order = r2g._graph.computeBestOrder(); // _graph.greedyTWSort(true);
		System.out.println("\nBest Order:   " + order);
		System.out.println("MAX Bin Size: " + r2g._graph._df.format(r2g._graph._dMaxBinaryWidth));
		System.out.println("Tree Width:   " + r2g._graph.computeTreeWidth(order));
		//r2g._graph.genDotFile(VIEWER_FILE);
	}

}
