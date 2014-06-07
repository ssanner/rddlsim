/**
 * RDDL: Defines methods to statically validate an RDDL specification.
 *       Currently unimplemented so relying on runtime EvalExceptions to
 *       detect errors.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.validate;

import java.io.File;

import rddl.RDDL;
import rddl.parser.parser;
import rddl.policy.NoopPolicy;
import rddl.policy.Policy;
import rddl.sim.Result;
import rddl.sim.Simulator;
import rddl.viz.GenericScreenDisplay;
import rddl.viz.NullScreenDisplay;
import rddl.viz.StateViz;

public class DynamicValidator {

	// Note when objects are referenced but not declared in object lists
	
	// Validates a domain and arbitrary first instance by simulating one iteration with a NOOP -- undefined
	// references will be caught although it should be pointed out that a dynamic simulation does not 
	// necessarily evaluate all conditional paths or states so this is more of a reality check.
	public static void main(String[] args) throws Exception {
		
		if (args.length < 1 || args.length > 2) {
			System.out.println("usage: RDDL-file-or-dir [instance-name]");
			System.exit(1);
		}
		String rddl_file = args[0];
		int rand_seed_sim = 123456;
		int rand_seed_policy = 123456;
		
		// Load RDDL files
		RDDL rddl = new RDDL(rddl_file);

		// Instantiate simulator, policy and state visualization
		String instance_name = args.length > 1 ? args[1] : rddl._tmInstanceNodes.firstKey();;
		Simulator sim = new Simulator(rddl, instance_name);
		Policy pol = new NoopPolicy();
		pol.setRandSeed(rand_seed_policy);
		pol.setRDDL(rddl);
		//StateViz viz = new NullScreenDisplay();
		StateViz viz = new GenericScreenDisplay();
			
		// Attempt to simulate one step... this will throw an exception if anything is undefined
		sim._i._nHorizon = 2;
		Result r = sim.run(pol, viz, rand_seed_sim);

		// Show all variable names in use
		if (RDDL.DEBUG_PVAR_NAMES)
			System.out.println("Pvar names in use: " + RDDL.PVAR_SRC_SET);
	}
}
