package rddl.det.mip;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rddl.RDDL.AGG_EXPR;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.CONN_EXPR;
import rddl.RDDL.EXPR;
import rddl.RDDL.LCONST;
import rddl.RDDL.LTYPED_VAR;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.OPER_EXPR;
import rddl.RDDL.PVAR_EXPR;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.QUANT_EXPR;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.TYPE_NAME;
import rddl.RDDL.OBJECT_VAL;
import rddl.RDDL.IF_EXPR;
import rddl.RDDL.REAL_CONST_EXPR;

public class EXPRtoGRBTests {
	
	private GRBEnv grb_env;
	private GRBModel grb_model;

	@Before
	public void setUp(){
		try {
			grb_env = new GRBEnv( "EXPRtoGRBTests.log");
			grb_model = new GRBModel( grb_env );
		} catch (GRBException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@After
	public void tearDown() throws GRBException{
		grb_model.write("last_grb_test.lp");
		EXPR.name_map.forEach( (a,b) -> System.out.println(a + " " + b ) );
		
		System.out.println("optimizing");
		grb_model.presolve();
		grb_model.optimize();
		grb_model.dispose();
	}
	
	@Test
	public void testGetGRBConstr() throws Exception {

		BOOL_CONST_EXPR b_true = new BOOL_CONST_EXPR( true ); BOOL_CONST_EXPR b_false = new BOOL_CONST_EXPR( false );
		GRBVar v_true = b_true.getGRBConstr( GRB.EQUAL, grb_model, null, null, null);
		GRBVar v_false = b_false.getGRBConstr( GRB.EQUAL, grb_model, null, null, null);
		//idempotent
		assertTrue( v_true.equals( b_true.getGRBConstr( GRB.EQUAL, grb_model, null, null, null ) ) );
		assertTrue( v_false.equals( b_false.getGRBConstr( GRB.EQUAL, grb_model, null, null, null ) ) );
		//expr = const_expr
		
		assertTrue( new OPER_EXPR( b_true, b_false, OPER_EXPR.PLUS ).getGRBConstr( GRB.EQUAL, grb_model, null, null, null ).equals( v_true ) ); 
		assertTrue( new OPER_EXPR( b_true, b_false, OPER_EXPR.TIMES ).getGRBConstr( GRB.EQUAL, grb_model, null, null, null ).equals( v_false ) ); 

		EXPR e = new PVAR_EXPR( "p", new ArrayList<>() ); 
		Map< PVAR_NAME, Character > p_type = Collections.singletonMap( new PVAR_NAME("p"), GRB.CONTINUOUS ) ;
		GRBVar v = e.getGRBConstr( GRB.EQUAL, grb_model, null, null, p_type ) ; 
		assertTrue( v.equals( new OPER_EXPR( e, b_false, OPER_EXPR.PLUS )
			.getGRBConstr( GRB.EQUAL, grb_model, null, null, p_type ) ) );

 		assertTrue( v.equals( new AGG_EXPR( AGG_EXPR.PROD, new ArrayList<>(), e )
 			.getGRBConstr( GRB.EQUAL, grb_model, null, null, p_type ) ) );
		assertTrue( e.equals( new AGG_EXPR( AGG_EXPR.PROD, new ArrayList<>(), e ) ) );
	
		assertTrue( v.equals( new AGG_EXPR( AGG_EXPR.SUM, new ArrayList<>(), e )
			.getGRBConstr( GRB.EQUAL, grb_model, null, null, p_type ) ) );
		assertTrue( e.equals( new AGG_EXPR( AGG_EXPR.SUM, new ArrayList<>(), e ) ) );
		
		e = new PVAR_EXPR( "q", new ArrayList<>() );
		Map<PVAR_NAME, Character> q_type = Collections.singletonMap( new PVAR_NAME("q"), GRB.BINARY );
		v = e.getGRBConstr( GRB.EQUAL, grb_model, null, null, q_type );
		assertTrue( e.equals( new QUANT_EXPR( QUANT_EXPR.EXISTS, new ArrayList<>(), e ) ) );
		assertTrue( e.equals( new QUANT_EXPR( QUANT_EXPR.FORALL, new ArrayList<>(), e ) ) );

		assertTrue( v.equals( new QUANT_EXPR( QUANT_EXPR.EXISTS, new ArrayList<>(), e )
			.getGRBConstr( GRB.EQUAL, grb_model, null, null, q_type ) ) );
		assertTrue( v.equals( new QUANT_EXPR( QUANT_EXPR.FORALL, new ArrayList<>(), e )
			.getGRBConstr( GRB.EQUAL, grb_model, null, null, q_type ) ) );
		
		OPER_EXPR nv = new OPER_EXPR( new INT_CONST_EXPR( 3 ), b_true , OPER_EXPR.TIMES );
		GRBVar nv_var = nv.getGRBConstr( GRB.EQUAL, grb_model, null, null, null );
		
		LTYPED_VAR x_var = new LTYPED_VAR( "?x", "x_obj" );
		ArrayList<LCONST> x_objects = new ArrayList<LCONST>();
		x_objects.add( new OBJECT_VAL("x1") ); x_objects.add( new OBJECT_VAL("x2") ); x_objects.add( new OBJECT_VAL("x3") );
		
		Map<TYPE_NAME, OBJECTS_DEF> objects = Collections.singletonMap( new TYPE_NAME("x_obj"), 
				new OBJECTS_DEF( "x_obj", x_objects) );
		GRBVar sum_var = new AGG_EXPR( AGG_EXPR.SUM, new ArrayList<LTYPED_VAR>( 
				   	Collections.singletonList( x_var ) ), b_true )
						.getGRBConstr( GRB.EQUAL, grb_model, null, objects , null );
		assertTrue( nv_var.equals( sum_var ) );
		
		//test for linear expr
		PVAR_EXPR p = new PVAR_EXPR("p" , new ArrayList<>( Collections.singletonList( new OBJECT_VAL("x1") ) ) );
		PVAR_EXPR q = new PVAR_EXPR("q", new ArrayList<>( Collections.singletonList( new OBJECT_VAL("x1") ) ) );
		Map<PVAR_NAME, Character> pq_type = new HashMap<PVAR_NAME, Character>();
//		pq_type.put( p._pName, GRB.CONTINUOUS); pq_type.put( q._pName, GRB.BINARY);
		pq_type.put( p._pName, GRB.INTEGER); pq_type.put( q._pName, GRB.BINARY);
//		pq_type.put( p._pName, GRB.BINARY); pq_type.put( q._pName, GRB.BINARY);
		System.out.println( new OPER_EXPR( p, q, OPER_EXPR.PLUS ).getGRBConstr(GRB.EQUAL, grb_model, null, null, pq_type) );
//		System.out.println( new OPER_EXPR( p, q, OPER_EXPR.TIMES ).getGRBConstr(GRB.EQUAL, grb_model, null, null, pq_type) );
		
		//test for IF expr
		IF_EXPR ife = new IF_EXPR( q , new REAL_CONST_EXPR(10d), new REAL_CONST_EXPR(20d) );
		ife.getGRBConstr( GRB.EQUAL, grb_model, null, null, pq_type );
	}

}
