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

import java.text.MessageFormat;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextListener;

/**
 * Collection of utility methods for the JSF API to ease adding faces messages. It also offers the possibility to set
 * a custom message resolver so that you can control the way how messages are been resolved. You can for example supply
 * an implementation wherein the message is been treated as for example a resource bundle key.
 * <p>
 * Here's an example:
 * <pre>
 * Messages.setResolver(new Messages.Resolver() {
 *     private static final String BASE_NAME = "com.example.i18n.messages";
 *     public String getMessage(String message, Object... params) {
 *         ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, Faces.getLocale());
 *         if (bundle.containsKey(message)) {
 *             message = bundle.getString(message);
 *         }
 *         return MessageFormat.format(message, params);
 *     }
 * });
 * </pre>
 * <p>
 * There is already a default resolver which just delegates the message and the parameters straight to
 * {@link MessageFormat#format(String, Object...)}. Note that the resolver can be set only once. It's recommend to do
 * it early during webapp's startup, for example with a {@link ServletContextListener}, or a
 * {@link ServletContainerInitializer}, or an eagerly initialized {@link ApplicationScoped} {@link ManagedBean}.
 *
 * @author Bauke Scholtz
 */
public final class Messages {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_RESOLVER_ALREADY_SET = "The resolver can be set only once.";

	// Message resolver -----------------------------------------------------------------------------------------------

	/**
	 * The message resolver allows the developers to change the way how messages are resolved.
	 *
	 * @author Bauke Scholtz
	 */
	public static interface Resolver {

		/**
		 * Returns the resolved message based on the given message and parameters.
		 * @param message The message which can be treated as for example a resource bundle key.
		 * @param params The message format parameters, if any.
		 * @return The resolved message.
		 */
		public String getMessage(String message, Object... params);

	}

	/**
	 * This is the default message resolver.
	 */
	private static final Resolver DEFAULT_RESOLVER = new Resolver() {

		@Override
		public String getMessage(String message, Object... params) {
			return MessageFormat.format(message, params);
		}

	};

	/**
	 * Initialize with the default resolver.
	 */
	private static Resolver resolver = DEFAULT_RESOLVER;

	/**
	 * Set the custom message resolver. It can be set only once. It's recommend to do it early during webapp's startup,
	 * for example with a {@link ServletContextListener}, or a {@link ServletContainerInitializer}, or an eagerly
	 * initialized {@link ApplicationScoped} {@link ManagedBean}.
	 * @param resolver The custom message resolver.
	 * @throws IllegalStateException When the resolver has already been set.
	 */
	public static void setResolver(Resolver resolver) {
		if (Messages.resolver == DEFAULT_RESOLVER) {
			Messages.resolver = resolver;
		}
		else {
			throw new IllegalStateException(ERROR_RESOLVER_ALREADY_SET);
		}
	}

	// Constructors ---------------------------------------------------------------------------------------------------

	private Messages() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Add a faces message of the given severity to the given client ID, with the given message body which is formatted
	 * with the given parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param severity The severity of the faces message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see Resolver#getMessage(String, Object...)
	 * @see FacesContext#addMessage(String, FacesMessage)
	 */
	public static void add(FacesMessage.Severity severity, String clientId, String message, Object... params) {
		FacesMessage facesMessage = new FacesMessage(severity, resolver.getMessage(message, params), null);
		FacesContext.getCurrentInstance().addMessage(clientId, facesMessage);
	}

	/**
	 * Add an INFO faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addInfo(String clientId, String message, Object... params) {
		add(FacesMessage.SEVERITY_INFO, clientId, message, params);
	}

	/**
	 * Add a WARN faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addWarn(String clientId, String message, Object... params) {
		add(FacesMessage.SEVERITY_WARN, clientId, message, params);
	}

	/**
	 * Add an ERROR faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addError(String clientId, String message, Object... params) {
		add(FacesMessage.SEVERITY_ERROR, clientId, message, params);
	}

	/**
	 * Add a FATAL faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFatal(String clientId, String message, Object... params) {
		add(FacesMessage.SEVERITY_FATAL, clientId, message, params);
	}

	/**
	 * Add a global INFO faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addGlobalInfo(String message, Object... params) {
		add(FacesMessage.SEVERITY_INFO, null, message, params);
	}

	/**
	 * Add a global WARN faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addGlobalWarn(String message, Object... params) {
		add(FacesMessage.SEVERITY_WARN, null, message, params);
	}

	/**
	 * Add a global ERROR faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addGlobalError(String message, Object... params) {
		add(FacesMessage.SEVERITY_ERROR, null, message, params);
	}

	/**
	 * Add a global FATAL faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addGlobalFatal(String message, Object... params) {
		add(FacesMessage.SEVERITY_FATAL, null, message, params);
	}

	/**
	 * Add a flash scoped faces message of the given severity to the given client ID, with the given message body which
	 * is formatted with the given parameters.
	 * <p>
	 * NOTE: the flash scope has in early Mojarra versions however some pretty peculiar problems. In older versions,
	 * the messages are remembered too long, or they are only displayed after refresh, or they are not displayed when
	 * the next request is on a different path. As of now, with Mojarra 2.1.7, only the last described problem remains.
	 * @param clientId The client ID to add the faces message for.
	 * @param severity The severity of the faces message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see Flash#setKeepMessages(boolean)
	 * @see #add(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlash(FacesMessage.Severity severity, String clientId, String message, Object... params) {
		Faces.getFlash().setKeepMessages(true);
		add(severity, clientId, message, params);
	}

	/**
	 * Add a flash scoped INFO faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashInfo(String clientId, String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_INFO, clientId, message, params);
	}

	/**
	 * Add a flash scoped WARN faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashWarn(String clientId, String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_WARN, clientId, message, params);
	}

	/**
	 * Add a flash scoped ERROR faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashError(String clientId, String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_ERROR, clientId, message, params);
	}

	/**
	 * Add a flash scoped FATAL faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashFatal(String clientId, String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_FATAL, clientId, message, params);
	}

	/**
	 * Add a flash scoped global INFO faces message, with the given message body which is formatted with the given
	 * parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashGlobalInfo(String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_INFO, null, message, params);
	}

	/**
	 * Add a flash scoped global WARN faces message, with the given message body which is formatted with the given
	 * parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashGlobalWarn(String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_WARN, null, message, params);
	}

	/**
	 * Add a flash scoped global ERROR faces message, with the given message body which is formatted with the given
	 * parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashGlobalError(String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_ERROR, null, message, params);
	}

	/**
	 * Add a flash scoped global FATAL faces message, with the given message body which is formatted with the given
	 * parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #addFlash(FacesMessage.Severity, String, String, Object...)
	 */
	public static void addFlashGlobalFatal(String message, Object... params) {
		addFlash(FacesMessage.SEVERITY_FATAL, null, message, params);
	}

}