/**
 * Utilities: Automatically chooses executables based on operating system.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 9/1/03
 *
 **/

package util;

public class WinUNIX {

	public static final int UNDEFINED   = 0;
	public static final int LINUX_LIKE  = 1;
	public static final int WINDOWS     = 2;
	
	public static int SYSTEM = UNDEFINED;
	
	public static String GVIZ_EXE = null;
	public static String GVIZ_CMD = null;
	public static String GVIZ_CMD_CLOSE = null;
	public static String GVIZ2_EXE = null;
	public static String GVIZ2_CMD = null;
	public static String GVIZ2_CMD_CLOSE = null;
	
	public static String USER_DIR = System.getProperty("user.dir");
	public static String FILE_SEP = System.getProperty("file.separator");
	public static String OS_NAME  = System.getProperty("os.name");

	static {
		
		if (OS_NAME.toLowerCase().startsWith("windows"))
			SYSTEM = WINDOWS;
		else
			SYSTEM = LINUX_LIKE;
		
		if (SYSTEM == WINDOWS) {
			
			GVIZ_EXE = "dot.exe -Tdot";
			GVIZ_CMD = "CMD /C dot.exe -Tdot";
			GVIZ_CMD_CLOSE = "";
			GVIZ2_EXE = "neato.exe -Tdot";
			GVIZ2_CMD = "CMD /C neato.exe -Tdot";
			GVIZ2_CMD_CLOSE = "";

		} else if (SYSTEM == LINUX_LIKE) {
			
			GVIZ_EXE = "dot -Tdot";
			GVIZ_CMD = "/bin/sh 'dot -Tdot";
			GVIZ_CMD_CLOSE = "'";
			GVIZ2_EXE = "neato -Tdot";
			GVIZ2_CMD = "/bin/sh 'neato -Tdot";
			GVIZ2_CMD_CLOSE = "'";

		} else {
			System.out.println("util.WinUNIX: Unrecognized OS.");
			System.exit(1);
		}
		
	}

}
