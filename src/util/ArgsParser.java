package util;

public class ArgsParser {
	
	public static int getOptionPos(String flag, String[] args) {
		if (args == null)
			return -1;
			
		for (int i = 0; i < args.length; i++) {
			if ((args[i].length() > 0) && (args[i].charAt(0) == '-')) {
			// Check if it is a negative number
				try {
					Double.valueOf(args[i]);
				} 
				catch (NumberFormatException e) {
					// found?
					if (args[i].equals("-" + flag))
						return i;
				}
			  }
			}
		return -1;
	}
	
	  public static String getOption(String flag, String[] args) throws Exception {		
		  String newString;
		  int i = getOptionPos(flag, args);
		
		  if (i > -1) {
			  if (args[i].equals("-" + flag)) {
				  if (i + 1 == args.length) {
					  throw new Exception("Missing value for -" + flag + " option.");
				  }
				  args[i] = "";
				  newString = new String(args[i + 1]);
				  args[i + 1] = "";
				  return newString;
			  }
			  if (args[i].charAt(1) == '-') {
				  return "";
			  }
		  }		
		  return "";
	  }	
}
