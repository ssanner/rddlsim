package rddl.det.mip;

import gurobi.GRB;
import gurobi.GRB.DoubleAttr;
import gurobi.GRB.DoubleParam;
import gurobi.GRB.IntAttr;
import gurobi.GRB.IntParam;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import rddl.EvalException;
import rddl.RDDL;
import rddl.RDDL.AGG_EXPR;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.BOOL_EXPR;
import rddl.RDDL.COMP_EXPR;
import rddl.RDDL.CONN_EXPR;
import rddl.RDDL.CPF_DEF;
import rddl.RDDL.DOMAIN;
import rddl.RDDL.DiracDelta;
import rddl.RDDL.EXPR;
import rddl.RDDL.INSTANCE;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.KronDelta;
import rddl.RDDL.LCONST;
import rddl.RDDL.LTERM;
import rddl.RDDL.LTYPED_VAR;
import rddl.RDDL.LVAR;
import rddl.RDDL.NONFLUENTS;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.OPER_EXPR;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_EXPR;
import rddl.RDDL.PVAR_INST_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.QUANT_EXPR;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.RDDL.TYPE_NAME;
import rddl.State;
import rddl.parser.ParseException;
import rddl.policy.Policy;
import rddl.viz.StateViz;
import util.Pair;
import util.Timer;

//FIXME : why is Policy an abstract class and not an interface ?
public class Translate implements Policy { //  extends rddl.policy.Policy {

	private static final int GRB_INFUNBDINFO = 1;
	private static final int GRB_DUALREDUCTIONS = 0;
	private static final double GRB_MIPGAP = 0.01;
	private static final double GRB_HEURISTIC = 0.2;
	protected static final LVAR TIME_PREDICATE = new LVAR( "?time" );
	private static final TYPE_NAME TIME_TYPE = new TYPE_NAME( "time" );
	private static final boolean OUTPUT_LP_FILE = false;
	private double TIME_LIMIT_MINS = 10; 
	
	private RDDL rddl_obj;
	protected int lookahead;
	protected State rddl_state;
	private DOMAIN rddl_domain;
	private INSTANCE rddl_instance;
	private NONFLUENTS rddl_nonfluents;
	private String instance_name;
	private String domain_name;
	private String GRB_log;
	protected HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> rddl_state_vars;
	protected HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> rddl_action_vars;
	protected HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> rddl_observ_vars;
	protected HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> rddl_interm_vars;
	
	protected List<String> string_state_vars;
	protected List<String> string_action_vars;
	private List<String> string_observ_vars;
	private List<String> string_interm_vars;
	private GRBEnv grb_env;
	protected GRBModel grb_model;
	private HashMap<PVAR_NAME, TYPE_NAME> pred_type = new HashMap<>();
	
//	private HashMap<String, GRBVar> grb_string_map  
//		= new HashMap<String, GRBVar>();
//	private HashMap<String, Pair> rddl_string_map 
//		= new HashMap<String,Pair>();
	
	private String OUTPUT_FILE = "model.lp";
	protected HashMap<TYPE_NAME, OBJECTS_DEF> objects;
	protected Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants = new HashMap<>();
	protected ArrayList< LCONST > TIME_TERMS = new ArrayList<>();
	protected HashMap<PVAR_NAME,  Character> type_map = new HashMap<>();
	protected List<EXPR> saved_expr = new ArrayList<RDDL.EXPR>();
	protected List<GRBVar> saved_vars = new ArrayList<GRBVar>();
	protected Timer translate_time;
	private StateViz viz;
	
	//pseudoconstructor - only one constructor allowed for rddl client
	protected void TranslateInit( final String domain_file, final String inst_file, 
			final int lookahead , final double timeout, final StateViz viz ) throws Exception, GRBException {
		this.viz = viz;
		TIME_LIMIT_MINS = timeout;
		
		initializeRDDL(domain_file, inst_file);
		initializeGRB( );
		
		this.lookahead = lookahead;
		
		objects = new HashMap<>( rddl_instance._hmObjects );
		if( rddl_nonfluents != null && rddl_nonfluents._hmObjects != null ){
			objects.putAll( rddl_nonfluents._hmObjects );
		}
		
		getConstants( );
		
		for( int t = 0 ; t < lookahead; ++t ){
			TIME_TERMS.add( new RDDL.OBJECT_VAL( "time" + t ) );
		}
		objects.put( TIME_TYPE,  new OBJECTS_DEF(  TIME_TYPE._STypeName, TIME_TERMS ) );
		
		for( Entry<PVAR_NAME,PVARIABLE_DEF> entry : rddl_state._hmPVariables.entrySet() ){
			final TYPE_NAME rddl_type = entry.getValue()._typeRange;
			final char grb_type = rddl_type.equals( TYPE_NAME.BOOL_TYPE ) ? GRB.BINARY : 
				rddl_type.equals( TYPE_NAME.INT_TYPE ) ? GRB.INTEGER : GRB.CONTINUOUS;
			type_map.put( entry.getKey(), grb_type );
		}
		
		System.out.println("----------- Types ---------- ");
		type_map.forEach( (a,b) -> System.out.println(a + " " + b) );

		translate_time = new Timer();
		translate_time.PauseTimer();

	}
	
