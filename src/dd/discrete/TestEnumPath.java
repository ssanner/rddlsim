package dd.discrete;

import java.util.ArrayList;

public class TestEnumPath {

	// Demonstrates how to enumerate paths
	public static void main(String[] args) {
		ArrayList<String> vars = new ArrayList();
		vars.add("a");
		vars.add("b");
		vars.add("c");
		ADD context = new ADD(vars);
		int dd = GetCountingDD(context, vars);
		
		// Show the DD whose paths are enumerated
		System.out.println("ADD:\n====\n" + context.printNode(dd));
		
		// Create your leaf processing operation inline as follows
		// Here the path and leaf value are simply printed to System.out
		System.out.println("\nPath enumeration:\n=================");
		context.enumeratePaths(dd, 
			new ADD.ADDLeafOperation() {
				public void processADDLeaf(ArrayList<String> assign,
						double leaf_val) {
					System.out.println(assign + " -> " + leaf_val);
				}
			});
	}

	// Constructs a DD: \sum_var (var ? 1d : 0d)
	public static int GetCountingDD(DD context, ArrayList<String> vars) {
		int ret = context.getConstantNode(0d);
		for (String var : vars)
			ret = context.applyInt(ret, context.getVarNode(var, 0d, 1d), DD.ARITH_SUM);
		return ret;
	}
}
