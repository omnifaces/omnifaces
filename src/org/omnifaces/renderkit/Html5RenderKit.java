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
 * <li><code>UIInput</code> (expect of <code>UISelect*</code>): <ul><li><code>type</code> (supported values are
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
 * To use the HTML5 render kit, register it as follows in <tt>faces-config.xml</tt>:
 * <pre>
 * &lt;render-kit&gt;
 *   &lt;render-kit-id&gt;HTML_BASIC&lt;/render-kit-id&gt;
 *   &lt;render-kit-class&gt;org.omnifaces.renderkit.Html5RenderKit&lt;/render-kit-class&gt;
 * &lt;/render-kit&gt;
 * </pre>
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

		private static final String[] UIFORM_HTML5_ATTRIBUTES = {
			"autocomplete"
			// "novalidate" attribute is not useable in a JSF form.
		};

		private static final String[] UISELECT_HTML5_ATTRIBUTES = {
			"autofocus"
			// "form" attribute is not useable in a JSF form.
		};

		private static final String[] UIINPUT_HTML5_ATTRIBUTES = {
			"autofocus", "list", "pattern", "placeholder"
			// "form*" attributes are not useable in a JSF form.
			// "multiple" attribute is only applicable on <input type="email"> and <input type="file"> and can't be
			// decoded by standard HtmlInputText.
			// "required" attribute can't be used as it would override JSF default "required" attribute behaviour.
		};

		private static final String[] UIINPUT_HTML5_RANGE_ATTRIBUTES = {
			"max", "min", "step"
		};

		private static final Set<String> UIINPUT_HTML5_RANGE_TYPES = Utils.unmodifiableSet(
			"range", "number", "date"
		);

		private static final Set<String> UIINPUT_HTML5_TYPES = Utils.unmodifiableSet(
			"text", "search", "email", "url", "tel", UIINPUT_HTML5_RANGE_TYPES
		);

		private static final String ERROR_UNSUPPORTED_UIINPUT_HTML5_TYPE =
			"UIInput type '%s' is not supported. Supported types are %s.";

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

		@Override
		public void writeAttribute(String name, Object value, String property) throws IOException {
			if ("type".equals(name) && "text".equals(value)) {
				UIComponent component = Components.getCurrentComponent();

				if (component instanceof UIInput) {
					Object type = component.getAttributes().get("type");

					if (type != null) {
						if (UIINPUT_HTML5_TYPES.contains(type)) {
							super.writeAttribute(name, type, null);
							return;
						}
						else {
							throw new IllegalArgumentException(
								String.format(ERROR_UNSUPPORTED_UIINPUT_HTML5_TYPE, type, UIINPUT_HTML5_TYPES));
						}
					}
				}
			}

			super.writeAttribute(name, value, property);
		}

		@Override
		public void endElement(String elementName) throws IOException {
			UIComponent component = Components.getCurrentComponent();

			if (component instanceof UIForm) {
				writeHtml5AttributesIfNecessary(component.getAttributes(), UIFORM_HTML5_ATTRIBUTES);
			}
			else if (isInstanceofUISelect(component)) {
				writeHtml5AttributesIfNecessary(component.getAttributes(), UISELECT_HTML5_ATTRIBUTES);
			}
			else if (component instanceof UIInput) {
				Map<String, Object> attributes = component.getAttributes();
				writeHtml5AttributesIfNecessary(attributes, UIINPUT_HTML5_ATTRIBUTES);
				Object type = attributes.get("type");

				if (UIINPUT_HTML5_RANGE_TYPES.contains(type)) {
					writeHtml5AttributesIfNecessary(attributes, UIINPUT_HTML5_RANGE_ATTRIBUTES);
				}
			}

			super.endElement(elementName);
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

		private void writeHtml5AttributesIfNecessary(Map<String, Object> attributes, String[] names) throws IOException {
			for (String name : names) {
				Object value = attributes.get(name);

				if (value != null) {
					writeAttribute(name, value, null);
				}
			}
		}

	}

}