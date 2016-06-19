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
package org.omnifaces.component.output;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.faces.event.PhaseId.RENDER_RESPONSE;
import static org.omnifaces.component.output.Cache.PropertyKeys.disabled;
import static org.omnifaces.component.output.Cache.PropertyKeys.key;
import static org.omnifaces.component.output.Cache.PropertyKeys.reset;
import static org.omnifaces.component.output.Cache.PropertyKeys.scope;
import static org.omnifaces.component.output.Cache.PropertyKeys.time;
import static org.omnifaces.component.output.Cache.PropertyKeys.useBuffer;
import static org.omnifaces.filter.OnDemandResponseBufferFilter.BUFFERED_RESPONSE;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Faces.getRequestAttribute;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.faces.component.FacesComponent;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;

import org.omnifaces.component.output.cache.CacheEntry;
import org.omnifaces.component.output.cache.CacheFactory;
import org.omnifaces.component.output.cache.CacheInitializer;
import org.omnifaces.component.output.cache.CacheInstancePerScopeProvider;
import org.omnifaces.component.output.cache.CacheProvider;
import org.omnifaces.component.output.cache.DefaultCache;
import org.omnifaces.component.output.cache.DefaultCacheProvider;
import org.omnifaces.component.output.cache.TimeToLiveCache;
import org.omnifaces.component.output.cache.el.CacheValue;
import org.omnifaces.component.output.cache.el.CachingValueExpression;
import org.omnifaces.filter.OnDemandResponseBufferFilter;
import org.omnifaces.io.ResettableBuffer;
import org.omnifaces.io.ResettableBufferedOutputStream;
import org.omnifaces.io.ResettableBufferedWriter;
import org.omnifaces.servlet.BufferedHttpServletResponse;
import org.omnifaces.servlet.HttpServletResponseOutputWrapper;
import org.omnifaces.util.Callback;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:cache&gt;</code> component allows to cache a fragment of rendered markup. The first
 * request for a page that has this component on it will cause this markup to be put into the cache. Then
 * for subsequent requests the cached content is used directly and none of the components, backing beans
 * and services that were used to generate this content in the first place will be consulted.
 * <p>
 * Caching can take place in application scope, or in session scope. For individual fragments a
 * time can be specified for which the cached content is valid. After this time is elapsed, the very
 * first request to the page containing the cache component in question will cause new content to be
 * rendered and put into the cache. A default time can be set per scope in web.xml.
 * <p>
 * For each scope a maximum capacity can be set. If the capacity for that scope is exceeded, an element will be
 * removed following a least recently used policy (LRU).
 * <p>
 * Via a cache provider mechanism an alternative cache implementation can be configured in web.xml. The default
 * cache is based on <a href="https://github.com/ben-manes/concurrentlinkedhashmap">https://github.com/ben-manes/concurrentlinkedhashmap</a>.
 *
 * <h3>Setting a custom caching provider</h3>
 * <p>
 * A custom caching provider can be set by using the <code>org.omnifaces.CACHE_PROVIDER</code> context
 * parameter in web.xml to point to an implementation of <code>org.omnifaces.component.output.cache.CacheProvider</code>.
 * For example:
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;org.omnifaces.CACHE_PROVIDER&lt;/param-name&gt;
 *     &lt;param-value&gt;com.example.MyProvider&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 * <p>
 * The default provider, <code>org.omnifaces.component.output.cache.DefaultCacheProvider</code> can be used as an
 * example.
 *
 * <h3>Global settings</h3>
 * <p>
 * For the default provider, the maximum capacity and the default time to live can be specified for the
 * supported scopes "session" and "application". If the maximum capacity is reached, an entry will be
 * evicted following a least recently used policy. The default time to live specifies for how long
 * entries are considered to be valid. A value for the <code>time</code> attribute on this component
 * will override this global default time to live. The following context parameters can be used in web.xml:
 * <table summary="All available context parameters">
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_APPLICATION_MAX_CAPACITY</code>
 * </td><td>
 * Sets the maximum number of elements that will be stored per web module (application scope).
 * Default: no limit
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_SESSION_MAX_CAPACITY</code>
 * </td><td>
 * Sets the maximum number of elements that will be stored per session.
 * Default: no limit.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_APPLICATION_TTL</code>
 * </td><td>
 * Sets the maximum amount of time in seconds that cached content is valid for the application scope.
 * Can be overriden by individal cache components.
 * Default: no limit.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_SETTING_SESSION_TTL</code>
 * </td><td>
 * Sets the maximum amount of time in seconds that cached content is valid for the session scope.
 * Can be overriden by individal cache components.
 * Default: no limit.
 * </td></tr>
 * <tr><td class="colFirst">
 * <code>org.omnifaces.CACHE_INSTALL_BUFFER_FILTER</code>
 * </td><td>
 * Boolean that when <code>true</code> installs a Servlet Filter (Servlet 3.0+ only) that works in conjunction with the
 * <code>useBuffer</code> attribute of the Cache component to enable an alternative way to grab the content that needs
 * to be cached. This is a convenience setting that is a short-cut for installing the
 * <code>org.omnifaces.servlet.BufferedHttpServletResponse</code> filter manually. If more finegrained control is needed
 * regarding which place in the filter chain the filter appears and which resources it exactly filters, this setting
 * should not be used and the mentioned filter should be manually configured.
 * Default: <code>false</code>.
 * </td></tr>
 * </table>
 *
 * @since 1.1
 * @author Arjan Tijms
 * @see org.omnifaces.component.output.cache.Cache
 * @see CacheEntry
 * @see CacheFactory
 * @see CacheInitializer
 * @see CacheInstancePerScopeProvider
 * @see CacheProvider
 * @see DefaultCache
 * @see DefaultCacheProvider
 * @see TimeToLiveCache
 * @see CacheValue
 * @see CachingValueExpression
 * @see OnDemandResponseBufferFilter
 * @see BufferedHttpServletResponse
 * @see HttpServletResponseOutputWrapper
 * @see ResettableBuffer
 * @see ResettableBufferedOutputStream
 * @see ResettableBufferedWriter
 * @see OutputFamily
 */
