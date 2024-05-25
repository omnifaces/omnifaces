/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.filter;

import static java.lang.String.format;
import static org.omnifaces.servlet.CompressedHttpServletResponse.Algorithm.BROTLI;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.omnifaces.io.ResettableBuffer;
import org.omnifaces.io.ResettableBufferedOutputStream;
import org.omnifaces.io.ResettableBufferedWriter;
import org.omnifaces.servlet.CompressedHttpServletResponse;
import org.omnifaces.servlet.CompressedHttpServletResponse.Algorithm;
import org.omnifaces.servlet.HttpServletResponseOutputWrapper;

/**
 * <p>
 * The {@link CompressedResponseFilter} will apply compression on HTTP responses whenever applicable. It will greatly
 * reduce the HTTP response size when applied on character based responses like HTML, CSS and JS, on average it can save
 * up to ~70% of bandwidth.
 * <p>
 * While HTTP response compression is normally to be configured in the servlet container (e.g. <code>&lt;Context compression="on"&gt;</code>
 * in Tomcat, or <code>&lt;property name="compression" value="on"&gt;</code> in GlassFish), this filter allows a servlet
 * container independent way of configuring HTTP response compression and also allows enabling HTTP response compression
 * anyway on 3rd party hosts where you have no control over servlet container configuration.
 * 
 * <h2>Compression algorithms</h2>
 * <p>
 * Currently three compression algorithms are supported: Brotli, GZIP and Deflate. When the client supports Brotli
 * compression <strong>and</strong> one of the classes specified in {@link Algorithm#BROTLI} is present in the runtime
 * classpath, then Brotli will be used. Else when the client supports GZIP compression, then GZIP will be used via the
 * standard JDK {@link GZIPOutputStream}. As last resort, when the client supports Deflate compression, then Deflate
 * will be used via the standard JDK {@link DeflaterOutputStream}.
 *
 * <h2>Installation</h2>
 * <p>
 * To get it to run, map this filter on the desired <code>&lt;url-pattern&gt;</code> or maybe even on the
 * <code>&lt;servlet-name&gt;</code> of the <code>FacesServlet</code>. A <code>Filter</code> is by default dispatched
 * on <code>REQUEST</code> only, you might want to explicitly add the <code>ERROR</code> dispatcher to get it to run
 * on error pages as well.
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;compressedResponseFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.CompressedResponseFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;compressedResponseFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *     &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *     &lt;dispatcher&gt;ERROR&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * Mapping on <code>/*</code> may be too global as some types of requests (comet, long polling, etc) cannot be compressed.
 * In that case, consider mapping it to the exact <code>&lt;servlet-name&gt;</code> of the {@link FacesServlet} in the
 * same <code>web.xml</code>.
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;compressedResponseFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.CompressedResponseFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;compressedResponseFilter&lt;/filter-name&gt;
 *     &lt;servlet-name&gt;facesServlet&lt;/servlet-name&gt;
 *     &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *     &lt;dispatcher&gt;ERROR&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h2>Configuration (optional)</h2>
 * <p>
 * This filter supports three initialization parameters which needs to be placed in <code>&lt;filter&gt;</code> element
 * as follows:
 * <pre>
 * &lt;init-param&gt;
 *     &lt;description&gt;The preferred algorithm. Must be one of Brotli, GZIP or Deflate (case insensitive). Defaults to automatic.&lt;/description&gt;
 *     &lt;param-name&gt;algorithm&lt;/param-name&gt;
 *     &lt;param-value&gt;GZIP&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;description&gt;The threshold size in bytes. Must be a number between 0 and 9999. Defaults to 150.&lt;/description&gt;
 *     &lt;param-name&gt;threshold&lt;/param-name&gt;
 *     &lt;param-value&gt;150&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;description&gt;The mimetypes which needs to be compressed. Must be a commaseparated string. Defaults to the below values.&lt;/description&gt;
 *     &lt;param-name&gt;mimetypes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         text/plain, text/html, text/xml, text/css, text/javascript, text/csv, text/rtf,
 *         application/xml, application/xhtml+xml, application/javascript, application/x-javascript, application/json,
 *         image/svg+xml
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * <p>
 * The default <code>threshold</code> is thus 150 bytes. This means that when the response is not larger than 150 bytes,
 * then it will not be compressed. Only when it's larger than 150 bytes, then it will be compressed. A
 * threshold of between 150 and 1000 bytes is recommended due to overhead and latency of compression/decompression.
 * The value must be a number between 0 and 9999. A value larger than 2000 is not recommended.
 * <p>
 * The <code>mimetypes</code> represents a comma separated string of mime types which needs to be compressed. It's
 * exactly that value which appears in the <code>Content-Type</code> header of the response. The in the above example
 * mentioned mime types are already the default values. Note that HTTP response compression does not have any benefit
 * when applied on binary mimetypes like images, office documents, PDF files, etcetera. So setting it for them is not
 * recommended.
 *
 * @author Bauke Scholtz
 * @since 4.5
 * @see CompressedHttpServletResponse
 * @see HttpServletResponseOutputWrapper
 * @see ResettableBuffer
 * @see ResettableBufferedOutputStream
 * @see ResettableBufferedWriter
 * @see HttpFilter
 */
