/**
 * RDDL: Abstact class interface used by Simulator to display the state.
 *       Can simply use a GenericScreenDisplay or extend with a
 *       graphical visualization for specific RDDL domains if desired.
 *       See SysAdminScreenDisplay for a trivial example for SysAdmin.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import rddl.State;

public abstract class StateViz {
	public StateViz() { }
	public abstract void display(State s, int time);
	public void close() { }
}
