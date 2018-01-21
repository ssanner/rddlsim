/**
 * RDDL: Defines all nodes in the internal tree representation of RDDL
 *		 and simulation code for sampling from expression constructs.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl;

import java.io.File;
import java.util.*;

import org.apache.commons.math3.*;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

import rddl.parser.parser;

import util.Pair;

public class RDDL {

	public final static boolean DEBUG_EXPR_EVAL	 = false;
	public final static boolean DEBUG_PVAR_NAMES = true;
	public static TreeSet<String> PVAR_SRC_SET = new TreeSet<String>();
	
	public static boolean ASSUME_ACTION_OBSERVED = false;
	public static boolean USE_PREFIX = false;
	public static boolean SUPPRESS_OBJECT_CAST = false;

	public RDDL() { }

	// public RDDL(RDDL rddl) { 
	//	addOtherRDDL(rddl);
	// }
	
	public RDDL(String rddl_file_or_dir) {
		try {
			File f = new File(rddl_file_or_dir);
			if (f.isDirectory()) {
				for (File f2 : f.listFiles())
					if (f2.getName().endsWith(".rddl") || f2.getName().endsWith(".rddl2")) {
						System.out.println("Loading: " + f2);
						addOtherRDDL(parser.parse(f2), f2.getName().substring(0, f2.getName().lastIndexOf('.')));  
					}
			} else
				addOtherRDDL(parser.parse(f), f.getName().substring(0, f.getName().lastIndexOf('.')));
		} catch (Exception e) {
			System.out.println("ERROR: Could not instantiate RDDL for '" + rddl_file_or_dir + "'\n");// + e);
			//e.printStackTrace();
			System.exit(1);
		}
	}

	public void addDomain(DOMAIN d) {
		if (_tmDomainNodes.containsKey(d._sDomainName)) {
			System.err.println("ERROR: conflicting (duplicate) domain names: " + d._sDomainName);
			System.exit(1);
		}
		_tmDomainNodes.put(d._sDomainName, d);
	}

	public void addInstance(INSTANCE i) {
		if (_tmInstanceNodes.containsKey(i._sName)) {
			System.err.println("ERROR: conflicting (duplicate) instance names: " + i._sName);
			System.exit(1);
		}
		_tmInstanceNodes.put(i._sName, i);		
	}

	public void addNonFluents(NONFLUENTS n) {
		if (_tmNonFluentNodes.containsKey(n._sName)) {
			System.err.println("ERROR: conflicting (duplicate) nonfluent names: " + n._sName);
			System.exit(1);
		}
		_tmNonFluentNodes.put(n._sName, n);		
	}
	
	public void addOtherRDDL(RDDL rddl, String fileName) {
		Set<String> overlap_d = new TreeSet<String>(_tmDomainNodes.keySet());
		Set<String> overlap_n = new TreeSet<String>(_tmNonFluentNodes.keySet());
		Set<String> overlap_i = new TreeSet<String>(_tmInstanceNodes.keySet());
		overlap_d.retainAll(rddl._tmDomainNodes.keySet());
		overlap_n.retainAll(rddl._tmNonFluentNodes.keySet());
		overlap_i.retainAll(rddl._tmInstanceNodes.keySet());
		if (overlap_d.size() != 0) {
			System.err.println("ERROR: conflicting (duplicate) domain names: " + overlap_d);
			System.exit(1);
		}
		if (overlap_n.size() != 0) {
			System.err.println("ERROR: conflicting (duplicate) nonfluent names: " + overlap_n);
			System.exit(1);
		}
		if (overlap_i.size() != 0) {
			System.err.println("ERROR: conflicting (duplicate) instance names: " + overlap_i);
			System.exit(1);
		}
		for (DOMAIN d : rddl._tmDomainNodes.values()) {
			d.setFileName(fileName);
		}
		for (INSTANCE i : rddl._tmInstanceNodes.values()) {
			i.setFileName(fileName);
		}
		for (NONFLUENTS n : rddl._tmNonFluentNodes.values()) {
			n.setFileName(fileName);
		}
		_tmDomainNodes.putAll(rddl._tmDomainNodes);
		_tmInstanceNodes.putAll(rddl._tmInstanceNodes);	
		_tmNonFluentNodes.putAll(rddl._tmNonFluentNodes);
	}

	public String toString() {
		
		// Since fluents in prefix format will always be surrounded by parens and object names will not, I believe
		// that it will be unambiguous to always suppress the dollar sign in prefix format, so I will make $-suppression
		// the default setting for PREFIX output.
		boolean suppress_object_cast_temp = RDDL.SUPPRESS_OBJECT_CAST;
		if (USE_PREFIX) 
			RDDL.SUPPRESS_OBJECT_CAST = true;
			
		StringBuilder sb = new StringBuilder();
		for (DOMAIN d : _tmDomainNodes.values())
			sb.append(d + "\n\n");
		for (NONFLUENTS n : _tmNonFluentNodes.values())
			sb.append(n + "\n\n");
		for (INSTANCE i : _tmInstanceNodes.values())
			sb.append(i + "\n\n");
		
		if (USE_PREFIX) 
			RDDL.SUPPRESS_OBJECT_CAST = suppress_object_cast_temp;
		
		return sb.toString();
	}
	
	public boolean containsObjectFluents() {
		for (DOMAIN d : _tmDomainNodes.values()) {
			for (PVARIABLE_DEF pvdef : d._hmPVariables.values()) {
				TYPE_NAME range = pvdef._typeRange;
				TYPE_DEF tdef = d._hmTypes.get(range);
				if (tdef != null && tdef._sType.equals("object"))
					return true;
			}
		}
		return false;
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
				} else if (s.equals("preconditions")) {
					_bPreconditions = true;
				} else {
					System.err.println("Unrecognized requirement '" + s + "'.");
				}
			}
		}
		
		public void setName(String s) {
			_sDomainName = s;
		}

		public void setFileName(String s) {
			_sFileName = s;
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
					if (_hmPVariables.containsKey(pv._pvarName))
						throw new Exception("PVariable definition: '" + pv._pvarName + "' defined twice!");
					_hmPVariables.put(pv._pvarName, pv);
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
					if (_hmCPF.containsKey(d._exprVarName._pName))
						throw new Exception("CPF definition: '" + d._exprVarName._pName + "' defined twice!");
					_hmCPF.put(d._exprVarName._pName, d);
				} else if (o instanceof REWARD_DEF) {
					if (_exprReward != null)
						throw new Exception("Reward defined twice!");
					_exprReward = ((REWARD_DEF)o)._expr;
				} else if (o instanceof STATE_CONS_DEF) {
					STATE_CONS_DEF d = (STATE_CONS_DEF)o;
					_alStateConstraints.add(d._exprStateCons);
				} else if (o instanceof ACTION_PRECOND_DEF) {
					ACTION_PRECOND_DEF d = (ACTION_PRECOND_DEF)o;
					_alActionPreconditions.add(d._exprStateCons);
				} else if (o instanceof STATE_INVARIANT_DEF) {
					STATE_INVARIANT_DEF d = (STATE_INVARIANT_DEF)o;
					_alStateInvariants.add(d._exprStateCons);
				} else if (o instanceof OBJECTS_DEF) {
					OBJECTS_DEF d = (OBJECTS_DEF)o;
					_hmObjects.put(d._sObjectClass, d);
				} else {
					throw new Exception("Unrecognized definition: " + o.getClass());
				}
			}
		}
		
		public String _sDomainName = null;
		public String _sFileName = null;
		public String _sCPFHeader  = null;
		
		// WARNING: these are no longer set properly... should avoid using them until they
		// are derived from domain analysis.
		public boolean _bConcurrent = false;  // more than one non-default action 
		public boolean _bContinuous = false;  // use of real type
		public boolean _bInteger = false;     // use of int type
		public boolean _bMultivalued = false; // use of enum type
		public boolean _bIntermediateNodes = false;    // use of nodes with level > 0
		public boolean _bStateConstraints = false;     // use of state constraints
		public boolean _bCPFDeterministic = false;     // cpfs are deterministic
		public boolean _bRewardDeterministic = false;  // reward is deterministic
		public boolean _bPartiallyObserved = false;    // domain is a POMDP
		public boolean _bPreconditions = false;	 // use of action preconditions
		
		public HashMap<TYPE_NAME,TYPE_DEF>      _hmTypes      = new HashMap<TYPE_NAME,TYPE_DEF>();
		public HashMap<PVAR_NAME,PVARIABLE_DEF> _hmPVariables = new HashMap<PVAR_NAME,PVARIABLE_DEF>();
		public HashMap<PVAR_NAME,CPF_DEF>       _hmCPF        = new HashMap<PVAR_NAME,CPF_DEF>();
		public HashMap<TYPE_NAME,OBJECTS_DEF>   _hmObjects = new HashMap<TYPE_NAME,OBJECTS_DEF>();

		public EXPR _exprReward = null;

		// RDDL2: this is deprecated but we need to keep it for backward compatibility 
		public ArrayList<BOOL_EXPR> _alStateConstraints    = new ArrayList<BOOL_EXPR>();
		
		public ArrayList<BOOL_EXPR> _alActionPreconditions = new ArrayList<BOOL_EXPR>();
		public ArrayList<BOOL_EXPR> _alStateInvariants     = new ArrayList<BOOL_EXPR>();
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("domain " + _sDomainName + " {\n");
			sb.append("  requirements = {\n");
			int len = sb.length();
			sb.append((_bCPFDeterministic ? "    cpf-deterministic,\n" : "") 
					+ (_bRewardDeterministic ? "    reward-deterministic,\n" : "") 
					+ (_bConcurrent ?  "    concurrent,\n" : "") 
					+ (_bContinuous ?  "    continuous,\n" : "")
					+ (_bInteger ?     "    integer-valued,\n" : "")
					+ (_bMultivalued ? "    multivalued,\n" : "")
					+ (_bIntermediateNodes ? "    intermediate-nodes,\n" : "")
					+ (_bStateConstraints ?  "    constrained-state,\n" : "")
					+ (_bPartiallyObserved ? "    partially-observed,\n" : "")
					+ (_bPreconditions ? "    preconditions,\n" : ""));
			if (sb.length() > len) // i.e, we've added some requirements 
				sb.delete(sb.length() - 2, sb.length() - 1); // Remove last ,
			sb.append("  };\n");
			
			sb.append("  types {\n");
			for (TYPE_DEF tdef : _hmTypes.values()) {
				sb.append("    " + tdef + "\n");
			}
			sb.append("  };\n");

			if (_hmObjects.size() > 0) {
				sb.append("  objects {\n");
				for (OBJECTS_DEF obj : _hmObjects.values()) {
					sb.append("    " + obj + "\n");
				}
				sb.append("  };\n");
			}

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

			if (_alStateConstraints.size() > 0) {
				sb.append("  state-action-constraints {\n");
				for (BOOL_EXPR sc : _alStateConstraints) {
					sb.append("    " + sc + ";\n");
				}
				sb.append("  };\n");
			}

			if (_alActionPreconditions.size() > 0) {
				sb.append("  action-preconditions {\n");
				for (BOOL_EXPR sc : _alActionPreconditions) {
					sb.append("    " + sc + ";\n");
				}
				sb.append("  };\n");
			}

			if (_alStateInvariants.size() > 0) {
				sb.append("  state-invariants {\n");
				for (BOOL_EXPR sc : _alStateInvariants) {
					sb.append("    " + sc + ";\n");
				}
				sb.append("  };\n");
			}

			sb.append("}");

			return sb.toString();
		}
	}

	
	//////////////////////////////////////////////////////////
		
	public static class INSTANCE {
		
		// objects and non_fluents may be null
		public INSTANCE(String name, String domain, String nonfluents, ArrayList nonfluentsList,
						ArrayList objects, ArrayList init_state, 
						Integer nondef_actions, Object horizon, double discount) {
			
			// If max-nondef actions was not specified, assume no constraints (represented here by Integer.MAX_VALUE -- could not computationally simulate more than this)
			if (nondef_actions == null)
				nondef_actions = new Integer(Integer.MAX_VALUE);
			
			_sName     = name;
			_sDomain   = domain;
			if (nonfluentsList != null) {
				_alNonFluents = (ArrayList<PVAR_INST_DEF>)nonfluentsList;
			}
			_sNonFluents	= nonfluents;
			_nNonDefActions = nondef_actions;
			if (horizon instanceof Integer) {
				_nHorizon = (Integer)horizon;
				_termCond = null;
			} else if (horizon instanceof BOOL_EXPR) {
				_nHorizon = Integer.MAX_VALUE;
				_termCond = (BOOL_EXPR)horizon;
			} else {
				System.err.println("Horizon specification not of a recognized type:\n-integer\n-pos-inf\n-terminate-when (boolean expression)}");
				System.exit(1);
			}
			_dDiscount = discount;
			if (objects != null)
				for (OBJECTS_DEF od : (ArrayList<OBJECTS_DEF>)objects)
					_hmObjects.put(od._sObjectClass, od);
			_alInitState = (ArrayList<PVAR_INST_DEF>)init_state;
		}

		public void setFileName(String s) {
			_sFileName = s;
		}
		
		public String _sName    = null;
		public String _sFileName = null;
		public String _sDomain   = null;
		public String _sNonFluents = null;
		public int _nNonDefActions = -1;
		public int    _nHorizon  = -1;
		public BOOL_EXPR _termCond  = null;
		public double _dDiscount = 0.9d;
		
		public HashMap<TYPE_NAME,OBJECTS_DEF> _hmObjects = new HashMap<TYPE_NAME,OBJECTS_DEF>();
		public ArrayList<PVAR_INST_DEF> _alInitState = new ArrayList<PVAR_INST_DEF>();
		public ArrayList<PVAR_INST_DEF> _alNonFluents = new ArrayList<PVAR_INST_DEF>();
		
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
			
			if (_alInitState != null && _alInitState.size() > 0) {
				sb.append("  init-state {\n");
				for (PVAR_INST_DEF isd : _alInitState) {
					sb.append("    " + isd + "\n");
				}
				sb.append("  };\n");
			}
			if (!_alNonFluents.isEmpty()) {
				sb.append("	 non-fluents {\n");
				for (PVAR_INST_DEF isd : _alNonFluents) {
					sb.append("	   " + isd + "\n");
				}
				sb.append("	 };\n");
				sb.append("}");
			}
			sb.append("  max-nondef-actions = "  + (_nNonDefActions == Integer.MAX_VALUE ? "pos-inf" : _nNonDefActions) + ";\n");
			sb.append("  horizon = "  + (_termCond != null ? "terminate-when (" + _termCond + ")" : _nHorizon) + ";\n");
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

		public void setFileName(String s) {
			_sFileName = s;
		}
		
		public String _sName = null;
		public String _sFileName = null;
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
			if (!(_sType.equals("enum") || _sType.equals("object") || _sType.equals("struct")))
				throw new Exception("RDDL: Illegal type '" + type + "' in typedef");
		}
		
		public TYPE_NAME _sName;
		public String _sType;
	}
	
	public static class ENUM_TYPE_DEF extends LCONST_TYPE_DEF {
	
		public ENUM_TYPE_DEF(String name, ArrayList values) throws Exception {
			super(name, "enum");
			_alPossibleValues = values;
		}
		
		public ENUM_TYPE_DEF(String name, ENUM_VAL min, ENUM_VAL max) throws Exception {
			super(name, "enum");
			if (min._intVal == null || max._intVal == null || min._intVal > max._intVal)
				throw new Exception("Could not instantiate integer range typedef for '" + name + 
						"' for range min '" + min + "' and max '" + max + "'");
			_alPossibleValues = new ArrayList();
			for (int i = min._intVal; i <= max._intVal; i++)
				_alPossibleValues.add(new ENUM_VAL("@" + i));
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName + " : {");
			boolean first = true;
			for (Object o : _alPossibleValues) {
				ENUM_VAL s = (ENUM_VAL)o;
				sb.append((first ? "" : ", ") + s);
				first = false;
			}
			sb.append("};");
			return sb.toString();
		}
	}

	public static class STRUCT_TYPE_DEF_MEMBER {
		
		public STRUCT_TYPE_DEF_MEMBER(TYPE_NAME typename, LCONST argname) {
			_type = typename;
			_sName = argname;
		}
		
		public TYPE_NAME _type;
		public LCONST    _sName;

		public int hashCode() {
			return _type.hashCode() + _sName.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o instanceof STRUCT_TYPE_DEF_MEMBER) {
				STRUCT_TYPE_DEF_MEMBER s = (STRUCT_TYPE_DEF_MEMBER)o;
				return _sName.equals(s._sName) && _type.equals(s._type);
			} else
				return false;
		}

		public String toString() {
			return _sName + " : " + _type;
		}
	}

	public static class STRUCT_TYPE_DEF extends TYPE_DEF {
		
		public STRUCT_TYPE_DEF(String name, ArrayList<STRUCT_TYPE_DEF_MEMBER> members) throws Exception {
			super(name, "enum");
			_alMembers = members;
			_typeGeneric = null;
			_sLabelEnumOrObjectType = null;
			initMemberIndices();
		}

		public STRUCT_TYPE_DEF(String label_range, String name, String type_name) throws Exception {
			super(name, "enum");
			_sLabelEnumOrObjectType = new TYPE_NAME(label_range);
			_alMembers = null;
			_typeGeneric = new TYPE_NAME(type_name);
			// cannot initialize members until we know the object enumeration
		}

		public TYPE_NAME _sLabelEnumOrObjectType; // Struct member labels must be drawn from an enum or object type  
		public TYPE_NAME _typeGeneric = null;
		public ArrayList<STRUCT_TYPE_DEF_MEMBER> _alMembers;
		public HashMap<LCONST,Integer> _hmMemberIndex;
		
		public void initIndefiniteTypes(ArrayList<LCONST> labels) {
			_alMembers = new ArrayList<STRUCT_TYPE_DEF_MEMBER>();
			for (LCONST label : labels)
				_alMembers.add(new STRUCT_TYPE_DEF_MEMBER(_typeGeneric, label));
			initMemberIndices();
		}
		
		public void initMemberIndices() {
			_hmMemberIndex = new HashMap<LCONST,Integer>();
			for (int i = 0; i < _alMembers.size(); i++) {
				STRUCT_TYPE_DEF_MEMBER m = (STRUCT_TYPE_DEF_MEMBER)_alMembers.get(i);
				_hmMemberIndex.put(m._sName, i);
			}
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_sName + " : ");
			if (_sLabelEnumOrObjectType != null) 
				sb.append("[" + _sLabelEnumOrObjectType + "]");
			sb.append("<");
			if (_alMembers != null) {
				boolean first = true;
				for (STRUCT_TYPE_DEF_MEMBER s : _alMembers) {
					sb.append((first ? "" : ", ") + s);
					first = false;
				}
			} else
				sb.append("? : " + _typeGeneric);
			sb.append(">;");
			return sb.toString();
		}

		public int getIndex(LCONST member) {
			Integer index = _hmMemberIndex.get(member);
			return (index == null) ? -1 : index.intValue();
		}
	}

	public static class STRUCT_VAL_MEMBER {
		
		public STRUCT_VAL_MEMBER(LCONST label, Object val) {
			_sLabel = label;
			_oVal = val;
		}
		
		public LCONST _sLabel;
		public Object _oVal;
		
		public int hashCode() {
			return _sLabel.hashCode() + _oVal.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o instanceof STRUCT_VAL_MEMBER) {
				STRUCT_VAL_MEMBER s = (STRUCT_VAL_MEMBER)o;
				return _sLabel.equals(s._sLabel) && _oVal.equals(s._oVal);
			} else
				return false;
		}
		
		public String toString() {
			return _sLabel + ": " + _oVal;
		}
	}

	public static class STRUCT_VAL {

		public STRUCT_VAL()  {
			_alMembers = new ArrayList<STRUCT_VAL_MEMBER>();
		}

		public STRUCT_VAL(ArrayList<STRUCT_VAL_MEMBER> members) {
			_alMembers = members;
		}

		public STRUCT_VAL(Object o) {
			_valueGeneric = o;
			_alMembers = null;
		}

		public STRUCT_VAL(LCONST label, Object o) {
			_valueGeneric = null;
			_alMembers = new ArrayList<STRUCT_VAL_MEMBER>();
			_alMembers.add(new STRUCT_VAL_MEMBER(label, o));
		}

		public ArrayList<STRUCT_VAL_MEMBER> _alMembers;
		public Object _valueGeneric = null;
		
		public void addMember(LCONST label, Object o) {
			_alMembers.add(new STRUCT_VAL_MEMBER(label, o));
		}
		
		public void addMemberAsFirst(LCONST label, Object o) {
			_alMembers.add(0, new STRUCT_VAL_MEMBER(label, o));
		}
		
		// If initialization was < ? : 0 > then instantiate
		public void instantiate(TYPE_NAME range_type, HashMap<TYPE_NAME,TYPE_DEF> type2def, HashMap<TYPE_NAME,ArrayList<LCONST>> type2lconsts) throws Exception {
			STRUCT_TYPE_DEF range_def = (STRUCT_TYPE_DEF)type2def.get(range_type);
			
			// First instantiate this level (if needed) 
			if (_valueGeneric != null) {
				_alMembers = new ArrayList<STRUCT_VAL_MEMBER>();
				ArrayList<LCONST> constants = type2lconsts.get(range_def._sLabelEnumOrObjectType);
				if (constants == null)
					throw new Exception("Could not find enum/object list for '" + range_def._sLabelEnumOrObjectType + "'");
				for (LCONST label : constants)
					_alMembers.add(new STRUCT_VAL_MEMBER(label, _valueGeneric));
			}
			
			// Regardless of whether above was expanded, recursively instantiate any subvectors
			for (int i = 0; i < _alMembers.size(); i++) {
				STRUCT_VAL_MEMBER s = (STRUCT_VAL_MEMBER)_alMembers.get(i);
				if (s._oVal instanceof STRUCT_VAL) {
					
					// Is this a generic type or a labeled type?
					if (_valueGeneric == null) // generic
						((STRUCT_VAL)s._oVal).instantiate(range_def._typeGeneric, type2def, type2lconsts);
					else // not generic
						((STRUCT_VAL)s._oVal).instantiate(range_def._alMembers.get(i)._type, type2def, type2lconsts);
				}
			}
		}
		
		public int hashCode() {
			int hashcode = 0;
			for (STRUCT_VAL_MEMBER s : _alMembers)
				hashcode = (hashcode << 1) + s.hashCode();
			return hashcode;
		}
		
		public boolean equals(Object o) {
			if (o instanceof STRUCT_VAL) {
				STRUCT_VAL s = (STRUCT_VAL)o;
				boolean matches = _alMembers.size() == s._alMembers.size();
				for (int i = 0; matches && i < _alMembers.size(); i++)
					matches = matches && _alMembers.get(i).equals(s._alMembers.get(i));
				return matches;
			} else
				return false;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("< ");
			//sb.append("(< ");
			if (_alMembers != null) {
				boolean first = true;
				for (STRUCT_VAL_MEMBER s : _alMembers) {
					sb.append((first ? "" : ", ") + s);
					first = false;
				}
			} else
				sb.append("? : " + _valueGeneric);
			sb.append(" >");
			//sb.append(" >)");
			return sb.toString();
		}
	}

	public static abstract class LCONST_TYPE_DEF extends TYPE_DEF {
		
		public LCONST_TYPE_DEF(String name, String type) throws Exception {
			super(name, type);
		}

		public ArrayList<LCONST> _alPossibleValues;
	}	
	
	public static class OBJECT_TYPE_DEF extends LCONST_TYPE_DEF {
	
		public OBJECT_TYPE_DEF(String name) throws Exception {
			super(name, "object");
		}

		public OBJECT_TYPE_DEF(String name, String superclass) throws Exception {
			super(name, "object");
			_typeSuperclass = new TYPE_NAME(superclass);
		}
		
		public TYPE_NAME _typeSuperclass = null;
		
		public String toString() {
			if(_typeSuperclass != null) {
				return _sName + " : " + _typeSuperclass + ";";
			} else {
				return _sName + " : object;";
			}
		}
	}
	
	public abstract static class PVARIABLE_DEF {
		
		public PVARIABLE_DEF(String name, String range, ArrayList param_types) {
			_pvarName = new PVAR_NAME(name);
			_typeRange = new TYPE_NAME(range);
			_alParamTypes = new ArrayList<TYPE_NAME>();
			for (String type : (ArrayList<String>)param_types)
				_alParamTypes.add(new TYPE_NAME(type));
		}
		
		public PVAR_NAME _pvarName;
		public TYPE_NAME _typeRange;
		public ArrayList<TYPE_NAME> _alParamTypes;		
	}
	
	public abstract static class PVARIABLE_WITH_DEFAULT_DEF extends PVARIABLE_DEF {
		public PVARIABLE_WITH_DEFAULT_DEF(String name, String range, ArrayList param_types) {
			super(name, range, param_types);
		}
		public Object  _oDefValue  = null;
	}
	
	public static class PVARIABLE_STATE_DEF extends PVARIABLE_WITH_DEFAULT_DEF {
		
		public PVARIABLE_STATE_DEF(String name, boolean non_fluent, 
				String range, ArrayList param_types, Object def_value) {
			super(name, range, param_types);
			_bNonFluent = non_fluent;
			_oDefValue = def_value;
		}
		
		public boolean _bNonFluent = false;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_pvarName);
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
					  ", " + _typeRange + ", default = " +	_oDefValue + "};");
			return sb.toString();
		}

	}
	
	public static class PVARIABLE_INTERM_DEF extends PVARIABLE_DEF {
		
		public PVARIABLE_INTERM_DEF(String name, boolean derived, String range, ArrayList param_types, Integer level) {
			super(name, range, param_types);
			_bDerived = derived;
			_nLevel = level;
		}		
		
		public boolean _bDerived = false;
		public int _nLevel = -1;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_pvarName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}			
			sb.append(" : {" + (_bDerived ? "derived-fluent" : "interm-fluent") + ", " + _typeRange + 
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
			sb.append(_pvarName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}			
			sb.append(" : {observ-fluent, " + _typeRange + "};");
			return sb.toString();
		}

	}

	public static class PVARIABLE_ACTION_DEF extends PVARIABLE_WITH_DEFAULT_DEF {
		
		public PVARIABLE_ACTION_DEF(String name, String range, 
				ArrayList param_types, Object def_value) {
			super(name, range, param_types);
			_oDefValue = def_value;
			
			// TODO: If this._sRange is a struct, validator should check that default arity matches
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(_pvarName);
			if (_alParamTypes.size() > 0) {
				boolean first = true;
				sb.append("(");
				for (TYPE_NAME term : _alParamTypes) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append(")");
			}	
//			if (_alArgs.size() > 0) {
//				boolean first = true;
//				sb.append("[");
//				for (ACTION_ARG arg : _alArgs) {
//					sb.append((first ? "" : ", ") + arg);
//					first = false;
//				}
//				sb.append("]");
//			}
			sb.append(" : {action-fluent, " + _typeRange + ", default = " + _oDefValue + "};");
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
	
		public STATE_CONS_DEF(EXPR cons) {
			this((BOOL_EXPR)cons); // PARSER RESTRICTION
		}

		public STATE_CONS_DEF(BOOL_EXPR cons) {
			_exprStateCons = cons;
		}
		
		public BOOL_EXPR _exprStateCons;
		
		public String toString() {
			return _exprStateCons.toString() + ";";
		}
	}
	
	public static class ACTION_PRECOND_DEF {
	
		public ACTION_PRECOND_DEF(EXPR cons) {
			this((BOOL_EXPR)cons); // PARSER RESTRICTION
		}

		public ACTION_PRECOND_DEF(BOOL_EXPR cons) {
			_exprStateCons = cons;
		}
		
		public BOOL_EXPR _exprStateCons;
		
		public String toString() {
			return _exprStateCons.toString() + ";";
		}
	}

	public static class STATE_INVARIANT_DEF {
	
		public STATE_INVARIANT_DEF(EXPR cons) {
			this((BOOL_EXPR)cons); // PARSER RESTRICTION
		}

		public STATE_INVARIANT_DEF(BOOL_EXPR cons) {
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
		public Object getTermSub(HashMap<LVAR, LCONST> subs, State s, RandomDataGenerator r)
		throws EvalException {
			return sample(subs, s, r);
		}
	}
			
	public static class LVAR extends LTERM {
		
		public LVAR(String var_name) {
			_sVarName  = var_name.intern();
			_nHashCode = var_name.hashCode();
			_bDet = true;
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

		public Object sample(HashMap<LVAR, LCONST> subs, State s, RandomDataGenerator r)
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
	
	// Used in exists, forall, sum_over, prod_over
	public static class LTYPED_VAR extends LTERM {
		
		public LTYPED_VAR(String var_name, String type) {
			_sVarName = new LVAR(var_name);
			_sType    = new TYPE_NAME(type);
			_bDet     = true;
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

		public Object sample(HashMap<LVAR, LCONST> subs, State s, RandomDataGenerator r)
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
	
	// TVAR_EXPR: a fluent in a term expression.
	// Identical to a PVAR_EXPR except restricted to be a subclass of LTERM and to return an LCONST on sampling.
	public static class TVAR_EXPR extends LTERM {
		
		public TVAR_EXPR(PVAR_EXPR p) {
			_pvarExpr = p;
			_bDet = p._bDet;
		}
		
		public PVAR_EXPR _pvarExpr;
		
		public String toString() {
			return _pvarExpr.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			// Sample must be either an OBJECT_VAL or ENUM_VAL (both LCONST)
			LCONST result = null;
			try {
				result = (LCONST)_pvarExpr.sample(subs, s, r);
			} catch (ClassCastException e) {
				System.out.println("Could not sample from " + this);
				throw e;
			}
			return result;
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {			
			_pvarExpr.collectGFluents(subs, s, gfluents);
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			return _pvarExpr.getDist(subs, s);
		}

	}
	
	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public abstract static class LCONST extends LTERM {
		
		public LCONST(String const_value) {
			_sConstValue = const_value.intern();
			_nHashCode = const_value.hashCode();
			_bDet = true;
		}
		
		public String _sConstValue;
		public int    _nHashCode;
		
		public String toString() {
			return _sConstValue;
		}
		
		public abstract String toSuppString();
		
		// Precomputed
		public int hashCode() {
			return _nHashCode;
		}
		
		// Name was interned so can check reference equality
		public boolean equals(Object o) {
			if (!(o instanceof LCONST))
				return false;
			return _sConstValue == ((LCONST)o)._sConstValue; 
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			// Nothing to collect
		}
		
		public Object sample(HashMap<LVAR, LCONST> subs, State s, RandomDataGenerator r)
			throws EvalException {
			return this;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("LCONST.getDist: Not a distribution.");
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
		public Integer _intVal = null; 
		public ENUM_VAL(String enum_name) {
			super(enum_name);
			
			if (enum_name.charAt(0) != '@' && !enum_name.equals("default")) { // I don't recall why PVAR_EXPR.DEFAULT is an ENUM_VAL, but accept this as special case 
				System.out.println("FATAL ERROR (LANGUAGE REQUIREMENT): Enum '" + enum_name + "' currently must defined with a leading @");
				System.exit(1);
			}
			
			// Allow enums to be interpreted as ints if the part after the @ is an int
			try {
				_intVal = Integer.parseInt(enum_name.substring(1));
			} catch(NumberFormatException nfe) {
				_intVal = null;
			}
		}
		
		@Override
		public String toSuppString() {
			return toString();
		}
	}
	
	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public static class OBJECT_VAL extends LCONST {
		public OBJECT_VAL(String enum_name) {
			// Allow a $ here, but remove it if present
			super(enum_name.charAt(0) == '$' ? enum_name.substring(1) : enum_name);
		}
		
		// We have an optional $ for object references except in expressions where required
		// (optional for reasons of backward compatibility, but in RDDL2, it is recommended 
		//  to always use the $ prefix of object).
		// Unlike ENUM_VAL, the "$" is not a part of the name since it is not required.
		public String toString() {
			if (SUPPRESS_OBJECT_CAST)
				return toSuppString();
			else
				return "$" + this._sConstValue;
		}

		@Override
		public String toSuppString() {
			return this._sConstValue;
		}
	}

	// Immutable... making public to avoid unnecessary
	// method calls, relying on user to respect immutability
	public static class PVAR_NAME implements Comparable {
		
		public PVAR_NAME(String pred_name) {
			_bPrimed = pred_name.endsWith("'");
			if (_bPrimed) {
				pred_name = pred_name.substring(0, pred_name.length() - 1);
				_pvarUnprimed = new PVAR_NAME(pred_name); // minus "'"
			} else
				_pvarUnprimed = this;
			if (DEBUG_PVAR_NAMES) 
				PVAR_SRC_SET.add(pred_name);
			_sPVarName = pred_name.intern();
			_sPVarNameCanon = pred_name.replace('-','_').toLowerCase().intern();
			_nHashCode = _sPVarNameCanon.hashCode() + (_bPrimed ? 1 : 0);
		}
		
		public String _sPVarName;
		public String _sPVarNameCanon;
		public boolean _bPrimed;
		public PVAR_NAME _pvarUnprimed;
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
		
		public static final String REAL   = "[Real]".intern();
		public static final String INT    = "[Int]".intern();
		public static final String BOOL   = "[Bool]".intern();
		public static final String ENUM   = "[Enum]".intern();
		public static final String STRUCT = "[Struct]".intern();
		
		String  _sType = UNKNOWN; // real, int, bool, enum
		public boolean _bDet  = false;    // deterministic?  (if not, then stochastic)
		
		public abstract Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException;
		
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
			_bDet = expr._bDet;
		}
		
		public EXPR _exprRealValue;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(DiracDelta " + _exprRealValue + ")";
			else
				return "DiracDelta(" + _exprRealValue + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			Object o = _exprRealValue.sample(subs, s, r);
			if (!(o instanceof Double))
				throw new EvalException("RDDL: DiracDelta only applies to real-valued data.\n" + this);
			return o;
		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			Double d = ConvertToNumber(_exprRealValue.sample(subs, s, null)).doubleValue();
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
			_bDet = expr._bDet;
		}
		
		public EXPR _exprIntValue;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(KronDelta " + _exprIntValue + ")";
			else
				return "KronDelta(" + _exprIntValue + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
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
			_bDet = false;
		}
		
		public EXPR _exprLowerReal;
		public EXPR _exprUpperReal;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(Uniform " + _exprLowerReal + " " + _exprUpperReal + ")";
			else
				return "Uniform(" + _exprLowerReal + ", " + _exprUpperReal + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			try {
				double l = ((Number)_exprLowerReal.sample(subs, s, r)).doubleValue();
				double u = ((Number)_exprUpperReal.sample(subs, s, r)).doubleValue();
				if (l > u)
					throw new EvalException("RDDL: Uniform upper bound '" + 
							u + "' must be greater than lower bound '" + l + "'");
				return r.nextUniform(l,u); 
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
			_bDet = false;
		}
		
		public EXPR _normalMeanReal;
		public EXPR _normalVarReal;
		
		public String toString() {
			if (USE_PREFIX) 
				return "(Normal " + _normalMeanReal + " " + _normalVarReal + ")";
			else
				return "Normal(" + _normalMeanReal + ", " + _normalVarReal + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			try {
				double mean = ((Number)_normalMeanReal.sample(subs, s, r)).doubleValue();
				double var  = ((Number)_normalVarReal.sample(subs, s, r)).doubleValue();
				if (var < 0)
					throw new EvalException("RDDL: Normal variance '" + var +  
							"' must be greater 0");
				// x ~ N(mean,sigma^2) is equivalent to x ~ sigma*N(0,1) + mean
				double stddev = Math.sqrt(var);
				if (stddev == 0d)
					return mean;
				else
					return r.nextGaussian(mean, stddev);
			} catch (Exception e) {
				throw new EvalException("RDDL: Normal only applies to real (or castable to real) mean and positive variance.\n" + e + "\n" + this);
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

	public static class Dirichlet extends EXPR {

		// Symmetric Dirichlet with parameter alpha
		public Dirichlet(String type, EXPR alpha) {
			_sTypeName = new TYPE_NAME(type);
			_exprAlpha = alpha;
			_bDet = false;
		}
		
		public TYPE_NAME _sTypeName = null;
		public EXPR      _exprAlpha = null;
		
		public Object sample(HashMap<LVAR, LCONST> subs, State s,
				RandomDataGenerator r) throws EvalException {

			// Build a vector of size _discrete._sTypeName as a STRUCT_VAL
			LCONST_TYPE_DEF etd = (LCONST_TYPE_DEF)s._hmTypes.get(_sTypeName);
			if (etd == null)
				throw new EvalException("Could not find type for " + _sTypeName + "\nAvailable types: " + s._hmTypes.keySet());
			
			double sym_alpha = ((Number)((EXPR)_exprAlpha).sample(subs, s, r)).doubleValue();			
			DirichletHelper dh = new DirichletHelper(sym_alpha, etd._alPossibleValues.size());
			double[] sample_vec = dh.sample(r);
			
			STRUCT_VAL ret = new STRUCT_VAL();			
			
			int index = 0;
			for (LCONST label : etd._alPossibleValues)
				ret.addMember(label, sample_vec[index++]);
						
			return ret;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("Not implemented");
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprAlpha.collectGFluents(subs, s, gfluents);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX)
				sb.append("(Dirichlet " + _sTypeName + " " + _exprAlpha + " )");
			else
				sb.append("Dirichlet(" + _sTypeName + ", " + _exprAlpha + ")");
			return sb.toString();
		}
	}
	
	public static class DirichletHelper {
		public double[] _shape = null;
		
		public DirichletHelper(double sym_shape, int size) {
			_shape = new double[size];
			for (int i = 0; i < size; i++)
				_shape[i] = sym_shape;
		}
		
		public DirichletHelper(double[] asym_shape) {
			_shape = asym_shape;
		}

		// If symmetric, then shape is parameter for all samples
		public double[] sample(RandomDataGenerator r) {
			
			// Draw unnormalized Gamma samples
			double[] dir_sample_vector = new double[_shape.length];
			for (int i = 0; i < dir_sample_vector.length; i++)
				dir_sample_vector[i] = r.nextGamma(_shape[i], 1d); // randomGamma(shape): vectorized gamma given vector shape, scale = 1d

			// Normalize
			double sum = 0;
			for (int i = 0; i < dir_sample_vector.length; i++) {
				sum += dir_sample_vector[i];
			}
			for (int i = 0; i < dir_sample_vector.length; i++) {
				dir_sample_vector[i] /= sum;
			}
			
			return dir_sample_vector;
		}
	}
	
	public static class Multinomial extends EXPR {

		public Multinomial(String type, EXPR count, ArrayList probs) {
			_distDiscrete = new Discrete(type, probs);
			_exprCount = count;
			_bDet = false;
		}
		
		public Discrete _distDiscrete = null;
		public EXPR     _exprCount = null;
		
		public Object sample(HashMap<LVAR, LCONST> subs, State s,
				RandomDataGenerator r) throws EvalException {
			Object o_count = ((EXPR)_exprCount).sample(subs, s, r);
			if (!(o_count instanceof Integer))
				throw new EvalException("Expected integer for evaluation of multinomial count expression, but received " + 
						o_count.getClass() + ": " + o_count + "\n" + toString());
			int count = ((Integer)o_count).intValue();
		
			// Build a vector of size _discrete._sTypeName as a STRUCT_VAL
			LCONST_TYPE_DEF etd = (LCONST_TYPE_DEF)s._hmTypes.get(_distDiscrete._sTypeName);
			STRUCT_VAL ret = new STRUCT_VAL();			
			HashMap<LCONST,Integer> label2index = new HashMap<LCONST,Integer>();
			
			int index = 0;
			for (LCONST label : etd._alPossibleValues) {
				ret.addMember(label, new Integer(0));
				label2index.put(label, index++);
			}
			
			// Sample count times and increment correct vector element
			for (int n = 0; n < count; n++) {
				LCONST sample_label = (LCONST)_distDiscrete.sample(subs, s, r);
				int sample_index = label2index.get(sample_label);
				STRUCT_VAL_MEMBER member = ret._alMembers.get(sample_index);
				if (!member._sLabel.equals(sample_label)) 
					throw new EvalException("Multinomial: internal error... incorrectly mapped label to index in STRUCT_VAL");
				int cur_val = ((Integer)member._oVal).intValue();
				member._oVal = new Integer(cur_val + 1);
			}
			
			return ret;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("Not implemented");
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprCount.collectGFluents(subs, s, gfluents);
			_distDiscrete.collectGFluents(subs, s, gfluents);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(Multinomial " + _distDiscrete._sTypeName + " " + _exprCount + " ( ");
				for (int i = 0; i < _distDiscrete._exprProbs.size(); i+=2)
					sb.append("(" + ((ENUM_VAL)_distDiscrete._exprProbs.get(i)) + " : " + ((EXPR)_distDiscrete._exprProbs.get(i+1)) + ") ");
				sb.append(")");
			} else {
				sb.append("Multinomial(" + _distDiscrete._sTypeName + ", " + _exprCount);
				for (int i = 0; i < _distDiscrete._exprProbs.size(); i+=2)
					sb.append(", " + ((ENUM_VAL)_distDiscrete._exprProbs.get(i)) + " : " + ((EXPR)_distDiscrete._exprProbs.get(i+1)));
			}
			sb.append(")");
			return sb.toString();
		}
	}

	public static class Discrete extends EXPR {
		
		public final static REAL_CONST_EXPR OTHERWISE_CASE = new REAL_CONST_EXPR(-1d);

		// TODO: probs storage as alternating label and expression is sloppy, should make an array of (label,prob) pairs
		public Discrete(String type, ArrayList probs) {
			_bDet = false;
			if (type != null)
				_sTypeName = new TYPE_NAME(type);
			_exprProbs = (ArrayList<EXPR>)probs;
			
			// Check last case for "otherwise" and build expression if necessary
			int last_index = _exprProbs.size() - 1;
			//System.out.println(_exprProbs.get(last_index) + " == " + OTHERWISE_CASE + ": " + _exprProbs.get(last_index).equals(OTHERWISE_CASE));
			if (_exprProbs.get(last_index).equals(OTHERWISE_CASE)) {
				EXPR otherwise_prob = new REAL_CONST_EXPR(1d);
				for (int i = 0; i < _exprProbs.size() - 2; i+=2) {
					EXPR case_prob  = _exprProbs.get(i+1); 
					try {
						otherwise_prob = new OPER_EXPR(otherwise_prob, case_prob, "-");
					} catch (Exception e) { // Fatal error
						e.printStackTrace();
						System.err.println(e);
						System.exit(1);
					}
				}
				
				_exprProbs.set(last_index, otherwise_prob);
			}
		}
		
		public TYPE_NAME       _sTypeName = null;
		public ArrayList<EXPR> _exprProbs = null; // At runtime, check these sum to 1
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(Discrete " + (_sTypeName != null ? _sTypeName : "") + " ( ");
				for (int i = 0; i < _exprProbs.size(); i+=2) {
                                    if (_exprProbs.get(i) instanceof ENUM_VAL)
					sb.append("(" + ((ENUM_VAL)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)) + ") ");
                                    else if (_exprProbs.get(i) instanceof OBJECT_VAL)
                                        sb.append("(" + ((OBJECT_VAL)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)) + ") ");
                                    else
                                        sb.append("(" + ((EXPR)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)) + ") ");
                                }
				sb.append(")");
			} else {
				sb.append("Discrete(" + (_sTypeName != null ? _sTypeName + ", ": ""));
				for (int i = 0; i < _exprProbs.size(); i+=2) {
                                    if (_exprProbs.get(i) instanceof ENUM_VAL)
					sb.append(((i > 0) ? ", " : "") + ((ENUM_VAL)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)));
                                    else if (_exprProbs.get(i) instanceof OBJECT_VAL)
                                        sb.append(((i > 0) ? ", " : "") + ((OBJECT_VAL)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)));
                                    else
                                        sb.append(((i > 0) ? ", " : "") + ((EXPR)_exprProbs.get(i)) + " : " + ((EXPR)_exprProbs.get(i+1)));
                                }
			}
			sb.append(")");
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			try {
				LCONST_TYPE_DEF etd = null;
				HashSet<LCONST> value_set = null;
				
				if (_sTypeName != null) {
					etd = (LCONST_TYPE_DEF)s._hmTypes.get(_sTypeName);
					value_set = new HashSet<LCONST>(etd._alPossibleValues);
				}
				
				ArrayList<LCONST> lconst_label = new ArrayList<LCONST>();
				ArrayList<Double> lconst_probs = new ArrayList<Double>();
				double total = 0d;
				for (int i = 0; i < _exprProbs.size(); i+=2) {
					LCONST case_label = (LCONST)_exprProbs.get(i);
					double case_prob  = ((Number)((EXPR)_exprProbs.get(i+1)).sample(subs, s, r)).doubleValue(); 
					
					lconst_label.add(case_label);
					lconst_probs.add(case_prob);
					
					total += case_prob;
					if (_sTypeName != null && !value_set.contains(case_label))
						throw new EvalException("'" + case_label + "' not found in " + etd._alPossibleValues + " for Discrete(" + _sTypeName + ", ... )");
				}
				//System.out.println(lconst_probs);
				if (Math.abs(1.0 - total) > 1.0e-6)
					throw new EvalException("Discrete probabilities did not sum to 1.0: " + total + " : " + lconst_probs);

				double rand = r.nextUniform(0d,1d);
				for (int i = 0; i < lconst_probs.size(); i++) {
					rand -= lconst_probs.get(i);
					if (rand < 0)
						return lconst_label.get(i);
				}
				throw new EvalException("Sampling error, failed to return value: " + lconst_probs);

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
	
	public static class Exponential extends EXPR {
		
		public Exponential(EXPR mean) {
			_exprMeanReal = mean;
			_bDet = false;
		}
		
		public EXPR _exprMeanReal;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Exponential " + _exprMeanReal + ")";
			else
				return "Exponential(" + _exprMeanReal + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			double mean = ((Number)_exprMeanReal.sample(subs, s, null)).doubleValue();
			return r.nextExponential(mean); 
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double mean = ((Number)_exprMeanReal.sample(subs, s, null)).doubleValue();
			return new Exponential(new REAL_CONST_EXPR(mean));
		}
		
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprMeanReal.collectGFluents(subs, s, gfluents);
		}
	
	}

	public static class Weibull extends EXPR {
		
		public Weibull(EXPR shape, EXPR scale) {
			_exprShape = shape;
			_exprScale = scale;
			_bDet = false;
		}
		
		public EXPR _exprShape;
		public EXPR _exprScale;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Weibull " + _exprShape + " " + _exprScale + ")";
			else
				return "Weibull(" + _exprShape + ", " + _exprScale + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			double shape = ((Number)_exprShape.sample(subs, s, null)).doubleValue();
			double scale = ((Number)_exprScale.sample(subs, s, null)).doubleValue();
			return r.nextWeibull(shape, scale);
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double shape = ((Number)_exprShape.sample(subs, s, null)).doubleValue();
			double scale = ((Number)_exprScale.sample(subs, s, null)).doubleValue();
			return new Weibull(new REAL_CONST_EXPR(shape), new REAL_CONST_EXPR(scale));
		}
	
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprShape.collectGFluents(subs, s, gfluents);
			_exprScale.collectGFluents(subs, s, gfluents);
		}

	}

	public static class Gamma extends EXPR {
		
		public Gamma(EXPR shape, EXPR scale) {
			_exprShape = shape;
			_exprScale = scale;
			_bDet = false;
		}
		
		public EXPR _exprShape;
		public EXPR _exprScale;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Gamma " + _exprShape + " " + _exprScale + ")";
			else
				return "Gamma(" + _exprShape + ", " + _exprScale + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			double shape = ((Number)_exprShape.sample(subs, s, null)).doubleValue();
			double scale = ((Number)_exprScale.sample(subs, s, null)).doubleValue();
			return r.nextGamma(shape, scale);
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double shape = ((Number)_exprShape.sample(subs, s, null)).doubleValue();
			double scale = ((Number)_exprScale.sample(subs, s, null)).doubleValue();
			return new Gamma(new REAL_CONST_EXPR(shape), new REAL_CONST_EXPR(scale));
		}
	
		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprShape.collectGFluents(subs, s, gfluents);
			_exprScale.collectGFluents(subs, s, gfluents);
		}

	}
	
	public static class Poisson extends EXPR {
		
		public Poisson(EXPR mean) {
			_exprMean = mean;
			_bDet = false;
		}
		
		public EXPR _exprMean;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Poisson " + _exprMean + ")";
			else
				return "Poisson(" + _exprMean + ")";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			double mean = ((Number)_exprMean.sample(subs, s, null)).doubleValue();
			return ((Long)r.nextPoisson(mean)).intValue();
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			double mean = ((Number)_exprMean.sample(subs, s, null)).doubleValue();
			return new Poisson(new REAL_CONST_EXPR(mean));
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_exprMean.collectGFluents(subs, s, gfluents);
		}
	}
	
	public static class Bernoulli extends BOOL_EXPR {
		
		public Bernoulli(EXPR prob) {
			_exprProb = prob;
			_bDet = false;
		}
		
		public EXPR _exprProb;
		
		public String toString() {
			if (USE_PREFIX)
				return "(Bernoulli " + _exprProb + ")";
			else
				return "Bernoulli(" + _exprProb + ")";
		}
		
		// Note: Dirichlet produces a vector
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			double prob = ((Number)_exprProb.sample(subs, s, r)).doubleValue();
			if (prob < 0.0d || prob > 1.0d)
				throw new EvalException("RDDL: Bernoulli prob " + prob + " not in [0,1]\n" + _exprProb);
			return r.nextUniform(0d,1d) < prob; // Bernoulli parameter is prob of being true
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
			_bDet = true;
		}
		
		public Integer _nValue;
		
		public String toString() {
			return _nValue.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
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
			_bDet = true;
		}
		
		public Double _dValue;
		
		public String toString() {
			return _dValue.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
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
	
	public static class STRUCT_EXPR_MEMBER {
		
		public STRUCT_EXPR_MEMBER(LCONST label, EXPR expr) {
			_sLabel = label;
			_expr   = expr;
		}
		
		public LCONST _sLabel;
		public EXPR   _expr;
		
		public int hashCode() {
			return _sLabel.hashCode() + _expr.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o instanceof STRUCT_EXPR_MEMBER) {
				STRUCT_EXPR_MEMBER s = (STRUCT_EXPR_MEMBER)o;
				return _sLabel.equals(s._sLabel) && _expr.equals(s._expr);
			} else
				return false;
		}
		
		public String toString() {
			return _sLabel + ": " + _expr;
		}
	}
	
	public static class STRUCT_EXPR extends EXPR {
		
		public STRUCT_EXPR() {
			_sType     = STRUCT;
			_alSubExpr = new ArrayList<STRUCT_EXPR_MEMBER>();
			_bDet = true;
		}

		public STRUCT_EXPR(ArrayList sub_expr) {
			_sType     = STRUCT;
			_alSubExpr = (ArrayList<STRUCT_EXPR_MEMBER>)sub_expr;
			_bDet = true;
			for (STRUCT_EXPR_MEMBER m : _alSubExpr)
				_bDet = _bDet && m._expr._bDet; // anything is false will make this false
		}

		public STRUCT_EXPR(LCONST label, EXPR expr) {
			_sType     = STRUCT;
			_alSubExpr = new ArrayList<STRUCT_EXPR_MEMBER>();
			_alSubExpr.add(new STRUCT_EXPR_MEMBER(label, expr));
			_bDet = expr._bDet;
		}

		public ArrayList<STRUCT_EXPR_MEMBER> _alSubExpr;

		public void addMember(LCONST label, EXPR expr) {
			_alSubExpr.add(new STRUCT_EXPR_MEMBER(label, expr));
			_bDet = _bDet && expr._bDet;
		}

		public void addMemberAsFirst(LCONST label, EXPR expr) {
			_alSubExpr.add(0, new STRUCT_EXPR_MEMBER(label, expr));
			_bDet = _bDet && expr._bDet;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder("(< ");
			boolean first = true;
			for (STRUCT_EXPR_MEMBER m : _alSubExpr) {
				if (!first)
					sb.append(", ");
				first = false;
				sb.append("\n      " + m.toString());
			}
			sb.append("\n      >)");
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			STRUCT_VAL ret = new STRUCT_VAL();
			for (STRUCT_EXPR_MEMBER m : _alSubExpr)
				ret.addMember(m._sLabel, m._expr.sample(subs, s, r));
			return ret;
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			STRUCT_EXPR ret = new STRUCT_EXPR();
			for (STRUCT_EXPR_MEMBER m : _alSubExpr)
				ret.addMember(m._sLabel, m._expr.getDist(subs, s));
			return ret;
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			for (STRUCT_EXPR_MEMBER m : _alSubExpr)
				m._expr.collectGFluents(subs, s, gfluents);
		}
	
	}
	
	public static class OPER_EXPR extends EXPR {
	
		public static final String PLUS  = "+".intern();
		public static final String MINUS = "-".intern();
		public static final String TIMES = "*".intern();
		public static final String DIV   = "/".intern();
		public static final String MIN   = "min".intern();
		public static final String MAX   = "max".intern();
		
		public static final Double   D_ZERO = Double.valueOf(0d);
		public static final Integer  I_ZERO = Integer.valueOf(0);
		public static final Boolean  B_ZERO = Boolean.valueOf(false);
		public static final ENUM_VAL E_ZERO = new ENUM_VAL("@0");
		
		public OPER_EXPR(EXPR e1, EXPR e2, String op) throws Exception {
			if (!op.equals(PLUS) && !op.equals(MINUS) && !op.equals(TIMES) && !op.equals(DIV))
				throw new Exception("Unrecognized arithmetic operator: " + op);
			_op = op.intern();
			_e1 = e1;
			_e2 = e2;
			_bDet = e1._bDet && e2._bDet;
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
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			
			// First check for early termination in case of TIMES and 0 -- needed to facilitate collectGfluents 
			if (_op == OPER_EXPR.TIMES) {
				
				Object o1 = null;
				try { // FIXME: using Exceptions here for standard control-flow, should allow sample to return null
					o1 = _e1.sample(subs, s, r);
				} catch (Exception e) { /* ignore here */ }
				if (o1 != null && (o1.equals(OPER_EXPR.D_ZERO) || o1.equals(OPER_EXPR.I_ZERO) || o1.equals(OPER_EXPR.B_ZERO) || o1.equals(OPER_EXPR.E_ZERO))) 
					return o1;
				
				Object o2 = null;
				try { // FIXME: using Exceptions here for standard control-flow, should allow sample to return null
					o2 = _e2.sample(subs, s, r);
				} catch (Exception e) { /* ignore here */ }
				if (o2 != null && (o2.equals(OPER_EXPR.D_ZERO) || o2.equals(OPER_EXPR.I_ZERO) || o2.equals(OPER_EXPR.B_ZERO) || o2.equals(OPER_EXPR.E_ZERO))) 
					return o2;
			}
			
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

			HashSet<Pair> local_fluents = new HashSet<Pair>();
			
			Object e1_eval = null;
			local_fluents.clear();
			_e1.collectGFluents(subs, s, local_fluents);
			boolean e1_is_indep_of_state = local_fluents.size() == 0;
			if (e1_is_indep_of_state && _e1._bDet)
				e1_eval = _e1.sample(subs, s, null);

			Object e2_eval = null;
			local_fluents.clear();
			_e2.collectGFluents(subs, s, local_fluents);
			boolean e2_is_indep_of_state = local_fluents.size() == 0;
			if (e2_is_indep_of_state && _e2._bDet)
				e2_eval = _e2.sample(subs, s, null);
			
			if (_op == OPER_EXPR.TIMES && 
					((e1_eval != null && (e1_eval.equals(D_ZERO) || e1_eval.equals(I_ZERO) || e1_eval.equals(B_ZERO) || e1_eval.equals(E_ZERO))) ||
					 (e2_eval != null && (e2_eval.equals(D_ZERO) || e2_eval.equals(I_ZERO) || e2_eval.equals(B_ZERO) || e2_eval.equals(E_ZERO)))))
				return; // We have a state-independent 0 times some value... the result must be 0 so we need not collect fluents
				
			if (e1_eval == null)
				_e1.collectGFluents(subs, s, gfluents);
			if (e2_eval == null)
				_e2.collectGFluents(subs, s, gfluents);
		}

	}

	public static Number ConvertToNumber(Object o) throws EvalException {
		if (o instanceof Boolean)
			return ((Boolean)o == true ? 1 : 0);
		else if (o instanceof Integer || o instanceof Double)
			return (Number)o;
		else
			throw new EvalException("RDDL.ConvertToNumber: Unrecognized number class: " + o.getClass());
	}
	
	public static Object ComputeArithmeticResult(Object o1, Object o2, String op) throws EvalException {

		// Recursive case for vectors
		if (o1 instanceof STRUCT_VAL && o2 instanceof STRUCT_VAL) {
			STRUCT_VAL s1 = (STRUCT_VAL)o1;
			STRUCT_VAL s2 = (STRUCT_VAL)o2;
			if (s1._alMembers.size() != s2._alMembers.size())
				throw new EvalException("Cannot perform elementwise vector operation on vectors of different lengths." + 
						"\nOperand 1: " + s1 + "\nOperand 2: " + s2 + "\nOp: " + op);

			STRUCT_VAL ret = new STRUCT_VAL();
			for (int i = 0; i < s1._alMembers.size(); i++) {
				STRUCT_VAL_MEMBER v1 = s1._alMembers.get(i);
				STRUCT_VAL_MEMBER v2 = s2._alMembers.get(i);
				if (!v1._sLabel.equals(v2._sLabel))
					throw new EvalException("Mismatched vector labels during elementwise vector operation: " + v1 + " vs " + v2 + " in" +
						"\nOperand 1: " + s1 + "\nOperand 2: " + s2 + "\nOp: " + op);
				ret.addMember(v1._sLabel, ComputeArithmeticResult(v1._oVal, v2._oVal, op));
			}
			return ret;
		}
		
		// Base cases...
		
		// If the enum is an int type, cast it to an int
		if (o1 instanceof ENUM_VAL && ((ENUM_VAL)o1)._intVal != null) {
			o1 = ((ENUM_VAL)o1)._intVal;
		}
		if (o2 instanceof ENUM_VAL && ((ENUM_VAL)o2)._intVal != null) {
			o2 = ((ENUM_VAL)o2)._intVal;
		}
		
		// First handle casting into compatible types (not that for now everything becomes a double -- could check for ints)
		if (o1 instanceof Boolean)
			o1 = ((Boolean)o1 == true ? 1 : 0);
		if (o2 instanceof Boolean)
			o2 = ((Boolean)o2 == true ? 1 : 0);
		if (!((o1 instanceof Integer) || (o1 instanceof Double))
			|| !((o2 instanceof Integer) || (o2 instanceof Double)))
			throw new EvalException("Operands 1 '" + o1 + "' (type:" + o1.getClass() + ") and 2 '" + o2 + "' (type:" + o2.getClass() + ") must be castable to int or real");
		
		// Perform int operations where possible
		if (o1 instanceof Integer && o2 instanceof Integer && op != OPER_EXPR.DIV) {
			if (op == OPER_EXPR.PLUS)
				return new Integer((Integer)o1 + (Integer)o2);
			else if (op == OPER_EXPR.MINUS)
				return new Integer((Integer)o1 - (Integer)o2);
			else if (op == OPER_EXPR.TIMES)
				return new Integer((Integer)o1 * (Integer)o2);
			else if (op == OPER_EXPR.MIN)
				return new Integer(Math.min((Integer)o1, (Integer)o2));
			else if (op == OPER_EXPR.MAX)
				return new Integer(Math.max((Integer)o1, (Integer)o2));
			else
				throw new EvalException("RDDL.OperExpr: Unrecognized operation: " + op);
		}
		
		// Now perform simple arithmetic operations
		if (op == OPER_EXPR.PLUS)
			return new Double(((Number)o1).doubleValue() + ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.MINUS)
			return new Double(((Number)o1).doubleValue() - ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.TIMES)
			return new Double(((Number)o1).doubleValue() * ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.DIV)
			return new Double(((Number)o1).doubleValue() / ((Number)o2).doubleValue());
		else if (op == OPER_EXPR.MIN)
			return new Double(Math.min(((Number)o1).doubleValue(), ((Number)o2).doubleValue()));
		else if (op == OPER_EXPR.MAX)
			return new Double(Math.max(((Number)o1).doubleValue(), ((Number)o2).doubleValue()));
		else
			throw new EvalException("RDDL.OperExpr: Unrecognized operation: " + op + " for " + o1 + " and " + o2); 
	}
	
	public static class AGG_EXPR extends EXPR {
	
		public static final String SUM  = "sum".intern();
		public static final String PROD = "prod".intern();
		public static final String MIN  = "min".intern();
		public static final String MAX  = "max".intern();
		
		public AGG_EXPR(String op, ArrayList<LTYPED_VAR> vars, EXPR e) throws Exception {
			if (!op.equals(SUM) && !op.equals(PROD) && !op.equals(MIN) && !op.equals(MAX))
				throw new Exception("Unrecognized aggregation operator: " + op);
			_op = op.intern();
			_alVariables = (ArrayList<LTYPED_VAR>)vars;
			_e  = e;
			_bDet = e._bDet;
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
				sb.append("(" + _op);
				boolean first = true;
				sb.append("_{");
				for (LTYPED_VAR term : _alVariables) {
					sb.append((first ? "" : ", ") + term);
					first = false;
				}
				sb.append("} " + _e + ")");
			}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {

			ArrayList<ArrayList<LCONST>> possible_subs = s.generateAtoms(_alVariables);
			Object result = null;
			
			// Check for a PROD with 0 even if all subexpressions not evaluable -- important for CollectGfluents
			if (_op == PROD) {
				
				HashSet<Pair> local_fluents = new HashSet<Pair>();
				
				for (ArrayList<LCONST> sub_inst : possible_subs) {
					for (int i = 0; i < _alVariables.size(); i++) {
						subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
					}
					
					local_fluents.clear();

					if (_e._bDet) { // (s.getPVariableType(p._pName) == State.NONFLUENT) {
						Object eval = null;
						try { // FIXME: using Exceptions here for standard control-flow, should allow sample to return null
							eval = _e.sample(subs, s, null);
						} catch (Exception e) { /* could not sample, ignore here */ }
						if (eval != null && (eval.equals(OPER_EXPR.D_ZERO) || eval.equals(OPER_EXPR.I_ZERO) || eval.equals(OPER_EXPR.B_ZERO) || eval.equals(OPER_EXPR.E_ZERO))) {

							// Clear all substitutions before early return
							for (int i = 0; i < _alVariables.size(); i++) {
								subs.remove(_alVariables.get(i)._sVarName);
							}

							return eval; // We can determine the result of this PROD due to a deterministic 0 value independent of state
						}
					}
				}
			}			
			
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
				else if (_op == SUM)
					result = ComputeArithmeticResult(result, interm_result, OPER_EXPR.PLUS);
				else if (_op == PROD)
					result = ComputeArithmeticResult(result, interm_result, OPER_EXPR.TIMES);
				else // op == MIN || op == MAX
					result = ComputeArithmeticResult(result, interm_result, _op); // same String
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

			// Check for early termination on PROD
			if (_op == PROD) {
				
				HashSet<Pair> local_fluents = new HashSet<Pair>();
				
				for (ArrayList<LCONST> sub_inst : possible_subs) {
					for (int i = 0; i < _alVariables.size(); i++) {
						subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
					}
					
					local_fluents.clear();
					_e.collectGFluents(subs, s, local_fluents);
					boolean expr_is_indep_of_state = local_fluents.size() == 0;

					if (expr_is_indep_of_state && _e._bDet) { // (s.getPVariableType(p._pName) == State.NONFLUENT) {
						Object eval = _e.sample(subs, s, null);
						if (eval.equals(OPER_EXPR.D_ZERO) || eval.equals(OPER_EXPR.I_ZERO) || eval.equals(OPER_EXPR.B_ZERO) || eval.equals(OPER_EXPR.E_ZERO)) {

							// Clear all substitutions before early return
							for (int i = 0; i < _alVariables.size(); i++) {
								subs.remove(_alVariables.get(i)._sVarName);
							}

							return; // We can determine the result of this PROD due to a deterministic 0 value independent of state
						}
					}
				}
			}			
			
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
	
	// TODO: Need a cleaner way to ensure that only boolean pvars go under forall, exists
	// NOTE: technically a PVAR_EXPR does not have to be a boolean expression (it
	// could be int/real), but at parse time we don't know so we just put it
	// under BOOL_EXPR which is a subclass of EXPR.
	public static class PVAR_EXPR extends BOOL_EXPR {

		public final static LCONST DEFAULT = new ENUM_VAL("default");

		public PVAR_EXPR(String s, ArrayList terms) {
			this(s, terms, null);
		}

		public PVAR_EXPR(String s, ArrayList terms, ArrayList members) {
			_bDet = true;
			_pName = new PVAR_NAME(s);
			_alTerms = new ArrayList<LTERM>();
			for (Object o : terms) {
				if (o instanceof LTERM)
					_alTerms.add((LTERM)o);
				else if (o instanceof PVAR_EXPR) 
					_alTerms.add(new TVAR_EXPR((PVAR_EXPR)o));
				else {
					System.err.println(_pName + " argument must be an enum/object type, but " + o + " is not.");
					System.exit(1);
				}
			}
			if (members != null) {
				_alMembers = new ArrayList<LTERM>();
				for (Object o : members)
					_alMembers.add((LTERM)o);
			}
		}
		
		public PVAR_NAME _pName;
		public ArrayList<LTERM>  _alTerms  = null;
		public ArrayList<LCONST> _subTerms = new ArrayList<LCONST>(); // Used for evaluation
		public ArrayList<LTERM>  _alMembers = null;
		public ArrayList<LCONST> _subMembers = new ArrayList<LCONST>(); // Used for evaluation
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) 
				sb.append("(");
			sb.append(_pName);
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
			if (_alMembers != null)
				for (LTERM member : _alMembers) {
					if (member instanceof TVAR_EXPR)
						sb.append(".[" + member + "]");
					else
						sb.append("." + member);
				}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			
			// Special case for using a pvariable name with ".default"
			if (_alMembers != null && (_alMembers.size() == 1) && (_alMembers.get(0) instanceof LCONST) && (_alMembers.get(0).equals(DEFAULT))) {
				PVARIABLE_DEF pvar_def = s._hmPVariables.get(_pName._pvarUnprimed /*unprimed version to look up range*/);
				if (pvar_def instanceof PVARIABLE_WITH_DEFAULT_DEF) {
					return ((PVARIABLE_WITH_DEFAULT_DEF)pvar_def)._oDefValue;
				} else {
					throw new EvalException("No '.default' value associated with " + _pName);
				}
			}
			
			// Build parameter list based on pvar definition, substitutions for vars, and evaluation of object expressions
			sampleLTerms(_subTerms, _alTerms, subs, s, r);
			
			Object ret_val = s.getPVariableAssign(_pName, _subTerms);
			if (ret_val == null)
				throw new EvalException("RDDL.PVAR_EXPR: No value assigned to pvariable '" + 
						_pName + _subTerms + "'" + (_subTerms.size() == 0 ? "\n... did you intend an enum value @" + _pName+ " or object value $" + _pName + "?" : "") + "");
			
			if (_alMembers != null) {
				
				// Get the vector index of 'member' by looking it up in the type_def for the range value of this pvar
				PVARIABLE_DEF pvar_def = s._hmPVariables.get(_pName._pvarUnprimed /*unprimed version to look up range*/);
				if (pvar_def == null) {
					System.err.println("RDDL.PVAR_EXPR: expected a type of '" + _pName._pvarUnprimed + "' but got " + pvar_def);
					System.err.println(s._hmPVariables.keySet());
				}
				TYPE_NAME range_type = pvar_def._typeRange;
		
				// Instantiate all members
				sampleLTerms(_subMembers, _alMembers, subs, s, r);
				
				// Dereference in the order they occur
				for (LCONST member : _subMembers) { 
					
					if (!(ret_val instanceof STRUCT_VAL))
						throw new EvalException("RDDL.PVAR_EXPR: expected a vector type to dereference '" + member + "' but got " + ret_val);
					
					// The current evaluation is a STRUCT_VAL, the range type for this structure is a STRUCT_TYPE_DEF, and member should index both
					STRUCT_VAL sval = (STRUCT_VAL)ret_val;
					STRUCT_TYPE_DEF range_struct_def = (STRUCT_TYPE_DEF)s._hmTypes.get(range_type);
					
					int index = range_struct_def.getIndex(member);
									
					if (index < 0)
						throw new EvalException("\nRDDL.PVAR_EXPR: could not find member '" + member + "' for '" + range_type + "'");
					if (!sval._alMembers.get(index)._sLabel.equals(member)) // Strings were interned so == possible
						throw new EvalException("\nRDDL.PVAR_EXPR: mismatch for ordering of '" + range_type + "', expected label '" + 
								sval._alMembers.get(index)._sLabel + "', but found label '" + member + "'." +
								"\n- The type definition is " + range_struct_def + " while the current vector being indexed is: " + sval + "." + 
								"\n- This error can result from a previous assignment in an < ... > expression that used an incorrect label ordering.");
					
					// Subselect the member from ret_val for return or further derefs
					ret_val = sval._alMembers.get(index)._oVal;
					
					// Update the range_type for ret_val (in case more derefs needed)
					range_type = range_struct_def._alMembers.get(index)._type; 
				}
			}
			
			return ret_val;
		}
		
		public void sampleLTerms(ArrayList<LCONST> ret_val, ArrayList<LTERM> to_sample, 
				HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			
			ret_val.clear();
			for (int i = 0; i < to_sample.size(); i++) {
				LTERM t = to_sample.get(i);
				if (t instanceof LCONST)
					ret_val.add((LCONST)t);
				else if (t instanceof LVAR) {
					LCONST sub = subs.get((LVAR)t);
					if (sub == null)
						throw new EvalException("RDDL.PVAR_EXPR: No substitution in " + subs + " for " + t + "\n" + this);
					ret_val.add(sub);
				} else if (t instanceof TVAR_EXPR) {
					// Here is where nested expressions are evaluated
					TVAR_EXPR tvar = (TVAR_EXPR)t;
					ret_val.add((LCONST)tvar.sample(subs, s, r));
				} else
					throw new EvalException("RDDL.PVAR_EXPR: Unrecognized term " + t + "\n" + this);
			}				
		}
		
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("PVAR_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			
			// Skip non-fluents
			PVARIABLE_DEF pvar_def = s._hmPVariables.get(_pName);
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
				} else if (t instanceof TVAR_EXPR) {
					TVAR_EXPR tvar = (TVAR_EXPR)t;
					tvar.collectGFluents(subs, s, gfluents);
					_subTerms.add((LCONST)tvar.sample(subs, s, null));
				} else
					throw new EvalException("RDDL.PVAR_EXPR: Unrecognized term " + t + "\n" + this);
			}
			
			gfluents.add(new Pair(_pName, _subTerms.clone()));
		}

	}

	public static class FUN_EXPR extends EXPR {

		// Integer functions -- two args
		public static final String DIV  = "div".intern();
		public static final String MOD  = "mod".intern();
		
		// Potentially integer functions -- two args
		public static final String MIN  = "min".intern();
		public static final String MAX  = "max".intern(); 

		// Potentiall integer functions -- single arg
		public static final String ABS  = "abs".intern();
		public static final String SGN  = "sgn".intern();
		public static final String ROUND = "round".intern();
		public static final String FLOOR = "floor".intern();
		public static final String CEIL  = "ceil".intern();

		// Real-valued functions -- two args
		public static final String POW  = "pow".intern();
		public static final String LOG  = "log".intern();

		// Real-valued functions -- single arg
		public static final String COS  = "cos".intern();
		public static final String SIN  = "sin".intern();
		public static final String TAN  = "tan".intern();
		public static final String ACOS = "acos".intern();
		public static final String ASIN = "asin".intern();
		public static final String ATAN = "atan".intern();
		public static final String COSH = "cosh".intern();
		public static final String SINH = "sinh".intern();
		public static final String TANH = "tanh".intern();
		public static final String EXP  = "exp".intern();
		public static final String LN   = "ln".intern();
		public static final String SQRT = "sqrt".intern();
		
		public static TreeSet<String> KNOWN_FUNCTIONS = new TreeSet<String>(
				Arrays.asList(new String[] {DIV, MOD, MIN, MAX, ABS, SGN, ROUND, FLOOR, CEIL, POW, LOG, COS, SIN, TAN, ACOS, ASIN, ATAN, COSH, SINH, TANH, EXP, LN, SQRT}));
		
		public FUN_EXPR(String s, ArrayList expressions) {
			
			_bDet = true;
			_sName = s.intern();
			_alArgs = new ArrayList<EXPR>();
			for (Object o : expressions) {
				if (o instanceof EXPR) {
					_alArgs.add((EXPR)o);
					_bDet = _bDet && ((EXPR)o)._bDet;
				} else {
					System.err.println(_sName + " argument must be an EXPR type, but " + o + " is not.");
					System.exit(1);
				}
			}
		}
		
		public String _sName;
		public ArrayList<EXPR>   _alArgs  = null;
		public ArrayList<Object> _alArgEval = new ArrayList<Object>(); // Used for evaluation
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) 
				sb.append("(");
			sb.append(_sName);
			if (_alArgs.size() > 0) {
				boolean first = true;
				if (!USE_PREFIX)
					sb.append("[");
				for (EXPR e : _alArgs) {
					if (USE_PREFIX)
						sb.append(" " + e);
					else
						sb.append((first ? "" : ", ") + e);
					first = false;
				}
			}
			
			if (USE_PREFIX) 
				sb.append(")");
			else
				sb.append("]");
			
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {		
			
			if (!KNOWN_FUNCTIONS.contains(_sName))
				throw new EvalException("Special function '" + _sName + "' is undefined, possible choices are\n" + KNOWN_FUNCTIONS); 

			_alArgEval.clear();

			// Sample all arguments
			for (EXPR e : _alArgs)
				_alArgEval.add(e.sample(subs, s, r));
			
			Object o1 = _alArgEval.get(0);
			Object o2 = _alArgEval.size() < 2 ? null : _alArgEval.get(1);
			
			// DIV, MOD: Integer functions -- two args
			if (_sName == DIV || _sName == MOD) {
				if (_alArgEval.size() != 2 || !(o1 instanceof Integer) || !(o2 instanceof Integer))
					throw new EvalException("Two operands of " + _sName + " must be integer, does not hold for: " + _alArgEval);
				if (_sName == DIV) 
					return ((Integer)o1) / ((Integer)o2);
				else // MOD
					return ((Integer)o1) % ((Integer)o2);
			}

			// MAX, MIN: Potentially integer functions -- two args
			if (_sName == MAX || _sName == MIN) {
				if (_alArgEval.size() != 2)
					throw new EvalException("Operands of " + _sName + " takes two arguments, but " + _alArgEval + " provided.");

				if (o1 instanceof Integer && o2 instanceof Integer) {
					if (_sName == MAX) 
						return Math.max((Integer)o1,(Integer)o2);
					else // MIN
						return Math.min((Integer)o1,(Integer)o2);
				}
				
				if (_sName == MAX) 
					return Math.max(((Number)o1).doubleValue(), ((Number)o2).doubleValue());
				else // MIN
					return Math.min(((Number)o1).doubleValue(), ((Number)o2).doubleValue());
			}

			// ABS, SGN: Potentially integer functions -- single arg
			if (_sName == ABS || _sName == SGN) {
				if (_alArgEval.size() != 1)
					throw new EvalException("Operands of " + _sName + " take one argument, but " + _alArgEval + " provided.");

				if (o1 instanceof Integer) {
					Integer i1 = (Integer)o1;
					if (_sName == ABS) 
						return Math.abs(i1);
					else // SGN
						return (i1 > 0 ? 1 : (i1 < 0 ? -1 : 0));
				}
				
				if (_sName == ABS) 
					return Math.abs(((Number)o1).doubleValue());
				else // SGN
					return Math.signum(((Number)o1).doubleValue());
			}

			// ROUND, FLOOR, CEIL: Integer output, floating point input
			if (_sName == ROUND || _sName == FLOOR || _sName == CEIL) {
				if (_alArgEval.size() != 1)
					throw new EvalException("Operands of " + _sName + " take one argument, but " + _alArgEval + " provided.");

				if (_sName == ROUND) 
					return (int)Math.round(((Number)o1).doubleValue());
				else if (_sName == FLOOR)
					return (int)Math.floor(((Number)o1).doubleValue());
				else // SGN
					return (int)Math.ceil(((Number)o1).doubleValue());
			}

			// POW(a,b), LOG(a,b): Real-valued functions of base b -- two args
			if (_sName == POW || _sName == LOG) {
				if (_alArgEval.size() != 2)
					throw new EvalException("Operands of " + _sName + " takes two arguments, but " + _alArgEval + " provided.");

				if (_sName == POW) 
					return Math.pow(((Number)o1).doubleValue(), ((Number)o2).doubleValue());
				else // LOG(a,b) = ln(a) / ln(b)
					return Math.log(((Number)o1).doubleValue()) / Math.log(((Number)o2).doubleValue());
			}
							
			// COS,SIN,TAN,ACOS,ASIN,ATAN,COSH,SINH,SINH,EXP,LN,SQRT
			// Real-valued functions -- single arg
			if (_alArgEval.size() != 1)
				throw new EvalException("Operands of " + _sName + " take one argument, but " + _alArgEval + " provided.");

			if (_sName == COS)
				return Math.cos(((Number)o1).doubleValue());
			else if (_sName == SIN)
				return Math.sin(((Number)o1).doubleValue());
			else if (_sName == TAN)
				return Math.tan(((Number)o1).doubleValue());
			else if (_sName == ACOS)
				return Math.acos(((Number)o1).doubleValue());
			else if (_sName == ASIN)
				return Math.asin(((Number)o1).doubleValue());
			else if (_sName == ATAN)
				return Math.atan(((Number)o1).doubleValue());
			else if (_sName == COSH)
				return Math.cosh(((Number)o1).doubleValue());
			else if (_sName == SINH)
				return Math.sinh(((Number)o1).doubleValue());
			else if (_sName == TANH)
				return Math.tanh(((Number)o1).doubleValue());
			else if (_sName == EXP)
				return Math.exp(((Number)o1).doubleValue());
			else if (_sName == LN)
				return Math.log(((Number)o1).doubleValue());
			else if (_sName == SQRT)
				return Math.sqrt(((Number)o1).doubleValue());
			
			throw new EvalException("ERROR: should not have reached this point in the code, special function '" + _sName + "' is defined, but not evaluated"); 
		}
				
		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("FUN_EXPR.getDist: Not a distribution.");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			
			for (EXPR e : _alArgs)
				e.collectGFluents(subs, s, gfluents);
		}

	}
	
	public static class IF_EXPR extends EXPR { 

		public IF_EXPR(EXPR test, EXPR true_branch, EXPR false_branch) {
			this((BOOL_EXPR)test, true_branch, false_branch); // PARSE RESTRICTION
		}

		public IF_EXPR(BOOL_EXPR test, EXPR true_branch, EXPR false_branch) {
			_test = test;
			_trueBranch = true_branch;
			_falseBranch = false_branch;
			_bDet = test._bDet && true_branch._bDet && false_branch._bDet;
		}
		
		public BOOL_EXPR _test;
		public EXPR _trueBranch;
		public EXPR _falseBranch;
		
		public String toString() {
			if (USE_PREFIX) // TODO: Change prefix to if-then-else?
				return "(if " + _test + " then " + _trueBranch + " else " + _falseBranch + ")";
			else
				return "if (" + _test + ") then [" + _trueBranch + "] else [" + _falseBranch + "]";
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
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
			//System.out.println("\nGfluents before " + "[" + gfluents.size() + "] " + _test + ": " + gfluents + "... subs:" + subs + " / det:" + _test._bDet);
			HashSet<Pair> test_gfluents = new HashSet<Pair>();
			_test.collectGFluents(subs, s, test_gfluents);
			boolean test_state_indep = test_gfluents.size() == 0;
			gfluents.addAll(test_gfluents);
			
			// We can only pre-determine the test outcome if the test is independent of state
			Boolean test_outcome = null;
			if (test_state_indep && _test._bDet) 
				test_outcome = (Boolean)_test.sample(subs, s, null);
			
			if (test_outcome == null || test_outcome == true) // could simplify, but this is explicit and obvious
				_trueBranch.collectGFluents(subs, s, gfluents);
			//System.out.println("\nGfluents after T[" + test_outcome + "]: " + "[" + gfluents.size() + "] " + _test + ": " + gfluents);
			
			if (test_outcome == null || test_outcome == false) // could simplify, but this is explicit and obvious
				_falseBranch.collectGFluents(subs, s, gfluents);
			//System.out.println("\nGfluents after F[" + test_outcome + "]: " + "[" + gfluents.size() + "] " + _test + ": " + gfluents);
			
		}
	}

	public static class CASE {
		
		public CASE(LTERM term_val, EXPR expr) {
			_termVal = term_val;
			_expr = expr;
			_bDefaultCase = (term_val == null);
		}
		
		public boolean  _bDefaultCase;
		public LTERM    _termVal;
		public EXPR     _expr;
		
		public String toString() {
			if (USE_PREFIX)
				return "(" + (_bDefaultCase ? "default" : "case " + _termVal) + " : " + _expr + ")";
			else
				return (_bDefaultCase ? "default" : "case " + _termVal) + " : " + _expr;
		}

	}
	
	public static class SWITCH_EXPR extends EXPR {
		
		public SWITCH_EXPR(LTERM term, ArrayList cases) {
			_bDet = term._bDet;
			_term = term;
			_cases = (ArrayList<CASE>)cases;
			for (CASE c : _cases)
				_bDet = _bDet && c._expr._bDet;
		}
		
		public LTERM _term; 
		public ArrayList<CASE> _cases = null;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (USE_PREFIX) {
				sb.append("(switch " + _term + " ( ");
				for (CASE c : _cases)
					sb.append(c + " ");
				sb.append(") )");				
			} else {
				sb.append("switch (" + _term + ") {");
				boolean first = true;
				for (CASE c : _cases) {
					sb.append((first ? "" : ", ") + c);
					first = false;
				}
				sb.append("}");
			}
			return sb.toString();
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {

			try {
				LCONST seval = (LCONST)_term.sample(subs, s, r);
				
				for (CASE c : _cases)
					if (c._bDefaultCase || seval.equals(c._termVal.sample(subs, s, r)))
						return c._expr.sample(subs, s, r);
				
				throw new EvalException("No case for '" + seval + "' in " + _cases);
				
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw new EvalException("RDDL.SWITCH_EXPR: Error:\n" + e + "while evaluating:\n" + this);
			}

		}

		public EXPR getDist(HashMap<LVAR,LCONST> subs, State s) throws EvalException {
			throw new EvalException("RDDL.SWITCH_EXPR: Error: getDist not implemented\n(can be done by converting to equivalent if-then-else)");
		}

		public void collectGFluents(HashMap<LVAR, LCONST> subs,	State s, HashSet<Pair> gfluents) 
			throws EvalException {
			_term.collectGFluents(subs, s, gfluents);
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
		public static final Boolean TRUE  = Boolean.valueOf(true);
		public static final Boolean FALSE = Boolean.valueOf(false);
	}

	// TODO: should never put a RandomDataGenerator variable directly under a quantifier,
	//       a RandomDataGenerator sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class QUANT_EXPR extends BOOL_EXPR {
		
		public final static String EXISTS = "exists".intern();
		public final static String FORALL = "forall".intern();
		
		public QUANT_EXPR(String quant, ArrayList vars, EXPR expr) throws Exception {
			this(quant, vars, (BOOL_EXPR)expr); // PARSER RESTRICTION
		}
		
		public QUANT_EXPR(String quant, ArrayList vars, BOOL_EXPR expr) throws Exception {
			if (!quant.equals(EXISTS) && !quant.equals(FORALL))
				throw new Exception("Unrecognized quantifier type: " + quant);
			_sQuantType = quant.intern();
			_alVariables = (ArrayList<LTYPED_VAR>)vars;
			_expr = expr;
			_bDet = expr._bDet;
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
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {

			//System.out.println("VARS: " + _alVariables);
			ArrayList<ArrayList<LCONST>> possible_subs = s.generateAtoms(_alVariables);
			//System.out.println(possible_subs);
			
			// First check for early termination even if some values are null -- to assist with collectGFluents
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < _alVariables.size(); i++) {
					subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
				}
								
				if (_expr._bDet) { // (s.getPVariableType(p._pName) == State.NONFLUENT) {
					Boolean eval = null;
					try { // FIXME: should not rely on Exception for control flow, sample should be able to return null
						eval = (Boolean)_expr.sample(subs, s, null);
					} catch (Exception e) { /* ignore here */ }

					if (eval != null && ((_sQuantType == FORALL && eval == false) || (_sQuantType == EXISTS && eval == true))) {
												
						// Clear all substitutions before early return
						for (int i = 0; i < _alVariables.size(); i++) {
							subs.remove(_alVariables.get(i)._sVarName);
						}

						return eval; // Terminate fluent collection
					}
				}
			}		
			
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

			// First check for a fully deterministic or deterministic early termination outcome 
			HashSet<Pair> local_fluents = new HashSet<Pair>();
			for (ArrayList<LCONST> sub_inst : possible_subs) {
				for (int i = 0; i < _alVariables.size(); i++) {
					subs.put(_alVariables.get(i)._sVarName, sub_inst.get(i));
				}
				
				local_fluents.clear();
				_expr.collectGFluents(subs, s, local_fluents);
				boolean expr_is_indep_of_state = local_fluents.size() == 0;		
				
				if (expr_is_indep_of_state && _expr._bDet) { // (s.getPVariableType(p._pName) == State.NONFLUENT) {
					boolean eval = (Boolean)_expr.sample(subs, s, null);
					// If can determine truth value of connective from nonfluents
					// then any other fluents are irrelevant
					if ((_sQuantType == FORALL && !eval) || (_sQuantType == EXISTS && eval)) {
												
						// Clear all substitutions before early return
						for (int i = 0; i < _alVariables.size(); i++) {
							subs.remove(_alVariables.get(i)._sVarName);
						}

						return; // Terminate fluent collection
					}
				}
			}
			
			// No early termination -- evaluate all possible substitutions and collect gluents
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

	// TODO: should never put a RandomDataGenerator variable directly under a connective,
	//       a RandomDataGenerator sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class CONN_EXPR extends BOOL_EXPR {

		public static final String IMPLY = "=>".intern();
		public static final String EQUIV = "<=>".intern();
		public static final String AND   = "^".intern();
		public static final String OR    = "|".intern();

		public CONN_EXPR(EXPR b1, EXPR b2, String conn) throws Exception {
			this((BOOL_EXPR)b1, (BOOL_EXPR)b2, conn); // PARSER RESTRICTION
		}
		
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
			
			_bDet = true;
			for (BOOL_EXPR e : _alSubNodes)
				_bDet = _bDet && e._bDet;
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
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			
			// First check for early termination even if some args are null -- collectGfluents requires this
			if (_sConn == IMPLY) {
				Boolean b1 = null;
				try { // FIXME: should not rely on Exception for control-flow, sample should be modified to return null
					b1 = (Boolean)_alSubNodes.get(0).sample(subs, s, r);
				} catch (Exception e) { /* ignore */ }

				Boolean b2 = null;
				try { // FIXME: should not rely on Exception for control-flow, sample should be modified to return null
					b2 = (Boolean)_alSubNodes.get(1).sample(subs, s, r);
				} catch (Exception e) { /* ignore */ }
				
				if ((b1 != null && b1 == false) || (b2 != null && b2 == true))
					return BOOL_EXPR.TRUE; // F => ? and ? => T is always true
					
			} else if (_sConn != EQUIV) { // must be AND/OR
				for (BOOL_EXPR b : _alSubNodes) {
					Boolean interm_result = null;
					try { // FIXME: should not rely on Exception for control-flow, sample should be modified to return null
						interm_result = (Boolean)b.sample(subs, s, r);
					} catch (Exception e) { /* ignore this */ }

					// Early cutoff detection
					if (interm_result != null && _sConn == AND && interm_result == false) // forall
						return BOOL_EXPR.FALSE;
					else if (interm_result != null && _sConn == OR && interm_result == true) // exists
						return BOOL_EXPR.TRUE;
				}
			}
			
			// Now evaluate as normal
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
			HashSet<Pair> local_fluents = new HashSet<Pair>();
			if (_sConn == AND || _sConn == OR) {
				boolean all_subnodes_state_indep = true;
				for (BOOL_EXPR b : _alSubNodes) {
					// The following is more general than check for non-fluents, but may not always deterministically evaluate
					local_fluents.clear();
					b.collectGFluents(subs, s, local_fluents);
					boolean b_is_indep_of_state = local_fluents.size() == 0;
				
					// 2014: Special check for action dependencies
					// A direct recursive compilation may circumvent the need for this action-dependency analysis since
					//   it may be lightweight to build a disjunction and later prune?  Currently have to enumerate all
					//   joint values of irrelevant variables.
					if (ASSUME_ACTION_OBSERVED && (b instanceof PVAR_EXPR) && s.getPVariableType(((PVAR_EXPR)b)._pName) == State.ACTION) {
						
						// If AND/false or OR/true then the other elements of this connective are irrelevant and can return with no relevant fluents
						//System.out.println("Testing if can ignoring branch: " + this + " / " + subs);
						Boolean eval = (Boolean)b.sample(subs, s, null);
						if ((_sConn == AND && Boolean.FALSE.equals(eval)) || (_sConn == OR && Boolean.TRUE.equals(eval))) {
							//System.out.println("Ignoring branch: " + this);
							return;
						}
					}
					
					// Check for early termination due to nonfluent state independence and deterministic evaluation
					if (b_is_indep_of_state && b._bDet) { // (s.getPVariableType(p._pName) == State.NONFLUENT) {
						boolean eval = (Boolean)b.sample(subs, s, null);
						// If can determine truth value of connective from nonfluents
						// then any other fluents are irrelevant
						if ((_sConn == AND && !eval) || (_sConn == OR && eval)) {
							//System.out.println("\n>> early termination on '" + subs + "'" + this);
							return; // Terminate fluent collection
						}
					} else {
						all_subnodes_state_indep = false; // Stochastic so state dependent 
					}
				}
				if (all_subnodes_state_indep)
					return; // This expressions value is not state dependent
			} else if (_sConn == IMPLY || _sConn == EQUIV) {
				Boolean lhs_condition = null;
				local_fluents.clear();
				_alSubNodes.get(0).collectGFluents(subs, s, local_fluents);
				boolean arg0_is_indep_of_state = local_fluents.size() == 0;
				if (arg0_is_indep_of_state && _alSubNodes.get(0)._bDet)// (s.getPVariableType(p._pName) == State.NONFLUENT) {
					lhs_condition = (Boolean)_alSubNodes.get(0).sample(subs, s, null);
				
				Boolean rhs_condition = null;
				local_fluents.clear();
				_alSubNodes.get(1).collectGFluents(subs, s, local_fluents);
				boolean arg1_is_indep_of_state = local_fluents.size() == 0;
				if (arg1_is_indep_of_state && _alSubNodes.get(1)._bDet) // (s.getPVariableType(p._pName) == State.NONFLUENT) {
					rhs_condition = (Boolean)_alSubNodes.get(1).sample(subs, s, null);

				if (lhs_condition != null && rhs_condition != null)
					return; // Can terminate since this statement's outcome is independent of state
				else if (_sConn == IMPLY && (Boolean.FALSE.equals(lhs_condition) || (Boolean.TRUE.equals(rhs_condition)))) 
					return; // Can terminate => if LHS false or RHS true since this statement's outcome is independent of state
			} 
			
			// Otherwise collect subnodes
			for (BOOL_EXPR b : _alSubNodes)
				b.collectGFluents(subs, s, gfluents);

			//System.out.println("CollGfluents: " + this + " -- " + gfluents);
		}

	}
	
	// TODO: should never put a RandomDataGenerator variable directly under a negation,
	//       a RandomDataGenerator sample should always be referenced by an intermediate
	//       variable so that it is consistent over repeated evaluations.
	public static class NEG_EXPR extends BOOL_EXPR {

		public NEG_EXPR(EXPR b) {
			this((BOOL_EXPR)b); // PARSER RESTRICTION
		}

		public NEG_EXPR(BOOL_EXPR b) {
			_subnode = b;
			_bDet = b._bDet;
		}
		
		public BOOL_EXPR _subnode;
		
		public String toString() {
			if (USE_PREFIX)
				return "(~ " + _subnode + ")";
			else
				return "~" + _subnode;
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
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
			_bDet = true;
			_bValue = b;
			_sType = BOOL;
		}
		
		public boolean _bValue;
		
		public String toString() {
			return Boolean.toString(_bValue);
		}
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
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
			_bDet = e1._bDet && e2._bDet;
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
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
						
			Object o1 = _e1.sample(subs, s, r);
			Object o2 = _e2.sample(subs, s, r);
			return ComputeCompResult(o1, o2, _comp);
		}
		
		public static Object ComputeCompResult(Object o1, Object o2, String comp) throws EvalException {
			
			// Recursive case for vectors
			if (o1 instanceof STRUCT_VAL && o2 instanceof STRUCT_VAL) {
				
				STRUCT_VAL s1 = (STRUCT_VAL)o1;
				STRUCT_VAL s2 = (STRUCT_VAL)o2;

				if ((comp != EQUAL) && (comp != NEQ)) 
					throw new EvalException("Cannot currently perform " + comp + " on vector types." +
							"\nOperand 1: " + s1 + "\nOperand 2: " + s2);
				
				if (s1._alMembers.size() != s2._alMembers.size())
					throw new EvalException("Cannot perform elementwise vector operation on vectors of different lengths." + 
							"\nOperand 1: " + s1 + "\nOperand 2: " + s2 + "\nComp: " + comp);

				Boolean equal = true;
				for (int i = 0; i < s1._alMembers.size() && equal; i++) {
					STRUCT_VAL_MEMBER v1 = s1._alMembers.get(i);
					STRUCT_VAL_MEMBER v2 = s2._alMembers.get(i);
					if (!v1._sLabel.equals(v2._sLabel))
						throw new EvalException("Mismatched vector labels during elementwise vector operation: " + v1 + " vs " + v2 + " in" +
							"\nOperand 1: " + s1 + "\nOperand 2: " + s2 + "\nComp: " + comp);
					equal = (Boolean)ComputeCompResult(v1._oVal, v2._oVal, EQUAL);
				}
				//System.out.println("EQUAL: " + equal + " for "+ s1 + " and " + s2);
				return (comp == EQUAL) ? equal : !equal;
			}

			// Base cases...
			
			// Handle special case of enum comparison
			if (o1 instanceof LCONST || o2 instanceof LCONST) {
				if (!(o1 instanceof LCONST && o2 instanceof LCONST))
					throw new EvalException("Both values in object/enum comparison " + comp + " (" + o1 + "/" + o1.getClass() + "," + o2 + "/" + o2.getClass() + ") must be object/enum\n");
				if (!(comp == NEQ || comp == EQUAL))
					throw new EvalException("Can only compare object/enum (" + o1 + "/" + o1.getClass() + "," + o2 + "/" + o2.getClass() + ") with == or ~=, not " + comp + "\n");
				return (comp == EQUAL) ? o1.equals(o2) : !o1.equals(o2);
			}
			
			// Convert boolean to numeric value (allows comparison of boolean with int/real)
			if (o1 instanceof Boolean)
				o1 = ((Boolean)o1 == true ? 1 : 0);
			if (o2 instanceof Boolean)
				o2 = ((Boolean)o2 == true ? 1 : 0);
			
			// Not so efficient, but should be correct
			double v1 = ((Number)o1).doubleValue();
			double v2 = ((Number)o2).doubleValue();
			
			if (comp == NEQ) {
				//System.out.println("- NOT EQUAL: " + (v1 != v2) + " for "+ v1 + " and " + v2);
				return (v1 != v2) ? TRUE : FALSE;
			} else if (comp == LESSEQ) {
				return (v1 <= v2) ? TRUE : FALSE;				
			} else if (comp == LESS) {
				return (v1 < v2) ? TRUE : FALSE;
			} else if (comp == GREATER) {
				return (v1 > v2) ? TRUE : FALSE;
			} else if (comp == GREATEREQ) {
				return (v1 >= v2) ? TRUE : FALSE;
			} else if (comp == EQUAL) {
				//System.out.println("- EQUAL: " + (v1 == v2) + " for "+ v1 + " and " + v2);
				return (v1 == v2) ? TRUE : FALSE;
			} else
				throw new EvalException("RDDL.COMP_EXPR: Unknown comparison operator: " + comp);
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
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			return null;
		}
	}
	
	public static class PVAR_INST_DEF {
	
		public PVAR_INST_DEF(String predname, Object value, ArrayList terms) {
			_sPredName = new PVAR_NAME(predname);
			_oValue = value;
			_alTerms = terms;
			//System.out.println("Made new: " + this.toString());
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
		
		public int hashCode() {
			return _sPredName.hashCode() + (_oValue.hashCode() << 10) + (_alTerms.hashCode() << 20); 
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
		
		public Object sample(HashMap<LVAR,LCONST> subs, State s, RandomDataGenerator r) throws EvalException {
			return null;
		}
	}
	
	/////////////////////////////////////////////////////////

	//public static void main(String[] args) {
	//
	//}

}
