//////////////////////////////////////////////////////////////////////
//
// File:     TokenStream.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
//
//////////////////////////////////////////////////////////////////////

// Package definition
package ppddl;

// Packages to import
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Tokenizes a generic text document
 *
 * @version   1.0
 * @author    Scott Sanner
 * @language  Java (JDK 1.3)
 **/
public class TokenStream {

    /** Static Constants **/
    protected static final int            EOF = -1;
    protected static final String VALID_CHARS = "-?:+/_";

    /** Non-static Data Members **/
    protected String               _sFilename;
    protected BufferedReader       _brInput;
    protected int                  _nLine;
    protected int                  _nLinePos;
    protected int                  _nPos;
    protected boolean              _bIsHTML;
    protected boolean              _bInComment;
    protected boolean              _bPushBack;
    protected char                 _cPushBack;

    /** Constructor - default
     **/
    public TokenStream() {
	_sFilename    = null;
	_brInput      = null;
	_nPos         = -1; // No chars read yet
	_nLinePos     = -1; // No chars read yet
	_nLine        = 1;  // Start at line 1
	_bInComment   = false;
	_bIsHTML      = false;
	_bPushBack    = false;
    }

    /** Open a file for tokenizing
     *  @param filename File from which to load data
     **/
    public void open(String name) 
	throws TokenStreamException {

	// Initialize components
	_sFilename = name;
	_nPos = _nLinePos = -1; // No chars read yet
	_nLine = 1;             // Start at line 1
	if (_brInput != null) {
	    close();
	}

	// Open the specified file
	try {
	    
	    if (name.toLowerCase().startsWith("http://")) {
		URL url = new URL(name);
		InputStream is = url.openConnection().getInputStream(); 
		_brInput = new BufferedReader(new InputStreamReader(is));
		_bIsHTML = true;
	    } else {
		_brInput = new BufferedReader(new FileReader(name));
	    }

	} catch (MalformedURLException ex) {
	    _sFilename = null;
	    _brInput   = null;
	    throw new TokenStreamException("Malformed URL", name);
	} catch (FileNotFoundException ex) {
	    _sFilename = null;
	    _brInput   = null;
	    throw new TokenStreamException("File not found", name);
	} catch (IOException ex) {
	    _sFilename = null;
	    _brInput   = null;
	    throw new TokenStreamException("IOException: " + ex, name);
	}
    }

    /** Close the file currently being tokenized
     **/
    public void close() 
	throws TokenStreamException {
	
	// Close the reader and reset 
	try {
	    if (_brInput != null) {
		_brInput.close();
		_brInput = null;
	    }

	} catch (IOException ex) {
	    throw new TokenStreamException("IOException: " + ex, 
					   _sFilename, _nLine, _nPos);
	}
    }
    
    /** Returns the next Token from a TokenStream or null
     *  if the end of the stream has been reached.  Throws
     *  a TokenStreamException if there is an IO error.
     *  @return The next token or null (if no tokens remain)
     **/    
    public Token nextToken() 
	throws TokenStreamException {

	StringBuffer token = new StringBuffer();
	char[] cbuf = new char[1];
	int token_len, cur_line_pos, cur_line;

	// Read a character at a time from the current line,
	// return on EOF or when current token complete
	while (true) {

	    // Read a character, check for EOF
	    try {
		if (_bPushBack) {
		    _bPushBack = false;
		    cbuf[0]    = _cPushBack;
		} else if (_brInput.read(cbuf, 0, 1) == EOF) {
		    token_len = token.length();
		    if (token_len > 0) {
			return new Token(token.toString(),
					 _sFilename, _nPos - token_len,
					 _nLine, _nLinePos - token_len); 		    
		    } else {
			return null;
		    }
		} else {
		    ++_nPos; // Read was successful
		}

		// Use temp line pos to retain current position
		// in case a newline resets it, otherwise position
		// will be calculated incorrectly when token returned
		cur_line = _nLine;
		cur_line_pos = ++_nLinePos;
		
	    } catch (IOException ex) {
		throw new TokenStreamException("IOException: " + ex, 
					       _sFilename, _nLine, _nPos);
	    }

	    // Handle newline
	    if (cbuf[0] == '\n') {
		++_nLine;
		_nLinePos = -1;
		_bInComment = false;
	    }

	    //////////////////////////////////////////////////////////
	    // Students: Modify the following section of code to
	    // handle additional delimiters, lowercase conversion 
	    // etc...
	    //////////////////////////////////////////////////////////

	    // This is a valid, non-tag character so decide what to
	    // do...  namely, if char is a delimiter then return the
	    // current token if it exists.
	    if (cbuf[0] == ';' || _bInComment) {
		_bInComment = true;
	    } else if (!Character.isLetterOrDigit(cbuf[0]) && !(VALID_CHARS.indexOf(cbuf[0]) >= 0)) {
		
		// A delimiter so return token if non-empty,
		// otherwise ignore character and continue
		boolean ws = isWhiteSpace(cbuf[0]);
		if (!ws) {
		    if (token.length() > 0) {
			_cPushBack = cbuf[0];
			_bPushBack = true;
		    } else {
			token.append(cbuf[0]);
		    }
		}

		token_len = token.length();
		if (token_len > 0) {
		    return new Token(token.toString(),
				     _sFilename, _nPos - token_len,
				     cur_line, cur_line_pos - token_len);
		}
		
	    } else {

		// Character is a letter or digit, so append
		token.append(cbuf[0]);
	    }

	    //////////////////////////////////////////////////////////
	}
    }

    public static boolean isWhiteSpace(char c) {
	switch (c) {
	case ' ':  return true;
	case '\n': return true;
	case '\t': return true;
	case '\r': return true;
	default:   return false;
	}
    }

}
