//////////////////////////////////////////////////////////////////////
//
// File:     PDDL.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   Parsing of PDDL files.
//
//////////////////////////////////////////////////////////////////////

// Package definition
package ppddl;

// Packages to import
import java.io.*;
import java.math.*;
import java.util.*;

/**
 * Reads a PDDL file... at this point leaves input in nested ArrayList form. See
 * ont.io.HierarchicalParser and ont.io.IR/IRLoader for a better way to parse
 * the PDDL with a lexer and a general set of match(.) methods.
 * 
 * @version 1.0
 * @author Scott Sanner
 * @language Java (JDK 1.3)
 **/
public class PPDDL {
	public ArrayList _alDomains;
	public ArrayList _alProblems;

	public Domain getDomain(String name) {
		Iterator i = _alDomains.iterator();
		while (i.hasNext()) {
			Domain d = (Domain) i.next();
			if (d._sName.equalsIgnoreCase(name)) {
				return d;
			}
		}
		return null;
	}

	public Problem getProblem(String name) {
		Iterator i = _alProblems.iterator();
		while (i.hasNext()) {
			Problem p = (Problem) i.next();
			if (p._sName.equalsIgnoreCase(name)) {
				return p;
			}
		}
		return null;
	}

	public List listDomains() {
		ArrayList al = new ArrayList();
		Iterator i = _alDomains.iterator();
		while (i.hasNext()) {
			Domain d = (Domain) i.next();
			al.add(d._sName);
		}
		return al;
	}

	public List listProblems() {
		ArrayList al = new ArrayList();
		Iterator i = _alProblems.iterator();
		while (i.hasNext()) {
			Problem p = (Problem) i.next();
			al.add(p._sName);
		}
		return al;
	}

	public static class Domain {

		public String _sName;
		public ArrayList _alRequirements;
		public ArrayList _alPredicates; // List of Pred (below)
		public ArrayList _alTypes;
		public ArrayList _alConstants;
		public ArrayList _alConstTypes;
		public ArrayList _alActions;

		public Domain(String name, ArrayList requirements,
				ArrayList predicates, ArrayList types, ArrayList constants,
				ArrayList ctypes, ArrayList actions) {
			_sName = name;
			_alRequirements = requirements;
			_alPredicates = predicates;
			_alTypes = types;
			_alConstants = constants;
			_alConstTypes = ctypes;
			_alActions = actions;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Domain: " + _sName + "\n");
			sb.append("  - Requirements:   " + _alRequirements + "\n");
			sb.append("  - Predicates:     " + _alPredicates + "\n");
			sb.append("  - Types:          " + _alTypes + "\n");
			sb.append("  - Constants:      " + _alConstants + "\n");
			sb.append("  - Constant types: " + _alConstTypes + "\n");
			sb.append("  - Actions:        " + _alActions + "\n");
			return sb.toString();
		}
	}

	public static class Pred {
		public String _sName;
		public ArrayList _alTypes;

		public Pred(String name, ArrayList types) {
			_sName = name;
			_alTypes = types;
		}

		public String toString() {
			return "Pred:" + _sName + _alTypes;
		}
	}

	public static class Problem {

		public String _sName;
		public String _sDomain;
		public ArrayList _alObjects;
		public ArrayList _alTypes;
		public ArrayList _alInit;
		public ArrayList _alGoal;
		public ArrayList _alMetric;
		public double _dReward;

		public Problem(String name, String domain, ArrayList objects,
				ArrayList types, ArrayList init, ArrayList goal, ArrayList metric, double reward) {
			_sName = name;
			_sDomain = domain;
			_alObjects = objects;
			_alTypes = types;
			_alInit = init;
			_alGoal = goal;
			_alMetric = metric;
			_dReward = reward;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("Problem: " + _sName + "\n");
			sb.append("  - Domain:  " + _sDomain + "\n");
			sb.append("  - Objects: " + _alObjects + "\n");
			sb.append("  - Types:   " + _alTypes + "\n");
			sb.append("  - Init:    " + _alInit + "\n");
			sb.append("  - Goal:    " + _alGoal + "\n");
			if (_alMetric != null)
				sb.append("  - Metric:  " + _alMetric + "\n");
			sb.append("  - Reward:  " + _dReward + "\n");
			return sb.toString();
		}
	}

	public static class Action {

		public String _sName;
		public ArrayList _alVars;
		public ArrayList _alTypes;
		public ArrayList _alPreconditions;
		public ArrayList _alEffects;

		public Action(String name, ArrayList vars, ArrayList types,
				ArrayList preconds, ArrayList effects) {
			_sName = name;
			_alVars = vars;
			_alTypes = types;
			_alPreconditions = preconds;
			_alEffects = effects;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("   Action:    " + _sName + "\n");
			sb.append("       - Variables: " + _alVars + "\n");
			sb.append("       - Types:     " + _alTypes + "\n");
			sb.append("       - Preconds:  " + _alPreconditions + "\n");
			sb.append("       - Effects:   " + _alEffects + "\n");
			return sb.toString();
		}
	}

