/**
 * RDDL: Simple Visualization for the Cyclic Inventory Management problem.
 * 
 * @author Timothy A. Mann (mann.timothy@acm.com)
 * @version 3/23/14
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

/**
 * Provides visualization for the Cyclic Inventory Management task.
 * 
 * <p>
 * The research leading to these results has received funding from the European
 * Research Counsel under the European Union's Seventh Framework Program
 * (FP7/2007-2013) / ERC Grant Agreement No 306638.
 * </p>
 * 
 * @author Timothy A. Mann
 * 
 */
public class CIMDisplay extends StateViz {

	private static final Color QUANT_COLOR = new Color(0.0f, 0.3f, 1.0f, 1.0f);
	private static final Color UNMET_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.4f);
	private static final Color DEMAND_COLOR = new Color(0.0f, 1.0f, 0.0f, 0.4f);

	private static final Color BACKDROP_COLOR = new Color(0.0f, 0.0f, 0.0f,
			0.3f);
	private static final int BACKDROP_PAD = 3;
	private static final int BACKDROP_ARC = 3;

	private static final int INSET_SIZE = 5;
	private static final int PREF_PIXEL_WIDTH = 640;
	private static final int PREF_PIXEL_HEIGHT = 480;

	private int _nTimeDelay;
	private JFrame _frame;
	private InventoryPanel _invPanel;
	private JLabel _roundLabel;
	private InventoryTotalsPanel _totalsPanel;

	public CIMDisplay() {
		_nTimeDelay = 500; // in milliseconds
	}

	@Override
	public void display(State s, int time) {
		try {
			System.out
					.println("TIME = " + time + ": " + getStateDescription(s));
		} catch (EvalException e) {
			System.out.println("\n\nError during visualization:");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates a visualization of the specified state. If the state
	 * <code>s == null</code>, then nothing is drawn.
	 * 
	 * @param s
	 *            a state
	 * @return an empty string (not null)
	 * @throws EvalException
	 */
	private String getStateDescription(State s) throws EvalException {
		StringBuilder sb = new StringBuilder();

		// If there is no frame yet, then we create one and pack it with the
		// different viz components.
		if (_frame == null) {
			JFrame.setDefaultLookAndFeelDecorated(true);

			_frame = new JFrame();
			_frame.setTitle("Cyclic Inventory Management");
			_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			_frame.setLayout(new GridLayout(1, 1));
			_frame.setPreferredSize(new Dimension(PREF_PIXEL_WIDTH,
					PREF_PIXEL_HEIGHT));

			JPanel panel = new JPanel();
			GridBagLayout gblayout = new GridBagLayout();
			panel.setLayout(gblayout);
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1;
			c.weighty = 1;
			c.insets = new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE,
					INSET_SIZE);
			c.fill = GridBagConstraints.BOTH;

			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1.0;
			c.weighty = 0.1;
			c.gridwidth = 1;
			c.gridheight = 1;
			JLabel invLabel = new JLabel("Per Component Inventory",
					JLabel.CENTER);
			gblayout.setConstraints(invLabel, c);
			panel.add(invLabel);

			_invPanel = new InventoryPanel();
			c.gridx = 0;
			c.gridy = 1;
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.gridwidth = 1;
			c.gridheight = 4;
			gblayout.setConstraints(_invPanel, c);
			panel.add(_invPanel);

			_roundLabel = new JLabel();
			c.gridx = 1;
			c.gridy = 0;
			c.weightx = 0.2;
			c.weighty = 0.1;
			c.gridwidth = 1;
			c.gridheight = 1;
			gblayout.setConstraints(_roundLabel, c);
			panel.add(_roundLabel);

			KeyPanel keyPanel = new KeyPanel();
			c.gridx = 1;
			c.gridy = 1;
			c.weightx = 0.2;
			c.weighty = 0.1;
			c.gridwidth = 1;
			c.gridheight = 1;
			gblayout.setConstraints(keyPanel, c);
			panel.add(keyPanel);

			JLabel totalsLabel = new JLabel("Inventory Totals", JLabel.CENTER);
			c.gridx = 1;
			c.gridy = 2;
			c.weightx = 0.2;
			c.weighty = 0.1;
			c.gridwidth = 1;
			c.gridheight = 1;
			gblayout.setConstraints(totalsLabel, c);
			panel.add(totalsLabel);

			_totalsPanel = new InventoryTotalsPanel();
			c.gridx = 1;
			c.gridy = 3;
			c.weightx = 0.2;
			c.weighty = 1.0;
			c.gridwidth = 1;
			c.gridheight = 2;
			gblayout.setConstraints(_totalsPanel, c);
			panel.add(_totalsPanel);

			// Set up the frame and make it visible
			_frame.add(panel);
			_frame.pack();
			_frame.setVisible(true);
		}

		///////////////////////////////////////////////////////////
		// Update the various viz components with state information
		///////////////////////////////////////////////////////////
		if (s != null) {
			PVAR_NAME ROUND = new PVAR_NAME("round");
			ArrayList<LCONST> params0 = new ArrayList<LCONST>(0);
			int round = (Integer) s.getPVariableAssign(ROUND, params0);
			_roundLabel.setText("Round: " + round);
		}
		_invPanel.updateState(s);
		_totalsPanel.updateState(s);
		_frame.repaint();

		// Sleep so the animation can be viewed at a frame rate of
		// 1000/_nTimeDelay per second
		try {
			Thread.sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}

		// Returns the empty string currently.
		// TODO Return something more meaningful
		return sb.toString();
	}

	public void close() {
		_frame.dispose();
	}

	/**
	 * A component that paints itself with a single solid (non-translucent)
	 * color.
	 * 
	 * @author Timothy A. Mann
	 * 
	 */
	public static class SolidColorComponent extends JComponent {
		private static final long serialVersionUID = 8947073636048085790L;
		private Color _c;

		/**
		 * Constructs a solid color component from any color. If the color
		 * argument contains an alpha channel then this constructor removes the
		 * alpha channel when it copies the color internally.
		 * 
		 * @param c
		 *            an color
		 */
		public SolidColorComponent(Color c) {
			_c = new Color(c.getRed(), c.getGreen(), c.getBlue());
		}

		@Override
		public void paintComponent(Graphics g) {
			g.setColor(_c);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
	}

	/**
	 * A component for painting the color coded key for this visualization.
	 * 
	 * @author Timothy A. Mann
	 * 
	 */
	public static class KeyPanel extends JPanel {
		private static final long serialVersionUID = -4878871758162122727L;

		public KeyPanel() {
			super();
			GridBagLayout gblayout = new GridBagLayout();
			setLayout(gblayout);
			GridBagConstraints cc = new GridBagConstraints();
			cc.gridx = 0;
			cc.gridwidth = 1;
			cc.gridheight = 1;
			cc.weightx = 0.1;
			cc.fill = GridBagConstraints.BOTH;
			GridBagConstraints cl = new GridBagConstraints();
			cl.gridx = 1;
			cl.gridwidth = 1;
			cl.gridheight = 1;
			cl.weightx = 0.9;
			cl.fill = GridBagConstraints.BOTH;

			cc.gridy = 0;
			cl.gridy = 0;
			add(new SolidColorComponent(QUANT_COLOR), cc);
			add(new JLabel("Quantity"), cl);

			cc.gridy = 2;
			cl.gridy = 2;
			add(new SolidColorComponent(UNMET_COLOR), cc);
			add(new JLabel("Unmet Demand"), cl);

			cc.gridy = 1;
			cl.gridy = 1;
			add(new SolidColorComponent(DEMAND_COLOR), cc);
			add(new JLabel("Demand"), cl);
		}
	}

	/**
	 * A component for visualizing quantity, demand, and unmet demand summed
	 * over all commodities. This gives a broad overview of what is going on.
	 * 
	 * @author Timothy A. Mann
	 * 
	 */
	public static class InventoryTotalsPanel extends JPanel {
		private static final long serialVersionUID = -4585572438558662818L;
		private State _state;

		public InventoryTotalsPanel() {
			super();
			_state = null;
		}

		public void updateState(State state) {
			_state = state;
		}

		@Override
		public void paintComponent(Graphics g) {
			if (_state != null) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(Color.BLACK);
				g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

				TYPE_NAME commodity_type = new TYPE_NAME("commodity");
				ArrayList<LCONST> list_commodities = _state._hmObject2Consts
						.get(commodity_type);

				int compPxlWidth = this.getWidth();
				int compPxlHeight = this.getHeight();

				int numColumns = 3;
				double pxlWidth = compPxlWidth / (double) numColumns;

				PVAR_NAME MAX_INVENTORY = new PVAR_NAME("MAX_INVENTORY");
				PVAR_NAME QUANT = new PVAR_NAME("quant");
				PVAR_NAME UNMET = new PVAR_NAME("unmet");
				PVAR_NAME DEMAND = new PVAR_NAME("demand");

				ArrayList<LCONST> params0 = new ArrayList<LCONST>(0);

				ArrayList<LCONST> params1 = new ArrayList<LCONST>(1);
				params1.add(null);

				try {
					double maxInventory = ((Integer) _state.getPVariableAssign(
							MAX_INVENTORY, params0)).doubleValue();

					int qSum = 0;
					int uSum = 0;
					int dSum = 0;
					for (int c = 0; c < list_commodities.size(); c++) {
						LCONST com = list_commodities.get(c);
						params1.set(0, com);
						int q = (Integer) _state.getPVariableAssign(QUANT,
								params1);
						qSum += q;

						int unmet = (Integer) _state.getPVariableAssign(UNMET,
								params1);
						uSum += unmet;

						int demand = (Integer) _state.getPVariableAssign(
								DEMAND, params1);
						dSum += demand;
					}

					Color quantColor = QUANT_COLOR;
					double qFillHeight = ((qSum / maxInventory) * compPxlHeight);
					Rectangle2D qRect = new Rectangle2D.Double(pxlWidth * 0,
							compPxlHeight - qFillHeight, pxlWidth, qFillHeight);
					g2.setColor(quantColor);
					g2.fill(qRect);
					g2.setColor(Color.BLACK);
					g2.draw(qRect);

					String qSumStr = String.valueOf(qSum);
					drawStringWithBackdrop(g2, qSumStr, 0, numColumns);

					Color unmetColor = UNMET_COLOR;
					double unmetFillHeight = ((uSum / maxInventory) * compPxlHeight);
					Rectangle2D uRect = new Rectangle2D.Double(pxlWidth * 2,
							compPxlHeight - unmetFillHeight, pxlWidth,
							unmetFillHeight);
					g2.setColor(unmetColor);
					g2.fill(uRect);

					String uSumStr = String.valueOf(uSum);
					drawStringWithBackdrop(g2, uSumStr, 2, numColumns);

					Color demandColor = DEMAND_COLOR;
					double demandFillHeight = ((dSum / maxInventory) * compPxlHeight);
					Rectangle2D dRect = new Rectangle2D.Double(pxlWidth * 1,
							compPxlHeight - demandFillHeight, pxlWidth,
							demandFillHeight);
					g2.setColor(demandColor);
					g2.fill(dRect);

					String dSumStr = String.valueOf(dSum);
					drawStringWithBackdrop(g2, dSumStr, 1, numColumns);

				} catch (EvalException ex) {
					ex.printStackTrace();
				} catch (NullPointerException ex) {
					// ex.printStackTrace();
				}

				g2.dispose();
			}
		}

		/**
		 * Draws a string with a translucent backdrop. This is useful for
		 * drawing strings on top of the visualization.
		 * 
		 * @param g2
		 *            a graphics instance
		 * @param content
		 *            the string to draw
		 * @param column
		 *            the column number
		 * @param numColumns
		 *            the total number of columns
		 */
		private void drawStringWithBackdrop(Graphics2D g2, String content,
				int column, int numColumns) {
			double colWidth = this.getWidth() / (double) numColumns;
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D strBounds = fm.getStringBounds(content, g2);

			int x = (int) (colWidth * column + 0.5 * colWidth - strBounds
					.getWidth() / 2);
			int y = (int) (this.getHeight() - 2 * BACKDROP_PAD);
			g2.setColor(BACKDROP_COLOR);
			g2.fillRoundRect(x - BACKDROP_PAD,
					(int) (y - strBounds.getHeight()),
					(int) (strBounds.getWidth() + 2 * BACKDROP_PAD),
					(int) (strBounds.getHeight() + 2 * BACKDROP_PAD),
					BACKDROP_ARC, BACKDROP_ARC);
			g2.setColor(Color.WHITE);
			g2.drawString(content, x, y);
		}
	}

	/**
	 * A component to display the inventory for each commodity separately.
	 * 
	 * @author Timothy A. Mann
	 * 
	 */
	public static class InventoryPanel extends JPanel {
		private static final long serialVersionUID = -8696171670145774254L;

		private State _state;

		public InventoryPanel() {
			super();
			_state = null;
		}

		public void updateState(State state) {
			_state = state;
		}

		@Override
		public void paintComponent(Graphics g) {
			if (_state != null) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(Color.BLACK);
				g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

				TYPE_NAME commodity_type = new TYPE_NAME("commodity");
				ArrayList<LCONST> list_commodities = _state._hmObject2Consts
						.get(commodity_type);

				int compPxlWidth = this.getWidth();
				int compPxlHeight = this.getHeight();

				int numCommodities = list_commodities.size();
				double commodityPxlWidth = compPxlWidth / numCommodities;

				PVAR_NAME MAX_INVENTORY = new PVAR_NAME("MAX_INVENTORY");
				PVAR_NAME QUANT = new PVAR_NAME("quant");
				PVAR_NAME UNMET = new PVAR_NAME("unmet");
				PVAR_NAME DEMAND = new PVAR_NAME("demand");

				ArrayList<LCONST> params0 = new ArrayList<LCONST>(0);

				ArrayList<LCONST> params1 = new ArrayList<LCONST>(1);
				params1.add(null);

				try {
					double maxInventory = ((Integer) _state.getPVariableAssign(
							MAX_INVENTORY, params0)).doubleValue();

					for (int c = 0; c < list_commodities.size(); c++) {
						LCONST com = list_commodities.get(c);
						params1.set(0, com);
						int q = (Integer) _state.getPVariableAssign(QUANT,
								params1);

						Color quantColor = QUANT_COLOR;
						double qFillHeight = ((q / maxInventory) * compPxlHeight);
						Rectangle2D qRect = new Rectangle2D.Double(
								commodityPxlWidth * c, compPxlHeight
										- qFillHeight, commodityPxlWidth,
								qFillHeight);
						g2.setColor(quantColor);
						g2.fill(qRect);
						g2.setColor(Color.BLACK);
						g2.draw(qRect);

						Color unmetColor = UNMET_COLOR;
						int unmet = (Integer) _state.getPVariableAssign(UNMET,
								params1);
						double unmetFillHeight = ((unmet / maxInventory) * compPxlHeight);
						Rectangle2D uRect = new Rectangle2D.Double(
								commodityPxlWidth * c, compPxlHeight
										- unmetFillHeight, commodityPxlWidth,
								unmetFillHeight);
						g2.setColor(unmetColor);
						g2.fill(uRect);

						Color demandColor = DEMAND_COLOR;
						int demand = (Integer) _state.getPVariableAssign(
								DEMAND, params1);
						double demandFillHeight = ((demand / maxInventory) * compPxlHeight);
						Rectangle2D dRect = new Rectangle2D.Double(
								commodityPxlWidth * c, compPxlHeight
										- demandFillHeight, commodityPxlWidth,
								demandFillHeight);
						g2.setColor(demandColor);
						g2.fill(dRect);

						drawStringWithBackdrop(g2, "U=" + unmet + ":D="
								+ demand + ":Q=" + q + ":" + com._sConstValue,
								c, numCommodities);
					}

				} catch (EvalException ex) {
					ex.printStackTrace();
				} catch (NullPointerException ex) {
					// ex.printStackTrace();
				}

				g2.dispose();
			}
		}

		/**
		 * Draws a string with a translucent backdrop. This is useful for
		 * drawing text over the visualization.
		 * 
		 * @param g2
		 *            a graphics instance
		 * @param content
		 *            the string to draw (the string is split into multiple
		 *            lines wherever ':' is encountered)
		 * @param column
		 *            the column to draw the string at
		 * @param numColumns
		 *            the total number of columns
		 */
		private void drawStringWithBackdrop(Graphics2D g2, String content,
				int column, int numColumns) {
			String[] lines = content.split(":");

			double colWidth = this.getWidth() / (double) numColumns;
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D strBounds = null;
			Rectangle2D[] lineBounds = new Rectangle2D[lines.length];
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				Rectangle2D rect = fm.getStringBounds(line, g2);
				lineBounds[i] = new Rectangle2D.Double(rect.getX(),
						rect.getY(), rect.getWidth(), rect.getHeight());
				if (strBounds == null) {
					strBounds = rect;
				} else {
					strBounds.setRect(strBounds.getX(), strBounds.getY(),
							Math.max(strBounds.getWidth(), rect.getWidth()),
							strBounds.getHeight() + rect.getHeight());
				}
			}

			int x = (int) (colWidth * column + 0.5 * colWidth - strBounds
					.getWidth() / 2);
			int y = (int) (strBounds.getHeight() + 2 * BACKDROP_PAD);
			g2.setColor(BACKDROP_COLOR);
			g2.fillRoundRect(x - BACKDROP_PAD,
					(int) (y - strBounds.getHeight()),
					(int) (strBounds.getWidth() + 2 * BACKDROP_PAD),
					(int) (strBounds.getHeight() + 2 * BACKDROP_PAD),
					BACKDROP_ARC, BACKDROP_ARC);

			int lheightSum = 0;
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].startsWith("Q=")) {
					g2.setColor(toSolidColor(QUANT_COLOR));
				} else if (lines[i].startsWith("D=")) {
					g2.setColor(toSolidColor(DEMAND_COLOR));
				} else if (lines[i].startsWith("U=")) {
					g2.setColor(toSolidColor(UNMET_COLOR));
				} else {
					g2.setColor(Color.WHITE);
				}

				int lineX = (int) (colWidth * column + 0.5 * colWidth - lineBounds[i]
						.getWidth() / 2);
				g2.drawString(lines[i], lineX, (y - lheightSum));
				lheightSum = (int) (lheightSum + lineBounds[i].getHeight());
			}
		}

		public Color toSolidColor(Color c) {
			return new Color(c.getRed(), c.getGreen(), c.getBlue());
		}
	}

}
