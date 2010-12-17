/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

public class SINodeIndex {
    
    public int _nGid;
    public int _nLow;
    public int _nHigh;
    
    public SINodeIndex(int gid, int low, int high) {
	_nGid  = gid;
	_nLow  = low;
	_nHigh = high;
    }
    
    public int hashCode() {
	// Perfect hash up to 2^10 = 1024 nodes				   
	return _nGid + _nLow * (1 << 10) + _nHigh * (1 << 20); 
    }
    
    public boolean equals(Object o) {
	SINodeIndex s = (SINodeIndex)o;
	return ((_nGid == s._nGid) && (_nLow == s._nLow) && (_nHigh == s._nHigh));
    }
}
