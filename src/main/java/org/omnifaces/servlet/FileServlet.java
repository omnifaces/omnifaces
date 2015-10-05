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

import static org.omnifaces.util.Utils.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omnifaces.filter.GzipResponseFilter;

/**
 * <p>
 * The well known "<a href="http://balusc.omnifaces.org/2009/02/fileservlet-supporting-resume-and.html">BalusC FileServlet</a>",
 * as an abstract template, slightly refactored, rewritten and modernized with a.o. fast NIO stuff instead of legacy
 * RandomAccessFile. GZIP support is stripped off as that can be done application wide via {@link GzipResponseFilter}.
 * <p>
 * This servlet properly deals with <code>ETag</code>, <code>If-None-Match</code> and <code>If-Modified-Since</code>
 * caching requests, hereby improving browser caching. This servlet also properly deals with <code>Range</code> and
 * <code>If-Range</code> ranging requests, which is required by most media players for proper audio/video streaming
 * and by clients for a proper resume of an aborted/paused download. This servlet is ideal for media files.
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
 * <p><strong>See also</strong>:
 * <br><a href="http://stackoverflow.com/q/13588149/157882">How to stream audio/video files such as MP3, MP4, AVI, etc using a Servlet</a>
 * <br><a href="http://stackoverflow.com/a/29991447/157882">Abstract template for a static resource servlet</a>
 *
 * @author Bauke Scholtz
 * @since 2.2
 *
 */
public abstract class FileServlet extends HttpServlet {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	public static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();
    public static final Long DEFAULT_EXPIRE_TIME_IN_MILLIS = new Long(TimeUnit.DAYS.toMillis(30));

	private static final long ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final String ETAG_HEADER = "W/\"%s-%s\"";
    private static final String CONTENT_DISPOSITION_HEADER = "%s;filename=\"%2$s\"; filename*=" + DEFAULT_CHARSET + "''%2$s";
	private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";

	private static final String ERROR_EXPIRES_ALREADY_SET =
		"The cache expire time can be set only once. You need to set it in init() method.";

	// Variables ------------------------------------------------------------------------------------------------------

	private static long expires = DEFAULT_EXPIRE_TIME_IN_MILLIS;

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
        File file;

        try {
        	file = getFile(request);
        }
        catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (file == null || !file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fileName = URLEncoder.encode(file.getName(), DEFAULT_CHARSET);
        long lastModified = file.lastModified();
		String eTag = String.format(ETAG_HEADER, fileName, lastModified);

		if (preconditionFailed(request, eTag, lastModified)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
		}

		if (setCacheHeaders(request, response, eTag, lastModified)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		long contentLength = file.length();
		List<Range> ranges = getRanges(request, eTag, lastModified, contentLength);

		if (ranges == null) {
			response.setHeader("Content-Range", "bytes */" + contentLength);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return;
		}
		else if (ranges.isEmpty()) {
			ranges.add(new Range(0, contentLength - 1, contentLength)); // Full file.
		}
		else {
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		}

		String contentType = setContentHeaders(request, response, fileName, ranges);

		if (head) {
			return;
		}

		writeContent(response, file, contentType, ranges);
	}

    /**
     * Returns the file associated with the given HTTP servlet request. If this method returns <code>null</code>, or if
     * {@link File#exists()} or {@link File#isFile()} returns <code>false</code>, then the servlet will then return a
     * HTTP 404 error. If this method throws {@link IllegalArgumentException}, then the servlet will return a HTTP 400
     * error.
     * @param request The involved HTTP servlet request.
     * @return The file associated with the given HTTP servlet request. If the file is invalid, then the servlet will
     * return a HTTP 404 error.
     * @throws IllegalArgumentException When the request is mangled in such way that it's not recognizable as a valid
     * file request. The servlet will then return a HTTP 400 error.
     */
    protected abstract File getFile(HttpServletRequest request) throws IllegalArgumentException;

    /**
     * Sets how long the resource may be cached by the client before it expires, in milliseconds. When not set, then a
     * default value of 30 days will be assumed. It can be set only once. It's recommended to do that during
     * {@link #init()} method of the servlet (and absolutely not in one of <code>doXxx()</code> methods).
     * @param expires Cache expire time in milliseconds.
	 * @throws IllegalStateException When the cache expire time has already been set.
     */
    protected void setExpires(long expires) {
    	if (FileServlet.expires == DEFAULT_EXPIRE_TIME_IN_MILLIS) {
    		FileServlet.expires = expires;
    	}
    	else {
			throw new IllegalStateException(ERROR_EXPIRES_ALREADY_SET);
		}
    }

	// Sub-actions ----------------------------------------------------------------------------------------------------

    /**
     * Returns true if it's a conditional request which must return 412.
     */
    private boolean preconditionFailed(HttpServletRequest request, String eTag, long lastModified) {
		String match = request.getHeader("If-Match");
        long unmodified = request.getDateHeader("If-Unmodified-Since");
        return (match != null) ? !matches(match, eTag) : (unmodified != -1 && modified(unmodified, lastModified));
    }

    /**
     * Set cache headers and returns true if it's a conditional request which must return 304.
     */
    private boolean setCacheHeaders(HttpServletRequest request, HttpServletResponse response, String eTag, long lastModified) {
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + expires);

        String noMatch = request.getHeader("If-None-Match");
        long modified = request.getDateHeader("If-Modified-Since");
        return (noMatch != null) ? matches(noMatch, eTag) : (modified != -1 && !modified(modified, lastModified));
    }