	// Constructor
	public PPDDL(String filename) {
		_alDomains = new ArrayList();
		_alProblems = new ArrayList();
		processFile(filename);

		// Also look for a generic "domain" file
		int pos = filename.lastIndexOf("/");
		if (pos >= 0) {
			String dir = filename.substring(0, pos);
			String gen_domain = dir + "/domain.pddl";
			processFile(gen_domain);
		}
	}

	public void processFile(String filename) {
		System.out.println("Processing: " + filename);
		ArrayList content = HierarchicalParser.ParseFile(filename);
		if (content == null) {
			System.out.println("[PDDL] Could not read: " + filename);
		} else {
			// System.out.println(content);
			// PrintFormattedList(content); // Remove
			list2IR(content);
		}
	}

	public void list2IR(ArrayList list) {

		Iterator i = list.iterator();
		while (i.hasNext()) {
			ArrayList l = (ArrayList) i.next();
			String s = (String) l.get(0);
			if (s.equalsIgnoreCase("define")) {
				ArrayList l2 = (ArrayList) l.get(1);
				String s2 = (String) l2.get(0);
				if (s2.equalsIgnoreCase("domain")) {
					domainList2IR(l);
				} else if (s2.equalsIgnoreCase("problem")) {
					problemList2IR(l);
				} else {
					System.out.println("Expected 'domain' or 'problem': " + s);
					return;
				}
			} else {
				System.out.println("Expected keyword 'define': " + s);
				return;
			}
		}
	}

	public void domainList2IR(ArrayList l) {
		ArrayList l2 = (ArrayList) l.get(1);
		String name = (String) l2.get(1); // [domain name]
		// System.out.println("Parsing domain: " + name);
		ArrayList reqs = new ArrayList();
		ArrayList predicates = new ArrayList();
		ArrayList constants = new ArrayList();
		ArrayList ctypes = new ArrayList();
		ArrayList types = new ArrayList();
		ArrayList actions = new ArrayList();
		for (int index = 2; index < l.size(); index++) {
			ArrayList nl = (ArrayList) l.get(index);
			String ident = (String) nl.get(0);
			if (ident.equalsIgnoreCase(":requirements")) {
				for (int i2 = 1; i2 < nl.size(); i2++) {
					reqs.add(nl.get(i2));
				}

			} else if (ident.equalsIgnoreCase(":constants")) {
				for (int i3 = 1; i3 < nl.size(); i3++) {
					constants.add(nl.get(i3));
					if ((i3 + 1) < nl.size() && "-".equals(nl.get(i3 + 1))) {
						ctypes.add(nl.get(i3 + 2));
						i3 += 2;
					} else {
						ctypes.add(null);
					}
				}

				String last_type = null;
				for (int i4 = ctypes.size() - 1; i4 >= 0; i4--) {
					String cur_type;
					if ((cur_type = (String) ctypes.get(i4)) == null) {
						if (last_type != null) {
							ctypes.set(i4, last_type);
						}
					} else {
						last_type = cur_type;
					}
				}

			} else if (ident.equalsIgnoreCase(":types")) {
				for (int i2 = 1; i2 < nl.size(); i2++) {
					types.add(nl.get(i2));
				}

			} else if (ident.equalsIgnoreCase(":predicates")) {
				for (int i2 = 1; i2 < nl.size(); i2++) {

					// Process predicates
					ArrayList predl = (ArrayList) nl.get(i2);
					String predname = (String) predl.get(0);
					ArrayList ptypes = new ArrayList();
					for (int i3 = 1; i3 < predl.size(); i3++) {

						// Process args (and types)
						String var = (String) predl.get(i3);
						if ((i3 + 1) < predl.size()
								&& "-".equals(predl.get(i3 + 1))) {
							Object type = predl.get(i3 + 2);
							ptypes.add(type);
							i3 += 2;

							// Process in reverse
							for (int i4 = ptypes.size() - 2; i4 >= 0; i4++) {
								if (ptypes.get(i4) == null) {
									ptypes.set(i4, type);
								} else {
									break;
								}
							}
						} else {
							ptypes.add(null);
						}
					}
					predicates.add(new Pred(predname, ptypes));
				}

			} else if (ident.equalsIgnoreCase(":action")) {
				actions.add(actionList2IR(nl));
			} else {
				System.out
						.println("Expected ':req's',':types',':predicates',':action':"
								+ ident);
				return;
			}
		}

		_alDomains.add(new Domain(name, reqs, predicates, types, constants,
				ctypes, actions));
	}