	public Map<EXPR, Double> doPlanInitState( ) throws Exception{
		return doPlan( rddl_instance._alInitState ); 
	}

	public Map< EXPR, Double > doPlan(  HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs ) throws Exception{

		prepareModel( subs );
		goOptimize();
		Map< EXPR, Double > ret = outputResults();
		if( OUTPUT_LP_FILE ) {
			outputLPFile( );
		}
		modelSummary();		
		cleanUp();
		return ret;
	}
	
	public Map< EXPR, Double >  doPlan( final ArrayList<PVAR_INST_DEF> initState ) throws Exception{

//		System.out.println( "Names : " );
//		RDDL.EXPR.name_map.forEach( (a,b) -> System.out.println( a + " " + b ) );
//		grb_model.set( GRB.IntParam.SolutionLimit, 1 );
		prepareModel( initState );
		goOptimize();
		Map< EXPR, Double > ret = outputResults();
		if( OUTPUT_LP_FILE ) {
			outputLPFile( );
		}
		
		modelSummary();		
		cleanUp();
		return ret;
	}

	protected void modelSummary() throws GRBException {
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
	}

	protected void cleanUp() throws GRBException {
		saved_expr.clear(); saved_vars.clear();
		RDDL.EXPR.cleanUpGRB();
		grb_model.dispose();
		grb_model = null; 
		grb_env.dispose();
		grb_env = null;
		initializeGRB();
	}
	
	private void prepareModel(  HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs ) throws Exception {
		translate_time.ResumeTimer();
		prepareModel();
		System.out.println("--------------Initial State-------------");
		translateInitialState( subs );
		translate_time.PauseTimer();		
	}
		
	
	private void prepareModel( ArrayList<PVAR_INST_DEF> initState ) throws Exception {
		translate_time.ResumeTimer();
		prepareModel();
		System.out.println("--------------Initial State-------------");
		translateInitialState( initState );
		translate_time.PauseTimer();		
	}
	
	private void prepareModel( ) throws Exception{
		System.out.println("--------------Translating CPTs-------------");
		translateCPTs( );
		System.out.println("--------------Translating Constraints-------------");
		translateConstraints( );
		System.out.println("--------------Translating Reward-------------");
		translateReward( );
	}

	protected Map<EXPR, Double> outputResults() {
		HashMap<EXPR, Double> ret = new HashMap< EXPR, Double >();

		try{
			System.out.println("---------- Interm trajectory ----------");
			for( int time = 0; time < lookahead; ++time ){
				ret.putAll( getAssignments( rddl_interm_vars, time ) ); 
			}
			
			System.out.println("---------- Output trajectory ----------");
			for( int time = 0; time < lookahead; ++time ){
				ret.putAll( getAssignments( rddl_state_vars, time ) );
			}
			
			System.out.println("---------- Output action assignments  ----------");
			for( int time = 0; time < lookahead-1; ++time ){
				ret.putAll( getAssignments( rddl_action_vars, time ) );
			}

			System.out.println( "Maximum (unscaled) bound violation : " +  + grb_model.get( DoubleAttr.BoundVio	) );
			System.out.println("Sum of (unscaled) constraint violations : " + grb_model.get( DoubleAttr.ConstrVioSum ) );
			System.out.println("Maximum integrality violation : "+ grb_model.get( DoubleAttr.IntVio ) );
			System.out.println("Sum of integrality violations : " + grb_model.get( DoubleAttr.IntVioSum ) );
			System.out.println("Objective value : " + grb_model.get( DoubleAttr.ObjVal ) );
		}catch( Exception exc ){
			exc.printStackTrace();
			dumpAllAssignments();
		}		
		return ret;
	}

