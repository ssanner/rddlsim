package rddl.translate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import rddl.RDDL;
import rddl.parser.parser;

public class RDDL2Prefix {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		RDDL.USE_PREFIX = true;
		
		if (args.length != 2) {
			System.out.println("\nusage: RDDL-file/directory output-dir\n");
			System.exit(1);
		}
		String rddl_file = args[0];
		String output_dir = args[1];
				
		if (output_dir.endsWith(File.separator))
			output_dir = output_dir.substring(output_dir.length() - 1);
		
		// If RDDL file is a directory, add all files
		ArrayList<File> rddl_files = new ArrayList<File>();
		ArrayList<File> rddl_error = new ArrayList<File>();
		File file = new File(rddl_file);
		if (file.isDirectory())
			rddl_files.addAll(Arrays.asList(file.listFiles()));
		else
			rddl_files.add(file);
		
		for (File f : (ArrayList<File>)rddl_files.clone()) {
						
			try {
				RDDL rddl = parser.parse(f);
				
				int rddl_index = f.getName().indexOf(".rddl");
				File output_file = new File(output_dir + File.separator + 
						f.getName().substring(0, rddl_index) + ".rddl_prefix");
				System.out.println("Writing to " + output_file);
				
				PrintStream ps = new PrintStream(new FileOutputStream(output_file));
				ps.print(rddl.toString());
				ps.close();
			
			} catch (Exception e) {
				System.err.println("Error processing: " + f);
				System.err.println(e);
				e.printStackTrace(System.err);
				System.err.flush();
				rddl_files.remove(f);
				rddl_error.add(f);
			}
		}
		
		System.out.println("\n\n===\n");
		for (File f : rddl_files)
			System.out.println("Processed: " + f);
		for (File f : rddl_error)
			System.out.println("Error processing: " + f);
	}

}
