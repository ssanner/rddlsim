package rddl.det.mip;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import rddl.State;
import rddl.EvalException;
import rddl.RDDL.EXPR;
import rddl.RDDL.LCONST;
import rddl.RDDL.LVAR;
import rddl.RDDL.OBJECTS_DEF;
import rddl.RDDL.OBJECT_VAL;
import rddl.RDDL.PVAR_EXPR;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.REAL_CONST_EXPR;
import rddl.RDDL.INT_CONST_EXPR;
import rddl.RDDL.BOOL_CONST_EXPR;
import rddl.RDDL.TYPE_NAME;
import util.Pair;
import util.Point;
import util.Polygon;
import util.Polygon.Builder;

public class EmergencyDomainDataReelElement extends DataReelElement {
	public static final ArrayList<LCONST> emptySubstitution = new ArrayList<LCONST>();
	private static final ArrayList<LCONST> xPosSubstitution = new ArrayList<LCONST>( Collections.singletonList( new OBJECT_VAL("xpos") ) );
	private static final ArrayList<LCONST> yPosSubstitution = new ArrayList<LCONST>( Collections.singletonList( new OBJECT_VAL("ypos") ) );
	
	public static final PVAR_NAME currentCallTimePvarName = new PVAR_NAME("currentCallTime");
	public static final PVAR_NAME currentCallPvarName = new PVAR_NAME("currentCall");
	public static final PVAR_NAME currentCallCodePvarName = new PVAR_NAME("currentCallCode");
	public static final PVAR_NAME currentCallRegionPvarName = new PVAR_NAME("currentCallRegion");
	
	public static final PVAR_EXPR currentCallPvar = new PVAR_EXPR("currentCall", 
			new ArrayList<LVAR>( Collections.singletonList( new LVAR("?c") ) ) );
	public static final PVAR_EXPR currentCallTimePvar = new PVAR_EXPR("currentCallTime", emptySubstitution);
	public static final PVAR_EXPR currentCallCodePvar = new PVAR_EXPR("currentCallCode", 
			new ArrayList<LVAR>( Collections.singletonList( new LVAR("?c") ) ) );
	public static final PVAR_EXPR currentCallRegionPvar = new PVAR_EXPR("currentCallRegion", 
			new ArrayList<LVAR>( Collections.singletonList( new LVAR("?r") ) ) );
	
	public static final PVAR_EXPR tempUniformCausePvar = new PVAR_EXPR("tempUniformCause",emptySubstitution);
	public static final PVAR_NAME tempUniformCausePvarName = new PVAR_NAME("tempUniformCause");
	public static final PVAR_EXPR tempUniformRegionPvar = new PVAR_EXPR("tempUniformRegion", emptySubstitution);
	public static final PVAR_NAME tempUniformRegionPvarName = new PVAR_NAME("tempUniformRegion");
	
	public static final PVAR_NAME gapTimePvarName = new PVAR_NAME("gapTime");
	public static final PVAR_EXPR gapTimePvar = new PVAR_EXPR("gapTime", emptySubstitution);
	
	private static final double FeetToMiles = 0.000189;
	private static final double GoodSamCenterX = 1417.63;
	private static final double GoodSamCenterY = 66.97;
	
	public static final int dataYear = 2011;
	private static final double GoodSamRadius = 0.5;
	
	private static final double StonybrookCenterX = 1415.01;
	private static final double StonybrookCenterY = 62.99;
	private static final double StonybrookRadius = 0.04;
	
	private static final double WestHillsCenterX = 1414.46;
	private static final double WestHillsCenterY = 63.85;
	private static final double WestHillsRadius = 0.04;
	
