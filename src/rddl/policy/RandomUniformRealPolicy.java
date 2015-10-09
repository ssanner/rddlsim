package rddl.policy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.State;

public class RandomUniformRealPolicy implements Policy {
	
	private double lower;
	private double upper;
	private RandomDataGenerator rand = new RandomDataGenerator();
	
	public RandomUniformRealPolicy( List<String> args ) {
		RandomUniformRealPolicyInit( Double.parseDouble( args.get(0) ), Double.parseDouble( args.get(1) ) ); 
	}
	
	public void RandomUniformRealPolicyInit( double lower_bd, double upper_bd ) {
		lower = lower_bd; upper = upper_bd;
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<>();
		for( final PVAR_NAME action_pred : s._alActionNames ){
			ArrayList<ArrayList<LCONST>> terms = s.generateAtoms(action_pred);
			for( final ArrayList<LCONST> term : terms ){
				ret .add( new PVAR_INST_DEF( action_pred._sPVarName, rand.nextUniform(lower, upper) , term ) );
			}
		}
		return ret;
	}
	
	
	
}
