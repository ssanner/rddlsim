/**
 * RDDL: Translates a RDDL Problem to a SPUDD (MDP) / Symbolic Perseus 
 *       (POMDP) language specification.
 *       
 * WARNING: This is the old translator that uses a non-additive form.
 *          Some domains with large additive rewards simply will not
 *          translate with this version so use at your own risk.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @author Sungwook Yoon (sungwook.yoon@gmail.com)
 * @version 10/16/10
 *
 **/
package rddl.translate;

import java.io.*;
import java.util.*;

import dd.discrete.ADD;
import dd.discrete.DD;

import rddl.*;
import rddl.RDDL.*;
import rddl.parser.*;
import util.*;

public class RDDL2FormatNonAdd {

	public final static boolean DEBUG_CPTS = true;
	public final static boolean SHOW_GRAPH = false;
	public final static boolean SHOW_RELEVANCE = false;

	public final static int STATE_ITER = 0;
	public final static int OBSERV_ITER = 1;

	// For Vanilla SPUDD: need to build ADD with variables for all state vars
	// For Concurrent SPUDD: will need to also add in action variables and
	// enumerate these
	// For Continuous SPUDD: will need RDDL to return an XADD compatible
	// decision tree with with state variables evaluated out or
	// concurrent encoding with state variables as XADD variables
	// Have to go through all states, actions assignments
	// Go through all next state vars and capture CPTs
	public final static String UNKNOWN = "unknown".intern();
	public final static String SPUDD_ORIG = "spudd_orig".intern();
	public final static String SPUDD_CURR = "spudd_sperseus".intern();
	public final static String SPUDD_CONC = "spudd_concurrent".intern();
	public final static String SPUDD_CONT = "spudd_continuous".intern();
	public final static String SPUDD_CONT_CONC = "spudd_continuous_concurrent"
			.intern();
	public final static String PPDDL = "ppddl".intern();

	public State _state;
	public INSTANCE _i;
	public NONFLUENTS _n;
	public DOMAIN _d;

	public ADD _context;
	public ArrayList<Integer> _alSaveNodes;
	public int DD_ZERO;
	public int DD_ONE;

	public ArrayList<String> _alAllVars;
	public ArrayList<String> _alStateVars;
	public ArrayList<String> _alNextStateVars;
	public ArrayList<String> _alActionVars;
	public ArrayList<String> _alObservVars;
	public HashMap<String, String> _hmPrimeRemap;

	public TreeMap<Pair, Integer> _var2transDD;
	public TreeMap<Pair, Integer> _var2observDD;
	public TreeMap<String, Integer> _act2rewardDD;

	public Integer _reward;
	public String _sTranslationType = UNKNOWN;

	// Painful introduction just to pass arguments for PPDDL processing
	// Sungwook: this is OK for now, but a cleaner way would be to
	// use a non-anonymous inner class and pass these arguments
	// to the constructor rather than using the current
	// anonymous inner class approach which requires these
	// static variables. -Scott
	public static PrintWriter PW;
	public static String S;

	// public static double rewardSum;

	public RDDL2FormatNonAdd(RDDL rddl, String instance_name,
			String translation_type) throws Exception {

		_sTranslationType = translation_type.toLowerCase().intern();

		_state = new State();

		// Set up instance, nonfluent, and domain information
		_i = rddl._tmInstanceNodes.get(instance_name);
		if (_i == null)
			throw new Exception("Instance '" + instance_name
					+ "' not found, choices are "
					+ rddl._tmInstanceNodes.keySet());

		_n = null;
		if (_i._sNonFluents != null)
			_n = rddl._tmNonFluentNodes.get(_i._sNonFluents);
		_d = rddl._tmDomainNodes.get(_i._sDomain);
		if (_d == null)
			throw new Exception("Could not get domain '" + _i._sDomain
					+ "' for instance '" + instance_name + "'");
		if (_n != null && !_i._sDomain.equals(_n._sDomain))
			throw new Exception(
					"Domain name of instance and fluents do not match: "
							+ _i._sDomain + " vs. " + _n._sDomain);

		// Create the initial state which will setup all the data structures
		// in State that we'll need to produce the DBN
		_state.init(_d._hmObjects, _n != null ? _n._hmObjects : null, _i._hmObjects,
				_d._hmTypes, _d._hmPVariables, _d._hmCPF, _i._alInitState,
				_n == null ? new ArrayList<PVAR_INST_DEF>() : _n._alNonFluents, _i._alNonFluents,
                _d._alStateConstraints, _d._alActionPreconditions, _d._alStateInvariants, 
				_d._exprReward, _i._nNonDefActions);

		// Build the CPTs
		buildCPTs();
	}

	////////////////////////////////////////////////////////////////////////////
	// ////
	// Export Methods for a Variety of Formats
	////////////////////////////////////////////////////////////////////////////
	// ////

