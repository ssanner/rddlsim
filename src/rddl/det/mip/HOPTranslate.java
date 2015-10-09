package rddl.det.mip;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.EvalException;
import rddl.RDDL;
import rddl.RDDL.OPER_EXPR;
import rddl.RDDL.QUANT_EXPR;
import rddl.State;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.BOOL_EXPR;
import rddl.RDDL.COMP_EXPR;
import rddl.RDDL.CPF_DEF;
import rddl.RDDL.EXPR;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.PVAR_EXPR;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.RDDL.TYPE_NAME;
import rddl.policy.Policy;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import gurobi.GRB.DoubleAttr;
import gurobi.GRB.IntAttr;

public class HOPTranslate extends Translate implements Policy {

	public static enum FUTURE_SAMPLING{
		SAMPLE {
			@Override
			public EXPR getFuture(EXPR e, RandomDataGenerator rand, Map<TYPE_NAME, OBJECTS_DEF> objects ) {
				return e.sampleDeterminization(rand);
			}
		}, MEAN {
			@Override
			public EXPR getFuture(EXPR e, RandomDataGenerator rand, Map<TYPE_NAME, OBJECTS_DEF> objects ) {
				return e.getMean(objects);
			}
		};
		
		public abstract EXPR getFuture( final EXPR e , final RandomDataGenerator rand, 
				Map<TYPE_NAME, OBJECTS_DEF> objects );
	}
	private int num_futures = 0;
	private FUTURE_SAMPLING future_gen;
	private RandomDataGenerator rand;
	
	
	protected static final LVAR future_PREDICATE = new LVAR( "?future" );
	private static final TYPE_NAME future_TYPE = new TYPE_NAME( "future" );
	protected ArrayList< LCONST > future_TERMS = new ArrayList<>();
	protected enum HINDSIGHT_STRATEGY { 
		ROOT, ALL_ACTIONS
	}
	private HINDSIGHT_STRATEGY hindsight_method;
	
