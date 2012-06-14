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

/**
 * <p>
 * This HTML5 render kit will return a special HTML5 response writer which checks for any HTML5 specific attributes
 * which are been set on the {@link UIForm} and {@link UIInput} components, but which are unsupported by JSF. So far in
 * JSF 2.0 and 2.1 only the <code>autocomplete</code> attribute is supported in {@link UIInput} components. All other
 * HTML5 attributes are by design ignored by the JSF standard HTML render kit. This HTML5 render kit adds the support
 * for the following HTML5 specific attributes:
 * <ul>
 * <li><code>UIForm</code>: <code>autocomplete</code></li>
 * <li><code>UISelectBoolean</code>, <code>UISelectOne</code> and <code>UISelectMany</code>: <code>autofocus</code></li>
 * <li><code>UIInput</code> (expect of <code>UISelect*</code>): <code>autofocus</code>, <code>list</code>,
 * <code>pattern</code> and <code>placeholder</code></li>
 * </ul>
 * <p>
 * Note: the <code>list</code> attribute expects a <code>&lt;datalist&gt;</code> element which needs to be coded in
 * "plain vanilla" HTML (and is currently, June 2012, only supported in Firefox and Opera). See also
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

		private static final String[] UIFormAttributes = {
			"autocomplete"
			// "novalidate" is not useable in a JSF form.
		};

		private static final String[] UISelectAttributes = {
			"autofocus"
			// "form" attribute is not useable in a JSF form.
		};

		private static final String[] UIInputAttributes = {
			"autofocus", "list", "pattern", "placeholder"
			// "form*" attributes are not useable in a JSF form.
			// "height" and "width" attributes are only applicable on <input type="image">.
			// "max", "min" and "step" are only applicable on <input type="number">.
			// "multiple" is only applicable on <input type="email"> and <input type="file">.
			// "required" can't be used as it would override JSF default "required" behaviour.
		};

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
		public void endElement(String elementName) throws IOException {
			UIComponent component = Components.getCurrentComponent();

			if (component instanceof UIForm) {
				writeHtml5AttributesIfNecessary(component.getAttributes(), UIFormAttributes);
			}
			else if (component instanceof UISelectBoolean
				|| component instanceof UISelectOne
				|| component instanceof UISelectMany)
			{
				writeHtml5AttributesIfNecessary(component.getAttributes(), UISelectAttributes);
			}
			else if (component instanceof UIInput) {
				writeHtml5AttributesIfNecessary(component.getAttributes(), UIInputAttributes);
			}

			super.endElement(elementName);
		}

		@Override
		public ResponseWriter getWrapped() {
			return wrapped;
		}

		// Helpers ----------------------------------------------------------------------------------------------------

		private void writeHtml5AttributesIfNecessary(Map<String, Object> attributes, String[] names) throws IOException {
			for (String name : names) {
				Object value = attributes.get(name);

				if (value != null) {
					writeAttribute(name, value, name);
				}
			}
		}

	}

}