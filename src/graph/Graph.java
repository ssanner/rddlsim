/**
 * Basic Graph Package Routines
 *
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 11/29/04
 *
 * - Following are attribute options for DOT viewer: (digraph and graph)
 *   - Node shape: [box,ellipse,diamond,circle,record,plaintext,polygon (w/ sides=#)]
 *   - Node color: [standard string colors, see DOT_NOTES.txt for examples]
 *   - Node style: [filled,...]
 *   - Edge style: [dashed,dotted,...]
 *   - Edge color: [see node color]
 *
 **/

package graph;

import graph.gviz.*; // Graph viewer

import java.io.*;
import java.text.*;
import java.util.*;

import util.*;

public class Graph {

	public static final String EMPTY_STR = "".intern();
	public static final String VIEWER_FILE = "tmp_viewer_file.dot";

	/* Node and link data structures */
	public boolean _bDirected; // Just used for printing, does not restrict links
	public boolean _bLeftRight;
	public boolean _bSuppressRank;

	public boolean _bBottomToTop; 
	
	public ArrayList _alRankSame;
	
	public HashMap _hmNodes;
	public HashMap _hmLinks;
	public HashMap _hmRevLinks; // This will be redundant for an undirected
								// graph
	public HashMap _hmNodeColor;
	public HashMap _hmNodeShape;
	public HashMap _hmNodeStyle;
	public HashMap _hmNodeLabel;
	public MapList _mlEdgeColor;
	public MapList _mlEdgeStyle;
	public MapList _mlEdgeLabel;

	public double _dMaxBinaryWidth; // == Binary TW + 1; computed during
									// greedyTW

	// 2^val = max memory entries needed during VE
	public int _nTreeWidth; // Standard TW, assuming all vars binary

	public boolean _bUseColor;

	/* For printing */
	public static final boolean PRINT_WARNING = false;

	public static DecimalFormat _df = new DecimalFormat("#.###");

	// ///////////////////////////////////////////////////////////////////////////
	// Basic Graph Construction
	// ///////////////////////////////////////////////////////////////////////////

	public Graph() {
		this(true, true, false, false);
	}

	public Graph(boolean directed) {
		this(directed, true, false, false);
	}
	
	public Graph(boolean directed, boolean bottom_to_top, boolean left_right, boolean multi_edges) {
		_bDirected = directed; // Just used for printing, does not restrict links
		_bLeftRight = left_right;
		_bSuppressRank = false;
		_hmNodes = new HashMap(); // Maps to size
		_alRankSame  = new ArrayList();
		_hmNodeColor = new HashMap();
		_hmNodeShape = new HashMap();
		_hmNodeStyle = new HashMap();
		_hmNodeLabel = new HashMap();
		_hmLinks = new HashMap();
		_hmRevLinks = new HashMap();
		_mlEdgeColor = new MapList(!multi_edges); // By default, only allow one edge
		_mlEdgeStyle = new MapList(!multi_edges); // By default, only allow one edge
		_mlEdgeLabel = new MapList(!multi_edges); // By default, only allow one edge
		_bUseColor = true;
		_dMaxBinaryWidth = -1d;
		_nTreeWidth = -1;
		_bBottomToTop = bottom_to_top;
	}

	public void setSuppressRank(boolean suppress) {
		_bSuppressRank = suppress;
	}
	
	public void setUseColor(boolean uc) {
		_bUseColor = uc;
	}

	public void setMultiEdges(boolean multi_edges) {
		_mlEdgeColor._bUnique = false;
		_mlEdgeStyle._bUnique = false;
		_mlEdgeLabel._bUnique = false;
	}

	public void setBottomToTop(boolean bottom_to_top) {
		_bBottomToTop = bottom_to_top;
	}
	
	public void addSameRank(Collection nodes) {
		_alRankSame.add(nodes);
	}
	
	public void addNode(Object o) {
		if (!_hmNodes.containsKey(o)) {
			_hmNodes.put(o, new Integer(1));
		}
	}

	public void addNode(Object o, int log_sz) {
		_hmNodes.put(o, new Integer(log_sz));
	}

	public void addNode(Object o, int log_sz, String color, String shape,
			String style) {
		_hmNodes.put(o, new Integer(log_sz));
		_hmNodeColor.put(o, (color == null) ? EMPTY_STR : color.intern());
		_hmNodeShape.put(o, (shape == null) ? EMPTY_STR : shape.intern());
		_hmNodeStyle.put(o, (style == null) ? EMPTY_STR : style.intern());
	}

	public void addNodeLabel(Object o, String s) {
		_hmNodeLabel.put(o, s);
	}
	
	public void remap(Map id2var) {
		for (Object o : ((Map)_hmNodeLabel.clone()).keySet()) {
			String cur_label = (String)_hmNodeLabel.get(o);
			if (cur_label == null || !cur_label.startsWith("x"))
				continue;
			cur_label = cur_label.substring(1);
			String new_label = (String)id2var.get(new Integer(cur_label));
			if (new_label == null)
				System.out.println("Graph error: remap of '" + cur_label + "' is null");
			_hmNodeLabel.put(o, new_label);
		}
	}

