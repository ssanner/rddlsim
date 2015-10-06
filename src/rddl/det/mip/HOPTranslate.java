package rddl.det.mip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import gurobi.GRB.DoubleAttr;
import gurobi.GRB.IntAttr;

public class HOPTranslate extends Translate {

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
	
	public HOPTranslate( final String domain_file, final String inst_file, 
			final int lookahead , final double timeout ,
			int n_futures, final FUTURE_SAMPLING sampling,
			final HINDSIGHT_STRATEGY hinsight_strat ) throws Exception, GRBException {
		super( domain_file, inst_file, lookahead, timeout );
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
		
		for( final HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> map : src ){
		
			for( final PVAR_NAME p : map.keySet() ){
				for( final ArrayList<LCONST> terms : map.get( p ) ){
					System.out.println( "CPT for " + p.toString() + terms );
					
					CPF_DEF cpf = null;
					if( rddl_state_vars.containsKey(p) ){
						cpf = rddl_state._hmCPFs.get( new PVAR_NAME( p._sPVarName + "'" ) );
					}else {
						cpf = rddl_state._hmCPFs.get( new PVAR_NAME( p._sPVarName ) );
					}
					
					Map<LVAR, LCONST> subs = getSubs( cpf._exprVarName._alTerms, terms );
					EXPR new_lhs_stationary = cpf._exprVarName.substitute( subs, constants, objects );
					EXPR new_rhs_stationary = cpf._exprEquals.substitute(subs, constants, objects);
					for( int t = 0 ; t < lookahead; ++t ){
						
						EXPR new_lhs_non_stationary = null;
//						GRBVar lhs_var = null;
						
						if( rddl_state_vars.containsKey(p) ){
							if( t == lookahead - 1 ){
								continue;
							}
							//FIXME : stationarity assumption
							new_lhs_non_stationary = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
									.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t+1) ), constants, objects);
//							lhs_var = new_lhs_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
						}else {
							new_lhs_non_stationary = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
									.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects);
//							lhs_var = new_lhs_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
						}
						
						//FIXME : stationarity assumption
						EXPR new_rhs_non_stationary = new_rhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
								.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects );
//						GRBVar rhs_var = new_rhs_non_stationary.getGRBConstr(GRB.EQUAL,  grb_model, constants, objects, type_map);
						
						System.out.println( p + " " + terms + " " + t );
						EXPR lhs_with_future = new_lhs_non_stationary.addTerm(future_PREDICATE, constants, objects);
						EXPR rhs_with_future = new_rhs_non_stationary.addTerm(future_PREDICATE, constants, objects);
						
