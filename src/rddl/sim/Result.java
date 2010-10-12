/**
 * RDDL: Stores result information for the RDDL simulator.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.sim;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Result {

	public DecimalFormat _df = new DecimalFormat("0.###");
	
	public Result(double accum_reward, ArrayList<Double> rewards) {
		_accumReward = accum_reward;
		_rewards = rewards;
	}
	
	public double _accumReward;
	public ArrayList<Double> _rewards;
	
	public String toString() {
		return "Objective value = " + _df.format(_accumReward) + ", rewards: " + _rewards.toString();
	}
}