	public void addNodeColor(Object o, String s) {
		_hmNodeColor.put(o, s);
	}

	public void addNodeShape(Object o, String s) {
		_hmNodeShape.put(o, s);
	}

	public void addNodeStyle(Object o, String s) {
		_hmNodeStyle.put(o, s);
	}

	public void addAllNodes(Collection c) {
		Iterator i = c.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (!_hmNodes.containsKey(o)) {
				_hmNodes.put(o, new Integer(1));
			}
		}
	}

	public void addAllNodes(Collection c, int log_sz) {
		Iterator i = c.iterator();
		while (i.hasNext()) {
			_hmNodes.put(i.next(), new Integer(log_sz));
		}
	}

	public void addUniLink(Object o1, Object o2) {
		addNode(o1);
		addNode(o2);
		getLinkSet(o1, true).add(o2);
		getRevLinkSet(o2, true).add(o1);
	}

	public void addUniLink(Object o1, Object o2, String color, String style,
			String label) {
		addNode(o1);
		addNode(o2);
		getLinkSet(o1, true).add(o2);
		getRevLinkSet(o2, true).add(o1);
		_mlEdgeColor.putValue(new Pair(o1, o2), (color == null) ? EMPTY_STR : color
				.intern());
		_mlEdgeStyle.putValue(new Pair(o1, o2), (style == null) ? EMPTY_STR : style
				.intern());
		_mlEdgeLabel.putValue(new Pair(o1, o2), (label == null) ? EMPTY_STR : label
				.intern());
		if (!_bDirected) {
			_mlEdgeColor.putValue(new Pair(o2, o1), (color == null) ? EMPTY_STR
					: color.intern());
			_mlEdgeStyle.putValue(new Pair(o2, o1), (style == null) ? EMPTY_STR
					: style.intern());
			_mlEdgeLabel.putValue(new Pair(o2, o1), (label == null) ? EMPTY_STR
					: label.intern());
		}
	}

	public void addUniLinksTo(Object o1, Collection c2) {
		addNode(o1);
		addAllNodes(c2);
		getLinkSet(o1, true).addAll(c2);
		Iterator i = c2.iterator();
		while (i.hasNext()) {
			getRevLinkSet(i.next(), true).add(o1);
		}
	}

	public void addUniLinksFrom(Collection c1, Object o2) {
		addAllNodes(c1);
		addNode(o2);
		Iterator i = c1.iterator();
		while (i.hasNext()) {
			getLinkSet(i.next(), true).add(o2);
		}
		getRevLinkSet(o2, true).addAll(c1);
	}

	public void addAllUniLinks(Collection c1, Collection c2) {
		addAllNodes(c1);
		addAllNodes(c2);
		Iterator i = c1.iterator();
		while (i.hasNext()) {
			getLinkSet(i.next(), true).addAll(c2);
		}
		i = c2.iterator();
		while (i.hasNext()) {
			getRevLinkSet(i.next(), true).addAll(c1);
		}
	}

	public void addBiLink(Object o1, Object o2) {
		addUniLink(o1, o2);
		addUniLink(o2, o1);
	}

	public void addBiLinks(Object o1, Collection c2) {
		addUniLinksTo(o1, c2);
		addUniLinksFrom(c2, o1);
	}

	public void addAllBiLinks(Collection c1, Collection c2) {
		addAllUniLinks(c1, c2);
		addAllUniLinks(c2, c1);
	}

	public void addAllPairLinks(Collection c) {
		Iterator i1 = c.iterator();
		while (i1.hasNext()) {
			Object o1 = i1.next();
			Iterator i2 = c.iterator();
			while (i2.hasNext()) {
				Object o2 = i2.next();
				if (o1 != o2) {
					addUniLink(o1, o2); // Will add all pairs
				}
			}
		}
	}

	public boolean isNode(Object o) {
		return _hmNodes.containsKey(o);
	}

	public boolean isUniLink(Object o1, Object o2) {
		Set s = getLinkSet(o1, false);
		if (s == null) {
			return false;
		} else {
			return s.contains(o2);
		}
	}

	public boolean isBiLink(Object o1, Object o2) {
		return (isUniLink(o1, o2) && isUniLink(o2, o1));
	}

	public String getNodeLabel(Object o1) {
		return (String) _hmNodeLabel.get(o1);
	}

	public String getNodeColor(Object o1) {
		return (String) _hmNodeColor.get(o1);
	}

	public String getNodeShape(Object o1) {
		return (String) _hmNodeShape.get(o1);
	}

	public String getNodeStyle(Object o1) {
		return (String) _hmNodeStyle.get(o1);
	}

	public ArrayList getEdgeColor(Object o1, Object o2) {
		return _mlEdgeColor.getValues(new Pair(o1, o2));
	}

	public ArrayList getEdgeStyle(Object o1, Object o2) {
		return _mlEdgeStyle.getValues(new Pair(o1, o2));
	}

	public ArrayList getEdgeLabel(Object o1, Object o2) {
		return _mlEdgeLabel.getValues(new Pair(o1, o2));
	}

	public Set getLinkSet(Object o1) {
		return getLinkSet(o1, false);
	}

	public Set getLinkSet(Object o1, boolean create) {
		Set s = (Set) _hmLinks.get(o1);
		if (s == null) {
			if (create) {
				s = new HashSet();
				_hmLinks.put(o1, s);
			} else {
				return null;
			}
		}
		return s;
	}

	public Set getRevLinkSet(Object o1) {
		return getRevLinkSet(o1, false);
	}

	public Set getRevLinkSet(Object o1, boolean create) {
		Set s = (Set) _hmRevLinks.get(o1);
		if (s == null) {
			if (create) {
				s = new HashSet();
				_hmRevLinks.put(o1, s);
			} else {
				return null;
			}
		}
		return s;
	}

	public void removeUniLink(Object o1, Object o2) {
		Set s = getLinkSet(o1, false);
		if (s != null) {
			s.remove(o2);
		}
		s = getRevLinkSet(o2, false);
		if (s != null) {
			s.remove(o1);
		}
	}

	// Should also make functions for removing multiple uni/bi links

	public void removeBiLink(Object o1, Object o2) {
		removeUniLink(o1, o2);
		removeUniLink(o2, o1);
	}

	public boolean equals(Object o) {
		if (o instanceof Graph) {
			Graph g = (Graph) o;
			return _hmNodes.equals(g._hmNodes) && _hmLinks.equals(g._hmLinks);
		} else {
			return false;
		}
	}

	public Object clone() {
		Graph g = new Graph();
		g._hmNodes.putAll(_hmNodes);
		g._hmLinks.putAll(_hmLinks);
		g._hmRevLinks.putAll(_hmRevLinks);
		return g;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Nodes:         " + _hmNodes + "\n");
		sb.append("Forward links: " + _hmLinks + "\n");
		sb.append("Reverse links: " + _hmRevLinks + "\n");
		return sb.toString();
	}

	// ///////////////////////////////////////////////////////////////////////////
	// Graph Viz Routines
	// ///////////////////////////////////////////////////////////////////////////

	protected List removeDuplicateStrings(List src) {
		return new ArrayList(new HashSet(src));
	}
	
	public boolean genFormatDotFile(String filename) {
		try {
			PrintStream ps = new PrintStream(new FileOutputStream(filename));
			ps.print(format());
			ps.close();
		} catch (Exception e) {
			System.err.println("Graph.formatDotFile: " + e);
			return false;
		}
		return true;
	}
	
	public StringBuilder format() {
	     try {

	           Process p = null;
	           if (_bDirected)
	        	   p = Runtime.getRuntime().exec(WinUNIX.GVIZ_EXE);
	           else
	        	   p = Runtime.getRuntime().exec(WinUNIX.GVIZ2_EXE);
	           BufferedReader process_out = new BufferedReader(new
	        		  InputStreamReader(p.getInputStream()));
	           PrintStream process_in  = new PrintStream(p.getOutputStream(), true);

	           // Provide input to process (could come from any stream)
	           genDotFile(process_in);
	           process_in.close(); // Need to close input stream so process exits!!!

	           // Get output from process (can also be used by BufferedReader to get
	           // line-by-line... see how fis_reader is constructed).
	           StringBuilder sb = new StringBuilder();
	           String line = null;
	           while ((line = process_out.readLine()) != null) {
	               sb.append(line + "\n");
	           }
	           process_out.close();
	           return sb;

	       } catch (IOException ioe) {
	           System.out.println("ERROR in Graph.format: Check executable.\n" + ioe);
	           return null;
	       }
	}
	
	public void genDotFile(String filename) {
		try {
			PrintStream os = new PrintStream(new FileOutputStream(filename));
			genDotFile(os);
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	public void genDotFile(PrintStream os) {

		os.println((_bDirected ? "digraph G { " : "graph G {\n  overlap = false;\n"));
		os.println("graph [ fontname = \"Helvetica\",fontsize=\"16\",ratio = \"auto\","
						+ "\n        size=\"7.5,10\""  
						+ (_bLeftRight ? ",rankdir=\"LR\"" : "")  
						+ ",ranksep=\"2.00\" ];"); // ,orientation=\"landscape\"
																			// ]");
		os.println("node [fontsize=\"16\"];");
		if (_bDirected) {
			if (_bBottomToTop) os.println("edge [dir=back,fontsize=\"16\"];");
		} else {
			os.println("edge [fontsize=\"16\"];");
		}

		// First print out all nodes
		Iterator i = _hmNodes.keySet().iterator();
		while (i.hasNext()) {
			Object v = i.next();
			os.println("\"" + v + "\" " + genDotNodeSpec(v) + ";");

		}

		// Now print all edges
		i = _hmNodes.keySet().iterator();
		while (i.hasNext()) {
			Object v = i.next();
			Set s = getLinkSet(v, false);
			if (s == null) {
				continue;
			}
			Iterator i2 = s.iterator();
			while (i2.hasNext()) {
				Object v2 = i2.next();

				// These link directions are reversed... OK for digraph
				// because edges reversed during
				// rendering, OK for undirected graph because edge is
				// undirected.
				if (_bDirected || ((Comparable) v).compareTo(v2) < 0 /* Make sure only
																	    one link added */) {
					List edge_spec = removeDuplicateStrings( genDotEdgeSpec(v, v2) );
					if (edge_spec.size() == 0)
						edge_spec.add(EMPTY_STR);
					for (Iterator it = edge_spec.iterator(); it.hasNext();)
						os.println("\"" + (_bBottomToTop ? v2 : v) + "\""
								+ (_bDirected ? " -> " : " -- ") + "\"" 
								+ (_bBottomToTop ? v : v2)
								+ "\" " + it.next().toString() + ";");
				}
			}
		}
		
		// Now print all rank same
		if (!_bSuppressRank)
			for (Object o : _alRankSame) {
				Collection c = (Collection)o;
				os.print("{ rank=same; ");
				for (Object node : c)
					os.print("\"" + node + "\"; ");
				os.println("};");
			}
		
		os.println("}");
	}

	public String genDotNodeSpec(Object o1) {
		StringBuffer sb = new StringBuffer();
		String prev = "";
		String color, shape, style, label;
		sb.append('[');
		label = getNodeLabel(o1);
		shape = getNodeShape(o1);
		style = getNodeStyle(o1);
		
		if (label != null) {
			// label = label.replaceAll("\n","\\n");
			label = ReplaceNewline(label);
			sb.append(prev + "label=\"" + label + "\"");
			prev = ",";
		}
		color = getNodeColor(o1);
		if (color != null || shape != null || style != null) {
			if (color != null && color != EMPTY_STR && _bUseColor) {
				sb.append(prev + "fillcolor=" + color);
				sb.append(",color=black");
				prev = ",";
			}
			if (shape != null && shape != EMPTY_STR) {
				sb.append(prev + "shape=" + shape);
				prev = ",";
			}
			if (style != null && style != EMPTY_STR && color != null && _bUseColor) {
				sb.append(prev + "style=" + style);
			}
		}
		sb.append(']');

		return sb.toString();
	}

	public static String ReplaceNewline(String s) {
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n') {
				ret.append("\\n");
			} else {
				ret.append(c);
			}
		}
		return ret.toString();
	}

	public ArrayList genDotEdgeSpec(Object o1, Object o2) {
		ArrayList color_a, style_a, label_a, ret = new ArrayList();
		String color, style, label;
		color_a = getEdgeColor(o1, o2);
		if (color_a.size() == 0) {
			return ret;
		}
		style_a = getEdgeStyle(o1, o2);
		label_a = getEdgeLabel(o1, o2);
		for (int i = 0; i < color_a.size(); i++) {
			StringBuffer sb = new StringBuffer("[");
			String prev = "";
			color = (String)color_a.get(i);
			style = (String)style_a.get(i);
			label = (String)label_a.get(i);
			if (color != EMPTY_STR && _bUseColor) {
				sb.append(prev + "color=" + color);
				prev = ",";
			}
			if (style != EMPTY_STR) {
				sb.append(prev + "style=" + style);
				prev = ",";
			}
			if (label != EMPTY_STR) {
				sb.append(prev + "label=\"" + label + '\"');
			}
			sb.append(']');
			ret.add(sb.toString());
		} 
		return ret;
	}
		
	public void launchViewer() {
		launchViewer(800, 600, 100, 100, 20);
	}
	
	public void launchViewer(int width, int height) {
		try {
			launchViewer(width, height, 0, 0, 20);
		} catch (Throwable t) {
			System.err.println(t);
			t.printStackTrace(System.err);
		}
	}
		
	public void launchViewer(int w, int h, int x_off, int y_off, int text_h) {
		genFormatDotFile(VIEWER_FILE);
		DotViewer dv = new DotViewer() {
			public void nodeClicked(String name) {
			}
		};
		dv.setWindowSizing(w, h, x_off, y_off, text_h);
		dv.showWindow(VIEWER_FILE);
	}

	// ///////////////////////////////////////////////////////////////////////////
	// Graph Analysis Algorithms
	//
	// Contains: Topological sort (DAG)
	// Greedy TW order
	// TW computation from order
	// ///////////////////////////////////////////////////////////////////////////

	/** Show different tree widths * */
	public List computeBestOrder() {
		System.out.println("Searching for a good order...");
		String[] name = new String[6];
		name[0] = "minf_deepest       ";
		name[1] = "minf_shallow       ";
		name[2] = "topological_normal ";
		name[3] = "topological_reverse";
		name[4] = "greedy_deepest     ";
		name[5] = "greedy_shallow     ";
		List[] order = new List[6];
		order[0] = minfillSort(true);
		order[1] = minfillSort(false);
		order[2] = topologicalSort(false);
		order[3] = topologicalSort(true);
		order[4] = greedyTWSort(true);
		order[5] = greedyTWSort(false);
		int best_tw = Integer.MAX_VALUE;
		int best_order = -1;
		for (int i = 0; i < order.length; i++) {
			int tw = computeTreeWidth(order[i]);
			System.out.print(((tw < best_tw) ? "* " : "  ") + "TW of "
					+ name[i] + ": " + tw);
			System.out.println("  " + order[i]);
			if (tw < best_tw) {
				best_tw = tw;
				best_order = i;
			}
		}
		System.out.println(name[best_order] + "/TW:" + best_tw + "...done");

		return order[best_order];
	}

	/** Computes the induced tree-width for a specified elimination order * */
	public int computeTreeWidth(List order) {

		int tw = 0;

		// Initialize a set of sets containing vars and their parents
		Iterator i = order.iterator();
		Set factors = new HashSet();
		while (i.hasNext()) {
			Object var = i.next();
			Set varset = new HashSet();
			varset.add(var);
			Set s = getLinkSet(var);
			if (s != null) {
				varset.addAll(s);
			}
			factors.add(varset);
		}
		// System.out.println(factors);

		// Now perform variable elimination, keeping track of size of merged
		// factor at each step
		i = order.iterator();
		while (i.hasNext()) {
			Object var = i.next();

			// Separate the sets
			Set factors_with_var = new HashSet();
			Set factors_without_var = new HashSet();
			Iterator j = factors.iterator();
			while (j.hasNext()) {
				Set factor_j = (Set) j.next();
				if (factor_j.contains(var)) {
					factors_with_var.add(factor_j);
				} else {
					factors_without_var.add(factor_j);
				}
			}

			// Compute the new joint factor
			Set new_factor = new HashSet();
			j = factors_with_var.iterator();
			while (j.hasNext()) {
				Set factor_k = (Set) j.next();
				new_factor.addAll(factor_k);
			}
			new_factor.remove(var); // We've eliminated this var!
			if (new_factor.size() > tw) { // TW is after var eliminated
				tw = new_factor.size();
			}

			// Now update the factors
			factors.clear();
			factors.addAll(factors_without_var);
			factors.add(new_factor);
			// System.out.println("Elim " + var + " -> " +
			// new_factor + " + " + factors_without_var);
		}

		return tw;
	}

	/**
	 * Greedy TW sort - simulate VE and make greedy choice at each step.
	 * 
	 * Factor generated for each child and set of parents so good for TW in
	 * directed graphs (e.g. Bayesian networks).
	 */
	public List greedyTWSort(boolean deepest_first) {
		ArrayList free = new ArrayList();

		// Find var depths and populate free set
		Object[] depths = computeAllVarDepths(deepest_first);
		for (int i = 0; i < depths.length; i++) {
			Map.Entry me = (Map.Entry) depths[i];
			free.add(me.getKey());
			// System.out.println(me);
		}

		// Initialize a set of sets containing vars and their parents
		Iterator it = free.iterator();
		Set factors = new HashSet();
		while (it.hasNext()) {
			Object var = it.next();
			Set varset = new HashSet();
			varset.add(var);
			Set s = getLinkSet(var);
			if (s != null) {
				varset.addAll(s);
			}
			factors.add(varset);
			// System.out.println("Adding varset " + varset + " for " + var);
		}

		return greedyTWSort(free, factors);
	}

	/**
	 * Greedy TW sort - given free variables and factors directly (does not use
	 * graph links, above method does this preprocessing)
	 * 
	 * For TP, have factor for each clause so build this apriori (hard to build
	 * from a graph).
	 * 
	 * **Note: Can call this directly (without setting up graph links). Main
	 * reason to do this would be for factor graphs which cannot be easily
	 * represented by binary edge graphs (i.e. a hyperedge for every factor).
	 * Can just build factor Set directly for each hyperedge (i.e. set of
	 * elements in a clause) and add it to factors. Default setting for using
	 * greedyTW with graph is to add a factor for every parent and child (but
	 * this would not correspond to elimination of relations, propositions in
	 * ordered resolution).
	 */
	public List greedyTWSort(List free, Set factors) {

		// System.out.println("Factors: " + factors);

		double cur_tw = 0d;
		ArrayList order = new ArrayList();

		// System.out.println(factors);

		// Now perform variable elimination, choosing the var which minimizes
		// tree-width
		while (!free.isEmpty()) {

			Iterator it = free.iterator();
			double min_tw = Double.POSITIVE_INFINITY;
			Object min_var = null;
			while (it.hasNext()) {
				Object var = it.next();
				double tw = getTWofElimVar(factors, var);
				if (tw < min_tw) {
					min_tw = tw;
					min_var = var;
					if (min_tw <= cur_tw) {
						break;
					}
				}
			}
			// System.out.print(".");
			// System.out.println("min: " + min_tw + " - " + min_var);

			// Separate the sets
			Set factors_with_var = new HashSet();
			Set factors_without_var = new HashSet();
			// System.out.println("Start Factors: " + factors);
			Iterator j = factors.iterator();
			while (j.hasNext()) {
				Set factor_j = (Set) j.next();
				if (factor_j.contains(min_var)) {
					factors_with_var.add(factor_j);
				} else {
					factors_without_var.add(factor_j);
				}
			}

			// Compute the new joint factor
			Set new_factor = new HashSet();
			j = factors_with_var.iterator();
			while (j.hasNext()) {
				Set factor_k = (Set) j.next();
				new_factor.addAll(factor_k);
			}
			// System.out.println("Elim: " + min_var + "->" + new_factor);
			new_factor.remove(min_var); // We've eliminated this var!
			cur_tw = (cur_tw > min_tw) ? cur_tw : min_tw;
			free.remove(min_var);
			order.add(min_var);

			// Now update the factors
			factors.clear();
			factors.addAll(factors_without_var);
			factors.add(new_factor);
			// System.out.println("End Factors: " + factors + "\n");
			// System.out.println("Elim " + var + " -> " +
			// new_factor + " + " + factors_without_var);
			// System.exit(1);
		}

		_dMaxBinaryWidth = cur_tw;
		_nTreeWidth = computeTreeWidth(order);
		// System.out.print("(" + _df.format(cur_tw) + ")");

		return order;
	}

	// Computes tree-width based on log_2 num entries
	static Set affected_vars = new HashSet();

	public double getTWofElimVar(Set factors, Object var) {

		// Separate the sets
		affected_vars.clear();
		Iterator j = factors.iterator();
		while (j.hasNext()) {
			Set factor_j = (Set) j.next();
			if (factor_j.contains(var)) {
				affected_vars.addAll(factor_j);
			}
		}
		// System.out.println(factors + ":" + affected_vars + "\n");
		double num_tuples = 1d;
		// System.out.println("Affected vars: " + affected_vars);
		j = affected_vars.iterator();
		Object t = null;
		while (j.hasNext()) {
			t = j.next();
			Integer sz = (Integer) (_hmNodes.get(t));
			if (sz == null) {
				if (PRINT_WARNING) {
					System.out
							.println("--------------------------------------------------------------");
					System.out.println("WARNING: Cannot retrieve " + t
							+ " from " + _hmNodes);
					System.out
							.println("It is likely that no edge was added for "
									+ t
									+ ", i.e. unit \nclause that does not interact "
									+ "with other predicates");
					System.out.println("Defaulting to 1d");
					System.out
							.println("--------------------------------------------------------------");
				}
				sz = new Integer(1);
				_hmNodes.put(t, sz);
			}
			num_tuples *= Math.pow(2d, sz.doubleValue());
		}
		return Math.log(num_tuples) / Math.log(2d);
	}

	/**
	 * Computes and sorts nodes by max number of ancestors (i.e. parents). Does
	 * not compute correct value for non-DAG!
	 */
	protected Object[] computeAllVarDepths(boolean deepest_first) {

		HashMap m = new HashMap();
		Iterator i = _hmNodes.keySet().iterator();
		while (i.hasNext()) {
			Object v = i.next();
			computeDepth(v, m);
		}

		Object[] arr = m.entrySet().toArray();
		Arrays.sort(arr, new ValueComparator(deepest_first));
		return arr;
	}

	protected int computeDepth(Object v, HashMap cache) {

		// To prevent infinite recursion - should not affect DAG
		cache.put(v, new Integer(0));

		Integer val = null;
		if ((val = (Integer) cache.get(v)) != null) {
			return val.intValue();
		}

		Set s = getLinkSet(v);
		if (s == null) {
			return 0; // No parents!
		}
		Iterator i = s.iterator();
		int max = 0;
		while (i.hasNext()) {
			Object p = i.next();
			int depth = computeDepth(p, cache) + 1;
			if (depth > max) {
				max = depth;
			}
		}
		cache.put(v, new Integer(max));
		return max;
	}

	public static class ValueComparator implements Comparator {

		boolean _bDeepestFirst;

		public ValueComparator(boolean deepest_first) {
			_bDeepestFirst = deepest_first;
		}

		public int compare(Object o1, Object o2) {
			Map.Entry me1 = (Map.Entry) o1;
			Map.Entry me2 = (Map.Entry) o2;
			if (_bDeepestFirst) {
				return -((Comparable) me1.getValue()).compareTo(me2.getValue());
			} else {
				return ((Comparable) me1.getValue()).compareTo(me2.getValue());
			}
		}
	}

	/**
	 * This min-fill heuristic is mentioned by Darwiche and Hopkins and was
	 * cited as the best heuristic they could find.
	 * 
	 * Heuristic may be wrong as suggested by results.
	 */
	public List minfillSort(boolean deepest_first) {
		ArrayList order = new ArrayList();
		HashSet parents = new HashSet();
		HashSet free = new HashSet();

		// Find var depths and populate free set
		Object[] depths = computeAllVarDepths(deepest_first);
		int i;
		for (i = 0; i < depths.length; i++) {
			Map.Entry me = (Map.Entry) depths[i];
			free.add(me.getKey());
			// System.out.println(me);
		}

		// Iterate until no more variables to put in order
		while (!free.isEmpty()) {
			int minf = Integer.MAX_VALUE;
			String minf_var = null;

			// Compute best minfill for vars not in parent set
			for (i = 0; i < depths.length; i++) {
				String var = (String) ((Map.Entry) depths[i]).getKey();
				if (parents.contains(var)) {
					continue;
				}

				// Not in parent set, so compute min fill
				Set sparents = getLinkSet(var, false);
				if (sparents == null) {
					free.remove(var);
					continue;
				}
				ArrayList vparents = new ArrayList(sparents);

				// Compute the fill for v by incrementing for any
				// parent pair that does not contain a link
				int fill = 0;
				for (int j = 0; j < vparents.size(); j++) {
					String vj = (String) vparents.get(j);
					if (!parents.contains(vj)) {
						continue;
					}
					for (int k = j + 1; k < vparents.size(); k++) {
						String vk = (String) vparents.get(k);
						if (!parents.contains(vk)) {
							continue;
						}
						// System.out.println("Checking " + vj + "<->" + vk);
						if (!isUniLink(vj, vk) && !isUniLink(vk, vj)) {
							fill++;
						}
					}
				}

				// Lowest fill so far?
				// System.out.print(fill + " ");
				if (fill < minf) {
					minf = fill;
					minf_var = var;
				}
			}

			// Add the min_fill var for this round
			// System.out.println(" -> Chose " + minf_var + "::" + minf);
			order.add(minf_var);
			free.remove(minf_var);
			parents.add(minf_var);
		}

		// System.out.println(order);
		// System.exit(1);
		return order;
	}

	/**
	 * Perform a topological sort of the variables. Ensures that a child comes
	 * before all parents.
	 * 
	 * Note: The topological sort (child first) is not guaranteed to be a good
	 * ordering but is probably better than random.
	 */
	public List topologicalSort(boolean reverse) {
		LinkedList order = new LinkedList();
		HashSet visited = new HashSet();

		// Perform dfs topological sort
		Object[] depths = computeAllVarDepths(true);
		for (int i = 0; i < depths.length; i++) {
			dfs((String) ((Map.Entry) depths[i]).getKey(), visited, order);
		}

		if (!reverse) {
			return order;
		}

		ArrayList rev_order = new ArrayList();
		int sz = order.size();
		for (int i = sz - 1; i >= 0; i--) {
			rev_order.add(order.get(i));
		}
		return rev_order;
	}

	/**
	 * Perform a dfs from a given var, placing var on list after all successors
	 * have been placed on list.
	 * 
	 * Enforces that a child comes before a parent in dfs manner.
	 */
	public void dfs(String var, Set visited, LinkedList order) {

		if (visited.contains(var)) {
			return;
		}

		visited.add(var);
		Set vparents = getLinkSet(var, false);
		if (vparents != null) {
			Iterator i = vparents.iterator();
			while (i.hasNext()) {
				dfs((String) i.next(), visited, order);
			}
		}

		order.addFirst(var);
	}

	public HashSet<Object> getSelfCycles() {
		HashSet<Object> self_cycles = new HashSet<Object>();
		Set vertices = _hmNodes.keySet();
		for (Object v : vertices) {
			Set edges = getLinkSet(v, false);
			if (edges != null && edges.contains(v))
				self_cycles.add(v);
		}
		return self_cycles;
	}
	
	// Note: not thread safe unless algorithm is synchronized
    int _scc_index = 0;
    Stack<Object> _scc_stack = new Stack<Object>();
    HashSet<HashSet<Object>> _scc_set        = new HashSet<HashSet<Object>>();
    HashMap<Object, Integer> _scc_node_index = new HashMap<Object, Integer>();
    HashMap<Object, Integer> _scc_low_link   = new HashMap<Object, Integer>();

	// Retrieve sets of all strongly connected components in a graph
	public HashSet<HashSet<Object>> getStronglyConnectedComponents() {

		if (!_bDirected) {
			System.err.println("WARNING: did you mean to evaluate strongly connected components on an undirected graph?");
			return null;
		}

		_scc_index = 0;
		_scc_stack.clear();
		_scc_set.clear();
		_scc_node_index.clear();
		_scc_low_link.clear();
		
		Set vertices = _hmNodes.keySet();
		for (Object v : vertices)
			if (!countExists(_scc_node_index, v))
				strongConnect(v);

		return new HashSet<HashSet<Object>>(_scc_set); // _scc_set may be cleared on a subsequent call
	}

	private void strongConnect(Object v) {

		setCount(_scc_node_index, v, _scc_index);
		setCount(_scc_low_link, v, _scc_index);
		++_scc_index;
		_scc_stack.push(v);

		Set edges = getLinkSet(v, false);
		if (edges != null)
			for (Object v2 : edges) {

				if (!countExists(_scc_node_index, v2)) {
					strongConnect(v2);
					setCount(_scc_low_link, v,
							Math.min(getCount(_scc_low_link, v),
									getCount(_scc_low_link, v2)));
				} else if (_scc_stack.contains(v2)) {
					setCount(_scc_low_link, v,
							Math.min(getCount(_scc_low_link, v),
									getCount(_scc_node_index, v2)));
				}
			}

		if (getCount(_scc_low_link, v) == getCount(_scc_node_index, v)) {
			HashSet<Object> subset = new HashSet<Object>();
			Object v2 = null;

			while (!v.equals(v2)) {
				v2 = _scc_stack.pop();
				subset.add(v2);
			}

			_scc_set.add(subset);
		}

	}
	
	// Detects any cycle including self-cycles
	// O(E+V) algorithm for cycle detection
	public boolean hasCycle() {
		
		Set vertices = _hmNodes.keySet();
		if (vertices.size() == 0)
			return false; // No cycles in an empty graph
		if (!_bDirected) {
			System.err.println("WARNING: did you mean to do cycle detection on an undirected graph?");
			return _hmLinks.size() > 0; // An undirected graph with any link has a cycle
		}

		Queue<Object> q; // The original code was written before Java generics existed!
		long counter = 0;
		HashMap<Object, Integer> in_degree = new HashMap<Object, Integer>();

		// Calculate the in-degree of all vertices and store 
		for (Object v : vertices) {
			Set edges = getLinkSet(v, false);
			if (edges == null)
				continue;
			for (Object dest : edges)
				incCount(in_degree, dest, 1);
		}
		// Find all vertices with in-degree == 0 (fringe nodes) and put in queue
		q = new LinkedList<Object>();
		for (Object v : vertices) {
			if (getCount(in_degree, v) == 0)
				q.add(v);
		}
		if (q.size() == 0)
			return true; // Cycle found not all vertices have in-bound edges

		// Remove all edges from fringe_nodes and add the destinations of
		// those edges if they themselves become fringe nodes.  If every
		// node does not become a fringe node, it is because there is a
		// cycle that prevents it from being added to the queue.
		for (counter = 0; !q.isEmpty(); counter++) {
			Object fringe_node = q.remove();
			Set edges = getLinkSet(fringe_node, false);
			if (edges == null)
				continue;
			for (Object dest : edges) {
				if (incCount(in_degree, dest, -1) == 0) {
					q.add(dest);
				}
			}
		}
		if (counter != vertices.size()) {
			return true; // Cycle found
		}
		return false;
	}

	public void setCount(HashMap<Object, Integer> obj2int, Object v, int val) {
		obj2int.put(v, val);
	}

	public int incCount(HashMap<Object, Integer> obj2int, Object v, int amount) {
		Integer val = obj2int.get(v);
		val = (val == null) ? amount : val + amount;
		obj2int.put(v, val);
		return val;
	}
	
	public int getCount(HashMap<Object,Integer> obj2int, Object v) {
		Integer val = obj2int.get(v);
		return (val == null) ? 0 : val;
	}
	
	public boolean countExists(HashMap<Object,Integer> obj2int, Object v) {
		return (obj2int.get(v) != null);
	}
	   
	// ///////////////////////////////////////////////////////////////////////////
	// Test Code
	// ///////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) {
		Graph g = new Graph(/*directed*/true, false, true, false);
		g.setBottomToTop(false);
		g.setMultiEdges(false); // Note: still does not allow cyclic edges

		g.addUniLink("a", "b");
		g.addUniLink("a", "d");
		g.addUniLink("b", "c");
		//g.addUniLink("a", "c");
		g.addUniLink("b", "e");
		g.addUniLink("a", "f");
		g.addUniLink("c", "a");
		g.addSameRank(Arrays.asList(new String[] {"f", "e", "c"}));
		g.setSuppressRank(false);
		g.genDotFile("graph_test.dot");
		g.launchViewer();

		System.out.println(g.format());
		
		System.out.println("Topological sort of nodes: " + g.topologicalSort(false));
		System.out.println("Has cycle: " + g.hasCycle());
		System.out.println("Strongly connected components: " + g.getStronglyConnectedComponents());
	}
		
	public static void main2(String[] args) {
		Graph g = new Graph(false);
		g.setBottomToTop(false);
		g.setMultiEdges(true); // Note: still does not allow cyclic edges
		ArrayList l1 = new ArrayList();
		l1.add("A");
		l1.add("B");
		l1.add("C");
		ArrayList l2 = new ArrayList();
		l2.add("D");
		l2.add("E");
		l2.add("F");
		g.addUniLinksTo("G", l1);
		g.addUniLinksFrom(l2, "H");
		g.addAllUniLinks(l1, l2);
		g.addUniLink("H", "I", "black", "solid", "<1.2,3.4>");
		g.addUniLink("H", "I", "black", "dotted", "<5.6,7.8>");
		g.addUniLink("H", "J", "red", "dashed", "text");
		g.addNodeLabel("H", "H's label");
		g.addNodeColor("H", "lightblue");
		g.addNodeShape("H", "box");
		g.addNodeStyle("H", "filled");

		
		// g.addUniLink("I", "G");
		
		// To generate a .ps file: "dot.exe -Tps test.dot > test.ps"
		g.genDotFile("test.dot");
		
		// To interactively view the graph in a Java window
		g.launchViewer();
		
		System.out.println(g);
		List order = g.computeBestOrder(); // g.greedyTWSort(true);
		System.out.println("\nOrder:        " + order);
		g.computeTreeWidth(order);
		System.out.println("MAX Bin Size: " + g._df.format(g._dMaxBinaryWidth));
		System.out.println("TW:           " + g._nTreeWidth + "\n");
		System.out.println("Has cycle: " + g.hasCycle());
		System.out.println("Strongly connected components: " + g.getStronglyConnectedComponents());
	}

}
