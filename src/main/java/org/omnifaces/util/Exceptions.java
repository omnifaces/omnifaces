/*
 * Copyright 2019 OmniFaces
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

import static org.omnifaces.util.Utils.isOneInstanceOf;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.servlet.ServletException;

/**
 * <p>
 * Collection of general utility methods with respect to working with exceptions. So far there's only an unwrapper and
 * a type checker.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Check if the caught exception has a ConstraintViolationException in its hierarchy.
 * catch (PersistenceException e) {
 *     if (Exceptions.is(e, ConstraintViolationException.class)) {
 *         // ...
 *     }
 * }
 * </pre>
 * <pre>
 * // Unwrap the caught FacesException until a non-FacesException is found.
 * catch (FacesException e) {
 *     Exception realRootCause = Exceptions.unwrap(e, FacesException.class);
 *     // ...
 * }
 * </pre>
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
	 * Unwrap the nested causes of given exception as long as until it is not an instance of the given types and then
	 * return it. If the given exception is already not an instance of the given types, then it will directly be
	 * returned. Or if the exception, unwrapped or not, does not have a nested cause anymore, then it will be returned.
	 * This is particularly useful if you want to unwrap the real root cause out of a nested hierarchy of
	 * {@link ServletException} or {@link FacesException}.
	 * @param exception The exception to be unwrapped.
	 * @param types The types which need to be unwrapped.
	 * @return The unwrapped root cause.
	 */
	@SafeVarargs
	public static Throwable unwrap(Throwable exception, Class<? extends Throwable>... types) {
		Throwable unwrappedException = exception;

		while (isOneInstanceOf(unwrappedException.getClass(), types) && unwrappedException.getCause() != null) {
			unwrappedException = unwrappedException.getCause();
		}

		return unwrappedException;
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
	public static Throwable unwrap(Throwable exception) {
		return unwrap(exception, FacesException.class, ELException.class);
	}

	/**
	 * Returns <code>true</code> if the given exception or one of its nested causes is an instance of the given type.
	 * @param <T> The generic throwable type.
	 * @param exception The exception to be checked.
	 * @param type The type to be compared to.
	 * @return <code>true</code> if the given exception or one of its nested causes is an instance of the given type.
	 */
	public static <T extends Throwable> boolean is(Throwable exception, Class<T> type) {
		Throwable unwrappedException = exception;

		while (unwrappedException != null) {
			if (type.isInstance(unwrappedException)) {
				return true;
			}

			unwrappedException = unwrappedException.getCause();
		}

		return false;
	}

}