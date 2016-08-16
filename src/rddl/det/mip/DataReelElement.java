package rddl.det.mip;

public abstract class DataReelElement {
	public DataReelElement(){}
	public DataReelElement(String line, String separator){}
	public DataReelElement slurp(String line, String separator) {
		return null;
	}
	public abstract int compareTo( DataReelElement other);
}
