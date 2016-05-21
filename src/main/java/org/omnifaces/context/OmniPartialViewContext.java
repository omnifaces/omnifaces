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
package org.omnifaces.context;

import static javax.servlet.RequestDispatcher.FORWARD_REQUEST_URI;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.responseReset;
import static org.omnifaces.util.Faces.setContextAttribute;
import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.getRequest;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getResponse;
import static org.omnifaces.util.FacesLocal.getViewId;
import static org.omnifaces.util.FacesLocal.invalidateSession;
import static org.omnifaces.util.FacesLocal.normalizeViewId;
import static org.omnifaces.util.Servlets.facesRedirect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;
import javax.faces.event.PhaseId;

import org.omnifaces.config.WebXml;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.Hacks;
import org.omnifaces.util.Json;

/**
 * <p>
 * This OmniFaces partial view context extends and improves the standard partial view context as follows:
 * <ul>
 * <li>Support for executing callback scripts by {@link PartialResponseWriter#startEval()}.</li>
 * <li>Support for adding arguments to an ajax response.</li>
 * <li>Any XML tags which Mojarra and MyFaces has left open after an exception in rendering of an already committed
 * ajax response, will now be properly closed. This prevents errors about malformed XML.</li>
 * <li>Fixes the no-feedback problem when a {@link ViewExpiredException} occurs during an ajax request on a page which
 * is restricted by <code>web.xml</code> <code>&lt;security-constraint&gt;</code>. The enduser will now properly be
 * redirected to the login page instead of retrieving an ajax response with only a changed view state (and effectively
 * thus no visual feedback at all).</li>
 * </ul>
 * You can use the {@link org.omnifaces.util.Ajax} utility class to easily add callback scripts and arguments.
 * <p>
 * This partial view context is already registered by OmniFaces' own <code>faces-config.xml</code> and thus gets
 * auto-initialized when the OmniFaces JAR is bundled in a web application, so end-users do not need to register this
 * partial view context explicitly themselves.
 *
 * @author Bauke Scholtz
 * @since 1.2
 * @see OmniPartialViewContextFactory
 */
