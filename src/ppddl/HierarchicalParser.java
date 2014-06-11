//////////////////////////////////////////////////////////////////////
//
// File:     HierarchicalParser.java
// Author:   Scott Sanner, University of Toronto (ssanner@cs.toronto.edu)
// Date:     9/1/2003
// Requires: comshell package
//
// Description:
//
//   Parsing of hierarchical files (i.e. LISP-like).  Really should
//   use a regular expression tokenizer like jlex... this is a bit of
//   a hack.
//
//////////////////////////////////////////////////////////////////////

// Package definition
package ppddl;

// Packages to import
import java.io.*;
import java.math.*;
import java.util.*;

/**
 * Input helper class.
 *
 * @version   1.0
 * @author    Scott Sanner
 * @language  Java (JDK 1.3)
 **/
public class HierarchicalParser
{
    /** Keyword identifier
     **/
    public static class Keyword { 
	public String _s;
	public Keyword(String s) {
	    _s = s;
	}
	public String toString() {
	    return "K:[" +_s + "]";
	}
	public boolean matches(String s) {
	    return _s.equalsIgnoreCase(s);
	}
    }

    /** Static file parsing methods
     **/
    public static ArrayList ParseFile(String filename) {

	try {
	    TokenStream ts = new TokenStream();
	    ts.open(filename);
	    return ParseFileInt(ts, 0);
	} catch (TokenStreamException tse) {
	    System.out.println("Error: " + tse);
	    return null;
	}

    }

    /** Handles paren nesting and converting Integer.Integer -> Double.
     *  Assumes an Integer must follow a period.
     **/
    public static ArrayList ParseFileInt(TokenStream ts, int level) {

	ArrayList a = new ArrayList();
	//StringBuffer sb = new StringBuffer("\n");
	//for (int i = 0; i < level; i++) {
	//    sb.append("   ");
	//}
	//a.add(sb.toString());
	try {
	    Token t;
	    while ((t = ts.nextToken()) != null) {
		
		if (t._sToken == null) {
		    switch (t._nSymbolID) {
			case Token.LPAREN: a.add(ParseFileInt(ts, level + 1)); break;
			case Token.RPAREN: return a;
			case Token.PERIOD: {
			    Token t_next = ts.nextToken();
			    if (Character.isLetter(t_next._sToken.charAt(0))) {
				// Keyword - so can load Spudd output files as well
				a.add(new Keyword(t_next._sToken));
			    } else {
				// Decimal number
				int max_index = a.size() - 1;
				Object o = a.get(max_index);
				String bds = null;
				if (o instanceof String) {
				    try {
					if (Character.isDigit(((String)o).charAt(0)) ||
					    (((String)o).charAt(0) == '-' && 
					     ((String)o).length() > 1 && 
					     Character.isDigit(((String)o).charAt(1)))) {

					    bds = ((String)o) + "." + t_next._sToken;
					} else {

					    bds = "." + t_next._sToken;
					}
					a.set(max_index, new BigDecimal(bds));
				    } catch (NumberFormatException nfe) {
					System.out.println("Parse error after period: " + t);
					System.out.println("Could not translate: " + bds);
					System.exit(1);
				    }
				} else {
				    System.out.println("WARNING: Number did not preceed '.' " + 
						       "followed by number: " + t);
				    //System.exit(1);
				}
			    }
			} break;
		    }
		} else if (t._bInteger) {
		    a.add(t._sToken); // Could make into a double
		} else {
		    a.add(t._sToken);
		}
	    }
	} catch (TokenStreamException tse) {
	    System.out.println("Error: " + tse);
	    return null;
	}

	if (level != 0) {
	    System.out.println("'" + ts._sFilename + 
			       "' contains unbalanced parentheses!");
	    System.exit(1);
	} 
	
	return a;
    }
}
