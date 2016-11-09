package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Wuga
 * This Class is used to adjust Graphviz *.dot file
 * The att.grappa package is too old that cannot parse .dot file with numeric values
 *
 * We try to fix the dot file by replacing numeric values with strings.
 */
public class DotFileFixer {
	
	public static final String VIEWER_FILE = "tmp_rddl_graphviz.dot";
	
	public static void fixFile(){
		try (BufferedReader br = Files.newBufferedReader(Paths.get(VIEWER_FILE))) {
		    String line;
		    String input = "";
		    String pattern = "=([0-9]*[.])?[0-9]+";
		    Pattern p = Pattern.compile(pattern);
		    while ((line = br.readLine()) != null) {
			    	Matcher m = p.matcher(line);
			    	if(m.find()){
			    	StringBuilder value = new StringBuilder(line.substring(m.start(), m.end()));
			    	value.insert(1, '"');
			    	value.append('"');
			    	line=line.replaceAll(pattern, value.toString());
		    	}
		    	input += line + System.lineSeparator();
		    }
		    File file = new File(VIEWER_FILE);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(input);
			fileWriter.flush();
			fileWriter.close();
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DotFileFixer.fixFile();
	}

}
