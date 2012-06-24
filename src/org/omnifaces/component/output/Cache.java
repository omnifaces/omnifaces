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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

@FacesComponent(Cache.COMPONENT_TYPE)
public class Cache extends UIComponentBase {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Cache";
	public static final String COMPONENT_FAMILY = "org.omnifaces.component.output";

	// TMP
	private static final Map<String, String> cacheStore = new ConcurrentHashMap<String, String>();

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

		String defaultKey = context.getViewRoot().getViewId() + "_" + this.getClientId(context);
		ResponseWriter responseWriter = context.getResponseWriter();
		String childRendering = null;

		if (!cacheStore.containsKey(defaultKey)) {
			Writer bufferWriter = new StringWriter();

			context.setResponseWriter(responseWriter.cloneWithWriter(bufferWriter));
			try {
				super.encodeChildren(context);
			} finally {
				context.setResponseWriter(responseWriter);
			}

			childRendering = bufferWriter.toString();
			cacheStore.put(defaultKey, childRendering);
		} else {
			childRendering = cacheStore.get(defaultKey);
		}

		responseWriter.write(childRendering);
	}

}