	public Action actionList2IR(ArrayList l) {

		String name = (String) l.get(1);
		ArrayList vars = new ArrayList();
		ArrayList types = new ArrayList();
		ArrayList precond = null, effect = null;
		for (int index = 2; index < l.size(); index += 2) {
			String ident = (String) l.get(index);
			if (ident.equalsIgnoreCase(":parameters")) {

				ArrayList predl = (ArrayList) l.get(index + 1);
				for (int i3 = 0; i3 < predl.size(); i3++) {

					// Process args (and types)
					String var = (String) predl.get(i3);
					vars.add(var);
					if ((i3 + 1) < predl.size()
							&& "-".equals(predl.get(i3 + 1))) {
						types.add(predl.get(i3 + 2));
						i3 += 2;
					} else {
						types.add(null);
					}
				}
			} else if (ident.equalsIgnoreCase(":precondition")) {

				precond = (ArrayList) l.get(index + 1);

			} else if (ident.equalsIgnoreCase(":effect")) {

				effect = (ArrayList) l.get(index + 1);

			} else {
				System.out.println("Expected ':params',':precond',':effects':"
						+ ident);
				return null;
			}
		}
		return new Action(name, vars, types, precond, effect);
	}

	public void problemList2IR(ArrayList l) {
		//System.out.println("Processing problem: " + l);
		ArrayList l2 = (ArrayList) l.get(1);
		String name = (String) l2.get(1);
		String domain = null;
		ArrayList objects = new ArrayList();
		ArrayList types = new ArrayList();
		ArrayList init = new ArrayList();
		ArrayList metric = null;
		ArrayList goal = null;
		double reward = -1;
		// System.out.println("Parsing problem: " + name);
		for (int index = 2; index < l.size(); index++) {
			ArrayList nl = (ArrayList) l.get(index);
			String ident = (String) nl.get(0);
			if (ident.equalsIgnoreCase(":domain")) {
				domain = (String) nl.get(1);
			} else if (ident.equalsIgnoreCase(":objects")) {

				for (int i3 = 1; i3 < nl.size(); i3++) {

					// Process args (and types)
					String obj = (String) nl.get(i3);
					objects.add(obj);
					if ((i3 + 1) < nl.size() && "-".equals(nl.get(i3 + 1))) {
						types.add(nl.get(i3 + 2));
						i3 += 2;
					} else {
						types.add(null);
					}
				}

				String last_type = null;
				for (int i4 = types.size() - 1; i4 >= 0; i4--) {
					String cur_type;
					if ((cur_type = (String) types.get(i4)) == null) {
						if (last_type != null) {
							types.set(i4, last_type);
						}
					} else {
						last_type = cur_type;
					}
				}

			} else if (ident.equalsIgnoreCase(":init")) {
				for (int i2 = 1; i2 < nl.size(); i2++) {
					init.add(nl.get(i2));
				}
			} else if (ident.equalsIgnoreCase(":metric")) {
				metric = nl;
			} else if (ident.equalsIgnoreCase(":goal")) {
				goal = (ArrayList) nl.get(1);
			} else if (ident.equalsIgnoreCase(":goal-reward")) {
				String sval = (String) nl.get(1);
				try {
					reward = Double.parseDouble(sval);
				} catch (NumberFormatException nfe) {
					System.out.println("Invalid goal reward: " + sval);
					return;
				}
			} else {
				System.out.println("Expected ':params',':precond',':effects',':metric'..."
						+ ident + "\n got: " + nl);
				return;
			}
		}
		Problem new_problem = new Problem(name, domain, objects, types, init, goal, metric, reward);
		//System.out.println("Adding problem: " + new_problem);
		_alProblems.add(new_problem);
	}

	public static void PrintFormattedList(ArrayList l) {
		PrintFormattedList(l, 0);
	}

	public static void PrintFormattedList(ArrayList l, int n) {
		Iterator i = l.iterator();
		boolean prev_recurse = false;
		System.out.print("\n" + Indent(n) + "[ ");
		while (i.hasNext()) {
			Object next = i.next();
			if (next instanceof ArrayList) {
				PrintFormattedList((ArrayList) next, n + 1);
				prev_recurse = true;
			} else {
				if (prev_recurse) {
					System.out.print("\n" + Indent(n) + "  ");
					prev_recurse = false;
				}
				System.out.print(/* next.getClass() + ":" + */next.toString()
						+ " ");
			}
		}
		System.out.print(" ]");
	}

	public static String Indent(int l) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < l; i++) {
			sb.append("   ");
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		if (args.length < 1) {
			System.out.println("PDDL: Requires a filename argument");
			System.exit(1);
		}

		PPDDL n = new PPDDL(args[0]);
		System.out.println(n._alDomains);
		System.out.println(n._alProblems);

	}
}
