/**
 * RDDL EvalException (thrown at runtime if error during simulation)
 *
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl;

public class EvalException extends Exception {
	public EvalException(String msg) {
		super(msg);
	}
}
