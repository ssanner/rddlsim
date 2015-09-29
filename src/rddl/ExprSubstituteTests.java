package rddl;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.sun.org.apache.xml.internal.utils.UnImplNode;

import rddl.RDDL.AGG_EXPR;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.BOOL_EXPR;
import rddl.RDDL.Bernoulli;
import rddl.RDDL.COMP_EXPR;
import rddl.RDDL.CONN_EXPR;
import rddl.RDDL.DiracDelta;
import rddl.RDDL.Dirichlet;
import rddl.RDDL.Discrete;
import rddl.RDDL.ENUM_VAL;
import rddl.RDDL.EXPR;
import rddl.RDDL.Exponential;
import rddl.RDDL.FUN_EXPR;
import rddl.RDDL.Gamma;
import rddl.RDDL.IF_EXPR;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.KronDelta;
import rddl.RDDL.LCONST;
import rddl.RDDL.LTERM;
import rddl.RDDL.LTYPED_VAR;
import rddl.RDDL.LVAR;
import rddl.RDDL.Multinomial;
import rddl.RDDL.NEG_EXPR;
import rddl.RDDL.Normal;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.OBJECT_VAL;
import rddl.RDDL.OPER_EXPR;
import rddl.RDDL.PVAR_EXPR;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.Poisson;
import rddl.RDDL.QUANT_EXPR;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.RDDL.STRUCT_EXPR;
import rddl.RDDL.SWITCH_EXPR;
import rddl.RDDL.TVAR_EXPR;
import rddl.RDDL.TYPE_NAME;
import rddl.RDDL.Uniform;
import rddl.RDDL.Weibull;

public class ExprSubstituteTests {

	@Test
	public void testSubstituteLVAR( ){
		LVAR t1 = new LVAR("?c");
		OBJECT_VAL o1 = new OBJECT_VAL("c1");
		EXPR c1 = t1.substitute( Collections.singletonMap( t1, o1 ), null, null );
		assertTrue( c1 instanceof LCONST && c1.equals(o1) );
		EXPR c2 = t1.substitute( Collections.EMPTY_MAP ,  null, null );
		EXPR c3 = t1.substitute( Collections.EMPTY_MAP, null, null );
		EXPR c4 = t1.substitute( Collections.singletonMap( new LVAR("?t"), new OBJECT_VAL("t1") ), null, null );
		assertTrue( c2.equals( t1 ) );
		assertTrue( c3.equals( t1 ) );
		assertTrue( c4.equals( t1 ) );
	}
	
	@Test
	public void testSubstituteLTYPED_VAR() {
		LTYPED_VAR lv = new LTYPED_VAR( "?c", "computer" );
		assertTrue( lv.substitute( Collections.EMPTY_MAP, null, null ).equals(lv) );
	}
	
