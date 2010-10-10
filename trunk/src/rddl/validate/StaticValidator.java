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

public class StaticValidator {

	// Ensure that all pvariable references in expressions refer to
	// defined pvariables
	
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
}