	private final static float[] EMS_CITY_X_POINTS = {
			1408.972615f,1408.972615f,1409.231934f,1409.257867f,1409.569052f,1409.569052f,
			1409.983963f,1409.983963f,1410.009894f,1410.398875f,1411.747339f,1411.747339f,
			1412.032591f,1412.058522f,1412.188183f,1412.317843f,1412.317843f,1413.043938f,
			1413.018007f,1412.603095f,1412.55123f,1412.836482f,1412.862414f,1413.381055f,
			1413.381055f,1413.925626f,1414.003423f,1414.340538f,1414.340538f,1414.781382f,
			1414.781382f,1415.27409f,1415.689002f,1416.18171f,1416.18171f,1416.855943f,
			1416.881874f,1417.452378f,1417.452378f,1417.374581f,1417.193058f,1417.193058f,
			1417.530174f,1417.582037f,1417.867289f,1417.919155f,1417.582037f,1417.504241f,
			1418.152541f,1418.126611f,1417.348651f,1418.126611f,1417.141195f,1417.141195f,
			1417.348651f,1417.659834f,1417.789493f,1417.556107f,1417.73763f,1417.763562f,
			1417.47831f,1417.452378f,1417.037466f,1416.933737f,1417.348651f,1417.348651f,1417.063399f,
			1417.089329f,1416.285439f,1416.18171f,1415.844594f,1416.05205f,1415.455613f,1415.689002f,
			1415.300023f,1415.40375f,1415.040702f,1414.62579f,1415.144431f,	1414.651723f,1415.222227f,
			1416.415098f,1415.974254f,1415.948321f,1415.53341f,1415.455613f,1414.599857f,1414.599857f,
			1414.314607f,1414.470198f,1414.184946f,1413.770034f,1413.71817f,1414.055286f,
			1414.003423f,1413.303259f,1413.173599f,1411.954795f,1412.058522f,1412.603095f,
			1412.732755f,1412.55123f,1412.914278f,1412.914278f,1412.525299f,1411.539883f,1411.436154f,
			1410.969379f,1411.099039f,1410.99531f,1411.306495f,1411.306495f,1411.073106f,1411.176835f,
			1410.554467f,1410.554467f,1411.176835f,1411.228698f,1411.047175f,1411.021243f,1410.86565f,
			1410.710058f,1410.295146f,1410.398875f,1410.632264f,1409.594982f};
	
	private static final float[] EMS_CITY_Y_POINTS = {65.21569929f,65.73376152f,65.75966586f,66.04460037f,
			66.01869792f,66.22592319f,66.22592319f,66.38134167f,66.79579221f,66.95121069f,
			66.95121069f,67.1843403f,67.1843403f,67.54698405f,67.57288839f,67.54698405f,
			67.1843403f,67.1843403f,67.5210816f,67.5210816f,67.83191856f,67.8578229f,
			68.89394925f,68.89394925f,69.36020658f,69.46381827f,69.8782707f,69.90417315f,
			70.05959163f,70.08549597f,70.21501011f,70.24091445f,70.21501011f,70.13730087f,
			70.05959163f,70.05959163f,70.21501011f,70.18910766f,70.00778484f,69.72285033f,
			69.69694788f,69.12707697f,69.12707697f,69.30839979f,69.28249734f,68.8680468f,
			68.63491719f,67.8578229f,67.57288839f,67.1843403f,66.5108577f,65.91508434f,
			65.57834304f,64.12776615f,64.10186181f,64.46450745f,64.25728218f,64.10186181f,
			64.07595936f,63.60970203f,63.55789524f,63.76512051f,63.79102485f,63.01392867f,
			62.85851019f,62.26273683f,62.23683438f,61.53744933f,62.23683438f,61.97780232f,
			62.05551156f,62.49586644f,62.67718737f,63.06573546f,63.32476752f,63.73921806f,
			64.05005691f,63.42837921f,62.91031698f,61.69286781f,61.66696347f,60.0609681f,
			60.03506376f,59.51700153f,59.56880643f,60.3977094f,60.19048413f,60.63083712f,
			60.70854636f,61.04528766f,61.30431972f,61.4079333f,60.99348087f,60.94167597f,
			60.26819337f,59.77603359f,58.68810045f,58.68810045f,60.13867734f,60.13867734f,
			61.43383575f,61.64106102f,61.66696347f,62.0814159f,61.7964795f,61.82238384f,
			61.20070803f,61.35612651f,61.51154499f,61.58925423f,61.97780232f,62.4699621f,
			62.49586644f,62.98802622f,63.06573546f,63.24705828f,63.29886318f,63.73921806f,
			63.73921806f,63.99825012f,64.07595936f,64.20547539f,64.28318463f,64.59402348f,
			64.67173272f,65.18979495f};
	
	private static final Polygon EMS_CITY_REGION;
	static {
		assert( EMS_CITY_X_POINTS.length == EMS_CITY_Y_POINTS.length );
		Builder pBuilder = Polygon.Builder();
		for( int i = 0; i < EMS_CITY_X_POINTS.length; ++i ){
			pBuilder.addVertex( new Point(EMS_CITY_X_POINTS[i], EMS_CITY_Y_POINTS[i]) );
		}
		EMS_CITY_REGION = pBuilder.build();
	}
	
