//////////////////////////////////////////////////////////////////////
//
// File:     TokenStreamException.java
// Project:  MIE457F Information Retrieval System
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
//
//////////////////////////////////////////////////////////////////////

// Package definition
package ppddl;

// Packages to import
import java.lang.*;

/**
 * An exception thrown by TokenStream
 *
 * @version   1.0
 * @author    Scott Sanner
 * @language  Java (JDK 1.3)
 **/
public class TokenStreamException
    extends Exception {

    /** Local data members **/
    public String _sSrc;
    public int    _nPos;
    public int    _nLine;
    public int    _nLinePos;

    /** Constructor
     *  @param error String specifying the error that occurred
     *  @param src   Source file/URL where error occurred
     **/
    public TokenStreamException(String error, String src) {
	super(error);
	_sSrc  = src;
	_nLine = _nLinePos = -1;
    }

    /** Constructor
     *  @param error String specifying the error that occurred
     *  @param src   Source file/URL where error occurred
     *  @param line  Line number where error occurred
     *  @param pos   Char position on line where error occurred
     **/
    public TokenStreamException(String error, String src, int line, int line_pos) {
	super(error);
	_sSrc     = src;
	_nLine    = line;
	_nLinePos = line_pos;
    }
}
