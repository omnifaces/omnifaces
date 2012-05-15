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
package org.omnifaces.component.output;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.omnifaces.util.Components;

/**
 * This component is used to catch the output from a Servlet resource and render it as output
 * to the JSF writer. In effect, this allows you to include both Servlets and JSP pages in e.g. Facelets.
 *
 * @author Arjan Tijms
 *
 */
@FacesComponent(ResourceInclude.COMPONENT_TYPE)
public class ResourceInclude extends UIComponentBase {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.ResourceInclude";

	public static final String COMPONENT_FAMILY = "org.omnifaces.component.output";

	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		Components.validateHasNoChildren(this);

		try {
			ExternalContext externalContext = context.getExternalContext();
			HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
			HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

			// Create dispatcher for the resource given by the component's page attribute.
			RequestDispatcher requestDispatcher = request.getRequestDispatcher((String) getAttributes().get("path"));

			// Catch the resource's output.
			CharResponseWrapper responseWrapper = new CharResponseWrapper(response);
			requestDispatcher.include(request, responseWrapper);

			// Write the output from the resource to the JSF response writer.
			context.getResponseWriter().write(responseWrapper.toString());
		}
		catch (ServletException e) {
			throw new IOException();
		}
	}

	static class CharResponseWrapper extends HttpServletResponseWrapper {

		private CharArrayWriter output;

		@Override
		public String toString() {
			return output.toString();
		}

		public CharResponseWrapper(HttpServletResponse response) {
			super(response);
			output = new CharArrayWriter();
		}

		public CharArrayWriter getCharWriter() {
			return output;
		}

		@Override
		public PrintWriter getWriter() {
			return new PrintWriter(output);
		}

		@Override
		public ServletOutputStream getOutputStream() {
			return new CharOutputStream(output);
		}

		public InputStream getInputStream() {
			return new ByteArrayInputStream(toString().getBytes());
		}
	}

	static class CharOutputStream extends ServletOutputStream {

		private Writer output;

		public CharOutputStream(Writer writer) {
			output = writer;
		}

		@Override
		public void write(int b) throws IOException {
			output.write(b);
		}

	}

}