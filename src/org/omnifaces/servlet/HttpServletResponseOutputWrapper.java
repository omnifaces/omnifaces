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
package org.omnifaces.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Convenience class for extending {@link HttpServletResponseWrapper} wherein the {@link ServletOutputStream} has to
 * be replaced by a custom implementation. This saves the developer from writing repeated {@link #getOutputStream()},
 * {@link #getWriter()} and {@link #flushBuffer()} boilerplate. All the developer has to do is to implement the
 * {@link #createOutputStream()} accordingly. This will in turn be used by both {@link #getOutputStream()} and
 * {@link #getWriter()}.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public abstract class HttpServletResponseOutputWrapper extends HttpServletResponseWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_GETOUTPUT_ALREADY_CALLED =
		"getOutputStream() has already been called on this response.";
	private static final String ERROR_GETWRITER_ALREADY_CALLED =
		"getWriter() has already been called on this response.";

	// Properties -----------------------------------------------------------------------------------------------------

	private ServletOutputStream output;
	private PrintWriter writer;
	private boolean passThrough;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link HttpServletResponseOutputWrapper} which wraps the given response.
	 * @param wrappedResponse The wrapped response.
	 */
	public HttpServletResponseOutputWrapper(HttpServletResponse wrappedResponse) {
		super(wrappedResponse);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the custom implementation of the {@link ServletOutputStream}.
	 * @return The custom implementation of the {@link ServletOutputStream}.
	 */
	protected abstract ServletOutputStream createOutputStream();

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (passThrough) {
			return super.getOutputStream();
		}
		
		
		if (writer != null) {
			throw new IllegalStateException(ERROR_GETWRITER_ALREADY_CALLED);
		}

		if (output == null) {
			output = createOutputStream();
		}

		return output;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (passThrough) {
			return super.getWriter();
		}
		
		
		if (output != null) {
			throw new IllegalStateException(ERROR_GETOUTPUT_ALREADY_CALLED);
		}

		if (writer == null) {
			writer = new PrintWriter(new OutputStreamWriter(createOutputStream(), getCharacterEncoding()), true);
		}

		return writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		super.flushBuffer();
		
		if (passThrough) {
			return;
		}

		if (writer != null) {
			writer.flush();
		}
		else if (output != null) {
			output.flush();
		}
	}

	/**
	 * Close the response body. This closes any created writer or output stream.
	 * @throws IOException When an I/O error occurs.
	 */
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
		}
		else if (output != null) {
			output.close();
		}
	}
	
	public boolean isPassThrough() {
    	return passThrough;
    }

	public void setPassThrough(boolean passThrough) {
    	this.passThrough = passThrough;
    }

}