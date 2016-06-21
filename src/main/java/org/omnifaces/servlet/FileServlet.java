/*
 * Copyright 2015 OmniFaces.
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

import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.encodeURI;
import static org.omnifaces.util.Utils.encodeURL;
import static org.omnifaces.util.Utils.startsWithOneOf;
import static org.omnifaces.util.Utils.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.filter.GzipResponseFilter;
import org.omnifaces.util.Servlets;

/**
 * <p>
 * The well known "<a href="http://balusc.omnifaces.org/2009/02/fileservlet-supporting-resume-and.html">BalusC FileServlet</a>",
 * as an abstract template, slightly refactored, rewritten and modernized with a.o. fast NIO stuff instead of legacy
 * RandomAccessFile. GZIP support is stripped off as that can be done application wide via {@link GzipResponseFilter}.
 * <p>
 * This servlet properly deals with <code>ETag</code>, <code>If-None-Match</code> and <code>If-Modified-Since</code>
 * caching requests, hereby improving browser caching. This servlet also properly deals with <code>Range</code> and
 * <code>If-Range</code> ranging requests (<a href="https://tools.ietf.org/html/rfc7233">RFC7233</a>), which is required
 * by most media players for proper audio/video streaming, and by webbrowsers and for a proper resume of an paused
 * download, and by download accelerators to be able to request smaller parts simultaneously. This servlet is ideal when
 * you have large files like media files placed outside the web application and you can't use the default servlet.
 *
 * <h3>Usage</h3>
 * <p>
 * Just extend this class and override the {@link #getFile(HttpServletRequest)} method to return the desired file. If
 * you want to trigger a HTTP 400 "Bad Request" error, simply throw {@link IllegalArgumentException}. If you want to
 * trigger a HTTP 404 "Not Found" error, simply return <code>null</code>, or a non-existent file.
 * <p>
 * Here's a concrete example which serves it via an URL like <code>/media/foo.ext</code>:
 *
 * <pre>
 * &#64;WebServlet("/media/*")
 * public class MediaFileServlet extends FileServlet {
 *
 *     private File folder;
 *
 *     &#64;Override
 *     public void init() throws ServletException {
 *         folder = new File("/var/webapp/media");
 *     }
 *
 *     &#64;Override
 *     protected File getFile(HttpServletRequest request) throws IllegalArgumentException {
 *         String pathInfo = request.getPathInfo();
 *
 *         if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
 *             throw new IllegalArgumentException();
 *         }
 *
 *         return new File(folder, pathInfo);
 *     }
 *
 * }
 * </pre>
 * <p>
 * You can embed it in e.g. HTML5 video tag as below:
 * <pre>
 * &lt;video src="#{request.contextPath}/media/video.mp4" controls="controls" /&gt;
 * </pre>
 *
 * <h3>Customizing <code>FileServlet</code></h3>
 * <p>
 * If more fine grained control is desired for handling "file not found" error, determining the cache expire time, the
 * content type, whether the file should be supplied as an attachment and the attachment's file name, then the developer
 * can opt to override one or more of the following protected methods:
 * <ul>
 * <li>{@link #handleFileNotFound(HttpServletRequest, HttpServletResponse)}
 * <li>{@link #getExpireTime(HttpServletRequest, File)}
 * <li>{@link #getContentType(HttpServletRequest, File)}
 * <li>{@link #isAttachment(HttpServletRequest, String)}
 * <li>{@link #getAttachmentName(HttpServletRequest, File)}
 * </ul>
 *
 * <p><strong>See also</strong>:
 * <ul>
 * <li><a href="http://stackoverflow.com/q/13588149/157882">How to stream audio/video files such as MP3, MP4, AVI, etc using a Servlet</a>
 * <li><a href="http://stackoverflow.com/a/29991447/157882">Abstract template for a static resource servlet</a>
 * </ul>
 *
 * @author Bauke Scholtz
 * @since 2.2
 */
