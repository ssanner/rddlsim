/**
 * RDDL: ParseException for use by CUP parser.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.parser;

import java.lang.Exception;

public class ParseException extends Exception {
	public int _nErrorLine;
	public String _sError;

	public ParseException(int line) {
		_nErrorLine = line;
		_sError = "No additional info";
	}

	public ParseException(int line, String s) {
		_nErrorLine = line;
		_sError = s;
	}

	public String toString() {
		return "Parse error on line " + _nErrorLine + ": " + _sError;
	}
}
