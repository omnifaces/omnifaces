package org.omnifaces.util;

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
	
	/**
	 * Return the String value of the given attribute
	 * @param tagAttribute The attribute to retrieve the value from
	 * @param context FaceletContext to use
	 * @return String value of this attribute, or null if the attribute is null
	 */
	public static String getString(TagAttribute tagAttribute, FaceletContext context) {
		return tagAttribute != null ? tagAttribute.getValue(context) : null;
	}
	
	/**
	 * Return the boolean value of the given attribute
	 * @param tagAttribute The attribute to retrieve the value from
	 * @param context FaceletContext to use
	 * @return boolean value of this attribute, or false if the attribute is null
	 */
	public static boolean getBoolean(TagAttribute tagAttribute, FaceletContext context) {
		return tagAttribute != null ? tagAttribute.getBoolean(context) : false;
	}
	
	/**
	 * Return the Object value of the given attribute
	 * @param tagAttribute The attribute to retrieve the value from
	 * @param context FaceletContext to use
	 * @return Object value of this attribute, or null if the attribute is null
	 */
	public static Object getObject(TagAttribute tagAttribute, FaceletContext context) {
		return tagAttribute != null ? tagAttribute.getObject(context) : null;
	}
}