	// TODO: Export PO-PPDDL (like SPUDD / Symbolic Perseus, first do detect)
	public void export(String directory) throws Exception {

		String separator = (directory.endsWith("\\") || directory.endsWith("/")) ? ""
				: File.separator;
		String filename = directory + separator /*+ _d._sDomainName + "."*/
				+ _i._sName;

		if (_sTranslationType == PPDDL) {
			if (_var2observDD.size() == 0)
				filename = filename + ".ppddl";
			else
				filename = filename + ".po-ppddl";
		} else {
			if (_var2observDD.size() == 0)
				filename = filename + ".spudd";
			else
				filename = filename + ".sperseus";
		}
		PrintWriter pw = new PrintWriter(new FileWriter(filename));
		// PrintWriter pw = new PrintWriter(System.out);

		// Strings interned so can test equality here
		if (_sTranslationType == SPUDD_ORIG)
			exportSPUDD(pw, false, false);
		else if (_sTranslationType == SPUDD_CURR)
			exportSPUDD(pw, true, false);
		else if (_sTranslationType == SPUDD_CONC)
			exportSPUDD(pw, false, true);
		else if (_sTranslationType == PPDDL)
			exportPPDDL(pw);
		else
			throw new Exception(_sTranslationType + " not currently supported.");

		pw.close();
		System.out.println("\n>> Exported: '" + filename + "'");
	}

	public void exportSPUDD(PrintWriter pw, boolean curr_format,
			boolean allow_conc) {
		pw
				.println("// Automatically produced by rddl.translate.RDDL2FormatNonAdd"/*
																				 * +
																				 * " using '"
																				 * +
																				 * _sTranslationType
																				 * +
																				 * "'"
																				 */);
		pw.println("// SPUDD / Symbolic Perseus Format for '" + _d._sDomainName
				+ "." + _i._sName + "'");

		// State (and action variables)
		pw.println("\n(variables ");
		for (String s : _alStateVars)
			pw.println("\t(" + s + " true false)");
		if (allow_conc)
			for (String a : _alActionVars)
				pw.println("\t(" + a + " true false)");
		pw.println(")");

		// Observations
		if (_alObservVars.size() > 0) {

			pw.println("\n(observations ");
			for (String s : _alObservVars)
				pw.println("\t(" + s.substring(0, s.length() - 1)
						+ " true false)");
			pw.println(")");
		}

		// Initial state
		HashMap<String,Boolean> init_state_assign = new HashMap<String,Boolean>();
		for (PVAR_INST_DEF def : _i._alInitState) {	
			// Get the assignments for this PVAR
			init_state_assign.put(CleanFluentName(def._sPredName.toString() + def._alTerms),
					(Boolean)def._oValue);
		}
		pw.println("\ninit [*");
		for (String s : _alStateVars) {
			
			Boolean bval = init_state_assign.get(s);
			if (bval == null) { // No assignment, get default value
				// This is a quick fix... a bit ugly
				PVAR_NAME p = new PVAR_NAME(s.split("__")[0]);
				bval = (Boolean)_state.getDefaultValue(p);
			}
			if (bval)
				pw.println("\t(" + s + " (true (1.0)) (false (0.0)))");
			else
				pw.println("\t(" + s + " (true (0.0)) (false (1.0)))");
		}
		pw.println("]");

		// Actions
		if (allow_conc) {
			exportSPUDDAction("<no action -- concurrent>", curr_format, pw);
		} else {
			for (String action_name : _alActionVars) {
				exportSPUDDAction(action_name, curr_format, pw);
			}
		}

		// Reward
		pw.print("\nreward");
		_context.exportTree(_reward, pw, curr_format, 1);

		// Discount and tolerance
		pw.println("\n\ndiscount " + _i._dDiscount);
		pw.println("horizon " + _i._nHorizon);
		// pw.println("tolerance 0.00001");
	}

	public void exportSPUDDAction(String action_name, boolean curr_format,
			PrintWriter pw) {

		pw.println("\naction "
				+ (action_name.equals("<no action -- concurrent>") ? ""
						: action_name));
		for (String s : _alStateVars) {
			pw.print("\t" + s);

			// Build both halves of dual action diagram if curr_format
			System.out.println("Getting: " + action_name + ", " + s);
			int dd = _var2transDD.get(new Pair(action_name, s));
			if (curr_format) {

				// System.out.println("Multiplying..." + dd + ", " + DD_ONE);
				// _context.printNode(dd);
				// _context.printNode(DD_ONE);
				int dd_true = _context.getVarNode(s + "'", 0d, 1d);
				dd_true = _context.applyInt(dd_true, dd, ADD.ARITH_PROD);

				int dd_false = _context.getVarNode(s + "'", 1d, 0d);
				// System.out.println("Multiplying..." + dd + ", " + DD_ONE);
				// _context.printNode(dd);
				// _context.printNode(DD_ONE);
				int one_minus_dd = _context.applyInt(DD_ONE, dd,
						ADD.ARITH_MINUS);
				dd_false = _context.applyInt(dd_false, one_minus_dd,
						ADD.ARITH_PROD);

				// Replace original DD with "dual action diagram" DD
				dd = _context.applyInt(dd_true, dd_false, ADD.ARITH_SUM);
			}

			_context.exportTree(dd, pw, curr_format, 2);
			pw.println();
		}
		if (_alObservVars.size() > 0) {
			pw.println("\tobserve ");
			for (String o : _alObservVars) {

				Integer dd = _var2observDD.get(new Pair(action_name, o));
				if (curr_format) {
					// dd = _context.remapGIDsInt(dd, _hmPrimeRemap);

					int dd_true = _context.getVarNode(o, 0d, 1d);
					dd_true = _context.applyInt(dd_true, dd, ADD.ARITH_PROD);

					int dd_false = _context.getVarNode(o, 1d, 0d);
					// System.out.println("Multiplying..." + dd + ", " +
					// DD_ONE);
					// _context.printNode(dd);
					// _context.printNode(DD_ONE);
					int one_minus_dd = _context.applyInt(DD_ONE, dd,
							ADD.ARITH_MINUS);
					dd_false = _context.applyInt(dd_false, one_minus_dd,
							ADD.ARITH_PROD);

					// Replace original DD with "dual action diagram" DD
					dd = _context.applyInt(dd_true, dd_false, ADD.ARITH_SUM);
				}

				pw.print("\t\t" + o.substring(0, o.length() - 1));
				_context.exportTree(dd, pw, curr_format, 3);
				pw.println();
			}
			pw.println("\tendobserve");
		}

		// Always show action cost (can be zero)
		int reward_dd = _act2rewardDD.get(action_name);
		int cost_dd = _context.applyInt(_reward, reward_dd, DD.ARITH_MINUS);
		// if (cost_dd != DD_ZERO) { // All functions are canonical
		pw.print("\tcost ");
		_context.exportTree(cost_dd, pw, curr_format, 2);
		pw.println();
		// }

		pw.println("endaction");
	}

