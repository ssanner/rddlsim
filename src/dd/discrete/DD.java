/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

import graph.Graph;

import java.io.PrintWriter;
import java.text.*;
import java.util.*;

/**
 * General class for implementation of ADD data structure
 **/
public abstract class DD {

    //////////////////////////////////////////////////////////////////
    //                           Constants
    //////////////////////////////////////////////////////////////////

	// For Graph Display Purposes
	public static final boolean USE_COLOR = true;
	public static final boolean USE_FESTIVE = false;
	
    // Static DD type identifiers
    public final static int TYPE_TABLE = 0;
    public final static int TYPE_ADD   = 1;
    public final static int TYPE_AADD  = 2;

    // Boolean/Arithmetic operation codes
    public static final int LOG_AND       = 10;
    public static final int LOG_OR        = 11;
    public static final int ARITH_SUM     = 0;
    public static final int ARITH_PROD    = 1;
    public static final int ARITH_MIN     = 2;
    public static final int ARITH_MAX     = 3;
    public static final int ARITH_DIV     = 4;
    public static final int ARITH_MINUS   = 5;
    public static final int RESTRICT_LOW  = 6;
    public static final int RESTRICT_HIGH = 7;

    // Pruning replacement types
    public static final int NO_REPLACE    = 0;
    public static final int REPLACE_LOW   = 1;
    public static final int REPLACE_HIGH  = 2;
    public static final int REPLACE_MIN   = 3;
    public static final int REPLACE_MAX   = 4;
    public static final int REPLACE_AVG   = 5;
    public static final int REPLACE_RANGE = 6;

    // Perform GC during flush caches?
    public final static boolean GC_DURING_FLUSH = false;

    // Max number of equivalence comparisons to do
    public final static int MAX_CMP = 200000;

    //////////////////////////////////////////////////////////////////
    //                        Static Variables
    //////////////////////////////////////////////////////////////////

    // Prune approximation settings
    public static int    PRUNE_TYPE       = NO_REPLACE;
    public static double PRUNE_PRECISION  = 1e-10;

    // For Comparison verification (see Compare.java)
    public static int MAX_ITER = 50;
        
    // No node ID can be negative
    public static final int INVALID    = -1;

    // The Java Runtime object
    public static Runtime RUNTIME = Runtime.getRuntime();

    // Not used at this time
    public static int GLOBAL_ID_COUNT  = 0;

    // For printing
    public static DecimalFormat _df = new DecimalFormat("#.###");

    // For timing
    public static long _lTime;
    
    //////////////////////////////////////////////////////////////////
    //                        Member Variables
    //////////////////////////////////////////////////////////////////

    // Members
    public HashMap    _hmID2VarName;
    public HashMap    _hmVarName2ID;
    public ArrayList  _alOrder;       // list of global var IDs for order
    public HashMap    _hmGVarToLevel; // maps from gid to level index in _alOrder

    // Nodes (and children) to keep when flushing caches
    public HashSet _hsSpecialNodes = new HashSet();

    //////////////////////////////////////////////////////////////////
    //                  Node Maintenance and Flushing
    //////////////////////////////////////////////////////////////////

    // Designate/remove/clear nodes to persist through flushing
    public void addSpecialNode(int n) {
	_hsSpecialNodes.add(new ADDRNode(n));
    }

    public void removeSpecialNode(int n) {
	_hsSpecialNodes.remove(new ADDRNode(n));
    }

    public void clearSpecialNodes() {
	_hsSpecialNodes.clear();
    }

    // Flush caches but save special nodes.  
    public abstract void flushCaches(boolean print_info);

    //////////////////////////////////////////////////////////////////
    //                         Construction
    //////////////////////////////////////////////////////////////////

    // Build a var ADD 
    public abstract int getVarNode(String var_name, double low, double high);

    /** @deprecated -- for backward compatibility, use at own risk **/
    public abstract int getVarNode(int var_id, double low, double high);

    // Build a constant ADD
    public abstract int getConstantNode(double val);

    // Build an ADD from a list (node is a list, high comes first for
    // internal nodes)
    public abstract int buildDDFromUnorderedTree(ArrayList l);
    
    // Build an ADD from a list with correct variable order (node is a
    // list, high comes first for internal nodes)
    public abstract int buildDDFromOrderedTree(ArrayList l);

    //////////////////////////////////////////////////////////////////
    //           Construction / Application / Evaluation
    //////////////////////////////////////////////////////////////////

    // Internal and external Apply
    public abstract int applyInt(int a1, int a2, int op);

    // For marginalizing out a node via sum, prod, max, or min.
    public abstract int opOut(int rid, int gid, int op);

    // For restricting a variable
    public abstract int restrict(int rid, int gid, int op);

    // Evaluate a DD: gid == val[assign_index] -> true/false
    public abstract double evaluate(int id, ArrayList assign);
 
    // Evaluate a DD using var -> assign map
    public abstract double evaluate(int id, HashMap var2assign);

    // Remap gids... gid_map = old_id -> new_id (assuming order consistent)
    public abstract int remapGIDsInt(int rid, HashMap gid_map);

