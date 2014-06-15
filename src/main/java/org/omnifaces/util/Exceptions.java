/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.util;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.servlet.ServletException;

/**
 * Collection of general utility methods with respect to working with exceptions.
 *
 * @author Bauke Scholtz
 */
public final class Exceptions {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Exceptions() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

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
	 * Unwrap the nested causes of given exception as long as until it is not an instance of {@link FacesException}
	 * (Mojarra) or {@link ELException} (MyFaces) and then return it. If the given exception is already not an instance
	 * of the mentioned types, then it will directly be returned. Or if the exception, unwrapped or not, does not have
	 * a nested cause anymore, then it will be returned.
	 * @param exception The exception to be unwrapped from {@link FacesException} and {@link ELException}.
	 * @return The unwrapped root cause.
	 * @since 1.4
	 */
	public static <T extends Throwable> Throwable unwrap(Throwable exception) {
		return unwrap(unwrap(exception, FacesException.class), ELException.class);
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