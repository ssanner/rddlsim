/**
 * Algebraic Decision Diagram Package -- Testing Methods
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

import graph.Graph;
import java.util.*;

public class Test {

	public static final int    MAX_GID = 7; // 5 -- 7 produces nice viewable diagrams
	public static final double PRUNE_PRECISION = 0.1d;
	public static final int    REPLACE_TYPE = DD.REPLACE_AVG; // NO_REPLACE, REPLACE_AVG
	public static final boolean SHOW_CACHE = false;
	
	public static Random _rand = new Random();
	public static long _lTime; // Internal timing stats
	public static ArrayList _order;

	// General testing methods for decision diagrams
	public static void main(String[] args) {

		// Initialize variables and order
	    ArrayList order = new ArrayList();
	    for (int i = 1; i <= MAX_GID; i++)
	    	order.add("x" + i); 
	    DD context = new ADD(order); 
	    
	    // Perform tests
	    TestEval(context);
		//TestCompression(context);
	}

	public static void TestEval(DD context) {

	    int dd = GetCountingDD(context, MAX_GID, false); // GetCountingDD, GetExpDD, true/false
	    
	    // Test different display methods
	    System.out.println(context.printNode(dd) + "\n");
	    DD.PrintEnum(context, dd);
	    context.getGraph(dd).launchViewer(1300, 770);

		// Assign each variable to a boolean value
		HashMap test_eval = new HashMap();
	    for (int i = 1; i <= MAX_GID; i++) {
	    	test_eval.put("x" + i, (i % 2) == 1 ? true : false);
	    }
	    
		// Test evaluation
		System.out.println("Eval final graph with\n - " + test_eval + "\n");
		System.out.println(" = " + context.evaluate(dd, test_eval));
	}

	public static void TestCompression(DD context) {

		// Prune?
		DD.PRUNE_TYPE = REPLACE_TYPE;
		DD.PRUNE_PRECISION = PRUNE_PRECISION;

	    int count_dd = GetCountingDD(context, MAX_GID, false); // GetCountingDD, GetExpDD, true/false
	    //int count_dd2 = GetExpDD(context, MAX_GID, false); // GetCountingDD, GetExpDD, true/false
	    //count_dd = context.applyInt(count_dd, count_dd2, DD.ARITH_SUM);
	    //int count_dd = GetExpDD(context, MAX_GID, false); // GetCountingDD, GetExpDD, true/false
	    //int count_dd = GetExpSumProdDD(context, (int)(MAX_GID/2), MAX_GID, 0.9d);
	    //int count_dd = GetExpProdDD(context, MAX_GID, 0.9d); // GetCountingProdDD, GetExpProdDD
	    System.out.println("Count DD [" + context.countExactNodes(count_dd) + "]:");
	    System.out.println(context.printNode(count_dd) + "\n");
	    //DD.PrintEnum(context,count_dd);

		Graph g3 = context.getGraph(count_dd);
		g3.genDotFile("clean.dot");
		g3.launchViewer(1300, 770);

		// AddPairNoise vs. AddAllPairNoise
	    int noise_count_dd = AddAllPairNoise(context, MAX_GID/* + 1*/, count_dd, 0.01d, false); // AddPairNoise
	    System.out.println("\n\nNoisy DD [" + context.countExactNodes(noise_count_dd) + "]:");
	    System.out.println(context.printNode(noise_count_dd) + "\n");
	    //DD.PrintEnum(context,noise_count_dd);

	    Graph g = context.getGraph(noise_count_dd);
		g.genDotFile("noisy.dot");
		g.launchViewer(1300, 770);

	    int reduce_noise_dd = context.pruneNodes(noise_count_dd);
	    //reduce_noise_dd = context.applyInt(reduce_noise_dd, reduce_noise_dd, DD.ARITH_SUM);
	    //int reduce_noise_dd2 = ((AADD)context).reduce(reduce_noise_dd);
	    System.out.println("\n\nReduced Noisy DD [" + context.countExactNodes(reduce_noise_dd) + "]:");
	    System.out.println(context.printNode(reduce_noise_dd) + "\n");
	    //DD.PrintEnum(context,reduce_noise_dd);
	    int dd_result = context.applyInt(noise_count_dd, reduce_noise_dd, DD.ARITH_MINUS);
	    System.out.println("Max approximation error: " + Math.max(Math.abs(context.getMinValue(dd_result)), 
	    							  		   Math.abs(context.getMaxValue(dd_result))));

		Graph g2 = context.getGraph(reduce_noise_dd);
		g2.genDotFile("reduced.dot");
		g2.launchViewer(1300, 770);
	}
	
	// Reset the timer
	public static void ResetTimer() {
		_lTime = System.currentTimeMillis();
	}

	// Get the elapsed time since resetting the timer
	public static long GetElapsedTime() {
		return System.currentTimeMillis() - _lTime;
	}

    // Make a randomized DD 
	public static int GetRandomizedDD(DD context, int num_vars, int num_sums) {
		
		int ret = context.getConstantNode(1d);
		
		for (int i = 0; i < num_sums; i++) {
		
			int assign = _rand.nextInt(1 << num_vars);
			double val = _rand.nextDouble();
			//System.out.println(assign + ": " + val);
			ret = context.applyInt(ret, GetJointDD(context, num_vars, assign, val), DD.ARITH_SUM);
			
		}
		
		return ret;
	}
	
	public static int GetJointDD(DD context, int num_bits, int var_assign, double val) {
		int ret = context.getConstantNode(val);
		for (int i = 1; i <= num_bits; i++) {
			//System.out.println(var_assign + ": " + (var_assign % 2));
			boolean local_assign = (var_assign % 2) == 1;
			var_assign = var_assign >> 1;
			ret = context.applyInt(ret, context.getVarNode(i, local_assign ? 0d : 1d, 
					local_assign ? 1d : 0d), DD.ARITH_PROD);
		}
		return ret;
	}
	
	// Returns a counting ADD from gid 1..max_gid
	public static int GetCountingDD(DD context, int max_gid, boolean skip) {
		int ret = context.getVarNode(/* gid */1, /* low */0d, /* high */1d);
		for (int i = (skip ? 3 : 2); i <= max_gid; i += (skip ? 2 : 1)) {
			ret = context.applyInt(ret, context.getVarNode(i, 0d, 1d),
					DD.ARITH_SUM);
		}
		return ret;
	}

	// Returns an exponential coefficient ADD (a + 2b + 4c + ...) from
	// 1..max_gid
	public static int GetExpDD(DD context, int max_gid, boolean skip) {
		int ret = context.getVarNode(1, 0d, 1d);
		int coef = 1;
		for (int i = (skip ? 3 : 2); i <= max_gid; i += (skip ? 2 : 1)) {
			coef = coef << 1;
			ret = context.applyInt(ret, context.getVarNode(i, 0d, (double) coef), 
					DD.ARITH_SUM);
		}
		return ret;
	}

	// Returns a product ADD (abcd... = val) from 1..max_gid
	public static int GetCountingProdDD(DD context, int max_gid, double gamma) {
		int ret = context.getVarNode(1, 1d, Math.pow(gamma, 1d));
		for (int i = 2; i <= max_gid; i++) {
			ret = context.applyInt(ret, context.getVarNode(i, 1d, Math.pow(
					gamma, 1d)), DD.ARITH_PROD);
		}
		return ret;
	}

	// Returns a product ADD (abcd... = val) from 1..max_gid
	public static int GetExpProdDD(DD context, int max_gid, double gamma) {
		int coef = 1;
		int ret = context.getVarNode(1, 1d, Math.pow(gamma, (double) coef));
		for (int i = 2; i <= max_gid; i++) {
			coef = coef << 1;
			ret = context.applyInt(ret, context.getVarNode(i, 1d, Math.pow(
					gamma, (double) coef)), DD.ARITH_PROD);
		}
		return ret;
	}

	public static int GetExpSumProdDD(DD context, int middle_var_id, int max_var_id, double gamma) {
		
		// Build \sum_{i=1}^{middle_var_id} 2^i x_i
		int sum_dd = context.getVarNode(/* var_id */1, /* low */0d, /* high */1d);
		for (int i = 2; i <= middle_var_id; i++) {
			sum_dd = context.applyInt(sum_dd, context.getVarNode(i, 0d, 1d),
					DD.ARITH_SUM);
		}
		
		// Build \prod_{i=middle_var_id+1}^{max_var_id} \gamma^{2^i x_i}
		int coef = 1;
		int prod_dd = context.getVarNode(middle_var_id+1, 1d, 
				Math.pow(gamma, (double) coef));
		for (int i = middle_var_id+2; i <= max_var_id; i++) {
			coef = coef << 1;
			prod_dd = context.applyInt(prod_dd, context.getVarNode(i, 1d, 
					Math.pow(gamma, (double) coef)), DD.ARITH_PROD);
		}
		
		// Add them together and return
		return context.applyInt(sum_dd, prod_dd, DD.ARITH_SUM);
	}

	public static int AddPairNoise(DD context, int max_gid, int dd, 
			double noise_level, boolean rand_noise) {
		
		// Handle the wraparound pair
		int pair = context.applyInt(context.getVarNode(1, 0d, noise_level), 
				                    context.getVarNode(max_gid, 0d, 1d), DD.ARITH_PROD);
		int ret = context.applyInt(dd, pair, DD.ARITH_SUM);

		// Multiply in sequential pairs
		for (int i = 1; i < max_gid; i++) {
			pair = context.applyInt(context.getVarNode(i, 0d, 
					rand_noise ? noise_level*_rand.nextDouble() : noise_level), 
                          			context.getVarNode(i + 1, 0d, 1d), DD.ARITH_PROD);
			ret = context.applyInt(ret, pair, DD.ARITH_SUM);
		}
		return ret;
	}

	public static int AddAllPairNoise(DD context, int max_gid, int dd, 
			double noise_level, boolean rand_noise) {
	
		// Handle the wraparound pair
		int pair = context.applyInt(context.getVarNode(1, 0d, noise_level), 
				                    context.getVarNode(max_gid, 0d, 1d), DD.ARITH_PROD);
		int ret = context.applyInt(dd, pair, DD.ARITH_SUM);
		
		// Multiply in all pairs
		for (int i = 1; i <= max_gid; i++) {
			for (int j = i; j <= max_gid; j++) { 
				pair = context.applyInt(context.getVarNode(i, 0d, 
						rand_noise ? noise_level*_rand.nextDouble() : noise_level), 
	                          			context.getVarNode(j, 0d, 1d), DD.ARITH_PROD);
				ret = context.applyInt(ret, pair, DD.ARITH_SUM);
			}
		}
		return ret;
	}
}