						for( int future_id = 0 ; future_id < num_futures; ++future_id ){
							EXPR lhs_future_id = lhs_with_future.substitute( 
									Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id)), 
									constants, objects );
							EXPR rhs_future_id = rhs_with_future.substitute( 
									Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id)), 
									constants, objects );
							
							EXPR lhs_future = future_gen.getFuture( lhs_future_id, rand, objects );
							EXPR rhs_future = future_gen.getFuture( rhs_future_id, rand, objects );

							GRBVar lhs_var = lhs_future.getGRBConstr( 
									GRB.EQUAL, grb_model, constants, objects, type_map);
							GRBVar rhs_var = rhs_future.getGRBConstr( 
									GRB.EQUAL, grb_model, constants, objects, type_map);
							
							grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "CPT_t_"+p.toString()+"_"+terms+"_"+t+"_"+future_id );

							saved_expr.add( lhs_future );
							saved_expr.add( rhs_future );

							saved_vars.add( lhs_var );
							saved_vars.add( rhs_var );
							
							if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
								break;
							}
						}
						
						grb_model.update();
						
					}
					
				}
			}
			
		}
		
		grb_model.setObjective(old_obj);
		grb_model.update();

	}
	
	@Override
	protected void translateReward() throws Exception {
		grb_model.setObjective( new GRBLinExpr() );
		grb_model.update();

		EXPR stationary = rddl_state._reward;
		EXPR stationary_clear = stationary.substitute( Collections.EMPTY_MAP, constants, objects);
		EXPR non_stationary = stationary_clear.addTerm( TIME_PREDICATE , constants, objects )
				.addTerm( future_PREDICATE, constants, objects);
		
		GRBLinExpr all_sum = new GRBLinExpr();
		
		for( int future_id = 0 ; future_id < num_futures ; ++future_id ){
			for( int time = 0 ; time < lookahead; ++time ){
				EXPR subs_tf = non_stationary.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(time)), 
						constants, objects)
						.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id)), 
								constants, objects);

				EXPR sampled_reward = future_gen.getFuture(subs_tf, rand, objects);
				
				GRBVar this_future_var = sampled_reward.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
				
				saved_expr.add( sampled_reward );
				saved_vars.add( this_future_var );
				//IDEA : weightby probability of trajecotry of futures
				//what is the probability of a determinization 
				all_sum.addTerm( 1.0d/num_futures, this_future_var );
			}
			if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
				break;
			}
		}
		
		grb_model.setObjective(all_sum);
		grb_model.update();
	}
	
	@Override
	protected void translateInitialState(ArrayList<PVAR_INST_DEF> initState)
			throws GRBException {
		
		Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> subs = getConsts( initState );
		
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
				GRBVar rhs_var = rhs_expr.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
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
		
		for( final EXPR e : constraints ){
			System.out.println( "Translating Constraint " + e );
			
//			substitution expands quantifiers
//			better to substitute for time first
			EXPR non_stationary_e = e.substitute( Collections.EMPTY_MAP, constants, objects)
					.addTerm(TIME_PREDICATE, constants, objects )
					.addTerm(future_PREDICATE, constants, objects);
//			this works. but is expensive
//			QUANT_EXPR all_time = new QUANT_EXPR( QUANT_EXPR.FORALL, 
//					new ArrayList<>( Collections.singletonList( new LTYPED_VAR( TIME_PREDICATE._sVarName,  TIME_TYPE._STypeName ) ) )
//							, non_stationary_e );
			
			
			//domain constraints are true for all times and futures 
			for( int t = 0 ; t < TIME_TERMS.size(); ++t ){
				for( int future_id = 0 ; future_id < num_futures; ++future_id ){
					EXPR this_tf = non_stationary_e
							.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects )
						 	.substitute( Collections.singletonMap( future_PREDICATE, future_TERMS.get(future_id) ), constants, objects );
				
					GRBVar constrained_var = this_tf.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
					grb_model.addConstr( constrained_var, GRB.EQUAL, 1, "constraint=1_"+e.toString()+"_t"+t+"_f" + future_id );

					saved_expr.add( this_tf ); saved_vars.add( constrained_var );

					if( future_gen.equals( FUTURE_SAMPLING.MEAN ) ){
						break;
					}
				}
			}
		}
		
		//hindishgt constraint
		getHindSightConstraintExpr(hindsight_method).forEach( new Consumer<EXPR>() {
			@Override
			public void accept(EXPR t) {
				GRBVar gvar = t.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
				try {
					grb_model.addConstr( gvar, GRB.EQUAL, 1, "hindsight_constraint_"+t.toString() );
					saved_expr.add( t ); saved_vars.add( gvar );
				} catch (GRBException e) {
					e.printStackTrace();
					System.exit(1);
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
		System.out.println( 
				new HOPTranslate(args[0], args[1], Integer.parseInt( args[2] ), Double.parseDouble( args[3] ), 
						Integer.parseInt( args[4] ), FUTURE_SAMPLING.valueOf( args[5] ), 
						HINDSIGHT_STRATEGY.valueOf( args[6] ) )
				.doPlanInitState() );
	}
	
	public Map< EXPR, Double >  doPlan( final ArrayList<PVAR_INST_DEF> initState ) throws Exception{
		Map< EXPR, Double > ret = new HashMap< EXPR, Double >();

		translate_time.ResumeTimer();
		System.out.println("--------------Translating CPTs-------------");
		translateCPTs( );
		System.out.println("--------------Translating Constraints-------------");
		translateConstraints( );
		System.out.println("--------------Translating Reward-------------");
		translateReward( );
		System.out.println("--------------Initial State-------------");
		translateInitialState( initState );
		translate_time.PauseTimer();
		
//		System.out.println( "Names : " );
//		RDDL.EXPR.name_map.forEach( (a,b) -> System.out.println( a + " " + b ) );
		
//		grb_model.set( GRB.IntParam.SolutionLimit, 1 );
		
		grb_model.update();
		
		System.out.println("Optimizing.............");
		
		grb_model.optimize();
		
		if( grb_model.get( IntAttr.Status ) == GRB.INFEASIBLE ){
//			while (true) {
				grb_model.computeIIS();
		        System.out.println("\nThe following constraint cannot be satisfied:");
		        for (GRBConstr c : grb_model.getConstrs()) {
		          if (c.get(GRB.IntAttr.IISConstr) == 1) {
		        	String constr = c.get(GRB.StringAttr.ConstrName);
		        	
		        	System.out.println( constr + " " + EXPR.reverse_name_map.get( constr ) );
			            // Remove a single constraint from the model
		//	            removed.add(c.get(GRB.StringAttr.ConstrName));
		//	            grb_model.remove(c);
		//	            break;
		          }
		        }
//			}
		}else if( grb_model.get( IntAttr.Status ) == GRB.UNBOUNDED ){
			System.out.println(  "Unbounded Ray : " + grb_model.get( DoubleAttr.UnbdRay ) );
		}else{
			try{
				rddl_action_vars.forEach( new BiConsumer<PVAR_NAME, ArrayList<ArrayList<LCONST> > >( ) {

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
								   ret.put( action_var, grb_var.get( DoubleAttr.X ) );
							   } catch (GRBException e) {
									e.printStackTrace();
							   }
								  
							}
						});
					}
					
				});
				
				System.out.println( "Maximum (unscaled) bound violation : " +  + grb_model.get( DoubleAttr.BoundVio	) );
				System.out.println("Sum of (unscaled) constraint violations : " + grb_model.get( DoubleAttr.ConstrVioSum ) );
				System.out.println("Maximum integrality violation : "+ grb_model.get( DoubleAttr.IntVio ) );
				System.out.println("Sum of integrality violations : " + grb_model.get( DoubleAttr.IntVioSum ) );
				System.out.println("Objective value : " + grb_model.get( DoubleAttr.ObjVal ) );
			}catch( Exception exc ){
				exc.printStackTrace();
				dumpAllAssignments();
			}
		}
