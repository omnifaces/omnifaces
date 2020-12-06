/*
 * Copyright 2020 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.exceptionhandler;

import static org.omnifaces.exceptionhandler.FullAjaxExceptionHandler.parseExceptionTypesParam;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.Faces.refreshWithQueryString;
import static org.omnifaces.util.FacesLocal.isResponseCommitted;

import java.util.Iterator;
import java.util.Optional;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.servlet.ServletContext;

import org.omnifaces.util.Exceptions;

/**
 * <p>
 * The {@link ExceptionSuppressor} will suppress all exceptions which are an instance of the types as listed in context
 * parameter {@value org.omnifaces.exceptionhandler.ExceptionSuppressor#PARAM_NAME_EXCEPTION_TYPES_TO_SUPPRESS} and
 * refresh the current page by redirecting to the current URL with query string. The context parameter value must be a
 * commaseparated string of fully qualified names of exception types. Note that this also covers subclasses of specified
 * exception types.
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.EXCEPTION_TYPES_TO_SUPPRESS&lt;/param-name&gt;
 *     &lt;param-value&gt;java.nio.channels.ClosedByInterruptException,java.nio.channels.IllegalSelectorException&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * This is useful for exceptions which are technically unavoidable such as those which boil down to that the network
 * connection is abruptly closed by the client by e.g. navigating away while the page is loading, or closing the browser
 * window/tab while the page is loading, or having the physical network connection cut down, or the physical machine
 * crashed, etcetera. All which are beyond control of the server and therefore not really interesting to have logged
 * into server logs.
 *
 * <h3>Installation</h3>
 * <p>
 * This handler must be registered by a factory as follows in <code>faces-config.xml</code> in order to get it to run:
 * <pre>
 * &lt;factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.ExceptionSuppressorFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 * <p>
 * In case there are multiple exception handlers, best is to register this handler as last one in the chain. For example,
 * when combined with {@link FullAjaxExceptionHandler}, this ordering will prevent the {@link FullAjaxExceptionHandler}
 * from taking over the handling of the to-be-suppressed exceptions.
 * <pre>
 * &lt;factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.ExceptionSuppressorFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * @author Lenny Primak
 * @see ExceptionSuppressorFactory
 * @since 3.9
 */
public class ExceptionSuppressor extends ExceptionHandlerWrapper {

	/**
	 * The context parameter name to specify exception types to suppress by {@link ExceptionSuppressor}. The context
	 * parameter value must be a commaseparated string of fully qualified names of exception types. Note that this also
	 * covers subclasses of specified exception types.
	 */
	public static final String PARAM_NAME_EXCEPTION_TYPES_TO_SUPPRESS =
		"org.omnifaces.EXCEPTION_TYPES_TO_SUPPRESS";

	private Class<? extends Throwable>[] exceptionTypesToSuppress;

	/**
	 * Construct a new exception suppressor around the given wrapped exception handler.
	 * @param wrapped The wrapped exception handler.
	 */
	public ExceptionSuppressor(ExceptionHandler wrapped) {
		this(wrapped, getExceptionTypesToSuppress(getServletContext()));
	}

	/**
	 * Construct a new exception suppressor around the given wrapped exception handler and using the given array of
	 * exception types to suppress.
	 * @param wrapped The wrapped exception handler.
	 * @param exceptionTypesToSuppress Array of exception types to suppress.
	 */
	@SafeVarargs
	protected ExceptionSuppressor(ExceptionHandler wrapped, Class<? extends Throwable>... exceptionTypesToSuppress) {
		super(wrapped);
		this.exceptionTypesToSuppress = exceptionTypesToSuppress;
	}

	/**
	 * Get the exception types to suppress. This can be specified via context parameter
	 * {@value org.omnifaces.exceptionhandler.ExceptionSuppressor#PARAM_NAME_EXCEPTION_TYPES_TO_SUPPRESS}.
	 * @param context The involved servlet context.
	 * @return Exception types to suppress.
	 */
	public static Class<? extends Throwable>[] getExceptionTypesToSuppress(ServletContext context) {
		return parseExceptionTypesParam(context, PARAM_NAME_EXCEPTION_TYPES_TO_SUPPRESS, null);
	}

	/**
	 * Inspect all {@link #getUnhandledExceptionQueuedEvents()} if any of them is caused by one of the exception types
	 * listed in {@link #PARAM_NAME_EXCEPTION_TYPES_TO_SUPPRESS}.
	 * If so, then drain the {@link #getUnhandledExceptionQueuedEvents()}, and refresh the current URL with query string.
	 */
	@Override
	public void handle() {
		handleSuppressedException(getContext());
		getWrapped().handle();
	}

	private void handleSuppressedException(FacesContext context) {
		if (context == null) {
			return; // Unexpected, most likely buggy JSF implementation or parent exception handler.
		}

		Optional<Throwable> suppressedException = findSuppressedException();

		if (!suppressedException.isPresent()) {
			return;
		}

		handleSuppressedException(context, suppressedException.get());

		if (!isResponseCommitted(context)) {
			refreshWithQueryString();
		}

		for (Iterator<ExceptionQueuedEvent> iter = getUnhandledExceptionQueuedEvents().iterator(); iter.hasNext();) {
			// Drain out the exceptions.
			iter.next();
			iter.remove();
		}
	}

	/**
	 * Subclasses can override this method to have finer grained control over what must happen when the given exception
	 * has been suppressed.
	 * @param context The involved faces context.
	 * @param suppressedException The suppressed exception.
	 */
	protected void handleSuppressedException(FacesContext context, Throwable suppressedException) {
		// NOOP.
	}

	private Optional<Throwable> findSuppressedException() {
		for (Iterator<ExceptionQueuedEvent> iter = getUnhandledExceptionQueuedEvents().iterator(); iter.hasNext();) {
			Throwable unhandledException = iter.next().getContext().getException();

			for (Class<? extends Throwable> exceptionTypeToSuppress : exceptionTypesToSuppress) {
				if (Exceptions.is(unhandledException, exceptionTypeToSuppress)) {
					return Optional.of(unhandledException);
				}
			}
		}

		return Optional.empty();
	}

}