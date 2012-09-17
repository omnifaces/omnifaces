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

import java.io.IOException;
import java.io.StringWriter;
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

import org.omnifaces.exceptionhandler.FullAjaxExceptionHandler;

/**
 * This OmniFaces partial view context ... [TBD].
 * <p>
 * This partial view context is already registered by OmniFaces' own <tt>faces-config.xml</tt> and thus gets
 * auto-initialized when the OmniFaces JAR is bundled in a webapp, so end-users do not need to register this partial
 * view context explicitly themselves.
 *
 * @author Bauke Scholtz
 */
public class OmniPartialViewContext extends PartialViewContextWrapper {

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
		this.arguments = new HashMap<String, Object>(3);
		this.callbackScripts = new ArrayList<String>(3);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public PartialResponseWriter getPartialResponseWriter() {
		if (writer == null) {
			writer = new OmniPartialResponseWriter(this, wrapped.getPartialResponseWriter());
		}

		return writer;
	}

	/**
	 * Reset the partial response. This clears any JavaScript arguments and callbacks set any data written to the
	 * {@link PartialResponseWriter}.
	 */
	public void resetPartialResponse() {
		if (writer != null) {
			writer.reset();
		}

		arguments.clear();
		callbackScripts.clear();
	}

	@Override
	public void setPartialRequest(boolean isPartialRequest) {
		wrapped.setPartialRequest(isPartialRequest);
	}

	@Override
	public PartialViewContext getWrapped() {
		return wrapped;
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
			throw new IllegalStateException("There is no current OmniPartialViewContext instance.");
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
			super(new BufferingResponseWriter(writer));
			this.context = context;
		}

		// Actions ----------------------------------------------------------------------------------------------------

		@Override
		public void endDocument() throws IOException {
//			if (!context.arguments.isEmpty()) {
//				// Convert to JSON?
//			}
//
//			for (String callbackScript : context.callbackScripts) {
//				startEval();
//				write(callbackScript);
//				endEval();
//			}

			super.endDocument();
			getWrapped().close();
		}

		/**
		 * Calls {@link PartialResponseWriter#endDocument()} and then {@link BufferingResponseWriter#reset()}.
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
		public BufferingResponseWriter getWrapped() {
			return (BufferingResponseWriter) super.getWrapped();
		}

		// Nested classes ---------------------------------------------------------------------------------------------

		/**
		 * This response writer buffers the entire response body until {@link #close()} is called.
		 * @author Bauke Scholtz
		 */
		private static class BufferingResponseWriter extends ResponseWriterWrapper {

			// Variables ----------------------------------------------------------------------------------------------

			private ResponseWriter wrapped;
			private ResponseWriter writer;
			private StringWriter buffer;

			// Constructors -------------------------------------------------------------------------------------------

			public BufferingResponseWriter(ResponseWriter wrapped) {
				this.wrapped = wrapped;
				this.buffer = new StringWriter();
			}

			// Actions ------------------------------------------------------------------------------------------------

			public void reset() {
				writer = null;
			}

			@Override
			public void close() throws IOException {
				super.flush();
				wrapped.write(buffer.toString());
				super.close();
			}

			@Override
			public ResponseWriter getWrapped() {
				if (writer == null) {
					buffer = new StringWriter();
					writer = wrapped.cloneWithWriter(buffer);
				}

				return writer;
			}

		}

	}

}