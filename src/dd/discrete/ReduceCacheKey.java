/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

public class ReduceCacheKey 
    implements Comparable {
    
    public Object _pObj;
    public int    _nGID;
    public int    _nOp;
    public int    _nLID;
    
    public ReduceCacheKey(Object o, int gid, int op, int lid) {
	_pObj    = o;
	_nGID    = gid;
	_nOp     = op;
	_nLID    = lid;
    }
    
    public int hashCode() {

	long d1;
	int  i1;

	return _pObj.hashCode() + _nGID + (_nOp << 7) + (_nLID << 10);
    }
    
    public boolean equals(Object o) {
	ReduceCacheKey s = (ReduceCacheKey)o;
	return ((_pObj    == s._pObj) && 
		(_nGID    == s._nGID) &&
		(_nOp     == s._nOp) &&
		(_nLID    == s._nLID));
    }

    public int compareTo(Object o) {

	ReduceCacheKey s = (ReduceCacheKey)o;

	// Check for source inequality
	if (this._pObj != s._pObj) {
	    int this_hash = this._pObj.hashCode();
	    int s_hash    = s._pObj.hashCode();
	    if (this_hash < s_hash) {
		return (-1);
	    } else if (this_hash > s_hash) {
		return (1);
	    }
	    //System.out.println("ReduceCacheKey: Objects not equal but hashcodes equal!!!");
	    //System.exit(1);
	    return this._pObj.toString().compareTo(s._pObj.toString());
	}

	// Check for OP inequality
	if (this._nOp < s._nOp) {
	    return (-1);
	} else if (this._nOp > s._nOp) {
	    return (1);
	}

	// Check for ID inequality
	if (this._nGID < s._nGID) {
	    return (-1);
	} else if (this._nGID > s._nGID) {
	    return (1);
	}

	// Check for Local ID inequality
	if (this._nLID < s._nLID) {
	    return (-1);
	} else if (this._nLID > s._nLID) {
	    return (1);
	}

	return (0);
    }

    public static double abs(double a) { return (a < 0) ? -a : a; }
}
