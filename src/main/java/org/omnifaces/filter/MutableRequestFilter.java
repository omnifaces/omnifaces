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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.parseRFC1123;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

/**
 * <p>
 * The {@link MutableRequestFilter} will wrap the incoming {@link HttpServletRequest} in a {@link MutableRequest} so
 * that the developer can mutate the headers and parameters in such way that the outcome of among others
 * {@link HttpServletRequest#getHeader(String)} and {@link HttpServletRequest#getParameter(String)} are influenced.
 *
 * <h3>Installation</h3>
 * <p>
 * To get this filter to run, map it as follows in <code>web.xml</code>:
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;mutableRequestFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.MutableRequestFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;mutableRequestFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * The ordering of the <code>&lt;filter-mapping&gt;</code> is significant. The {@link MutableRequest} is not available
 * in any filter which is mapped <em>before</em> the {@link MutableRequestFilter}.
 *
 * <h3>Usage</h3>
 * <p>
 * When inside {@link FacesContext}, use {@link Faces#getMutableRequestHeaderMap()} or
 * {@link Faces#getMutableRequestParameterMap()}.
 * Or when having only a {@link HttpServletRequest} at hands, use
 * {@link Servlets#getMutableRequestHeaderMap(HttpServletRequest)} or
 * {@link Servlets#getMutableRequestParameterMap(HttpServletRequest).
 *
 * @author Bauke Scholtz
 * @since 3.14
 * @see HttpFilter
 * @see MutableRequest
 */
public class MutableRequestFilter extends HttpFilter {

	/**
	 * The name of the request attribute under which the {@link MutableRequest} will be stored.
	 */
	public static final String MUTABLE_REQUEST = "org.omnifaces.mutable_request";

	private static final String ERROR_NOT_INSTALLED = "The MutableRequestFilter is not installed."
		+ " Refer to its javadoc how to install it.";

	private static final String ERROR_NOT_AVAILABLE = "The MutableRequest is not available yet."
		+ " This method should only be invoked *after* the MutableRequestFilter is invoked."
		+ " If you are actually trying to invoke it from another filter, then try reordering the filters.";

	private static boolean installed;

	/**
	 * Tracks whether this filter is installed.
	 */
	@Override
	public void init() throws ServletException {
		installed = true;
	}

	/**
	 * Creates and sets the mutable request.
	 */
	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException {
		MutableRequest mutableRequest = new MutableRequest(request);
		request.setAttribute(MUTABLE_REQUEST, mutableRequest);
		chain.doFilter(mutableRequest, response);
	}

	/**
	 * Returns the mutable request.
	 * @return The mutable request.
	 * @throws IllegalStateException When the {@link MutableRequestFilter} is not installed or not invoked yet.
	 * @since 3.14
	 */
	public static MutableRequest getMutableRequest(HttpServletRequest request) {
		MutableRequest mutableRequest = (MutableRequest) request.getAttribute(MUTABLE_REQUEST);

		if (mutableRequest == null) {
			throw new IllegalStateException(installed ? ERROR_NOT_AVAILABLE : ERROR_NOT_INSTALLED);
		}

		return mutableRequest;
	}

	/**
	 * @author Bauke Scholtz
	 * @since 3.14
	 * @see MutableRequestFilter
	 * @see HttpServletRequestWrapper
	 */
	public static class MutableRequest extends HttpServletRequestWrapper {

		private Map<String, List<String>> mutableHeaderMap;
		private Map<String, List<String>> mutableParameterMap;

		public MutableRequest(HttpServletRequest wrapped) {
			super(wrapped);
		}

		/**
		 * Returns the mutable header map of the current request.
		 * @return The mutable header map of the current request.
		 */
		public Map<String, List<String>> getMutableHeaderMap() {
			if (mutableHeaderMap == null) {
				mutableHeaderMap = new HashMap<>();

				for (String name : list(super.getHeaderNames())) {
					mutableHeaderMap.put(name, new ArrayList<>(list(super.getHeaders(name))));
				}
			}

			return mutableHeaderMap;
		}

		@Override
		public String getHeader(String name) {
			List<String> values = getMutableHeaderMap().get(name);
			return isEmpty(values) ? null : values.get(0);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return enumeration(getMutableHeaderMap().keySet());
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			List<String> values = getMutableHeaderMap().get(name);
			return enumeration(values == null ? emptyList() : values);
		}

		@Override
		public long getDateHeader(String name) {
			String value = this.getHeader(name);

			if (value == null) {
				return -1;
			}

			try {
				return parseRFC1123(value).getTime();
			}
			catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		@Override
		public int getIntHeader(String name) {
			String value = this.getHeader(name);
			return value == null ? -1 : Integer.parseInt(value);
		}

		/**
		 * Returns the mutable parameter map of the current request.
		 * @return The mutable parameter map of the current request.
		 */
		public Map<String, List<String>> getMutableParameterMap() {
			if (mutableParameterMap == null) {
				mutableParameterMap = new HashMap<>();

				for (String name : list(super.getParameterNames())) {
					mutableParameterMap.put(name, new ArrayList<>(asList(super.getParameterValues(name))));
				}
			}

			return mutableParameterMap;
		}

		@Override
		public String getParameter(String name) {
			List<String> values = getMutableParameterMap().get(name);
			return isEmpty(values) ? null : values.get(0);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return unmodifiableMap(getMutableParameterMap().entrySet().stream()
				.collect(toMap(Entry::getKey, entry -> toArray(entry.getValue()))));
		}

		@Override
		public Enumeration<String> getParameterNames() {
			return enumeration(getMutableParameterMap().keySet());
		}

		@Override
		public String[] getParameterValues(String name) {
			List<String> values = getMutableParameterMap().get(name);
			return isEmpty(values) ? null : toArray(values);
		}

		private static String[] toArray(List<String> values) {
			return values.toArray(new String[values.size()]);
		}

	}

}
