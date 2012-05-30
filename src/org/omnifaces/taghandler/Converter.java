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
package org.omnifaces.taghandler;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

/**
 * The <code>&lt;o:converter&gt;</code> basically extends the <code>&lt;f:converter&gt;</code> tag family with the
 * possibility to evaluate the value expression in all attributes on a per request basis instead of on a per view
 * build time basis. This allows the developer to change the attributes on a per request basis.
 * <p>
 * When you specify for example the standard <code>&lt;f:convertDateTime&gt;</code> by
 * <code>converterId="javax.faces.DateTime"</code>, then you'll be able to use all its attributes such as
 * <code>pattern</code> and <code>locale</code> as per its documentation, but then with the possibility to supply
 * request based value expressions.
 * <pre>
 * &lt;o:converter converterId="javax.faces.DateTime" pattern="#{item.pattern}" locale="#{item.locale}" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 */
public class Converter extends RenderTimeTagHandler {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_MISSING_CONVERTERID =
		"o:converter attribute 'converterId' or 'binding' must be specified.";
	private static final String ERROR_INVALID_CONVERTERID =
		"o:converter attribute 'converterId' must refer an valid converter ID. The converter ID '%s' cannot be found.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public Converter(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the parent component is new, then create a {@link javax.faces.convert.Converter} based on the
	 * <code>binding</code> and/or <code>converterId</code> attributes as per the standard JSF
	 * <code>&lt;f:converter&gt;</code> implementation and collect the render time attributes. Then create an anonymous
	 * <code>Converter</code> implementation which wraps the created <code>Converter</code> and delegates the methods
	 * to it after setting the render time attributes. Finally set the anonymous implementation on the parent component.
	 * @param context The involved facelet context.
	 * @param parent The parent component to set the <code>Converter</code> on.
	 * @throws IOException If something fails at I/O level.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		final javax.faces.convert.Converter converter = createConverter(context);
		final RenderTimeAttributes attributes = collectRenderTimeAttributes(context, converter);
		((ValueHolder) parent).setConverter(new javax.faces.convert.Converter() {

			@Override
			public Object getAsObject(FacesContext context, UIComponent component, String value) {
				attributes.invokeSetters(context.getELContext(), converter);
				return converter.getAsObject(context, component, value);
			}

			@Override
			public String getAsString(FacesContext context, UIComponent component, Object value) {
				attributes.invokeSetters(context.getELContext(), converter);
				return converter.getAsString(context, component, value);
			}
		});
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Create the converter based on the <code>binding</code> and/or <code>converterId</code> attribute.
	 * @param context The involved facelet context.
	 * @return The created converter.
	 * @throws IllegalArgumentException If the <code>converterId</code> attribute is invalid or missing while the
	 * <code>binding</code> attribute is also missing.
	 * @see Application#createConverter(String)
	 */
	private javax.faces.convert.Converter createConverter(FaceletContext context) {
		ValueExpression binding = getValueExpression(context, "binding", javax.faces.convert.Converter.class);
		ValueExpression converterId = getValueExpression(context, "converterId", String.class);
		javax.faces.convert.Converter converter = null;

		if (binding != null) {
			converter = (javax.faces.convert.Converter) binding.getValue(context);
		}

		if (converterId != null) {
			try {
				converter = context.getFacesContext().getApplication()
					.createConverter((String) converterId.getValue(context));
			}
			catch (FacesException e) {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_CONVERTERID, converterId));
			}

			if (binding != null) {
				binding.setValue(context, converter);
			}
		}
		else if (converter == null) {
			throw new IllegalArgumentException(ERROR_MISSING_CONVERTERID);
		}

		return converter;
	}

}