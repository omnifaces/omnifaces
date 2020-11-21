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

import static java.lang.Boolean.TRUE;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getFlashAttribute;
import static org.omnifaces.util.Faces.refreshWithQueryString;
import static org.omnifaces.util.Faces.setFlashAttribute;

import java.util.Iterator;

import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;

import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.event.ExceptionQueuedEvent;

/**
 * <p>
 * The {@link ViewExpiredExceptionHandler} will suppress any {@link ViewExpiredException} and refresh the current page
 * by redirecting to the current URL with query string. Additionally, it will set a flash attribute indicating that the
 * {@link ViewExpiredException} was handled by this exception handler.
 *
 * <h3>Installation</h3>
 * <p>
 * This handler must be registered by a factory as follows in <code>faces-config.xml</code> in order to get it to run:
 * <pre>
 * &lt;factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.ViewExpiredExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 * <p>
 * In case there are multiple excepiton handlers, best is to register this handler as last one in the chain. For example,
 * when combined with {@link FullAjaxExceptionHandler}, this ordering will prevent the {@link FullAjaxExceptionHandler}
 * from taking over the handling of the {@link ViewExpiredException}.
 * <pre>
 * &lt;factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.FullAjaxExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.ViewExpiredExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * <h3>Note</h3>
 * <p>
 * It's your own responsibility to make sure that the end user gets some form of feedback as to why exactly the page is
 * refreshed and any submitted input values are lost. In order to check whether the previous request threw a
 * {@link ViewExpiredException} which was handled by this exception handler, then you can use
 * {@link ViewExpiredExceptionHandler#wasViewExpired()} in managed beans and
 * <code>#{flash['org.omnifaces.view_expired'] eq true}</code> in EL.
 * <p>
 * This approach will not work when the refresh in turn triggers yet another redirect elsewhere in the logic. In case
 * you want to retain the condition for the next request, then you need to ensure that the involved logic explicitly
 * triggers {@link Flash#keep(String)} in order to keep the flash attribute for the subsequent request. In the scope of
 * OmniFaces, this is already taken care of by {@link Faces#redirect(String, Object...)} and derivates.
 *
 * @author Lenny Primak
 * @see ViewExpiredExceptionHandlerFactory
 * @since 3.9
 */
public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

	/**
	 * The flash attribute name of a boolean value indicating that the previous request threw a
	 * {@link ViewExpiredException} which was handled by this exception handler.
	 */
	public static final String FLASH_ATTRIBUTE_VIEW_EXPIRED = "org.omnifaces.view_expired";

	/**
	 * Construct a new view expired exception handler around the given wrapped exception handler.
	 * @param wrapped The wrapped exception handler.
	 */
	public ViewExpiredExceptionHandler(ExceptionHandler wrapped) {
		super(wrapped);
	}

	/**
	 * Inspect all {@link #getUnhandledExceptionQueuedEvents()} if any of them is caused by {@link ViewExpiredException}.
	 * If so, then drain the {@link #getUnhandledExceptionQueuedEvents()}, set the flash attribute and refresh the
	 * current URL with query string.
	 */
	@Override
	public void handle() {
		handleViewExpiredException(getContext());
		getWrapped().handle();
	}

	private void handleViewExpiredException(FacesContext context) {
		if (context == null) {
			return; // Unexpected, most likely buggy JSF implementation or parent exception handler.
		}

		if (!isCausedByViewExpiredException()) {
			return;
		}

		setFlashAttribute(FLASH_ATTRIBUTE_VIEW_EXPIRED, TRUE);
		refreshWithQueryString();

		for (Iterator<ExceptionQueuedEvent> iter = getUnhandledExceptionQueuedEvents().iterator(); iter.hasNext();) {
			// Drain out the exceptions.
			iter.next();
			iter.remove();
		}
	}

	private boolean isCausedByViewExpiredException() {
		for (Iterator<ExceptionQueuedEvent> iter = getUnhandledExceptionQueuedEvents().iterator(); iter.hasNext();) {
			if (Exceptions.is(iter.next().getContext().getException(), ViewExpiredException.class)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns <code>true</code> if the previous request threw a {@link ViewExpiredException} which was handled by this
	 * exception handler.
	 * @return <code>true</code> if the previous request threw a {@link ViewExpiredException} which was handled by this
	 * exception handler.
	 */
	public static boolean wasViewExpired() {
		return getFlashAttribute(FLASH_ATTRIBUTE_VIEW_EXPIRED) == TRUE;
	}

}