/*
 * Copyright 2017 OmniFaces
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

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static org.omnifaces.util.Components.getParams;
import static org.omnifaces.util.Faces.getRequestDomainURL;
import static org.omnifaces.util.FacesLocal.getBookmarkableURL;
import static org.omnifaces.util.FacesLocal.getRequestDomainURL;
import static org.omnifaces.util.FacesLocal.getRequestURI;
import static org.omnifaces.util.FacesLocal.setRequestAttribute;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.formatURLWithQueryString;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:url&gt;</code> is a component which renders the given JSF view ID as a bookmarkable URL with support
 * for exposing it into the request scope by the variable name as specified by the <code>var</code> attribute instead of
 * rendering it.
 * <p>
 * This component also supports adding query string parameters to the URL via nested <code>&lt;f:param&gt;</code> and
 * <code>&lt;o:param&gt;</code>. This can be used in combination with <code>includeViewParams</code> and
 * <code>includeRequestParams</code>. The <code>&lt;f|o:param&gt;</code> will override any included view or request
 * parameters on the same name. To conditionally add or override, use the <code>disabled</code> attribute of
 * <code>&lt;f|o:param&gt;</code>.
 * <p>
 * This component fills the gap caused by absence of JSTL <code>&lt;c:url&gt;</code> in Facelets. This component is
 * useful for generating URLs for usage in e.g. plain HTML <code>&lt;link&gt;</code> elements and JavaScript variables.
 *
 * <h3>Domain</h3>
 * <p>
 * The domain of the URL defaults to the current domain. It is possible to provide a full qualified domain name (FQDN)
 * via the <code>domain</code> attribute which the URL is to be prefixed with. This can be useful if a canonical page
 * shall point to a different domain or a specific subdomain.
 * <p>
 * Valid formats and values for <code>domain</code> attribute are:
 * <ul>
 * <li><code>&lt;o:url domain="http://example.com" /&gt;</code></li>
 * <li><code>&lt;o:url domain="//example.com" /&gt;</code></li>
 * <li><code>&lt;o:url domain="example.com" /&gt;</code></li>
 * <li><code>&lt;o:url domain="/" /&gt;</code></li>
 * <li><code>&lt;o:url domain="//" /&gt;</code></li>
 * </ul>
 * <p>
 * The <code>domain</code> value will be validated by {@link URL} and throw an illegal argument exception when invalid.
 * If the value equals <code>/</code>, then the URL becomes domain-relative.
 * If the value equals or starts with <code>//</code>, or does not contain any scheme, then the URL becomes scheme-relative.
 *
 * <h3>Request and view parameters</h3>
 * <p>You can optionally include all GET request query string parameters or only JSF view parameters in the resulting
 * URL via <code>includeRequestParams="true"</code> or <code>includeViewParams="true"</code>.
 * The <code>includeViewParams</code> is ignored when <code>includeRequestParams="true"</code>.
 * The <code>&lt;f|o:param&gt;</code> will override any included request or view parameters on the same name.
 *
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * &lt;p&gt;Full URL of current page is: &lt;o:url /&gt;&lt;/p&gt;
 * &lt;p&gt;Full URL of another page is: &lt;o:url viewId="/another.xhtml" /&gt;&lt;/p&gt;
 * &lt;p&gt;Full URL of current page including view params is: &lt;o:url includeViewParams="true" /&gt;&lt;/p&gt;
 * &lt;p&gt;Full URL of current page including query string is: &lt;o:url includeRequestParams="true" /&gt;&lt;/p&gt;
 * &lt;p&gt;Domain-relative URL of current page is: &lt;o:url domain="/" /&gt;&lt;/p&gt;
 * &lt;p&gt;Scheme-relative URL of current page is: &lt;o:url domain="//" /&gt;&lt;/p&gt;
 * &lt;p&gt;Scheme-relative URL of current page on a different domain is: &lt;o:url domain="sub.example.com" /&gt;&lt;/p&gt;
 * &lt;p&gt;Full URL of current page on a different domain is: &lt;o:url domain="https://sub.example.com" /&gt;&lt;/p&gt;
 * </pre>
 * <pre>
 * &lt;o:url var="_linkCanonical"&gt;
 *     &lt;o:param name="foo" value="#{bean.foo}" /&gt;
 * &lt;/o:url&gt;
 * &lt;link rel="canonical" href="#{_linkCanonical}" /&gt;
 * </pre>
 * <pre>
 * &lt;o:url var="_linkNext" includeViewParams="true"&gt;
 *     &lt;f:param name="page" value="#{bean.pageIndex + 1}" /&gt;
 * &lt;/o:url&gt;
 * &lt;link rel="next" href="#{_linkNext}" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 2.4
 * @see OutputFamily
 */
