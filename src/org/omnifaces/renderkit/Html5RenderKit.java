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
package org.omnifaces.renderkit;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitWrapper;

import org.omnifaces.util.Components;
import org.omnifaces.util.Utils;

/**
 * <p>
 * This HTML5 render kit adds support for HTML5 specific attributes which are unsupported by the JSF {@link UIForm} and
 * {@link UIInput} components. So far in JSF 2.0 and 2.1 only the <code>autocomplete</code> attribute is supported in
 * {@link UIInput} components. All other attributes are by design ignored by the JSF standard HTML render kit. This
 * HTML5 render kit supports the following HTML5 specific attributes:
 * <ul>
 * <li><code>UIForm</code>: <ul><li><code>autocomplete</code></li></ul></li>
 * <li><code>UISelectBoolean</code>, <code>UISelectOne</code> and <code>UISelectMany</code>:
 * <ul><li><code>autofocus</code></li></ul></li>
 * <li><code>HtmlInputTextarea</code>: <ul><li><code>autofocus</code></li><li><code>maxlength</code></li>
 * <li><code>placeholder</code></li><li><code>wrap</code></li></ul></li>
 * <li><code>HtmlInputText</code>: <ul><li><code>type</code> (supported values are
 * <code>text</code> (default), <code>search</code>, <code>email</code>, <code>url</code>, <code>tel</code>,
 * <code>range</code>, <code>number</code> and <code>date</code>)</li><li><code>autofocus</code></li>
 * <li><code>list</code></li><li><code>pattern</code></li><li><code>placeholder</code></li><li><code>min</code></li>
 * <li><code>max</code></li><li><code>step</code></li></ul>(the latter three are only supported on <code>type</code> of
 * <code>range</code>, <code>number</code> and <code>date</code>)</li>
 * </ul>
 * <p>
 * Note: the <code>list</code> attribute expects a <code>&lt;datalist&gt;</code> element which needs to be coded in
 * "plain vanilla" HTML (and is currently, June 2012, only supported in Firefox 4 and Opera 11). See also
 * <a href="http://www.html5tutorial.info/html5-datalist.php">HTML5 tutorial</a>.
 * <p>
 * Refer the documentation of {@link Html5RenderKitFactory} how to setup it.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public class Html5RenderKit extends RenderKitWrapper {

	// Properties -----------------------------------------------------------------------------------------------------

	private RenderKit wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new HTML5 render kit around the given wrapped render kit.
	 * @param wrapped The wrapped render kit.
	 */
	public Html5RenderKit(RenderKit wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new HTML5 response writer which in turn wraps the default response writer.
	 */
	@Override
	public ResponseWriter createResponseWriter(Writer writer, String contentTypeList, String characterEncoding) {
		return new Html5ResponseWriter(super.createResponseWriter(writer, contentTypeList, characterEncoding));
	}

	@Override
	public RenderKit getWrapped() {
		return wrapped;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This HTML5 response writer does all the job.
	 * @author Bauke Scholtz
	 */
	static class Html5ResponseWriter extends ResponseWriterWrapper {

		// Constants --------------------------------------------------------------------------------------------------

		private static final Set<String> HTML5_UIFORM_ATTRIBUTES = Utils.unmodifiableSet(
			"autocomplete"
			// "novalidate" attribute is not useable in a JSF form.
		);

		private static final Set<String> HTML5_UISELECT_ATTRIBUTES = Utils.unmodifiableSet(
			"autofocus"
			// "form" attribute is not useable in a JSF form.
		);

		private static final Set<String> HTML5_TEXTAREA_ATTRIBUTES = Utils.unmodifiableSet(
			"autofocus", "maxlength", "placeholder", "wrap"
			// "form" attribute is not useable in a JSF form.
			// "required" attribute can't be used as it would override JSF default "required" attribute behaviour.
		);

		private static final Set<String> HTML5_INPUT_ATTRIBUTES = Utils.unmodifiableSet(
			"autofocus", "list", "pattern", "placeholder"
			// "form*" attributes are not useable in a JSF form.
			// "multiple" attribute is only applicable on <input type="email"> and <input type="file"> and can't be
			// decoded by standard HtmlInputText.
			// "required" attribute can't be used as it would override JSF default "required" attribute behaviour.
		);

		private static final Set<String> HTML5_INPUT_RANGE_ATTRIBUTES = Utils.unmodifiableSet(
			"max", "min", "step"
		);

		private static final Set<String> HTML5_INPUT_RANGE_TYPES = Utils.unmodifiableSet(
			"range", "number", "date"
		);

		private static final Set<String> HTML5_INPUT_TYPES = Utils.unmodifiableSet(
			"text", "search", "email", "url", "tel", HTML5_INPUT_RANGE_TYPES
		);

		private static final String ERROR_UNSUPPORTED_HTML5_INPUT_TYPE =
			"HtmlInputText type '%s' is not supported. Supported types are " + HTML5_INPUT_TYPES + ".";

		// Properties -------------------------------------------------------------------------------------------------

		private ResponseWriter wrapped;

		// Constructors -----------------------------------------------------------------------------------------------

		public Html5ResponseWriter(ResponseWriter wrapped) {
			this.wrapped = wrapped;
		}

		// Actions ----------------------------------------------------------------------------------------------------

		@Override
		public ResponseWriter cloneWithWriter(Writer writer) {
			return new Html5ResponseWriter(super.cloneWithWriter(writer));
		}

		/**
		 * An override which checks if the given component is an instance of {@link UIForm} or {@link UIInput} and then
		 * write HTML5 attributes which are explicitly been set by the developer.
		 */
		@Override
		public void startElement(String name, UIComponent component) throws IOException {
			super.startElement(name, component);

			if (component instanceof UIForm && "form".equals(name)) {
				writeHtml5AttributesIfNecessary(component.getAttributes(), HTML5_UIFORM_ATTRIBUTES);
			}
			else if (component instanceof UIInput) {
				if (isInstanceofUISelect(component) && ("input".equals(name) || "select".equals(name))) {
					writeHtml5AttributesIfNecessary(component.getAttributes(), HTML5_UISELECT_ATTRIBUTES);
				}
				else if (component instanceof HtmlInputTextarea && "textarea".equals(name)) {
					writeHtml5AttributesIfNecessary(component.getAttributes(), HTML5_TEXTAREA_ATTRIBUTES);
				}
				else if (component instanceof HtmlInputText && "input".equals(name)) {
					Map<String, Object> attributes = component.getAttributes();
					writeHtml5AttributesIfNecessary(attributes, HTML5_INPUT_ATTRIBUTES);

					if (HTML5_INPUT_RANGE_TYPES.contains(attributes.get("type"))) {
						writeHtml5AttributesIfNecessary(attributes, HTML5_INPUT_RANGE_ATTRIBUTES);
					}
				}
			}
		}

		/**
		 * An override which checks if an attribute of <code>type="text"</code> is been written by an {@link UIInput}
		 * component and if so then check if the <code>type</code> attribute isn't been explicitly set by the developer
		 * and if so then write it.
		 * @throws IllegalArgumentException When the <code>type</code> attribute is not supported.
		 */
		@Override
		public void writeAttribute(String name, Object value, String property) throws IOException {
			if ("type".equals(name) && "text".equals(value)) {
				UIComponent component = Components.getCurrentComponent();

				if (component instanceof HtmlInputText) {
					Object type = component.getAttributes().get("type");

					if (type != null) {
						if (HTML5_INPUT_TYPES.contains(type)) {
							super.writeAttribute(name, type, null);
							return;
						}
						else {
							throw new IllegalArgumentException(
								String.format(ERROR_UNSUPPORTED_HTML5_INPUT_TYPE, type));
						}
					}
				}
			}

			super.writeAttribute(name, value, property);
		}

		@Override
		public ResponseWriter getWrapped() {
			return wrapped;
		}

		// Helpers ----------------------------------------------------------------------------------------------------

		private boolean isInstanceofUISelect(UIComponent component) {
			return component instanceof UISelectBoolean
				|| component instanceof UISelectOne
				|| component instanceof UISelectMany;
		}

		private void writeHtml5AttributesIfNecessary(Map<String, Object> attributes, Set<String> names)
			throws IOException
		{
			for (String name : names) {
				Object value = attributes.get(name);

				if (value != null) {
					super.writeAttribute(name, value, null);
				}
			}
		}

	}

}