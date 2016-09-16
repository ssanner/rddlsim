package rddl.det.mip;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;

import rddl.RDDL.EXPR;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.OBJECT_VAL;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;
import util.Pair;

public class EmergencyDomainDataReel {
	private String file_name;
	private String separator;
	private int numFolds;
	private boolean IID;
	private boolean sequential;

	private ArrayList<EmergencyDomainDataReelElement>[] frames;
	private ArrayList<EmergencyDomainDataReelElement> all_frames;
	private int trainingFoldIdx;
	private int testingFoldIdx;
	private int testingInstanceIdx;
	
	public ArrayList<Integer> getLeads(final EmergencyDomainDataReelElement someFrame,
			final int foldIdx){
		assert( sequential );
		ArrayList<Integer> ret = new ArrayList<>();
		
		for( int i = 1 ; i < frames[foldIdx].size(); ++i ){
			final EmergencyDomainDataReelElement prevCall = frames[foldIdx].get(i-1);
			final EmergencyDomainDataReelElement curCall = frames[foldIdx].get(i);
			assert( ! prevCall.callDate.isAfter( curCall.callDate ) );
			boolean chosen = false;
			if( prevCall.callDate.isBefore( curCall.callDate ) ){
				if( ((prevCall.compareTo(someFrame) <= 0) && (curCall.compareTo(someFrame) < 0))
					|| ((prevCall.compareTo(someFrame) > 0) && (curCall.compareTo(someFrame) > 0)) ){
					chosen = true;
				}
			}else if ( (prevCall.compareTo(someFrame) <= 0) && (curCall.compareTo(someFrame) > 0) ){
				chosen = true;
			}
			//must be greater by some minimum. First eliminate same minute calls. Comes down to precision handling by Gurobi. 
//			if( chosen && prevCall.callTime.getMinute()==curCall.callTime.getMinute() && curCall.callTime.getHour()==prevCall.callTime.getHour() ){
//				ret.add(i+1);
//			}else
			if( chosen ){
				ret.add(i);
			}
		}
		return ret;
	}
	
	public EmergencyDomainDataReelElement getNextTestingInstance( ){
		assert( testingInstanceIdx < frames[getTestingFoldIdx()].size() );
		return frames[getTestingFoldIdx()].get(testingInstanceIdx++);
	}
	
	public EmergencyDomainDataReel(String file_name, String separator, 
			final boolean sequential, final boolean iid, 
			final int numfolds, final int trainingFoldIdx, final int testingFoldIdx) throws FileNotFoundException, IOException {
		this.file_name = file_name;
		this.separator = separator;
		this.IID = iid;
		this.sequential = sequential;
		this.numFolds = numfolds;
		this.trainingFoldIdx = trainingFoldIdx;
		this.setTestingFoldIdx(testingFoldIdx);
		
		all_frames = new ArrayList<EmergencyDomainDataReelElement>();

		try(BufferedReader br =
                   new BufferedReader(new FileReader(file_name))){
			String line = br.readLine();
			while( (line = br.readLine()) != null ){
				EmergencyDomainDataReelElement thisEntry = new EmergencyDomainDataReelElement(line, separator, true);
				all_frames.add( thisEntry );	
			}
		}
		
		int total_size = all_frames.size();
		int fold_size = (int) Math.floor( (1.0*total_size) / numFolds );
		
		frames = new ArrayList[ numFolds ];
		if(sequential){
			for( int i = 0 ; i < numFolds-1; ++i ){
				frames[i] = new ArrayList<>(all_frames.subList(i*fold_size, (i+1)*fold_size));
			}
			frames[numFolds-1] = new ArrayList<>(all_frames.subList((numFolds-1)*fold_size, all_frames.size())); 
		}else{
			throw new UnsupportedOperationException("only sequential data reel implemented");
		}
		
		System.out.println(all_frames.size() + " calls loaded.");
		for( int i = 0 ; i < numFolds; ++i ){
			System.out.println( "Fold " + i + " " + frames[i].size() );
		}
	}
	
	//methods to convert from and back RDDL state

	
	protected ArrayList<Pair<EXPR,EXPR>> to_RDDL_EXPR_constraints(ArrayList<EmergencyDomainDataReelElement>[] futures,
			final LVAR future_PREDICATE, final ArrayList<LCONST> future_indices,
			final LVAR TIME_PREDICATE, final ArrayList<LCONST> time_indices, 
			Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants, 
			Map<TYPE_NAME, OBJECTS_DEF> objects ){
		assert( futures.length == future_indices.size() );
		assert( futures[0].size() == time_indices.size() );
		
		ArrayList<Pair<EXPR,EXPR>> ret = new ArrayList<>();
		for( int f = 0; f < futures.length; ++f ){
//			int offset = 0;
			double prev_time = 0;
			for( int t = 0; t < futures[f].size(); ++t ){
				EmergencyDomainDataReelElement cur_future = futures[f].get(t);
				double cur_future_call_time_double = EmergencyDomainDataReelElement.timeToDouble(cur_future.callTime, cur_future.callDate);
				
				if( cur_future_call_time_double <= prev_time ){
//					cur_future_call_time_double = prev_time + 0.01;
					try{
						throw new Exception("call too close");
					}catch( Exception exc ){
						exc.printStackTrace();
						System.exit(1);
					}
				}
				
				assert( prev_time <= cur_future_call_time_double );
				prev_time = cur_future_call_time_double;
				
				ArrayList<Pair<EXPR, EXPR>> expr_constraints = 
						cur_future.to_RDDL_EXPR_constraints( f, t, future_PREDICATE,
								future_indices, TIME_PREDICATE, time_indices, constants, objects );
//				System.out.println( expr_constraints );
				
				ret.addAll( expr_constraints );
			}
		}
		return ret;
	}
	