	private static final Polygon FIRE_CITY_REGION;
	private static final float[] FIRE_CITY_X_POINTS = {1413.32919f,1412.784618f,1412.940211f,
			1413.19953f,1413.355122f,1413.588511f,1413.744103f,1413.458851f,1413.147666f,
			1413.121734f,1412.940211f,1412.162251f,1412.136318f,1411.902931f,1411.902931f,
			1411.384291f,1411.358358f,1412.240047f,1412.240047f,1412.680891f,1412.862414f,
			1412.577162f,1412.577162f,1412.42157f,1412.29191f,1412.317843f,1412.214114f,
			1411.954795f,1411.851066f,1412.032591f,1412.084454f,1412.214114f,1412.240047f,
			1412.473435f,1412.499366f,1413.043938f,1413.043938f,1413.225463f,1413.381055f,
			1413.562578f,1413.562578f,1413.951559f,1414.133082f,1414.210878f,1414.444267f,
			1414.522063f,1415.040702f,1415.40375f,1415.766798f,1415.53341f,1415.689002f,
			1415.429683f,1415.377817f,1415.351886f,1414.651723f,1414.184946f,1413.821897f,
			1414.003423f,1414.729519f,1414.418334f,1414.418334f,1414.418334f,1414.49613f,
			1414.522063f,1414.340538f,1414.107149f,1414.081219f,1413.951559f,1413.562578f,
			1413.925626f};
	
	private static final float[] FIRE_CITY_Y_POINTS = {60.03506376f,60.03506376f,61.92599553f,61.92599553f,
			62.18502759f,62.18502759f,63.27296073f,63.32476752f,63.19525149f,62.8067034f,
			62.36635041f,62.36635041f,62.67718737f,62.67718737f,63.01392867f,63.01392867f,
			63.22115394f,63.58379958f,63.71331561f,63.73921806f,64.05005691f,64.07595936f,
			64.72353951f,65.29340853f,65.34521532f,65.75966586f,65.91508434f,65.91508434f,
			66.40724412f,66.40724412f,66.56266449f,66.56266449f,66.79579221f,66.76988976f,
			67.00301748f,67.00301748f,67.13253351f,67.13253351f,67.02892182f,67.02892182f,
			66.69218052f,66.69218052f,66.58856694f,66.847599f,66.76988976f,66.95121069f,
			66.95121069f,66.89940579f,66.847599f,66.58856694f,66.38134167f,66.04460037f,
			65.94098679f,65.21569929f,64.8271512f,63.76512051f,63.63560448f,63.66150882f,
			63.29886318f,63.24705828f,62.93621943f,62.54767134f,62.39225286f,61.84828629f,
			61.64106102f,61.64106102f,61.90009308f,61.90009308f,61.64106102f,61.69286781f};
	
	static {
		assert( FIRE_CITY_X_POINTS.length == FIRE_CITY_Y_POINTS.length );
		Builder pBuilder = Polygon.Builder();
		for( int i = 0; i < FIRE_CITY_X_POINTS.length; ++i ){
			pBuilder.addVertex( new Point(FIRE_CITY_X_POINTS[i], FIRE_CITY_Y_POINTS[i]) );
		}
		FIRE_CITY_REGION = pBuilder.build();
	}
	
	private static final ArrayList<String> CAUSE_CODES =  new ArrayList<String>( Arrays.asList(new String[]{"AFA","All","Code1Medical","Code3Med",
			"Code3Trauma","EMS","Fire","Hazmat","MassCasualty","MVA","NuisanceFire",
			"Other","OtherEMS","OtherFire","Overpressure","Rescue","StructureFire",
			"Transport","VehicleFire","WildlandFire"}) );
//	region : {Full,GoodSam-T,Stonybrook,Westhills,EMS-City,EMS-County,Fire-City,Fire-Rural};
	private static final ArrayList<String> REGIONS = new ArrayList<String>( Arrays.asList(new String[]{
			"Full","GoodSam-T","Stonybrook","Westhills","EMS-City","EMS-County","Fire-City","Fire-Rural"
	}));

	private static final OBJECT_VAL CODE1MEDICAL_CAUSE_CODE = new OBJECT_VAL("Code1Medical");
	private static final OBJECT_VAL CODE3MED_CAUSE_CODE = new OBJECT_VAL("Code3Med");
	private static final OBJECT_VAL TRANSPORT_CAUSE_CODE = new OBJECT_VAL("Transport");
	private static final OBJECT_VAL OTHER_CAUSE_CODE = new OBJECT_VAL("Other");
	private static final OBJECT_VAL HAZMAT_CAUSE_CODE = new OBJECT_VAL("Hazmat");
	private static final OBJECT_VAL AFA_CAUSE_CODE = new OBJECT_VAL("AFA");
	private static final OBJECT_VAL OTHERFIRE_CAUSE_CODE = new OBJECT_VAL("OtherFire");
	private static final OBJECT_VAL STRUCTUREFIRE_CAUSE_CODE = new OBJECT_VAL("StructureFire");
	private static final OBJECT_VAL MVA_CAUSE_CODE = new OBJECT_VAL("MVA");
	private static final OBJECT_VAL CODE3TRAUMA_CAUSE_CODE = new OBJECT_VAL("Code3Trauma");
	private static final OBJECT_VAL EMS_CAUSE_CODE = new OBJECT_VAL("EMS");
	private static final OBJECT_VAL MASSCASUALTY_CAUSE_CODE = new OBJECT_VAL("MassCasualty");
	private static final OBJECT_VAL OTHEREMS_CAUSE_CODE = new OBJECT_VAL("OtherEMS");
	private static final OBJECT_VAL RESCUE_CAUSE_CODE = new OBJECT_VAL("Rescue");
	
