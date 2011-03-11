package rddl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.translate.RDDL2Format;

public class ActionGenerator {

	public static TreeMap<String,ArrayList<PVAR_INST_DEF>> getLegalBoolActionMap(State s) 
		throws EvalException {
	
		ArrayList<PVAR_INST_DEF> actions = new ArrayList<PVAR_INST_DEF>();
	
		// Build a map from propositional action names to actions
		// that can be returned from this policy.
		TreeMap<String,ArrayList<PVAR_INST_DEF>> action_map = new TreeMap<String,ArrayList<PVAR_INST_DEF>>();
	
		//if (ALLOW_NOOP) {
		action_map.put("noop", (ArrayList<PVAR_INST_DEF>)actions.clone());
		//}
		
		HashSet<HashSet<PVAR_INST_DEF>> action_set = new HashSet<HashSet<PVAR_INST_DEF>>();
		buildBooleanActionSet(s, s._nMaxNondefActions, actions, action_map, action_set);
		
		return action_map;
	}

	public static void buildBooleanActionSet(State s, int actions_left, 
			ArrayList<PVAR_INST_DEF> action_list,
			TreeMap<String,ArrayList<PVAR_INST_DEF>> action_map,
			HashSet<HashSet<PVAR_INST_DEF>> action_set) {
	
		--actions_left;
		if (actions_left < 0) {
			// TODO: Build string name with "___"
			// TODO: Add multiple action ability to cclient
			// TODO: Add multiple action translation to problem generator
			//       ... verify equivalent for translations
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (PVAR_INST_DEF p : action_list) {
				if (!first)
					sb.append("___");
				sb.append(RDDL2Format.CleanFluentName("" + p._sPredName + p._alTerms));
				first = false;
			}
			ArrayList<PVAR_INST_DEF> action_list_to_add = (ArrayList<PVAR_INST_DEF>)action_list.clone();
			HashSet<PVAR_INST_DEF> action_set_to_add = new HashSet<PVAR_INST_DEF>(action_list_to_add);
			if (!action_set.contains(action_set_to_add)) {
				action_map.put(sb.toString(), action_list_to_add);
				action_set.add(action_set_to_add);
			}
		} else {
		
			for (PVAR_NAME p : s._alActionNames) {
				
				// Get term instantiations for that action and select *one*
				ArrayList<ArrayList<LCONST>> inst = null;
				try {
					inst = s.generateAtoms(p);
				} catch (EvalException e) {
					System.out.println("ERROR: could not generate atoms for " + p + "\n" + e);
					e.printStackTrace();
					System.exit(1);
				}
					
				boolean passed_constraints = false;
				for (int i = 0; i < inst.size(); i++) {
					ArrayList<LCONST> terms = inst.get(i);
					PVAR_INST_DEF cur_action = new PVAR_INST_DEF(p._sPVarName, new Boolean(true), terms);
					boolean action_already_added = action_list.contains(cur_action);
					if (!action_already_added)
						action_list.add(cur_action);
					passed_constraints = true;
					try {
						s.checkStateActionConstraints(action_list);
					} catch (EvalException e) {
						passed_constraints = false;
					}
					if (passed_constraints) {
						buildBooleanActionSet(s, actions_left, action_list, action_map, action_set);
					}
					if (!action_already_added)
						action_list.remove(action_list.size() - 1);
				}
			}
		}
	}

}