	private void goOptimize() throws GRBException {

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
		}
		
	}

	protected void dumpAllAssignments() {
		try {
			FileWriter file_write = new FileWriter( new File( OUTPUT_FILE + ".result" ) );
			EXPR.grb_cache.forEach( new BiConsumer<EXPR, GRBVar>() {
		    	public void accept(EXPR t, GRBVar u) {
		    		GRBVar[] u_arr = new GRBVar[]{ u };
		    		try {
		    			char[] type = grb_model.get( GRB.CharAttr.VType , u_arr );
		    			file_write.write( t + " " + type[0] + " " + Arrays.toString( grb_model.get( GRB.DoubleAttr.X, u_arr ) ) );
					} catch (GRBException | IOException e) {
						System.out.println("EXPR : " + t );
						e.printStackTrace();
					}
		    	};
			});		
			file_write.flush();
			file_write.close();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	protected void outputLPFile() throws GRBException, IOException {
		grb_model.write( OUTPUT_FILE );
		
		List<String> src = new ArrayList<>( EXPR.reverse_name_map.keySet() );
		Collections.sort( src, new Comparator<String>() {

			@Override
			public int compare(  String o1, String o2) {
				return (new Integer(o1.length()).compareTo( 
							new Integer( o2.length()) ) );
			}
		});
		Collections.reverse( src );
		
		Files.write( Paths.get( OUTPUT_FILE + ".post" ), 
				Files.readAllLines( Paths.get( OUTPUT_FILE ) ).stream().map( new Function<String, String>() {

					@Override
					public String apply(String t) {
						String ret = t;
						for( String entry :  src ){
							ret = ret.replace( entry, EXPR.reverse_name_map.get( entry ) );
						}
						return ret;
					}
					
				}).collect( Collectors.toList() ) );		
	}


	private void outputAssignments( final HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> map, final int time ) {
		map.forEach( new BiConsumer<PVAR_NAME, ArrayList<ArrayList<LCONST>>>( ) {
			@Override
			public void accept(PVAR_NAME pvar, ArrayList<ArrayList<LCONST>> u) {
				u.stream().forEach( new Consumer< ArrayList<LCONST> >() {

					   @Override
					   public void accept( ArrayList<LCONST> term ) {
						   EXPR expr = new PVAR_EXPR( pvar._sPVarName, term ).addTerm(TIME_PREDICATE, constants, objects);
						
						   EXPR subs_t = expr.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(time) ), constants, objects);
						   try {
								System.out.println( subs_t + "=" + EXPR.grb_cache.get( subs_t ).get( DoubleAttr.X ) );
						   } catch (GRBException e) {
								e.printStackTrace();
						   }
						}
				});
				}
			});
	}
	
	protected Map<EXPR, Double> getAssignments( final HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> map, final int time ) {
		final HashMap< EXPR, Double > ret = new HashMap< >();
		
		map.forEach( new BiConsumer<PVAR_NAME, ArrayList<ArrayList<LCONST>>>( ) {
			@Override
			public void accept(PVAR_NAME pvar, ArrayList<ArrayList<LCONST>> u) {
				u.stream().forEach( new Consumer< ArrayList<LCONST> >() {

					   @Override
					   public void accept( ArrayList<LCONST> term ) {
						   EXPR expr = new PVAR_EXPR( pvar._sPVarName, term ).addTerm(TIME_PREDICATE, constants, objects);
						
						   EXPR subs_t = expr.substitute( 
								   Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(time) ), constants, objects);
						   try {
							   GRBVar grb_var = EXPR.grb_cache.get( subs_t );
							   assert( grb_var != null );
							   ret.put( subs_t, grb_var.get( DoubleAttr.X ) );
						   } catch (GRBException e) {
								e.printStackTrace();
						   }
						}
				});
				}
			});
		return ret;
	}

	private void getConstants() throws EvalException {
		ArrayList<PVAR_INST_DEF> all_consts = new ArrayList<PVAR_INST_DEF>();
		for( final PVAR_NAME p : rddl_state._alNonFluentNames ){
			ArrayList<ArrayList<LCONST>> atoms = rddl_state.generateAtoms( p );
			Object def = rddl_state.getDefaultValue(p);
			for( final ArrayList<LCONST> atom : atoms ){
				all_consts.add( new PVAR_INST_DEF(p._sPVarName, def, atom) );
			}
		}
		if( rddl_nonfluents != null && rddl_nonfluents._alNonFluents != null ){
			all_consts.addAll( rddl_nonfluents._alNonFluents );
		}
		
		constants.putAll( getConsts( all_consts ) );//overwrite default values		
//		constants.putAll( getConsts(  rddl_nonfluents._alNonFluents ) );
		System.out.println("Constants: " );
		System.out.println("---------------------------------------" );
		constants.forEach( (a,b) -> System.out.println( a + " : " + b ) );
	}

	protected void translateConstraints() throws Exception {
		
		GRBExpr old_obj = grb_model.getObjective();
//		translateMaxNonDef( );
		System.out.println("--------------Translating Constraints-------------");
		
		ArrayList<BOOL_EXPR> constraints = new ArrayList<BOOL_EXPR>();
		constraints.addAll( rddl_state._alActionPreconditions ); constraints.addAll( rddl_state._alStateInvariants );
		for( final EXPR e : constraints ){
			System.out.println( "Translating Constraint " + e );
			
//			substitution expands quantifiers
//			better to substitute for time first
			EXPR non_stationary_e = e.substitute( Collections.EMPTY_MAP, constants, objects)
					.addTerm(TIME_PREDICATE, constants, objects );
//			this works. but is expensive
//			QUANT_EXPR all_time = new QUANT_EXPR( QUANT_EXPR.FORALL, 
//					new ArrayList<>( Collections.singletonList( new LTYPED_VAR( TIME_PREDICATE._sVarName,  TIME_TYPE._STypeName ) ) )
//							, non_stationary_e );
			for( int t = 0 ; t < TIME_TERMS.size(); ++t ){
				EXPR this_t = non_stationary_e.substitute( 
						 	Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects);
				GRBVar constrained_var = this_t.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
				grb_model.addConstr( constrained_var, GRB.EQUAL, 1, "constraint=1_"+e.toString()+"time="+t );
//				grb_model.update();
				
				saved_expr.add( this_t ); saved_vars.add( constrained_var );
			}
		}
		
		grb_model.setObjective(old_obj);
//		grb_model.update();
	}

