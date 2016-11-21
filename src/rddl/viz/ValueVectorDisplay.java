package rddl.viz;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVARIABLE_INTERM_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class ValueVectorDisplay extends StateViz {

	public boolean _bSuppressNonFluents = false;
	public boolean _bSuppressIntermFluents = false;
	public boolean _bSuppressActionFluents = false;
	public boolean _bSuppressWriteFile = false;
	public String _sDataPath = null;
	public String _sLabelPath = null;
	public PrintWriter _pDataOut = null;
	public PrintWriter _pLabelOut = null;
	
	public ValueVectorDisplay() {
		_bSuppressNonFluents = true;
	}

	public ValueVectorDisplay(boolean suppress_nonfluents,boolean suppress_intermfluents) {
		_bSuppressNonFluents = suppress_nonfluents;
		_bSuppressIntermFluents = suppress_intermfluents;
	}
	
	public void stateOnly(){
		_bSuppressActionFluents = true;
		_bSuppressNonFluents = true;
		_bSuppressIntermFluents = true;		
	}
	
	public void stateAction(){
		_bSuppressActionFluents = false;
//		_bSuppressNonFluents = false;
		_bSuppressNonFluents = true;
		_bSuppressIntermFluents = true;
	}
	
	public void writeFile(String data_path, String label_path){
		_bSuppressWriteFile = true;
		_sDataPath = data_path;
		_sLabelPath = label_path;		
		_pDataOut = getFileHandler(data_path);
		_pLabelOut = getFileHandler(label_path);
	}
	
	public void close(){
		if(_pDataOut!=null&&_pLabelOut!=null){
			_pDataOut.close();
			_pLabelOut.close();
		}
	}
	
	private PrintWriter getFileHandler(String path){
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter out = null;
		try {
		    fw = new FileWriter(path);
		    bw = new BufferedWriter(fw);
		    out = new PrintWriter(bw);
		} catch (IOException e) {
		    System.out.println("There is no existing directory for file:"+path);
		}		
		return out;
	}
	

	@Override
	public void display(State s, int time) {
		// TODO Auto-generated method stub
		String vector = getStateDescription(s);
		//System.out.println(vector);
		if(_bSuppressWriteFile == true){
			if(_bSuppressActionFluents == true){
				_pLabelOut.println(vector);
			}else{
				_pDataOut.println(vector);
			}
		}
	}
	
	public String getStateDescription(State s) {
		StringBuilder sb = new StringBuilder("");
		
		// Go through all variable types (state, interm, observ, action, nonfluent)
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : s._hmTypeMap.entrySet()) {
			
			if (_bSuppressNonFluents && e.getKey().equals("nonfluent"))
				continue;
			
			if (_bSuppressIntermFluents && e.getKey().equals("interm"))
				continue;
			
			if (_bSuppressActionFluents && e.getKey().equals("action"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) {
				try {
					PVARIABLE_DEF pvar_def = s._hmPVariables.get(p);
					// Go through all term groundings for variable p
					ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);
					if(pvar_def._typeRange.equals(TYPE_NAME.BOOL_TYPE)){
						for (int i=0; i<gfluents.size();i++)
							sb.append(((boolean)s.getPVariableAssign(p, gfluents.get(i))?1:0)+((i+1)==gfluents.size()? "" : ","));
					}else{										
						for (int i=0; i<gfluents.size();i++)
							sb.append(s.getPVariableAssign(p, gfluents.get(i))+((i+1)==gfluents.size()? "" : ","));
					}
					sb.append(",");
						
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment " + s + " for " + p + "\n");
				}
			}
		}
		String output = sb.toString();		
		return output.substring(0, output.length()-1);
	}
	
	
	public void initFileWriting(State s){
		String variable = getGroundNames(s);
		if(_bSuppressWriteFile == true){
			if(_bSuppressActionFluents == true){
				_pLabelOut.println(variable);
			}else{
				_pDataOut.println(variable);
			}
		}
	}
	/**
	 * @param s
	 * @return String
	 * 
	 * For each output files, we need to build several lines to show the variable name and terms of each data columns.
	 * Since the list could be quite big, we probably need to write it in multiple lines.
	 */
	public String getGroundNames(State s){
		StringBuilder sb = new StringBuilder("");
		
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : s._hmTypeMap.entrySet()) {			
			if (_bSuppressNonFluents && e.getKey().equals("nonfluent"))
				continue;
			if (_bSuppressIntermFluents && e.getKey().equals("interm"))
				continue;
			if (_bSuppressActionFluents && e.getKey().equals("action"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) {

				String var_type = e.getKey();
				try {
					// Go through all term groundings for variable p
					ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);										
					for (ArrayList<LCONST> gfluent : gfluents){
						String terms = gfluent.toString();	
						terms =terms.replace(',', '|');
						sb.append(var_type + ": " + p + (gfluent.size() > 0 ? terms : "") + ",");
					}
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment " + s + " for " + p + "\n");
				}
			}
		}
		String output = sb.toString();		
		return output.substring(0, output.length()-1);
	}

}