public abstract class FileServlet extends HttpServlet {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	private static final Long DEFAULT_EXPIRE_TIME_IN_SECONDS = TimeUnit.DAYS.toSeconds(30);
	private static final long ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1);
	private static final String ETAG = "W/\"%s-%s\"";
	private static final Pattern RANGE_PATTERN = Pattern.compile("^bytes=[0-9]*-[0-9]*(,[0-9]*-[0-9]*)*$");
	private static final String CONTENT_DISPOSITION_HEADER = "%s;filename=\"%2$s\"; filename*=UTF-8''%2$s";
	private static final String MULTIPART_BOUNDARY = UUID.randomUUID().toString();

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response, true);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response, false);
	}

	private void doRequest(HttpServletRequest request, HttpServletResponse response, boolean head) throws IOException {
		response.reset();
		Resource resource;

		try {
			resource = new Resource(getFile(request));
		}
		catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (resource.file == null) {
			handleFileNotFound(request, response);
			return;
		}

		if (preconditionFailed(request, resource)) {
			response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
			return;
		}

		setCacheHeaders(response, resource, getExpireTime(request, resource.file));

		if (notModified(request, resource)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		List<Range> ranges = getRanges(request, resource);

		if (ranges == null) {
			response.setHeader("Content-Range", "bytes */" + resource.length);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return;
		}

		if (!ranges.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		}
		else {
			ranges.add(new Range(0, resource.length - 1)); // Full content.
		}

		String contentType = setContentHeaders(request, response, resource, ranges);

		if (head) {
			return;
		}

		writeContent(response, resource, ranges, contentType);
	}

	/**
	 * Returns the file associated with the given HTTP servlet request.
	 * If this method throws {@link IllegalArgumentException}, then the servlet will return a HTTP 400 error.
	 * If this method returns <code>null</code>, or if {@link File#isFile()} returns <code>false</code>, then the
	 * servlet will invoke {@link #handleFileNotFound(HttpServletRequest, HttpServletResponse)}.
	 * @param request The involved HTTP servlet request.
	 * @return The file associated with the given HTTP servlet request.
	 * @throws IllegalArgumentException When the request is mangled in such way that it's not recognizable as a valid
	 * file request. The servlet will then return a HTTP 400 error.
	 */
	protected abstract File getFile(HttpServletRequest request) throws IllegalArgumentException;

	/**
	 * Handles the case when the file is not found.
	 * <p>
	 * The default implementation sends a HTTP 404 error.
	 * @param request The involved HTTP servlet request.
	 * @param response The involved HTTP servlet response.
	 * @throws IOException When something fails at I/O level.
	 * @since 2.3
	 */
	protected void handleFileNotFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Returns how long the resource may be cached by the client before it expires, in seconds.
	 * <p>
	 * The default implementation returns 30 days in seconds.
	 * @param request The involved HTTP servlet request.
	 * @param file The involved file.
	 * @return The client cache expire time in seconds (not milliseconds!).
	 */
	protected long getExpireTime(HttpServletRequest request, File file) {
		return DEFAULT_EXPIRE_TIME_IN_SECONDS;
	}

	/**
	 * Returns the content type associated with the given HTTP servlet request and file.
	 * <p>
	 * The default implementation delegates {@link File#getName()} to {@link ServletContext#getMimeType(String)} with a
	 * fallback default value of <code>application/octet-stream</code>.
	 * @param request The involved HTTP servlet request.
	 * @param file The involved file.
	 * @return The content type associated with the given HTTP servlet request and file.
	 */
	protected String getContentType(HttpServletRequest request, File file) {
		return coalesce(request.getServletContext().getMimeType(file.getName()), "application/octet-stream");
	}

	/**
	 * Returns <code>true</code> if we must force a "Save As" dialog based on the given HTTP servlet request and content
	 * type as obtained from {@link #getContentType(HttpServletRequest, File)}.
	 * <p>
	 * The default implementation will return <code>true</code> if the content type does <strong>not</strong> start with
	 * <code>text</code> or <code>image</code>, and the <code>Accept</code> request header is either <code>null</code>
	 * or does not match the given content type.
	 * @param request The involved HTTP servlet request.
	 * @param contentType The content type of the involved file.
	 * @return <code>true</code> if we must force a "Save As" dialog based on the given HTTP servlet request and content
	 * type.
	 */
	protected boolean isAttachment(HttpServletRequest request, String contentType) {
		String accept = request.getHeader("Accept");
		return !startsWithOneOf(contentType, "text", "image") && (accept == null || !accepts(accept, contentType));
	}

	/**
	 * Returns the file name to be used in <code>Content-Disposition</code> header.
	 * This does not need to be URL-encoded as this will be taken care of.
	 * <p>
	 * The default implementation returns {@link File#getName()}.
	 * @param request The involved HTTP servlet request.
	 * @param file The involved file.
	 * @return The file name to be used in <code>Content-Disposition</code> header.
	 * @since 2.3
	 */
	protected String getAttachmentName(HttpServletRequest request, File file) {
		return file.getName();
	}

	// Sub-actions ----------------------------------------------------------------------------------------------------

	/**
	 * Returns true if it's a conditional request which must return 412.
	 */
	private boolean preconditionFailed(HttpServletRequest request, Resource resource) {
		String match = request.getHeader("If-Match");
		long unmodified = request.getDateHeader("If-Unmodified-Since");
		return (match != null) ? !matches(match, resource.eTag) : (unmodified != -1 && modified(unmodified, resource.lastModified));
	}

	/**
	 * Set cache headers.
	 */
	private void setCacheHeaders(HttpServletResponse response, Resource resource, long expires) {
		Servlets.setCacheHeaders(response, expires);
		response.setHeader("ETag", resource.eTag);
		response.setDateHeader("Last-Modified", resource.lastModified);
	}

	/**
	 * Returns true if it's a conditional request which must return 304.
	 */
	private boolean notModified(HttpServletRequest request, Resource resource) {
		String noMatch = request.getHeader("If-None-Match");
		long modified = request.getDateHeader("If-Modified-Since");
		return (noMatch != null) ? matches(noMatch, resource.eTag) : (modified != -1 && !modified(modified, resource.lastModified));
	}

	/**
	 * Get requested ranges. If this is null, then we must return 416. If this is empty, then we must return full file.
	 */
	private List<Range> getRanges(HttpServletRequest request, Resource resource) {
		List<Range> ranges = new ArrayList<>(1);
		String rangeHeader = request.getHeader("Range");

		if (rangeHeader == null) {
			return ranges;
		}
		else if (!RANGE_PATTERN.matcher(rangeHeader).matches()) {
			return null; // Syntax error.
		}

		String ifRange = request.getHeader("If-Range");

		if (ifRange != null && !ifRange.equals(resource.eTag)) {
			try {
				long ifRangeTime = request.getDateHeader("If-Range");

				if (ifRangeTime != -1 && modified(ifRangeTime, resource.lastModified)) {
					return ranges;
				}
			}
			catch (IllegalArgumentException ifRangeHeaderIsInvalid) {
				return ranges;
			}
		}

		for (String rangeHeaderPart : rangeHeader.split("=")[1].split(",")) {
			Range range = parseRange(rangeHeaderPart, resource.length);

			if (range == null) {
				return null; // Logic error.
			}

			ranges.add(range);
		}

		return ranges;
	}

	/**
	 * Parse range header part. Returns null if there's a logic error (i.e. start after end).
	 */
	private Range parseRange(String range, long length) {
		long start = sublong(range, 0, range.indexOf('-'));
		long end = sublong(range, range.indexOf('-') + 1, range.length());

		if (start == -1) {
			start = length - end;
			end = length - 1;
		}
		else if (end == -1 || end > length - 1) {
			end = length - 1;
		}

		if (start > end) {
			return null; // Logic error.
		}

		return new Range(start, end);
	}

	/**
	 * Set content headers.
	 */
	private String setContentHeaders(HttpServletRequest request, HttpServletResponse response, Resource resource, List<Range> ranges) {
		String contentType = getContentType(request, resource.file);
		String disposition = isAttachment(request, contentType) ? "attachment" : "inline";
		String filename = encodeURI(getAttachmentName(request, resource.file));
		response.setHeader("Content-Disposition", String.format(CONTENT_DISPOSITION_HEADER, disposition, filename));
		response.setHeader("Accept-Ranges", "bytes");

		if (ranges.size() == 1) {
			Range range = ranges.get(0);
			response.setContentType(contentType);
			response.setHeader("Content-Length", String.valueOf(range.length));

			if (response.getStatus() == HttpServletResponse.SC_PARTIAL_CONTENT) {
				response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + resource.length);
			}
		}
		else {
			response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
		}

		return contentType;
	}

	/**
	 * Write given file to response with given content type and ranges.
	 */
	private void writeContent(HttpServletResponse response, Resource resource, List<Range> ranges, String contentType) throws IOException, FileNotFoundException {
		ServletOutputStream output = response.getOutputStream();

		if (ranges.size() == 1) {
			Range range = ranges.get(0);
			stream(resource.file, output, range.start, range.length);
		}
		else {
			for (Range range : ranges) {
				output.println();
				output.println("--" + MULTIPART_BOUNDARY);
				output.println("Content-Type: " + contentType);
				output.println("Content-Range: bytes " + range.start + "-" + range.end + "/" + resource.length);
				stream(resource.file, output, range.start, range.length);
			}

			output.println();
			output.println("--" + MULTIPART_BOUNDARY + "--");
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if the given match header matches the given ETag value.
	 */
	private static boolean matches(String matchHeader, String eTag) {
		String[] matchValues = matchHeader.split("\\s*,\\s*");
		Arrays.sort(matchValues);
		return Arrays.binarySearch(matchValues, eTag) > -1
			|| Arrays.binarySearch(matchValues, "*") > -1;
	}

	/**
	 * Returns true if the given modified header is older than the given last modified value.
	 */
	private static boolean modified(long modifiedHeader, long lastModified) {
		return (modifiedHeader + ONE_SECOND_IN_MILLIS <= lastModified); // That second is because the header is in seconds, not millis.
	}

	/**
	 * Returns a substring of the given string value from the given begin index to the given end index as a long.
	 * If the substring is empty, then -1 will be returned.
	 */
	private static long sublong(String value, int beginIndex, int endIndex) {
		String substring = value.substring(beginIndex, endIndex);
		return substring.isEmpty() ? -1 : Long.parseLong(substring);
	}

	/**
	 * Returns true if the given accept header accepts the given value.
	 */
	private static boolean accepts(String acceptHeader, String toAccept) {
		String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
		Arrays.sort(acceptValues);
		return Arrays.binarySearch(acceptValues, toAccept) > -1
			|| Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
			|| Arrays.binarySearch(acceptValues, "*/*") > -1;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * Convenience class for a file resource.
	 */
	private static class Resource {
		private final File file;
		private final long length;
		private final long lastModified;
		private final String eTag;

		public Resource(File file) {
			if (file != null && file.isFile()) {
				this.file = file;
				length = file.length();
				lastModified = file.lastModified();
				eTag = String.format(ETAG, encodeURL(file.getName()), lastModified);
			}
			else {
				this.file = null;
				length = 0;
				lastModified = 0;
				eTag = null;
			}
		}

	}

	/**
	 * Convenience class for a byte range.
	 */
	private static class Range {
		private final long start;
		private final long end;
		private final long length;

		public Range(long start, long end) {
			this.start = start;
			this.end = end;
			length = end - start + 1;
		}

	}

}