//	private void translateMaxNonDef() {
//		EXPR sum = new REAL_CONST_EXPR( 0d );
//		for( Entry<PVAR_NAME, ArrayList<ArrayList<LCONST>>> action_var : rddl_action_vars.entrySet() ){
//			for( ArrayList<LCONST> terms : action_var.getValue() ){
//				sum = new OPER_EXPR( sum, new PVAR_EXPR( action_var.getKey()._sPVarName, terms ), OPER_EXPR.PLUS );
//			}
//		}
//		sum = sum.substitute( Collections.EMPTY_MAP, constants, objects).addTerm(TIME_PREDICATE);
//		COMP_EXPR action_constraint_stationary = new COMP_EXPR( sum, 
//				new INT_CONST_EXPR( rddl_instance._nNonDefActions ), COMP_EXPR.LESSEQ );
//		QUANT_EXPR action_constraint_non_stationary = new QUANT_EXPR( QUANT_EXPR.FORALL, 
//				new ArrayList<LTYPED_VAR>(
//						Collections.singletonList( new LTYPED_VAR( TIME_PREDICATE._sVarName, TIME_TYPE._STypeName ) ) ), 
//						action_constraint_stationary );
//		GRBVar constrained_var = action_constraint_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
//		grb_model.addConstr( constrained_var, GRB.EQUAL, 1, "maxnondef=1" );
//		grb_model.update();		
//	}

	protected void translateInitialState(ArrayList<PVAR_INST_DEF> initState) throws GRBException {
		HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs = getConsts( initState );
		translateInitialState( subs );
		
	}

	protected void translateInitialState( HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs ) throws GRBException {
		
		GRBExpr old_obj = grb_model.getObjective();
		
		for( final PVAR_NAME p : rddl_state_vars.keySet() ){
			for( final ArrayList<LCONST> terms : rddl_state_vars.get( p ) ){
				Object rhs = null;
				if( subs.containsKey( p ) && subs.get( p ).containsKey( terms ) ){
					rhs = subs.get(p).get( terms );
				}else{
					rhs = rddl_state.getDefaultValue(p);
				}

				PVAR_EXPR stationary_pvar_expr = new PVAR_EXPR( p._sPVarName, terms );
				EXPR non_stationary_pvar_expr = stationary_pvar_expr
						.addTerm( TIME_PREDICATE, constants, objects )
						.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(0) ) , constants, objects); 
				GRBVar lhs_var = non_stationary_pvar_expr.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
				
				EXPR rhs_expr = null;
				if( rhs instanceof Boolean ){
					rhs_expr = new BOOL_CONST_EXPR( (boolean) rhs );
				}else if( rhs instanceof Double ){
					rhs_expr = new REAL_CONST_EXPR( (double)rhs );
				}else if( rhs instanceof Integer ){
					rhs_expr = new INT_CONST_EXPR( (int)rhs );
				}
				GRBVar rhs_var = rhs_expr.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
				grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "initState_"+p.toString()+terms );
