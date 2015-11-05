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

public class ReservoirHandCodedPolicy implements Policy {
	
	private RandomDataGenerator rng = new RandomDataGenerator();
	private PVarHeatMap viz = new PVarHeatMap( PVarHeatMap.reservoir_tags );
	
	public ReservoirHandCodedPolicy( List<String> args ) {
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		//for each reservoir
		//send random amount out on random pipe
		//keep track of used pipes
		TYPE_NAME type_reserv = new TYPE_NAME("reservoir");
		PVAR_NAME rlevel = new PVAR_NAME("rlevel");
		TYPE_NAME pipe_type = new TYPE_NAME("pipe"); 
		PVAR_NAME send_pvar = new PVAR_NAME("send");
		PVAR_NAME connect_pvar = new PVAR_NAME("CONNECT");
		HashMap<ArrayList<LCONST>, Object> pipes = s._nonfluents.get( connect_pvar );
		PVAR_NAME capacity_pvar = new PVAR_NAME("CAPACITY");
		
		HashMap< LCONST, Double > remaining_level = new HashMap<>();
		HashMap< LCONST, Double > remaining_cap = new HashMap<>();
		
		ArrayList<PVAR_INST_DEF> ret = new ArrayList<>();
		
		for( final Map.Entry<ArrayList<LCONST>, Object> this_pipe : pipes.entrySet() ){
			
			//pick random source among two ends
			int src_ind = rng.nextInt(1, 2);
			LCONST src = this_pipe.getKey().get( src_ind );
			
			//dest
			LCONST dest = this_pipe.getKey().get( ( src_ind == 1 ) ? 2 : 1 ) ;
			
			double rlevelsrc = 0;
			if( remaining_level.containsKey( src ) ){
				rlevelsrc = remaining_level.get( src );
			}else{
				rlevelsrc = (double) s.getPVariableAssign( rlevel, new ArrayList<LCONST>( Collections.singletonList( src ) ) );	
			}
			
			double remaining_at_dest;
			if( remaining_cap.containsKey( dest ) ){
				remaining_at_dest = remaining_cap.get( dest );
			}else{
				double capacity_dest = (double) s.getPVariableAssign(capacity_pvar, new ArrayList<LCONST>( ) );//Collections.singletonList( dest ) ) );	
				double dest_level = (double) s.getPVariableAssign( rlevel, new ArrayList<LCONST>( Collections.singletonList( dest ) ) );
				remaining_at_dest = capacity_dest - dest_level;
				remaining_cap.put( dest, remaining_at_dest );
			}
			
			double upper = Math.min( rlevelsrc,  remaining_at_dest );
					
			final Double send = ( upper <= 0 ) ? 0 : rng.nextUniform( 0d, upper*0.25 );
			
			remaining_level.put( src, rlevelsrc-send );
			remaining_cap.put( dest, remaining_at_dest-send );
			
			ArrayList<LCONST> terms = new ArrayList<LCONST>( );
			terms.add( src ); terms.add( this_pipe.getKey().get(0) );
			ret.add( new PVAR_INST_DEF( send_pvar._sPVarName, send, terms ) );
			
		}
		
		viz .display(s, 0);
		
		return ret;
	}

}
