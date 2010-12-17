/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

public class SNodePair {
    
    public int _nA1;
    public int _nA2;
    public int _nOp;
    
    public SNodePair(int lid1, int lid2, int op) {
	_nA1 = lid1;
	_nA2 = lid2;
	_nOp = op;
    }
    
    public SNodePair(int lid1, int lid2) {
	_nA1 = lid1;
	_nA2 = lid2;
	_nOp = 0;
    }
    
    public int hashCode() {
	// Perfect hash up to 2^16 = 65536 nodes				   
	return _nA1 + (_nA2 << 16) + (_nA2 >> 16) + (_nOp << 24) + (_nOp >> 8); 
    }
    
    public boolean equals(Object o) {
	SNodePair s = (SNodePair)o;
	return ((_nA1 == s._nA1) && (_nA2 == s._nA2) && (_nOp == s._nOp));
    }
}