//				grb_model.update();
				
				System.out.println( non_stationary_pvar_expr + " " + rhs_expr );
				
				saved_vars.add( lhs_var ); saved_vars.add( rhs_var );
				saved_expr.add( non_stationary_pvar_expr ); saved_expr.add( rhs_expr );
			}
		}
		
		grb_model.setObjective(old_obj);
//		grb_model.update();
		
	}

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
						GRBVar lhs_var = null;
						
						if( rddl_state_vars.containsKey(p) ){
							if( t == lookahead - 1 ){
								continue;
							}
							//FIXME : stationarity assumption
							new_lhs_non_stationary = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
									.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t+1) ), constants, objects);
							lhs_var = new_lhs_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
						}else {
							new_lhs_non_stationary = new_lhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
									.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects);
							lhs_var = new_lhs_non_stationary.getGRBConstr( GRB.EQUAL, grb_model, constants, objects, type_map);
						}
						
						//FIXME : stationarity assumption
						EXPR new_rhs_non_stationary = new_rhs_stationary.addTerm(TIME_PREDICATE, constants, objects )
								.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(t) ), constants, objects );
						GRBVar rhs_var = new_rhs_non_stationary.getGRBConstr(GRB.EQUAL,  grb_model, constants, objects, type_map);
						
						System.out.println( p + " " + terms + " " + t );
						grb_model.addConstr( lhs_var, GRB.EQUAL, rhs_var, "CPT_t_"+p.toString()+"_"+terms );
//						grb_model.update();
						
						saved_expr.add( new_lhs_non_stationary );
						saved_expr.add( new_rhs_non_stationary );
						
						saved_vars.add( lhs_var );
						saved_vars.add( rhs_var );
						
					}
					
				}
			}
			
		}
		
		grb_model.setObjective(old_obj);
//		grb_model.update();
		
	}

	protected Map<LVAR,LCONST> getSubs(ArrayList<LTERM> terms, ArrayList<LCONST> consts) {
		Map<LVAR, LCONST> ret = new HashMap<RDDL.LVAR, RDDL.LCONST>();
		for( int i = 0 ; i < terms.size(); ++i ){
			assert( terms.get(i) instanceof LVAR );
			ret.put( (LVAR)terms.get(i), consts.get(i) );
		}
		return ret;
	}

	protected HashMap< PVAR_NAME, HashMap< ArrayList<LCONST>, Object> > getConsts(ArrayList<PVAR_INST_DEF> consts) {
		HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> ret 
			= new HashMap< PVAR_NAME, HashMap< ArrayList<LCONST>, Object> >();
		for( final PVAR_INST_DEF p : consts ){
			if( ret.get(p._sPredName) == null ){
				ret.put( p._sPredName, new HashMap<ArrayList<LCONST>, Object>() );
			}
			HashMap<ArrayList<LCONST>, Object> inner_map = ret.get( p._sPredName );
			inner_map.put( p._alTerms, p._oValue );
			ret.put( p._sPredName, inner_map );//unnecessary
		}
		return ret;
	}

//	private void translateInitialState() throws GRBException {
//		for( final PVAR_INST_DEF p : rddl_instance._alInitState ){
//			PVAR_EXPR pe = new PVAR_EXPR( p._sPredName._sPVarName , pred_type.get( p._sPredName )._STypeName,  p._alTerms );
//			pe.getGRBConstr( GRB.EQUAL, grb_model, 
//						Collections.singletonMap( 
//								p._sPredName, Collections.singletonMap( p._alTerms, p._oValue) ), null );
//			
//			GRBVar var = getGRBVar(p._sPredName, p._alTerms, 0);
//			GRBLinExpr expr = new GRBLinExpr();
//			expr.addConstant( ((Number)p._oValue).doubleValue() );
//			grb_model.addConstr(var, GRB.EQUAL, expr, "C0__" + var.toString() );
//		}
//		//max-nondef does not make sense with real valued actions
//		//TODO discounting
//	}

