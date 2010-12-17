/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

import graph.Graph;

public abstract class ADDNode {
    
    // Only used to give unique ID to every internal
    // node - not var ID! - should only be used for
    // saving and for node identification in Apply()
    // and low/high comparison.
    public int _nLocalID;  
    
    public abstract String toString(Object context, int depth);

    public abstract void toGraph(Object context, Graph g);
    
    public static String indent(int depth) {
		int i;
		StringBuffer sb = new StringBuffer();
		for (i = 0; i <= depth; i++) {
			sb.append("   ");
		}
		return sb.toString();
	}
}