	private static final OBJECT_VAL EMS_CITY_REGION_CODE = new OBJECT_VAL("EMS-City");
	private static final OBJECT_VAL WESTHILLS_REGION_CODE= new OBJECT_VAL("Westhills");
	private static final OBJECT_VAL STONYBROOK_REGION_CODE = new OBJECT_VAL("Stonybrook");
	private static final OBJECT_VAL GOODSAM_REGION_CODE = new OBJECT_VAL("GoodSam-T");
	private static final OBJECT_VAL FIRE_RURAL_REGION_CODE = new OBJECT_VAL("Fire-Rural");
	private static final OBJECT_VAL FIRE_CITY_REGION_CODE = new OBJECT_VAL("Fire-City");
	private static final OBJECT_VAL EMS_COUNTY_REGION_CODE = new OBJECT_VAL("EMS-County");
	private static final OBJECT_VAL FIRE_CAUSE_CODE = new OBJECT_VAL("Fire");
	private static final OBJECT_VAL VEHICLE_FIRE_CAUSE_CODE = new OBJECT_VAL("VehicleFire");
	private static final OBJECT_VAL WILDLAND_FIRE_CAUSE_CODE = new OBJECT_VAL("WildlandFire");
	private static final OBJECT_VAL NUISANCE_FIRE_CAUSE_CODE = new OBJECT_VAL("NuisanceFire");
//	private static final double CALL_RADIUS = 0.2;
	
//	private static DecimalFormat _df = new DecimalFormat("##.########");
	private static HashMap<String,OBJECT_VAL> _natureCodeMap = new HashMap<>();
	
