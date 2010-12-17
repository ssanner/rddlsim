/**
 * RDDL: Main state representation and transition function 
 *       computation methods; this class requires everything
 *       to simulate a RDDL domain instance.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl;

import java.util.*;

import rddl.RDDL.*;
import util.Pair;

public class State {

	public final static boolean DISPLAY_UPDATES = false;
	
	public final static int UNDEFINED = 0;
	public final static int STATE     = 1;
	public final static int NONFLUENT = 2;
	public final static int ACTION    = 3;
	public final static int INTERM    = 4;
	public final static int OBSERV    = 5;
	
	// PVariable definitions
	public HashMap<PVAR_NAME,PVARIABLE_DEF> _hmPVariables;
	
	// Type definitions
	public HashMap<TYPE_NAME,TYPE_DEF> _hmTypes;
	
	// CPF definitions
	public HashMap<PVAR_NAME,CPF_DEF> _hmCPFs;
	
	// Object ID lookup... we use IntArrays because hashing and comparison
	// operations will be much more efficient this way than with Strings.
	public HashMap<TYPE_NAME,ArrayList<LCONST>> _hmObject2Consts;
	
	// Lists of variable names
	public ArrayList<PVAR_NAME> _alStateNames = new ArrayList<PVAR_NAME>();
	public ArrayList<PVAR_NAME> _alActionNames = new ArrayList<PVAR_NAME>();
	public TreeMap<Pair,PVAR_NAME> _tmIntermNames = new TreeMap<Pair,PVAR_NAME>();
	public ArrayList<PVAR_NAME> _alIntermNames = new ArrayList<PVAR_NAME>();
	public ArrayList<PVAR_NAME> _alObservNames = new ArrayList<PVAR_NAME>();
	public ArrayList<PVAR_NAME> _alNonFluentNames = new ArrayList<PVAR_NAME>();
	public HashMap<String,ArrayList<PVAR_NAME>> _hmTypeMap = new HashMap<String,ArrayList<PVAR_NAME>>();
	
	// String -> (IntArray -> Object)
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _state;
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _nonfluents;
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _actions;
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _interm;
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _observ;

	// Constraints
	public ArrayList<BOOL_EXPR> _alConstraints;
	public EXPR _reward;
	public int _nMaxNondefActions = -1;
	
	// Temporarily holds next state while it is being computed
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _nextState;

	public void init(HashMap<TYPE_NAME,OBJECTS_DEF> nonfluent_objects,
					 HashMap<TYPE_NAME,OBJECTS_DEF> instance_objects,
					 HashMap<TYPE_NAME,TYPE_DEF> typedefs,
					 HashMap<PVAR_NAME,PVARIABLE_DEF> pvariables,
					 HashMap<PVAR_NAME,CPF_DEF> cpfs,
					 ArrayList<PVAR_INST_DEF> init_state,
					 ArrayList<PVAR_INST_DEF> nonfluents,
					 ArrayList<BOOL_EXPR> state_action_constraints,
					 EXPR reward, 
					 int max_nondef_actions) {
		
		_hmPVariables = pvariables;
		_hmTypes = typedefs;
		_hmCPFs = cpfs;
		_alConstraints = state_action_constraints;
		_reward = reward;
		_nMaxNondefActions = max_nondef_actions;
		
		// Map object class name to list
		_hmObject2Consts = new HashMap<TYPE_NAME,ArrayList<LCONST>>();
		if (nonfluent_objects != null)
			for (OBJECTS_DEF obj_def : nonfluent_objects.values())
				_hmObject2Consts.put(obj_def._sObjectClass, obj_def._alObjects);
		if (instance_objects != null)
			for (OBJECTS_DEF obj_def : instance_objects.values())
				_hmObject2Consts.put(obj_def._sObjectClass, obj_def._alObjects);
		for (Map.Entry<TYPE_NAME,TYPE_DEF> e : typedefs.entrySet()) {
			if (e.getValue() instanceof ENUM_TYPE_DEF) {
				ENUM_TYPE_DEF etd = (ENUM_TYPE_DEF)e.getValue();
				ArrayList<LCONST> values = new ArrayList<LCONST>();
				for (ENUM_VAL v : etd._alPossibleValues)
					values.add(v);
				_hmObject2Consts.put(etd._sName, values);
			}
		}

		// Initialize assignments (missing means default)
		_state      = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_interm     = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_nextState  = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_observ     = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_actions    = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_nonfluents = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();

		// Initialize variable lists
		for (Map.Entry<PVAR_NAME,PVARIABLE_DEF> e : _hmPVariables.entrySet()) {
			PVAR_NAME pname   = e.getKey();
			PVARIABLE_DEF def = e.getValue();
			if (def instanceof PVARIABLE_STATE_DEF && !((PVARIABLE_STATE_DEF)def)._bNonFluent) {
				_alStateNames.add(pname);
				_state.put(pname, new HashMap<ArrayList<LCONST>,Object>());
				_nextState.put(pname, new HashMap<ArrayList<LCONST>,Object>());
			} else if (def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)def)._bNonFluent) {
				_alNonFluentNames.add(pname);
				_nonfluents.put(pname, new HashMap<ArrayList<LCONST>,Object>());
			} else if (def instanceof PVARIABLE_ACTION_DEF) {
				_alActionNames.add(pname);
				_actions.put(pname, new HashMap<ArrayList<LCONST>,Object>());
			} else if (def instanceof PVARIABLE_OBS_DEF) {
				_alObservNames.add(pname);
				_observ.put(pname, new HashMap<ArrayList<LCONST>,Object>());
			} else if (def instanceof PVARIABLE_INTERM_DEF) {
				_alIntermNames.add(pname);
				_tmIntermNames.put(new Pair(((PVARIABLE_INTERM_DEF)def)._nLevel, pname), pname);
				_interm.put(pname, new HashMap<ArrayList<LCONST>,Object>());
			}
		}
		_hmTypeMap.put("states", _alStateNames);
		_hmTypeMap.put("nonfluent", _alNonFluentNames);
		_hmTypeMap.put("action", _alActionNames);
		_hmTypeMap.put("observ", _alObservNames);
		_hmTypeMap.put("interm", _alIntermNames);

		// Set initial state and pvariables
		setPVariables(_state, init_state);
		if (nonfluents != null)
			setPVariables(_nonfluents, nonfluents);
	}

	public void checkStateActionConstraints(ArrayList<PVAR_INST_DEF> actions)  
		throws EvalException {
		
		// Clear then set the actions
		for (PVAR_NAME p : _actions.keySet())
			_actions.get(p).clear();
		int non_def = setPVariables(_actions, actions);

		// Check max-nondef actions
		if (non_def > _nMaxNondefActions)
			throw new EvalException("Number of non-default actions (" + non_def + 
					") exceeds limit (" + _nMaxNondefActions + ")");
		
		// Check state-action constraints
		HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
		for (BOOL_EXPR constraint : _alConstraints) {
			// satisfied must be true if get here
			try {
				if (! (Boolean)constraint.sample(subs, this, null) )
					throw new EvalException("Violated state-action constraint: " + constraint);
			} catch (NullPointerException e) {
				System.out.println("\n***SIMULATOR ERROR EVALUATING: " + constraint);
				throw e;
			} catch (ClassCastException e) {
				System.out.println("\n***SIMULATOR ERROR EVALUATING: " + constraint);
				throw e;
			}
		}
	}
		
	public void computeNextState(ArrayList<PVAR_INST_DEF> actions, Random r) 
		throws EvalException {

		// Clear then set the actions
		for (PVAR_NAME p : _actions.keySet())
			_actions.get(p).clear();
		setPVariables(_actions, actions);
		
		//System.out.println("Starting state: " + _state + "\n");
		//System.out.println("Starting nonfluents: " + _nonfluents + "\n");
		
		// First compute intermediate variables, level-by-level
		HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
		if (DISPLAY_UPDATES) System.out.println("Updating intermediate variables");
		for (Map.Entry<Pair, PVAR_NAME> e : _tmIntermNames.entrySet()) {
			int level   = (Integer)e.getKey()._o1;
			PVAR_NAME p = e.getValue();
			
			// Generate updates for each ground fluent
			//System.out.println("Updating interm var " + p + " @ level " + level + ":");
			ArrayList<ArrayList<LCONST>> gfluents = generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				if (DISPLAY_UPDATES) System.out.print("- " + p + gfluent + " @ " + level + " := ");
				CPF_DEF cpf = _hmCPFs.get(p);
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				Object value = cpf._exprEquals.sample(subs, this, r);
				if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value
				HashMap<ArrayList<LCONST>,Object> pred_assign = _interm.get(p);
				pred_assign.put(gfluent, value);
			}
		}
		
		// Do same for observations
		if (DISPLAY_UPDATES) System.out.println("Updating observations");
		for (PVAR_NAME p : _alObservNames) {
			
			// Generate updates for each ground fluent
			//System.out.println("Updating observation var " + p);
			ArrayList<ArrayList<LCONST>> gfluents = generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				if (DISPLAY_UPDATES) System.out.print("- " + p + gfluent + " := ");
				CPF_DEF cpf = _hmCPFs.get(p);
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				Object value = cpf._exprEquals.sample(subs, this, r);
				if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value
				HashMap<ArrayList<LCONST>,Object> pred_assign = _observ.get(p);
				pred_assign.put(gfluent, value);
			}
		}
		
		// Do same for next-state (keeping in mind primed variables)
		if (DISPLAY_UPDATES) System.out.println("Updating next state");
		for (PVAR_NAME p : _alStateNames) {
						
			// Get default value
			Object def_value = null;
			PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
			if (!(pvar_def instanceof PVARIABLE_STATE_DEF) ||
				((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
				throw new EvalException("Expected state variable, got nonfluent: " + p);
			def_value = ((PVARIABLE_STATE_DEF)pvar_def)._oDefValue;
			
			// Generate updates for each ground fluent
			PVAR_NAME primed = new PVAR_NAME(p._sPVarName + "'");
			//System.out.println("Updating next state var " + primed + " (" + p + ")");
			ArrayList<ArrayList<LCONST>> gfluents = generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				if (DISPLAY_UPDATES) System.out.print("- " + primed + gfluent + " := ");
				CPF_DEF cpf = _hmCPFs.get(primed);
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				Object value = cpf._exprEquals.sample(subs, this, r);
				if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value if not default
				if (!value.equals(def_value)) {
					HashMap<ArrayList<LCONST>,Object> pred_assign = _nextState.get(p);
					pred_assign.put(gfluent, value);
				}
			}
		}
	}
	
	public void advanceNextState() throws EvalException {
		HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> temp = _state;
		_state = _nextState;
		_nextState = temp;
		
		// Clear the non-state, non-constant, non-action variables
		for (PVAR_NAME p : _nextState.keySet())
			_nextState.get(p).clear();
		for (PVAR_NAME p : _interm.keySet())
			_interm.get(p).clear();
		for (PVAR_NAME p : _observ.keySet())
			_observ.get(p).clear();
	}
	
	public int setPVariables(HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> assign, 
							  ArrayList<PVAR_INST_DEF> src) {

		int non_def = 0;
		for (PVAR_INST_DEF def : src) {
			
			// Get the assignments for this PVAR
			HashMap<ArrayList<LCONST>,Object> pred_assign = assign.get(def._sPredName);
			
			// Get default value if it exists
			Object def_value = null;
			PVARIABLE_DEF pvar_def = _hmPVariables.get(def._sPredName);
			if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
				def_value = ((PVARIABLE_STATE_DEF)pvar_def)._oDefValue;
			else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
				def_value = ((PVARIABLE_ACTION_DEF)pvar_def)._oDefValue;
			
			// Set value if non-default
			if (!def_value.equals(def._oValue)) {
				pred_assign.put(def._alTerms, def._oValue);
				++non_def;
			}
		}
		
		return non_def;
	}

	/////////////////////////////////////////////////////////////////////////////
	//             Methods for Querying and Setting Fluent Values
	/////////////////////////////////////////////////////////////////////////////
	
	public Object getPVariableDefault(PVAR_NAME p) {
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
		if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
			return ((PVARIABLE_STATE_DEF) pvar_def)._oDefValue;
		else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
			return ((PVARIABLE_ACTION_DEF) pvar_def)._oDefValue;
		return null;
	}
	
	public int getPVariableType(PVAR_NAME p) {
		
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);

		if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			return NONFLUENT;
		else if (pvar_def instanceof PVARIABLE_STATE_DEF && !((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			return STATE;
		else if (pvar_def instanceof PVARIABLE_ACTION_DEF)
			return ACTION;
		else if (pvar_def instanceof PVARIABLE_INTERM_DEF)
			return INTERM;
		else if (pvar_def instanceof PVARIABLE_OBS_DEF)
			return OBSERV;
		
		return UNDEFINED;
	}
	
	public Object getPVariableAssign(PVAR_NAME p, ArrayList<LCONST> terms) {

		// Get default value if it exists
		Object def_value = null;
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
		if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
			def_value = ((PVARIABLE_STATE_DEF) pvar_def)._oDefValue;
		else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
			def_value = ((PVARIABLE_ACTION_DEF) pvar_def)._oDefValue;

		// Get correct variable assignments
		HashMap<ArrayList<LCONST>,Object> var_src = null;
		if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = _nonfluents.get(p);
		else if (pvar_def instanceof PVARIABLE_STATE_DEF && !((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = _state.get(p);
		else if (pvar_def instanceof PVARIABLE_ACTION_DEF)
			var_src = _actions.get(p);
		else if (pvar_def instanceof PVARIABLE_INTERM_DEF)
			var_src = _interm.get(p);
		else if (pvar_def instanceof PVARIABLE_OBS_DEF)
			var_src = _observ.get(p);
		
		if (var_src == null)
			return null;
		
		// Lookup value, return default (if available) if value not found
		Object ret = var_src.get(terms);
		if (ret == null)
			ret = def_value;
		return ret;
	}	
		
	public boolean setPVariableAssign(PVAR_NAME p, ArrayList<LCONST> terms, 
			Object value) {
		
		// Get default value if it exists
		Object def_value = null;
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
		if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
			def_value = ((PVARIABLE_STATE_DEF) pvar_def)._oDefValue;
		else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
			def_value = ((PVARIABLE_ACTION_DEF) pvar_def)._oDefValue;

		// Get correct variable assignments
		HashMap<ArrayList<LCONST>,Object> var_src = null;
		if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = _nonfluents.get(p);
		else if (pvar_def instanceof PVARIABLE_STATE_DEF && !((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = _state.get(p);
		else if (pvar_def instanceof PVARIABLE_ACTION_DEF)
			var_src = _actions.get(p);
		else if (pvar_def instanceof PVARIABLE_INTERM_DEF)
			var_src = _interm.get(p);
		else if (pvar_def instanceof PVARIABLE_OBS_DEF)
			var_src = _observ.get(p);
		
		if (var_src == null)
			return false;

		// Set value (or remove if default)
		if (def_value.equals(value)) {
			var_src.remove(terms);			
		} else {
			var_src.put(terms, value);
		}
		return true;
	}
			
	//////////////////////////////////////////////////////////////////////
	
	public ArrayList<ArrayList<LCONST>> generateAtoms(PVAR_NAME p) throws EvalException {
		ArrayList<ArrayList<LCONST>> list = new ArrayList<ArrayList<LCONST>>();
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
		//System.out.print("Generating pvars for " + pvar_def + ": ");
		if (pvar_def == null) {
			System.out.println("Error, could not generate atoms for unknown variable name.");
			new Exception().printStackTrace();
		}
		generateAtoms(pvar_def, 0, new ArrayList<LCONST>(), list);
		//System.out.println(list);
		return list;
	}
	
	private void generateAtoms(PVARIABLE_DEF pvar_def, int index, 
			ArrayList<LCONST> cur_assign, ArrayList<ArrayList<LCONST>> list) throws EvalException {
		if (index >= pvar_def._alParamTypes.size()) {
			// No more indices to generate
			list.add(cur_assign);
		} else {
			// Get the object list for this index
			TYPE_NAME type = pvar_def._alParamTypes.get(index);
			ArrayList<LCONST> objects = _hmObject2Consts.get(type);
			for (LCONST obj : objects) {
				ArrayList<LCONST> new_assign = (ArrayList<LCONST>)cur_assign.clone();
				new_assign.add(obj);
				generateAtoms(pvar_def, index+1, new_assign, list);
			}
		}
	}
	
	public ArrayList<ArrayList<LCONST>> generateAtoms(ArrayList<LTYPED_VAR> tvar_list) {
		ArrayList<ArrayList<LCONST>> list = new ArrayList<ArrayList<LCONST>>();
		generateAtoms(tvar_list, 0, new ArrayList<LCONST>(), list);
		return list;
	}
	
	private void generateAtoms(ArrayList<LTYPED_VAR> tvar_list, int index, 
			ArrayList<LCONST> cur_assign, ArrayList<ArrayList<LCONST>> list) {
		if (index >= tvar_list.size()) {
			// No more indices to generate
			list.add(cur_assign);
		} else {
			// Get the object list for this index
			TYPE_NAME type = tvar_list.get(index)._sType;
			ArrayList<LCONST> objects = _hmObject2Consts.get(type);
			if (objects == null) {
				System.out.println("Object type '" + type + "' did not have any objects or enumerated values defined.");
			}
			//System.out.println(type + " : " + objects);
			for (LCONST obj : objects) {
				ArrayList<LCONST> new_assign = (ArrayList<LCONST>)cur_assign.clone();
				new_assign.add(obj);
				generateAtoms(tvar_list, index+1, new_assign, list);
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		// Go through all variable types (state, interm, observ, action, nonfluent)
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : _hmTypeMap.entrySet()) {
			
			if (e.getKey().equals("nonfluent"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) 
				try {
					// Go through all term groundings for variable p
					ArrayList<ArrayList<LCONST>> gfluents = generateAtoms(p);										
					for (ArrayList<LCONST> gfluent : gfluents)
						sb.append("- " + e.getKey() + ": " + p + 
								(gfluent.size() > 0 ? gfluent : "") + " := " + 
								getPVariableAssign(p, gfluent) + "\n");
						
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment" + e.getKey() + " for " + p + "\n");
				}
		}
				
		return sb.toString();
	}
}
