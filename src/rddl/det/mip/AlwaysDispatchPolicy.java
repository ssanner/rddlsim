package rddl.det.mip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.State;
import rddl.policy.Policy;
import rddl.RDDL.TYPE_NAME;

public class AlwaysDispatchPolicy implements Policy {
	
	public AlwaysDispatchPolicy( List<String> args ) {
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<PVAR_INST_DEF>();
		ret.add( new PVAR_INST_DEF("dispatch", Boolean.TRUE, new ArrayList<LCONST>() ) );
		
		try{
			s.computeIntermFluents(ret, new RandomDataGenerator() );
			s.checkStateActionConstraints( ret );
			return ret;
		}catch( EvalException exc ){
//			exc.printStackTrace();
			ret.remove(0);
		}
		return ret;

	}

}