public class OmniPartialViewContext extends PartialViewContextWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String AJAX_DATA = "var OmniFaces=OmniFaces||{};OmniFaces.Ajax={data:%s};";
	private static final String ERROR_NO_OMNI_PVC = "There is no current OmniPartialViewContext instance.";

	// Variables ------------------------------------------------------------------------------------------------------

	private PartialViewContext wrapped;
	private Map<String, Object> arguments;
	private List<String> callbackScripts;
	private OmniPartialResponseWriter writer;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces partial view context around the given wrapped partial view context.
	 * @param wrapped The wrapped partial view context.
	 */
	public OmniPartialViewContext(PartialViewContext wrapped) {
		this.wrapped = wrapped;
		setCurrentInstance(this);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public PartialResponseWriter getPartialResponseWriter() {
		if (writer == null) {
			writer = new OmniPartialResponseWriter(this, super.getPartialResponseWriter());
		}

		return writer;
	}

	/**
	 * An override which checks if the web.xml security constraint has been triggered during this ajax request
	 * (which can happen when the session has been timed out) and if so, then perform a redirect to the originally
	 * requested page. Otherwise the enduser ends up with an ajax response containing only the new view state
	 * without any form of visual feedback.
	 */
	@Override
	public void processPartial(PhaseId phaseId) {
		if (phaseId == PhaseId.RENDER_RESPONSE) {
			String loginURL = WebXml.INSTANCE.getFormLoginPage();

			if (loginURL != null) {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				String loginViewId = normalizeViewId(facesContext, loginURL);

				if (loginViewId.equals(getViewId(facesContext))) {
					String originalURL = getRequestAttribute(facesContext, FORWARD_REQUEST_URI);

					if (originalURL != null) {
						try {
							invalidateSession(facesContext); // Prevent server from remembering security constraint fail caused by ajax.
							facesRedirect(getRequest(facesContext), getResponse(facesContext), originalURL); // This also clears cache.
						}
						catch (IOException e) {
							throw new FacesException(e);
						}

						return;
					}
				}
			}
		}

		super.processPartial(phaseId);
	}

	@Override // Necessary because this is missing in PartialViewContextWrapper (will be fixed in JSF 2.2).
	public void setPartialRequest(boolean partialRequest) {
		getWrapped().setPartialRequest(partialRequest);
	}

	@Override
	public PartialViewContext getWrapped() {
		return wrapped;
	}

	/**
	 * Add an argument to the partial response. This is as JSON object available by <code>OmniFaces.Ajax.data</code>.
	 * For supported argument value types, read {@link Json#encode(Object)}. If a given argument type is not supported,
	 * then an {@link IllegalArgumentException} will be thrown during end of render response.
	 * @param name The argument name.
	 * @param value The argument value.
	 */
	public void addArgument(String name, Object value) {
		if (arguments == null) {
			arguments = new HashMap<>();
		}

		arguments.put(name, value);
	}

	/**
	 * Add a callback script to the partial response. This script will be executed once the partial response is
	 * successfully retrieved at the client side.
	 * @param callbackScript The callback script to be added to the partial response.
	 */
	public void addCallbackScript(String callbackScript) {
		if (callbackScripts == null) {
			callbackScripts = new ArrayList<>();
		}

		callbackScripts.add(callbackScript);
	}

	/**
	 * Reset the partial response. This clears any JavaScript arguments and callbacks set any data written to the
	 * {@link PartialResponseWriter}.
	 * @see FullAjaxExceptionHandler
	 */
	public void resetPartialResponse() {
		if (writer != null) {
			writer.reset();
		}

		arguments = null;
		callbackScripts = null;
	}

	/**
	 * Close the partial response. If the writer is still in update phase, then end the update and the document. This
	 * fixes the Mojarra problem of incomplete ajax responses caused by exceptions during ajax render response.
	 * @see FullAjaxExceptionHandler
	 */
	public void closePartialResponse() {
		if (writer != null && writer.updating) {
			try {
				writer.endUpdate();
				writer.endDocument();
			}
			catch (IOException e) {
				throw new FacesException(e);
			}
		}
	}

	// Static ---------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current instance of the OmniFaces partial view context.
	 * @return The current instance of the OmniFaces partial view context.
	 * @throws IllegalStateException When there is no current instance of the OmniFaces partial view context. That can
	 * happen when the {@link OmniPartialViewContextFactory} is not properly registered, or when there's another
	 * {@link PartialViewContext} implementation which doesn't properly delegate through the wrapped instance.
	 */
	public static OmniPartialViewContext getCurrentInstance() {
		return getCurrentInstance(getContext());
	}

	/**
	 * Returns the current instance of the OmniFaces partial view context from the given faces context.
	 * @param context The faces context to obtain the current instance of the OmniFaces partial view context from.
	 * @return The current instance of the OmniFaces partial view context from the given faces context.
	 * @throws IllegalStateException When there is no current instance of the OmniFaces partial view context. That can
	 * happen when the {@link OmniPartialViewContextFactory} is not properly registered, or when there's another
	 * {@link PartialViewContext} implementation which doesn't properly delegate through the wrapped instance.
	 */
	public static OmniPartialViewContext getCurrentInstance(FacesContext context) {
		OmniPartialViewContext instance = getContextAttribute(context, OmniPartialViewContext.class.getName());

		if (instance != null) {
			return instance;
		}

		// Not found. Well, maybe the context attribute map was cleared for some reason. Get it once again.
		instance = unwrap(context.getPartialViewContext());

		if (instance != null) {
			setCurrentInstance(instance);
			return instance;
		}

		// Still not found. Well, maybe RichFaces is installed which doesn't use PartialViewContextWrapper.
		if (Hacks.isRichFacesInstalled()) {
			PartialViewContext pvc = Hacks.getRichFacesWrappedPartialViewContext();

			if (pvc != null) {
				instance = unwrap(pvc);

				if (instance != null) {
					setCurrentInstance(instance);
					return instance;
				}
			}
		}

		// Still not found. Well, it's end of story.
		throw new IllegalStateException(ERROR_NO_OMNI_PVC);
	}

	private static void setCurrentInstance(OmniPartialViewContext instance) {
		setContextAttribute(OmniPartialViewContext.class.getName(), instance);
	}

	private static OmniPartialViewContext unwrap(PartialViewContext context) {
		PartialViewContext unwrappedContext = context;

		while (!(unwrappedContext instanceof OmniPartialViewContext) && unwrappedContext instanceof PartialViewContextWrapper) {
			unwrappedContext = ((PartialViewContextWrapper) unwrappedContext).getWrapped();
		}

		if (unwrappedContext instanceof OmniPartialViewContext) {
			return (OmniPartialViewContext) unwrappedContext;
		}
		else {
			return null;
		}
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This OmniFaces partial response writer adds support for passing arguments to JavaScript context, executing
	 * oncomplete callback scripts, resetting the ajax response (specifically for {@link FullAjaxExceptionHandler}) and
	 * fixing incomlete XML response in case of exceptions.
	 * @author Bauke Scholtz
	 */
	private static class OmniPartialResponseWriter extends PartialResponseWriter {

		// Variables --------------------------------------------------------------------------------------------------

		private OmniPartialViewContext context;
		private PartialResponseWriter wrapped;
		private boolean updating;

		// Constructors -----------------------------------------------------------------------------------------------

		public OmniPartialResponseWriter(OmniPartialViewContext context, PartialResponseWriter wrapped) {
			super(wrapped);
			this.wrapped = wrapped; // We can't rely on getWrapped() due to MyFaces broken PartialResponseWriter.
			this.context = context;
		}

		// Overridden actions -----------------------------------------------------------------------------------------

		/**
		 * An override which remembers if we're updating or not.
		 * @see #endDocument()
		 * @see #reset()
		 */
		@Override
		public void startUpdate(String targetId) throws IOException {
			updating = true;
			wrapped.startUpdate(targetId);
		}

		/**
		 * An override which remembers if we're updating or not.
		 * @see #endDocument()
		 * @see #reset()
		 */
		@Override
		public void endUpdate() throws IOException {
			updating = false;
			wrapped.endUpdate();
		}

		/**
		 * An override which writes all {@link OmniPartialViewContext#arguments} as JSON to the extension and all
		 * {@link OmniPartialViewContext#callbackScripts} to the eval. It also checks if we're still updating, which
		 * may occur when MyFaces is used and an exception was thrown during rendering the partial response, and then
		 * gently closes the partial response which MyFaces has left open.
		 */
		@Override
		public void endDocument() throws IOException {
			if (updating) {
				// If endDocument() method is entered with updating=true, then it means that MyFaces is used and that
				// an exception was been thrown during ajax render response. The following calls will gently close the
				// partial response which MyFaces has left open.
				// Mojarra never enters endDocument() method with updating=true, this is handled in reset() method.
				endCDATA();
				endUpdate();
			}
			else {
				if (context.arguments != null) {
					startEval();
					write(String.format(AJAX_DATA, Json.encode(context.arguments)));
					endEval();
				}

				if (context.callbackScripts != null) {
					for (String callbackScript : context.callbackScripts) {
						startEval();
						write(callbackScript);
						endEval();
					}
				}
			}

			wrapped.endDocument();
		}

		// Custom actions ---------------------------------------------------------------------------------------------

		/**
		 * Reset the partial response writer. It checks if we're still updating, which may occur when Mojarra is used
		 * and an exception was thrown during rendering the partial response, and then gently closes the partial
		 * response which Mojarra has left open. This would clear the internal state of the wrapped partial response
		 * writer and thus make it ready for reuse without risking malformed XML.
		 */
		public void reset() {
			try {
				wrapped.flush(); // Note: this doesn't actually flush to writer, but clears internal state.

				if (updating) {
					// If reset() method is entered with updating=true, then it means that Mojarra is used and that
					// an exception was been thrown during ajax render response. The following calls will gently close
					// the partial response which Mojarra has left open.
					// MyFaces never enters reset() method with updating=true, this is handled in endDocument() method.
					wrapped.startError("");
					wrapped.endError();
					wrapped.endElement("partial-response"); // Don't use endDocument() as it will flush.
				}
			}
			catch (IOException e) {
				throw new FacesException(e);
			}
			finally {
				responseReset();
			}
		}

		// Delegate actions -------------------------------------------------------------------------------------------
		// Due to MyFaces broken PartialResponseWriter, which doesn't delegate to getWrapped() method, but instead to
		// the local variable wrapped, we can't use getWrapped() in our own PartialResponseWriter implementations.

		@Override
		public void startError(String errorName) throws IOException {
			wrapped.startError(errorName);
		}

		@Override
		public void startEval() throws IOException {
			wrapped.startEval();
		}

		@Override
		public void startExtension(Map<String, String> attributes) throws IOException {
			wrapped.startExtension(attributes);
		}

		@Override
		public void startInsertAfter(String targetId) throws IOException {
			wrapped.startInsertAfter(targetId);
		}

		@Override
		public void startInsertBefore(String targetId) throws IOException {
			wrapped.startInsertBefore(targetId);
		}

		@Override
		public void endError() throws IOException {
			wrapped.endError();
		}

		@Override
		public void endEval() throws IOException {
			wrapped.endEval();
		}

		@Override
		public void endExtension() throws IOException {
			wrapped.endExtension();
		}

		@Override
		public void endInsert() throws IOException {
			wrapped.endInsert();
		}

		@Override
		public void delete(String targetId) throws IOException {
			wrapped.delete(targetId);
		}

		@Override
		public void redirect(String url) throws IOException {
			wrapped.redirect(url);
		}

		@Override
		public void updateAttributes(String targetId, Map<String, String> attributes) throws IOException {
			wrapped.updateAttributes(targetId, attributes);
		}

	}

}