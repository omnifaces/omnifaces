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
package org.omnifaces.util;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toSet;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getFlash;
import static org.omnifaces.util.Utils.stream;

import java.text.MessageFormat;
import java.util.Iterator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.validator.ValidatorException;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.omnifaces.cdi.Startup;

/**
 * <p>
 * Collection of utility methods for the JSF API with respect to working with {@link FacesMessage}.
 *
 * <h2>Usage</h2>
 * <p>
 * Some examples:
 * <pre>
 * // In a validator.
 * Messages.throwValidatorException("Invalid input.");
 * </pre>
 * <pre>
 * // In a validator, as extra message on another component.
 * Messages.addError("someFormId:someInputId", "This is also invalid.");
 * </pre>
 * <pre>
 * // In a managed bean action method.
 * Messages.addGlobalError("Unknown login, please try again.");
 * </pre>
 * <pre>
 * // In a managed bean action method which uses Post-Redirect-Get.
 * Messages.addFlashGlobalInfo("New item with id {0} is successfully added.", item.getId());
 * return "items?faces-redirect=true";
 * </pre>
 * <p>
 * There is also a builder which also allows you to set the message detail. Some examples:
 * <pre>
 * // In a validator.
 * Messages.create("Invalid input.").detail("Value {0} is not expected.", value).throwValidatorException();
 * </pre>
 * <pre>
 * // In a validator, as extra message on another component.
 * Messages.create("This is also invalid.").error().add("someFormId:someInputId");
 * </pre>
 * <pre>
 * // In a managed bean action method.
 * Messages.create("Unknown login, please try again.").error().add();
 * </pre>
 * <pre>
 * // In a managed bean action method which uses Post-Redirect-Get.
 * Messages.create("New item with id {0} is successfully added.", item.getId()).flash().add();
 * return "items?faces-redirect=true";
 * </pre>
 *
 * <h2>Message resolver</h2>
 * <p>
 * It also offers the possibility to set a custom message resolver so that you can control the way how messages are been
 * resolved. You can for example supply an implementation wherein the message is been treated as for example a resource
 * bundle key. Here's an example:
 * <pre>
 * Messages.setResolver(new Messages.Resolver() {
 *     private static final String BASE_NAME = "com.example.i18n.messages";
 *     public String getMessage(String message, Object... params) {
 *         ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, Faces.getLocale());
 *         if (bundle.containsKey(message)) {
 *             message = bundle.getString(message);
 *         }
 *         return params.length &gt; 0 ? MessageFormat.format(message, params) : message;
 *     }
 * });
 * </pre>
 * <p>
 * There is already a default resolver which just delegates the message and the parameters straight to
 * {@link MessageFormat#format(String, Object...)}. Note that the resolver can be set only once. It's recommend to do
 * it early during webapp's startup, for example with a {@link ServletContextListener} as {@link WebListener}, or a
 * {@link ServletContainerInitializer} in custom JAR, or a {@link ApplicationScoped} bean, or an eagerly initialized
 * {@link Startup} bean.
 *
 * <h2>Design notice</h2>
 * <p>
 * Note that all of those shortcut methods by design only sets the message summary and ignores the message detail (it
 * is not possible to offer varargs to parameterize <em>both</em> the summary and the detail). The message summary is
 * exactly the information which is by default displayed in the <code>&lt;h:message(s)&gt;</code>, while the detail is
 * by default only displayed when you explicitly set the <code>showDetail="true"</code> attribute.
 * <p>
 * To create a {@link FacesMessage} with a message detail as well, use the {@link Message} builder as you can obtain by
 * {@link Messages#create(String, Object...)}.
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
	public interface Resolver {

		/**
		 * Returns the resolved message based on the given message and parameters.
		 * @param message The message which can be treated as for example a resource bundle key.
		 * @param params The message format parameters, if any.
		 * @return The resolved message.
		 */
		String getMessage(String message, Object... params);

	}

	/**
	 * This is the default message resolver.
	 */
	private static final Resolver DEFAULT_RESOLVER = (message, params) -> Utils.isEmpty(params) ? message : format(message, params);

	/**
	 * Initialize with the default resolver.
	 */
	private static Resolver resolver = DEFAULT_RESOLVER;

	/**
	 * Set the custom message resolver. It can be set only once. It's recommend to do it early during webapp's startup,
	 * for example with a {@link ServletContextListener} as {@link WebListener}, or a
	 * {@link ServletContainerInitializer} in custom JAR, or a {@link ApplicationScoped} bean, or an eagerly initialized
	 * {@link Startup} bean.
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

	// Builder --------------------------------------------------------------------------------------------------------

	/**
	 * Create a faces message with the default INFO severity and the given message body which is formatted with the
	 * given parameters as summary message. To set the detail message, use {@link Message#detail(String, Object...)}.
	 * To change the default INFO severity, use {@link Message#warn()}, {@link Message#error()}, or
	 * {@link Message#fatal()}. To make it a flash message, use {@link Message#flash()}. To finally add it to the faces
	 * context, use either {@link Message#add(String)} to add it for a specific client ID, or {@link Message#add()} to
	 * add it as a global message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @return The {@link Message} builder.
	 * @see Messages#createInfo(String, Object...)
	 * @see Resolver#getMessage(String, Object...)
	 * @since 1.1
	 */
	public static Message create(String message, Object... params) {
		return new Message(createInfo(message, params));
	}

	/**
	 * Faces message builder.
	 *
	 * @author Bauke Scholtz
	 * @since 1.1
	 */
	public static final class Message {

		private FacesMessage facesMessage;

		private Message(FacesMessage facesMessage) {
			this.facesMessage = facesMessage;
		}

		/**
		 * Set the detail message of the current message.
		 * @param detail The detail message to be set on the current message.
		 * @param params The detail message format parameters, if any.
		 * @return The current {@link Message} instance for further building.
		 * @see FacesMessage#setDetail(String)
		 */
		public Message detail(String detail, Object... params) {
			facesMessage.setDetail(resolver.getMessage(detail, params));
			return this;
		}

		/**
		 * Set the severity of the current message to WARN. Note: it defaults to INFO already.
		 * @return The current {@link Message} instance for further building.
		 * @see FacesMessage#setSeverity(jakarta.faces.application.FacesMessage.Severity)
		 */
		public Message warn() {
			facesMessage.setSeverity(FacesMessage.SEVERITY_WARN);
			return this;
		}

		/**
		 * Set the severity of the current message to ERROR. Note: it defaults to INFO already.
		 * @return The current {@link Message} instance for further building.
		 * @see FacesMessage#setSeverity(jakarta.faces.application.FacesMessage.Severity)
		 */
		public Message error() {
			facesMessage.setSeverity(FacesMessage.SEVERITY_ERROR);
			return this;
		}

		/**
		 * Set the severity of the current message to FATAL. Note: it defaults to INFO already.
		 * @return The current {@link Message} instance for further building.
		 * @see FacesMessage#setSeverity(jakarta.faces.application.FacesMessage.Severity)
		 */
		public Message fatal() {
			facesMessage.setSeverity(FacesMessage.SEVERITY_FATAL);
			return this;
		}

		/**
		 * Make the current message a flash message. Use this when you need to display the message after a redirect.
		 * @return The current {@link Message} instance for further building.
		 * @see Flash#setKeepMessages(boolean)
		 */
		public Message flash() {
			getFlash().setKeepMessages(true);
			return this;
		}

		/**
		 * Add the current message for the given client ID.
		 * @param clientId The client ID to add the current message for.
		 * @see FacesContext#addMessage(String, FacesMessage)
		 */
		public void add(String clientId) {
			Messages.add(clientId, facesMessage);
		}

		/**
		 * Add the current message as a global message.
		 * @see FacesContext#addMessage(String, FacesMessage)
		 */
		public void add() {
			Messages.addGlobal(facesMessage);
		}

		/**
		 * Returns the so far built message.
		 * @return The so far built message.
		 */
		public FacesMessage get() {
			return facesMessage;
		}

		/**
		 * Throws the so far built message as a {@link ConverterException}.
		 * @throws ConverterException
		 * @since 3.5
		 */
		public void throwConverterException() throws ConverterException {
			throw new ConverterException(facesMessage);
		}

		/**
		 * Throws the so far built message as a {@link ValidatorException}.
		 * @throws ValidatorException
		 * @since 3.5
		 */
		public void throwValidatorException() throws ValidatorException {
			throw new ValidatorException(facesMessage);
		}

	}

	// Shortcuts - create message -------------------------------------------------------------------------------------

	/**
	 * Create a faces message of the given severity with the given message body which is formatted with the given
	 * parameters. Useful when a faces message is needed to construct a {@link ConverterException} or a
	 * {@link ValidatorException}.
	 * @param severity The severity of the faces message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @return A new faces message of the given severity with the given message body which is formatted with the given
	 * parameters.
	 * @see Resolver#getMessage(String, Object...)
	 */
	public static FacesMessage create(FacesMessage.Severity severity, String message, Object... params) {
		return new FacesMessage(severity, resolver.getMessage(message, params), null);
	}

	/**
	 * Create an INFO faces message with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @return A new INFO faces message with the given message body which is formatted with the given parameters.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 */
	public static FacesMessage createInfo(String message, Object... params) {
		return create(FacesMessage.SEVERITY_INFO, message, params);
	}

	/**
	 * Create a WARN faces message with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @return A new WARN faces message with the given message body which is formatted with the given parameters.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 */
	public static FacesMessage createWarn(String message, Object... params) {
		return create(FacesMessage.SEVERITY_WARN, message, params);
	}

	/**
	 * Create an ERROR faces message with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @return A new ERROR faces message with the given message body which is formatted with the given parameters.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 */
	public static FacesMessage createError(String message, Object... params) {
		return create(FacesMessage.SEVERITY_ERROR, message, params);
	}

	/**
	 * Create a FATAL faces message with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @return A new FATAL faces message with the given message body which is formatted with the given parameters.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 */
	public static FacesMessage createFatal(String message, Object... params) {
		return create(FacesMessage.SEVERITY_FATAL, message, params);
	}

	// Shortcuts - throw validator/converter exception ----------------------------------------------------------------

	/**
	 * Throw a {@link ConverterException} with an ERROR faces message with the given message body which is formatted
	 * with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @throws ConverterException
	 * with the given parameters.
	 * @see #createError(String, Object...)
	 * @since 3.5
	 */
	public void throwConverterException(String message, Object... params) throws ConverterException {
		throw new ConverterException(createError(message, params));
	}

	/**
	 * Throw a {@link ValidatorException} with an ERROR faces message with the given message body which is formatted
	 * with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @throws ValidatorException
	 * with the given parameters.
	 * @see #createError(String, Object...)
	 * @since 3.5
	 */
	public void throwValidatorException(String message, Object... params) throws ValidatorException {
		throw new ValidatorException(createError(message, params));
	}

	// Shortcuts - add message ----------------------------------------------------------------------------------------

	/**
	 * Add the given faces message to the given client ID. When the client ID is <code>null</code>, it becomes a
	 * global faces message. This can be filtered in a <code>&lt;h:messages globalOnly="true"&gt;</code>.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The faces message.
	 * @see FacesContext#addMessage(String, FacesMessage)
	 */
	public static void add(String clientId, FacesMessage message) {
		getContext().addMessage(clientId, message);
	}

	/**
	 * Add a faces message of the given severity to the given client ID, with the given message body which is formatted
	 * with the given parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param severity The severity of the faces message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 * @see #add(String, FacesMessage)
	 */
	public static void add(FacesMessage.Severity severity, String clientId, String message, Object... params) {
		add(clientId, create(severity, message, params));
	}

	/**
	 * Add an INFO faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createInfo(String, Object...)
	 * @see #add(String, FacesMessage)
	 */
	public static void addInfo(String clientId, String message, Object... params) {
		add(clientId, createInfo(message, params));
	}

	/**
	 * Add a WARN faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createWarn(String, Object...)
	 * @see #add(String, FacesMessage)
	 */
	public static void addWarn(String clientId, String message, Object... params) {
		add(clientId, createWarn(message, params));
	}

	/**
	 * Add an ERROR faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createError(String, Object...)
	 * @see #add(String, FacesMessage)
	 */
	public static void addError(String clientId, String message, Object... params) {
		add(clientId, createError(message, params));
	}

	/**
	 * Add a FATAL faces message to the given client ID, with the given message body which is formatted with the given
	 * parameters.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createFatal(String, Object...)
	 * @see #add(String, FacesMessage)
	 */
	public static void addFatal(String clientId, String message, Object... params) {
		add(clientId, createFatal(message, params));
	}

	// Shortcuts - add global message ---------------------------------------------------------------------------------

	/**
	 * Add a global faces message. This adds a faces message to a client ID of <code>null</code>.
	 * @param message The global faces message.
	 * @see #add(String, FacesMessage)
	 */
	public static void addGlobal(FacesMessage message) {
		add(null, message);
	}

	/**
	 * Add a global faces message of the given severity, with the given message body which is formatted with the given
	 * parameters.
	 * @param severity The severity of the global faces message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 * @see #addGlobal(FacesMessage)
	 */
	public static void addGlobal(FacesMessage.Severity severity, String message, Object... params) {
		addGlobal(create(severity, message, params));
	}

	/**
	 * Add a global INFO faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createInfo(String, Object...)
	 * @see #addGlobal(FacesMessage)
	 */
	public static void addGlobalInfo(String message, Object... params) {
		addGlobal(createInfo(message, params));
	}

	/**
	 * Add a global WARN faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createWarn(String, Object...)
	 * @see #addGlobal(FacesMessage)
	 */
	public static void addGlobalWarn(String message, Object... params) {
		addGlobal(createWarn(message, params));
	}

	/**
	 * Add a global ERROR faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createError(String, Object...)
	 * @see #addGlobal(FacesMessage)
	 */
	public static void addGlobalError(String message, Object... params) {
		addGlobal(createError(message, params));
	}

	/**
	 * Add a global FATAL faces message, with the given message body which is formatted with the given parameters.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createFatal(String, Object...)
	 * @see #addGlobal(FacesMessage)
	 */
	public static void addGlobalFatal(String message, Object... params) {
		addGlobal(createFatal(message, params));
	}

	// Shortcuts - add flash message ----------------------------------------------------------------------------------

	/**
	 * Add a flash scoped faces message to the given client ID. Use this when you need to display the message after a
	 * redirect.
	 * <p>
	 * NOTE: the flash scope has in early Mojarra versions however some pretty peculiar problems. In older versions,
	 * the messages are remembered too long, or they are only displayed after refresh, or they are not displayed when
	 * the next request is on a different path. Only since Mojarra 2.1.14, all known flash scope problems are solved.
	 * @param clientId The client ID to add the flash scoped faces message for.
	 * @param message The faces message.
	 * @see Flash#setKeepMessages(boolean)
	 * @see #add(String, FacesMessage)
	 */
	public static void addFlash(String clientId, FacesMessage message) {
		getFlash().setKeepMessages(true);
		add(clientId, message);
	}

	/**
	 * Add a flash scoped faces message of the given severity to the given client ID, with the given message body which
	 * is formatted with the given parameters. Use this when you need to display the message after a redirect.
	 * @param clientId The client ID to add the faces message for.
	 * @param severity The severity of the faces message.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #create(jakarta.faces.application.FacesMessage.Severity, String, Object...)
	 * @see #addFlash(String, FacesMessage)
	 */
	public static void addFlash(FacesMessage.Severity severity, String clientId, String message, Object... params) {
		addFlash(clientId, create(severity, message, params));
	}

	/**
	 * Add a flash scoped INFO faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters. Use this when you need to display the message after a redirect.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createInfo(String, Object...)
	 * @see #addFlash(String, FacesMessage)
	 */
	public static void addFlashInfo(String clientId, String message, Object... params) {
		addFlash(clientId, createInfo(message, params));
	}

	/**
	 * Add a flash scoped WARN faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters. Use this when you need to display the message after a redirect.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createWarn(String, Object...)
	 * @see #addFlash(String, FacesMessage)
	 */
	public static void addFlashWarn(String clientId, String message, Object... params) {
		addFlash(clientId, createWarn(message, params));
	}

	/**
	 * Add a flash scoped ERROR faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters. Use this when you need to display the message after a redirect.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createError(String, Object...)
	 * @see #addFlash(String, FacesMessage)
	 */
	public static void addFlashError(String clientId, String message, Object... params) {
		addFlash(clientId, createError(message, params));
	}

	/**
	 * Add a flash scoped FATAL faces message to the given client ID, with the given message body which is formatted
	 * with the given parameters. Use this when you need to display the message after a redirect.
	 * @param clientId The client ID to add the faces message for.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createFatal(String, Object...)
	 * @see #addFlash(String, FacesMessage)
	 */
	public static void addFlashFatal(String clientId, String message, Object... params) {
		addFlash(clientId, createFatal(message, params));
	}

	// Shortcuts - add global flash message ---------------------------------------------------------------------------

	/**
	 * Add a flash scoped global faces message. This adds a faces message to a client ID of <code>null</code>. Use this
	 * when you need to display the message after a redirect.
	 * @param message The global faces message.
	 * @see #addFlash(String, FacesMessage)
	 */
	public static void addFlashGlobal(FacesMessage message) {
		addFlash(null, message);
	}

	/**
	 * Add a flash scoped global INFO faces message, with the given message body which is formatted with the given
	 * parameters. Use this when you need to display the message after a redirect.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createInfo(String, Object...)
	 * @see #addFlashGlobal(FacesMessage)
	 */
	public static void addFlashGlobalInfo(String message, Object... params) {
		addFlashGlobal(createInfo(message, params));
	}

	/**
	 * Add a flash scoped global WARN faces message, with the given message body which is formatted with the given
	 * parameters. Use this when you need to display the message after a redirect.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createWarn(String, Object...)
	 * @see #addFlashGlobal(FacesMessage)
	 */
	public static void addFlashGlobalWarn(String message, Object... params) {
		addFlashGlobal(createWarn(message, params));
	}

	/**
	 * Add a flash scoped global ERROR faces message, with the given message body which is formatted with the given
	 * parameters. Use this when you need to display the message after a redirect.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createError(String, Object...)
	 * @see #addFlashGlobal(FacesMessage)
	 */
	public static void addFlashGlobalError(String message, Object... params) {
		addFlashGlobal(createError(message, params));
	}

	/**
	 * Add a flash scoped global FATAL faces message, with the given message body which is formatted with the given
	 * parameters. Use this when you need to display the message after a redirect.
	 * @param message The message body.
	 * @param params The message format parameters, if any.
	 * @see #createFatal(String, Object...)
	 * @see #addFlashGlobal(FacesMessage)
	 */
	public static void addFlashGlobalFatal(String message, Object... params) {
		addFlashGlobal(createFatal(message, params));
	}

	// Shortcuts - check messages -------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if there are no faces messages, otherwise <code>false</code>.
	 * @return <code>true</code> if there are no faces messages, otherwise <code>false</code>.
	 * @see FacesContext#getMessageList()
	 * @since 2.2
	 */
	public static boolean isEmpty() {
		return getContext().getMessageList().isEmpty();
	}

	/**
	 * Returns <code>true</code> if there are no faces messages for the given client ID, otherwise <code>false</code>.
	 * @return <code>true</code> if there are no faces messages for the given client ID, otherwise <code>false</code>.
	 * @param clientId The client ID to check the messages for.
	 * @see FacesContext#getMessageList(String)
	 * @since 2.2
	 */
	public static boolean isEmpty(String clientId) {
		return getContext().getMessageList(clientId).isEmpty();
	}

	/**
	 * Returns <code>true</code> if there are no global faces messages, otherwise <code>false</code>.
	 * @return <code>true</code> if there are no global faces messages, otherwise <code>false</code>.
	 * @see FacesContext#getMessageList(String)
	 * @since 2.2
	 */
	public static boolean isGlobalEmpty() {
		return isEmpty(null);
	}

	// Shortcuts - clear messages -------------------------------------------------------------------------------------

	/**
	 * Clears faces messages of the given severity associated with the given client IDs.
	 * @param severity The severity to clear faces message for. If this is null, then all severities are matched.
	 * @param clientIds The client IDs to clear faces messages for. If this is null or empty, then all faces messages
	 * are cleared. If this contains null, then all global faces messages are cleared.
	 * @return <code>true</code> if at least one faces message of the given severity associated with the given client
	 * IDs was cleared.
	 * @see FacesContext#getMessages()
	 * @see FacesContext#getMessages(String)
	 * @since 3.5
	 */
	public static boolean clear(FacesMessage.Severity severity, String... clientIds) {
		if (Utils.isEmpty(clientIds)) {
			return clear(getContext().getMessages(), severity);
		}
		else {
			return stream(clientIds).map(clientId -> clear(getContext().getMessages(clientId), severity)).collect(toSet()).contains(true);
		}
	}

	private static boolean clear(Iterator<FacesMessage> iterator, FacesMessage.Severity severity) {
		boolean atLeastOneCleared = false;

		while (iterator.hasNext()) {
			FacesMessage facesMessage = iterator.next();

			if (severity == null || severity.equals(facesMessage.getSeverity())) {
				iterator.remove();
				atLeastOneCleared = true;
			}
		}

		return atLeastOneCleared;
	}

	/**
	 * Clears faces messages associated with the given client IDs.
	 * @param clientIds The client IDs to clear faces messages for. If this is empty, then all faces messages are
	 * cleared. If this contains null, then all global faces messages are cleared.
	 * @return <code>true</code> if at least one faces message associated with the given client IDs was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clear(String... clientIds) {
		return clear(null, clientIds);
	}

	/**
	 * Clears INFO faces messages associated with the given client IDs.
	 * @param clientIds The client IDs to clear INFO faces messages for. If this is empty, then all INFO faces messages
	 * are cleared. If this contains null, then all global INFO faces messages are cleared.
	 * @return <code>true</code> if at least one INFO faces message associated with the given client IDs was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clearInfo(String... clientIds) {
		return clear(FacesMessage.SEVERITY_INFO, clientIds);
	}

	/**
	 * Clears WARN faces messages associated with the given client IDs.
	 * @param clientIds The client IDs to clear WARN faces messages for. If this is empty, then all WARN faces messages
	 * are cleared. If this contains null, then all global WARN faces messages are cleared.
	 * @return <code>true</code> if at least one WARN faces message associated with the given client IDs was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clearWarn(String... clientIds) {
		return clear(FacesMessage.SEVERITY_WARN, clientIds);
	}

	/**
	 * Clears ERROR faces messages associated with the given client IDs.
	 * @param clientIds The client IDs to clear ERROR faces messages for. If this is empty, then all ERROR faces
	 * messages are cleared. If this contains null, then all global ERROR faces messages are cleared.
	 * @return <code>true</code> if at least one ERROR faces message associated with the given client IDs was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clearError(String... clientIds) {
		return clear(FacesMessage.SEVERITY_ERROR, clientIds);
	}

	/**
	 * Clears FATAL faces messages associated with the given client IDs.
	 * @param clientIds The client IDs to clear FATAL faces messages for. If this is empty, then all FATAL face
	 * messages are cleared. If this contains null, then all global FATAL faces messages are cleared.
	 * @return <code>true</code> if at least one FATAL faces message associated with the given client IDs was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clearFatal(String... clientIds) {
		return clear(FacesMessage.SEVERITY_FATAL, clientIds);
	}

	/**
	 * Clears global faces messages of given severity.
	 * @param severity The severity of the faces message. If this is null, then all severities are matched.
	 * @return <code>true</code> if at least one global faces message of given severity was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clearGlobal(FacesMessage.Severity severity) {
		return clear(severity, (String) null);
	}

	/**
	 * Clears global faces messages of all severities.
	 * @return <code>true</code> if at least one global faces message was cleared.
	 * @see #clear(jakarta.faces.application.FacesMessage.Severity, String...)
	 * @since 3.5
	 */
	public static boolean clearGlobal() {
		return clear((FacesMessage.Severity) null, (String) null);
	}

	/**
	 * Clears global INFO faces messages.
	 * @return <code>true</code> if at least one global INFO faces message was cleared.
	 * @see #clearInfo(String...)
	 * @since 3.5
	 */
	public static boolean clearGlobalInfo() {
		return clearInfo((String) null);
	}

	/**
	 * Clears global WARN faces messages.
	 * @return <code>true</code> if at least one global WARN faces message was cleared.
	 * @see #clearWarn(String...)
	 * @since 3.5
	 */
	public static boolean clearGlobalWarn() {
		return clearWarn((String) null);
	}

	/**
	 * Clears global ERROR faces messages.
	 * @return <code>true</code> if at least one global ERROR faces message was cleared.
	 * @see #clearError(String...)
	 * @since 3.5
	 */
	public static boolean clearGlobalError() {
		return clearError((String) null);
	}

	/**
	 * Clears global FATAL faces messages.
	 * @return <code>true</code> if at least one global FATAL faces message was cleared.
	 * @see #clearFatal(String...)
	 * @since 3.5
	 */
	public static boolean clearGlobalFatal() {
		return clearFatal((String) null);
	}

}