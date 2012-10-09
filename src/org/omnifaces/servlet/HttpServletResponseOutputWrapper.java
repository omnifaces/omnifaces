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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Convenience class for extending {@link HttpServletResponseWrapper} wherein the {@link ServletOutputStream} has to
 * be replaced by a custom implementation. This saves the developer from writing repeated {@link #getOutputStream()},
 * {@link #getWriter()} and {@link #flushBuffer()} boilerplate. All the developer has to do is to implement the
 * {@link #createOutputStream()} accordingly. This will in turn be used by both {@link #getOutputStream()} and
 * {@link #getWriter()}. The boolean property <code>passThrough</code>, which defaults to <code>false</code> also
 * enables the developer to control whether to pass through to the wrapped {@link ServletOutputStream} or not.
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
	private Resettable buffer;
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
	 * Returns the custom implementation of the servlet response {@link OutputStream}.
	 * @return The custom implementation of the servlet response {@link OutputStream}.
	 */
	protected abstract OutputStream createOutputStream();

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (passThrough) {
			return super.getOutputStream();
		}

		if (writer != null) {
			throw new IllegalStateException(ERROR_GETWRITER_ALREADY_CALLED);
		}

		if (output == null) {
			buffer = new ResettableBufferedOutputStream(createOutputStream(), getBufferSize());
			output = new ServletOutputStream() {
				@Override
				public void write(int b) throws IOException {
					((OutputStream) buffer).write(b);
				}
				@Override
				public void flush() throws IOException {
					((OutputStream) buffer).flush();
				}
				@Override
				public void close() throws IOException {
					((OutputStream) buffer).close();
				}
			};
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
			buffer = new ResettableBufferedWriter(new OutputStreamWriter(createOutputStream(),
				getCharacterEncoding()), getCharacterEncoding(), getBufferSize());
			writer = new PrintWriter((Writer) buffer);
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

	@Override
	public void reset() {
		super.reset();

		if (buffer != null) {
			buffer.reset();
		}
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns whether the writing has to be passed through to the wrapped {@link ServletOutputStream}.
	 * @return <code>true</code>, if the writing has to be passed through to the wrapped {@link ServletOutputStream},
	 * otherwise <code>false</code>.
	 */
	public boolean isPassThrough() {
    	return passThrough;
    }

	/**
	 * Sets whether the writing has to be passed through to the wrapped {@link ServletOutputStream}.
	 * @param passThrough set to <code>true</code> if the writing has to be passed through to the wrapped
	 * {@link ServletOutputStream}.
	 */
	public void setPassThrough(boolean passThrough) {
    	this.passThrough = passThrough;
    }

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * @author Bauke Scholtz
	 */
	private static interface Resettable {

		/**
		 * Perform a reset.
		 */
		void reset();

	}

	/**
	 * @author Bauke Scholtz
	 */
	private static class ResettableBufferedOutputStream extends OutputStream implements Resettable {

		// Variables ------------------------------------------------------------------------------------------------------

		private OutputStream output;
		private ByteArrayOutputStream buffer;
		private int bufferSize;
		private int writtenBytes;

		// Constructors ---------------------------------------------------------------------------------------------------

		/**
		 * Construct a new resettable buffered output stream which wraps the given output stream and forcibly buffers
		 * everything until the given buffer size, regardless of flush calls.
		 * @param output The wrapped output stream .
		 * @param bufferSize The buffer size.
		 */
		public ResettableBufferedOutputStream(OutputStream output, int bufferSize) {
			this.output = output;
			this.buffer = new ByteArrayOutputStream(bufferSize);
			this.bufferSize = bufferSize;
		}

		// Actions --------------------------------------------------------------------------------------------------------

		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) b }, 0, 1);
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			write(bytes, 0, bytes.length);
		}

		@Override
		public void write(byte[] bytes, int offset, int length) throws IOException {
			if (buffer != null) {
				if ((writtenBytes += length) > bufferSize) {
					output.write(buffer.toByteArray());
					buffer = null;
				}
				else {
					buffer.write(bytes, offset, length);
					return;
				}
			}

			if (buffer == null) {
				output.write(bytes, offset, length);
			}
		}

		@Override
		public void reset() {
			buffer = new ByteArrayOutputStream(bufferSize);
			writtenBytes = 0;
		}

		@Override
		public void flush() throws IOException {
			if (buffer == null) {
				output.flush();
			}
		}

		@Override
		public void close() throws IOException {
			if (buffer != null) {
				output.write(buffer.toByteArray());
				buffer = null;
			}

			output.close();
		}

	}

	/**
	 * @author Bauke Scholtz
	 */
	private static class ResettableBufferedWriter extends Writer implements Resettable {

		// Variables --------------------------------------------------------------------------------------------------

		private Writer writer;
		private Charset charset;
		private CharArrayWriter buffer;
		private int bufferSize;
		private int writtenBytes;

		// Constructors -----------------------------------------------------------------------------------------------

		/**
		 * Construct a new resettable buffered writer which wraps the given writer, uses the given character encoding
		 * to measure the amount of written bytes and forcibly buffers everything until the given buffer size,
		 * regardless of flush calls.
		 * @param writer The wrapped writer.
		 * @param characterEncoding The character encoding.
		 * @param bufferSize The buffer size.
		 */
		public ResettableBufferedWriter(Writer writer, String characterEncoding, int bufferSize) {
			this.writer = writer;
			this.charset = Charset.forName(characterEncoding);
			this.buffer = new CharArrayWriter(bufferSize);
			this.bufferSize = bufferSize;
		}

		// Actions ----------------------------------------------------------------------------------------------------

		@Override
		public void write(char[] chars, int offset, int length) throws IOException {
			if (buffer != null) {
				if ((writtenBytes += charset.encode(CharBuffer.wrap(chars)).limit()) > bufferSize) {
					writer.write(buffer.toCharArray());
					buffer = null;
				}
				else {
					buffer.write(chars, offset, length);
					return;
				}
			}

			if (buffer == null) {
				writer.write(chars, offset, length);
			}
		}

		@Override
		public void reset() {
			buffer = new CharArrayWriter(bufferSize);
			writtenBytes = 0;
		}

		@Override
		public void flush() throws IOException {
			if (buffer == null) {
				writer.flush();
			}
		}

		@Override
		public void close() throws IOException {
			if (buffer != null) {
				writer.write(buffer.toCharArray());
				buffer = null;
			}

			writer.close();
		}

	}

}