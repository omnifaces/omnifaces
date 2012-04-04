package org.omnifaces.util;

import javax.faces.FacesException;
import javax.servlet.ServletException;


/**
 * Collection of general utility methods with respect to working with exceptions.
 *
 * @author Bauke Scholtz
 */
public class Exceptions {

	/**
	 * Unwrap the nested causes of given exception as long as until it is not an instance of the given type and then
	 * return it. If the given exception is already not an instance of the given type, then it will directly be
	 * returned. Or if the exception, unwrapped or not, does not have a nested cause anymore, then it will be returned.
	 * This is particularly useful if you want to unwrap the real root cause out of a nested hierarchy of
	 * {@link ServletException} or {@link FacesException}.
	 * @param exception The exception to be unwrapped.
	 * @param type The type which needs to be unwrapped.
	 * @return The unwrapped root cause.
	 */
	public static <T extends Throwable> Throwable unwrap(Throwable exception, Class<T> type) {
		while (type.isInstance(exception) && exception.getCause() != null) {
			exception = exception.getCause();
		}

		return exception;
	}

	/**
	 * Returns <code>true</code> if the given exception or one of its nested causes is an instance of the given type.
	 * @param exception The exception to be checked.
	 * @param type The type to be compared to.
	 * @return <code>true</code> if the given exception or one of its nested causes is an instance of the given type.
	 */
	public static <T extends Throwable> boolean is(Throwable exception, Class<T> type) {
	    for (;exception != null; exception = exception.getCause()) {
	        if (type.isInstance(exception)) {
	            return true;
	        }
	    }

	    return false;
	}

}