	public void exportPPDDL(PrintWriter pw) {
		// first write domain
		pw
				.println(";; Automatically produced by rddl.translate.RDDL2FormatNonAdd"/*
																				 * +
																				 * " using '"
																				 * +
																				 * _sTranslationType
																				 * +
																				 * "'"
																				 */);
		pw.println("(define (domain " + _d._sDomainName + ")");
		// Sungwook: we are using :rewards not :disjunctive-goals.
		// pw.println(
		// "\t(:requirements :adl :probabilistic-effects :disjunctive-goals)");
		pw.println("\t(:requirements :adl :probabilistic-effects :rewards)");
		pw.println("\t(:predicates ");
		for (String s : _alStateVars) {
			pw.println("\t\t(" + s + ")");
		}
		// pw.println("\t\t(agoal)");
		pw.println("\t)");
		if (_alObservVars.size() > 0) {
			pw.println("\t(:observations ");
			for (String s : _alObservVars) {
				s = s.substring(0, s.length() - 1);
				pw.println("\t\t(" + s + ")");
			}
			pw.println("\t)");
		}
		for (String action_name : _alActionVars) {
			exportPPDDLAction(action_name, pw);
		}
		// pw.println(
		// "\t(:action achieve-goal :parameters () :precondition () :effect (agoal))"
		// );
		pw.println(")");

		// write problem
		pw.println("(define (problem " + _i._sName + ")");
		pw.println("\t(:domain " + _d._sDomainName + ")");
		pw.println("\t(:init ");
		
		// Initial state
		HashMap<String,Boolean> init_state_assign = new HashMap<String,Boolean>();
		for (PVAR_INST_DEF def : _i._alInitState) {	
			// Get the assignments for this PVAR
			init_state_assign.put(CleanFluentName(def._sPredName.toString() + def._alTerms),
					(Boolean)def._oValue);
		}
		for (String s : _alStateVars) {
			
			Boolean bval = init_state_assign.get(s);
			if (bval == null) { // No assignment, get default value
				// This is a quick fix... a bit ugly
				PVAR_NAME p = new PVAR_NAME(s.split("__")[0]);
				bval = (Boolean)_state.getDefaultValue(p);
			}

			if (bval) {
//				pw.println("\t\t(probabilistic 1.0 " + s + ")");
				pw.println("\t\t(" + s + ")");
			} else {
//				pw.println("\t\t(probabilistic 0.0 " + s +")");
			}
		}
		pw.println("\t)");
		pw.println("\t(:metric maximize (reward))");
		pw.println("\t;; (:horizon " + _i._nHorizon + ")");
		pw.println("\t;; (:discount " + _i._dDiscount + ")");
		pw.println(")");

		pw.close();
	}

