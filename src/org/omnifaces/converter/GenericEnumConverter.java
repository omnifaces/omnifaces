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
package org.omnifaces.converter;

import static org.omnifaces.util.Faces.getViewAttribute;
import static org.omnifaces.util.Faces.setViewAttribute;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectMany;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

import org.omnifaces.util.Messages;

/**
 * This generic enum converter is intended for use in {@link UISelectMany} components whose value is been bound to a
 * <code>List&lt;E&gt;</code> property where <code>E</code> is an enum. Even though JSF has already a built-in
 * {@link EnumConverter}, this doesn't work for a <code>List&lt;E&gt;</code> property as the generic type information
 * <code>E</code> is lost during runtime. The list would be filled with unconverted <code>String</code> values instead
 * which may in turn cause <code>ClassCastException</code>.
 * <p>
 * If replacing the <code>List&lt;E&gt;</code> property by a <code>T[]</code> (e.g. <code>Role[]</code> in case of a
 * <code>Role</code> enum) is not an option due to design restrictions (e.g. JPA <code>@ElementCollection</code>, etc),
 * then you'd need to create an explicit converter for the enum type like follows:
 * <pre>
 * {@literal @}FacesConverter("roleConverter")
 * public class RoleConverter extends EnumConverter {
 *     public RoleConverter() {
 *         super(Role.class);
 *     }
 * }
 * </pre>
 * <p>
 * However, creating a new converter for every single enum type, only and only for use in {@link UISelectMany} with a
 * <code>List&lt;E&gt;</code> property, may be a bit clumsy. This generic enum converter is intended to remove the need
 * to create a new enum converter every time.
 * <p>
 * This converter is available by converter ID <code>omnifaces.GenericEnumConverter</code>. Basic usage example:
 * <pre>
 * &lt;h:selectManyCheckbox value="#{bean.selectedEnums}" converter="omnifaces.GenericEnumConverter"&gt;
 *   &lt;f:selectItems value="#{bean.availableEnums}" /&gt;
 * &lt;/h:selectManyCheckbox&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @link http://stackoverflow.com/q/3822058/157882
 * @since 1.2
 */
@FacesConverter(value = "omnifaces.GenericEnumConverter")
public class GenericEnumConverter implements Converter {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ATTRIBUTE_ENUM_TYPE = "GenericEnumConverter.%s";
	private static final String ERROR_NO_ENUM_TYPE = "Given type ''{0}'' is not an enum.";
	private static final String ERROR_NO_ENUM_VALUE = "Given value ''{0}'' is not an enum of type ''{1}''.";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
		if (modelValue == null) {
			return "-";
		}

		if (modelValue instanceof Enum) {
			Class<Enum> enumType = ((Enum) modelValue).getDeclaringClass();
			setViewAttribute(String.format(ATTRIBUTE_ENUM_TYPE, component.getClientId(context)), enumType);
			return ((Enum) modelValue).name();
		}
		else {
			throw new ConverterException(Messages.createError(ERROR_NO_ENUM_TYPE, modelValue.getClass()));
		}
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (submittedValue == null || submittedValue.isEmpty() || submittedValue.equals("-")) {
			return null;
		}

		Class<Enum> enumType = getViewAttribute(String.format(ATTRIBUTE_ENUM_TYPE, component.getClientId(context)));

		try {
			return Enum.valueOf(enumType, submittedValue);
		}
		catch (IllegalArgumentException e) {
			throw new ConverterException(Messages.createError(ERROR_NO_ENUM_VALUE, submittedValue, enumType));
		}
	}

}