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

import static java.lang.Boolean.TRUE;
import static org.omnifaces.component.output.Cache.PropertyKeys.key;
import static org.omnifaces.component.output.Cache.PropertyKeys.scope;
import static org.omnifaces.component.output.Cache.PropertyKeys.time;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.omnifaces.component.output.cache.CacheFactory;
import org.omnifaces.util.State;

/**
 * <strong>Cache</strong> is a component that captures the mark-up rendered by its children and caches this for future
 * requests.
 * 
 * @since 1.1
 * @author Arjan Tijms
 */
@FacesComponent(Cache.COMPONENT_TYPE)
public class Cache extends UIComponentBase {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Cache";
	public static final String COMPONENT_FAMILY = "org.omnifaces.component.output";
	public static final String VALUE_SET = "org.omnifaces.cache.VALUE_SET";
	public static final String DEFAULT_SCOPE = "session";

	private final State state = new State(getStateHelper());

	enum PropertyKeys {
		key, scope, time
	}

	/**
	 * Returns {@link #COMPONENT_FAMILY}.
	 */
	@Override
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {

		String key = getKeyWithDefault(context);

		ResponseWriter responseWriter = context.getResponseWriter();
		org.omnifaces.component.output.cache.Cache scopedCache = CacheFactory.getCache(context, getScope());

		String childRendering = scopedCache.get(key);

		if (childRendering == null) {
			Writer bufferWriter = new StringWriter();

			context.setResponseWriter(responseWriter.cloneWithWriter(bufferWriter));
			try {
				super.encodeChildren(context);
			} finally {
				context.setResponseWriter(responseWriter);
			}

			childRendering = bufferWriter.toString();
			int time = getTime();
			if (time > 0) {
				scopedCache.put(key, childRendering, time);
			} else {
				scopedCache.put(key, childRendering);
			}
			
			// Marker to register we added a value to the cache during this request
			context.getExternalContext().getRequestMap().put(VALUE_SET, TRUE);
		}

		responseWriter.write(childRendering);
	}
	
	@Override
	protected boolean isVisitable(VisitContext visitContext) {
		
		FacesContext context = visitContext.getFacesContext();
		
		// Visit us and our children if a value for the cache was set in this request, or
		// if no value was cached yet.
		return
			TRUE.equals(context.getExternalContext().getRequestMap().get(VALUE_SET)) ||
				
			CacheFactory.getCache(context, getScope()).get(getKeyWithDefault(context)) == null;
	}
	
	private String getKeyWithDefault(FacesContext context) {
		String key = getKey();
		if (key == null) {
			key = context.getViewRoot().getViewId() + "_" + this.getClientId(context);
		}
		
		return key;
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

}
