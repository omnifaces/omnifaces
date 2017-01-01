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
package org.omnifaces.util;

import static org.omnifaces.util.Components.getAttribute;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

/**
 * Collection of utility methods for the JSF API with respect to working with {@link Renderer}.
 *
 * @author Bauke Scholtz
 * @since 1.5
 */
public final class Renderers {

	// Constants --------------------------------------------------------------------------------------------------

	/** Renderer type of standard JSF stylesheet component resource. */
	public static final String RENDERER_TYPE_CSS = "javax.faces.resource.Stylesheet";

	/** Renderer type of standard JSF script component resource. */
	public static final String RENDERER_TYPE_JS = "javax.faces.resource.Script";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Renderers() {
		// Hide constructor.
	}

	// Text -----------------------------------------------------------------------------------------------------------

	/**
	 * Write the given text either HTML-escaped or unescaped. Beware of potential XSS attack holes when user-controlled
	 * input is written unescaped!
	 * @param writer The involved response writer.
	 * @param component The associated UI component, usually the parent component.
	 * @param text The text to be written.
	 * @param escape Whether to HTML-escape the given text or not.
	 * @throws IOException When an I/O error occurs.
	 */
	public static void writeText(ResponseWriter writer, UIComponent component, String text, boolean escape)
		throws IOException
	{
		if (escape) {
			writer.writeText(text, component, null);
		}
		else {
			writer.write(text);
		}
	}

	// Attributes -----------------------------------------------------------------------------------------------------

	/**
	 * Write component attribute of the given name, if it's not empty.
	 * Both HTML attribute name and component property name defaults to the given component attribute name.
	 * @param writer The involved response writer.
	 * @param component The associated UI component, usually the parent component.
	 * @param name The component attribute name whose value should be written.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttribute(ResponseWriter writer, UIComponent component, String name) throws IOException {
		writeAttribute(writer, name, getAttribute(component, name), name);
	}

	/**
	 * Write component attribute of the given name, if it's not empty, as given HTML attribute name.
	 * Component property name defaults to the given component attribute name.
	 * @param writer The involved response writer.
	 * @param component The associated UI component, usually the parent component.
	 * @param name The component attribute name whose value should be written.
	 * @param html The HTML attribute name to be written.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttribute(ResponseWriter writer, UIComponent component, String name, String html)
		throws IOException
	{
		writeAttribute(writer, html, getAttribute(component, name), name);
	}

	/**
	 * Write component attribute of the given name, if it's not empty, as given HTML attribute name associated with
	 * given component property name.
	 * @param writer The involved response writer.
	 * @param component The associated UI component, usually the parent component.
	 * @param name The component attribute name whose value should be written.
	 * @param html The HTML attribute name to be written.
	 * @param property The associated component property name.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttribute
		(ResponseWriter writer, UIComponent component, String name, String html, String property)
			throws IOException
	{
		writeAttribute(writer, html, getAttribute(component, name), property);
	}

	/**
	 * Write given attribute value, if it's not empty, as given HTML attribute name.
	 * Component property name defaults to given HTML attribute name.
	 * @param writer The involved response writer.
	 * @param html The HTML attribute name to be written.
	 * @param value The HTML attribute value to be written.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttribute(ResponseWriter writer, String html, Object value) throws IOException {
		writeAttribute(writer, html, value, html);
	}

	/**
	 * Write given attribute value, if it's not empty, as given HTML attribute name associated with given component
	 * property name.
	 * @param writer The involved response writer.
	 * @param html The HTML attribute name to be written.
	 * @param value The HTML attribute value to be written.
	 * @param property The associated component property name.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttribute(ResponseWriter writer, String html, Object value, String property)
		throws IOException
	{
		if (value != null) {
			writer.writeAttribute(html, value, property);
		}
	}

	/**
	 * Write component attributes of the given names, if it's not empty.
	 * Both HTML attribute name and component property name defaults to the given component attribute name.
	 * @param writer The involved response writer.
	 * @param component The associated UI component, usually the parent component.
	 * @param names The names of the attributes to be written.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttributes(ResponseWriter writer, UIComponent component, String... names)
		throws IOException
	{
		for (String name : names) {
			writeAttribute(writer, name, getAttribute(component, name), name);
		}
	}

	/**
	 * Write component attributes of the given property-HTML mapping of names, if it's not empty.
	 * Map key will be used as component property name and map value will be used as HTML attribute name.
	 * @param writer The involved response writer.
	 * @param component The associated UI component, usually the parent component.
	 * @param names Mapping of component property-HTML attribute names of the attributes to be written.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 */
	public static void writeAttributes(ResponseWriter writer, UIComponent component, Map<String, String> names)
		throws IOException
	{
		for (Entry<String, String> entry : names.entrySet()) {
			String name = entry.getKey();
			String html = entry.getValue();
			writeAttribute(writer, html, getAttribute(component, name), name);
		}
	}

	/**
	 * Write ID of component if necessary. That is, when it's explicitly set or the component has client behaviors.
	 * @param writer The involved response writer.
	 * @param component The associated UI component.
	 * @throws IOException When an I/O error occurs.
	 * @see ResponseWriter#writeAttribute(String, Object, String)
	 * @since 2.5
	 */
	public static void writeIdAttributeIfNecessary(ResponseWriter writer, UIComponent component) throws IOException {
		if (component.getId() != null
			|| (component instanceof ClientBehaviorHolder
				&& !((ClientBehaviorHolder) component).getClientBehaviors().isEmpty()))
		{
			writeAttribute(writer, "id", component.getClientId());
		}
	}

}