	public void HOPTranslateInit( final String domain_file, final String inst_file, 
			final int lookahead , final double timeout ,
			int n_futures, final FUTURE_SAMPLING sampling,
			final HINDSIGHT_STRATEGY hinsight_strat ) throws Exception, GRBException {
		this.num_futures = n_futures;
		this.future_gen = sampling;
		rand = new RandomDataGenerator( );
		
		for( int f = 0 ; f < this.num_futures; ++f ){
			future_TERMS.add( new RDDL.OBJECT_VAL( "future" + f ) );
			if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
				break;
			}
		}
		objects.put( future_TYPE,  new OBJECTS_DEF(  future_TYPE._STypeName, future_TERMS ) );
		if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
			num_futures = 1;
		}
		this.hindsight_method =  hinsight_strat;
	}
	
	@Override
	protected void translateCPTs() throws GRBException {
		
		GRBExpr old_obj = grb_model.getObjective();
		
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
		
		src.parallelStream().forEach( new Consumer< HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST> > > >() {

			@Override
			public void accept(
					HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> t) {
				t.entrySet().parallelStream().forEach( new Consumer< Entry<PVAR_NAME, ArrayList< ArrayList<LCONST>> > >() {

					@Override
					public void accept(
							Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> entry ) {
						entry.getValue().parallelStream().forEach( new Consumer< ArrayList<LCONST> >() {
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
								
								time_terms_indices.parallelStream().forEach( new Consumer< Integer >() {
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
										
										future_terms_indices.parallelStream().forEach( 
												new Consumer<Integer>() {
													public void accept(Integer future_term_index) {
														EXPR lhs = lhs_with_f.substitute(
																Collections.singletonMap( future_PREDICATE, future_TERMS.get( future_term_index ) ), constants, objects);
														EXPR rhs = rhs_with_f.substitute(
																Collections.singletonMap( future_PREDICATE, future_TERMS.get( future_term_index) ), constants, objects);
														
														EXPR lhs_future = future_gen.getFuture( lhs, rand, objects );
														EXPR rhs_future = future_gen.getFuture( rhs, rand, objects );
														
//														synchronized ( lhs_future ) {
//															synchronized ( rhs_future ) {
																synchronized ( grb_model ) {
																	
																	GRBVar lhs_var = lhs_future.getGRBConstr( 
																			GRB.EQUAL, grb_model, constants, objects, type_map);
																	GRBVar rhs_var = rhs_future.getGRBConstr( 
																			GRB.EQUAL, grb_model, constants, objects, type_map);
																	
																	try {
																		grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "CPT_"+p.toString()+"_"+terms+"_"+time_term_index+"_"+future_term_index );
																	} catch (GRBException e) {
																		e.printStackTrace();
																		System.exit(1);
																	}

																	saved_expr.add( lhs_future );
																	saved_expr.add( rhs_future );

																	saved_vars.add( lhs_var );
																	saved_vars.add( rhs_var );
																}
//															}
//														}
														
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
								
//		for( final HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> map : src ){
//		
//			for( final PVAR_NAME p : map.keySet() ){
//				for( final ArrayList<LCONST> terms : map.get( p ) ){
//					
//					
//					
//					
//					for( int t = 0 ; t < lookahead; ++t ){
//						
//						EXPR new_lhs_non_stationary = null;
////						GRBVar lhs_var = null;
//						
//						if( rddl_state_vars.containsKey(p) ){
//							if( t == lookahead - 1 ){
//								continue;
//							}
//							//FIXME : stationarity assumption
//							new_lhs_non_stationary = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
//									.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t+1) ), constants, objects);
////							lhs_var = new_lhs_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
//						}else {
//							new_lhs_non_stationary = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
//									.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects);
////							lhs_var = new_lhs_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
//						}
//						
//						//FIXME : stationarity assumption
//						EXPR new_rhs_non_stationary = new_rhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
//								.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects );
////						GRBVar rhs_var = new_rhs_non_stationary.getGRBConstr(GRB.EQUAL,  grb_model, constants, objects, type_map);
//						
//						System.out.println( p + " " + terms + " " + t );
//						EXPR lhs_with_future = new_lhs_non_stationary.addTerm(future_PREDICATE, constants, objects);
//						EXPR rhs_with_future = new_rhs_non_stationary.addTerm(future_PREDICATE, constants, objects);
//						
//						for( int future_id = 0 ; future_id < num_futures; ++future_id ){
//							EXPR lhs_future_id = lhs_with_future.substitute( 
//									Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id)), 
//									constants, objects );
//							EXPR rhs_future_id = rhs_with_future.substitute( 
//									Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id)), 
//									constants, objects );
//							
//							EXPR lhs_future = future_gen.getFuture( lhs_future_id, rand, objects );
//							EXPR rhs_future = future_gen.getFuture( rhs_future_id, rand, objects );
//
//							GRBVar lhs_var = lhs_future.getGRBConstr( 
//									GRB.EQUAL, grb_model, constants, objects, type_map);
//							GRBVar rhs_var = rhs_future.getGRBConstr( 
//									GRB.EQUAL, grb_model, constants, objects, type_map);
//							
//							grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "CPT_t_"+p.toString()+"_"+terms+"_"+t+"_"+future_id );
//
//							saved_expr.add( lhs_future );
//							saved_expr.add( rhs_future );
//
//							saved_vars.add( lhs_var );
//							saved_vars.add( rhs_var );
//							
//							if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
//								break;
//							}
//						}
//						
//						grb_model.update();
//						
//					}
//					
//				}
//			}
//			
//		}
		
		grb_model.setObjective(old_obj);
		grb_model.update();

	}
	
	@Override
	protected void translateReward() throws Exception {
		grb_model.setObjective( new GRBLinExpr() );
		grb_model.update();

		final EXPR stationary = rddl_state._reward;
		final EXPR stationary_clear = stationary.substitute( Collections.EMPTY_MAP, constants, objects);
		final EXPR non_stationary = stationary_clear.addTerm( TIME_PREDICATE , constants, objects )
				.addTerm( future_PREDICATE, constants, objects);
		
		GRBLinExpr all_sum = new GRBLinExpr();
		
		future_TERMS.parallelStream().forEach( new Consumer<LCONST>() {
			@Override
			public void accept(LCONST future_term) {
				TIME_TERMS.parallelStream().forEach( new Consumer<LCONST>() {
					@Override
					public void accept(LCONST time_term) {
						final EXPR subs_tf = non_stationary.substitute( Collections.singletonMap( TIME_PREDICATE, time_term ), 
								constants, objects)
								.substitute( Collections.singletonMap( future_PREDICATE, future_term ),constants, objects);
						System.out.println( "Reward_" + time_term + "_" + future_term );
						
						synchronized( grb_model ){
							GRBVar this_future_var = subs_tf.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
							
							saved_expr.add( subs_tf );
							saved_vars.add( this_future_var );
							//IDEA : weightby probability of trajecotry of futures
							//what is the probability of a determinization 
							all_sum.addTerm( 1.0d/num_futures, this_future_var );
						}
					}
				});
			}
		});
		
//		for( int future_id = 0 ; future_id < num_futures ; ++future_id ){
//			for( int time = 0 ; time < lookahead; ++time ){
//				EXPR subs_tf = non_stationary.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(time)), 
//						constants, objects)
//						.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id)), 
//								constants, objects);
//
//				GRBVar this_future_var = subs_tf.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
//				
//				saved_expr.add( subs_tf );
//				saved_vars.add( this_future_var );
//				//IDEA : weightby probability of trajecotry of futures
//				//what is the probability of a determinization 
//				all_sum.addTerm( 1.0d/num_futures, this_future_var );
//			}
//			if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
//				break;
//			}
//		}
		
		grb_model.setObjective(all_sum);
		grb_model.update();
	}
	
	@Override
	protected void translateInitialState( HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs )
			throws GRBException {
		
		GRBExpr old_obj = grb_model.getObjective();
		
		for( final PVAR_NAME p : rddl_state_vars.keySet() ){
			for( final ArrayList<LCONST> terms : rddl_state_vars.get( p ) ){
				Object rhs = null;
				if( subs.containsKey( p ) && subs.get( p ).containsKey( terms ) ){
					rhs = subs.get(p).get( terms );
				}else{
					rhs = rddl_state.getDefaultValue(p);
				}

				EXPR rhs_expr = null;
				if( rhs instanceof Boolean ){
					rhs_expr = new BOOL_CONST_EXPR( (boolean) rhs );
				}else if( rhs instanceof Double ){
					rhs_expr = new REAL_CONST_EXPR( (double)rhs );
				}else if( rhs instanceof Integer ){
					rhs_expr = new INT_CONST_EXPR( (int)rhs );
				}
				GRBVar rhs_var = rhs_expr.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map );
				saved_vars.add( rhs_var ); saved_expr.add( rhs_expr );
				
				PVAR_EXPR stationary_pvar_expr = new PVAR_EXPR( p._sPVarName, terms );
				EXPR non_stationary_pvar_expr = stationary_pvar_expr
						.addTerm( TIME_PREDICATE, constants, objects )
						.addTerm( future_PREDICATE, constants, objects);
				
				for( int future_id = 0 ; future_id < num_futures; ++future_id ){
					EXPR this_future_init_state = non_stationary_pvar_expr
					.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(0) ) , constants, objects)
					.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id) ), constants, objects);
				
					GRBVar lhs_var = this_future_init_state.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
					grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "initState_"+p.toString()+terms+"f_"+future_id );
					
					saved_vars.add( lhs_var ); saved_expr.add( this_future_init_state );
					
					if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
						break;
					}
				}
			}
		}
		
		grb_model.setObjective(old_obj);
		grb_model.update();
	}
	
	@Override
	protected void translateConstraints() throws Exception {

		GRBExpr old_obj = grb_model.getObjective();
//		translateMaxNonDef( );
		System.out.println("--------------Translating Constraints-------------");
		
		ArrayList<BOOL_EXPR> constraints = new ArrayList<BOOL_EXPR>();
		//domain constraints 
		constraints.addAll( rddl_state._alActionPreconditions ); constraints.addAll( rddl_state._alStateInvariants );
		
		constraints.parallelStream().forEach( new Consumer< BOOL_EXPR >() {
			@Override
			public void accept(BOOL_EXPR e) {
				System.out.println( "Translating Constraint " + e );	
				final EXPR non_stationary_e = e.substitute( Collections.EMPTY_MAP, constants, objects)
						.addTerm(TIME_PREDICATE, constants, objects )
						.addTerm(future_PREDICATE, constants, objects);
				
				TIME_TERMS.parallelStream().forEach( new Consumer< LCONST >() {
					@Override
					public void accept(LCONST time_term ) {
						future_TERMS.parallelStream().forEach( new Consumer<LCONST>() { 
							@Override
							public void accept(LCONST future_term ) {
								final EXPR this_tf = non_stationary_e
										.substitute( Collections.singletonMap( TIME_PREDICATE, time_term ), constants, objects )
									 	.substitute( Collections.singletonMap( future_PREDICATE, future_term ), constants, objects );
								synchronized( grb_model ){
									try {
										GRBVar constrained_var = this_tf.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
										grb_model.addConstr( constrained_var, GRB.EQUAL, 1, "constraint=1_"+e.toString()+"_"+
												time_term+"_" + future_term );
										saved_expr.add( this_tf ); saved_vars.add( constrained_var );
									} catch (GRBException e) {
										e.printStackTrace();
										System.exit(1);
									}
								}
							}
						});
					}
				});
			}
		});
		
		
//		for( final EXPR e : constraints ){
//			System.out.println( "Translating Constraint " + e );
//			
////			substitution expands quantifiers
////			better to substitute for time first
//			EXPR non_stationary_e = e.substitute( Collections.EMPTY_MAP, constants, objects)
//					.addTerm(TIME_PREDICATE, constants, objects )
//					.addTerm(future_PREDICATE, constants, objects);
////			this works. but is expensive
////			QUANT_EXPR all_time = new QUANT_EXPR( QUANT_EXPR.FORALL, 
////					new ArrayList<>( Collections.singletonList( new LTYPED_VAR( TIME_PREDICATE._sVarName,  TIME_TYPE._STypeName ) ) )
////							, non_stationary_e );
//			
//			
//			//domain constraints are true for all times and futures 
//			for( int t = 0 ; t < TIME_TERMS.size(); ++t ){
//				for( int future_id = 0 ; future_id < num_futures; ++future_id ){
//					EXPR this_tf = non_stationary_e
//							.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects )
//						 	.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id) ), constants, objects );
//				
//					GRBVar constrained_var = this_tf.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
//					grb_model.addConstr( constrained_var, GRB.EQUAL, 1, "constraint=1_"+e.toString()+"_t"+t+"_f" + future_id );
//
//					saved_expr.add( this_tf ); saved_vars.add( constrained_var );
//
//					if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
//						break;
//					}
//				}
//			}
//		}
		
		//hindishgt constraint
		getHindSightConstraintExpr(hindsight_method).parallelStream().forEach( new Consumer< BOOL_EXPR >() {
			@Override
			public void accept( BOOL_EXPR t) {
				synchronized( grb_model ){
					GRBVar gvar = t.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
					try {
						grb_model.addConstr( gvar, GRB.EQUAL, 1, "hindsight_constraint_"+t.toString() );
						saved_expr.add( t ); saved_vars.add( gvar );
					} catch (GRBException e) {
						e.printStackTrace();
						System.exit(1);
					}					
				}
			}
		});
		
		grb_model.setObjective(old_obj);
		grb_model.update();
		
	}
	
	private ArrayList<BOOL_EXPR> getHindSightConstraintExpr( HINDSIGHT_STRATEGY hindsight_method ) {
		ArrayList<BOOL_EXPR> ret = new ArrayList<BOOL_EXPR>();
		
		rddl_action_vars.forEach( new BiConsumer<PVAR_NAME, ArrayList<ArrayList<LCONST>>> () {
			public void accept( PVAR_NAME pvar , ArrayList<ArrayList<LCONST>> u) {
				u.forEach( new Consumer< ArrayList<LCONST>>() {
					@Override
					public void accept(ArrayList<LCONST> terms) {
						PVAR_EXPR pvar_expr = new PVAR_EXPR( pvar._sPVarName, terms );
						EXPR with_tf = pvar_expr.addTerm(TIME_PREDICATE, constants, objects)
								.addTerm(future_PREDICATE, constants, objects);
						
						for( final LCONST time : TIME_TERMS ){
							EXPR this_t = with_tf.substitute( Collections.singletonMap(TIME_PREDICATE, time ), constants, objects);
							EXPR ref_expr = this_t.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(0) ), constants, objects);
							
							for( final LCONST future : future_TERMS ){
								EXPR addedd = this_t.substitute( Collections.singletonMap( future_PREDICATE, future), constants, objects);
								ret.add( new COMP_EXPR( ref_expr, addedd, COMP_EXPR.EQUAL ) );
							}
							
							if( hindsight_method.equals( HINDSIGHT_STRATEGY.ROOT ) ){
								break;
							}
						}
					}
				});
			};
		});
		return ret;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println( Arrays.toString( args ) );
		System.out.println( new HOPTranslate( Arrays.asList( args ) ).doPlanInitState() );
	}
	
	public HOPTranslate( List<String> args ) throws Exception{
//		super.TranslateInit( domain_file, inst_file, lookahead, timeout );
		super( args.subList(0, 4) );
		System.out.println( args );
		HOPTranslateInit( args.get( 0 ), args.get( 1 ), Integer.parseInt( args.get( 2 ) ), Double.parseDouble( args.get( 3 ) ), 
				Integer.parseInt( args.get( 4 ) ), FUTURE_SAMPLING.valueOf( args.get( 5 ) ), 
				HINDSIGHT_STRATEGY.valueOf( args.get( 6 ) ) );
	}
	
	@Override
	protected Map< EXPR, Double > outputResults(){
		
		DecimalFormat df = new DecimalFormat("#.#####"); df.setRoundingMode( RoundingMode.DOWN );
		Map< EXPR, Double > ret = new HashMap< EXPR, Double >();
		
		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> src = new HashMap<>();
		src.putAll( rddl_action_vars );
		src.putAll( rddl_interm_vars );
		src.putAll( rddl_state_vars );
		
		src.forEach( new BiConsumer<PVAR_NAME, ArrayList<ArrayList<LCONST> > >( ) {

			@Override
			public void accept(PVAR_NAME pvar,
					ArrayList<ArrayList<LCONST>> u) {
				u.forEach( new Consumer< ArrayList<LCONST> >( ) {
					@Override
					public void accept(ArrayList<LCONST> terms ) {
						EXPR action_var = new PVAR_EXPR( pvar._sPVarName, terms )
							.addTerm(TIME_PREDICATE, constants, objects)
							.addTerm(future_PREDICATE, constants, objects)
							.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(0) ), constants, objects)
							.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(0) ) , constants, objects);
						
					  try {
						   GRBVar grb_var = EXPR.grb_cache.get( action_var );
						   assert( grb_var != null );
						   ret.put( action_var, 
								   Double.valueOf( df.format( grb_var.get( DoubleAttr.X ) ) ) );
					   } catch (GRBException e) {
							e.printStackTrace();
					   }
						  
					}
				});
			}
			
		});
		
		return ret;
	}
	
	@Override
	protected ArrayList<PVAR_INST_DEF> getRootActions(Map<EXPR, Double> ret_expr) {
		final ArrayList<PVAR_INST_DEF> ret = new ArrayList<PVAR_INST_DEF>();
		
		rddl_action_vars.entrySet().parallelStream().forEach( new Consumer< Map.Entry< PVAR_NAME, ArrayList<ArrayList<LCONST>> > >() {
			@Override
			public void accept( Map.Entry< PVAR_NAME , ArrayList<ArrayList<LCONST>> > entry ) {
				final PVAR_NAME pvar = entry.getKey();
				entry.getValue().parallelStream().forEach( new Consumer< ArrayList<LCONST> >() {
					@Override
					public void accept(ArrayList<LCONST> terms ) {
						final EXPR lookup = new PVAR_EXPR( pvar._sPVarName, terms )
							.addTerm(TIME_PREDICATE, constants, objects)
							.addTerm(future_PREDICATE, constants, objects)
							.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(0) ), constants, objects)
							.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(0) ) , constants, objects);
						assert( ret_expr.containsKey( lookup ) );
						synchronized( ret ){
							ret.add( new PVAR_INST_DEF( pvar._sPVarName, ret_expr.get( lookup ), terms ) );	
						}
						
					}
				});
			}
		});
		
		return ret;
	}
	
}