	public void exportPPDDLAction(String action_name, PrintWriter pw) {
		pw.println("\t(:action " + action_name);
		// pw.println("\t\t(:parameters ())");
		// pw.println("\t\t(:precondition ())");
		pw.println("\t\t:effect (and ");

		PW = pw;
		for (String s : _alStateVars) {
			S = s;
			int dd = _var2transDD.get(new Pair(action_name, s));
			_context.enumeratePaths(dd, new ADD.ADDLeafOperation() {
				public void processADDLeaf(ArrayList<String> assign,
						double leaf_val) {
					// Sungwook: I've made a few changes to correct and
					// simplify the effects produced. -Scott
					boolean print_true_effect = (!assign.contains(S) && (leaf_val > 0d));
					boolean print_false_effect = (!assign.contains("~" + S) && (leaf_val < 1d));
					if (!print_true_effect && !print_false_effect)
						return;

					PW.print("\t\t\t");
					boolean use_when = (assign.size() > 0);
					if (use_when) {
						PW.print("(when (and ");
						for (String a : assign) {
							if (_var2observDD.size() != 0) { // DB: No priming
																// in POPDDL
								boolean not = a.startsWith("~");
								int beginIndex = (not) ? 1 : 0;
								int endIndex = (a.endsWith("'")) ? (a.length() - 1)
										: a.length();
								String prop = a.substring(beginIndex, endIndex);
								if (not) {
									PW.print(" (not (" + prop + "))");
								} else {
									PW.print(" (" + prop + ")");
								}
							} else { // PPDDL
								if (a.startsWith("~")) {
									PW.print(" (not (" + a.substring(1) + "))");
								} else {
									PW.print(" (" + a + ")");
								}
							}
						}
						PW.print(") ");
					}

					PW.print("(probabilistic ");
					if (print_true_effect)
						PW.print(leaf_val + " (" + S + ") ");
					if (print_false_effect)
						PW.print((1d - leaf_val) + " (not " + "(" + S + ")"
								+ ")");

					if (use_when)
						PW.println("))");
					else
						PW.println(")"); // Don't need to close when
				}
			});

			// This is the action specific reward
			// int reward_dd = _act2rewardDD.get(action_name);
		}

		// Now export the reward for this action
		PW = pw;
		int reward_dd = _act2rewardDD.get(action_name);
		_context.enumeratePaths(reward_dd, new ADD.ADDLeafOperation() {
			public void processADDLeaf(ArrayList<String> assign, double leaf_val) {

				if (leaf_val == 0d)
					return;

				PW.print("\t\t\t");
				boolean use_when = (assign.size() > 0);
				if (use_when) {
					PW.print("(when (and ");
					for (String a : assign) {
						if (a.startsWith("~")) {
							PW.print(" (not (" + a.substring(1) + "))");
						} else {
							PW.print(" (" + a + ")");
						}
					}
					PW.print(") ");
				}
				String operation = leaf_val > 0d ? "increase" : "decrease";
				PW.println("(" + operation + " (reward) " + Math.abs(leaf_val)
						+ "))");
			}
		});
		pw.println("\t\t)");

		if (_alObservVars.size() > 0) {
			pw.println("\t\t:observation (and ");
			for (String s : _alObservVars) {
				S = s.substring(0, s.length() - 1);
				for (String o : _alObservVars) {
					Integer dd = _var2observDD.get(new Pair(action_name, o));
					_context.enumeratePaths(dd, new ADD.ADDLeafOperation() {
						public void processADDLeaf(ArrayList<String> assign,
								double leaf_val) {
							// Sungwook: I've made a few changes to correct and
							// simplify the effects produced. -Scott
							boolean print_true_effect = (!assign.contains(S) && (leaf_val > 0d));
							boolean print_false_effect = (!assign.contains("~"
									+ S) && (leaf_val < 1d));
							if (!print_true_effect && !print_false_effect)
								return;

							PW.print("\t\t\t");
							boolean use_when = (assign.size() > 0);
							if (use_when) {
								PW.print("(when (and ");
								for (String a : assign) {
									boolean not = a.startsWith("~");
									int beginIndex = (not) ? 1 : 0;
									int endIndex = (a.endsWith("'")) ? (a
											.length() - 1) : a.length();
									String prop = a.substring(beginIndex,
											endIndex);
									if (not) {
										PW.print(" (not (" + prop + "))");
									} else {
										PW.print(" (" + prop + ")");
									}
								}
								PW.print(") ");
							}

							PW.print("(probabilistic ");
							if (print_true_effect)
								PW.print(leaf_val + " (" + S + ") ");
							if (print_false_effect)
								PW.print((1d - leaf_val) + " (not " + "(" + S
										+ ")" + ")");

							if (use_when)
								PW.println("))");
							else
								PW.println(")"); // Don't need to close when
						}
					});
				}
			}
			pw.println("\t\t)");
		} // End observations

		pw.println("\t)");
	}

