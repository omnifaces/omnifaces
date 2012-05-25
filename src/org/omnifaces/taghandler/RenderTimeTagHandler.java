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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * Base class which allows the implementor to collect render time tag attributes.
 *
 * @author Bauke Scholtz
 */
abstract class RenderTimeTagHandler extends TagHandler {

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public RenderTimeTagHandler(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Collect the EL properties of the given object. If the property is a literal text (i.e. no EL expression),
	 * then it will just be set directly on the given object, else it will be collected and returned.
	 * @param context The involved facelet context.
	 * @param object The object to collect EL properties for.
	 * @return The EL properties of the given object.
	 */
	protected RenderTimeAttributes collectRenderTimeAttributes(FaceletContext context, Object object) {
		RenderTimeAttributes attributes = new RenderTimeAttributes();

		try {
			for (PropertyDescriptor property : Introspector.getBeanInfo(object.getClass()).getPropertyDescriptors()) {
				Method setter = property.getWriteMethod();

				if (setter == null) {
					continue;
				}

				ValueExpression valueExpression = getValueExpression(context, property.getName(), property.getPropertyType());

				if (valueExpression == null) {
					continue;
				}

				if (valueExpression.isLiteralText()) {
					setter.invoke(object, valueExpression.getValue(context));
				}
				else {
					attributes.add(setter, valueExpression);
				}
			}
		}
		catch (Exception e) {
			throw new FacesException(e);
		}

		return attributes;
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method to get the given attribute as a {@link ValueExpression}, or <code>null</code> if there is no
	 * such attribute.
	 * @param context The involved facelet context.
	 * @param name The attribute name to return the value expression for.
	 * @param type The type of the value expression.
	 * @return The given attribute as a {@link ValueExpression}.
	 */
	protected <T> ValueExpression getValueExpression(FaceletContext context, String name, Class<T> type) {
		TagAttribute attribute = getAttribute(name);
		return (attribute != null) ? attribute.getValueExpression(context, type) : null;
	}


	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * Convenience class which holds all render time attribute setters and value expressions.
	 *
	 * @author Bauke Scholtz
	 */
	protected static class RenderTimeAttributes {

		private Map<Method, ValueExpression> attributes;

		private RenderTimeAttributes() {
			attributes = new HashMap<Method, ValueExpression>();
		}

		private void add(Method setter, ValueExpression valueExpression) {
			attributes.put(setter, valueExpression);
		}

		public void invokeSetters(ELContext elContext, Object object) {
			for (Entry<Method, ValueExpression> entry : attributes.entrySet()) {
				try {
					entry.getKey().invoke(object, entry.getValue().getValue(elContext));
				}
				catch (Exception e) {
					throw new FacesException(e);
				}
			}
		}
	}

}
