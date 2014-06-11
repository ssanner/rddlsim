//////////////////////////////////////////////////////////////////////
//
// File:     Token.java
// Project:  MIE457F Information Retrieval System
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
//
//////////////////////////////////////////////////////////////////////

// Package definition
package ppddl;

// Packages to import
import java.io.*;
import java.math.*;
import java.util.*;

/**
 * A generic token structure for TokenStream
 *
 * @version   1.0
 * @author    Scott Sanner
 * @language  Java (JDK 1.3)
 **/
public class Token
{
    // Note: We could add more information to a token such as
    // its type (e.g., symbol, number, string, etc...) but
    // this structure suffices for an initial implementation.

    /* Constants */
    public static final int LPAREN = 1;
    public static final int RPAREN = 2;
    public static final int PERIOD = 3;
    public static final int COMMA  = 4;
    public static final int SEMI   = 5;
    public static final int COLON  = 6;
    public static final int SLASH  = 7;

    /** Data members **/
    public String  _sSourceDoc; // Source location for token
    public String  _sToken;     // String associated with a token
    public int     _nPos;       // Character position in file (absolute)
    public int     _nLine;      // Line number in file
    public int     _nLinePos;   // Char position on current line
    public int     _nSymbolID;  // If token null then this is the symbol ID
    public boolean _bInteger;   // Indicates if the String is numeric
    public BigInteger _biIntValue;  // Integer value if an integer

    /** Constructor
     *  @param token    String associated with token
     *  @param src      Source file/URL for token
     *  @param pos      Absolute char position in file
     *  @param line     Absolute line position in a file
     *  @param line_pos Char position relative to line
     **/
    public Token(String token, String src, int pos, 
		 int line, int line_pos) {
	_sToken     = token;
	_sSourceDoc = src;
	_nPos       = pos;
	_nLine      = line;
	_nLinePos   = line_pos;
	_biIntValue = null;

	if (_sToken.length() == 1) {
	    char c = _sToken.charAt(0);
	    switch(c) {
	    case '(': _nSymbolID = LPAREN; _sToken = null; break;
	    case ')': _nSymbolID = RPAREN; _sToken = null; break;
	    case '.': _nSymbolID = PERIOD; _sToken = null; break;
	    case ',': _nSymbolID = COMMA;  _sToken = null; break;
	    case ';': _nSymbolID = SEMI;   _sToken = null; break;
	    case ':': _nSymbolID = COLON;  _sToken = null; break;
	    case '/': _nSymbolID = SLASH;  _sToken = null; break;
	    }
	}

	if (_sToken != null) {
	    try {
		_biIntValue = new BigInteger(_sToken);
		_bInteger   = true;
	    } catch (NumberFormatException e) { 
		_bInteger   = false;
	    }
	}
    }

    /** Get string for class
     *  @return String representing class contents
     **/
    public String toString() {
	return "<" + (_bInteger ? "#" + _biIntValue : 
		      (_sToken == null ? "ID:" +_nSymbolID : "'" + _sToken + "'")) + 
	    ", Source:'" + _sSourceDoc + "', Pos:" + 
	    _nPos + ", Line:" + _nLine + ", LinePos:" + _nLinePos + ">";
    }
}
