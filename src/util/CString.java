/** This class is for interned strings -- it hashes and tests equality much
 *  faster than the standard String class.  (Speeds up code up to 7x in my
 *  exprience).
 *  
 *  @author ssanner@gmail.com
 */
package util;

public class CString implements Comparable {

	int _hashCode;
	String _string;
	
	public CString(String s) {
		_string = s.intern();
		_hashCode = _string.hashCode();
	}
	
	public int hashCode() {
		return _hashCode;
	}
	
	public boolean equals(Object o) {
		if (o instanceof CString)
			return this._string == ((CString)o)._string;
		else
			return false;
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof CString)
			return this._string.compareTo(((CString)o)._string);
		else
			return 0;
	}
	
	public String toString() {
		return _string;
	}
}
