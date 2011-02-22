/**
 * RDDL: Defines all nodes in the internal tree representation of RDDL
 *       and simulation code for sampling from expression constructs.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl;

import java.util.*;

import util.Pair;

public class RDDL {

	public final static boolean DEBUG_EXPR_EVAL = false;
	
	public static boolean USE_PREFIX = false;
	
	public RDDL() { }

	public RDDL(RDDL rddl) { 
		_tmDomainNodes.putAll(rddl._tmDomainNodes);
		_tmInstanceNodes.putAll(rddl._tmInstanceNodes);	
		_tmNonFluentNodes.putAll(rddl._tmNonFluentNodes);
	}

	public void addDomain(DOMAIN d) {
		_tmDomainNodes.put(d._sDomainName, d);
	}

	public void addInstance(INSTANCE i) {
		_tmInstanceNodes.put(i._sName, i);		
	}

	public void addNonFluents(NONFLUENTS n) {
		_tmNonFluentNodes.put(n._sName, n);		
	}
	
	public void addOtherRDDL(RDDL rddl) {
		_tmDomainNodes.putAll(rddl._tmDomainNodes);
		_tmInstanceNodes.putAll(rddl._tmInstanceNodes);	
		_tmNonFluentNodes.putAll(rddl._tmNonFluentNodes);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (DOMAIN d : _tmDomainNodes.values())
			sb.append(d + "\n\n");
		for (NONFLUENTS n : _tmNonFluentNodes.values())
			sb.append(n + "\n\n");
		for (INSTANCE i : _tmInstanceNodes.values())
			sb.append(i + "\n\n");
		return sb.toString();
	}
	
	public TreeMap<String,DOMAIN>     _tmDomainNodes    = new TreeMap<String,DOMAIN>();
	public TreeMap<String,NONFLUENTS> _tmNonFluentNodes = new TreeMap<String,NONFLUENTS>();
	public TreeMap<String,INSTANCE>   _tmInstanceNodes  = new TreeMap<String,INSTANCE>();

	public static class DOMAIN {
		
		public DOMAIN(ArrayList l) {
			for (String s : (ArrayList<String>)l) {
				if (s.equals("concurrent")) {
					_bConcurrent = true;
				} else if (s.equals("continuous")) {
					_bContinuous = true;
				} else if (s.equals("integer-valued")) {
					_bInteger = true;
				} else if (s.equals("multivalued")) {
					_bMultivalued = true;
				} else if (s.equals("intermediate-nodes")) {
					_bIntermediateNodes = true;
				} else if (s.equals("constrained-state")) {
					_bStateConstraints = true;
				} else if (s.equals("cpf-deterministic")) {
					_bCPFDeterministic = true;
				} else if (s.equals("reward-deterministic")) {
					_bRewardDeterministic = true;
				} else if (s.equals("partially-observed")) {
					_bPartiallyObserved = true;
				} else {
					System.err.println("Unrecognized requirement '" + s + "'.");
				}
			}
		}
		
		public void setName(String s) {
			_sDomainName = s;
		}
		
		public void addDefs(ArrayList l) throws Exception {
			for (Object o : l) {
				if (o instanceof TYPE_DEF) {
					TYPE_DEF t = (TYPE_DEF)o;
					if (_hmTypes.containsKey(t._sName))
						throw new Exception("Type definition: '" + t._sName + "' defined twice!");
					_hmTypes.put(t._sName, t);
				} else if (o instanceof PVARIABLE_DEF) {
					PVARIABLE_DEF pv = (PVARIABLE_DEF)o;
					if (_hmPVariables.containsKey(pv._sName))
						throw new Exception("PVariable definition: '" + pv._sName + "' defined twice!");
					_hmPVariables.put(pv._sName, pv);
				} else if (o instanceof CPF_HEADER_NAME) {
					CPF_HEADER_NAME n = (CPF_HEADER_NAME)o;
					_sCPFHeader = n._sName;
					if (n._sName.equals("cpfs")) {
						if (_bCPFDeterministic)
							throw new Exception("'cpfs' used but requirements indicated cpfs were deterministic... use 'cdfs' instead.");
					} else if (n._sName.equals("cdfs")) {
						if (!_bCPFDeterministic)
							throw new Exception("'cdfs' used but requirements did not indicate 'cpf-deterministic'.");
					} else 
						throw new Exception("Unrecognized cpfs/cdfs header.");
				} else if (o instanceof CPF_DEF) {
					CPF_DEF d = (CPF_DEF)o;
					if (_hmCPF.containsKey(d._exprVarName._sName))
						throw new Exception("CPF definition: '" + d._exprVarName._sName + "' defined twice!");
					_hmCPF.put(d._exprVarName._sName, d);
				} else if (o instanceof REWARD_DEF) {
					if (_exprReward != null)
						throw new Exception("Reward defined twice!");
					_exprReward = ((REWARD_DEF)o)._expr;
				} else if (o instanceof STATE_CONS_DEF) {
					STATE_CONS_DEF d = (STATE_CONS_DEF)o;
					_alStateConstraints.add(d._exprStateCons);
				} else {
					throw new Exception("Unrecognized definition: " + o.getClass());
				}
			}
		}
		
		public String _sDomainName = null;
		public String _sCPFHeader  = null;
		
		public boolean _bConcurrent = false;  // more than one non-default action 
		public boolean _bContinuous = false;  // use of real type
		public boolean _bInteger = false;     // use of int type
		public boolean _bMultivalued = false; // use of enum type
		public boolean _bIntermediateNodes = false;    // use of nodes with level > 0
		public boolean _bStateConstraints = false;     // use of state constraints
		public boolean _bCPFDeterministic = false;     // cpfs are deterministic
		public boolean _bRewardDeterministic = false;  // reward is deterministic
		public boolean _bPartiallyObserved = false;    // domain is a POMDP
		
		public HashMap<TYPE_NAME,TYPE_DEF>      _hmTypes      = new HashMap<TYPE_NAME,TYPE_DEF>();
		public HashMap<PVAR_NAME,PVARIABLE_DEF> _hmPVariables = new HashMap<PVAR_NAME,PVARIABLE_DEF>();
		public HashMap<PVAR_NAME,CPF_DEF>       _hmCPF        = new HashMap<PVAR_NAME,CPF_DEF>();

		public EXPR _exprReward = null;

		public ArrayList<BOOL_EXPR> _alStateConstraints = new ArrayList<BOOL_EXPR>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("domain " + _sDomainName + " {\n");
			sb.append("  requirements = {\n"
					+ (_bCPFDeterministic ? "    cpf-deterministic,\n" : "") 
					+ (_bRewardDeterministic ? "    reward-deterministic,\n" : "") 
					+ (_bConcurrent ?  "    concurrent,\n" : "") 
					+ (_bContinuous ?  "    continuous,\n" : "")
					+ (_bInteger ?     "    integer-valued,\n" : "")
					+ (_bMultivalued ? "    multivalued,\n" : "")
					+ (_bIntermediateNodes ? "    intermediate-nodes,\n" : "")
					+ (_bStateConstraints ?  "    constrained-state,\n" : "")
					+ (_bPartiallyObserved ? "    partially-observed,\n" : ""));
			sb.delete(sb.length() - 2, sb.length() - 1); // Remove last ,
			sb.append("  };\n");
			
			sb.append("  types {\n");
			for (TYPE_DEF tdef : _hmTypes.values()) {
				sb.append("    " + tdef + "\n");
			}
			sb.append("  };\n");

			//sb.append(" (:constants \n");
			//for (CONSTANT_DEF cdef : _tmConstants.values()) {
			//	sb.append("  " + cdef + "\n");
			//}
			//sb.append(" )\n");

			sb.append("  pvariables {\n");
			for (PVARIABLE_DEF pvdef : _hmPVariables.values()) {
				sb.append("    " + pvdef + "\n");
			}
			sb.append("  };\n");

			sb.append("  " + _sCPFHeader + " {\n");
			for (CPF_DEF cpfdef : _hmCPF.values()) {
				sb.append("    " + cpfdef + "\n");
			}
			sb.append("  };\n");

			sb.append("  reward = " + _exprReward + ";\n");

			sb.append("  state-constraints {\n");
			for (BOOL_EXPR sc : _alStateConstraints) {
				sb.append("    " + sc + ";\n");
			}
			sb.append("  };\n");
			
			sb.append("}");

			return sb.toString();
		}
	}

	
	//////////////////////////////////////////////////////////
		
	public static class INSTANCE {
		
		// objects and non_fluents may be null
		public INSTANCE(String name, String domain, String nonfluents, 
						ArrayList objects, ArrayList init_state, 
						int nondef_actions, int horizon, double discount) {
			_sName     = name;
			_sDomain   = domain;
			_sNonFluents    = nonfluents;
			_nNonDefActions = nondef_actions;
			_nHorizon  = horizon;
			_dDiscount = discount;
			if (objects != null)
				for (OBJECTS_DEF od : (ArrayList<OBJECTS_DEF>)objects)
					_hmObjects.put(od._sObjectClass, od);
			_alInitState = (ArrayList<PVAR_INST_DEF>)init_state;
		}
		
		public String _sName     = null;
		public String _sDomain   = null;
		public String _sNonFluents = null;
		public int _nNonDefActions = -1;
		public int    _nHorizon  = -1;
		public double _dDiscount = 0.9d;
		
		public HashMap<TYPE_NAME,OBJECTS_DEF> _hmObjects = new HashMap<TYPE_NAME,OBJECTS_DEF>();
		public ArrayList<PVAR_INST_DEF> _alInitState = new ArrayList<PVAR_INST_DEF>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("instance " + _sName + " {\n");
			sb.append("  domain = "   + _sDomain + ";\n");
			if (_sNonFluents != null)
				sb.append("  non-fluents = "   + _sNonFluents + ";\n");
			if (_hmObjects.size() > 0) {
				sb.append("  objects {\n");
				for (OBJECTS_DEF obj : _hmObjects.values()) {
					sb.append("    " + obj + "\n");
				}
				sb.append("  };\n");
			}
			// TODO: will not handle non-boolean fluents
			sb.append("  init-state {\n");
			for (PVAR_INST_DEF isd : _alInitState) {
				sb.append("    " + isd + "\n");
			}
			sb.append("  };\n");
			sb.append("  max-nondef-actions = "  + _nNonDefActions + ";\n");
			sb.append("  horizon = "  + _nHorizon + ";\n");
			sb.append("  discount = " + _dDiscount + ";\n");
			sb.append("}");

			return sb.toString();
		}
	}

	//////////////////////////////////////////////////////////
	
	public static class NONFLUENTS {
		
		// objects may be null
		public NONFLUENTS(String name, String domain, ArrayList objects, ArrayList non_fluents) {
			_sName     = name;
			_sDomain   = domain;
			if (objects != null)
				for (OBJECTS_DEF od : (ArrayList<OBJECTS_DEF>)objects)
					_hmObjects.put(od._sObjectClass, od);
			_alNonFluents = (ArrayList<PVAR_INST_DEF>)non_fluents;
		}
		
		public String _sName = null;
		public String _sDomain = null;
		
		public HashMap<TYPE_NAME,OBJECTS_DEF> _hmObjects = new HashMap<TYPE_NAME,OBJECTS_DEF>();
		public ArrayList<PVAR_INST_DEF> _alNonFluents = new ArrayList<PVAR_INST_DEF>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("non-fluents " + _sName + " {\n");
			sb.append("  domain = "   + _sDomain + ";\n");
			if (_hmObjects.size() > 0) {
				sb.append("  objects {\n");
				for (OBJECTS_DEF obj : _hmObjects.values()) {
					sb.append("    " + obj + "\n");
				}
				sb.append("  };\n");
			}
			// TODO: will not handle non-boolean fluents
			sb.append("  non-fluents {\n");
			for (PVAR_INST_DEF isd : _alNonFluents) {
				sb.append("    " + isd + "\n");
			}
			sb.append("  };\n");
			sb.append("}");

			return sb.toString();
		}
	}
	
	/////////////////////////////////////////////////////////
	
	public abstract static class TYPE_DEF {
				
		public TYPE_DEF(String name, String type) throws Exception {
			_sName = new TYPE_NAME(name);
			_sType = type.intern();
			if (!(_sType.equals("enum") || _sType.equals("object")))
				throw new Exception("RDDL: Illegal type '" + type + "' in typedef");
		}
		
		public TYPE_NAME _sName;
		public String _sType;
	}
	
	public static class ENUM_TYPE_DEF extends TYPE_DEF {
	
		public ENUM_TYPE_DEF(String name, ArrayList values) throws Exception {
			super(name, "enum");
			_alPossibleValues = (ArrayList<ENUM_VAL>)values;
		}
		
		public ArrayList<ENUM_VAL> _alPossibleValues;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName + " : {");
			boolean first = true;
			for (ENUM_VAL s : _alPossibleValues) {
				sb.append((first ? "" : ", ") + s);
				first = false;
			}
			sb.append("};");
			return sb.toString();
		}
	}

	public static class OBJECT_TYPE_DEF extends TYPE_DEF {
	
		public OBJECT_TYPE_DEF(String name) throws Exception {
			super(name, "object");
		}
		
		public ArrayList<String> _alPossibleValues;
		
		public String toString() {
			return "" + _sName + " : " + _sType + ";";
		}
	}
	
	public abstract static class PVARIABLE_DEF {
		
		public PVARIABLE_DEF(String name, String range, ArrayList param_types) {
			_sName = new PVAR_NAME(name);
			_sRange = new TYPE_NAME(range);
			_alParamTypes = new ArrayList<TYPE_NAME>();
			for (String type : (ArrayList<String>)param_types)
				_alParamTypes.add(new TYPE_NAME(type));
		}
		
		public PVAR_NAME _sName;
		public String _sType;
		public TYPE_NAME _sRange;
		public ArrayList<TYPE_NAME> _alParamTypes;
	}
	
	public static class PVARIABLE_STATE_DEF extends PVARIABLE_DEF {
		
		public PVARIABLE_STATE_DEF(String name, boolean non_fluent, 
				String range, ArrayList param_types, Object def_value) {
			super(name, range, param_types);
			_bNonFluent = non_fluent;
			_oDefValue = def_value;
		}
		
		public boolean _bNonFluent = false;
		public Object  _oDefValue  = null;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}			
			sb.append(" : {" + (_bNonFluent ? "non-fluent" : "state-fluent") + 
					  ", " + _sRange + ", default = " +	_oDefValue + "};");
			return sb.toString();
		}

	}
	
	public static class PVARIABLE_INTERM_DEF extends PVARIABLE_DEF {
		
		public PVARIABLE_INTERM_DEF(String name, String range, ArrayList param_types, Integer level) {
			super(name, range, param_types);
			_nLevel = level;
		}		
		
		public int _nLevel = -1;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}			
			sb.append(" : {interm-fluent, " + _sRange + 
					  ", level = " + _nLevel + "};");
			return sb.toString();
		}

	}
	
	public static class PVARIABLE_OBS_DEF extends PVARIABLE_DEF {
		
		public PVARIABLE_OBS_DEF(String name, String range, ArrayList param_types) {
			super(name, range, param_types);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}			
			sb.append(" : {observ-fluent, " + _sRange + "};");
			return sb.toString();
		}

	}

	public static class PVARIABLE_ACTION_DEF extends PVARIABLE_DEF {
		
		public PVARIABLE_ACTION_DEF(String name, String range, 
				ArrayList param_types, Object def_value) {
			super(name, range, param_types);
			_oDefValue = def_value;
		}

		public Object _oDefValue;

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}			
			sb.append(" : {action-fluent, " + _sRange + ", default = " + _oDefValue + "};");
			return sb.toString();
		}
	}

	public static class CPF_HEADER_NAME {
		public CPF_HEADER_NAME(String s) { _sName = s; }
		public String _sName;
		public String toString() { return _sName; }
	}
		
	public static class CPF_DEF {
	
		public CPF_DEF(PVAR_EXPR pexpr, EXPR expr) {
			_exprVarName = pexpr;
			_exprEquals  = expr;
		}
		
		public PVAR_EXPR _exprVarName;
		public EXPR _exprEquals;
		
		public String toString() {
			return _exprVarName + " = " + _exprEquals + ";";
		}
	}

	public static class REWARD_DEF {
	
		public REWARD_DEF(EXPR expr) {
			_expr = expr;
		}
		
		public EXPR  _expr;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("reward " + _expr + ";");
			return sb.toString();
		}
	}

	public static class STATE_CONS_DEF {
	
		public STATE_CONS_DEF(BOOL_EXPR cons) {
			_exprStateCons = cons;
		}
		
		public BOOL_EXPR _exprStateCons;
		
		public String toString() {
			return _exprStateCons.toString() + ";";
		}
	}
		
	//////////////////////////////////////////////////////////

	// TODO: To enable object fluents, remove TVAR_EXPR and modify parser 
	//       to nest PVAR_EXPRs as LTERMs and cast output to LCONST rather
	//       than just ENUM_VAL to allow for a type of "object-fluent"; 
	//       or can let TVAR_EXPR remain (although a little redundant and 
	//       just directly modify to return general LCONST and allow an 
	//       "object fluent" expression type.  Note: still want to separate 
	//       objects/enums from general arithmetic expressions.
	public static abstract class LTERM extends EXPR { 
		public Object getTermSub(HashMap<LVAR, LCONST> subs, State s, Random r)
		throws EvalException {
			return sample(subs, s, r);
		}
	}
			
	public static class LVAR extends LTERM {
		
		public LVAR(String var_name) {
			_sVarName  = var_name.intern();
			_nHashCode = var_name.hashCode(); 
		}
		
		public String _sVarName;
		public int    _nHashCode;
				
		// Precomputed
		public int hashCode() {
			return _nHashCode;
		}
		
		// Name was interned so can check reference equality
		public boolean equals(Object o) {
			return _sVarName == ((LVAR)o)._sVarName; 
		}

		public String toString() {
			return _sVarName;
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}

		public Object sample(HashMap<LVAR, LCONST> subs, State s, Random r)
		throws EvalException {
			LCONST sub = subs.get(this);
			if (sub == null)
				throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + this + "\n" + this);
			return sub;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("LVAR.getDist: Not a distribution.");
		}

	}
	
	public static class LTYPED_VAR extends LTERM {
		
		public LTYPED_VAR(String var_name, String type) {
			_sVarName = new LVAR(var_name);
			_sType    = new TYPE_NAME(type);
		}
		
		public LVAR _sVarName;
		public TYPE_NAME _sType;
		
		public String toString() {
			if (USE_PREFIX)
				return "(" + _sVarName + " : " + _sType + ")";
			else
				return _sVarName + " : " + _sType;
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}

		public Object sample(HashMap<LVAR, LCONST> subs, State s, Random r)
		throws EvalException {
			LCONST sub = subs.get(this);
			if (sub == null)
				throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + this + "\n" + this);
			return sub;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("LTYPED_VAR.getDist: Not a distribution.");
		}

	}
	
	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public abstract static class LCONST extends LTERM {
		
		public LCONST(String const_value) {
			_sConstValue = const_value.intern();
			_nHashCode = const_value.hashCode();
		}
		
		public String _sConstValue;
		public int    _nHashCode;
		
		public String toString() {
			return _sConstValue;
		}
		
		// Precomputed
		public int hashCode() {
			return _nHashCode;
		}
		
		// Name was interned so can check reference equality
		public boolean equals(Object o) {
			return _sConstValue == ((LCONST)o)._sConstValue; 
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}
		
		public Object sample(HashMap<LVAR, LCONST> subs, State s, Random r)
			throws EvalException {
			return this;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("LCONST.getDist: Not a distribution.");
		}

	}

	public static class TVAR_EXPR extends LTERM {
		
		public TVAR_EXPR(String s, ArrayList terms) {
			_sName = new PVAR_NAME(s);
			_alTerms = (ArrayList<LTERM>)terms;
		}
		
		public TVAR_EXPR(PVAR_EXPR p) {
			_sName = p._sName;
			_alTerms = p._alTerms;
		}
		
		public PVAR_NAME _sName;
		public ArrayList<LTERM>  _alTerms  = null;
		public ArrayList<LCONST> _subTerms = new ArrayList<LCONST>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX)
				sb.append("(");
			sb.append(_sName);
			if (_alTerms.size() > 0) {
				boolean first = true;
				if (!USE_PREFIX) 
					sb.append("(");
				for (LTERM term : _alTerms) {
					if (USE_PREFIX)
						sb.append(" " + term);
					else
						sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			} else if (USE_PREFIX) // Prefix always parenthesized
				sb.append(")");	
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			
			_subTerms.clear();
			for (int i = 0; i < _alTerms.size(); i++) {
				LTERM t = _alTerms.get(i);
				if (t instanceof LCONST)
					_subTerms.add((LCONST)t);
				else if (t instanceof LVAR) {
					LCONST sub = subs.get((LVAR)t);
					if (sub == null)
						throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + t + "\n" + this);
					_subTerms.add(sub);
				} else if (t instanceof ENUM_VAL) {
					_subTerms.add((ENUM_VAL)t);
				} else if (t instanceof TVAR_EXPR) {
					TVAR_EXPR tvar = (TVAR_EXPR)t;
					_subTerms.add((ENUM_VAL)tvar.sample(subs, s, r));
				} else
					throw new EvalException("RDDL.PVAR_EXPR: Unrecognized term " + t + "\n" + this);
			}
			
			ENUM_VAL ret_val = (ENUM_VAL)s.getPVariableAssign(_sName, _subTerms);
			if (ret_val == null)
				throw new EvalException("RDDL.PVAR_EXPR: No value assigned to pvariable '" + 
						_sName + _subTerms + "'" + (_subTerms.size() == 0 ? "\n... did you intend an enum value @" + _sName+ "?" : "") + "");
			return ret_val;
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			
			// Skip non-fluents
			PVARIABLE_DEF pvar_def = s._hmPVariables.get(_sName);
			if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
				return;
			
			_subTerms.clear();
			for (int i = 0; i < _alTerms.size(); i++) {
				LTERM t = _alTerms.get(i);
				if (t instanceof LCONST)
					_subTerms.add((LCONST)t);
				else if (t instanceof LVAR) {
					LCONST sub = subs.get((LVAR)t);
					if (sub == null)
						throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + t + "\n" + this);
					_subTerms.add(sub);
				} else if (t instanceof ENUM_VAL) {
					_subTerms.add((ENUM_VAL)t);
				} else if (t instanceof TVAR_EXPR) {
					TVAR_EXPR tvar = (TVAR_EXPR)t;
					tvar.collectGFluents(subs, s, gfluents);
					_subTerms.add((ENUM_VAL)tvar.sample(subs, s, null));
				} else
					throw new EvalException("RDDL.PVAR_EXPR: Unrecognized term " + t + "\n" + this);
			}
			
			gfluents.add(new Pair(_sName, _subTerms.clone()));
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("TVAR_EXPR.getDist: Not a distribution.");
		}

	}
	
	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public static class TYPE_NAME {
		
		public final static TYPE_NAME BOOL_TYPE = new TYPE_NAME("bool");
		public final static TYPE_NAME INT_TYPE  = new TYPE_NAME("int");
		public final static TYPE_NAME REAL_TYPE = new TYPE_NAME("real");
		
		public TYPE_NAME(String obj_name) {
			_STypeName = obj_name.intern();
			_nHashCode = obj_name.hashCode();
		}
		
		public String _STypeName;
		public int    _nHashCode;
		
		public String toString() {
			return _STypeName;
		}
		
		// Precomputed
		public int hashCode() {
			return _nHashCode;
		}
		
		// Name was interned so can check reference equality
		public boolean equals(Object o) {
			return _STypeName == ((TYPE_NAME)o)._STypeName; 
		}
	}
		
	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public static class ENUM_VAL extends LCONST {
		public ENUM_VAL(String enum_name) {
			super(enum_name);
		}
	}
	
	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public static class OBJECT_VAL extends LCONST {
		public OBJECT_VAL(String enum_name) {
			super(enum_name);
		}
	}

	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public static class PVAR_NAME implements Comparable {
		
		public PVAR_NAME(String pred_name) {
			_bPrimed = pred_name.endsWith("'");
			if (_bPrimed) {
				pred_name = pred_name.substring(0, pred_name.length() - 1);
			}
			_sPVarName = pred_name.intern();
			_sPVarNameCanon = pred_name.replace('-','_').intern();
			_nHashCode = _sPVarNameCanon.hashCode() + (_bPrimed ? 1 : 0);
		}
		
		public String _sPVarName;
		public String _sPVarNameCanon;
		public boolean _bPrimed;
		public int    _nHashCode;
		
		public String toString() {
			return _sPVarName + (_bPrimed ? "'" : "");
		}
		
		// Precomputed
		public int hashCode() {
			return _nHashCode;
		}
		
		// Name was interned so can check reference equality
		public boolean equals(Object o) {
			return _sPVarNameCanon == ((PVAR_NAME)o)._sPVarNameCanon
			       && _bPrimed == ((PVAR_NAME)o)._bPrimed;
		}

		// Does the job to handle "'"... could make more efficient
		public int compareTo(Object o) {
			return toString().compareTo(o.toString());
		}
	}
	
	//////////////////////////////////////////////////////////

	public static abstract class EXPR { 
		
		public static final String UNKNOWN = "[Unknown type]".intern(); 
		
		public static final String REAL    = "[Real]".intern();
		public static final String INT     = "[Int]".intern();
		public static final String BOOL    = "[Bool]".intern();
		public static final String ENUM    = "[Enum]".intern();
		
		String  _sType = UNKNOWN; // real, int, bool, enum
		Boolean _bDet  = null;    // deterministic?  (if not, then stochastic)
		
		public abstract Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException;
		
		public abstract void collectGFluents(HashMap<LVAR,LCONST> subs, State s, HashSet<Pair> gfluents) throws EvalException;

		// Can support a prefix notation if requested
		//public abstract String toPrefix();
		
		// Recurses until distribution then samples parameters (assuming deterministic)
		public abstract EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException;
	}
	
	////////////////////////////////////////////////////////// 
	
	public static class DiracDelta extends EXPR {
		
		public DiracDelta(EXPR expr) {
			_exprRealValue = expr;
		}
		
		public EXPR _exprRealValue;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(DiracDelta " + _exprRealValue + ")";
			else
				return "DiracDelta(" + _exprRealValue + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			Object o = _exprRealValue.sample(subs, s, r);
			if (!(o instanceof Double))
				throw new EvalException("RDDL: DiracDelta only applies to real-valued data.\n" + this);
			return o;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			Double d = ((Number)_exprRealValue.sample(subs, s, null)).doubleValue();
			return new DiracDelta(new REAL_CONST_EXPR(d));
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprRealValue.collectGFluents(subs, s, gfluents);
		}
	}
	
	public static class KronDelta extends EXPR {
		
		public KronDelta(EXPR expr) {
			_exprIntValue = expr;
		}
		
		public EXPR _exprIntValue;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(KronDelta " + _exprIntValue + ")";
			else
				return "KronDelta(" + _exprIntValue + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			Object o = _exprIntValue.sample(subs, s, r);
			if (!(o instanceof Integer) && !(o instanceof Boolean) && !(o instanceof ENUM_VAL /*enum*/))
				throw new EvalException("RDDL: KronDelta only applies to integer/boolean data, not " + (o == null ? null : o.getClass()) + ".\n" + this);
			return o;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			Object o = _exprIntValue.sample(subs, s, null);
			if (o instanceof Integer) 
				return new KronDelta(new INT_CONST_EXPR((Integer)o));
			if (o instanceof Boolean)
				return new KronDelta(new BOOL_CONST_EXPR((Boolean)o));

			return null;
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprIntValue.collectGFluents(subs, s, gfluents);
		}

	}
	
	public static class Uniform extends EXPR {
		
		public Uniform(EXPR lower, EXPR upper) {
			_exprLowerReal = lower;
			_exprUpperReal = upper;
		}
		
		public EXPR _exprLowerReal;
		public EXPR _exprUpperReal;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(Uniform " + _exprLowerReal + " " + _exprUpperReal + ")";
			else
				return "Uniform(" + _exprLowerReal + ", " + _exprUpperReal + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			try {
				double l = ((Number)_exprLowerReal.sample(subs, s, r)).doubleValue();
				double u = ((Number)_exprUpperReal.sample(subs, s, r)).doubleValue();
				if (l > u)
					throw new EvalException("RDDL: Uniform upper bound '" + 
							u + "' must be greater than lower bound '" + l + "'");
				return r.nextDouble()*(u-l) + l; 
			} catch (Exception e) {
				throw new EvalException("RDDL: Uniform only applies to real (or castable to real) data.\n" + e + "\n" + this);
			}
		}
		

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {

			try {
				double l = ((Number)_exprLowerReal.sample(subs, s, null)).doubleValue();
				double u = ((Number)_exprUpperReal.sample(subs, s, null)).doubleValue();
				if (l > u)
					throw new EvalException("RDDL: Uniform upper bound '" + 
							u + "' must be greater than lower bound '" + l + "'");
				return new Uniform(new REAL_CONST_EXPR(l), new REAL_CONST_EXPR(u)); 
			} catch (Exception e) {
				throw new EvalException("RDDL: Uniform only applies to real (or castable to real) data.\n" + e + "\n" + this);
			}
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
		throws EvalException {
			_exprLowerReal.collectGFluents(subs, s, gfluents);
			_exprUpperReal.collectGFluents(subs, s, gfluents);
		}
		
	}

	public static class Normal extends EXPR {
		
		public Normal(EXPR mean, EXPR var) {
			_normalMeanReal = mean;
			_normalVarReal  = var;
		}
		
		public EXPR _normalMeanReal;
		public EXPR _normalVarReal;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(Normal " + _normalMeanReal + " " + _normalVarReal + ")";
			else
				return "Normal(" + _normalMeanReal + ", " + _normalVarReal + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			try {
				double mean = ((Number)_normalMeanReal.sample(subs, s, r)).doubleValue();
				double var  = ((Number)_normalVarReal.sample(subs, s, r)).doubleValue();
				if (var < 0)
					throw new EvalException("RDDL: Normal variance '" + var +  
							"' must be greater 0");
				// x ~ N(mean,sigma^2) is equivalent to x ~ sigma*N(0,1) + mean
				return r.nextGaussian()*Math.sqrt(var) + mean; 
			} catch (Exception e) {
				throw new EvalException("RDDL: Normal only applies to real (or castable to real) mean and variance.\n" + e + "\n" + this);
			}
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double mean = ((Number)_normalMeanReal.sample(subs, s, null)).doubleValue();
			double var  = ((Number)_normalVarReal.sample(subs, s, null)).doubleValue();
			if (var < 0)
				throw new EvalException("RDDL: Normal variance '" + var +  
						"' must be greater 0");
			// x ~ N(mean,sigma^2) is equivalent to x ~ sigma*N(0,1) + mean
			return new Normal(new REAL_CONST_EXPR(mean), new REAL_CONST_EXPR(var)); 
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_normalMeanReal.collectGFluents(subs, s, gfluents);
			_normalVarReal.collectGFluents(subs, s, gfluents);
		}
	}
	
	public static class Exponential extends EXPR {
		
		public Exponential(EXPR lambda) {
			_exprLambdaReal = lambda;
		}
		
		public EXPR _exprLambdaReal;
		
		public String toString() {
			return "(Exponential " + _exprLambdaReal + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			throw new EvalException("RDDL: Sampling from Exponential not yet implemented");
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double lambda = ((Number)_exprLambdaReal.sample(subs, s, null)).doubleValue();
			return new Exponential(new REAL_CONST_EXPR(lambda));
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprLambdaReal.collectGFluents(subs, s, gfluents);
		}
	
	}
	
	public static class Discrete extends EXPR {
		
		public Discrete(String enum_type, ArrayList probs) {
			_sEnumType = new TYPE_NAME(enum_type);
			_exprProbs = (ArrayList<EXPR>)probs;
		}
		
		public TYPE_NAME       _sEnumType;
		public ArrayList<EXPR> _exprProbs = null; // At runtime, check these sum to 1
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(Discrete " + _sEnumType + " ( ");
				for (int i = 0; i < _exprProbs.size(); i+=2)
					sb.append("(" + ((ENUM_VAL)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)) + ") ");
				sb.append(")");
			} else {
				sb.append("Discrete(" + _sEnumType);
				for (int i = 0; i < _exprProbs.size(); i+=2)
					sb.append(", " + ((ENUM_VAL)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)));
			}
			sb.append(")");
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			try {
				ENUM_TYPE_DEF etd = (ENUM_TYPE_DEF)s._hmTypes.get(_sEnumType);
				ArrayList<ENUM_VAL> enum_label = new ArrayList<ENUM_VAL>();
				ArrayList<Double> enum_probs = new ArrayList<Double>();
				double total = 0d;
				for (int i = 0; i < _exprProbs.size(); i+=2) {
					enum_label.add((ENUM_VAL)_exprProbs.get(i));
					enum_probs.add(((Number)((EXPR)_exprProbs.get(i+1)).sample(subs, s, r)).doubleValue());
					total += enum_probs.get(i/2);
				}
				if (Math.abs(1.0 - total) > 1.0e-6)
					throw new EvalException("Discrete probabilities did not sum to 1.0: " + total + " : " + enum_probs);
				if (!new HashSet<ENUM_VAL>(enum_label).equals(new HashSet<ENUM_VAL>(etd._alPossibleValues)))
					throw new EvalException("Expected enum values: " + etd._alPossibleValues + ", but got " + enum_label);

				double rand = r.nextDouble();
				for (int i = 0; i < enum_probs.size(); i++) {
					rand -= enum_probs.get(i);
					if (rand < 0)
						return enum_label.get(i);
				}
				throw new EvalException("Sampling error, failed to return value: " + enum_probs);

			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new EvalException("RDDL: Discrete only applies to real (or castable to real) values that sum to one.\n" + e + "\n" + this);
			}
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {

			throw new EvalException("Not implemented");
			//return null;
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			for (Object o : _exprProbs) 
				if (o instanceof EXPR)
					((EXPR)o).collectGFluents(subs, s, gfluents);
		}
	}
	
	public static class Geometric extends EXPR {
		
		public Geometric(EXPR prob) {
			_exprProb = prob;
		}
		
		public EXPR _exprProb;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Geometric " + _exprProb + ")";
			else
				return "Geometric(" + _exprProb + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			throw new EvalException("RDDL: Sampling from Geometric not yet implemented");
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double prob = ((Number)_exprProb.sample(subs, s, null)).doubleValue();
			return new Geometric(new REAL_CONST_EXPR(prob));
		}
	
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprProb.collectGFluents(subs, s, gfluents);
		}

	}
	
	public static class Poisson extends EXPR {
		
		public Poisson(EXPR lambda) {
			_exprLambda = lambda;
		}
		
		public EXPR _exprLambda;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Poisson " + _exprLambda + ")";
			else
				return "Poisson(" + _exprLambda + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			throw new EvalException("RDDL: Sampling from Poisson not yet implemented");
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double lambda = ((Number)_exprLambda.sample(subs, s, null)).doubleValue();
			return new Geometric(new REAL_CONST_EXPR(lambda));
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprLambda.collectGFluents(subs, s, gfluents);
		}
	}
	
	public static class Bernoulli extends BOOL_EXPR {
				
		public Bernoulli(EXPR prob) {
			_exprProb = prob;
		}
		
		public EXPR _exprProb;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Bernoulli " + _exprProb + ")";
			else
				return "Bernoulli(" + _exprProb + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			double prob = ((Number)_exprProb.sample(subs, s, r)).doubleValue();
			if (prob < 0.0d || prob > 1.0d)
				throw new EvalException("RDDL: Bernoulli prob " + prob + " not in [0,1]\n" + _exprProb);
			if (r.nextDouble() < prob) // Bernoulli parameter is prob of being true
				return TRUE;
			else 
				return FALSE;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double prob = ((Number)_exprProb.sample(subs, s, null)).doubleValue();
			return new Bernoulli(new REAL_CONST_EXPR(prob));
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprProb.collectGFluents(subs, s, gfluents);
		}

	}
	
	//////////////////////////////////////////////////////////

	public static class INT_CONST_EXPR extends EXPR {
		
		public INT_CONST_EXPR(Integer i) {
			_nValue = i;
			_sType = INT;
		}
		
		public Integer _nValue;
		
		public String toString() {
			return _nValue.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			return _nValue;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("INT_CONST_EXPR.getDist: Not a distribution.");
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}

	}

	public static class REAL_CONST_EXPR extends EXPR {
		
		public REAL_CONST_EXPR(Double d) {
			_dValue = d;
			_sType  = REAL;
		}
		
		public Double _dValue;
		
		public String toString() {
			return _dValue.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			return _dValue;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("REAL_CONST_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}
	
	}

	public static class OPER_EXPR extends EXPR {
	
		public static final String PLUS  = "+".intern();
		public static final String MINUS = "-".intern();
		public static final String TIMES = "*".intern();
		public static final String DIV   = "/".intern();
		
		public OPER_EXPR(EXPR e1, EXPR e2, String op) throws Exception {
			if (!op.equals(PLUS) && !op.equals(MINUS) && !op.equals(TIMES) && !op.equals(DIV))
				throw new Exception("Unrecognized arithmetic operator: " + op);
			_op = op.intern();
			_e1 = e1;
			_e2 = e2;
		}
		
		public EXPR _e1 = null;
		public EXPR _e2 = null;
		public String _op = UNKNOWN;
		
		public String toString() {
			if (USE_PREFIX)
				return "(" + _op + " " + _e1 + " " + _e2 + ")";
			else
				return "(" + _e1 + " " + _op + " " + _e2 + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			try {
				Object o1 = _e1.sample(subs, s, r);
				Object o2 = _e2.sample(subs, s, r);
				return ComputeArithmeticResult(o1, o2, _op);
			} catch (EvalException e) {
				throw new EvalException(e + "\n" + this);
			}
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("OPER_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_e1.collectGFluents(subs, s, gfluents);
			_e2.collectGFluents(subs, s, gfluents);
		}

	}

	public static Object ComputeArithmeticResult(Object o1, Object o2, String op) throws EvalException {

		if (o1 instanceof Boolean)
			o1 = ((Boolean)o1 == true ? 1 : 0);
		if (o2 instanceof Boolean)
			o2 = ((Boolean)o2 == true ? 1 : 0);
		if (!((o1 instanceof Integer) || (o1 instanceof Double))
			|| !((o2 instanceof Integer) || (o2 instanceof Double)))
			throw new EvalException("Operands 1 '" + o1 + "' and 2 '" + o2 + "' must be castable to int or real");
		
		if (o1 instanceof Integer && o2 instanceof Integer && op != OPER_EXPR.DIV) {
			if (op == OPER_EXPR.PLUS)
				return new Integer((Integer)o1 + (Integer)o2);
			else if (op == OPER_EXPR.MINUS)
				return new Integer((Integer)o1 - (Integer)o2);
			else if (op == OPER_EXPR.TIMES)
				return new Integer((Integer)o1 * (Integer)o2);
			else
				throw new EvalException("RDDL.OperExpr: Unrecognized operation: " + op);
		}
		
		if (op == OPER_EXPR.PLUS)
			return new Double(((Number)o1).doubleValue() + ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.MINUS)
			return new Double(((Number)o1).doubleValue() - ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.TIMES)
			return new Double(((Number)o1).doubleValue() * ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.DIV)
			return new Double(((Number)o1).doubleValue() / ((Number)o2).doubleValue());
		else
			throw new EvalException("RDDL.OperExpr: Unrecognized operation: " + op); 
	}
	
	public static class AGG_EXPR extends EXPR {
	
		public static final String SUM  = "sum".intern();
		public static final String PROD = "prod".intern();
		
		public AGG_EXPR(String op, ArrayList<LTYPED_VAR> vars, EXPR e) throws Exception {
			if (!op.equals(SUM) && !op.equals(PROD))
				throw new Exception("Unrecognized aggregation operator: " + op);
			_op = op.intern();
			_alVariables = (ArrayList<LTYPED_VAR>)vars;
			_e  = e;
		}
		
		public EXPR   _e = null;
		public String _op = UNKNOWN;
		public ArrayList<LTYPED_VAR> _alVariables = null;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(" + _op + " ( ");
				for (LTYPED_VAR term : _alVariables)
					sb.append(term + " ");
				sb.append(") " + _e + ")");			
			} else {
				sb.append("[" + _op);
				boolean first = true;
				sb.append("_{");
				for (LTYPED_VAR term : _alVariables) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append("} " + _e + "]");
			}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {

			ArrayList<ArrayList<LCONST>> possible_subs = s.generateAtoms(_alVariables);
			Object result = null;
			
			// Evaluate all possible substitutions
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < _alVariables.size(); i++) {
					subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
				}
				
				Object interm_result = _e.sample(subs, s, r);
				if (DEBUG_EXPR_EVAL)
					System.out.println(" - " + subs + " : " + interm_result);

				if (result == null)
					result = interm_result;
				else 
					result = ComputeArithmeticResult(result, interm_result, 
							     _op == SUM ? OPER_EXPR.PLUS : OPER_EXPR.TIMES);
			}
		
			// Clear all substitutions
			for (int i = 0; i < _alVariables.size(); i++) {
				subs.remove(_alVariables.get(i)._sVarName);
			}
			
			return result;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("AGG_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			
			ArrayList<ArrayList<LCONST>> possible_subs = s.generateAtoms(_alVariables);
			Object result = null;
			
			// Evaluate all possible substitutions
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < _alVariables.size(); i++) {
					subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
				}
				
				_e.collectGFluents(subs, s, gfluents);
			}
		
			// Clear all substitutions
			for (int i = 0; i < _alVariables.size(); i++) {
				subs.remove(_alVariables.get(i)._sVarName);
			}
		}
		
	}
	
	// TODO: Need a way to ensure that only boolean pvars go under forall
	// NOTE: technically a PVAR_EXPR does not have to be a boolean expression (it
	// could be int/real), but at parse time we don't know so we just put it
	// under BOOL_EXPR which is a subclass of EXPR.
	public static class PVAR_EXPR extends BOOL_EXPR {
		
		public PVAR_EXPR(String s, ArrayList terms) {
			_sName = new PVAR_NAME(s);
			_alTerms = (ArrayList<LTERM>)terms;
		}
		
		public PVAR_NAME _sName;
		public ArrayList<LTERM>  _alTerms  = null;
		public ArrayList<LCONST> _subTerms = new ArrayList<LCONST>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) 
				sb.append("(");
			sb.append(_sName);
			if (_alTerms.size() > 0) {
				boolean first = true;
				if (!USE_PREFIX)
					sb.append("(");
				for (LTERM term : _alTerms) {
					if (USE_PREFIX)
						sb.append(" " + term);
					else
						sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			} else if (USE_PREFIX) // Prefix always parenthesized
				sb.append(")");				
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			
			_subTerms.clear();
			for (int i = 0; i < _alTerms.size(); i++) {
				LTERM t = _alTerms.get(i);
				if (t instanceof LCONST)
					_subTerms.add((LCONST)t);
				else if (t instanceof LVAR) {
					LCONST sub = subs.get((LVAR)t);
					if (sub == null)
						throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + t + "\n" + this);
					_subTerms.add(sub);
				} else if (t instanceof ENUM_VAL) {
					_subTerms.add((ENUM_VAL)t);
				} else if (t instanceof TVAR_EXPR) {
					TVAR_EXPR tvar = (TVAR_EXPR)t;
					_subTerms.add((ENUM_VAL)tvar.sample(subs, s, r));
				} else
					throw new EvalException("RDDL.PVAR_EXPR: Unrecognized term " + t + "\n" + this);
			}
			
			Object ret_val = s.getPVariableAssign(_sName, _subTerms);
			if (ret_val == null)
				throw new EvalException("RDDL.PVAR_EXPR: No value assigned to pvariable '" + 
						_sName + _subTerms + "'" + (_subTerms.size() == 0 ? "\n... did you intend an enum value @" + _sName+ "?" : "") + "");
			return ret_val;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("PVAR_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			
			// Skip non-fluents
			PVARIABLE_DEF pvar_def = s._hmPVariables.get(_sName);
			if (pvar_def instanceof PVARIABLE_STATE_DEF && ((PVARIABLE_STATE_DEF)pvar_def)._bNonFluent)
				return;
			
			_subTerms.clear();
			for (int i = 0; i < _alTerms.size(); i++) {
				LTERM t = _alTerms.get(i);
				if (t instanceof LCONST)
					_subTerms.add((LCONST)t);
				else if (t instanceof LVAR) {
					LCONST sub = subs.get((LVAR)t);
					if (sub == null)
						throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + t + "\n" + this);
					_subTerms.add(sub);
				} else if (t instanceof ENUM_VAL) {
					_subTerms.add((ENUM_VAL)t);
				} else if (t instanceof TVAR_EXPR) {
					TVAR_EXPR tvar = (TVAR_EXPR)t;
					tvar.collectGFluents(subs, s, gfluents);
					_subTerms.add((ENUM_VAL)tvar.sample(subs, s, null));
				} else
					throw new EvalException("RDDL.PVAR_EXPR: Unrecognized term " + t + "\n" + this);
			}
			
			gfluents.add(new Pair(_sName, _subTerms.clone()));
		}

	}
	
	// TODO: should never put a random variable as an if test expression,
	//       a random sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class IF_EXPR extends EXPR { 

		public IF_EXPR(BOOL_EXPR test, EXPR true_branch, EXPR false_branch) {
			_test = test;
			_trueBranch = true_branch;
			_falseBranch = false_branch;
		}
		
		public BOOL_EXPR _test;
		public EXPR _trueBranch;
		public EXPR _falseBranch;
		
		public String toString() {
			if (USE_PREFIX)
				return "(if " + _test + " then " + _trueBranch + " else " + _falseBranch + ")";
			else
				return "if (" + _test + ") then [" + _trueBranch + "] else [" + _falseBranch + "]";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			Object test = _test.sample(subs, s, r);
			if (!(test instanceof Boolean))
				throw new EvalException("RDDL.IF_EXPR: test " + _test + " did not evaluate to boolean: " + test+ "\n" + this);
			if (((Boolean)test).booleanValue())
				return _trueBranch.sample(subs, s, r);
			else
				return _falseBranch.sample(subs, s, r);
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			Object test = _test.sample(subs, s, null);
			if (!(test instanceof Boolean))
				throw new EvalException("RDDL.IF_EXPR: test " + _test + " did not evaluate to boolean: " + test+ "\n" + this);
			if (((Boolean)test).booleanValue())
				return _trueBranch.getDist(subs, s);
			else
				return _falseBranch.getDist(subs, s);
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			//System.out.println("\nGfluents before " + "[" + gfluents.size() + "] " + _test + ": " + gfluents);
			_test.collectGFluents(subs, s, gfluents);
			//System.out.println("\nGfluents after " + "[" + gfluents.size() + "] " + _test + ": " + gfluents);
			_trueBranch.collectGFluents(subs, s, gfluents);
			//System.out.println("\nGfluents after true branch " + "[" + gfluents.size() + "] " + _test + ": " + gfluents);
			_falseBranch.collectGFluents(subs, s, gfluents);
			//System.out.println("\nGfluents after false branch " + "[" + gfluents.size() + "] " + _test + ": " + gfluents);
		}

	}

	public static class CASE {
		
		public CASE(ENUM_VAL enum_value, EXPR expr) {
			_sEnumValue = enum_value;
			_expr = expr;
		}
		
		public ENUM_VAL _sEnumValue;
		public EXPR     _expr;
		
		public String toString() {
			if (USE_PREFIX)
				return "(case " + _sEnumValue + " : " + _expr + ")";
			else
				return "case " + _sEnumValue + " : " + _expr;
		}

	}
	
	// TODO: should never put a random variable as a switch test expression,
	//       a random sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class SWITCH_EXPR extends EXPR {
		
		public SWITCH_EXPR(PVAR_EXPR enum_var, ArrayList cases) {
			_enumVar = enum_var;
			_cases = (ArrayList<CASE>)cases;
		}
		
		public PVAR_EXPR _enumVar; // Check enum!
		public ArrayList<CASE> _cases = null;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(switch " + _enumVar + " ( ");
				for (CASE c : _cases)
					sb.append(c + " ");
				sb.append(") )");				
			} else {
				sb.append("switch (" + _enumVar + ") {");
				boolean first = true;
				for (CASE c : _cases) {
					sb.append((first ? "" : ", ") + c);
					first = false;
				}
				sb.append("}");
			}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {

			try {
				PVARIABLE_DEF pvd = s._hmPVariables.get(_enumVar._sName);
				ENUM_TYPE_DEF etd = (ENUM_TYPE_DEF)s._hmTypes.get(pvd._sRange);
				if (etd == null)
					throw new EvalException("Enumerated variable " + _enumVar._sName + " is not defined.");
				ENUM_VAL seval = (ENUM_VAL)_enumVar.sample(subs, s, r);
				
				// A little inefficient, could store possible values as HashSet
				if (!etd._alPossibleValues.contains(seval)) 
					throw new EvalException("Expression result '" + seval + "' not contained in " + etd._alPossibleValues);
				
				// A little inefficient, could use HashMap
				for (CASE c : _cases)
					if (seval.equals(c._sEnumValue))
						return c._expr.sample(subs, s, r);
				
				throw new EvalException("No case for '" + seval + "' in " + _cases);
				
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new EvalException("RDDL: Switch requires enumerated variable type.\n" + e + "\n" + this);
			}

		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {

			try {
				PVARIABLE_DEF pvd = s._hmPVariables.get(_enumVar._sName);
				ENUM_TYPE_DEF etd = (ENUM_TYPE_DEF)s._hmTypes.get(pvd._sRange);
				if (etd == null)
					throw new EvalException("Enumerated variable " + _enumVar._sName + " is not defined.");
				ENUM_VAL seval = (ENUM_VAL)_enumVar.sample(subs, s, null);
				
				// A little inefficient, could store possible values as HashSet
				if (!etd._alPossibleValues.contains(seval)) 
					throw new EvalException("Expression result '" + seval + "' not contained in " + etd._alPossibleValues);
				
				// A little inefficient, could use HashMap
				for (CASE c : _cases)
					if (seval.equals(c._sEnumValue))
						return c._expr.getDist(subs, s);
				
				throw new EvalException("No case for '" + seval + "' in " + _cases);
				
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new EvalException("RDDL: Switch requires enumerated variable type.\n" + e + "\n" + this);
			}

		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_enumVar.collectGFluents(subs, s, gfluents);
			for (CASE c : _cases)
				c._expr.collectGFluents(subs, s, gfluents);
		}
	}

	//////////////////////////////////////////////////////////
	
	// Rule is that an expression below a forall/exists will be
	// evaluated in GroundKb, otherwise will be recursively evaluated
	// as a boolean expression.  
	//
	// Cannot use int/real vars (with equality) below a quantifier 
	// (should allow at a later time).
	//
	// Special handling for count above a ground evaluable expression
	// (no int/real vars).
	public abstract static class BOOL_EXPR extends EXPR { 
		public static final Boolean TRUE  = new Boolean(true);
		public static final Boolean FALSE = new Boolean(false);
	}

	// TODO: should never put a random variable directly under a quantifier,
	//       a random sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class QUANT_EXPR extends BOOL_EXPR {
		
		public final static String EXISTS = "exists".intern();
		public final static String FORALL = "forall".intern();
		
		public QUANT_EXPR(String quant, ArrayList vars, BOOL_EXPR expr) throws Exception {
			if (!quant.equals(EXISTS) && !quant.equals(FORALL))
				throw new Exception("Unrecognized quantifier type: " + quant);
			_sQuantType = quant.intern();
			_alVariables = (ArrayList<LTYPED_VAR>)vars;
			_expr = expr;
		}
		
		public String _sQuantType = null;
		public ArrayList<LTYPED_VAR> _alVariables = new ArrayList<LTYPED_VAR>();
		public BOOL_EXPR _expr;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(" + _sQuantType);
				sb.append(" ( ");
				for (LTYPED_VAR term : _alVariables)
					sb.append(term + " ");
				sb.append(") " + _expr + ")");			
			} else {
				sb.append("[" + _sQuantType);
				boolean first = true;
				sb.append("_{");
				for (LTYPED_VAR term : _alVariables) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append("} " + _expr + "]");
			}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {

			//System.out.println("VARS: " + _alVariables);
			ArrayList<ArrayList<LCONST>> possible_subs = s.generateAtoms(_alVariables);
			//System.out.println(possible_subs);
			Boolean result = null;
			
			// Evaluate all possible substitutions
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < _alVariables.size(); i++) {
					subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
				}
				
				// Update result
				Boolean interm_result = (Boolean)_expr.sample(subs, s, r);
				//System.out.println("Quant " + _sQuantType + " [" + subs + "]" + result + "/" + interm_result); 
				if (DEBUG_EXPR_EVAL)
					System.out.println(subs + " : " + interm_result);
				
				if (result == null)
					result = interm_result;
				else 
					result = (_sQuantType == FORALL) ? result && interm_result 
							  						 : result || interm_result;
				//System.out.println("After: " + result + " " + (_sQuantType == FORALL));
								
				// Early cutoff detection
				if (_sQuantType == FORALL && result == false)
					return BOOL_EXPR.FALSE;
				else if (_sQuantType == EXISTS && result == true) // exists
					return BOOL_EXPR.TRUE;
			}
		
			// Clear all substitutions
			for (int i = 0; i < _alVariables.size(); i++) {
				subs.remove(_alVariables.get(i)._sVarName);
			}
			
			return result;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("QUANT_EXPR.getDist: Cannot get distribution for a quantifier.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {

			//System.out.println("VARS: " + _alVariables);
			ArrayList<ArrayList<LCONST>> possible_subs = s.generateAtoms(_alVariables);
			//System.out.println(possible_subs);
			Boolean result = null;
			
			// Evaluate all possible substitutions
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < _alVariables.size(); i++) {
					subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
				}
				
				// Update result
				_expr.collectGFluents(subs, s, gfluents);

			}
		
			// Clear all substitutions
			for (int i = 0; i < _alVariables.size(); i++) {
				subs.remove(_alVariables.get(i)._sVarName);
			}
		}

	}

	// TODO: should never put a random variable directly under a connective,
	//       a random sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class CONN_EXPR extends BOOL_EXPR {

		public static final String IMPLY = "=>".intern();
		public static final String EQUIV = "<=>".intern();
		public static final String AND   = "^".intern();
		public static final String OR    = "|".intern();
		
		public CONN_EXPR(BOOL_EXPR b1, BOOL_EXPR b2, String conn) throws Exception {
			if (!conn.equals(IMPLY) && !conn.equals(EQUIV) && 
				!conn.equals(AND) && !conn.equals(OR))
				throw new Exception("Unrecognized logical connective: " + conn);
			_sConn = conn.intern();
			if (b1 instanceof CONN_EXPR && ((CONN_EXPR)b1)._sConn == _sConn)
				_alSubNodes.addAll(((CONN_EXPR)b1)._alSubNodes);
			else
				_alSubNodes.add(b1);
			if (b2 instanceof CONN_EXPR && ((CONN_EXPR)b2)._sConn == _sConn)
				_alSubNodes.addAll(((CONN_EXPR)b2)._alSubNodes);
			else
				_alSubNodes.add(b2);
		}
		
		public String _sConn;
		public ArrayList<BOOL_EXPR> _alSubNodes = new ArrayList<BOOL_EXPR>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder("(");
			if (USE_PREFIX) {
				sb.append(_sConn + " ");
				for (BOOL_EXPR b : _alSubNodes)
					sb.append(b + " ");
			} else {
				boolean first = true;
				for (BOOL_EXPR b : _alSubNodes) {
					sb.append((first ? "" : " " + _sConn + " ") + b);
					first = false;
				}
			}
			sb.append(")");
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			
			if (_sConn == IMPLY) {
				Boolean b1 = (Boolean)_alSubNodes.get(0).sample(subs, s, r);
				if (!b1)
					return TRUE;
				else
					return (Boolean)_alSubNodes.get(1).sample(subs, s, r);
			} else if (_sConn == EQUIV) {
				return ((Boolean)_alSubNodes.get(0).sample(subs, s, r)).equals(
						(Boolean)_alSubNodes.get(1).sample(subs, s, r));
			}

			// Now handle AND/OR
			Boolean result = null;
			for (BOOL_EXPR b : _alSubNodes) {
				Boolean interm_result = (Boolean)b.sample(subs, s, r);
				if (result == null)
					result = interm_result;
				else 
					result = (_sConn == AND) ? result && interm_result 
							  				 : result || interm_result;
		
				// Early cutoff detection
				if (_sConn == AND && result == false)
					return BOOL_EXPR.FALSE;
				else if (_sConn == OR && result == true) // exists
					return BOOL_EXPR.TRUE;
			}
			return result;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("CONN_EXPR.getDist: Cannot get distribution for a connective.");
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			
			// First go through and check for early termination in the case of AND / OR
			if (_sConn == AND || _sConn == OR) {
				for (BOOL_EXPR b : _alSubNodes) {
					if (b instanceof PVAR_EXPR) {
						PVAR_EXPR p = (PVAR_EXPR)b;
						if (s.getPVariableType(p._sName) == State.NONFLUENT) {
							boolean eval = (Boolean)p.sample(subs, s, null);
							// If can determine truth value of connective from nonfluents
							// then any other fluents are irrelevant
							if ((_sConn == AND && !eval) || (_sConn == OR && eval)) {
								//System.out.println("\n>> early termination on '" + subs + "'" + this);
								return; // Terminate fluent collection
							}
						}
					}
				}
			}
			
			// Otherwise collect subnodes
			for (BOOL_EXPR b : _alSubNodes)
				b.collectGFluents(subs, s, gfluents);
		}

	}
	
	// TODO: should never put a random variable directly under a negation,
	//       a random sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class NEG_EXPR extends BOOL_EXPR {

		public NEG_EXPR(BOOL_EXPR b) {
			_subnode = b;
		}
		
		public BOOL_EXPR _subnode;
		
		public String toString() {
			if (USE_PREFIX)
				return "(~ " + _subnode + ")";
			else
				return "~" + _subnode;
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			Boolean b = (Boolean)_subnode.sample(subs, s, r);
			return !b;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("NEG_EXPR.getDist: Cannot get distribution under a negation.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_subnode.collectGFluents(subs, s, gfluents);
		}
	}
	
	public static class BOOL_CONST_EXPR extends BOOL_EXPR {
		
		public BOOL_CONST_EXPR(boolean b) {
			_bValue = b;
			_sType = BOOL;
		}
		
		public boolean _bValue;
		
		public String toString() {
			return Boolean.toString(_bValue);
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			return _bValue;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("BOOL_CONST_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}
	
	}

	public static class COMP_EXPR extends BOOL_EXPR {
		
		public static final String NEQ = "~=".intern();
		public static final String LESSEQ = "<=".intern(); 
		public static final String LESS = "<".intern();
		public static final String GREATEREQ = ">=".intern(); 
		public static final String GREATER = ">".intern(); 
		public static final String EQUAL = "==".intern(); 

		public COMP_EXPR(EXPR e1, EXPR e2, String comp) throws Exception {
			if (!comp.equals(NEQ) && !comp.equals(LESSEQ) 
				&& !comp.equals(LESS) && !comp.equals(GREATEREQ)
				&& !comp.equals(GREATER) && !comp.equals(EQUAL))
					throw new Exception("Unrecognized inequality: " + comp);
			_comp = comp.intern();
			_e1 = e1;
			_e2 = e2;
		}
		
		public EXPR _e1 = null;
		public EXPR _e2 = null;
		public String _comp = UNKNOWN;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(" + _comp + " " + _e1 + " " + _e2 + ")";
			else
				return "(" + _e1 + " " + _comp + " " + _e2 + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
						
			Object o1 = _e1.sample(subs, s, r);
			Object o2 = _e2.sample(subs, s, r);
						
			// Handle special case of enum comparison
			if (o1 instanceof ENUM_VAL || o2 instanceof ENUM_VAL) {
				if (!(o1 instanceof ENUM_VAL && o2 instanceof ENUM_VAL))
					throw new EvalException("RDDL.COMP_EXPR: both values in enum comparison must be enum" + _comp + "\n" + this);
				if (!(_comp == NEQ || _comp == EQUAL))
					throw new EvalException("RDDL.COMP_EXPR: can only compare enums with == or ~=: " + _comp + "\n" + this);
				return (_comp == EQUAL) ? o1.equals(o2) : !o1.equals(o2);
			}
			
			// Convert boolean to numeric value (allows comparison of boolean with int/real)
			if (o1 instanceof Boolean)
				o1 = ((Boolean)o1 == true ? 1 : 0);
			if (o2 instanceof Boolean)
				o2 = ((Boolean)o2 == true ? 1 : 0);
			
			// Not so efficient, but should be correct
			double v1 = ((Number)o1).doubleValue();
			double v2 = ((Number)o2).doubleValue();
			
			if (_comp == NEQ) {
				return (v1 != v2) ? TRUE : FALSE;
			} else if (_comp == LESSEQ) {
				return (v1 <= v2) ? TRUE : FALSE;				
			} else if (_comp == LESS) {
				return (v1 < v2) ? TRUE : FALSE;
			} else if (_comp == GREATER) {
				return (v1 > v2) ? TRUE : FALSE;
			} else if (_comp == GREATEREQ) {
				return (v1 >= v2) ? TRUE : FALSE;
			} else if (_comp == EQUAL) {
				return (v1 == v2) ? TRUE : FALSE;
			} else
				throw new EvalException("RDDL.COMP_EXPR: Illegal comparison operator: " + _comp + "\n" + this);

		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("COMP_EXPR.getDist: Not a distribution.");
		}
	
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_e1.collectGFluents(subs, s, gfluents);
			_e2.collectGFluents(subs, s, gfluents);
		}

	}

	public static class OBJ_COMP_EXPR extends BOOL_EXPR {
		
		public static final String NEQ = "~=".intern();
		public static final String EQUAL = "==".intern(); 

		public OBJ_COMP_EXPR(LTERM t1, LTERM t2, String comp) throws Exception {
			if (!comp.equals(NEQ) && !comp.equals(EQUAL))
					throw new Exception("Unrecognized term (object) comparison: " + comp);
			_comp = comp.intern();
			_t1 = t1;
			_t2 = t2;
		}
		
		public LTERM _t1 = null;
		public LTERM _t2 = null;
		public String _comp = UNKNOWN;
		
		public String toString() {
			if (USE_PREFIX)
				return "(" + _comp + " " + _t1 + " " + _t2 + ")";
			else
				return "(" + _t1 + " " + _comp + " " + _t2 + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
						
			Object o1 = _t1.getTermSub(subs, s, r);
			Object o2 = _t2.getTermSub(subs, s, r);
						
			// Handle special case of term (object) comparison
			if (!(o1 instanceof LCONST && o2 instanceof LCONST))
				throw new EvalException("RDDL.COMP_EXPR: both values in object comparison must be object terms: " + _comp + "\n" + this);
			if (!(_comp == NEQ || _comp == EQUAL))
				throw new EvalException("RDDL.COMP_EXPR: can only compare objects with == or ~=: " + _comp + "\n" + this);
			return (_comp == EQUAL) ? o1.equals(o2) : !o1.equals(o2);
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("COMP_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}
	}

	//////////////////////////////////////////////////////////////////
	
	public static class OBJECTS_DEF {
		
		public OBJECTS_DEF(String objclass, ArrayList objects) {
			_sObjectClass = new TYPE_NAME(objclass);
			_alObjects = objects;
		}
		
		public TYPE_NAME _sObjectClass;
		public ArrayList<LCONST> _alObjects = new ArrayList<LCONST>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder(_sObjectClass + " : {");
			boolean first = true;
			for (LCONST obj : _alObjects) {
				sb.append((first ? "" : ", ") + obj);
				first = false;
			}
			sb.append("};");
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			return null;
		}
	}
	
	public static class PVAR_INST_DEF {
	
		public PVAR_INST_DEF(String predname, Object value, ArrayList terms) {
			_sPredName = new PVAR_NAME(predname);
			_oValue = value;
			_alTerms = terms;
		}
		
		public PVAR_NAME _sPredName;
		public Object _oValue;
		public ArrayList<LCONST> _alTerms = null;

		public boolean equals(Object o) {
			PVAR_INST_DEF pid = (PVAR_INST_DEF)o;
			return _sPredName.equals(pid._sPredName)
				&& _oValue.equals(pid._oValue)
				&& _alTerms.equals(pid._alTerms);
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (_oValue instanceof Boolean) {
				sb.append((((Boolean)_oValue) ? "" : "~") + _sPredName);
				if (_alTerms.size() > 0) {
					boolean first = true;
					sb.append("(");
					for (LCONST term : _alTerms) {
						sb.append((first ? "" : ", ") + term);
						first = false;
					}
					sb.append(")");
				}
				sb.append(";");
			} else {
				sb.append(_sPredName);
				if (_alTerms.size() > 0) {
					boolean first = true;
					sb.append("(");
					for (LCONST term : _alTerms) {
						sb.append((first ? "" : ", ") + term);
						first = false;
					}
					sb.append(")");
				}
				sb.append(" = " + _oValue + ";");
			}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, Random r) throws EvalException {
			return null;
		}
	}
	
	/////////////////////////////////////////////////////////

	//public static void main(String[] args) {
	//
	//}

}