	/**
	 * Returns requested ranges. If this is null, then we must return 416. If this is empty, then we must return full file.
	 */
	private List<Range> getRanges(HttpServletRequest request, String eTag, long lastModified, long contentLength) {
		List<Range> ranges = new ArrayList<>(1);
		String range = request.getHeader("Range");

		if (range == null) {
			return ranges;
		}
		else if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) { // Syntax error.
			return null;
		}

		String ifRange = request.getHeader("If-Range");

		if (ifRange != null && !ifRange.equals(eTag)) {
			try {
				long ifRangeTime = request.getDateHeader("If-Range");

				if (ifRangeTime != -1 && modified(ifRangeTime, lastModified)) {
					return ranges;
				}
			}
			catch (IllegalArgumentException ifRangeHeaderIsInvalid) {
				return ranges;
			}
		}

		for (String part : range.substring(6).split(",")) {
			// Assuming a file with length of 100, the following examples returns bytes at:
			// 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
			long start = sublong(part, 0, part.indexOf("-"));
			long end = sublong(part, part.indexOf("-") + 1, part.length());

			if (start == -1) {
				start = contentLength - end;
				end = contentLength - 1;
			}
			else if (end == -1 || end > contentLength - 1) {
				end = contentLength - 1;
			}

			if (start > end) { // Logic error.
				return null;
			}

			ranges.add(new Range(start, end, contentLength));
		}

		return ranges;
	}

	/**
	 * Set content headers and returns the content type.
	 */
	private String setContentHeaders(HttpServletRequest request, HttpServletResponse response, String fileName, List<Range> ranges) {
		String contentType = request.getServletContext().getMimeType(fileName);

		if (contentType == null) {
			contentType = "application/octet-stream";
		}
		else if (contentType.startsWith("text")) {
			contentType += ";charset=" + DEFAULT_CHARSET;
		}

		String disposition = "inline";

		if (!(contentType.startsWith("text") || contentType.startsWith("image"))) {
			String accept = request.getHeader("Accept");

			if (accept == null || !accepts(accept, contentType)) {
				disposition = "attachment";
			}
		}

		response.setHeader("Content-Disposition", String.format(CONTENT_DISPOSITION_HEADER, disposition, fileName));
		response.setHeader("Accept-Ranges", "bytes");

		if (ranges.size() == 1) {
			Range range = ranges.get(0);
			response.setContentType(contentType);
			response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.total);
			response.setHeader("Content-Length", String.valueOf(range.length));
		}
		else {
			response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
		}

		return contentType;
	}

	/**
	 * Write given file to response with given content type and ranges.
	 */
	private void writeContent(HttpServletResponse response, File file, String contentType, List<Range> ranges) throws IOException, FileNotFoundException {
		OutputStream output = response.getOutputStream();

		if (ranges.size() == 1) {
			Range range = ranges.get(0);
			stream(file, output, range.start, range.length);
		}
		else {
			ServletOutputStream sos = (ServletOutputStream) output;

			for (Range range : ranges) {
				sos.println();
				sos.println("--" + MULTIPART_BOUNDARY);
				sos.println("Content-Type: " + contentType);
				sos.println("Content-Range: bytes " + range.start + "-" + range.end + "/" + range.total);
				stream(file, output, range.start, range.length);
			}

			sos.println();
			sos.println("--" + MULTIPART_BOUNDARY + "--");
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
		return (substring.length() > 0) ? Long.parseLong(substring) : -1;
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
	 * Convenience class for a byte range.
	 */
	private static class Range {
		long start;
		long end;
		long length;
		long total;

		/**
		 * Construct a byte range.
		 * @param start Start of the byte range.
		 * @param end End of the byte range.
		 * @param total Total length of the byte source.
		 */
		public Range(long start, long end, long total) {
			this.start = start;
			this.end = end;
			length = end - start + 1;
			this.total = total;
		}

	}

}