package rddl.det.mip;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.math3.random.RandomDataGenerator;

import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBVar;
import rddl.EvalException;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.CPF_DEF;
import rddl.RDDL.EXPR;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.PVAR_EXPR;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.State;
import rddl.viz.StateViz;
import util.Pair;
import util.Timer;

public class EmergencyDomainHOPTranslate extends HOPTranslate {

	private static final boolean SHOW_TIMING = false;
	public static final PVAR_NAME firstResponsePvarName = new PVAR_NAME("firstResponse");
	public static final PVAR_NAME fullResponsePvarName = new PVAR_NAME("fullResponse");
	public static final PVAR_NAME overwhelmPvarName = new PVAR_NAME("overwhelm");
	
	private EmergencyDomainDataReel reel;
	private FileWriter outFile;
	
	public EmergencyDomainHOPTranslate(List<String> args) throws Exception {
		super(args.subList(0, args.size()-3));
		reel = new EmergencyDomainDataReel( args.get( args.size()-5 ), ",", true, 
				false, Integer.parseInt( args.get( args.size()- 4 ) ), //numfolds
				Integer.parseInt( args.get( args.size()- 3 ) ), //training fold
				Integer.parseInt( args.get( args.size()- 2 ) ) ); //testing fold
		outFile = new FileWriter(new File( args.get( args.size()-1 ) ) );
	}
	
