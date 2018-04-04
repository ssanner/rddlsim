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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.RDDL.BOOL_EXPR;
import rddl.RDDL.CPF_DEF;
import rddl.RDDL.ENUM_TYPE_DEF;
import rddl.RDDL.ENUM_VAL;
import rddl.RDDL.EXPR;
import rddl.RDDL.LCONST;
import rddl.RDDL.LCONST_TYPE_DEF;
import rddl.RDDL.LTYPED_VAR;
import rddl.RDDL.LVAR;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.OBJECT_TYPE_DEF;
import rddl.RDDL.PVARIABLE_ACTION_DEF;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVARIABLE_INTERM_DEF;
import rddl.RDDL.PVARIABLE_OBS_DEF;
import rddl.RDDL.PVARIABLE_STATE_DEF;
import rddl.RDDL.PVARIABLE_WITH_DEFAULT_DEF;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.STRUCT_TYPE_DEF;
import rddl.RDDL.STRUCT_VAL;
import rddl.RDDL.TYPE_DEF;
import rddl.RDDL.TYPE_NAME;
import rddl.viz.RDDL2Graph;
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

	// Orderings for evaluating derived and intermediate fluents
	public ArrayList<Pair> _alIntermGfluentOrdering  = new ArrayList<Pair>();
	public ArrayList<Pair> _alDerivedGfluentOrdering = new ArrayList<Pair>();

	// Constraints
	//public ArrayList<BOOL_EXPR> _alConstraints;
	public ArrayList<BOOL_EXPR> _alActionPreconditions;
	public ArrayList<BOOL_EXPR> _alStateInvariants;
	public EXPR _reward;
	public int _nMaxNondefActions = -1;
	
	// Underlying graphical model
	public RDDL2Graph _r2g = null;
	
	// Temporarily holds next state while it is being computed
	public HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> _nextState;

	public void init(HashMap<TYPE_NAME,OBJECTS_DEF> domain_objects,
					 HashMap<TYPE_NAME,OBJECTS_DEF> nonfluent_objects,
					 HashMap<TYPE_NAME,OBJECTS_DEF> instance_objects,
					 HashMap<TYPE_NAME,TYPE_DEF> typedefs,
					 HashMap<PVAR_NAME,PVARIABLE_DEF> pvariables,
					 HashMap<PVAR_NAME,CPF_DEF> cpfs,
					 ArrayList<PVAR_INST_DEF> init_state,
					 ArrayList<PVAR_INST_DEF> nf_nonfluents,
					 ArrayList<PVAR_INST_DEF> i_nonfluents,
					 ArrayList<BOOL_EXPR> state_action_constraints, // deprecated but still usable
					 ArrayList<BOOL_EXPR> action_preconditions,
					 ArrayList<BOOL_EXPR> state_invariants,
					 EXPR reward, 
					 int max_nondef_actions) {
		
		_hmPVariables = pvariables;
		_hmTypes = typedefs;
		_hmCPFs = cpfs;
		
		//_alConstraints = state_action_constraints; 
		_alActionPreconditions = new ArrayList<BOOL_EXPR>(); 
		_alActionPreconditions.addAll(action_preconditions);
		
		// Deprecated but we still have to support -- put them in action preconditions
		// since action preconditions are checked at the same point where state-action
		// constraints were previously checked
		_alActionPreconditions.addAll(state_action_constraints); 

		// State invariants are new in RDDL2 -- cannot mention actions or next-state variables
		// (checked in every state upon initially reaching that state)
		_alStateInvariants = new ArrayList<BOOL_EXPR>(); 
		_alStateInvariants.addAll(state_invariants);
		
		_reward = reward;
		_nMaxNondefActions = max_nondef_actions;
		
		// =============================
		
		// Map object/enum class name to list (NOTE: all enum and object value lists initialized here)
		// (Now that we allow superclasses we first have to preprocess all object definitions and ensure
		//  that we instantiate parents before children and then recursively instantiate children)
		
		_hmObject2Consts = new HashMap<TYPE_NAME,ArrayList<LCONST>>();
		if (domain_objects != null) {
			for (OBJECTS_DEF obj_def : domain_objects.values()) {
				addConstants(obj_def._sObjectClass, obj_def._alObjects);
			}
		}
		if (nonfluent_objects != null) {
			for (OBJECTS_DEF obj_def : nonfluent_objects.values()) {
				addConstants(obj_def._sObjectClass, obj_def._alObjects);
			}
		}
		if (instance_objects != null) {
			for (OBJECTS_DEF obj_def : instance_objects.values()) {
				addConstants(obj_def._sObjectClass, obj_def._alObjects);
			}
		}
		for (Map.Entry<TYPE_NAME,TYPE_DEF> e : typedefs.entrySet()) {
			if (e.getValue() instanceof ENUM_TYPE_DEF) {
				ENUM_TYPE_DEF etd = (ENUM_TYPE_DEF)e.getValue();
				ArrayList<LCONST> values = new ArrayList<LCONST>();
				for (LCONST v : etd._alPossibleValues)
					values.add(v);
				addConstants(etd._sName, values);
			}
		}

		HashMap<TYPE_NAME,ArrayList<LCONST>> inheritedObjects = new HashMap<TYPE_NAME,ArrayList<LCONST>>();
		
		// Now add in constants to superclasses as well
		for (TYPE_NAME tname : _hmObject2Consts.keySet()) {
				
			// Add superclass constants for each tname
			TYPE_NAME cur_tname = tname;
			ArrayList<LCONST> child_constants = new ArrayList<LCONST>();
			while (true) {
				TYPE_DEF def = typedefs.get(cur_tname);
				// Terminate loop if enum or no superclass
				if (!(def instanceof OBJECT_TYPE_DEF) || ((OBJECT_TYPE_DEF)def)._typeSuperclass == null) {
					break;
				}
				ArrayList<LCONST> new_child_constants = _hmObject2Consts.get(cur_tname);
				if (new_child_constants != null) {
					child_constants.addAll(new_child_constants);
				}

				// We have a superclass, so add it's constants
				cur_tname = ((OBJECT_TYPE_DEF)def)._typeSuperclass; // Update for future iterations

				if (inheritedObjects.get(cur_tname) != null) {
					ArrayList<LCONST> merged_objects = inheritedObjects.get(cur_tname);
					merged_objects.addAll(child_constants);
					inheritedObjects.put(cur_tname, merged_objects);
				} else {                
					inheritedObjects.put(cur_tname, child_constants);
				}
			}
		}

		for (HashMap.Entry<TYPE_NAME,ArrayList<LCONST>> entry : inheritedObjects.entrySet()) {
			addConstants(entry.getKey(), entry.getValue());
		}

		// =============================
		
		// TODO: Expand enum and object types according to the constants
		for (Map.Entry<TYPE_NAME,TYPE_DEF> e : typedefs.entrySet()) {
			if (e.getValue() instanceof STRUCT_TYPE_DEF && ((STRUCT_TYPE_DEF)e.getValue())._typeGeneric != null) {
				STRUCT_TYPE_DEF ldef = (STRUCT_TYPE_DEF)e.getValue();
				ArrayList<LCONST> constants = _hmObject2Consts.get(ldef._sLabelEnumOrObjectType);
				if (constants == null) {
					System.err.println("Could not instantiate object tuple\n" + ldef + 
							"\nwith constants from '" + ldef._sLabelEnumOrObjectType+ "'");
					System.exit(1);
				}
				ldef.initIndefiniteTypes(constants);
			}
		}

		// Initialize assignments (missing means default)
		_state      = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_interm     = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_nextState  = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_observ     = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_actions    = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		_nonfluents = new HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>>();
		
		// Initialize variable lists and vector defaults (if needed)
		_alStateNames.clear();
		_alNonFluentNames.clear();
		_alActionNames.clear();
		_alObservNames.clear();
		_alIntermNames.clear();
		boolean undefined_levels = false;
		for (Map.Entry<PVAR_NAME,PVARIABLE_DEF> e : _hmPVariables.entrySet()) {
			PVAR_NAME pname   = e.getKey();
			PVARIABLE_DEF def = e.getValue();
						
			// Expand the default value definition if it is a vector containing <? : type>
			if (def instanceof PVARIABLE_WITH_DEFAULT_DEF) {
				PVARIABLE_WITH_DEFAULT_DEF ddef = (PVARIABLE_WITH_DEFAULT_DEF)def;
						
				// If the default value is a vector type, we should instantiate it
				// (in case it contains a <? : val> expansion type)
				if (ddef._oDefValue instanceof STRUCT_VAL) {
					String msg_def_value = def + " with " + ddef._oDefValue.toString(); // Save in case of error since we overwrite
					try {
						((STRUCT_VAL)ddef._oDefValue).instantiate(ddef._typeRange, typedefs, _hmObject2Consts);
					} catch (Exception e2) {
						System.err.println("ERROR: Could not instantiate object tuple: " + msg_def_value +
								"\n... check definition and that all subtypes and object/enum lists are defined.\n" + e2);
						System.exit(1);
					}
				}
			}
			
			// Book-keeping for all PVARIABLEs
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
				int level = ((PVARIABLE_INTERM_DEF)def)._nLevel;
				if (level < 0)
					undefined_levels = true; 
				_alIntermNames.add(pname);
				_tmIntermNames.put(new Pair(level, pname), pname);
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
		setPVariables(_nonfluents, nf_nonfluents);
		setPVariables(_nonfluents, i_nonfluents);
			
		// Derive fluent ordering
		try {
			_r2g = new RDDL2Graph(this);
			deriveDAGOrdering();
			//System.out.println("Derived: " + _alDerivedGfluentOrdering);
			//System.out.println("Interm:  " + _alIntermGfluentOrdering);
		} catch (Exception e) {
			System.out.println("Could not derive legal fluent ordering:\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
		
		// Compute derived fluents from state
		try {
			computeDerivedFluents();
		} catch (EvalException e) {
			System.out.println("Could not evaluate/initialize derived fluents in initial state:\n" + e);
			System.out.println("**Ensure that derived fluents only depend on other derived fluents and state fluents (not intermediate or observation fluents)");
			System.exit(1);
		}
	}
	
	private void deriveDAGOrdering() throws Exception {

		// First we need to detect cycles and exit if we found any		
		if (_r2g._graph.hasCycle()) {
		
			// General loops
			StringBuilder msg = new StringBuilder();
			msg.append("\nERROR: the DBN dependency graph contains one or more cycles as follows:");
			HashSet<HashSet<Object>> sccs = _r2g._graph.getStronglyConnectedComponents();
			for (HashSet<Object> connected_component : sccs)
				if (connected_component.size() > 1)
					System.err.println("- Cycle: " + connected_component);
			
			// Self-cycles 
			HashSet<Object> self_cycles = _r2g._graph.getSelfCycles();
			for (Object v : self_cycles)
				msg.append("- Self-cycle: [" + v + "]");
			
			throw new Exception(msg.toString());
		}
		
		// No cycles, extract an ordering
		List ordering = _r2g._graph.topologicalSort(false);
		for (Object fluent_name : ordering) {		
			Pair gfluent = _r2g._hmName2IntermGfluent.get((String)fluent_name);

			// We only want interms and derived predicates and only these are in the HashMap
			if (gfluent != null) { 

				PVARIABLE_INTERM_DEF def = (PVARIABLE_INTERM_DEF)_hmPVariables.get((PVAR_NAME)gfluent._o1);
				
				// Separate lists, eval derived then interm, add parents before children since we have to evaluate top-down
				if (def._bDerived)
					_alDerivedGfluentOrdering.add(gfluent); 
				else						
					_alIntermGfluentOrdering.add(gfluent); 
			}
		}
	}

	public void addConstants(TYPE_NAME object_class, ArrayList<LCONST> constants) {
		
		// First check that object_class is defined 
		if (!(_hmTypes.get(object_class) instanceof RDDL.OBJECT_TYPE_DEF) &&
			!(_hmTypes.get(object_class) instanceof RDDL.ENUM_TYPE_DEF)) {
			System.err.println("FATAL ERROR: '" + 
					object_class + "' is not a defined object/enum type; " + 
					"cannot initialize with " + constants + ".");
			System.exit(1);
		}
		
		// Merge constants without duplication
		ArrayList<LCONST> new_constants = new ArrayList<LCONST>(constants);
		ArrayList<LCONST> cur_constants = _hmObject2Consts.get(object_class);
		if (cur_constants != null) {
			for (LCONST c : cur_constants) {
				if (!new_constants.contains(c)) {
					new_constants.add(c);
				}
			}
		}
		_hmObject2Consts.put(object_class, new_constants);
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
		for (BOOL_EXPR constraint : _alActionPreconditions) {
			// satisfied must be true if get here
			try {
				if (! (Boolean)constraint.sample(subs, this, null) )
					throw new EvalException("Violated state invariant or action precondition constraint: " + constraint + "\n**in state**\n" + this);
			} catch (NullPointerException e) {
				System.out.println("\n***SIMULATOR ERROR EVALUATING: " + constraint);
				throw e;
			} catch (ClassCastException e) {
				System.out.println("\n***SIMULATOR ERROR EVALUATING: " + constraint);
				throw e;
			}
		}
	}

	public void checkStateInvariants()  
			throws EvalException {
			
		// Check state invariants 
		// (should not mention actions or next state variables -- 
		//  nothing to substitute since current state known)
		HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
		for (BOOL_EXPR constraint : _alStateInvariants) {
			// satisfied must be true if get here
			try {
				if (! (Boolean)constraint.sample(subs, this, null) )
					throw new EvalException("\nViolated state invariant constraint: " + constraint + 
							"\nNOTE: state invariants should never be violated by a correctly defined transition model starting from a legal initial state.\n" + 
							"**in state**\n" + this);
			} catch (NullPointerException e) {
				System.out.println("\n***SIMULATOR ERROR EVALUATING: " + constraint);
				throw e;
			} catch (ClassCastException e) {
				System.out.println("\n***SIMULATOR ERROR EVALUATING: " + constraint);
				throw e;
			}
		}
	}

	public boolean checkTerminationCondition(BOOL_EXPR cond) throws EvalException {
		try {
			HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
			return (Boolean)cond.sample(subs, this, null);
		} catch (EvalException e) {
			System.out.println("\n***SIMULATOR ERROR EVALUATING TERMINATION CONDITION: " + cond);
			throw e;
		}
	}
	
	public void computeNextState(ArrayList<PVAR_INST_DEF> actions, RandomDataGenerator _rand) 
		throws EvalException {

		// Clear then set the actions
		for (PVAR_NAME p : _actions.keySet())
			_actions.get(p).clear();
		setPVariables(_actions, actions);
		
		//System.out.println("Starting state: " + _state + "\n");
		//System.out.println("Starting nonfluents: " + _nonfluents + "\n");
		
		// First compute intermediate variables (derived should have already been computed)
		HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
		if (DISPLAY_UPDATES) System.out.println("Updating intermediate variables");
		for (Pair ifluent : _alIntermGfluentOrdering) {
			
			PVAR_NAME p = (PVAR_NAME)ifluent._o1;
			ArrayList<LCONST> gfluent = (ArrayList<LCONST>)ifluent._o2;
			
			if (DISPLAY_UPDATES) System.out.print("- " + p + gfluent);
			CPF_DEF cpf = _hmCPFs.get(p);
			if (cpf == null) 
				throw new EvalException("Could not find cpf for: " + p + gfluent);
			
			subs.clear();
			for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
				LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
				LCONST c = (LCONST)gfluent.get(i);
				subs.put(v,c);
			}
			
			Object value = cpf._exprEquals.sample(subs, this, _rand);
			if (DISPLAY_UPDATES) System.out.println(value);
			
			// Update value
			HashMap<ArrayList<LCONST>,Object> pred_assign = _interm.get(p);
			pred_assign.put(gfluent, value);
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
				if (cpf == null) 
					throw new EvalException("Could not find cpf for: " + primed + 
							"... did you forget to prime (') the variable in the cpf definition?");
				
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				Object value = cpf._exprEquals.sample(subs, this, _rand);
				if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value if not default
				if (!value.equals(def_value)) {
					HashMap<ArrayList<LCONST>,Object> pred_assign = _nextState.get(p);
					pred_assign.put(gfluent, value);
				}
			}
		}
		
		// Make sure observations are cleared prior to computing new ones
		for (PVAR_NAME p : _observ.keySet())
			_observ.get(p).clear();

		// Do same for observations... note that this occurs after the next state
		// update because observations in a POMDP may be modeled on the current
		// and next state, i.e., P(o'|s,a,s').
		if (DISPLAY_UPDATES) System.out.println("Updating observations");
		for (PVAR_NAME p : _alObservNames) {
			
			// Generate updates for each ground fluent
			//System.out.println("Updating observation var " + p);
			ArrayList<ArrayList<LCONST>> gfluents = generateAtoms(p);
			
			for (ArrayList<LCONST> gfluent : gfluents) {
				if (DISPLAY_UPDATES) System.out.print("- " + p + gfluent + " := ");
				CPF_DEF cpf = _hmCPFs.get(p);
				if (cpf == null) 
					throw new EvalException("Could not find cpf for: " + p);
	
				subs.clear();
				for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
					LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
					LCONST c = (LCONST)gfluent.get(i);
					subs.put(v,c);
				}
				
				Object value = cpf._exprEquals.sample(subs, this, _rand);
				if (DISPLAY_UPDATES) System.out.println(value);
				
				// Update value
				HashMap<ArrayList<LCONST>,Object> pred_assign = _observ.get(p);
				pred_assign.put(gfluent, value);
			}
		}
	}
	
	public void computeDerivedFluents() throws EvalException {
		
		// Compute derived variables in order
		HashMap<LVAR,LCONST> subs = new HashMap<LVAR,LCONST>();
		if (DISPLAY_UPDATES) System.out.println("Updating intermediate variables");
		for (Pair ifluent : _alDerivedGfluentOrdering) {
			
			PVAR_NAME p = (PVAR_NAME)ifluent._o1;
			ArrayList<LCONST> gfluent = (ArrayList<LCONST>)ifluent._o2;
			
			if (DISPLAY_UPDATES) System.out.print("- " + p + gfluent);
			CPF_DEF cpf = _hmCPFs.get(p);
			if (cpf == null) 
				throw new EvalException("Could not find cpf for: " + p + gfluent);
			
			subs.clear();
			for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
				LVAR v = (LVAR)cpf._exprVarName._alTerms.get(i);
				LCONST c = (LCONST)gfluent.get(i);
				subs.put(v,c);
			}
			
			if (!cpf._exprEquals._bDet)
				throw new EvalException("Derived fluent " + p + gfluent + " cannot have stochastic definition: " + cpf._exprEquals);

			// No randomness for derived fluents (can pass null)
			Object value = cpf._exprEquals.sample(subs, this, null);
			if (DISPLAY_UPDATES) System.out.println(value);
			
			// Update value
			HashMap<ArrayList<LCONST>,Object> pred_assign = _interm.get(p);
			pred_assign.put(gfluent, value);
		}		
	}
	
	public void advanceNextState() throws EvalException {
		// For backward compatibility with code that has previously called this
		// method with 0 parameters, we'll assume observations are cleared by default
		advanceNextState(true /* clear observations */);
	}
	
	public void advanceNextState(boolean clear_observations) throws EvalException {
		HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> temp = _state;
		_state = _nextState;
		_nextState = temp;
		
		// Clear the non-state, non-constant, non-action variables
		for (PVAR_NAME p : _nextState.keySet())
			_nextState.get(p).clear();
		for (PVAR_NAME p : _interm.keySet())
			_interm.get(p).clear();
		if (clear_observations)  
			for (PVAR_NAME p : _observ.keySet())
				_observ.get(p).clear();
		
		// Compute derived fluents from new state
		computeDerivedFluents();
	}
	
	public void clearPVariables(HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> assign) {
		for (HashMap<ArrayList<LCONST>,Object> pred_assign : assign.values())
			pred_assign.clear();
	}
	
	public int setPVariables(HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> assign, 
							  ArrayList<PVAR_INST_DEF> src) {

		int non_def = 0;
		boolean fatal_error = false;
		for (PVAR_INST_DEF def : src) {
			
			// Get the assignments for this PVAR
			HashMap<ArrayList<LCONST>,Object> pred_assign = assign.get(def._sPredName);
			if (pred_assign == null) {
				System.out.println("FATAL ERROR: '" + def._sPredName + "' not defined");
				fatal_error = true;
			}
			
			// Get default value if it exists
			Object def_value = null;
			PVARIABLE_DEF pvar_def = _hmPVariables.get(def._sPredName);
			if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
				def_value = ((PVARIABLE_STATE_DEF)pvar_def)._oDefValue;
			else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
				def_value = ((PVARIABLE_ACTION_DEF)pvar_def)._oDefValue;
			
			// Set value if non-default
			if (def_value != null && !def_value.equals(def._oValue)) {
				pred_assign.put(def._alTerms, def._oValue);
				++non_def;
			} else if ( pvar_def instanceof PVARIABLE_OBS_DEF ) {
				pred_assign.put(def._alTerms, def._oValue);
			}
		}
		
		if (fatal_error) {
			System.out.println("ABORTING DUE TO FATAL ERRORS");
			System.exit(1);
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
	
	public Object getDefaultValue(PVAR_NAME p) {
		
		Object def_value = null;
		PVARIABLE_DEF pvar_def = _hmPVariables.get(new PVAR_NAME(p._sPVarName));
		if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
			def_value = ((PVARIABLE_STATE_DEF) pvar_def)._oDefValue;
		else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
			def_value = ((PVARIABLE_ACTION_DEF) pvar_def)._oDefValue;	
		
		return def_value;
	}
	
	public Object getPVariableAssign(PVAR_NAME p, ArrayList<LCONST> terms) throws EvalException {

		// Get default value if it exists
		Object def_value = null;
		boolean primed = p._bPrimed;
		p = p._pvarUnprimed; // We'll look up the unprimed version, but check for priming later
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
		
		if (pvar_def == null)
			throw new EvalException("ERROR: undefined pvariable: " + p);
		else if (pvar_def._alParamTypes.size() != terms.size()) 
			throw new EvalException("ERROR: expected " + pvar_def._alParamTypes.size() + 
					" parameters for " + p + ", but got " + terms.size() + ": " + terms);
		
		// Initialize with defaults in case not assigned
		if (pvar_def instanceof PVARIABLE_STATE_DEF) { // state & non_fluents
			def_value = ((PVARIABLE_STATE_DEF) pvar_def)._oDefValue;
			if (def_value == null)
				throw new EvalException("ERROR: Default value should not be null for state fluent " + pvar_def);
		} else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) { // actions
			def_value = ((PVARIABLE_ACTION_DEF) pvar_def)._oDefValue;
			if (def_value == null)
				throw new EvalException("ERROR: Default value should not be null for action fluent " + pvar_def);
		}
		//System.out.println("Default value: " + def_value);

		// Get correct variable assignments
		HashMap<ArrayList<LCONST>,Object> var_src = null;
		if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = _nonfluents.get(p);
		else if (pvar_def instanceof PVARIABLE_STATE_DEF && !((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = /*CHECK PRIMED*/ primed ? _nextState.get(p) : _state.get(p); // Note: (next) state index by non-primed pvar
		else if (pvar_def instanceof PVARIABLE_ACTION_DEF)
			var_src = _actions.get(p);
		else if (pvar_def instanceof PVARIABLE_INTERM_DEF)
			var_src = _interm.get(p);
		else if (pvar_def instanceof PVARIABLE_OBS_DEF)
			var_src = _observ.get(p);
			
		if (var_src == null)
			throw new EvalException("ERROR: no variable source for " + p);
		
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
		boolean primed = p._bPrimed;
		p = new PVAR_NAME(p._sPVarName);
		PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
		
		if (pvar_def == null) {
			System.out.println("ERROR: undefined pvariable: " + p);
			return false;
		} else if (pvar_def._alParamTypes.size() != terms.size()) {
			System.out.println("ERROR: expected " + pvar_def._alParamTypes.size() + 
					" parameters for " + p + ", but got " + terms.size() + ": " + terms);
			return false;
		}
		
		if (pvar_def instanceof PVARIABLE_STATE_DEF) // state & non_fluents
			def_value = ((PVARIABLE_STATE_DEF) pvar_def)._oDefValue;
		else if (pvar_def instanceof RDDL.PVARIABLE_ACTION_DEF) // actions
			def_value = ((PVARIABLE_ACTION_DEF) pvar_def)._oDefValue;

		// Get correct variable assignments
		HashMap<ArrayList<LCONST>,Object> var_src = null;
		if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = _nonfluents.get(p);
		else if (pvar_def instanceof PVARIABLE_STATE_DEF && !((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
			var_src = primed ? _nextState.get(p) : _state.get(p); // Note: (next) state index by non-primed pvar
		else if (pvar_def instanceof PVARIABLE_ACTION_DEF)
			var_src = _actions.get(p);
		else if (pvar_def instanceof PVARIABLE_INTERM_DEF)
			var_src = _interm.get(p);
		else if (pvar_def instanceof PVARIABLE_OBS_DEF)
			var_src = _observ.get(p);
		
		if (var_src == null) {
			System.out.println("ERROR: no variable source for " + p);
			return false;
		}

		// Set value (or remove if default)... n.b., def_value could be null if not s,a,s'
		if (value == null || value.equals(def_value)) {
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
			if (objects == null)
				throw new EvalException("ERROR: could not find definition of object type '" + type + "'\nwhen instantiating " + pvar_def);
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
					PVARIABLE_DEF pvar_def = _hmPVariables.get(p);
					boolean derived = (pvar_def instanceof PVARIABLE_INTERM_DEF) && ((PVARIABLE_INTERM_DEF)pvar_def)._bDerived;
					
					ArrayList<ArrayList<LCONST>> gfluents = generateAtoms(p);										
					for (ArrayList<LCONST> gfluent : gfluents)
						sb.append("- " + (derived ? "derived" : e.getKey()) + ": " + p + 
								(gfluent.size() > 0 ? gfluent : "") + " := " + 
								getPVariableAssign(p, gfluent) + "\n");
						
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment" + e.getKey() + " for " + p + "\n");
				}
		}
				
		return sb.toString();
	}
}
