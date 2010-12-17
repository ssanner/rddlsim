/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

public class ADDRNode {
    
    // This is assigned by an outer struct and gives
    // access to formula assoc with global-ID, is
    // analogy of prop var ID for ADDs.
    public int _lid;
    
    public ADDRNode(int lid) {
	_lid = lid;
    }

    public void set(int lid) {
	_lid = lid;
    }
    
    public String toString() {
	return "<" + _lid + ">";
    }

    public String toString(Object context, int depth) {
	StringBuffer sb = new StringBuffer();	
	if (context instanceof ADD) {
	    sb.append("[ " + ((ADD)context).getNode(_lid).toString(context,depth+1));
	} else {
	    return "[ ERROR: " + context.getClass() + " ] "; 
	}
	sb.append("]");	
	return sb.toString();
    }
    
    public int hashCode() {
	return _lid;
    }
    
    public boolean equals(Object o) {
	return (_lid == ((ADDRNode)o)._lid);
    }
}