@FacesComponent(Cache.COMPONENT_TYPE)
public class Cache extends OutputFamily {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Cache";
	public static final String VALUE_SET = "org.omnifaces.cache.VALUE_SET";
	public static final String DEFAULT_SCOPE = "session";
	public static final String START_CONTENT_MARKER = "<!-- START CACHE FOR %s -->";
	public static final String END_CONTENT_MARKER = "<!-- END CACHE FOR %s -->";

	private static final String ERROR_NO_BUFFERED_RESPONSE = String.format(
		"No buffered response found in request, but 'useBuffer' set to true. Check setting the '%s' context parameter or installing the '%s' filter manually.",
		CacheInitializer.CACHE_INSTALL_BUFFER_FILTER, OnDemandResponseBufferFilter.class
	);
	private static final Class<? extends SystemEvent> PRE_RENDER = PreRenderViewEvent.class;

	private final State state = new State(getStateHelper());

	enum PropertyKeys {
		key, scope, time, useBuffer, reset, disabled
	}

	public Cache() {

		final FacesContext context = FacesContext.getCurrentInstance();

		// Execute the following code in PreRenderView, since at construction time the "useBuffer" and "key" attributes
		// have not been set, and there is no @PostContruct for UIComponents.
		subscribeToViewEvent(PRE_RENDER, new Callback.SerializableVoid() {

			private static final long serialVersionUID = 1L;

			@Override
			public void invoke() {

				if (!isDisabled() && isUseBuffer() && !hasCachedValue(context)) {

					final BufferedHttpServletResponse bufferedHttpServletResponse = getRequestAttribute(BUFFERED_RESPONSE);

					if (bufferedHttpServletResponse == null) {
						throw new IllegalStateException(ERROR_NO_BUFFERED_RESPONSE);
					}

					// Start buffering the response from now on
					bufferedHttpServletResponse.setPassThrough(false);

					// After the RENDER_RESPONSE phase, copy the area we need to cache from the response buffer
					// and insert it into our cache
					subscribeToRequestAfterPhase(RENDER_RESPONSE, new Callback.Void() {
						@Override
						public void invoke() {
							String content = null;

							try {
								content = getContentFromBuffer(bufferedHttpServletResponse.getBufferAsString());
							}
							catch (IOException e) {
								throw new IllegalStateException(e);
							}

							if (content != null) {
								cacheContent(context, content);
							}
						}

					});
				}
			}
		});
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {

		if (isDisabled()) {
			super.encodeChildren(context);
			return;
		}

		String key = getKeyWithDefault(context);

		ResponseWriter responseWriter = context.getResponseWriter();
		org.omnifaces.component.output.cache.Cache scopedCache = getCacheImpl(context);

		if (isReset()) {
			scopedCache.remove(key);
		}

		String childRendering = scopedCache.get(key);

		if (childRendering == null) {
			Writer bufferWriter = new StringWriter();

			ResponseWriter bufferedResponseWriter = responseWriter.cloneWithWriter(bufferWriter);

			context.setResponseWriter(bufferedResponseWriter);

			try {
				if (isUseBuffer()) {
					bufferedResponseWriter.write(getStartContentMarker());
				}

				super.encodeChildren(context);

				if (isUseBuffer()) {
					bufferedResponseWriter.write(getEndContentMarker());
				}
			} finally {
				context.setResponseWriter(responseWriter);
			}

			childRendering = bufferWriter.toString();

			cacheContent(context, scopedCache, key, childRendering);
		}

		responseWriter.write(childRendering);
	}

	/**
	 * Gets a named attribute associated with the main cache entry this component is using to store
	 * the rendering of its child components.
	 *
	 * @param context the current FacesContext
	 * @param name name of the attribute to retrieve a value for
	 * @return value associated with the named attribute
	 * @since 1.2
	 */
	public Object getCacheAttribute(FacesContext context, String name) {
		return getCacheImpl(context).getAttribute(getKeyWithDefault(context), name);
	}

	/**
	 * Sets a named attribute associated with the main cache entry this component is using to store
	 * the rendering of its child components.
	 *
	 * @param context the current FacesContext
	 * @param name name of the attribute under which the value is stored
	 * @param value the value that is to be stored
	 * @since 1.2
	 */
	public void setCacheAttribute(FacesContext context, String name, Object value) {
		getCacheImpl(context).putAttribute(getKeyWithDefault(context), name, value, getTime());
	}

	@Override
	protected boolean isVisitable(VisitContext visitContext) {

		FacesContext context = visitContext.getFacesContext();

		// Visit us and our children if a value for the cache was set in this request, or
		// if no value was cached yet.
		return isDisabled() || isCachedValueJustSet(context) || !hasCachedValue(context);
	}

	private void cacheContent(FacesContext context, String content) {
		cacheContent(context, CacheFactory.getCache(context, getScope()), getKeyWithDefault(context), content);
	}

	private void cacheContent(FacesContext context, org.omnifaces.component.output.cache.Cache scopedCache, String key, String content) {
		int time = getTime();
		if (time > 0) {
			scopedCache.put(key, content, time);
		} else {
			scopedCache.put(key, content);
		}

		// Marker to register we added a value to the cache during this request
		context.getExternalContext().getRequestMap().put(VALUE_SET, TRUE);
	}

	private String getKeyWithDefault(FacesContext context) {
		String key = getKey();
		if (key == null) {
			key = context.getViewRoot().getViewId() + "_" + this.getClientId(context);
		}

		return key;
	}

	private org.omnifaces.component.output.cache.Cache getCacheImpl(FacesContext context) {
		return CacheFactory.getCache(context, getScope());
	}

	/**
	 *
	 * @param context the FacesContext
	 * @return true if a value was inserted in the cache during this request, false otherwise
	 */
	private boolean isCachedValueJustSet(FacesContext context) {
		return TRUE.equals(context.getExternalContext().getRequestMap().get(VALUE_SET));
	}

	/**
	 *
	 * @param context the FacesContext
	 * @return true if there is a value in the cache corresponding to this component, false otherwise
	 */
	private boolean hasCachedValue(FacesContext context) {
		return CacheFactory.getCache(context, getScope()).get(getKeyWithDefault(context)) != null;
	}

	private String getStartContentMarker() {
		return String.format(START_CONTENT_MARKER, getClientId());
	}

	private String getEndContentMarker() {
		return String.format(END_CONTENT_MARKER, getClientId());
	}

	private String getContentFromBuffer(String buffer) {
		String startMarker = getStartContentMarker();
		int startIndex = buffer.indexOf(startMarker);

		if (startIndex != -1) {

			String endMarker = getEndContentMarker();
			int endIndex = buffer.indexOf(endMarker);

			if (endIndex != -1) {

				return buffer.substring(startIndex + startMarker.length(), endIndex);
			}
		}

		return null;
	}


	// Attribute getters/setters --------------------------------------------------------------------------------------

	public String getKey() {
		return state.get(key);
	}

	public void setKey(String keyValue) {
		state.put(key, keyValue);
	}

	public String getScope() {
		return state.get(scope, DEFAULT_SCOPE);
	}

	public void setScope(String scopeValue) {
		state.put(scope, scopeValue);
	}

	public Integer getTime() {
		return state.get(time, -1);
	}

	public void setTime(Integer timeValue) {
		state.put(time, timeValue);
	}

	public boolean isUseBuffer() {
		return state.get(useBuffer, FALSE);
	}

	public void setUseBuffer(boolean useBufferValue) {
		state.put(useBuffer, useBufferValue);
	}

	public boolean isReset() {
		return state.get(reset, FALSE);
	}

	public void setReset(boolean resetValue) {
		state.put(reset, resetValue);
	}

	/**
	 * Returns whether this cache is disabled.
	 * @return Whether this cache is disabled.
	 * @since 1.8
	 */
	public boolean isDisabled() {
		return state.get(disabled, FALSE);
	}

	/**
	 * Sets whether this cache is disabled.
	 * @param disabledValue Whether this cache is disabled.
	 * @since 1.8
	 */
	public void setDisabled(boolean disabledValue) {
		state.put(disabled, disabledValue);
	}

}