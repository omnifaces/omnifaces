/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.component.output;

import static java.lang.String.format;
import static org.omnifaces.resourcehandler.ViewResourceHandler.isViewResourceRequest;
import static org.omnifaces.util.Components.getParams;
import static org.omnifaces.util.Faces.getRequestDomainURL;
import static org.omnifaces.util.Faces.isDevelopment;
import static org.omnifaces.util.FacesLocal.getBookmarkableURL;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.formatURLWithQueryString;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;

import jakarta.faces.application.Application;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

import org.omnifaces.component.ParamHolder;
import org.omnifaces.resourcehandler.ViewResourceHandler;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:sitemapUrl&gt;</code> is a component which renders the given target URL or JSF view ID as a sitemap
 * URL with support for adding additional query string parameters to the URL via nested <code>&lt;f:param&gt;</code>
 * and <code>&lt;o:param&gt;</code>.
 * <p>
 * This component is largely based off the {@link Url} component behind <code>&lt;o:url&gt;</code>, but then tailored
 * specifically for usage in <code>sitemap.xml</code> file. The {@link ViewResourceHandler} must be registered in
 * <code>faces-config.xml</code> in order to get JSF components to run in <code>/sitemap.xml</code>.
 *
 * <h2>Values</h2>
 * <p>
 * You can supply the sitemap URL via either the <code>value</code> attribute or the <code>viewId</code> attribute. When
 * both are specified, the <code>value</code> attribute takes precedence and the <code>viewId</code> attribute is ignored.
 *
 * <h2>Domain</h2>
 * <p>
 * When the target URL is specified as <code>viewId</code>, then the domain of the target URL defaults to the current
 * domain. It is possible to provide a full qualified domain name (FQDN) via the <code>domain</code> attribute which
 * the URL is to be prefixed with. This can be useful if a canonical page shall point to a different domain or a
 * specific subdomain.
 * <p>
 * Valid formats and values for <code>domain</code> attribute are:
 * <ul>
 * <li><code>&lt;o:sitemapUrl ... domain="https://example.com" /&gt;</code></li>
 * <li><code>&lt;o:sitemapUrl ... domain="//example.com" /&gt;</code></li>
 * <li><code>&lt;o:sitemapUrl ... domain="example.com" /&gt;</code></li>
 * <li><code>&lt;o:sitemapUrl ... domain="/" /&gt;</code></li>
 * <li><code>&lt;o:sitemapUrl ... domain="//" /&gt;</code></li>
 * </ul>
 * <p>
 * The <code>domain</code> value will be validated by {@link URL} and throw an illegal argument exception when invalid.
 * If the domain equals <code>/</code>, then the URL becomes domain-relative.
 * If the domain equals or starts with <code>//</code>, or does not contain any scheme, then the URL becomes scheme-relative.
 * If the <code>value</code> attribute is specified, then the <code>domain</code> attribute is ignored.
 *
 * <h2>Request parameters</h2>
 * <p>
 * You can add query string parameters to the URL via nested <code>&lt;f:param&gt;</code> and <code>&lt;o:param&gt;</code>.
 * To conditionally add or override, use the <code>disabled</code> attribute of <code>&lt;f|o:param&gt;</code>.
 *
 * <h2>Usage</h2>
 * <p>
 * Usage example of <code>/sitemap.xml</code> as a JSF view:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;urlset
 *     xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
 *     xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
 *     xmlns:o="http://omnifaces.org/ui"
 * &gt;
 *     &lt;ui:repeat value="#{sitemapBean.products}" var="product"&gt;
 *         &lt;o:sitemapUrl viewId="/product.xhtml" lastModified="#{product.lastModified}" changeFrequency="weekly" priority="1.0"&gt;
 *             &lt;o:param name="id" value="#{product}" converter="productConverter" /&gt;
 *         &lt;/o:sitemapUrl&gt;
 *     &lt;/ui:repeat&gt;
 * &lt;/urlset&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 3.10
 * @see OutputFamily
 * @see ViewResourceHandler
 */