	@Override
	protected void prepareModel( ) throws Exception{
		translate_time.ResumeTimer();
		System.out.println("--------------Translating Constraints-------------");
		translateConstraints( );
		
		ArrayList<HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>> src 
		= new ArrayList< HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> >();
		src.add( rddl_state_vars ); src.add( rddl_interm_vars ); src.add( rddl_observ_vars );
		
		ArrayList<Integer> time_terms_indices = new ArrayList<Integer>( TIME_TERMS.size() );
		for( int i = 0 ; i < TIME_TERMS.size(); ++i ){
			time_terms_indices.add( i );
		}
		
		ArrayList<Integer> future_terms_indices = new ArrayList<Integer>( future_TERMS.size() );
		for( int i = 0 ; i < future_TERMS.size(); ++i ){
			future_terms_indices.add( i );
		}
		
		src.stream().forEach( new Consumer< HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST> > > >() {

			@Override
			public void accept(
					HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> t) {
				
				t.entrySet().stream().forEach( new Consumer< Entry<PVAR_NAME, ArrayList< ArrayList<LCONST>> > >() {

					
					@Override
					public void accept(
							Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> entry ) {

						final String pvarName = entry.getKey()._sPVarName;
						if( pvarName.equals(EmergencyDomainDataReelElement.currentCallPvarName._sPVarName) ||
							pvarName.equals(EmergencyDomainDataReelElement.currentCallTimePvarName._sPVarName) || 
							pvarName.equals(EmergencyDomainDataReelElement.tempUniformRegionPvarName._sPVarName) ||  
							pvarName.equals(EmergencyDomainDataReelElement.tempUniformCausePvarName._sPVarName) ){
							return;
						}
							
						entry.getValue().stream().forEach( new Consumer< ArrayList<LCONST> >() {
							@Override
							public void accept(ArrayList<LCONST> terms) {
								System.out.println(  "CPT_"+ entry.getKey().toString()+"_"+terms );
								PVAR_NAME p = entry.getKey();
								CPF_DEF cpf = null;
								if( rddl_state_vars.containsKey(p) ){
									cpf = rddl_state._hmCPFs.get( new PVAR_NAME( p._sPVarName + "'" ) );
								}else {
									cpf = rddl_state._hmCPFs.get( new PVAR_NAME( p._sPVarName ) );
								}
											
								Map<LVAR, LCONST> subs = getSubs( cpf._exprVarName._alTerms, terms );
								EXPR new_lhs_stationary = cpf._exprVarName.substitute( subs, constants, objects );
								EXPR new_rhs_stationary = cpf._exprEquals.substitute(subs, constants, objects);
											
								EXPR lhs_with_tf = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects)
										.addTerm(future_PREDICATE, constants, objects);
								EXPR rhs_with_tf = new_rhs_stationary.addTerm(TIME_PREDICATE, constants, objects)
										.addTerm(future_PREDICATE, constants, objects);
											
								time_terms_indices.stream().forEach( new Consumer< Integer >() {
									@Override
									public void accept(Integer time_term_index ) {
										EXPR lhs_with_f_temp = null;
										if( rddl_state_vars.containsKey(p) ){
											if( time_term_index == lookahead-1 ){
												return;
											}
											lhs_with_f_temp = lhs_with_tf.substitute(
													Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get( time_term_index + 1 ) ), constants, objects);
										}else{
											lhs_with_f_temp = lhs_with_tf.substitute(
													Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get( time_term_index ) ), constants, objects);
										}
										final EXPR lhs_with_f = lhs_with_f_temp;
										final EXPR rhs_with_f = rhs_with_tf.substitute( 
										Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get( time_term_index ) ), constants, objects);
													
										future_terms_indices.stream().forEach( new Consumer<Integer>() {

											public void accept(Integer future_term_index) {
												EXPR lhs = lhs_with_f.substitute(
														Collections.singletonMap( future_PREDICATE, future_TERMS.get( future_term_index ) ), constants, objects);
												EXPR rhs = rhs_with_f.substitute(
														Collections.singletonMap( future_PREDICATE, future_TERMS.get( future_term_index) ), constants, objects);
																	
												EXPR lhs_future = future_gen.getFuture( lhs, rand, objects );
												EXPR rhs_future = future_gen.getFuture( rhs, rand, objects );
																	
												synchronized ( grb_model ) {
													try {
														GRBVar lhs_var = lhs_future.getGRBConstr( 
															GRB.EQUAL, grb_model, constants, objects, type_map);
														GRBVar rhs_var = rhs_future.getGRBConstr( 
															GRB.EQUAL, grb_model, constants, objects, type_map);
													
														GRBConstr this_constr 
															= grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "CPT_"+p.toString()+"_"+terms+"_"+time_term_index+"_"+future_term_index );
														saved_constr.add( this_constr );
														saved_expr.add(lhs_future);
														saved_expr.add(rhs_future);
													} catch (GRBException e) {
														e.printStackTrace();
														System.exit(1);
													}
												}
											}
										} );
									} 
								} );
							}
						} );
					}
				});
			}
		});
		
		System.out.println("--------------Translating Reward-------------");
		translateReward( );
		translate_time.PauseTimer();
	}
	
	@Override
	protected void translateCPTs(HashMap<PVAR_NAME,HashMap<ArrayList<LCONST>,Object>> subs) throws GRBException {
		
		GRBExpr old_obj = grb_model.getObjective();
		
		ArrayList<HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>>> src 
		= new ArrayList< HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> >();
		src.add( rddl_state_vars ); src.add( rddl_interm_vars ); src.add( rddl_observ_vars );
		
		final RandomDataGenerator rng = this.rand;
		final int numFutures = this.num_futures;
		final int length = this.lookahead;
		
		EmergencyDomainDataReelElement currentElem = new EmergencyDomainDataReelElement(subs);
		ArrayList<EmergencyDomainDataReelElement>[] futures 
			= reel.getFutures(currentElem, rng, numFutures, length, reel.getTrainingFoldIdx() );
		ArrayList<Pair<EXPR, EXPR>> futuresExpressions 
			= reel.to_RDDL_EXPR_constraints(futures, future_PREDICATE, 
				future_TERMS, TIME_PREDICATE, TIME_TERMS, constants, objects);
		for( Pair<EXPR,EXPR> pairFuture : futuresExpressions ){
			final EXPR lhs_future = pairFuture._o1;
			final EXPR rhs_future = pairFuture._o2;
			synchronized ( grb_model ) {
				GRBVar lhs_var = lhs_future.getGRBConstr( 
						GRB.EQUAL, grb_model, constants, objects, type_map);
				GRBVar rhs_var = rhs_future.getGRBConstr( 
						GRB.EQUAL, grb_model, constants, objects, type_map);
				try {
					System.out.println( "Data_"+lhs_future.toString()+"_"+rhs_future.toString() );
					GRBConstr this_constr 
						= grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, 
								"Data_"+lhs_future.toString()+"_"+rhs_future.toString() );
					to_remove_constr.add( this_constr );
					to_remove_expr.add(lhs_future);
					to_remove_expr.add(rhs_future);
				} catch (GRBException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		
		grb_model.setObjective(old_obj);
		grb_model.update();
	}
	
	public Pair<Double,Double> evaluatePlanner( final int numRounds,  final StateViz stateViz, 
			final boolean randomize_test ) throws Exception{
		ArrayList<Double> rewards = new ArrayList<Double>();		
		for( int round = 0; round < numRounds; ++round ){
			double cur_discount = 1.0;
			double accum_reward = 0.0;		
			
			
			rddl_state.init( rddl_domain._hmObjects, rddl_nonfluents != null ? rddl_nonfluents._hmObjects : null, 
					rddl_instance._hmObjects, rddl_domain._hmTypes, rddl_domain._hmPVariables, rddl_domain._hmCPF,
					rddl_instance._alInitState, rddl_nonfluents == null ? null : rddl_nonfluents._alNonFluents, 
					rddl_domain._alStateConstraints, rddl_domain._alActionPreconditions, rddl_domain._alStateInvariants,  
					rddl_domain._exprReward, rddl_instance._nNonDefActions);
//			final int test_start_idx = rand.nextInt( 0, reel.getNumTestInstances()-1-this.rddl_instance._nHorizon );
//			final int test_start_idx = (int)(round * (reel.getNumTestInstances()/numRounds));
//			assert( test_start_idx < reel.getNumTestInstances() && test_start_idx + this.rddl_instance._nHorizon < reel.getNumTestInstances() );
			
			reel.resetTestIndex( 0 ); //test_start_idx );

			EmergencyDomainDataReelElement stored_next_thing = null;
			for( int step = 0; step < this.rddl_instance._nHorizon; ++step ){
				
				if( step == 0 || !randomize_test ){
					//copy back next state of exogenous vars
					EmergencyDomainDataReelElement exo_thing = reel.getNextTestingInstance();
					exo_thing.setInState( rddl_state );
				}else{
					stored_next_thing.setInState( rddl_state );
				}
				System.out.println( "Next State : " + rddl_state );
				
				Timer timer = new Timer();
				ArrayList<PVAR_INST_DEF> rddl_action = getActions(rddl_state);
				
				try {
					System.out.println("------------------------------------");
					System.out.println("State : " + rddl_state );
					System.out.println("Action : " + rddl_action);
					System.out.println("------------------------------------");
					
					if( randomize_test ){
						EmergencyDomainDataReelElement cur_thing = new EmergencyDomainDataReelElement(rddl_state);
						ArrayList<Integer> next_indices = reel.getLeads(cur_thing, reel.getTestingFoldIdx() );
						EmergencyDomainDataReelElement thatElem = reel.getInstance( 
								next_indices.get( rand.nextInt(0, next_indices.size()-1) ), reel.getTestingFoldIdx() );

						//fix date to be not in the past
						LocalDate newCallDate;
						if( thatElem.callTime.isBefore(cur_thing.callTime) ){
							newCallDate = LocalDate.ofYearDay( cur_thing.callDate.getYear(), cur_thing.callDate.getDayOfYear()+1);
						}else{
							newCallDate = LocalDate.ofYearDay( cur_thing.callDate.getYear(), cur_thing.callDate.getDayOfYear()); 
						}
						stored_next_thing = new EmergencyDomainDataReelElement( thatElem.callId, thatElem.natureCode, 
								newCallDate, thatElem.callTime, thatElem.callAddress, thatElem.callX, thatElem.callY, false );
						
						System.out.println( "Current : " + cur_thing );
						System.out.println( "Candidates : " + next_indices );
						System.out.println( "Selected : " + stored_next_thing );
					}
					
					rddl_state.computeNextState(rddl_action, rand);
					System.out.println("Interm State : " + rddl_state );
					System.out.println("------------------------------------");
					outFile.write( 60*getFirstResponse(rddl_state) + "," + 60*getFullResponse(rddl_state) + "," + getOverwhelm(rddl_state) );
					outFile.write("\n");
					outFile.flush();
					if(stateViz != null){
						stateViz.display(rddl_state, step);			
					}
				} catch (Exception ee) {
					System.out.println("FATAL SERVER EXCEPTION:\n" + ee);
					throw ee;
				}
				
				try {
					rddl_state.checkStateActionConstraints(rddl_action);
				} catch (Exception e) {
					System.out.println("TRIAL ERROR -- STATE-ACTION CONSTRAINT VIOLATION:\n" + e);
					throw e;
				}
				
				if (SHOW_TIMING){
					System.out.println("**TIME to compute next state: " + timer.GetTimeSoFarAndReset());
				}
					
				// Calculate reward / objective and store
				final double immediate_reward = ((Number)rddl_domain._exprReward.sample(
						new HashMap<LVAR,LCONST>(),rddl_state, rand)).doubleValue();

				
				accum_reward += cur_discount * immediate_reward;
				cur_discount *= rddl_instance._dDiscount;
				
				if (SHOW_TIMING){
					System.out.println("**TIME to copy observations & update rewards: " + timer.GetTimeSoFarAndReset());
				}
			
				rddl_state.advanceNextState();				
									
				if (SHOW_TIMING){
					System.out.println("**TIME to advance state: " + timer.GetTimeSoFarAndReset());
				}
			}
			outFile.flush();
			System.out.println("Round reward " + accum_reward);
			rewards.add( accum_reward );
			
			cleanUp();
			handleOOM();
			
		}
		
		System.out.println("Round rewards : " + rewards );
		final double session_mean_reward = rewards.stream().mapToDouble(r->r).average().getAsDouble();
		final double stdev = Math.sqrt( (1.0/(numRounds-1))*(rewards.stream()
								.mapToDouble(r->(r-session_mean_reward)*(r-session_mean_reward))
								.sum()) );
		return new Pair<Double,Double>(session_mean_reward, stdev);
	}
	
	public static double getFirstResponse(State s) throws EvalException {
		return ((Number) s.getPVariableAssign(firstResponsePvarName , EmergencyDomainDataReelElement.emptySubstitution)).doubleValue();
	}

	public static double getFullResponse(State s) throws EvalException {
		return ((Number) s.getPVariableAssign(fullResponsePvarName, EmergencyDomainDataReelElement.emptySubstitution)).doubleValue();
	}
	
	public static boolean getOverwhelm(State s) throws EvalException {
		return (boolean) s.getPVariableAssign(overwhelmPvarName, EmergencyDomainDataReelElement.emptySubstitution);
	}

	public static void main(String[] args) throws Exception {
		System.out.println( Arrays.toString( args ) );
		EmergencyDomainHOPTranslate planner = new EmergencyDomainHOPTranslate( 
				Arrays.asList( args ).subList(0, args.length-2) );
		System.out.println( planner.evaluatePlanner(
				Integer.parseInt( args[args.length-2] ), 
				null, //new EmergencyDomainStateViz(1300,30,1500,80), 
				Boolean.parseBoolean( args[ args.length-1 ] ) ) );
	}


}