	static{
		_natureCodeMap.put("2ND", STRUCTUREFIRE_CAUSE_CODE );
		_natureCodeMap.put("ACCBIK", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("ACCFTL", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("ACCINJ", MVA_CAUSE_CODE );
		_natureCodeMap.put("ACCNON", MVA_CAUSE_CODE );
		_natureCodeMap.put("ACCPED", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("ACCUNK", MVA_CAUSE_CODE );
		_natureCodeMap.put("ALLERG", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("APT", STRUCTUREFIRE_CAUSE_CODE );
		_natureCodeMap.put("ASLT3", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("ATTSUI", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("BACK1", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("BACK3", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("BLEED1", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("BLEED3", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("BREATH", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("BURNCO", OTHERFIRE_CAUSE_CODE );
		_natureCodeMap.put("CFA", AFA_CAUSE_CODE );
		_natureCodeMap.put("CHOKE", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("CODE", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("DEATH", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("DIABET", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("EXPOSE", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("FALL1", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("FALL3", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("FIGHT", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("FLUE", STRUCTUREFIRE_CAUSE_CODE );
		_natureCodeMap.put("HEART", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("LIFE3", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("LIFT", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("LINES", OTHERFIRE_CAUSE_CODE );
		_natureCodeMap.put("MANDWN", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("MED1", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("MED3", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("OD", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("ODOR", OTHERFIRE_CAUSE_CODE );
		_natureCodeMap.put("PUBLIC", OTHER_CAUSE_CODE );
		_natureCodeMap.put("RFA", AFA_CAUSE_CODE );
		_natureCodeMap.put("SEIZE", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("SMOKE", OTHERFIRE_CAUSE_CODE );
		_natureCodeMap.put("SPILL", HAZMAT_CAUSE_CODE );
		_natureCodeMap.put("STROKE", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("TRANS", TRANSPORT_CAUSE_CODE );
		_natureCodeMap.put("TRAUM1", CODE1MEDICAL_CAUSE_CODE );
		_natureCodeMap.put("TRAUM3", CODE3TRAUMA_CAUSE_CODE );
		_natureCodeMap.put("UNCON", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("UNKMED", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put("UNKMED", CODE3MED_CAUSE_CODE );
		_natureCodeMap.put(CODE3MED_CAUSE_CODE._sConstValue, CODE3MED_CAUSE_CODE);
		_natureCodeMap.put(CODE1MEDICAL_CAUSE_CODE._sConstValue, CODE1MEDICAL_CAUSE_CODE);
		_natureCodeMap.put(TRANSPORT_CAUSE_CODE._sConstValue, TRANSPORT_CAUSE_CODE);
		_natureCodeMap.put(HAZMAT_CAUSE_CODE._sConstValue, HAZMAT_CAUSE_CODE);
		_natureCodeMap.put(OTHERFIRE_CAUSE_CODE._sConstValue, OTHERFIRE_CAUSE_CODE);
		_natureCodeMap.put(AFA_CAUSE_CODE._sConstValue, AFA_CAUSE_CODE);
		_natureCodeMap.put(OTHER_CAUSE_CODE._sConstValue, OTHER_CAUSE_CODE);
		_natureCodeMap.put(STRUCTUREFIRE_CAUSE_CODE._sConstValue, STRUCTUREFIRE_CAUSE_CODE);
		_natureCodeMap.put(CODE3TRAUMA_CAUSE_CODE._sConstValue, CODE3TRAUMA_CAUSE_CODE);
		_natureCodeMap.put(MVA_CAUSE_CODE._sConstValue, MVA_CAUSE_CODE);
	}
	
	protected String callId;
	protected String natureCode;
	protected LocalDate callDate;
	protected LocalTime callTime;
	protected String callAddress;
	protected double callX;
	protected double callY;
		
	protected EmergencyDomainDataReelElement(String callId, String natureCode,
			LocalDate callDate, LocalTime callTime, String callAddress,
			double callX, double callY, final boolean convertToMiles) {
		this.callId = callId;
		this.natureCode = natureCode;
		this.callDate= LocalDate.of( callDate.getYear(), callDate.getMonth(), callDate.getDayOfMonth() );
		this.callTime = callTime;
		this.callAddress = callAddress;
		this.callX = (convertToMiles) ? FeetToMiles*callX : callX;
		this.callY = (convertToMiles) ? FeetToMiles*callY : callY;
	}
		
	protected EmergencyDomainDataReelElement(final String[] splits, final boolean convertToMiles){
		this( splits[0], splits[1], LocalDate.parse(splits[2]), LocalTime.parse(splits[3]), 
				splits[4], Double.parseDouble(splits[5]), Double.parseDouble(splits[6]) , convertToMiles);
	}
		
	public EmergencyDomainDataReelElement(String line, String separator, boolean convertToMiles){
		this( line.split(separator) , convertToMiles);
	}

	public DataReelElement slurp(String line, String separator, boolean convertToMiles) {
		return new EmergencyDomainDataReelElement(line, separator, convertToMiles);
	}
	
	@Override
	public int compareTo(DataReelElement other) {
		if( other instanceof EmergencyDomainDataReelElement ){
			EmergencyDomainDataReelElement other_elem = (EmergencyDomainDataReelElement)other;
//			if( Math.abs( other_elem.callX - this.callX ) < CALL_RADIUS && 
//					Math.abs( other_elem.callY - this.callY ) < CALL_RADIUS ){
//				return 0;
//			}
			final int timeCompare = this.callTime.compareTo(other_elem.callTime);
			return timeCompare;
		}
		return -1;
	}

	@Override
	public String toString() {
		return "EmergencyDomainDataReelElement [callId=" + callId + ", natureCode=" + natureCode + ", callDate="
				+ callDate + ", callTime=" + callTime + ", callAddress=" + callAddress + ", callX=" + callX + ", callY="
				+ callY + "]";
	}
	
	//interfacing with RDDL state. This type is a partial state. 
	public EmergencyDomainDataReelElement( State s ) throws EvalException{
		this("XXX", getCurrentCauseCode(s), LocalDate.ofYearDay(dataYear, getCurrentCallDate(s)),getCurrentCallTime(s),"AAA",
				getCurrentCallX(s),getCurrentCallY(s), false );
	}

//	private static LocalDate getCurrentCallDay(State s) throws EvalException {
//		int rddl_date = ((int)s.getPVariableAssign(currentCallDayPvarName , emptySubstitution));
//		return LocalDate.ofYearDay(dataYear, rddl_date);
//	}

	protected static String getCurrentCauseCode(State s) throws EvalException {
		ArrayList<ArrayList<LCONST>> subs = s.generateAtoms( currentCallCodePvarName );
		ArrayList<LCONST> ret = null;
		for( ArrayList<LCONST> assign : subs ){
			if( ((boolean)s.getPVariableAssign(currentCallCodePvarName, assign)) ){
				ret  = assign;
				break;
			}
		}
		return ret.get(0)._sConstValue;
	}

	private static int getCurrentCallDate(State s) throws EvalException {
		return 1+(int)Math.floor( ((double)s.getPVariableAssign(currentCallTimePvarName, emptySubstitution ))/24.0);
	}

	protected static double getCurrentCallY(State s) throws EvalException {
		return (double)s.getPVariableAssign(currentCallPvarName, yPosSubstitution );
	}

	protected static double getCurrentCallX(State s) throws EvalException {
		return (double)s.getPVariableAssign(currentCallPvarName, xPosSubstitution );
	}

	protected static LocalTime getCurrentCallTime(State s) throws EvalException {
		return doubleToTime(((double)s.getPVariableAssign(currentCallTimePvarName, emptySubstitution )));
	}
	
	public EmergencyDomainDataReelElement( HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs ){
		this("XXX", getCurrentCauseCode(subs), LocalDate.ofYearDay(dataYear, getCurrentCallDate(subs)),
				getCurrentCallTime(subs),"AAA",getCurrentCallX(subs),getCurrentCallY(subs), false );
	}

	private static String getCurrentCauseCode(HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs) {
		HashMap<ArrayList<LCONST>, Object> codes_bools = subs.get(currentCallCodePvarName);
		ArrayList<LCONST> ret = null;
		for( Entry<ArrayList<LCONST>, Object> entry : codes_bools.entrySet() ){
			if( ((boolean)entry.getValue()) ){
				ret = entry.getKey();
				break;
			}
		}
		return ret.get(0)._sConstValue;
		
	}

	private static int getCurrentCallDate(HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs) {
		return 1+(int)Math.floor( ((double)subs.get(currentCallTimePvarName).get( emptySubstitution ))/24.0);
	}

	public EmergencyDomainDataReelElement(EmergencyDomainDataReelElement other) {
		this(other.callId,other.natureCode,other.callDate,other.callTime,other.callAddress,other.callX,other.callY,false);
	}

	protected static LocalTime getCurrentCallTime(HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs) {
		return doubleToTime((double)subs.get(currentCallTimePvarName).get( emptySubstitution ));
	}
//
//	protected static LocalDate getCurrentCallDay(HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs) {
//		return LocalDate.ofYearDay(dataYear, ((int)subs.get(currentCallDayPvarName).get( emptySubstitution )));
//	}
	
	protected static double getCurrentCallY(HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs) {
		return ((Number)(subs.get( currentCallPvarName ).get(yPosSubstitution ))).doubleValue();
	}

	protected static double getCurrentCallX(HashMap<PVAR_NAME, HashMap<ArrayList<LCONST>, Object>> subs) {
		return ((Number)(subs.get( currentCallPvarName ).get(xPosSubstitution ))).doubleValue();
	}
	
	public static double timeToDouble( LocalTime time, LocalDate date ){
		double ret_time = time.getHour()+(time.getMinute()/60.0)+(time.getSecond()/(60*60.0));
		double ret_date = (date.getDayOfYear()-1)*24;
		double ret = ret_date + ret_time; //Double.valueOf( _df .format( ret_date + ret_time ) );
		assert( ret >= 0 );//&& ret < 24);
		return ret;
	}
	
	public static LocalTime doubleToTime( final double t ){
		assert( t >= 0 );
		int numdays = (int) Math.floor(t/24.0);
		double time_of_day = t - 24*numdays;
		double val_t = time_of_day;//Double.valueOf( _df.format(time_of_day) );
		
		//t = h + m/60 + s/3600, 0<h<24, 0<m<60, 0<s<60 are ints
		//15.2345 = 15h, 0.2345*60=14.07=>14m, 14.07-14=0.07*60=4s
		//h = floor(t)
		final int h = (int)Math.floor( val_t );
		//m = floor( (t-h)*60 = m+s/60 )
		final int m = (int)Math.floor( (val_t-h)*60 );
		//s = floor( (t-h-m/60)*3600 = s )
		final int s = (int)Math.floor( (val_t-h-m/60d)*3600 );
		
		return LocalTime.of(h, m, s);
	}
	
	protected ArrayList<Pair<EXPR,EXPR>> to_RDDL_EXPR_constraints(
			final int future, final int time_step, 
			final LVAR future_PREDICATE, final ArrayList<LCONST> future_indices,
			final LVAR TIME_PREDICATE, final ArrayList<LCONST> time_indices, 
			Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants, 
			Map<TYPE_NAME, OBJECTS_DEF> objects, double prev_call_time ){
		ArrayList<Pair<EXPR,EXPR>> ret = new ArrayList<>();
		
		double current_call_time_double = timeToDouble( this.callTime, this.callDate );
		final double the_gap = current_call_time_double-prev_call_time;
		assert( the_gap >= 0d );
		
		EXPR lhs_callT = addStepFuture(currentCallTimePvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
				.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step) ), constants, objects )
				.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
		ret.add( new Pair<EXPR,EXPR>( lhs_callT, new REAL_CONST_EXPR( current_call_time_double ) ) );

		EXPR lhs_callX = addStepFuture(currentCallPvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
						.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step) ), constants, objects )
						.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects )
						.substitute( Collections.singletonMap(new LVAR("?c"),  new OBJECT_VAL("xpos") ), constants, objects );
		ret.add( new Pair<EXPR,EXPR>( lhs_callX, new REAL_CONST_EXPR( this.callX ) ) );
		
		
		EXPR lhs_callY = addStepFuture(currentCallPvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
				.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step) ), constants, objects )
				.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects )
				.substitute( Collections.singletonMap(new LVAR("?c"),  new OBJECT_VAL("ypos") ), constants, objects );
		ret.add( new Pair<EXPR,EXPR>( lhs_callY, new REAL_CONST_EXPR( this.callY ) ) );
		
		OBJECT_VAL closest_cause = getClosestParentCause( this.natureCode );
		int cause_idx = CAUSE_CODES.indexOf( closest_cause._sConstValue );
		
		for( String cause_type : CAUSE_CODES ){ 
			EXPR other_callCode = addStepFuture(currentCallCodePvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
					.substitute( Collections.singletonMap( new LVAR("?c"), new OBJECT_VAL(cause_type) ), constants, objects)
					.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step) ), constants, objects )
					.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
			if( cause_type.equals(closest_cause._sConstValue) ){
				ret.add( new Pair<EXPR,EXPR>( other_callCode, new BOOL_CONST_EXPR( true ) ) );
			}else{
				ret.add( new Pair<EXPR,EXPR>( other_callCode, new BOOL_CONST_EXPR( false ) ) );				
			}
		}
		
		final OBJECT_VAL matched_region = getRegion( this.callX, this.callY, this.natureCode );
		final int region_idx = REGIONS.indexOf(matched_region._sConstValue);
				
		for( final String region_str : REGIONS ){
			EXPR other_region = addStepFuture(currentCallRegionPvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
					.substitute( Collections.singletonMap( new LVAR("?r"), new OBJECT_VAL(region_str) ), constants, objects)
					.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step) ), constants, objects )
					.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
			
			if( region_str.equals(matched_region._sConstValue) ){
				ret.add( new Pair<EXPR,EXPR>( other_region, new BOOL_CONST_EXPR( true ) ) );
			}else{
				ret.add( new Pair<EXPR,EXPR>( other_region, new BOOL_CONST_EXPR( false ) ) );				
			}
		}
		
		if( time_step != 0 ){ //time_indices.size()-1 ){
			EXPR lhs_time = addStepFuture(gapTimePvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
					.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step-1) ), constants, objects )
					.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
			ret.add( new Pair<EXPR, EXPR>(lhs_time, new REAL_CONST_EXPR(the_gap) ) );
		
			EXPR lhs_tempuniform_cause = addStepFuture(tempUniformCausePvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
				.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step-1) ), constants, objects )
				.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
			ret.add( new Pair<EXPR,EXPR>( lhs_tempuniform_cause, new REAL_CONST_EXPR(1.0*cause_idx) ) );

			EXPR lhs_tempuniform_region = addStepFuture(tempUniformRegionPvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
					.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step-1) ), constants, objects )
					.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
			ret.add( new Pair<EXPR,EXPR>(lhs_tempuniform_region, new REAL_CONST_EXPR(1.0*region_idx) ) );
		}
		
//		int rhs_callDay = this.callDate.getDayOfYear();
//		EXPR lhs_callDay = addStepFuture(currentCallDayPvar, future_PREDICATE, TIME_PREDICATE, constants, objects)
//				.substitute( Collections.singletonMap( TIME_PREDICATE, time_indices.get(time_step) ), constants, objects )
//				.substitute( Collections.singletonMap( future_PREDICATE, future_indices.get(future) ), constants, objects );
//		ret.add( new Pair<EXPR,EXPR>( lhs_callDay, new INT_CONST_EXPR( rhs_callDay ) ) );
		
//		System.out.println( ret );
		return ret;
	}

	protected static OBJECT_VAL getRegion(double callX, double callY, String natureCode) {
		//region : {Full,GoodSam-T,Stonybrook,Westhills,EMS-City,EMS-County,Fire-City,Fire-Rural};
		//need centers of circular regions
		if( (Math.pow( callX - GoodSamCenterX, 2) + Math.pow( callY - GoodSamCenterY, 2) <=  GoodSamRadius*GoodSamRadius)
				&& getClosestParentCause(natureCode).equals(TRANSPORT_CAUSE_CODE ) ){
			return GOODSAM_REGION_CODE;
		}else if( (Math.pow( callX - StonybrookCenterX, 2) + Math.pow( callY - StonybrookCenterY, 2) <=  StonybrookRadius*StonybrookRadius)
				&& getClosestParentCause(natureCode).equals(CODE3MED_CAUSE_CODE ) ){
			return STONYBROOK_REGION_CODE;
		}else if( (Math.pow( callX - WestHillsCenterX, 2) + Math.pow( callY - WestHillsCenterY, 2) <=  WestHillsRadius*WestHillsRadius)
				&& getClosestParentCause(natureCode).equals(CODE3MED_CAUSE_CODE ) ){
			return WESTHILLS_REGION_CODE;
		}else if( EMS_CITY_REGION.contains( new Point((float)callX, (float)callY) ) && isEMS(natureCode) ){
			return EMS_CITY_REGION_CODE;
		}else if( isEMS(natureCode) ){
			return EMS_COUNTY_REGION_CODE;
		}else if( FIRE_CITY_REGION.contains( new Point((float)callX, (float)callY) ) && isFire(natureCode) ){
			return FIRE_CITY_REGION_CODE;
		}else if( isFire(natureCode) ){
			return FIRE_RURAL_REGION_CODE;
		}
		//need points of polygon
		
		return new OBJECT_VAL("Full");
	}


	private static boolean isFire(String natureCode) {
		OBJECT_VAL nearest_code = getClosestParentCause(natureCode);
		return ( nearest_code.equals(AFA_CAUSE_CODE) || nearest_code.equals(FIRE_CAUSE_CODE) 
				|| nearest_code.equals(NUISANCE_FIRE_CAUSE_CODE) || nearest_code.equals(OTHERFIRE_CAUSE_CODE) 
				|| nearest_code.equals(STRUCTUREFIRE_CAUSE_CODE) || nearest_code.equals(VEHICLE_FIRE_CAUSE_CODE) 
				|| nearest_code.equals(WILDLAND_FIRE_CAUSE_CODE) );
	}

	private static boolean isEMS(String natureCode) {
		OBJECT_VAL nearest_code = getClosestParentCause(natureCode);
//		cause : {AFA,All,Code1Medical,Code3Med,Code3Trauma,EMS,Fire,Hazmat,MassCasualty,
//			 MVA,NuisanceFire,Other,OtherEMS,OtherFire,
//			 Overpressure,Rescue,StructureFire,Transport,VehicleFire,WildlandFire};
		return ( nearest_code.equals( CODE3MED_CAUSE_CODE ) || nearest_code.equals( CODE1MEDICAL_CAUSE_CODE ) 
				|| nearest_code.equals( CODE3TRAUMA_CAUSE_CODE ) || nearest_code.equals( EMS_CAUSE_CODE )  
				|| nearest_code.equals( MASSCASUALTY_CAUSE_CODE ) || nearest_code.equals( MVA_CAUSE_CODE )
				|| nearest_code.equals( OTHEREMS_CAUSE_CODE ) || nearest_code.equals( RESCUE_CAUSE_CODE )
				|| nearest_code.equals( TRANSPORT_CAUSE_CODE ) );
	}

	public static OBJECT_VAL getClosestParentCause(String natureCode) {
		if( _natureCodeMap.containsKey(natureCode) ){
			return _natureCodeMap.get(natureCode);
		}
		return new OBJECT_VAL("All");
	}

	protected EXPR addStepFuture(final EXPR expression, final LVAR future_predicate, final LVAR time_predicate,
			Map<PVAR_NAME, Map<ArrayList<LCONST>, Object>> constants, Map<TYPE_NAME, OBJECTS_DEF> objects) {
		return expression.addTerm(time_predicate, constants, objects)
					.addTerm(future_predicate, constants, objects);
	}
	
	public void setInState( final State s ) throws EvalException{
		s.clearIntermFluents();
		s.setPVariableAssign( currentCallTimePvarName, emptySubstitution, timeToDouble(this.callTime, this.callDate) );
		s.setPVariableAssign( currentCallPvarName, xPosSubstitution, this.callX );
		s.setPVariableAssign( currentCallPvarName, yPosSubstitution, this.callY );
		
		clearAssignments(s,currentCallCodePvarName);
		s.setPVariableAssign(currentCallCodePvarName, new ArrayList<LCONST>( 
				Collections.singletonList( getClosestParentCause(this.natureCode) ) ), true );
		
		clearAssignments(s,currentCallRegionPvarName);
		s.setPVariableAssign( currentCallRegionPvarName, 
				new ArrayList<LCONST>( Collections.singletonList( 
						getRegion(this.callX, this.callY, this.natureCode) ) ), true );
		
//		s.setPVariableAssign( currentCallDayPvarName, emptySubstitution, this.callDate.getDayOfYear() );
	}

	private static void clearAssignments(State s, PVAR_NAME pvar) throws EvalException {
		ArrayList<ArrayList<LCONST>> subs = s.generateAtoms(pvar);
		for( ArrayList<LCONST> assign : subs ){
			s.setPVariableAssign(pvar, assign, null);
		}
	}
	
}