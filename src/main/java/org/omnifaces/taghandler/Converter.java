/*
 * Copyright 2021 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.taghandler;

import static org.omnifaces.taghandler.DeferredTagHandlerHelper.collectDeferredAttributes;
import static org.omnifaces.taghandler.DeferredTagHandlerHelper.getValueExpression;

import java.io.IOException;
import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagHandlerDelegate;

import org.omnifaces.cdi.converter.ConverterManager;
import org.omnifaces.taghandler.DeferredTagHandlerHelper.DeferredAttributes;
import org.omnifaces.taghandler.DeferredTagHandlerHelper.DeferredTagHandler;
import org.omnifaces.taghandler.DeferredTagHandlerHelper.DeferredTagHandlerDelegate;

/**
 * <p>
 * The <code>&lt;o:converter&gt;</code> is a taghandler that extends the standard <code>&lt;f:converter&gt;</code> tag
 * family with support for deferred value expressions in all attributes. In other words, the converter attributes are
 * not evaluated anymore on a per view build time basis, but just on every access like as with UI components and bean
 * properties. This has among others the advantage that they can be evaluated on a per-iteration basis inside an
 * iterating component, and that they can be set on a custom converter without needing to explicitly register it in a
 * tagfile.
 *
 * <h3>Usage</h3>
 * <p>
 * When you specify for example the standard <code>&lt;f:convertDateTime&gt;</code> by
 * <code>converterId="javax.faces.DateTime"</code>, then you'll be able to use all its attributes such as
 * <code>pattern</code> and <code>locale</code> as per its documentation, but then with the possibility to supply
 * deferred value expressions.
 * <pre>
 * &lt;o:converter converterId="javax.faces.DateTime" pattern="#{item.pattern}" locale="#{item.locale}" /&gt;
 * </pre>
 * <p>
 * The converter ID of all standard JSF converters can be found in
 * <a href="https://jakarta.ee/specifications/platform/8/apidocs/javax/faces/convert/package-summary.html">their javadocs</a>.
 * First go to the javadoc of the class of interest, then go to <code>CONVERTER_ID</code> in its field summary
 * and finally click the Constant Field Values link to see the value.
 *
 * <h3>JSF 2.3 compatibility</h3>
 * <p>
 * The <code>&lt;o:converter&gt;</code> is currently not compatible with converters which are managed via JSF 2.3's
 * new <code>managed=true</code> attribute set on the {@link FacesConverter} annotation, at least not when using
 * Mojarra. Internally, the converters are wrapped in another instance which doesn't have the needed setter methods
 * specified. In order to get them to work with <code>&lt;o:converter&gt;</code>, the <code>managed=true</code>
 * attribute needs to be removed, so that OmniFaces {@link ConverterManager} will automatically manage them.
 *
 * @author Bauke Scholtz
 * @see DeferredTagHandlerHelper
 */
public class Converter extends ConverterHandler implements DeferredTagHandler {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The constructor.
	 * @param config The converter config.
	 */
	public Converter(ConverterConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Create a {@link javax.faces.convert.Converter} based on the <code>binding</code> and/or <code>converterId</code>
	 * attributes as per the standard JSF <code>&lt;f:converter&gt;</code> implementation and collect the render time
	 * attributes. Then create an anonymous <code>Converter</code> implementation which wraps the created
	 * <code>Converter</code> and delegates the methods to it after setting the render time attributes. Finally set the
	 * anonymous implementation on the parent component.
	 * @param context The involved facelet context.
	 * @param parent The parent component to set the <code>Converter</code> on.
	 * @throws IOException If something fails at I/O level.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		boolean insideCompositeComponent = UIComponent.getCompositeComponentParent(parent) != null;

		if (!ComponentHandler.isNew(parent) && !insideCompositeComponent) {
			// If it's not new nor inside a composite component, we're finished.
			return;
		}

		if (!(parent instanceof ValueHolder) || (insideCompositeComponent && getAttribute("for") == null)) {
			// It's inside a composite component and not reattached. TagHandlerDelegate will pickup it and pass the target component back if necessary.
			super.apply(context, parent);
			return;
		}

		ValueExpression binding = getValueExpression(context, this, "binding", Object.class);
		ValueExpression id = getValueExpression(context, this, "converterId", String.class);
		javax.faces.convert.Converter<Object> converter = createInstance(context.getFacesContext(), context, binding, id);
		DeferredAttributes attributes = collectDeferredAttributes(context, this, converter);
		((ValueHolder) parent).setConverter(new DeferredConverter(converter, binding, id, attributes));
	}

	@Override
	public TagAttribute getTagAttribute(String name) {
		return getAttribute(name);
	}

	@Override
	protected TagHandlerDelegate getTagHandlerDelegate() {
		return new DeferredTagHandlerDelegate(this, super.getTagHandlerDelegate());
	}

	@Override
	public boolean isDisabled(FaceletContext context) {
		return false; // This attribute isn't supported on converters anyway.
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static javax.faces.convert.Converter<Object> createInstance(FacesContext facesContext, ELContext elContext, ValueExpression binding, ValueExpression id) {
		return DeferredTagHandlerHelper.createInstance(elContext, binding, id, facesContext.getApplication()::createConverter, "converter");
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * So that we can have a serializable converter.
	 *
	 * @author Bauke Scholtz
	 */
	protected static class DeferredConverter implements javax.faces.convert.Converter<Object>, Serializable {
		private static final long serialVersionUID = 1L;

		private transient javax.faces.convert.Converter<Object> converter;
		private final ValueExpression binding;
		private final ValueExpression id;
		private final DeferredAttributes attributes;

		public DeferredConverter(javax.faces.convert.Converter<Object> converter, ValueExpression binding, ValueExpression id, DeferredAttributes attributes) {
			this.converter = converter;
			this.binding = binding;
			this.id = id;
			this.attributes = attributes;
		}

		@Override
		public Object getAsObject(FacesContext context, UIComponent component, String value) {
			javax.faces.convert.Converter<Object> converter = getConverter(context);
			attributes.invokeSetters(context.getELContext(), converter);
			return converter.getAsObject(context, component, value);
		}

		@Override
		public String getAsString(FacesContext context, UIComponent component, Object value) {
			javax.faces.convert.Converter<Object> converter = getConverter(context);
			attributes.invokeSetters(context.getELContext(), converter);
			return converter.getAsString(context, component, value);
		}

		private javax.faces.convert.Converter<Object> getConverter(FacesContext context) {
			if (converter == null) {
				converter = Converter.createInstance(context, context.getELContext(), binding, id);
			}

			return converter;
		}
	}

}