	@Test
	public void testExpandQuantifierLTYPED_VAR(){
		LTYPED_VAR lv = new LTYPED_VAR( "?c", "computer" );
		ArrayList<LTYPED_VAR> lvars = new ArrayList<LTYPED_VAR>();
		lvars.add( lv );
		
		ArrayList<LCONST> c_objects = new ArrayList<LCONST>();
		c_objects.add( new OBJECT_VAL("c1") );c_objects.add( new OBJECT_VAL("c2") );c_objects.add( new OBJECT_VAL("c3") );
		
		HashMap<TYPE_NAME, OBJECTS_DEF> c_obj_map = new HashMap<TYPE_NAME, OBJECTS_DEF>();
		c_obj_map.put( new TYPE_NAME("computer"),  new OBJECTS_DEF("computer",c_objects ) );
		
		List<EXPR> quantified = EXPR.expandQuantifier( lv, lvars, c_obj_map,  null );
		assertTrue( quantified.get(0).equals( new OBJECT_VAL("c1") ) );
		assertTrue( quantified.get(1).equals( new OBJECT_VAL("c2") ) );
		assertTrue( quantified.get(2).equals( new OBJECT_VAL("c3") ) );
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteTVAR_EXPR() {
		try{
			throw new UnsupportedOperationException("not implemented test case");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteENUM_VAL() {
		ENUM_VAL v = new ENUM_VAL("@low");
		assertTrue( v.substitute( Collections.EMPTY_MAP, null, null ).equals( v ) );
	}
	
	@Test
	public void testSubstituteOBJECT_VAL() {
		OBJECT_VAL v = new OBJECT_VAL("low");
		assertTrue( v.substitute( Collections.EMPTY_MAP, null, null ).equals( v ) );
	}
	
	@Test
	public void testSubstituteBOOL_CONST_EXPR() throws Exception {
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteCOMP_EXPR() throws Exception {
		try{
			throw new UnsupportedOperationException("test not implemented");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteDiracDelta() {
		BOOL_CONST_EXPR e = new BOOL_CONST_EXPR(true);
		DiracDelta d = new DiracDelta( e );
		assertTrue( d.substitute( Collections.EMPTY_MAP, null, null ).equals( d ) );
	}
	
	@Test
	public void testSubstituteKronDelta() {
		KronDelta k = new KronDelta( new INT_CONST_EXPR( 5 ) );
		assertTrue( k.substitute( Collections.EMPTY_MAP, null, null ).equals( k ) );
	}
	
	@Test
	public void testSubstituteUniform() {
		Uniform u = new Uniform( new REAL_CONST_EXPR(0d), new REAL_CONST_EXPR(1d) );
		assertTrue( u.substitute( Collections.EMPTY_MAP, null, null ).equals(u) );
	}
	
	@Test
	public void testSubstituteNormal() {
		Normal n = new Normal( new REAL_CONST_EXPR(0d), new REAL_CONST_EXPR(1d) );
		assertTrue( n.substitute(Collections.EMPTY_MAP, null, null).equals(n) );
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteDirichlet() {
		try{
			throw new UnsupportedOperationException(this.getClass().toString() + " testSubstitute not  implemented. -ANR");
		}catch( Exception exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteMultinomial() {
		//FIXME
		try{
			throw new UnsupportedOperationException(this.getClass().toString() + " testSubstitute not  implemented.");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteDiscrete() {
		//FIXME
		try{
			throw new UnsupportedOperationException(this.getClass().toString() + " testSubstitute not  implemented. -ANR");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteExponential() {
		Exponential e = new Exponential( new REAL_CONST_EXPR(0d) );
		assertTrue( e.substitute( Collections.EMPTY_MAP, null, null).equals(e) );
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteWeibull() {
		//FIXME
		try{
			throw new UnsupportedOperationException(this.getClass().toString() + " testSubstitute not  implemented. -ANR");
		}catch( Exception exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteGamma() {
		Gamma g = new Gamma( new REAL_CONST_EXPR(1d), new REAL_CONST_EXPR(2d) );
		assertTrue( g.substitute(Collections.EMPTY_MAP, null, null).equals(g) );
	}
	
	@Test
	public void testSubstitutePoisson() {
		Poisson p = new Poisson( new REAL_CONST_EXPR(5d) );
		assertTrue( p.substitute(Collections.EMPTY_MAP, null, null).equals(p) );
	}
	
	@Test
	public void testSubstituteBernoulli() {
		Bernoulli b = new Bernoulli( new REAL_CONST_EXPR(0.5d) );
		assertTrue( b.substitute(Collections.EMPTY_MAP, null, null).equals(b) );
	}
	
	@Test
	public void testSubstituteIntConstExpr() {
		assertTrue( new INT_CONST_EXPR(2).substitute( Collections.EMPTY_MAP, null, null ).equals( new INT_CONST_EXPR(2) ) );
	}
	
	@Test
	public void testSubstituteRealConstExpr() {
		assertTrue( new REAL_CONST_EXPR(0d).substitute( Collections.EMPTY_MAP, null, null ).equals( new REAL_CONST_EXPR(0d) ) );
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteStructExpr() {
		//FIXME
		try{
			throw new UnsupportedOperationException(this.getClass().toString() + " testSubstitute not  implemented. -ANR");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteOperExpr() throws Exception {
		PVAR_EXPR e1 = new PVAR_EXPR("p", new ArrayList<LVAR>( Collections.singletonList( new LVAR("?x" ) ) ) );
		REAL_CONST_EXPR zero = new REAL_CONST_EXPR(0d);
		REAL_CONST_EXPR one = new REAL_CONST_EXPR(1d);
		
		Map<LVAR, LCONST> subs = new HashMap<RDDL.LVAR, RDDL.LCONST>();
		subs.put( new LVAR("?x"), new OBJECT_VAL("x1") );
		
		Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants = new HashMap<>();
		ArrayList<LCONST> x1 = new ArrayList<LCONST>(); x1.add( new OBJECT_VAL("x1" ) );
		constants.put( new PVAR_NAME("p"), Collections.singletonMap( x1, 42d ) );
		
		//e+0, 0+e
		assertTrue( new OPER_EXPR( e1, zero, OPER_EXPR.PLUS ).equals(e1) );
		assertTrue( new OPER_EXPR( zero, e1, OPER_EXPR.PLUS ).equals(e1) );
		//e*1, 1*e
		assertTrue( new OPER_EXPR( e1, one, OPER_EXPR.TIMES ).equals(e1) );
		assertTrue( new OPER_EXPR( one, e1, OPER_EXPR.TIMES ).equals(e1) );
		//e*0, 0*e
		assertTrue( new OPER_EXPR( e1, zero, OPER_EXPR.TIMES).equals(zero) );
		assertTrue( new OPER_EXPR( zero, e1, OPER_EXPR.TIMES).equals(zero) );
		//0/e
		assertTrue( new OPER_EXPR( zero, e1, OPER_EXPR.DIV).equals(zero) );
		//e/1
		assertTrue( new OPER_EXPR( e1, one, OPER_EXPR.DIV).equals(e1) );
		//c+c,c-c,c/c
		assertTrue( new OPER_EXPR( e1, e1, OPER_EXPR.PLUS).substitute(subs, constants, null).equals( new REAL_CONST_EXPR(2*42d) ) );
		assertTrue( new OPER_EXPR( e1, e1, OPER_EXPR.MINUS).substitute(subs, constants, null).equals( new REAL_CONST_EXPR(0d) ) );
		assertTrue( new OPER_EXPR( e1, e1, OPER_EXPR.DIV).substitute(subs, constants, null).equals( new REAL_CONST_EXPR(1d) ) );
		//without const : 
		//e+e
		assertTrue( new OPER_EXPR( e1, e1, OPER_EXPR.PLUS).substitute( Collections.EMPTY_MAP, null, null).equals( 
				new OPER_EXPR( new REAL_CONST_EXPR(2d), e1, OPER_EXPR.TIMES ) ) );
		//e-e
		assertTrue( new OPER_EXPR( e1, e1, OPER_EXPR.MINUS).substitute(subs, null, null).equals( new REAL_CONST_EXPR(0d) ) );
		//e/e
		assertTrue( new OPER_EXPR( e1, e1, OPER_EXPR.DIV).substitute(subs, null, null).equals( new REAL_CONST_EXPR(1d) ) );
		//e1 op e2 - general case TODO 
	}
	
	@Test
	public void testExpandQuantifierAggExpr() {
		ArrayList<LVAR> xy = new ArrayList<LVAR>(); 
		xy.add( new LVAR("?x") ); xy.add( new LVAR("?y") );
		PVAR_EXPR pe = new PVAR_EXPR( "p", xy );
		
		ArrayList<LTYPED_VAR> lvar = new ArrayList<>(  Collections.singletonList(
				new LTYPED_VAR("?x", "x_obj") ) );
		
		ArrayList<LCONST> x12 = new ArrayList<LCONST>();
		OBJECT_VAL x1 = new OBJECT_VAL("x1");
		OBJECT_VAL x2 = new OBJECT_VAL("x2") ;
		x12.add( x1 );x12.add( x2 );
		HashMap<TYPE_NAME,OBJECTS_DEF> objects = new HashMap<RDDL.TYPE_NAME, RDDL.OBJECTS_DEF>();
		objects.put( new TYPE_NAME("x_obj"), new OBJECTS_DEF("x_obj", x12 ) );
		
		try {
			
			List<EXPR> expanded = EXPR.expandQuantifier( pe, lvar, objects, null );
			ArrayList<LTERM> x1y = new ArrayList<>(); x1y.add( x1 ); x1y.add( new LVAR("?y") );
			assertTrue(  expanded.get(0).equals( 
					new PVAR_EXPR("p", x1y  ) ) );
			ArrayList<LTERM> x2y = new ArrayList<>(); x2y.add( x2 ); x2y.add( new LVAR("?y") );
			assertTrue(  expanded.get(1).equals( 
					new PVAR_EXPR("p", x2y ) ) );
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Test
	public void testSubstituteAggExpr() {
		ArrayList<LVAR> xy = new ArrayList<LVAR>(); 
		xy.add( new LVAR("?x") ); xy.add( new LVAR("?y") );
		PVAR_EXPR pe = new PVAR_EXPR( "p", xy );
		
		ArrayList<LTYPED_VAR> lvar = new ArrayList<>(  Collections.singletonList(
				new LTYPED_VAR("?x", "x_obj") ) );
		
		ArrayList<LCONST> x123 = new ArrayList<LCONST>();
		x123.add( new OBJECT_VAL("x1") );x123.add( new OBJECT_VAL("x2") );x123.add( new OBJECT_VAL("x3") );
		
		HashMap<TYPE_NAME,OBJECTS_DEF> objects = new HashMap<RDDL.TYPE_NAME, RDDL.OBJECTS_DEF>();
		objects.put( new TYPE_NAME("x_obj"), new OBJECTS_DEF("x_obj", x123 ) );
		
		HashMap<LVAR, LCONST> subs_y = new HashMap<LVAR,LCONST>();
		subs_y.put( new LVAR("?y"), new OBJECT_VAL("y1" ) );
		
		HashMap<LVAR, LCONST> subs_x = new HashMap<LVAR,LCONST>();
		subs_x.put( new LVAR("?x"), new OBJECT_VAL("x1" ) );
		
		try {
			AGG_EXPR a_x = new AGG_EXPR( AGG_EXPR.SUM, lvar , pe );
			assertTrue( a_x.substitute( subs_y, null, objects ).equals( 
					new AGG_EXPR( AGG_EXPR.SUM, lvar, pe.substitute( subs_y, null, objects ) ) ) );

			//substitute for x should remove quantifier
			ArrayList<LTERM> x1y = new ArrayList<LTERM>(); 
			x1y.add( new OBJECT_VAL("x1") ); x1y.add( new LVAR("?y") );
			assertTrue( a_x.substitute( subs_x, null, objects ).equals( new PVAR_EXPR("p", x1y )  ) );
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Test
	public void testSubstitutePVAR_EXPR() {
		ArrayList<LVAR> xy = new ArrayList<>();
		xy.add( new LVAR("?x") );xy.add( new LVAR("?y") );
		PVAR_EXPR pe = new PVAR_EXPR("p", xy);
		
		ArrayList<LCONST> x1y1  = new ArrayList<>();
		x1y1.add( new OBJECT_VAL("x1") );x1y1.add( new OBJECT_VAL("y1") );
		HashMap<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants 
			= new HashMap<PVAR_NAME, Map<ArrayList<LCONST>, Object>>();
		constants.put( new PVAR_NAME("p"), Collections.singletonMap( x1y1, 5d ) );

		HashMap<LVAR, LCONST> subs_x1 = new HashMap<LVAR, LCONST> ();
		subs_x1.put( new LVAR("?x"), new OBJECT_VAL("x1") );
		
		HashMap<LVAR, LCONST> subs_x1y1 = new HashMap<LVAR, LCONST> ();
		subs_x1y1.put( new LVAR("?x"), new OBJECT_VAL("x1") );
		subs_x1y1.put( new LVAR("?y"), new OBJECT_VAL("y1") );
		
		//no subs
		assertTrue( pe.substitute( Collections.EMPTY_MAP, null, null).equals(pe) );
		
		//partial substitution
		assertTrue( pe.substitute( subs_x1, null, null ).equals( new PVAR_EXPR( "p", 
				new ArrayList<>( 
						xy.stream().map( m -> m.substitute(subs_x1, null, null) ).collect( Collectors.toList() ) ) ) ) );
		
		//substitute in constant
		assertTrue( pe.substitute(subs_x1y1, constants, null).equals( new REAL_CONST_EXPR( 5d ) ) );
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteFunExpr() {
		//FIXME
		try{
			throw new UnsupportedOperationException(this.getClass().toString() + " testSubstitute not  implemented. -ANR");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteIfExpr() throws Exception {
		EXPR x = new PVAR_EXPR("x", new ArrayList<>() );
		EXPR y = new PVAR_EXPR("y", new ArrayList<>() );
		
		EXPR e1 = new OPER_EXPR(x, y, OPER_EXPR.PLUS );
		EXPR e2 = new OPER_EXPR(x, y, OPER_EXPR.MINUS );
		//if 1 then e1 else e2 = e1
		assertTrue( new IF_EXPR( new BOOL_CONST_EXPR(true), e1, e2).equals( e1 ) );
		//if 0 then e1 else e2 = e2
		assertTrue( new IF_EXPR( new BOOL_CONST_EXPR(false), e1, e2).equals( e2 ) );
		//if NF then e1 else e2
		
		BOOL_EXPR NF = new PVAR_EXPR( "NF", new ArrayList<>() );
		IF_EXPR ife = new IF_EXPR( NF, e1, e2);
		Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants = new HashMap<>();
		constants.put( new PVAR_NAME("NF"), Collections.singletonMap( new ArrayList<>(), true ) );
		assertTrue( ife.substitute( Collections.EMPTY_MAP, constants, null ).equals( e1 ) );
		
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testSubstituteSwitchExpr() throws Exception {
		try{
			throw new UnsupportedOperationException("not yet tested.");
		}catch( UnsupportedOperationException exc ){
			exc.printStackTrace();
			throw exc;
		}
	}
	
	@Test
	public void testSubstituteQuantExpr() throws Exception {
		BOOL_CONST_EXPR c_true = new BOOL_CONST_EXPR(true);
		BOOL_CONST_EXPR c_false = new BOOL_CONST_EXPR(false);
		
		LTYPED_VAR x = new LTYPED_VAR("?x", "ex" );
		LTYPED_VAR y = new LTYPED_VAR("?y", "ey" );
		PVAR_EXPR p_x = new PVAR_EXPR( "p", new ArrayList<LTERM>( Collections.singletonList(x) ) );
		
		HashMap<TYPE_NAME, OBJECTS_DEF> objects = new HashMap<>();
		ArrayList<LCONST> exc_obj = new ArrayList<>();
		exc_obj.add( new OBJECT_VAL("x1") ); exc_obj.add( new OBJECT_VAL("x2") );
		objects.put( new TYPE_NAME("ex"), new OBJECTS_DEF( "ex", exc_obj  ) );
		
		//quant c = c
		assertTrue( new QUANT_EXPR( QUANT_EXPR.EXISTS, new ArrayList< LTYPED_VAR > ( 
				Collections.singletonList( x ) ), c_true ).equals( c_true ) );
		assertTrue( new QUANT_EXPR( QUANT_EXPR.FORALL, new ArrayList< LTYPED_VAR > ( 
				Collections.singletonList( x ) ), c_true ).equals( c_true ) );
		assertTrue( new QUANT_EXPR( QUANT_EXPR.EXISTS, new ArrayList< LTYPED_VAR > ( 
				Collections.singletonList( x ) ), c_false ).equals( c_false ) );
		assertTrue( new QUANT_EXPR( QUANT_EXPR.FORALL, new ArrayList< LTYPED_VAR > ( 
				Collections.singletonList( x ) ), c_false ).equals( c_false ) );
		
		//quant_x p(x) = T | constants
		Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> x1_true = new HashMap<>();
		x1_true.put( new PVAR_NAME("p"), Collections.singletonMap( 
				new ArrayList<LCONST>( Collections.singletonList( (LCONST)(new OBJECT_VAL("x1" )) ) ), true ) );
		Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> x1_false = new HashMap<>();
		x1_false.put( new PVAR_NAME("p"), Collections.singletonMap( 
				new ArrayList<LCONST>( Collections.singletonList( (LCONST)(new OBJECT_VAL("x1" )) ) ), false ) );
		
		assertTrue( new QUANT_EXPR( QUANT_EXPR.EXISTS, new ArrayList< LTYPED_VAR > ( 
				Collections.singletonList( x ) ), p_x ).substitute( Collections.EMPTY_MAP, x1_true, objects )
				.equals( c_true ) );
		assertTrue( new QUANT_EXPR( QUANT_EXPR.FORALL, new ArrayList< LTYPED_VAR > ( 
				Collections.singletonList( x ) ), p_x ).substitute( Collections.EMPTY_MAP, x1_false, objects )
				.equals( c_false ) );
		
		//quant_x,y q(x,y) | x = {x1}
		ArrayList<LTYPED_VAR> xy = new ArrayList<LTYPED_VAR>();
		xy.add(x); xy.add(y); 
		PVAR_EXPR q_xy = new PVAR_EXPR( "q", xy );
		Map<LVAR, LCONST> sub_x = new HashMap<>(); sub_x.put( x._sVarName, new OBJECT_VAL("x1") );
		//quantifier is expanded now
		QUANT_EXPR q = new QUANT_EXPR( QUANT_EXPR.EXISTS, xy, q_xy );
		assertTrue( q.substitute( sub_x ,  null, objects ).equals( 
				q.substitute( sub_x ,  null, objects ).substitute(sub_x, null, objects) ) );
	}
	
	@Test
	public void testSubstituteConnExpr() throws Exception {
		PVAR_EXPR x = new PVAR_EXPR("p", new ArrayList<>( ) );
		BOOL_CONST_EXPR c_false = new BOOL_CONST_EXPR( false );
		BOOL_CONST_EXPR c_true = new BOOL_CONST_EXPR( true );
		// x ^ F = F
		assertTrue( new CONN_EXPR(x, c_false, CONN_EXPR.AND).equals(c_false) );
		// x v F = x
		assertTrue( new CONN_EXPR(x, c_false, CONN_EXPR.OR).equals(x) );
		// x ^ T = x
		assertTrue( new CONN_EXPR(x, c_true, CONN_EXPR.AND).equals(x) );
		// x v T = T
		assertTrue( new CONN_EXPR(x, c_true, CONN_EXPR.OR).equals( c_true ) );
		// F => x = T - cant catch this because same does not hold for F=>x=>y and F=>x=>y=>z
//		assertTrue( new CONN_EXPR( c_false, x, CONN_EXPR.IMPLY).equals( c_true ) );
		// T => x = x 
		assertTrue( new CONN_EXPR( c_true, x, CONN_EXPR.IMPLY).equals( x ) );
		// x => T = T
		assertTrue( new CONN_EXPR(x, c_true, CONN_EXPR.IMPLY).equals( c_true ) );
		// x => F = x => F ; this case not caught by filter 
		assertTrue( new CONN_EXPR(x, c_false, CONN_EXPR.IMPLY).equals( new CONN_EXPR(x, c_false, CONN_EXPR.IMPLY) ) );
		
		// F => x => y = ( !F v x ) => y = ( F ^ !x ) v y = y 
		// 
		// x => NF
		BOOL_EXPR nf = new PVAR_EXPR( "nf", new ArrayList<>() );
		Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants = new HashMap<>();
		constants.put( new PVAR_NAME("nf"), Collections.singletonMap( 
				new ArrayList< LCONST >( Collections.EMPTY_LIST ) , true ) );
		assertTrue( new CONN_EXPR( x, nf , CONN_EXPR.IMPLY ).substitute( Collections.EMPTY_MAP, constants, null ).equals( c_true ) );
		
		// p(x) => q(y) 
		LVAR l_x = new LVAR("?x"); LVAR l_y = new LVAR("?y"); 
		PVAR_EXPR p_x = new PVAR_EXPR("p", new ArrayList<LTERM>( Collections.singletonList( (LTERM) l_x ) ) ) ;
		PVAR_EXPR q_y = new PVAR_EXPR("q", new ArrayList<LTERM>( Collections.singletonList( (LTERM) l_y ) ) ) ;
		Map<LVAR, LCONST> sub_x = new HashMap<>(); sub_x.put( l_x,  new OBJECT_VAL("x1") );
		assertTrue( new CONN_EXPR( p_x, q_y , CONN_EXPR.IMPLY ).substitute( sub_x , null, null ).equals( 
				new CONN_EXPR( p_x.substitute(sub_x, null, null), q_y.substitute(sub_x, null, null), CONN_EXPR.IMPLY ) ) );
		
	}

	@Test
	public void testSubstituteNegExpr() throws Exception {
		PVAR_EXPR pvar = new PVAR_EXPR( "p", new ArrayList<LCONST>() );
		NEG_EXPR ne = new NEG_EXPR( pvar );
		//neg(neg p) = p
		assertTrue( new NEG_EXPR( ne ).equals( pvar ) );
		//neg(neg(neg p)) = neg p
		assertTrue( new NEG_EXPR( new NEG_EXPR( ne ) ).equals( ne ) );
		//neg(T) = F
		assertTrue( new NEG_EXPR( new BOOL_CONST_EXPR(false) ).equals( new BOOL_CONST_EXPR( true ) ) );
		assertTrue( new NEG_EXPR( new BOOL_CONST_EXPR(true) ).equals( new BOOL_CONST_EXPR( false ) ) );
	}
	
}
