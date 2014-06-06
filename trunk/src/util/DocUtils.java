/** Frequently used document utilities (reading files).
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package util;

import java.io.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DocUtils {
	
	public static final String SPLIT_TOKENS = "[!\"#$%&'()*+,./:;<=>?\\[\\]^`{|}~\\s]"; // missing: [_-@]
		
	public final static DecimalFormat DF2 = new DecimalFormat("#.##");
	public final static DecimalFormat DF3 = new DecimalFormat("#.###");

	public static String ReadFile(File f) {
		return ReadFile(f, false);
	}
	
	public static String ReadFile(File f, boolean keep_newline) {
		try {
			StringBuilder sb = new StringBuilder();
			java.io.BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				sb.append((sb.length()> 0 ? (keep_newline ? "\n" : " ") : "") + line);
			}
			br.close();
			return sb.toString();
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			return null;
		}
	}
}