	public void buildCPTs() throws Exception {

		_var2transDD = new TreeMap<Pair, Integer>();
		_var2observDD = new TreeMap<Pair, Integer>();
		_act2rewardDD = new TreeMap<String, Integer>();

		// Verify no intermediate variables
		if (_state._tmIntermNames.size() > 0)
			throw new Exception(
					"Cannot convert to SPUDD format: contains intermediate variables");

		// Should verify that max-nondef-actions is 1 for non-concurrent
		// versions.
		if (_i._nNonDefActions > 1 && _sTranslationType != SPUDD_CONC
				&& _sTranslationType != SPUDD_CONT_CONC)
			throw new Exception(
					"This domain uses concurrency, but the translation type '"
							+ _sTranslationType + "' does not support it.");
		else if (_i._nNonDefActions == 1
				&& (_sTranslationType == SPUDD_CONC || _sTranslationType == SPUDD_CONT_CONC)) {
			throw new Exception(
					"This domain does not use concurrency, but the translation type '"
							+ _sTranslationType
							+ "' is intended for concurrency.");
		}

		// Get all variables
		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> state_vars = collectStateVars();
		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> action_vars = collectActionVars();
		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> observ_vars = collectObservationVars();

		_alStateVars = new ArrayList<String>();
		for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e : state_vars
				.entrySet()) {
			PVAR_NAME p = e.getKey();
			ArrayList<ArrayList<LCONST>> assignments = e.getValue();
			for (ArrayList<LCONST> assign : assignments) {
				String name = CleanFluentName(p.toString() + assign);
				_alStateVars.add(name);
			}
		}
		_hmPrimeRemap = new HashMap<String, String>();
		_alNextStateVars = new ArrayList<String>();
		for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e : state_vars
				.entrySet()) {
			PVAR_NAME p = e.getKey();
			ArrayList<ArrayList<LCONST>> assignments = e.getValue();
			for (ArrayList<LCONST> assign : assignments) {
				String name = CleanFluentName(p.toString() + assign);
				_alNextStateVars.add(name + "'");
				_hmPrimeRemap.put(name, name + "'");
			}
		}

		// Interned so String equality can be tested by equality
		// If supporting concurrency then encode actions as variables.
		_alActionVars = new ArrayList<String>();
		_alActionVars.add("noop");
		for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e : action_vars
				.entrySet()) {
			PVAR_NAME p = e.getKey();
			ArrayList<ArrayList<LCONST>> assignments = e.getValue();
			for (ArrayList<LCONST> assign : assignments) {
				String name = CleanFluentName(p.toString() + assign);
				_alActionVars.add(name);
			}
		}

		_alObservVars = new ArrayList<String>();
		for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e : observ_vars
				.entrySet()) {
			PVAR_NAME p = e.getKey();
			ArrayList<ArrayList<LCONST>> assignments = e.getValue();
			for (ArrayList<LCONST> assign : assignments) {
				String name = CleanFluentName(p.toString() + assign);
				_alObservVars.add(name + "'"); // SPUDD uses primed observation
												// vars
			}
		}
		_alAllVars = new ArrayList<String>();
		_alAllVars.addAll(_alStateVars);
		_alAllVars.addAll(_alNextStateVars);
		if (_sTranslationType == SPUDD_CONC
				|| _sTranslationType == SPUDD_CONT_CONC)
			_alAllVars.addAll(_alActionVars);
		_alAllVars.addAll(_alObservVars);

		// Build the ADD context for the trees
		System.out.println(_alAllVars);
		_context = new ADD(_alAllVars);
		DD_ONE = _context.getConstantNode(1d);
		DD_ZERO = _context.getConstantNode(0d);
		_alSaveNodes = new ArrayList<Integer>();
		_alSaveNodes.add(DD_ONE);
		_alSaveNodes.add(DD_ZERO);

		System.out.println("State vars:  " + state_vars);
		System.out.println("Action vars: " + action_vars);
		System.out.println("Observ vars: " + observ_vars);

		// For each action, set it to on and others to false and generate all
		// CPTs. Assume boolean, using only logical connectives and Bernoulli
		// distributions or KronDelta (with deterministic evaluation per state/
		// action) at leaves. Expand all sum/prod. Collect all relevant vars.
		// Simplify nonfluents to their actual value.

		// What is a canonical data structure with random variables???
		// Seems to be distributions pushed down as far as possible.
		// Easiest if recursive nestings are separated into different
		// diagrams, otherwise temporary intermediate variables are needed.

		for (int iter = STATE_ITER; iter <= OBSERV_ITER; iter++) {

			// This loop handles both transition and observation construction
			HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> src = iter == STATE_ITER ? state_vars
					: observ_vars;

			for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e : src
					.entrySet()) {

				// Go through all variable names p for a variable type
				PVAR_NAME p = e.getKey();
				ArrayList<ArrayList<LCONST>> assignments = e.getValue();
				// System.out.println(_state._hmCPFs);

				CPF_DEF cpf = _state._hmCPFs.get(new PVAR_NAME(p.toString()
						+ (iter == STATE_ITER ? "'" : "")));

				HashMap<LVAR, LCONST> subs = new HashMap<LVAR, LCONST>();
				for (ArrayList<LCONST> assign : assignments) {

					String cpt_var = CleanFluentName(p.toString() + assign);
					System.out.println("Processing: " + cpt_var);

					subs.clear();
					for (int i = 0; i < cpf._exprVarName._alTerms.size(); i++) {
						LVAR v = (LVAR) cpf._exprVarName._alTerms.get(i);
						LCONST c = (LCONST) assign.get(i);
						subs.put(v, c);
					}

					// This method determines all ground fluents relevant to
					// the cpf for a variable for the given substitution 'subs'
					HashSet<Pair> relevant_vars = new HashSet<Pair>();
					EXPR cpf_expr = cpf._exprEquals;
					if (_d._bCPFDeterministic) // collectGFluents expects a
												// distribution so convert to a
												// Delta function if needed
						cpf_expr = new KronDelta(cpf_expr);

					cpf_expr.collectGFluents(subs, _state, relevant_vars);
					if (SHOW_RELEVANCE)
						System.out.println("Vars relevant to " + cpt_var + ": "
								+ relevant_vars);

					// Filter out action vars if we are not doing a concurrent
					// encoding
					// with actions as state variables
					if (_sTranslationType != SPUDD_CONC
							&& _sTranslationType != SPUDD_CONT_CONC)
						relevant_vars = filterOutActionVars(relevant_vars);

					// Go through all actions and initialize state assignment to
					// false
					for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e2 : action_vars
							.entrySet()) {

						PVAR_NAME action_name = e2.getKey();
						ArrayList<ArrayList<LCONST>> action_assignments = e2
								.getValue();

						// Go through instances for action_name
						for (ArrayList<LCONST> action_assign : action_assignments)
							_state.setPVariableAssign(action_name,
									action_assign, RDDL.BOOL_CONST_EXPR.FALSE);

					}

					// Now go through all actions setting each one to be true in
					// turn
					if (_sTranslationType != SPUDD_CONC
							&& _sTranslationType != SPUDD_CONT_CONC) {

						// NON-CONCURRENT CASE, EXPLICITLY ENUMERATE ALL
						// SINGLETON ACTIONS
						// TODO: if concurrent (max-nondef-actions > 1) better
						// to enumerate all joint
						// actions as a Map<name -> (Set Pair<PVAR_NAME
						// action,ArrayList<LCONST> vars>)>
						// ... need a |A|^|max-nondef-actions| enumeration
						// method that checks constraints
						// ... then this method goes through each set one-by-one
						// setting it true then
						// back to false
						for (Map.Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> e2 : action_vars
								.entrySet()) {

							PVAR_NAME action_name = e2.getKey();
							ArrayList<ArrayList<LCONST>> action_assignments = e2
									.getValue();

							// Clear out ADD caches if required
							_context.clearSpecialNodes();
							for (int n : _alSaveNodes)
								_context.addSpecialNode(n);
							_context.flushCaches(true);

							// Use null as a sign of a NOOP
							action_assignments.add(null /*
														 * new
														 * ArrayList<LCONST>()
														 */); // Add the empty
																// list
							action_assignments = (ArrayList<ArrayList<LCONST>>) action_assignments
									.clone();
							for (ArrayList<LCONST> action_assign : action_assignments) {

								String action_instance;
								if (action_assign != null) {
									action_instance = CleanFluentName(action_name
											.toString()
											+ action_assign);
									_state.setPVariableAssign(action_name,
											action_assign,
											RDDL.BOOL_CONST_EXPR.TRUE);
								} else
									action_instance = "noop";

								// Build action-specific reward
								Integer rew_fun = _act2rewardDD
										.get(action_instance);
								if (rew_fun == null) {
									HashSet<Pair> rew_relevant_vars = new HashSet<Pair>();

									HashMap<LVAR, LCONST> empty_sub = new HashMap<LVAR, LCONST>();
									EXPR rew_expr = _state._reward;
									if (_d._bRewardDeterministic) // collectGFluents
																	// expects
																	// distribution
										rew_expr = new DiracDelta(rew_expr);
									rew_expr.collectGFluents(empty_sub, _state,
											rew_relevant_vars);
									rew_relevant_vars = filterOutActionVars(rew_relevant_vars);

									if (SHOW_RELEVANCE)
										System.out
												.println("Vars relevant to reward: "
														+ rew_relevant_vars);
									rew_fun = enumerateAssignments(
											new ArrayList<Pair>(
													rew_relevant_vars),
											rew_expr, empty_sub, 0);
									_act2rewardDD.put(action_instance, rew_fun);
									_alSaveNodes.add(rew_fun);
								}

								// For this action, enumerate all relevant state
								// assignments
								// and build the CPT. (Could be more efficient
								// by recursively
								// constructing ADD from RDDL expression, but
								// this becomes
								// extremely difficult when intermediate
								// constructs like
								// sum/prod are used in comparisons. This method
								// is generic
								// and relatively simple.)
								if (SHOW_RELEVANCE)
									System.out.println("Vars relevant to "
											+ action_instance + ", " + cpt_var
											+ ": " + relevant_vars);

								int cpt = enumerateAssignments(
										new ArrayList<Pair>(relevant_vars),
										cpf_expr, subs, 0);
								_alSaveNodes.add(cpt);
								if (iter == STATE_ITER)
									_var2transDD.put(new Pair(action_instance,
											cpt_var), cpt);
								else
									_var2observDD.put(new Pair(action_instance,
											cpt_var + "'"), cpt);

								// Undo so next action can be set
								if (action_assign != null)
									_state.setPVariableAssign(action_name,
											action_assign,
											RDDL.BOOL_CONST_EXPR.FALSE);
							}
						}
					} else {
						// CONCURRENT CASE, NO NEED TO ENUMERATE ACTIONS
						// (IMPLICIT VARS IN CPT)
						int cpt = enumerateAssignments(new ArrayList<Pair>(
								relevant_vars), cpf_expr, subs, 0);
						_alSaveNodes.add(cpt);
						if (iter == STATE_ITER)
							_var2transDD.put(new Pair(
									"<no action -- concurrent>", cpt_var), cpt);
						else
							_var2observDD.put(new Pair(
									"<no action -- concurrent>", cpt_var), cpt);
					}
				}
			}
		}

		// Finally compute function for reward
		//
		// Get the non-action specific reward -- assuming defaults for actions,
		// so may be incorrect if rewards are action-specific... in this case
		// see _act2rewardDD.
		HashSet<Pair> relevant_vars = new HashSet<Pair>();
		HashMap<LVAR, LCONST> empty_sub = new HashMap<LVAR, LCONST>();
		EXPR rew_expr = _state._reward;
		if (_d._bRewardDeterministic) // collectGFluents expects distribution
			rew_expr = new DiracDelta(rew_expr);
		rew_expr.collectGFluents(empty_sub, _state, relevant_vars);

		if (_sTranslationType != SPUDD_CONC
				&& _sTranslationType != SPUDD_CONT_CONC)
			relevant_vars = filterOutActionVars(relevant_vars);

		System.out.println("Vars relevant to reward: " + relevant_vars);
		_reward = null;
		try {
			_reward = enumerateAssignments(new ArrayList<Pair>(relevant_vars),
					rew_expr, empty_sub, 0);
			_alSaveNodes.add(_reward);
		} catch (Exception e) {
		}

		// Debug
		if (DEBUG_CPTS) {
			PrintWriter ps = new PrintWriter(System.out);

			int i = 0;
			System.out.println();
			for (Map.Entry<Pair, Integer> e : _var2transDD.entrySet()) {
				if (++i > 3)
					break;
				System.out.println("Transition: " + e); // + " :: " +
														// _context.printNode
														// ((Integer
														// )e.getValue()));
				// _context.exportTree(e.getValue(), ps, true);
				// ps.println("\n\n");
				ps.flush();
				if (SHOW_GRAPH)
					_context.getGraph(e.getValue()).launchViewer();
			}

			i = 0;
			System.out.println();
			for (Map.Entry<Pair, Integer> e : _var2observDD.entrySet()) {
				if (++i > 3)
					break;
				System.out.println("Observation: " + e); // + " :: " +
															// _context.printNode
															// ((Integer)e.
															// getValue()));
				if (SHOW_GRAPH)
					_context.getGraph(e.getValue()).launchViewer();
			}

			i = 0;
			System.out.println();
			boolean reward_action_dependent = false;
			Integer last_reward = null;
			for (Map.Entry<String, Integer> e : _act2rewardDD.entrySet()) {
				if (++i > 3)
					break;
				System.out.println("Action-based reward: " + e); // + " :: " +
																	// _context
																	// .printNode
																	// (
																	// (Integer)
																	// e
																	// .getValue
																	// ()));
				if (SHOW_GRAPH)
					_context.getGraph(e.getValue()).launchViewer();

				// Check to see if reward is independent of the action
				if (last_reward != null) {
					reward_action_dependent = reward_action_dependent
							|| (!e.getValue().equals(last_reward));
					// System.out.println(reward_action_dependent + " " +
					// e.getValue() + " != " + last_reward + ", " +
					// e.getValue().getClass() + ", " + last_reward.getClass() +
					// " = " + (!e.getValue().equals(last_reward)));
				}
				last_reward = e.getValue();
			}

			// Get the non-action specific reward -- assuming defaults for
			// actions
			System.out.println("\nGeneral reward = " + _reward);
			if (SHOW_GRAPH)
				_context.getGraph(_reward).launchViewer();

			if (last_reward != null)
				reward_action_dependent = reward_action_dependent
						|| (!_reward.equals(last_reward));
			// System.out.println(reward_action_dependent);

			if (reward_action_dependent)
				System.err
						.println("NOTE: Reward is action dependent... verify this is correctly reflected in action cost functions.");
		}
	}

	private HashSet<Pair> filterOutActionVars(HashSet<Pair> relevant_vars) {
		HashSet<Pair> new_vars = new HashSet<Pair>();
		for (Pair p : relevant_vars)
			if (_state.getPVariableType((PVAR_NAME) p._o1) != State.ACTION)
				new_vars.add(p);
		return new_vars;
	}

	public int enumerateAssignments(ArrayList<Pair> vars, EXPR cpf_expr,
			HashMap<LVAR, LCONST> subs, int index) throws EvalException {
		return enumerateAssignments(vars, cpf_expr, subs, index, _context
				.getConstantNode(1d));
	}

	public int enumerateAssignments(ArrayList<Pair> vars, EXPR cpf_expr,
			HashMap<LVAR, LCONST> subs, int index, int cpt)
			throws EvalException {

		// Need to build an assignment ADD on the way down, use it
		// at leaf to add to current CPT and return result

		// Recurse until index == vars.size
		if (index >= vars.size()) {

			// At this point, all state and action variables are set
			// Just need to get the distribution for the appropriate CPT
			RDDL.EXPR e = cpf_expr.getDist(subs, _state);
			double prob_true = -1d;
			// System.out.println("RDDL.EXPR: " + e);
			if (e instanceof KronDelta) {
				EXPR e2 = ((KronDelta) e)._exprIntValue;
				if (e2 instanceof INT_CONST_EXPR)
					// Should either be true (1) or false (0)... same as
					// prob_true
					prob_true = (double) ((INT_CONST_EXPR) e2)._nValue;
				else if (e2 instanceof BOOL_CONST_EXPR)
					prob_true = ((BOOL_CONST_EXPR) e2)._bValue ? 1d : 0d;
				else
					throw new EvalException("Unhandled KronDelta argument: "
							+ e2.getClass());
			} else if (e instanceof Bernoulli) {
				prob_true = ((REAL_CONST_EXPR) ((Bernoulli) e)._exprProb)._dValue;
			} else if (e instanceof DiracDelta) {
				// NOTE: this is not a probability, but rather an actual value
				// (presumably for the reward). This method is a little
				// overloaded... need to consider whether there will be
				// any problems arising from this overloading. -Scott
				prob_true = ((REAL_CONST_EXPR) ((DiracDelta) e)._exprRealValue)._dValue;
			} else
				throw new EvalException("Unhandled distribution type: "
						+ e.getClass());

			// Now build CPT for action variables
			return _context.scalarMultiply(cpt, prob_true);

		} else {
			PVAR_NAME p = (PVAR_NAME) vars.get(index)._o1;
			ArrayList<LCONST> terms = (ArrayList<LCONST>) vars.get(index)._o2;
			String var_name = CleanFluentName(p._sPVarName + terms
					+ (p._bPrimed ? "\'" : ""));
			// System.out.println(var_name);

			// Set to true
			_state.setPVariableAssign(p, terms, RDDL.BOOL_CONST_EXPR.TRUE);
			int high = _context.getVarNode(var_name, 0d, 1d);
			int ret_high = enumerateAssignments(vars, cpf_expr, subs,
					index + 1, _context.applyInt(cpt, high, ADD.ARITH_PROD));

			// Set to false
			_state.setPVariableAssign(p, terms, RDDL.BOOL_CONST_EXPR.FALSE);
			int low = _context.getVarNode(var_name, 1d, 0d);
			int ret_low = enumerateAssignments(vars, cpf_expr, subs, index + 1,
					_context.applyInt(cpt, low, ADD.ARITH_PROD));

			// Unassign
			_state.setPVariableAssign(p, terms, null);

			// Sum both sides of the returned CPT and return it
			return _context.applyInt(ret_high, ret_low, ADD.ARITH_SUM);
		}
	}

	public HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> collectStateVars()
			throws EvalException {

		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> state_vars = new HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>();

		for (PVAR_NAME p : _state._alStateNames) {
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			state_vars.put(p, gfluents);
		}

		return state_vars;
	}

	public HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> collectActionVars()
			throws EvalException {

		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> action_vars = new HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>();

		for (PVAR_NAME p : _state._alActionNames) {
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			action_vars.put(p, gfluents);
		}

		return action_vars;
	}

	public HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> collectObservationVars()
			throws EvalException {

		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> observ_vars = new HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>();

		for (PVAR_NAME p : _state._alObservNames) {
			ArrayList<ArrayList<LCONST>> gfluents = _state.generateAtoms(p);
			observ_vars.put(p, gfluents);
		}

		return observ_vars;
	}

	public static String CleanFluentName(String s) {
		s = s.replace("[", "__");
		s = s.replace("]", "");
		s = s.replace(", ", "_");
		s = s.replace(',', '_');
		s = s.replace(' ', '_');
		s = s.replace('-', '_');
		s = s.replace("()", "");
		if (s.endsWith("__"))
			s = s.substring(0, s.length() - 2);
		return s;
	}

	////////////////////////////////////////////////////////////////////////////
	// ////
	// Testing Methods
	////////////////////////////////////////////////////////////////////////////
	// ////

	public static void ShowFileFormats() {
		System.out.println("Supported languages are");
		System.out.println("  spudd_sperseus");
		System.out.println("  ppddl");
		System.out.println("  spudd_orig (an older SPUDD format)");
		System.out
				.println("  spudd_conc (SPUDD format supporting concurrency)");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Parse file
		// RDDL rddl = parser.parse(new
		// File("files/boolean/rddl/game_of_life_nointerm_mdp.rddl"));
		// RDDL rddl = parser.parse(new
		// File("files/boolean/rddl/game_of_life_nointerm_pomdp.rddl"));
		// RDDL rddl = parser.parse(new
		// File("files/boolean/rddl/sysadmin_bool_mdp.rddl"));
		// RDDL rddl = parser.parse(new
		// File("files/boolean/rddl/sysadmin_bool_pomdp.rddl"));

		if (args.length != 3) {
			System.out
					.println("\nusage: RDDL-file/directory output-dir file-format\n");
			ShowFileFormats();
			System.exit(1);
		}
		String arg2_intern = args[2].intern();
		if (arg2_intern != SPUDD_ORIG && arg2_intern != SPUDD_CURR
				&& arg2_intern != SPUDD_CONC && arg2_intern != PPDDL) {
			System.out.println("\nFile format '" + arg2_intern
					+ "' not supported yet.\n");
			ShowFileFormats();
			System.exit(2);
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
			RDDL r = null;
			try {
				r = parser.parse(f);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Error processing: " + f + ", skipping...");
				rddl_files.remove(f);
				continue;
			}
			file2rddl.put(f, r);
			rddl.addOtherRDDL(r, f.getName());
		}
		
		for (File f : rddl_files) {
						
			try {	
				for (String instance_name : file2rddl.get(f)._tmInstanceNodes.keySet()) {
					RDDL2FormatNonAdd r2s = new RDDL2FormatNonAdd(rddl, instance_name, arg2_intern);
					r2s.export(output_dir);
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
