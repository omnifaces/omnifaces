/*
 * Copyright 2013 OmniFaces.
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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.omnifaces.util.Servlets.isFacesDevelopment;
import static org.omnifaces.util.Servlets.isFacesResourceRequest;
import static org.omnifaces.util.Servlets.setCacheHeaders;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.util.Servlets;

/**
 * <p>
 * This filter will control the cache-related headers of the response. Cache-related headers have a major impact on
 * performance (network bandwidth and server load) and user experience (up to date content and non-expired views).
 * <p>
 * By default, when no initialization parameters are specified, the filter will instruct the client (generally, the
 * webbrowser) to <strong>not</strong> cache the response. This is recommended on dynamic pages with stateful forms with
 * a <code>javax.faces.ViewState</code> hidden field. If such a page were cached, and the enduser navigates to it by
 * webbrowser's back button, and then re-submits it, then the enduser would face a
 * <a href="http://stackoverflow.com/a/3642969/157882"><code>ViewExpiredException</code></a>.
 * <p>
 * However, on stateless resources, caching the response would be beneficial. Set the expire time to the same time as
 * you'd like to use as refresh interval of the resource, which can be 10 seconds (to avoid F5-madness on resources
 * which are subject to quick changes), but also minutes or even hours, days or weeks. For example, a list of links, a
 * news page, a JS/CSS/image file, etc.
 * <p>
 * Any sane server and client adheres the following rules as to caching:
 * <ul>
 * <li>When the enduser performs page-to-page navigation, or when the enduser selects URL in address bar and presses
 * enter key again, while the resource is cached, then the client will just load it from the cache without hitting the
 * server in any way.
 * <li>Or when the cache is expired, or when the enduser does a soft-refresh by pressing refresh button or F5 key, then:
 * <ul>
 * <li>When the <code>ETag</code> or <code>Last-Modified</code> header is present on cached resource, then the client
 * will perform a so-called conditional GET request with <code>If-None-Match</code> or <code>If-Modified-Since</code>
 * headers. If the server responds with HTTP status 304 ("not modified") along with the updated cache-related headers,
 * then the client will keep the resource in cache and expand its expire time based on the headers. Note:
 * <code>ETag</code> takes precedence over <code>Last-Modified</code> when both are present and consequently
 * <code>If-None-Match</code> takes precedence over <code>If-Modified-Since</code> when both are present.
 * <li>When those headers are <strong>not</strong> present, then the behavior is the same as during a hard-refresh.
 * </ul>
 * <li>Or when the resource is not cached, or when the enduser does a hard-refresh by pressing <code>Ctrl</code> key
 * along with refresh button or F5, then the webbrowser will perform a fresh new request and purge any cached resource.
 * </ul>
 * <p>
 * <strong>Important notice</strong>: this filter automatically skips JSF resources, such as the ones served by
 * <code>&lt;h:outputScript&gt;</code>, <code>&lt;h:outputStylesheet&gt;</code>, <code>@ResourceDependency</code>, etc.
 * Their cache-related headers are namely <a href="http://stackoverflow.com/q/15057932/157882">already</a> controlled
 * by the <code>ResourceHandler</code> implementation. In Mojarra and MyFaces, the default expiration time is 1 week
 * (604800000 milliseconds), which can be configured by a <code>web.xml</code> context parameter with the following name and
 * a value in milliseconds, e.g. <code>3628800000</code> for 6 weeks:
 * <ul>
 * <li>Mojarra: <code>com.sun.faces.defaultResourceMaxAge</code>
 * <li>MyFaces: <code>org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES</code>
 * </ul>
 * <p>
 * It would not make sense to control their cache-related headers with this filter as they would be overridden anyway.
 *
 * <h3>Configuration</h3>
 * <p>
 * This filter supports the <code>expires</code> initialization parameter which must be a number between 0 and 999999999
 * with optionally the 'w', 'd', 'h', 'm' or 's' suffix standing for respectively 'week', 'day', 'hour', 'minute' and
 * 'second'. For example: '6w' is 6 weeks. The default suffix is 's'. So, when the suffix is omitted, it's treated as
 * seconds. For example: '86400' is 86400 seconds, which is effectively equal to '86400s', '1440m', '24h' and '1d'.
 * <p>
 * Imagine that you've the following resources:
 * <ul>
 * <li>All <code>/forum/*</code> pages: cache 10 seconds.
 * <li>All <code>*.pdf</code> and <code>*.zip</code> files: cache 2 days.
 * <li>All other pages: no cache.
 * </ul>
 * <p>
 * Then you can configure the filter as follows (filter name is fully free to your choice, but keep it sensible):
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;noCache&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.CacheControlFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter&gt;
 *     &lt;filter-name&gt;cache10seconds&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.CacheControlFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;expires&lt;/param-name&gt;
 *         &lt;param-value&gt;10s&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/filter&gt;
 * &lt;filter&gt;
 *     &lt;filter-name&gt;cache2days&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.CacheControlFilter&lt;/filter-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;expires&lt;/param-name&gt;
 *         &lt;param-value&gt;2d&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/filter&gt;
 *
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;noCache&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;cache10seconds&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/forum/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;cache2days&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;*.pdf&lt;/url-pattern&gt;
 *     &lt;url-pattern&gt;*.zip&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * Note: put the more specific URL patterns in the end of filter mappings. Due to the way how filters work, there's
 * unfortunately no simple way to skip the filter on <code>/*</code> when e.g. <code>*.pdf</code> is matched. You can
 * always map the no cache filter specifically to <code>FacesServlet</code> if you intend to disable caching on
 * <strong>all</strong> JSF pages. Here's an example assuming that you've configured the <code>FacesServlet</code> with
 * a servlet name of <code>facesServlet</code>:
 * <pre>
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;noCache&lt;/filter-name&gt;
 *     &lt;servlet-name&gt;facesServlet&lt;/servlet-name&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h3>Actual headers</h3>
 * <p>If the <code>expires</code> init param is set with a value which represents a time larger than 0 seconds, then the
 * following headers will be set:
 * <ul>
 * <li><code>Cache-Control: public,max-age=[expiration time in seconds],must-revalidate</code></li>
 * <li><code>Expires: [expiration date of now plus expiration time in seconds]</code></li>
 * </ul>
 * <p>If the <code>expires</code> init param is absent, or set with a value which represents a time equal to 0 seconds,
 * then the following headers will be set:
 * <ul>
 * <li><code>Cache-Control: no-cache,no-store,must-revalidate</code></li>
 * <li><code>Expires: [expiration date of 0]</code></li>
 * <li><code>Pragma: no-cache</code></li>
 * </ul>
 *
 * <h3>JSF development stage</h3>
 * <p>To speed up development, caching by this filter is <strong>disabled</strong> when JSF project stage is set to
 * <code>Development</code> as per {@link Servlets#isFacesDevelopment(javax.servlet.ServletContext)}.
 *
 * @author Bauke Scholtz
 * @since 1.7
 * @see HttpFilter
 */
