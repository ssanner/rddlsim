/**
 * Algebraic Decision Diagram Package
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 7/25/03
 *
 **/

package dd.discrete;

import graph.Graph;

public class ADDDNode extends ADDNode {
    
    public double _dLower;
    public double _dUpper;

    public String _sLowerLabel; // Optional labels, i.e. which max contributed?
    public String _sUpperLabel; 
    
    // For a single value
    public ADDDNode(int lid, double val) {
	_nLocalID  = lid;
	_dLower    = val;
	_dUpper    = val;
	_sLowerLabel = null;
	_sUpperLabel = null;
    }
    
    // For a range value
    public ADDDNode(int lid, double min, double max) {
	_nLocalID = lid;
	_dLower   = min;
	_dUpper   = max;
	_sLowerLabel = null;
	_sUpperLabel = null;
    }
    
    // For a range value
    public ADDDNode(int lid, double min, double max, 
		    String lower_label, String upper_label) {
	_nLocalID = lid;
	_dLower   = min;
	_dUpper   = max;
	_sLowerLabel = lower_label;
	_sUpperLabel = upper_label;
    }
    
    public String toString() {
	return "*" + DD._df.format(_dLower) + "*";
    }

    public String toString(Object context, int depth) {
	
	if (_dUpper == _dLower) {
	    String label = "";
	    if (_sUpperLabel != null) {
		label = ": <" + _sUpperLabel + "> ";
	    }
	    return "[ #" + _nLocalID + " <" + ADD._df.format(_dLower) + "> ] " + label;
	} else {
	    String label = "";
	    if (_sLowerLabel != null ||_sUpperLabel != null) {
		if (_sLowerLabel == null) {
		    label = ": <" + _sUpperLabel + "> ";
		} else if (_sUpperLabel == null) {
		    label = ": <" + _sLowerLabel + "> ";
		} else if (_sUpperLabel.equals(_sLowerLabel)) {
		    label = ": <" + _sUpperLabel + "> ";
		} else {
		    label = ": <" + _sLowerLabel + "," + _sUpperLabel + "> ";
		}
		label = ": " + _sUpperLabel;
	    }
	    return "[ #" + _nLocalID + " <" + ADD._df.format(_dLower) 
		                      + "," + ADD._df.format(_dUpper) + "> ] " + label;
	}
    }
    
    
    public void toGraph(Object context, Graph g) {
    	g.addNode("#" + _nLocalID);
		g.addNodeLabel("#" + _nLocalID, DD._df.format(_dLower));
		if (DD.USE_COLOR) {
			if (DD.USE_FESTIVE) 
				g.addNodeColor("#" + _nLocalID, "red"); // red, darkred, lightsalmon
			else
				g.addNodeColor("#" + _nLocalID, "lightsalmon"); // red, darkred, lightsalmon
		}
		g.addNodeShape("#" + _nLocalID, "box");
		g.addNodeStyle("#" + _nLocalID, "filled");

    }

}