@FacesComponent(SitemapUrl.COMPONENT_TYPE)
public class SitemapUrl extends OutputFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.output.SitemapUrl#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.SitemapUrl";

	/** The available values of the "changefreq" element of the sitemap URL. */
	public enum ChangeFrequency {
		// Cannot be uppercased. They have to exactly match the sitemap spec.
		always,
		hourly,
		daily,
		weekly,
		monthly,
		yearly,
		never
	}

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_REQUEST =
		"o:sitemapUrl can only be used in a file registered in org.omnifaces.VIEW_RESOURCE_HANDLER_URIS context param.";

	private static final String ERROR_MISSING_VALUE_OR_VIEWID =
		"o:sitemapUrl 'value' or 'viewId' attribute must be set.";

	private static final String ERROR_INVALID_DOMAIN =
		"o:sitemapUrl 'domain' attribute '%s' does not represent a valid domain.";

	private static final String ERROR_INVALID_PRIORITY =
		"o:sitemapUrl 'priority' attribute '%s' must be between 0.0 and 1.0.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		value,
		viewId,
		domain,
		lastModified,
		changeFrequency,
		priority
	}

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs the {@link SitemapUrl} component.
	 * @throws IllegalStateException when {@link Application#getProjectStage()} is {@link ProjectStage#Development} and
	 * the current request is not for the {@link ViewResourceHandler} at all.
	 */
	public SitemapUrl() {
		if (isDevelopment() && !isViewResourceRequest(getFacesContext())) {
			throw new IllegalStateException(ERROR_INVALID_REQUEST);
		}

		setRendererType(null);
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// UIComponent overrides ------------------------------------------------------------------------------------------

	/**
	 * Renders the start of the <code>&lt;url&gt;</code> parent element.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		context.getResponseWriter().startElement("url", this);
	}

	/**
	 * Delegates to {@link #encodeLocation(FacesContext)}, {@link #encodeLastModified(FacesContext)}, {@link #encodeChangeFrequency(FacesContext)} and
	 * {@link #encodePriority(FacesContext)} in this order.
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		encodeLocation(context);
		encodeLastModified(context);
		encodeChangeFrequency(context);
		encodePriority(context);
	}

	/**
	 * Renders the end of the <code>&lt;url&gt;</code> parent element.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		context.getResponseWriter().endElement("url");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Renders the <code>&lt;loc&gt;</code> child element with either the value of {@link #getValue()},
	 * or the value of {@link #getViewId()} and {@link #getDomain()} combined.
	 * @param context The involved faces context.
	 * @throws IOException When an I/O error occurs.
	 * @throws IllegalArgumentException When the {@link #getDomain()} does not represent a valid domain.
	 */
	protected void encodeLocation(FacesContext context) throws IOException {
		String value = getValue();
		List<ParamHolder<Object>> params = getParams(this);
		String loc;

		if (value != null) {
			loc = context.getExternalContext().encodeResourceURL(formatURLWithQueryString(value, toQueryString(params)));
		}
		else {
			String viewId = getViewId();

			if (viewId == null) {
				throw new IllegalArgumentException(ERROR_MISSING_VALUE_OR_VIEWID);
			}

			String uri = getBookmarkableURL(context, viewId, params, false);
			loc = Url.getBookmarkableURLWithDomain(context, uri, getDomain(), ERROR_INVALID_DOMAIN);
		}

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("loc", this);
		writer.writeText(loc, (value != null) ? "value" : "viewId");
		writer.endElement("loc");
	}

	/**
	 * Renders the <code>&lt;lastmod&gt;</code> child element with the value of {@link #getLastModified()}, if any.
	 * It may only encode formats specified in https://www.w3.org/TR/NOTE-datetime
	 * @param context The involved faces context.
	 * @throws IOException When an I/O error occurs.
	 */
	protected void encodeLastModified(FacesContext context) throws IOException {
		Temporal lastModified = getLastModified();

		if (lastModified != null) {
			if (lastModified instanceof LocalDateTime) {
				lastModified = ((LocalDateTime) lastModified).atZone(ZoneId.systemDefault()); // Time zone is required by spec.
			}

			if (lastModified instanceof ZonedDateTime) {
				lastModified = ((ZonedDateTime) lastModified).toOffsetDateTime(); // Time zone names are not supported by spec.
			}

			ResponseWriter writer = context.getResponseWriter();
			writer.startElement("lastmod", this);
			writer.writeText(lastModified.toString(), PropertyKeys.lastModified.name());
			writer.endElement("lastmod");
		}
	}

	/**
	 * Renders the <code>&lt;changefreq&gt;</code> child element with the value of {@link #getChangeFrequency()}, if any.
	 * @param context The involved faces context.
	 * @throws IOException When an I/O error occurs.
	 */
	protected void encodeChangeFrequency(FacesContext context) throws IOException {
		ChangeFrequency changeFrequency = getChangeFrequency();

		if (changeFrequency != null) {
			ResponseWriter writer = context.getResponseWriter();
			writer.startElement("changefreq", this);
			writer.writeText(changeFrequency, PropertyKeys.changeFrequency.name());
			writer.endElement("changefreq");
		}
	}

	/**
	 * Renders the <code>&lt;priority&gt;</code> child element with the value of {@link #getPriority()}, if any.
	 * @param context The involved faces context.
	 * @throws IOException When an I/O error occurs.
	 * @throws IllegalArgumentException When the {@link #getPriority()} is not between 0.0 and 1.0 (inclusive).
	 */
	protected void encodePriority(FacesContext context) throws IOException {
		BigDecimal priority = getPriority();

		if (priority != null) {
			if (priority.compareTo(BigDecimal.ZERO) < 0 || priority.compareTo(BigDecimal.ONE) > 0) {
				throw new IllegalArgumentException(format(ERROR_INVALID_PRIORITY, priority));
			}

			ResponseWriter writer = context.getResponseWriter();
			writer.startElement("priority", this);
			writer.writeText(priority, PropertyKeys.priority.name());
			writer.endElement("priority");
		}
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the value of the "loc" element of the sitemap URL.
	 * Note: when specified, then {@link #getViewId()} and {@link #getDomain()} are ignored.
	 * @return The value of the "loc" element of the sitemap URL.
	 */
	public String getValue() {
		return state.get(PropertyKeys.value);
	}

	/**
	 * Sets the value of the "loc" element of the sitemap URL.
	 * Note: when specified, then {@link #getViewId()} and {@link #getDomain()} are ignored.
	 * @param value The value of the "loc" element of the sitemap URL.
	 */
	public void setValue(String value) {
		state.put(PropertyKeys.value, value);
	}

	/**
	 * Returns the view ID to create the URI part of the "loc" element of the sitemap URL for.
	 * Note: this is ignored when {@link #getValue()} is specified.
	 * @return The view ID to create the URI part of the "loc" element of the sitemap URL for.
	 */
	public String getViewId() {
		return state.get(PropertyKeys.viewId);
	}

	/**
	 * Sets the view ID to create the URI part of the "loc" element of the sitemap URL for.
	 * Note: this is ignored when {@link #getValue()} is specified.
	 * @param viewId The view ID to create the URI part of the "loc" element of the sitemap URL for.
	 */
	public void setViewId(String viewId) {
		state.put(PropertyKeys.viewId, viewId);
	}

	/**
	 * Returns the domain of the "loc" element of the sitemap URL.
	 * Note: this is ignored when {@link #getValue()} is specified.
	 * @return The domain of the "loc" element of the sitemap URL for.
	 */
	public String getDomain() {
		return state.get(PropertyKeys.domain, getRequestDomainURL());
	}

	/**
	 * Sets the domain of the "loc" element of the sitemap URL.
	 * Note: this is ignored when {@link #getValue()} is specified.
	 * @param domain The domain of the "loc" element of the sitemap URL for.
	 */
	public void setDomain(String domain) {
		state.put(PropertyKeys.domain, domain);
	}

	/**
	 * Returns the value of the "lastmod" element of the sitemap URL.
	 * @return The value of the "lastmod" element of the sitemap URL.
	 */
	public Temporal getLastModified() {
		return state.get(PropertyKeys.lastModified);
	}

	/**
	 * Sets the value of the "lastmod" element of the sitemap URL.
	 * @param lastModified The value of the "lastmod" element of the sitemap URL.
	 */
	public void setLastModified(Temporal lastModified) {
		state.put(PropertyKeys.lastModified, lastModified);
	}

	/**
	 * Returns the value of the "changefreq" element of the sitemap URL.
	 * @return The value of the "changefreq" element of the sitemap URL.
	 */
	public ChangeFrequency getChangeFrequency() {
		return state.get(PropertyKeys.changeFrequency);
	}

	/**
	 * Sets the value of the "changefreq" element of the sitemap URL.
	 * @param changeFrequency The value of the "changefreq" element of the sitemap URL.
	 */
	public void setChangeFrequency(ChangeFrequency changeFrequency) {
		state.put(PropertyKeys.changeFrequency, changeFrequency);
	}

	/**
	 * Returns the value of the "priority" element of the sitemap URL.
	 * @return The value of the "priority" element of the sitemap URL.
	 */
	public BigDecimal getPriority() {
		return state.get(PropertyKeys.priority);
	}

	/**
	 * Sets the value of the "priority" element of the sitemap URL.
	 * @param priority The value of the "priority" element of the sitemap URL.
	 */
	public void setPriority(BigDecimal priority) {
		state.put(PropertyKeys.priority, priority);
	}

}