public class CompressedResponseFilter extends HttpFilter {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(CompressedResponseFilter.class.getName());

    private static final String LOG_BROTLI_AVAILABLE = "CompressedResponseFilter: Brotli is available via %s.";
    private static final String LOG_BROTLI_UNAVAILABLE = "CompressedResponseFilter: Brotli is unavailable;"
        + " GZIP/Deflate will be used instead.";

    private static final String INIT_PARAM_ALGORITHM = "algorithm";
    private static final String INIT_PARAM_THRESHOLD = "threshold";
    private static final String INIT_PARAM_MIMETYPES = "mimetypes";

    private static final int DEFAULT_THRESHOLD = 150;
    private static final Set<String> DEFAULT_MIMETYPES = unmodifiableSet(
        "text/plain", "text/html", "text/xml", "text/css", "text/javascript", "text/csv", "text/rtf",
        "application/xml", "application/xhtml+xml", "application/javascript", "application/x-javascript",
        "application/json", "image/svg+xml"
    );

    private static final String ERROR_ALGORITHM = "The 'algorithm' init param must be one of Brotli, GZIP or Deflate"
        + " (case insensitive). Encountered an invalid value of '%s'.";
    private static final String ERROR_THRESHOLD = "The 'threshold' init param must be a number between 0 and 9999."
        + " Encountered an invalid value of '%s'.";
    private static final String ERROR_BROTLI_UNAVAILABLE = "CompressedResponseFilter: Brotli is unavailable;"
        + " Please make sure that at least one of the supported Brotli libraries is installed.";

    // Vars -----------------------------------------------------------------------------------------------------------

    private Algorithm algorithm;
    private Set<String> mimetypes = DEFAULT_MIMETYPES;
    private int threshold = DEFAULT_THRESHOLD;

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Initializes the filter parameters.
     */
    @Override
    public void init() throws ServletException {
        String algorithmParam = getInitParameter(INIT_PARAM_ALGORITHM);

        if (algorithmParam != null) {
            try {
                algorithm = Algorithm.valueOf(algorithmParam.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                throw new ServletException(format(ERROR_ALGORITHM, algorithmParam), e);
            }
        }

        if (algorithm == null || algorithm == BROTLI) {
            if (Algorithm.BROTLI.isAvailable()) {
                logger.info(format(LOG_BROTLI_AVAILABLE, Algorithm.BROTLI.getOutputStreamClass()));
            }
            else if (algorithm == null) {
                logger.info(LOG_BROTLI_UNAVAILABLE);
            }
            else {
                throw new IllegalStateException(ERROR_BROTLI_UNAVAILABLE);
            }
        }

        String thresholdParam = getInitParameter(INIT_PARAM_THRESHOLD);

        if (thresholdParam != null) {
            if (!thresholdParam.matches("[0-9]{1,4}")) {
                throw new ServletException(format(ERROR_THRESHOLD, thresholdParam));
            }
            else {
                threshold = Integer.valueOf(thresholdParam);
            }
        }

        String mimetypesParam = getInitParameter(INIT_PARAM_MIMETYPES);

        if (mimetypesParam != null) {
            mimetypes = new HashSet<>(Arrays.asList(mimetypesParam.split("\\s*,\\s*")));
        }
    }

    /**
     * Perform the filtering job. Only if the client accepts an algorithm based on the request headers, then wrap the
     * response in a {@link CompressedHttpServletResponse} and pass it through the filter chain.
     */
    @Override
    public void doFilter
        (HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
            throws ServletException, IOException
    {
        Algorithm acceptedAlgorithm = (algorithm == null) ? Algorithm.find(request).orElse(null) : algorithm.accepts(request) ? algorithm : null;

        if (acceptedAlgorithm != null) {
            CompressedHttpServletResponse compressedResponse = new CompressedHttpServletResponse(response, acceptedAlgorithm, threshold, mimetypes);
            chain.doFilter(request, compressedResponse);
            compressedResponse.close(); // Mandatory for the case the threshold limit hasn't been reached.
        }
        else {
            chain.doFilter(request, response);
        }
    }

}