	public ArrayList<EmergencyDomainDataReelElement>[] getFutures(
			final EmergencyDomainDataReelElement current, final RandomDataGenerator rand, 
			final int numFutures, final int length, final int foldIdx){

		ArrayList<EmergencyDomainDataReelElement>[] ret = new ArrayList[numFutures];
		System.out.println("Imagining futures ... ");
		for( int t = 0; t < length; ++t ){
			for( int f = 0 ; f < numFutures; ++f ){
				if( t == 0 ){
					ret[f] = new ArrayList<>( );
					ret[f].add( current );
				}else{
					EmergencyDomainDataReelElement prevCall = ret[f].get(t-1);
					ArrayList<Integer> leads = getLeads(prevCall, foldIdx);	
					
					final int idx = rand.nextInt(0, leads.size()-1);
					final EmergencyDomainDataReelElement thatElem = ( frames[foldIdx].get( leads.get( idx ) ) );
					
					//fix date to be not in the past
					LocalDate newCallDate;
					if( thatElem.callTime.isBefore(prevCall.callTime) ){
						newCallDate = LocalDate.ofYearDay( prevCall.callDate.getYear(), prevCall.callDate.getDayOfYear()+1);
					}else{
						newCallDate = LocalDate.ofYearDay( prevCall.callDate.getYear(), prevCall.callDate.getDayOfYear()); 
					}
					
					//randomize call x and y in the same region ? 
					
					ret[f].add( new EmergencyDomainDataReelElement( thatElem.callId, thatElem.natureCode, 
							newCallDate, thatElem.callTime, thatElem.callAddress, thatElem.callX, thatElem.callY, false ) ); 
				}
				if( t == length-1 ){
					System.out.println("Future " + f + " " + ret[f]);
				}
			}
		}
		
		return ret;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		EmergencyDomainDataReel reel = new EmergencyDomainDataReel("./files/emergency_domain/jan_2011_calls.csv", 
				",", true, false, 2, 0, 1);
		EmergencyDomainDataReelElement current = reel.all_frames.get(17);
		System.out.println( current );
		
		ArrayList<Integer> leads = reel.getLeads(current, 0);
		System.out.println("Leads : " + leads);
		for( final int l : leads ){
			System.out.println( reel.frames[reel.trainingFoldIdx].get(l) );
		}
		
		RandomDataGenerator rand = new RandomDataGenerator();
		final int numFutures = 40;
		final int length = 10;
		
		ArrayList<EmergencyDomainDataReelElement>[] futures = reel.getFutures(current, rand , numFutures, length, 0);
		for( ArrayList<EmergencyDomainDataReelElement> future : futures ){
			System.out.println(future);
			System.out.println("==============================================");
		}
		
		final LVAR TIME_PREDICATE = new LVAR( "?time" );
		final LVAR future_PREDICATE = new LVAR( "?future" );
		
		System.out.println( reel.to_RDDL_EXPR_constraints(futures, future_PREDICATE, TIME_PREDICATE, numFutures, length, 
									  Collections.<PVAR_NAME, Map<ArrayList<LCONST>, Object>>emptyMap(), 
									  Collections.<TYPE_NAME, OBJECTS_DEF>emptyMap()) );
		
		//TODO : TEST : State -> Element
		//TODO : TEST : EXPR -> GRBConstr -> HOP
		//Tested : Element -> Futures -> Expressions
		
	}

	private ArrayList<Pair<EXPR,EXPR>> to_RDDL_EXPR_constraints(final ArrayList<EmergencyDomainDataReelElement>[] futures, final LVAR future_PREDICATE,
			final LVAR time_PREDICATE, int numFutures, int length, Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants,
			Map<TYPE_NAME, OBJECTS_DEF> objects) {
		ArrayList<LCONST> future_indices = makeIndices(numFutures, "future");
		ArrayList<LCONST> time_indices = makeIndices(length, "time");
		return to_RDDL_EXPR_constraints(futures, future_PREDICATE, future_indices, time_PREDICATE, time_indices, constants, objects);
	}
	
	private static ArrayList<LCONST> makeIndices( final int numIdx, final String prefix ){
		ArrayList<LCONST> ret = new ArrayList<LCONST>();
		for( int f = 0; f < numIdx; ++f ){
			ret.add( new OBJECT_VAL(prefix + f) );
		}
		return ret;
	}

	public void resetTestIndex(final int idx) {
		assert( idx >= 0 && idx < getNumTestInstances() );
		testingInstanceIdx=idx;
	}

	public int getNumTestInstances() {
		return frames[getTestingFoldIdx()].size();
	}

	public int getTestingFoldIdx() {
		return testingFoldIdx;
	}

	public void setTestingFoldIdx(int testingFoldIdx) {
		this.testingFoldIdx = testingFoldIdx;
	}

	public EmergencyDomainDataReelElement getInstance(final int idx, final int foldIdx ){ 
		return frames[foldIdx].get(idx);
	}

	public int getTrainingFoldIdx() {
		return trainingFoldIdx;
	}
}
