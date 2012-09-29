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

import static java.lang.Boolean.*;
import static javax.faces.event.PhaseId.*;
import static org.omnifaces.component.output.Cache.PropertyKeys.*;
import static org.omnifaces.filter.OnDemandResponseBufferFilter.*;
import static org.omnifaces.util.Events.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.faces.component.FacesComponent;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;

import org.omnifaces.component.output.cache.CacheFactory;
import org.omnifaces.component.output.cache.CacheInitializerListener;
import org.omnifaces.filter.OnDemandResponseBufferFilter;
import org.omnifaces.servlet.BufferedHttpServletResponse;
import org.omnifaces.util.Callback;
import org.omnifaces.util.Faces;
import org.omnifaces.util.State;

/**
 * <strong>Cache</strong> is a component that captures the mark-up rendered by its children and caches this for future
 * requests.
 *
 * @since 1.1
 * @author Arjan Tijms
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
		CacheInitializerListener.CACHE_INSTALL_BUFFER_FILTER, OnDemandResponseBufferFilter.class
	);
	private static Class<? extends SystemEvent> PRE_RENDER = PreRenderViewEvent.class;

	private final State state = new State(getStateHelper());

	enum PropertyKeys {
		key, scope, time, useBuffer
	}

	public Cache() {

		final FacesContext context = FacesContext.getCurrentInstance();

		// Execute the following code in PreRenderView, since at construction time the "useBuffer" and "key" attributes
		// have not been set, and there is no @PostContruct for UIComponents.
		subscribeToViewEvent(PRE_RENDER, new Callback.Void() {

			@Override
			public void invoke() {

				if (isUseBuffer() && !hasCachedValue(context)) {

					final BufferedHttpServletResponse bufferedHttpServletResponse = Faces.getRequestAttribute(BUFFERED_RESPONSE);

					if (bufferedHttpServletResponse == null) {
						throw new IllegalStateException(ERROR_NO_BUFFERED_RESPONSE);
					}

					// Start buffering the response from now on
					bufferedHttpServletResponse.setPassThrough(false);

					// After the RENDER_RESPONSE phase, copy the area we need to cache from the response buffer
					// and insert it into our cache
					setCallbackAfterPhaseListener(RENDER_RESPONSE, new Callback.Void() {

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

		String key = getKeyWithDefault(context);

		ResponseWriter responseWriter = context.getResponseWriter();
		org.omnifaces.component.output.cache.Cache scopedCache = getCacheImpl(context);

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
		return isCachedValueJustSet(context) || !hasCachedValue(context);
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

	public Boolean isUseBuffer() {
    	return state.get(useBuffer, FALSE);
    }

	public void setUseBuffer(Boolean useBufferValue) {
    	state.put(useBuffer, useBufferValue);
    }

}
