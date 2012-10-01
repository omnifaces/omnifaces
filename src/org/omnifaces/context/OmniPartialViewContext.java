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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;

import org.omnifaces.config.WebXml;
import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;
import org.omnifaces.util.Ajax;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Json;

/**
 * This OmniFaces partial view context extends and improves the standard partial view context as follows:
 * <ul>
 * <li>Support for executing callback scripts.</li>
 * <li>Support for adding arguments to ajax response.</li>
 * <li>Buffers the response until <code>javax.faces.FACELETS_BUFFER_SIZE</code> (regardless of flush calls).</li>
 * <li>Resettable buffer so that exceptions during ajax rendering can be properly handled.</li>
 * <li>Fixes the no-feedback problem when ViewExpiredException occurs during ajax request on a restricted page.</li>
 * </ul>
 * You can use the {@link Ajax} utility class to easily add callback scripts and arguments.
 * <p>
 * This partial view context is already registered by OmniFaces' own <tt>faces-config.xml</tt> and thus gets
 * auto-initialized when the OmniFaces JAR is bundled in a webapp, so end-users do not need to register this partial
 * view context explicitly themselves.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public class OmniPartialViewContext extends PartialViewContextWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String AJAX_DATA = "OmniFaces=OmniFaces||{};OmniFaces.Ajax={data:%s};";
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
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public PartialResponseWriter getPartialResponseWriter() {
		if (writer == null) {
			writer = new OmniPartialResponseWriter(this, wrapped.getPartialResponseWriter());
		}

		return writer;
	}

	@Override
	public void setPartialRequest(boolean isPartialRequest) {
		wrapped.setPartialRequest(isPartialRequest);
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
			arguments = new HashMap<String, Object>(3);
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
			callbackScripts = new ArrayList<String>(3);
		}

		callbackScripts.add(callbackScript);
	}

	/**
	 * Reset the partial response. This clears any JavaScript arguments and callbacks set any data written to the
	 * {@link PartialResponseWriter}.
	 */
	public void resetPartialResponse() {
		if (writer != null) {
			writer.reset();
		}

		arguments = null;
		callbackScripts = null;
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
		PartialViewContext context = FacesContext.getCurrentInstance().getPartialViewContext();

		while (!(context instanceof OmniPartialViewContext) && context instanceof PartialViewContextWrapper) {
			context = ((PartialViewContextWrapper) context).getWrapped();
		}

		if (context instanceof OmniPartialViewContext) {
			return (OmniPartialViewContext) context;
		}
		else {
			throw new IllegalStateException(ERROR_NO_OMNI_PVC);
		}
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This OmniFaces partial response writer adds support for passing arguments to JavaScript context, executing
	 * oncomplete callback scripts, resetting the ajax response (specifically for {@link FullAjaxExceptionHandler}).
	 * @author Bauke Scholtz
	 */
	private static class OmniPartialResponseWriter extends PartialResponseWriter {

		// Variables --------------------------------------------------------------------------------------------------

		private OmniPartialViewContext context;

		// Constructors -----------------------------------------------------------------------------------------------

		public OmniPartialResponseWriter(OmniPartialViewContext context, PartialResponseWriter writer) {
			super(new ResettableBufferedResponseWriter(writer, Faces.getResponseBufferSize()));
			this.context = context;
		}

		// Actions ----------------------------------------------------------------------------------------------------

		/**
		 * An override which checks if the web.xml security constraint has been triggered during this ajax request
		 * (which can happen when the session has been timed out) and if so, then perform a redirect to the originally
		 * requested page. Otherwise the enduser ends up with an ajax response containing only the new view state
		 * without any form of visual feedback.
		 */
		@Override
		public void startDocument() throws IOException {
			super.startDocument();
			String loginURL = WebXml.INSTANCE.getFormLoginPage();

			if (loginURL != null) {
				String loginViewId = Faces.normalizeViewId(loginURL);

				if (loginViewId.equals(Faces.getViewId())) {
					String originalURL = Faces.getRequestAttribute("javax.servlet.forward.request_uri");

					if (originalURL != null) {
						redirect(originalURL);
					}
				}
			}
		}

		/**
		 * An override which writes all {@link OmniPartialViewContext#arguments} as JSON to the extension and all
		 * {@link OmniPartialViewContext#callbackScripts} to the eval.
		 */
		@Override
		public void endDocument() throws IOException {
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

			super.endDocument();
			getWrapped().close();
		}

		/**
		 * Calls {@link PartialResponseWriter#endDocument()} and then {@link ResettableBufferedResponseWriter#reset()}.
		 */
		public void reset() {
			try {
				super.endDocument(); // Clears any internal state of this PartialResponseWriter.
			}
			catch (IOException e) {
				throw new FacesException(e);
			}

			getWrapped().reset();
		}

		@Override
		public ResettableBufferedResponseWriter getWrapped() {
			return (ResettableBufferedResponseWriter) super.getWrapped();
		}

		// Nested classes ---------------------------------------------------------------------------------------------

		/**
		 * This response writer buffers the entire response body until buffer size is reached, regardless of flush,
		 * which allows us to perform a reset before the buffer size is reached.
		 * @author Bauke Scholtz
		 */
		private static class ResettableBufferedResponseWriter extends ResponseWriterWrapper {

			// Variables ----------------------------------------------------------------------------------------------

			private ResponseWriter wrapped;
			private int bufferSize;
			private CharArrayWriter buffer;
			private ResponseWriter writer;

			// Constructors -------------------------------------------------------------------------------------------

			public ResettableBufferedResponseWriter(ResponseWriter wrapped, int bufferSize) {
				this.wrapped = wrapped;
				this.bufferSize = bufferSize;
				this.buffer = new CharArrayWriter(bufferSize);
			}

			// Actions ------------------------------------------------------------------------------------------------

			public void reset() {
				writer = null;
				buffer.reset();
			}

			@Override
			public void flush() throws IOException {
				if (buffer.size() > bufferSize) { // TODO: buffer size is actually measured in bytes, not chars, right?
					wrapped.write(buffer.toCharArray());
					buffer.reset();
					super.flush();
				}
			}

			@Override
			public void close() throws IOException {
				wrapped.write(buffer.toCharArray());
				super.close();
			}

			@Override
			public ResponseWriter getWrapped() {
				if (writer == null) {
					writer = wrapped.cloneWithWriter(buffer);
				}

				return writer;
			}

		}

	}

}