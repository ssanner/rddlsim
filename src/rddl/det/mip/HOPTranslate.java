package rddl.det.mip;

import gurobi.GRBException;

public class HOPTranslate extends Translate {

	public enum FUTURE_SAMPLING {
		protected int num_futures;
		protected enum SAMPLING_STRATEGY {
			MEAN, SAMPLE
		}
	};
	
	
	private int num_futures = 0;
	private FUTURE_SAMPLING future_gen;
	
	public HOPTranslate( final String domain_file, final String inst_file, 
			final int lookahead , final double timeout ,
			final int num_futures, final FUTURE_SAMPLING sampling ) throws Exception, GRBException {
		super( domain_file, inst_file, lookahead, timeout );
		this.num_futures = num_futures;
		this.future_gen = sampling;
	}
	
	
	
}