    //////////////////////////////////////////////////////////////////
    //                    Arithmetic Operations
    //////////////////////////////////////////////////////////////////

    // Arithmetic operations
    public abstract double getMinValue(int id);
    public abstract double getMaxValue(int id);
    public abstract int    scalarMultiply(int id, double val);
    public abstract int    scalarAdd(int id, double val);
    public abstract int    invert(int id); // -ADD
    public abstract int    negate(int id); // 1/ADD

    //////////////////////////////////////////////////////////////////
    //                        Approximation
    //////////////////////////////////////////////////////////////////

    public abstract int    pruneNodes(int id);

    //////////////////////////////////////////////////////////////////
    //                     Internal Statistics
    //////////////////////////////////////////////////////////////////

    // Quick cache snapshot
    public abstract void showCacheSize();

    // Total cache snapshot
    public abstract long getCacheSize();

    // An exact count for the ADD rooted at _nRoot
    public abstract long countExactNodes(int id);

    // Set of the actual node ids
    public abstract Set getExactNodes(int id);

    // Set of the actual variable ids
    public abstract Set getGIDs(int id);

    //////////////////////////////////////////////////////////////////
    //                   Printing and Comparison
    //////////////////////////////////////////////////////////////////

    // Show pruning information
    public abstract void pruneReport();

    // Get graph of representation (for ADD, AADD)
    public abstract Graph getGraph(int id);
    
    // Export ADD to parsable tree format
    public abstract void exportTree(int n, PrintWriter ps, boolean label_branches);
    public abstract void exportTree(int n, PrintWriter ps, boolean label_branches, int level);
    
    // Print out a node showing internal representation
    public abstract String printNode(int id);

    // Printing out the full table
    public static void PrintEnum(DD a, int id) {

	// Traverse all paths and verify output
	int nvars = a._alOrder.size();
	ArrayList assign = new ArrayList();
	Boolean TRUE  = new Boolean(true);
	Boolean FALSE = new Boolean(false);

	// Initialize assignment
	for (int c = 0; c < nvars; c++) {
	    assign.add(FALSE); 
	}
	
	// Now print table
	for (int c = 0; c < (1 << nvars); c++) {

	    // Set all bits
	    // c >> 0 is low bit which comes last (pos 2)
	    for (int b = nvars - 1; b >= 0; b--) {
		assign.set(b, (((c >> (nvars - b - 1)) & 1) == 1) ? TRUE : FALSE);
	    }

	    // Now show the assignment
	    System.out.println("Assignment: " + 
			       PrintBitVector(assign) + " -> " + 
			       DD._df.format(a.evaluate(id, assign)));
	}
	System.out.println();

    }

    // Helper function 
    public static String PrintBitVector(ArrayList l) {
    	return PrintBitVector(l, true);
    }

    public static String PrintBitVector(ArrayList l, boolean print_brackets) {
		StringBuffer sb = new StringBuffer();
		if (print_brackets) sb.append("[ ");
		Iterator i = l.iterator();
		while (i.hasNext()) {
		    Boolean b = (Boolean)i.next();
		    sb.append( b.booleanValue() ? "1 " : "0 ");
		}
		if (print_brackets) sb.append("]");
	
		return sb.toString();
    }
        
    // Compare ADD and AADD over all assignments (assume same vars)
    public static double CompareEnum(DD a1, int n1, DD a2, int n2) {

	// Traverse all paths and verify output
	double max_diff = 0.0d;
	int nvars = a1._alOrder.size();
	ArrayList assign = new ArrayList();
	Boolean TRUE  = new Boolean(true);
	Boolean FALSE = new Boolean(false);

	// Initialize assignment
	for (int c = 0; c < nvars; c++) {
	    assign.add(FALSE); 
	}
	
	// Now compare all assignments
	boolean equiv = true;
	int c = 0;
	for (; (c < (1 << nvars)) && equiv && c < MAX_CMP; c++) {

	    // Set all bits
	    // c >> 0 is low bit which comes last (pos 2)
	    for (int b = nvars - 1; b >= 0; b--) {
		assign.set(b, (((c >> (nvars - b - 1)) & 1) == 1) ? TRUE : FALSE);
	    }

	    // Compare values for this assignment
	    double val1 = a1.evaluate(n1, assign);
	    double val2 = a2.evaluate(n2, assign);
	    double diff = Math.abs(val1 - val2);

	    if (max_diff < diff) {
		max_diff = diff;
	    } 
	    if (Double.isNaN(diff) || 
		diff == Double.POSITIVE_INFINITY || 
		diff == Double.NEGATIVE_INFINITY) {
		max_diff = Double.NaN;
	    }
	}
	
	//System.out.println("Iter: " + c + ", nvars: " + nvars);
	return max_diff;
    }

    //////////////////////////////////////////////////////////////////
    //                        Miscellaneous
    //////////////////////////////////////////////////////////////////

    // Reset the timer
    public static void ResetTimer() {
	_lTime = System.currentTimeMillis();
    }

    // Get the elapsed time since resetting the timer
    public static long GetElapsedTime() {
	return System.currentTimeMillis() - _lTime;
    }
}