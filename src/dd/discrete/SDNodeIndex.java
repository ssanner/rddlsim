/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

import java.util.*;

public class SDNodeIndex {
    
    public double _dLowerVal;
    public double _dUpperVal;
    
    public SDNodeIndex(double min, double max) {
	_dLowerVal = min;
	_dUpperVal = max;
    }
    
    public int hashCode() {
	// Will only be identical for min/max with same
	// average - TODO: Need to check this distribution!!!
	return (int)((Double.doubleToLongBits(_dLowerVal) >>> 25)+1 +
		     (Double.doubleToLongBits(_dUpperVal) >>> 25)+1);
    }
    
    public boolean equals(Object o) {
	SDNodeIndex s = (SDNodeIndex)o;
	return (Math.abs(_dLowerVal - s._dLowerVal) <= 1e-10d && 
		Math.abs(_dUpperVal - s._dUpperVal) <= 1e-10d);
    }

    public static class SDNComparator
	implements Comparator {
	
	public boolean _bInc;

	public SDNComparator(boolean inc) {
	    _bInc = inc;
	}

	public int compare(Object o1, Object o2) {
	    
	    SDNodeIndex d1 = (SDNodeIndex)o1;
	    SDNodeIndex d2 = (SDNodeIndex)o2;

	    if (_bInc) {
		
		if (d1._dLowerVal < d2._dLowerVal) {
		    return -1;
		} else if (d1._dLowerVal > d2._dLowerVal) {
		    return 1;
		} 
		
		if (d1._dUpperVal < d2._dUpperVal) {
		    return -1;
		} else if (d1._dUpperVal > d2._dUpperVal) {
		    return 1;
		} 

		// Same so return 0
		return 0;

	    } else {

		if (d1._dUpperVal > d2._dUpperVal) {
		    return -1;
		} else if (d1._dUpperVal < d2._dUpperVal) {
		    return 1;
		} 
		
		if (d1._dLowerVal > d2._dLowerVal) {
		    return -1;
		} else if (d1._dLowerVal < d2._dLowerVal) {
		    return 1;
		} 
		
		// Same so return 0
		return 0;

	    }
	}

	public boolean equals(Object o) {
	    return (this == o);
	}
    }
}