//	private void translateCPTs() throws EvalException, GRBException {
//		
//		for( int t = 0 ; t < lookahead; ++t ){
//			for( final PVAR_NAME p : rddl_state._alStateNames ){
//				final CPF_DEF cpf_def  = rddl_state._hmCPFs.get( new PVAR_NAME( p._sPVarName + "'" ) );
//				ArrayList<LTERM> param_names = cpf_def._exprVarName._alTerms ; 
//				ArrayList<ArrayList<LCONST>> params = rddl_state.generateAtoms(p);
//				//p'(?x) = p(?x) + a(?x) => p_t(x1) = p_{t-1}(x1) + a_{t-1}(x1)
//				for( final ArrayList<LCONST> param : params ){
//					HashMap<LVAR, LCONST> subs = getSubs( param_names, param );
//					//encode lhs
//					PVAR_EXPR timed_pvar = new PVAR_EXPR( p._sPVarName + "_t" + t , 
//							pred_type.get( p )._STypeName, param );
//					GRBVar timed_gvar = timed_pvar.getGRBConstr( GRB.EQUAL, grb_model, constants, null);
//					//encode rhs
//					EXPR substituted_expr = cpf_def._exprEquals.substitute(subs, constants, null);
//					GRBVar substituted_gvar = substituted_expr.getGRBConstr( GRB.EQUAL, grb_model, constants, null);
//					//lhs = rhs
//					grb_model.addConstr( timed_gvar,  substituted_gvar, expr, name)
//							
//							getGRBVar( p, param, t+1 );//t+1 for next time step (primed)
//					final String grb_varname = CleanFluentName( p.toString() + param );
//					ConstraintLeaf cl = new ConstraintLeaf(grb_var, grb_varname, t );
//					translateLinearExp( cpf_def._exprEquals, subs, 1.0d, cl, t );
//					cl.addConstraint();
//				}
//			}
//		}
//	}

	protected void translateReward() throws Exception{
		EXPR stationary = rddl_state._reward;
		//expand quantifier
		//filter constants
		EXPR stationary_clear = stationary.substitute( Collections.EMPTY_MAP, constants, objects);
		//add time 
		EXPR non_stationary = stationary_clear.addTerm( TIME_PREDICATE , constants, objects );
		//expand time
		//this works but expensive
		//just iterate over time 
		//reset objective
		grb_model.setObjective( new GRBLinExpr() );
//		grb_model.update();
				
		for( int time = 0 ; time < lookahead; ++time ){
			EXPR subs_t = non_stationary.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(time)), constants, objects);
			saved_expr.add( subs_t );
			saved_vars.add( subs_t.addGRBObjectiveTerm(grb_model, constants, objects, type_map) );
		}
//		grb_model.update();
	}
	
//	private HashMap<LVAR, LCONST> getSubs( final ArrayList<LTERM> param_names,
//			final ArrayList<LCONST> param ) {
//		HashMap<LVAR, LCONST> subs = new HashMap<RDDL.LVAR, RDDL.LCONST>();
	
