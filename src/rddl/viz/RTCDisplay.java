/**
 * RDDL: Simple Visualization for the Room Temperature Control problem.
 *
 * @author Ivan Zhou
 * @version 11/06/16
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.awt.Dimension;
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

public class RTCDisplay extends StateViz {

	private static final Color HOT_COLOR = Color.black;
	private static final Color COLD_COLOR = Color.gray;
	private static final Color COMFORT_COLOR = Color.green;
	private static final Color BLOCK_COLOR  = new Color(238, 238, 238);
	private static final int AIR_COLOR_PARAMETER_1 = 255;	//Change the intensive of the color
	private static final int AIR_COLOR_PARAMETER_2 = 255;
	private static final int AIR_COLOR_PARAMETER_3 = 255;
	private static final int INSET_SIZE = 5;
	private static final int PREF_PIXEL_WIDTH = 700;
	private static final int PREF_PIXEL_HEIGHT = 480;

	private int _nTimeDelay;
	private JFrame _frame;	//Create a frame
	private TemperaturePanel _temPanel; //Panel for displaying temperature changes in space
	static ArrayList<Double> Temperature = new ArrayList<>();
	static ArrayList<Double> Heat_v = new ArrayList<>();

	public RTCDisplay(){
		_nTimeDelay = 500; //in milliseconds
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

	private String getStateDescription(State s) throws EvalException{
		StringBuilder sb = new StringBuilder();	//Object like String, except that its length and content can be modified

		// If there is no frame yet, then we create one and pack it with the
		if (_frame == null){
			JFrame.setDefaultLookAndFeelDecorated(true);
			_frame = new JFrame();
			_frame.setTitle("Room Temperature Control");	//Set the title of the frame
			_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			_frame.setLayout(new GridLayout(1, 1));
			_frame.setPreferredSize(new Dimension(PREF_PIXEL_WIDTH,
					PREF_PIXEL_HEIGHT));

			//Add a panel with multiple components
			JPanel panel = new JPanel();
			GridBagLayout gblayout = new GridBagLayout();
			panel.setLayout(gblayout);
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1;
			c.weighty = 1;
			c.insets = new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE,
					INSET_SIZE);
			c.fill = GridBagConstraints.BOTH;

			c.gridx = 0;		//Specify the row and column at the upper left of the component
			c.gridy = 0;
			c.weightx = 1.0;	//Specify the distribution of space among the columns
			c.weighty = 0.1;	//Specify the distribution of space among the rows
			c.gridwidth = 1;	//Specify the number of columns
			c.gridheight = 1;	//Specify the number of rows

			JLabel invLabel = new JLabel("Space Temperature Monitoring",JLabel.CENTER);
			gblayout.setConstraints(invLabel, c);
			panel.add(invLabel);	//Panel for the title of the chart

			_temPanel = new TemperaturePanel();
			c.gridx = 0;
			c.gridy = 1;
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.gridwidth = 1;
			c.gridheight = 1;
			gblayout.setConstraints(_temPanel,c);
			panel.add(_temPanel);	//Panel for displaying the change in temperature

			KeyPanel keyPanel = new KeyPanel();
			c.gridx = 1;
			c.gridy = 1;
			c.weightx = 0.1;
			c.weighty = 1.0;
			c.gridwidth = 1;
			c.gridheight = 1;
			gblayout.setConstraints(keyPanel, c);
			panel.add(keyPanel);	//Panel showing the color-coded key

			//Set up the frame and make it visible
			_frame.add(panel);
			_frame.pack();
			_frame.setVisible(true);
		}

		//Update the panel components with state information
		_temPanel.updateState(s);	//Update the Temperature Panel
		_frame.repaint();

		// Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
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
		//_frame.dispose();
	}

	/**
	 * A component that paints itself with a single solid (non-translucent)
	 * color.
	 *
	 * @author Timothy A. Mann
	 *
	 */
	public static class SolidColorComponent extends JComponent {
		private static final long serialVersionUID = 8947073636048085790L;	//The serialVersionUID is used as a version control in a Serializable class.
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
			g.fillRect(0, 0, this.getWidth(), this.getHeight());	//fillRect(x, y, width, height): Fill a specified rectangle. The left and right edges: x and x + width - 1. The top and bottom edges: y and y + height - 1.
		}
	}

	/**
	 * A component for visualizing the change in temperature in each space according to time.
	 * @author Ivan
	 *
	 */
	public static class TemperaturePanel extends JPanel {
		private static final long serialVersionUID = -8696171670145774254L;
		private State _state;
		private double intensity = 1;
		public Color AIR_COLOR = new Color((int)(AIR_COLOR_PARAMETER_1*intensity), (int)(AIR_COLOR_PARAMETER_2*intensity), (int)(AIR_COLOR_PARAMETER_3*intensity));

		public TemperaturePanel(){
			super();
			_state = null;
		}

		public void updateState(State state) {
			_state = state;
		}

		@Override
		public void paintComponent(Graphics g){
			if (_state != null){
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(Color.BLACK);
				g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

				TYPE_NAME space_type = new TYPE_NAME("space");	//Object
				ArrayList<LCONST> list_space = _state._hmObject2Consts.get(space_type);	//Get a list of Object

				int compPxlWidth = this.getWidth();
				int compPxlHeight = this.getHeight();

				int numSpace = list_space.size(); //Number of objects
				double spacePxlHeight = compPxlHeight / (numSpace+0.1);	//The height of each bar in the panel
				double spacePxlWidth = compPxlWidth / 45;	//The width of each block for the temperature of the space; the maximum amount is set as 50

				PVAR_NAME TEMP = new PVAR_NAME("TEMP");	//State-Fluent
				PVAR_NAME ROUND = new PVAR_NAME("round");	//State-Fluent
				PVAR_NAME AIR_VOLUME =  new PVAR_NAME("AIR_VOLUME");	//State-Fluent indicates if the air is on
				PVAR_NAME TEMP_UP = new PVAR_NAME("TEMP_UP");	//Upper Bound of the desired temperature
				PVAR_NAME TEMP_LOW = new PVAR_NAME("TEMP_LOW");	//Lower bound of the desired temperature
				PVAR_NAME AIR_MAX = new PVAR_NAME("AIR_MAX");
				PVAR_NAME IS_ROOM = new PVAR_NAME("IS_ROOM");
				ArrayList<LCONST> params0 = new ArrayList<LCONST>(0);
				ArrayList<LCONST> params1 = new ArrayList<LCONST>(1); //Record on space at a time
				params1.add(null);

				try{
					int round = (Integer) _state.getPVariableAssign(ROUND, params0);	//Get the Round Number, used for changing the x-position of the bar
					//We will use two constant number for the temperature ceiling and ground; later we will use variables based on the domain specification
					for (int s = 0; s < list_space.size(); s++){
						LCONST space = list_space.get(s);
						params1.set(0, space);
						double t = (Double) _state.getPVariableAssign(TEMP, params1);	//Get the temperature of the space
						double maxTemp = (Double) _state.getPVariableAssign(TEMP_UP, params1);	//Upper Bound of the desired temperature
						double minTemp = (Double) _state.getPVariableAssign(TEMP_LOW, params1);////Lower bound of the desired temperatur
						double air_v = (Double) _state.getPVariableAssign(AIR_VOLUME, params1);	//Get the Action Fluent Air
						double air_max = (Double) _state.getPVariableAssign(AIR_MAX, params1); 
						boolean is_room = (Boolean) _state.getPVariableAssign(IS_ROOM, params1); 
						Temperature.add(t);
						Heat_v.add(air_v);
						Color tempColor;


						//Use the Object Name to indicate each bar.
						String object_name = String.valueOf(list_space.get(s));
						int x = 2;
						int y = (int) (spacePxlHeight*(s + 1.1));
						g2.setColor(Color.red);
						g2.drawString(object_name, x, y);

						for (int r = 0; r < round; r++){

							t = Temperature.get(r*numSpace+s);	//Space Temperature
							air_v = Heat_v.get(r*numSpace+s);	//If the air is on
							if (t > maxTemp)
								tempColor = HOT_COLOR;	//Temperature is beyond the comfortable range
							else if (t < minTemp)
								tempColor = COLD_COLOR;	//Temperature is below the comfortable range
							else tempColor = COMFORT_COLOR;	//Temperature is at the comfortable range

							Rectangle2D tRect_0 = new Rectangle2D.Double(spacePxlWidth*(r+3), spacePxlHeight * (s+0.1), spacePxlWidth, spacePxlHeight);	//The x-position will later be changed according to the TIME
							g2.setColor(tempColor);
							g2.fill(tRect_0);

							double tFillHeight = ((1-Math.min(t, 40.0)/40)*spacePxlHeight); //Use the temperature value to decide on the height of the bar; later the maximum ceiling will be changed to a variable
							Rectangle2D tRect_Block = new Rectangle2D.Double(spacePxlWidth*(r+3), spacePxlHeight * (s+0.1), spacePxlWidth, tFillHeight);	//The x-position will later be changed according to the TIME
							g2.setColor(BLOCK_COLOR);
							g2.fill(tRect_Block);

							/*
							 * The current temperature is affected by the HVAC (on//off) in the previous round;
							 * therefore, we mark the AIR_ON one round before the current one to reflect this after-effect
							 */
							if (air_v > 0 && is_room){
								Rectangle2D tRect_Air = new Rectangle2D.Double(spacePxlWidth*(r+3-1), spacePxlHeight * (s+0.2), spacePxlWidth, 5);
								intensity = (1-air_v/air_max);
								AIR_COLOR = new Color((int)(AIR_COLOR_PARAMETER_1), (int)(AIR_COLOR_PARAMETER_2*intensity), (int)(AIR_COLOR_PARAMETER_3*intensity));g2.setColor(AIR_COLOR);
								g2.fill(tRect_Air);
							}

						}

					}
				} catch (EvalException ex){
					ex.printStackTrace();
				} catch (NullPointerException ex){
					//ex.printStackTrace();
				}

				g2.dispose();	//Release the system resources
			}
		}
	}

	/**
	 * A component for painting the color coded key for this visualization.
	 *
	 *
	 *
	 */
	public static class KeyPanel extends JPanel {
		private static final long serialVersionUID = -4878871758162122727L;
		private Color AIR_COLOR = new Color(AIR_COLOR_PARAMETER_1, 0,0);


		public KeyPanel() {
			super();
			GridBagLayout gblayout = new GridBagLayout();
			setLayout(gblayout);
			GridBagConstraints cc = new GridBagConstraints();
			cc.gridx = 0;
			cc.gridwidth = 1;
			cc.gridheight = 1;
			cc.weightx = 0.3;
			cc.fill = GridBagConstraints.BOTH;	//Resize the component both horizontally and vertically
			GridBagConstraints cl = new GridBagConstraints();
			cl.gridx = 1;
			cl.gridwidth = 1;
			cl.gridheight = 1;
			cl.weightx = 0.7;
			cl.fill = GridBagConstraints.BOTH;

			cc.gridy = 0;
			cl.gridy = 0;
			add(new SolidColorComponent(COLD_COLOR), cc);
			add(new JLabel("Too Cold"), cl);

			cc.gridy = 1;
			cl.gridy = 1;
			add(new SolidColorComponent(COMFORT_COLOR), cc);
			add(new JLabel("Comfortable"), cl);

			cc.gridy = 2;
			cl.gridy = 2;
			add(new SolidColorComponent(HOT_COLOR), cc);
			add(new JLabel("Too Hot"), cl);

			cc.gridy = 3;
			cl.gridy = 3;
			add(new SolidColorComponent(AIR_COLOR), cc);
			add(new JLabel("The HVAC is On"), cl);
		}
	}

}
