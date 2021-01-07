/*
 * Copyright 2021 OmniFaces
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

import static jakarta.faces.application.FacesMessage.SEVERITY_FATAL;
import static org.omnifaces.util.Messages.addGlobal;

import java.util.Iterator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;

/**
 * <p>
 * The {@link FacesMessageExceptionHandler} will add every exception as a global FATAL faces message.
 *
 * <h2>Installation</h2>
 * <p>
 * This handler must be registered by a factory as follows in <code>faces-config.xml</code> in order to get it to run:
 * <pre>
 * &lt;factory&gt;
 *     &lt;exception-handler-factory&gt;org.omnifaces.exceptionhandler.FacesMessageExceptionHandlerFactory&lt;/exception-handler-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * <h2>Note</h2>
 * <p>
 * It's your own responsibility to make sure that the faces messages are being shown. Make sure that there's a
 * <code>&lt;h:messages&gt;</code> or any equivalent component (OmniFaces, PrimeFaces, etc) is present in the view and
 * that it can handle global messages and that it's explicitly or automatically updated in case of ajax requests. Also
 * make sure that you don't have bugs in rendering of your views. This exception handler is not capable of handling
 * exceptions during render response. It will fail silently.
 *
 * <h2>Customizing <code>FacesMessageExceptionHandler</code></h2>
 * <p>
 * If more fine grained control of creating the FATAL faces message is desired, then the developer can opt to extend
 * this {@link FacesMessageExceptionHandler} and override the following method:
 * <ul>
 * <li>{@link #createFatalMessage(Throwable)}
 * </ul>
 *
 * @author Bauke Scholtz
 * @see FacesMessageExceptionHandlerFactory
 * @see DefaultExceptionHandlerFactory
 * @since 1.8
 */
public class FacesMessageExceptionHandler extends ExceptionHandlerWrapper {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new faces message exception handler around the given wrapped exception handler.
	 * @param wrapped The wrapped exception handler.
	 */
	public FacesMessageExceptionHandler(ExceptionHandler wrapped) {
		super(wrapped);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Set every exception as a global FATAL faces message.
	 */
	@Override
	public void handle() {
		for (Iterator<ExceptionQueuedEvent> iter = getUnhandledExceptionQueuedEvents().iterator(); iter.hasNext();) {
			addGlobal(new FacesMessage(SEVERITY_FATAL, createFatalMessage(iter.next().getContext().getException()), null));
			iter.remove();
		}

		getWrapped().handle();
	}

	/**
	 * Create fatal message based on given exception which will in turn be passed to
	 * {@link FacesContext#addMessage(String, jakarta.faces.application.FacesMessage)}.
	 * The default implementation returns {@link Throwable#toString()}.
	 * @param exception The exception to create fatal message for.
	 * @return The fatal message created based on the given exception.
	 */
	protected String createFatalMessage(Throwable exception) {
		return exception.toString();
	}

}