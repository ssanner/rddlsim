package rddl.evaluate;

import java.util.ArrayList;

public class Statistics {

	public static double Min(ArrayList<Double> l) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < l.size(); i++)
			min = Math.min(min, l.get(i));
		return min;
	}

	public static double Max(ArrayList<Double> l) {
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < l.size(); i++)
			max = Math.max(max, l.get(i));
		return max;
	}

	public static double Avg(ArrayList<Double> l) {
		double accum = 0d;
		for (int i = 0; i < l.size(); i++) {
			accum += l.get(i);
		}
		return accum / l.size();
	}
	
	public static double StdError95(ArrayList<Double> l) {
		if (l.size() <= 1)
			return Double.NaN;
			
		double avg = Avg(l);
		double ssq = 0d;
		for (int i = 0; i < l.size(); i++) {
			double factor = (l.get(i) - avg);
			ssq += factor * factor;
		}	
		double stdev = Math.sqrt(ssq / (l.size() - 1));
		return 1.96d * (stdev / Math.sqrt(l.size()));
	}
}
