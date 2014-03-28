/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.component.script;

import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;

import javax.faces.application.Resource;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;

import org.omnifaces.util.Hacks;

/**
 * <strong>DeferredScript</strong> is a component which defers the loading of the given script resource to the window
 * load event. In other words, the given script resource is only loaded when the window is really finished with loading.
 * So, the enduser can start working with the webpage without waiting for the additional scripts to be loaded. Usually,
 * it are those kind of scripts which are just for progressive enhancement and thus not essential for the functioning
 * of the webpage.
 * <p>
 * This will give bonus points with among others the Google PageSpeed tool, on the contrary to placing the script at
 * bottom of body, or using <code>defer="true"</code> or even <code>async="true"</code>.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
@FacesComponent(DeferredScript.COMPONENT_TYPE)
@ResourceDependency(library="omnifaces", name="omnifaces.js", target="head")
@ListenerFor(systemEventClass=PostAddToViewEvent.class)
public class DeferredScript extends ScriptFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.script.DeferredScript";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
		if (event instanceof PostAddToViewEvent) {
			FacesContext context = FacesContext.getCurrentInstance();
			UIViewRoot view = context.getViewRoot();
			boolean ajaxRequest = context.getPartialViewContext().isAjaxRequest();

			if (!ajaxRequest || !view.getComponentResources(context, "body").contains(this)) {
				view.addComponentResource(context, this, "body");
				String library = (String) getAttributes().get("library");
				String name = (String) getAttributes().get("name");
				Hacks.setScriptResourceRendered(context, library, name);
			}
		}
	}

	/**
	 * Writes a <code>&lt;script&gt;</code> element which calls <code>OmniFaces.DeferredScript.add</code> with as
	 * arguments the script URL and, if any, the onsuccess and/or onerror callbacks. If the script resource is not
	 * resolvable, then a <code>RES_NOT_FOUND</code> will be written to <code>src</code> attribute instead.
	 */
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		String library = (String) getAttributes().get("library");
		String name = (String) getAttributes().get("name");
		Resource resource = context.getApplication().getResourceHandler().createResource(name, library);

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("script", this);
		writer.writeAttribute("type", "text/javascript", "type");

		if (resource != null) {
			writer.write("OmniFaces.DeferredScript.add('");
			writer.write(resource.getRequestPath());
			writer.write('\'');

			String onbegin = (String) getAttributes().get("onbegin");
			String onsuccess = (String) getAttributes().get("onsuccess");
			String onerror = (String) getAttributes().get("onerror");
			boolean hasOnbegin = !isEmpty(onbegin);
			boolean hasOnsuccess = !isEmpty(onsuccess);
			boolean hasOnerror = !isEmpty(onerror);

			if (hasOnbegin || hasOnsuccess || hasOnerror) {
				encodeFunctionArgument(writer, onbegin, hasOnbegin);
			}

			if (hasOnsuccess || hasOnerror) {
				encodeFunctionArgument(writer, onsuccess, hasOnsuccess);
			}

			if (hasOnerror) {
				encodeFunctionArgument(writer, onerror, true);
			}

			writer.write(");");
		}
		else {
			writer.writeURIAttribute("src", "RES_NOT_FOUND", "src");
		}

		writer.endElement("script");
	}

	private void encodeFunctionArgument(ResponseWriter writer, String function, boolean hasFunction) throws IOException {
		if (hasFunction) {
			writer.write(",function() {");
			writer.write(function);
			writer.write('}');
		}
		else {
			writer.write(",null");
		}
	}

}