//		outputLPFile( );

		System.out.println( "Status : "+ grb_model.get( IntAttr.Status ) + "(Optimal/Inf/Unb: " + GRB.OPTIMAL + ", " + GRB.INFEASIBLE +", " + GRB.UNBOUNDED + ")" );
		System.out.println( "Number of solutions found : " + grb_model.get( IntAttr.SolCount ) );
		System.out.println( "Number of simplex iterations performed in most recent optimization : " + grb_model.get( DoubleAttr.IterCount ) );	
		System.out.println( "Number of branch-and-cut nodes explored in most recent optimization : " + grb_model.get( DoubleAttr.NodeCount ) );

//		System.out.println("Maximum (unscaled) primal constraint error : " + grb_model.get( DoubleAttr.ConstrResidual ) );	

//		System.out.println("Sum of (unscaled) primal constraint errors : " + grb_model.get( DoubleAttr.ConstrResidualSum ) );
//		System.out.println("Maximum (unscaled) dual constraint error : " + grb_model.get( DoubleAttr.DualResidual ) ) ;
//		System.out.println("Sum of (unscaled) dual constraint errors : " + grb_model.get( DoubleAttr.DualResidualSum ) );

		System.out.println( "#Variables : "+ grb_model.get( IntAttr.NumVars ) );
		System.out.println( "#Integer variables : "+ grb_model.get( IntAttr.NumIntVars ) );
		System.out.println( "#Binary variables : "+ grb_model.get( IntAttr.NumBinVars ) );
		System.out.println( "#Constraints : "+ grb_model.get( IntAttr.NumConstrs ) );
		System.out.println( "#NumPWLObjVars : "+ grb_model.get( IntAttr.NumPWLObjVars ) );
				
		System.out.println("#State Vars : " + string_state_vars.size() );
		System.out.println("#Action Vars : " + string_action_vars.size() );
		System.out.println("Optimization Runtime(mins) : " + grb_model.get( DoubleAttr.Runtime )/60d );
		System.out.println("Translation time(mins) : " + translate_time.GetTimeSoFarInMinutes() );		
		
		saved_expr.clear(); saved_vars.clear();
		grb_model.dispose();
		
		return ret;
	}
	
}
