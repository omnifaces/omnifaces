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
package org.omnifaces.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

/**
 * This filter will apply GZIP compression on responses whenever applicable. Whilst this is normally to be configured
 * at the servletcontainer side (e.g. <code>&lt;Context compression="on"&gt;</code> in Tomcat, or
 * <code>&lt;property name="compression" value="on"&gt;</code> in Glassfish), this filter allows a servletcontainer
 * independent way of configuring GZIP compression.
 * <p>
 * To get it to run, map this filter on the desired <code>&lt;url-pattern&gt;</code> or maybe even on the
 * <code>&lt;servlet-name&gt;</code> of the <code>FacesServlet</code>.
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;gzipResponseFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;org.omnifaces.filter.GzipResponseFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;gzipResponseFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * This filter supports two initialization parameters which needs to be placed in <code>&lt;filter&gt;</code> element
 * as follows which shows the default values:
 * <pre>
 * &lt;init-param&gt;
 *   &lt;description&gt;
 *     The threshold size in bytes. Must be a number between 0 and 9999. Defaults to 500.
 *   &lt;/description&gt;
 *   &lt;param-name&gt;threshold&lt;/param-name&gt;
 *   &lt;param-value&gt;500&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *   &lt;description&gt;
 *     The mimetypes which needs to be compressed. Must be a commaseparated string. Defaults to the below values.
 *   &lt;/description&gt;
 *   &lt;param-name&gt;mimetypes&lt;/param-name&gt;
 *   &lt;param-value&gt;
 *     text/plain, text/html, text/css, text/javascript, text/csv, text/rtf,
 *     application/xml, application/xhtml+xml, application/javascript, application/json
 *   &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * <p>
 * The default <code>threshold</code> is thus 500 bytes. This means that when the response is not larger than 500 bytes,
 * then it will not be compressed with GZIP. Only when it's larger than 500 bytes, then it will be compressed. A
 * threshold of between 150 and 1000 bytes is recommended due to overhead and latency of compression/decompression.
 * The value must be a number between 0 and 9999. A value larger than 2000 is not recommended.
 * <p>
 * The <code>mimetypes</code> represents a commaseparated string of mime types which needs to be compressed. It's
 * exactly that value which appears in the <code>Content-Type</code> header of the response. The in the above example
 * mentioned mime types are already the default values. Note that GZIP does not have any benefit when applied on
 * binary mimetypes like images, office documents, PDF files, etcetera. So setting it for them is not recommended.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public class GzipResponseFilter extends HttpFilter {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String INIT_PARAM_THRESHOLD = "threshold";
	private static final String INIT_PARAM_MIMETYPES = "mimetypes";

	private static final int DEFAULT_THRESHOLD = 500;
	private static final Set<String> DEFAULT_MIMETYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
		"text/plain", "text/html", "text/css", "text/javascript", "text/csv", "text/rtf",
		"application/xml", "application/xhtml+xml", "application/javascript", "application/json"
	)));

	private static final String ERROR_THRESHOLD = "The 'threshold' init param must be a number between 0 and 9999."
		+ " Encountered an invalid value of '%s'.";

	// Vars -----------------------------------------------------------------------------------------------------------

	private Set<String> mimetypes = DEFAULT_MIMETYPES;
	private int threshold = DEFAULT_THRESHOLD;

	/**
	 * Initializes the filter parameters.
	 */
	@Override
	public void init() throws ServletException {
		String threshold = getInitParameter(INIT_PARAM_THRESHOLD);

		if (threshold != null) {
			if (!threshold.matches("\\d{1,4}")) {
				throw new ServletException(String.format(ERROR_THRESHOLD, threshold));
			}
			else {
				this.threshold = Integer.valueOf(threshold);
			}
		}

		String mimetypes = getInitParameter(INIT_PARAM_MIMETYPES);

		if (mimetypes != null) {
			this.mimetypes = new HashSet<String>(Arrays.asList(mimetypes.split("\\s*,\\s*")));
		}
	}

	/**
	 * Perform the filtering job.
	 */
	@Override
	public void doFilter
		(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
			throws ServletException, IOException
	{
		if (acceptsGzip(request)) {
			GzipHttpServletResponse gzipResponse = new GzipHttpServletResponse(response, threshold, mimetypes);

			try {
				chain.doFilter(request, gzipResponse);
			}
			finally {
				gzipResponse.complete();
			}
		}
		else {
			chain.doFilter(request, response);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns whether the given request indicates that the client accepts GZIP encoding.
	 * @param request The request to be checked.
	 * @return <code>true</code> if the client accepts GZIP encoding, otherwise <code>false</code>.
	 */
	private static boolean acceptsGzip(HttpServletRequest request) {
		for (Enumeration<String> e = request.getHeaders("Accept-Encoding"); e.hasMoreElements();) {
			if (e.nextElement().contains("gzip")) {
				return true;
			}
		}

		return false;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This HTTP servlet response wrapper will return the {@link GzipServletOutputStream} on {@link #getWriter()} and
	 * {@link #getOutputStream()} and remember the content length (to avoid full content length being set instead of
	 * the gzipped content length). The {@link GzipServletOutputStream} will in turn decide whether to use GZIP or not
	 * based on the content type of the original response.
	 *
	 * @author Bauke Scholtz
	 */
	static class GzipHttpServletResponse extends HttpServletResponseWrapper {

		private GzipServletOutputStream gzipOutput;
		private ServletOutputStream output;
		private PrintWriter writer;
		private int contentLength;

		public GzipHttpServletResponse(HttpServletResponse wrapped, int threshold, Set<String> mimetypes)
			throws IOException
		{
			super(wrapped);
			this.gzipOutput = new GzipServletOutputStream(this, threshold, mimetypes);
		}

		@Override
		public void setContentLength(int contentLength) {
			this.contentLength = contentLength;
		}

		/**
		 * This should be called only when GZIP compression won't be used.
		 */
		void setSuperContentLength() {
			if (contentLength != 0) {
				super.setContentLength(contentLength);
			}
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (writer != null) {
				throw new IllegalStateException("getWriter() has already been called on this response.");
			}

			if (output == null) {
				output = gzipOutput;
			}

			return output;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (output != null) {
				throw new IllegalStateException("getOutputStream() has already been called on this response.");
			}

			if (writer == null) {
				writer = new PrintWriter(new OutputStreamWriter(gzipOutput, getCharacterEncoding()), true);
			}

			return writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			if (writer != null) {
				writer.flush();
			}
			else if (output != null) {
				output.flush();
			}
		}

		/**
		 * Calling complete() after writing is mandatory as this forces a flush of threshold buffer when necessary.
		 */
		void complete() throws IOException {
			if (writer != null) {
				writer.close();
			}
			else if (output != null) {
				output.close();
			}
		}
	}

	/**
	 * This servlet output stream will switch to GZIP compression and set the appropriate response headers when the
	 * given threshold is exceeded and one of the given mimetypes is matched.
	 *
	 * @author Bauke Scholtz
	 */
	static class GzipServletOutputStream extends ServletOutputStream {

		private GzipHttpServletResponse response;
		private ServletResponse originalResponse;
		private byte[] thresholdBuffer;
		private Set<String> mimetypes;
		private int thresholdLength;
		private OutputStream output;
		private ServletOutputStream originalOutput;

		public GzipServletOutputStream(GzipHttpServletResponse response, int threshold, Set<String> mimetypes)
			throws IOException
		{
			this.response = response;
			this.originalResponse = response.getResponse();
			this.thresholdBuffer = (threshold > 0) ? new byte[threshold] : null;
			this.mimetypes = mimetypes;
			this.originalOutput = originalResponse.getOutputStream();
		}

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
			if (length == 0) {
				return;
			}

			if (thresholdBuffer != null) {
				if ((length - offset) <= (thresholdBuffer.length - thresholdLength)) {
					System.arraycopy(bytes, offset, thresholdBuffer, thresholdLength, length);
					thresholdLength += length;
					return;
				}
				else {
					// Threshold buffer has exceeded. Now use GZIP if possible.
					writeOutput(thresholdBuffer, 0, thresholdLength);
					thresholdBuffer = null;
				}
			}

			writeOutput(bytes, offset, length);
		}

		private void writeOutput(byte[] bytes, int offset, int length) throws IOException {
			if (output == null) {
				String contentType = response.getContentType();

				if (contentType == null || !mimetypes.contains(contentType.split(";", 2)[0])) {
					// Content type of null is unexpected, but you never know with starters.
					// The split on semicolon gets rid of content type attributes like charset.

					response.setSuperContentLength();
					output = originalOutput;
				}
				else {
					response.addHeader("Content-Encoding", "gzip");
					String vary = response.getHeader("Vary");
					response.setHeader("Vary", (vary != null && !vary.equals("*") ? vary + "," : "") + "Accept-Encoding");
					output = new GZIPOutputStream(originalOutput);
				}
			}

			output.write(bytes, offset, length);
		}

		@Override
		public void flush() throws IOException {
			if (output != null) {
				output.flush();
			}
		}

		@Override
		public void close() throws IOException {
			if (thresholdBuffer != null) {
				// Threshold buffer hasn't exceeded. Stick to normal streaming.
				originalResponse.setContentLength(thresholdLength);
				originalOutput.write(thresholdBuffer, 0, thresholdLength);
				originalOutput.close();
			}
			else if (output != null) {
				output.close();
			}
		}

	}

}