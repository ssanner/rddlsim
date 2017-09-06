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

public class StaticValidator {

	// Note when objects are referenced but not declared in object lists
	
	// Ensure that all pvariable references in expressions refer to
	// defined pvariables
	
	// CPF definitions must reference all variables
	
	// Mark expression types and verify correct type usage (i.e., can't put
	// an integer pvariable in a boolean expression)
	
	// Check for no free variables in CPFs
	
	// Check that any referenced objects are included in objects
	
	// Check that objects are not redefined between state and nonfluents
	
	// Check enum values are defined
	
	// Strict stratification of intermediate and next-state variables
	
	// Check requirements are all satisfied
	// - all branches need to be RVs for stochastic, non-RVs for deterministic
	
	// Note: need runtime validation to verify state constraints
	
	// Make sure variable names are not identical (done now in RDDL.addDefs)
	
	// Object names cannot be reserved words

	// Only reward and observations can reference next-state variables
	
	// Check for illegal default values (0 for boolean)
}