@FacesComponent(Url.COMPONENT_TYPE)
public class Url extends OutputFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Url";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"A value expression is disallowed on 'var' attribute of Url.";

	private static final String ERROR_INVALID_DOMAIN =
		"o:url 'domain' attribute '%s' does not represent a valid domain.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		var,
		domain,
		viewId,
		includeViewParams,
		includeRequestParams;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * An override which checks if this isn't been invoked on <code>var</code> attribute.
	 * Finally it delegates to the super method.
	 * @throws IllegalArgumentException When this value expression is been set on <code>var</code> attribute.
	 */
	@Override
	public void setValueExpression(String name, ValueExpression binding) {
		if (PropertyKeys.var.toString().equals(name)) {
			throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
		}

		super.setValueExpression(name, binding);
	}

	/**
	 * Returns <code>false</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return false;
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		String viewId = getViewId();
		Map<String, List<String>> params = getParams(this, isIncludeRequestParams(), isIncludeViewParams());
		String url = (viewId == null) ? getActionURL(context, params) : getBookmarkableURL(context, viewId, params, false);
		String domain = getDomain();

		if ("//".equals(domain)) {
			url = getRequestDomainURL(context).split(":", 2)[1] + url;
		}
		else if (!"/".equals(domain)) {
			String normalizedDomain = domain.contains("//") ? domain : ("//") + domain;

			try {
				new URL(normalizedDomain.startsWith("//") ? ("http:" + normalizedDomain) : normalizedDomain);
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException(format(ERROR_INVALID_DOMAIN, domain), e);
			}

			url = stripTrailingSlash(normalizedDomain) + url;
		}

		if (getVar() != null) {
			setRequestAttribute(context, getVar(), url);
		}
		else {
			context.getResponseWriter().writeText(url, null);
		}
	}

	private static String getActionURL(FacesContext context, Map<String, List<String>> params) {
		return formatURLWithQueryString(getRequestURI(context), toQueryString(params));
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the variable name which exposes the URL into the request scope.
	 * @return The variable name which exposes the URL into the request scope.
	 */
	public String getVar() {
		return state.get(PropertyKeys.var);
	}

	/**
	 * Sets the variable name which exposes the URL into the request scope.
	 * @param var The variable name which exposes the URL into the request scope.
	 */
	public void setVar(String var) {
		state.put(PropertyKeys.var, var);
	}

	/**
	 * Returns the domain of the URL. Defaults to current domain.
	 * @return The domain of the URL.
	 */
	public String getDomain() {
		return state.get(PropertyKeys.domain, getRequestDomainURL());
	}

	/**
	 * Sets the domain of the URL.
	 * @param domain The domain of the URL.
	 */
	public void setDomain(String domain) {
		state.put(PropertyKeys.domain, domain);
	}

	/**
	 * Returns the view ID to create URL for. Defaults to current view ID.
	 * @return The view ID to create URL for.
	 */
	public String getViewId() {
		return state.get(PropertyKeys.viewId);
	}

	/**
	 * Sets the view ID to create URL for.
	 * @param viewId The view ID to create URL for.
	 */
	public void setViewId(String viewId) {
		state.put(PropertyKeys.viewId, viewId);
	}

	/**
	 * Returns whether or not the view parameters should be encoded into the URL. Defaults to <code>false</code>.
	 * This setting is ignored when <code>includeRequestParams</code> is set to <code>true</code>.
	 * @return Whether or not the view parameters should be encoded into the URL.
	 */
	public boolean isIncludeViewParams() {
		return state.get(PropertyKeys.includeViewParams, FALSE);
	}

	/**
	 * Sets whether or not the view parameters should be encoded into the URL.
	 * This setting is ignored when <code>includeRequestParams</code> is set to <code>true</code>.
	 * @param includeViewParams Whether or not the view parameters should be encoded into the URL.
	 */
	public void setIncludeViewParams(boolean includeViewParams) {
		state.put(PropertyKeys.includeViewParams, includeViewParams);
	}

	/**
	 * Returns whether or not the request query string parameters should be encoded into the URL. Defaults to <code>false</code>.
	 * When set to <code>true</code>, then this will override the <code>includeViewParams</code> setting.
	 * @return Whether or not the request query string parameters should be encoded into the URL.
	 */
	public boolean isIncludeRequestParams() {
		return state.get(PropertyKeys.includeRequestParams, FALSE);
	}

	/**
	 * Sets whether or not the request query string parameters should be encoded into the URL.
	 * When set to <code>true</code>, then this will override the <code>includeViewParams</code> setting.
	 * @param includeRequestParams Whether or not the request query string parameters should be encoded into the URL.
	 */
	public void setIncludeRequestParams(boolean includeRequestParams) {
		state.put(PropertyKeys.includeRequestParams, includeRequestParams);
	}

}