//		for( int i = 0 ; i < param_names.size(); ++i ){
//			subs.put( (LVAR)param_names.get(i), param.get(i) );
//		}
//		return subs;
//	}
//	
	private void initializeRDDL(final String domain_file, final String instance_file) throws Exception {
		RDDL domain_rddl = null, instance_rddl = null;
		try {
			domain_rddl = rddl.parser.parser.parse( new File( domain_file) );
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("domain file did not parse.");
			System.exit(1);
		}
		try {
			instance_rddl = rddl.parser.parser.parse( new File( instance_file ) );
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("instance file did not parse.");
			System.exit(1);
		}
		
		this.rddl_obj = new RDDL();
		this.rddl_obj.addOtherRDDL(domain_rddl);
		this.rddl_obj.addOtherRDDL(instance_rddl);
		
		// Set up instance, nonfluent, and domain information
		//I assume that there is only one instance and domain in the respective files
		this.instance_name = instance_rddl._tmInstanceNodes.keySet().iterator().next() ;
		this.domain_name = domain_rddl._tmDomainNodes.keySet().iterator().next();
		
		rddl_instance = rddl_obj._tmInstanceNodes.get( instance_name );
		if (rddl_instance == null){
			throw new Exception("Instance '" + instance_name + 
					"' not found, choices are " + rddl_obj._tmInstanceNodes.keySet());
		}
		
		rddl_nonfluents = null;
		if (rddl_instance._sNonFluents != null){
			rddl_nonfluents = rddl_obj._tmNonFluentNodes.get(rddl_instance._sNonFluents);
		}
		
		rddl_domain = rddl_obj._tmDomainNodes.get(rddl_instance._sDomain);
		if ( rddl_domain == null){
			throw new Exception("Could not get domain '" + 
					rddl_instance._sDomain + "' for instance '" + instance_name + "'");
		}
		
		if (rddl_nonfluents != null && !rddl_instance._sDomain.equals(rddl_nonfluents._sDomain)){
			throw new Exception("Domain name of instance and fluents do not match: " + 
					rddl_instance._sDomain + " vs. " + rddl_nonfluents._sDomain);
		}	
		
		this.rddl_state = new rddl.State();
		rddl_state.init( rddl_domain._hmObjects, rddl_nonfluents != null ? rddl_nonfluents._hmObjects : null, 
				rddl_instance._hmObjects, rddl_domain._hmTypes, rddl_domain._hmPVariables, rddl_domain._hmCPF,
				rddl_instance._alInitState, rddl_nonfluents == null ? null : rddl_nonfluents._alNonFluents, 
				rddl_domain._alStateConstraints, rddl_domain._alActionPreconditions, rddl_domain._alStateInvariants,  
				rddl_domain._exprReward, rddl_instance._nNonDefActions);
		
		this.rddl_state_vars = collectGroundings( rddl_state._alStateNames );
		this.rddl_action_vars = collectGroundings( rddl_state._alActionNames );
		this.rddl_observ_vars = collectGroundings( rddl_state._alObservNames );
		this.rddl_interm_vars = collectGroundings( rddl_state._alIntermNames );
		
		this.string_state_vars = cleanMap( rddl_state_vars );
		this.string_action_vars = cleanMap( rddl_action_vars );
		this.string_observ_vars = cleanMap( rddl_observ_vars );
		this.string_interm_vars = cleanMap( rddl_interm_vars );
		
	}
	
	private void initializeGRB( ) throws GRBException {
		this.GRB_log = domain_name + "__" + instance_name + ".grb";
		//create vars for state, action, interm vars over time
		this.grb_env = new GRBEnv(GRB_log);
		grb_env.set( GRB.DoubleParam.TimeLimit, TIME_LIMIT_MINS*60 );
		grb_env.set( GRB.DoubleParam.MIPGap, GRB_MIPGAP );
		grb_env.set( DoubleParam.Heuristics, GRB_HEURISTIC );
		grb_env.set( IntParam.InfUnbdInfo , GRB_INFUNBDINFO );
		grb_env.set( IntParam.DualReductions, GRB_DUALREDUCTIONS );
		this.grb_model = new GRBModel( grb_env );
		//max
		grb_model.set( GRB.IntAttr.ModelSense, -1);
//		grb_model.update();
	}

	private List<String> cleanMap( final HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> map ) {
		List<String> ret = new ArrayList<String>();
		map.forEach( (a,b) -> b.forEach( m -> ret.add( CleanFluentName( a.toString() + m ) ) ) );
		return ret;
	}
	
	public HashMap< PVAR_NAME, ArrayList<ArrayList<LCONST>> > collectGroundings( final ArrayList<PVAR_NAME> preds )
		throws EvalException {
		HashMap<PVAR_NAME, ArrayList<ArrayList<LCONST>>> ret 
			= new  HashMap<RDDL.PVAR_NAME, ArrayList<ArrayList<LCONST>>>();
		
		for( PVAR_NAME p : preds ){
			ArrayList<ArrayList<LCONST>> gfluents = rddl_state.generateAtoms(p);
			ret.put(p, gfluents);
			PVARIABLE_DEF def = rddl_state._hmPVariables.get(p);
			pred_type.put( p, def._typeRange );
		}
		return ret ;
	}
	
	public static String CleanFluentName(String s) {
		s = s.replace("[", "__");
		s = s.replace("]", "");
		s = s.replace(", ", "_");
		s = s.replace(',','_');
		s = s.replace(' ','_');
		s = s.replace('-','_');
		s = s.replace("()","");
		s = s.replace("(", "__");
		s = s.replace(")", "");
		s = s.replace("$", "");
		if (s.endsWith("__"))
			s = s.substring(0, s.length() - 2);
		if (s.endsWith("__'")) {
			s = s.substring(0, s.length() - 3);
			s = s + "\'"; // Really need to escape it?  Don't think so.
		}
		return s;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println( Arrays.toString( args ) );
		System.out.println( new Translate( Arrays.asList( args ) ).doPlanInitState() );
	}
	
	public Translate( List<String> args) throws Exception {
		System.out.println( args );
		StateViz viz = null;
		if( args.get(4).equals("reservoir") ){
			viz = new StateViz() {
				private final static String OUTPUT_FILE_LEVEL = "reservoir_levels_last.viz";
				private final static String OUTPUT_FILE_RAIN = "reservoir_rain_last.viz";
				
				private BufferedWriter outfile_level = null;;
				private BufferedWriter outfile_rain = null;;
				
				@Override
				protected void finalize() throws Throwable {
					outfile_level.close(); outfile_rain.close();
				}
				
				@Override
				public void display(State s, int time) {
					if( outfile_level == null ){
						try {
							outfile_level = new BufferedWriter( new FileWriter( new File( OUTPUT_FILE_LEVEL ) ) );
							outfile_rain = new BufferedWriter( new FileWriter( new File( OUTPUT_FILE_RAIN ) ) );
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					try {
						PVAR_NAME pvar = new PVAR_NAME("rlevel");						
						double[] levels = s.generateAtoms( pvar ).stream()
								.mapToDouble( new ToDoubleFunction< ArrayList<LCONST> > () {
									public double applyAsDouble( ArrayList<LCONST> value) {
										try {
											return (Double)s.getPVariableAssign(pvar, value);
										} catch (EvalException e) {
											e.printStackTrace();
										}
										return Double.NaN;
									}
								} ).toArray();
						outfile_level.write( Arrays.toString( levels ) );
						outfile_level.write( "\n" );
						outfile_level.flush();
						
						PVAR_NAME rainvar = new PVAR_NAME("rain");
						double[] rain = s.generateAtoms( rainvar ).stream()
								.mapToDouble( new ToDoubleFunction< ArrayList<LCONST> > () {
									public double applyAsDouble( ArrayList<LCONST> value) {
										try {
											return (Double)s.getPVariableAssign( rainvar, value);
										} catch (EvalException e) {
											e.printStackTrace();
										}
										return Double.NaN;
									}
								} ).toArray();
						outfile_rain.write( Arrays.toString( rain ) );
						outfile_rain.write( "\n" );
						outfile_rain.flush();
						
					} catch (EvalException | IOException e) {
						e.printStackTrace();
					}
				}
			};
		}
		
		TranslateInit( args.get(0), args.get(1), Integer.parseInt( args.get(2) ), Double.parseDouble( args.get(3) ),
				viz );
//		doPlanInitState();
	}

	@Override
	public ArrayList<PVAR_INST_DEF> getActions(State s) throws EvalException {
		System.out.println("State : " + s );
		try {
			Map<EXPR, Double> ret_expr = doPlan( s._state );
			ArrayList<PVAR_INST_DEF> ret = getRootActions(ret_expr);
			System.out.println( "Action : " + ret );
			viz.display(s, 0);
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected ArrayList<PVAR_INST_DEF> getRootActions(Map<EXPR, Double> ret_expr) {
		final ArrayList<PVAR_INST_DEF> ret = new ArrayList<>();
		
		rddl_action_vars.entrySet().parallelStream().forEach( new Consumer< Map.Entry< PVAR_NAME, ArrayList<ArrayList<LCONST>> > >() {
			@Override
			public void accept( Map.Entry< PVAR_NAME , ArrayList<ArrayList<LCONST>> > entry ) {
				final PVAR_NAME pvar = entry.getKey();
				entry.getValue().parallelStream().forEach( new Consumer< ArrayList<LCONST> >() {
					@Override
					public void accept(ArrayList<LCONST> terms ) {
						final EXPR lookup = new PVAR_EXPR( pvar._sPVarName, terms )
							.addTerm(TIME_PREDICATE, constants, objects)
							.substitute( Collections.singletonMap( TIME_PREDICATE, TIME_TERMS.get(0) ), constants, objects);
						assert( ret_expr.containsKey( lookup ) );
						ret.add( new PVAR_INST_DEF( pvar._sPVarName, ret_expr.get( lookup ), terms ) );
					}
				});
			}
		});
		
		return ret;
	}
	
}