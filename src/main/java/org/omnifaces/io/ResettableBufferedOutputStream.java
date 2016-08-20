/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This resettable buffered output stream will buffer everything until the given buffer size, regardless of flush calls.
 * Only when the buffer size is exceeded, or when close is called, then the buffer will be actually flushed.
 * <p>
 * There is a {@link #reset()} method which enables the developer to reset the buffer, as long as it's not flushed yet,
 * which can be determined by {@link #isResettable()}.
 *
 * @author Bauke Scholtz
 * @see ResettableBufferedWriter
 */
public class ResettableBufferedOutputStream extends OutputStream implements ResettableBuffer {

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
		this.bufferSize = bufferSize;
		buffer = new ByteArrayOutputStream(bufferSize);
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
			writtenBytes += (length - offset);

			if (writtenBytes > bufferSize) {
				output.write(buffer.toByteArray());
				output.write(bytes, offset, length);
				buffer = null;
			}
			else {
				buffer.write(bytes, offset, length);
			}
		}
		else {
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

	@Override
	public boolean isResettable() {
		return buffer != null;
	}

}