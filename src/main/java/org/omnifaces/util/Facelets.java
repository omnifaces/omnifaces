/*
 * Copyright 2016 OmniFaces
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

import javax.el.ValueExpression;
import javax.enterprise.inject.Typed;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;

/**
 * <p>
 * Collection of utility methods for Facelets code.
 *
 *@author Arjan Tijms
 *@since 2.0
*/
@Typed
public final class Facelets {

	private static final String ERROR_EL_DISALLOWED = "The '%s' attribute may not be an EL expression.";

	private Facelets() {
		// Hide constructor.
	}

	/**
	 * Returns the String value of the given tag attribute.
	 * @param context The involved Facelet context.
	 * @param tagAttribute The tag attribute to retrieve the value from.
	 * @return The String value of the given tag attribute, or null if the tag attribute is null.
	 */
	public static String getString(FaceletContext context, TagAttribute tagAttribute) {
		return tagAttribute != null ? tagAttribute.getValue(context) : null;
	}

	/**
	 * Returns the String literal of the given tag attribute.
	 * @param tagAttribute The tag attribute to retrieve the value from.
	 * @param name The tag attribute name; this is only used in exception message.
	 * @return The String literal of the given tag attribute, or null if the tag attribute is null.
	 * @throws IllegalArgumentException When the attribute is not a literal.
	 * @since 2.6
	 */
	public static String getStringLiteral(TagAttribute tagAttribute, String name) {
		if (tagAttribute != null) {
			if (tagAttribute.isLiteral()) {
				return tagAttribute.getValue();
			}
			else {
				throw new IllegalArgumentException(String.format(ERROR_EL_DISALLOWED,  name));
			}
		}

		return null;
	}

	/**
	 * Returns the boolean value of the given tag attribute.
	 * @param context The involved Facelet context.
	 * @param tagAttribute The tag attribute to retrieve the value from.
	 * @return The boolean value of the given tag attribute, or false if the tag attribute is null.
	 */
	public static boolean getBoolean(FaceletContext context, TagAttribute tagAttribute) {
		return tagAttribute != null ? tagAttribute.getBoolean(context) : false;
	}

	/**
	 * Returns the Object value of the given tag attribute
	 * @param context The involved Facelet context.
	 * @param tagAttribute The tag attribute to retrieve the value from.
	 * @return The Object value of the given tag attribute, or null if the tag attribute is null.
	 */
	public static Object getObject(FaceletContext context, TagAttribute tagAttribute) {
		return tagAttribute != null ? tagAttribute.getObject(context) : null;
	}

	/**
	 * Returns the typed Object value of the given tag attribute
	 * @param <T> The expected return type.
	 * @param context The involved Facelet context.
	 * @param tagAttribute The tag attribute to retrieve the value from.
	 * @param type The expected type of the Object value.
	 * @return The typed Object value of the given tag attribute, or null if the tag attribute is null.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObject(FaceletContext context, TagAttribute tagAttribute, Class<?> type) {
		return tagAttribute != null ? (T) tagAttribute.getValueExpression(context, type).getValue(context) : null;
	}

	/**
	 * Returns the value of the given tag attribute as a value expression, so it can be carried around and evaluated at
	 * a later moment in the lifecycle without needing the Facelet context.
	 * @param context The involved Facelet context.
	 * @param tagAttribute The tag attribute to extract the value expression from.
	 * @param type The expected type of the value behind the value expression.
	 * @return The value of the given tag attribute as a value expression, or null if the tag attribute is null.
	 */
	public static ValueExpression getValueExpression(FaceletContext context, TagAttribute tagAttribute, Class<?> type) {
		return tagAttribute != null ? tagAttribute.getValueExpression(context, type) : null;
	}

}