public class CacheControlFilter extends HttpFilter {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String INIT_PARAM_EXPIRES = "expires";
	private static final long DEFAULT_EXPIRES = 0;
	private static final long DAYS_PER_WEEK = 7;
	private static final String ERROR_EXPIRES = "The 'expires' init param must be a number between 0 and 999999999 with"
		+ " optionally the 'w', 'd', 'h', 'm' or 's' suffix. For example: '6w' is 6 weeks. Default suffix is 's' for"
		+ " seconds. For example: '86400' is 86400 seconds. Encountered an invalid value of '%s'.";

	private enum Unit {
		W(DAYS.toSeconds(DAYS_PER_WEEK)), D(DAYS.toSeconds(1)), H(HOURS.toSeconds(1)), M(MINUTES.toSeconds(1)), S(1);

		private long seconds;

		private Unit(long seconds) {
			this.seconds = seconds;
		}

		public long toSeconds(long value) {
			return value * seconds;
		}
	}

	// Vars -----------------------------------------------------------------------------------------------------------

	private long expires = DEFAULT_EXPIRES;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Initialize the <code>expires</code> parameter.
	 */
	@Override
	public void init() throws ServletException {
		if (isFacesDevelopment(getServletContext())) {
			return; // Don't cache during development.
		}

		String expiresParam = getInitParameter(INIT_PARAM_EXPIRES);

		if (expiresParam != null) {
			if (!expiresParam.matches("[0-9]{1,9}[wdhms]?")) {
				throw new ServletException(String.format(ERROR_EXPIRES, expiresParam));
			}

			String[] parts = expiresParam.split("(?=[wdhms])");
			long number = Long.valueOf(parts[0]);

			if (parts.length > 1) {
				String unit = parts[1];
				number = Unit.valueOf(unit.toUpperCase()).toSeconds(number);
			}

			expires = number;
		}
	}

	/**
	 * Set the necessary response headers based on <code>expires</code> initialization parameter.
	 */
	@Override
	public void doFilter
		(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
			throws ServletException, IOException
	{
		if (!isFacesResourceRequest(request)) {
			setCacheHeaders(response, expires);
		}

		chain.doFilter(request, response);
	}

}