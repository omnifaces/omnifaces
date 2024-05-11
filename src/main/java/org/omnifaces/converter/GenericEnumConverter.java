/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *	 https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.converter;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.omnifaces.el.ExpressionInspector.getMethodReference;
import static org.omnifaces.util.Components.VALUE_ATTRIBUTE;
import static org.omnifaces.util.Components.getAttribute;
import static org.omnifaces.util.Faces.getViewAttribute;
import static org.omnifaces.util.Faces.setViewAttribute;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Utils.isOneOf;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectMany;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.EnumConverter;
import javax.faces.convert.FacesConverter;

import org.omnifaces.util.Utils;

/**
 * <p>
 * The <code>omnifaces.GenericEnumConverter</code> is intended for use in {@link UISelectMany} components whose value is
 * been bound to a <code>List&lt;E&gt;</code> property where <code>E</code> is an enum. Even though JSF has already a
 * built-in {@link EnumConverter}, this doesn't work for a <code>List&lt;E&gt;</code> property as the generic type
 * information <code>E</code> is lost during runtime. The list would be filled with unconverted <code>String</code>
 * values instead which may in turn cause <code>ClassCastException</code> during postprocessing in the business logic.
 * <p>
 * This can be solved by using a <code>E[]</code> property instead of <code>List&lt;E&gt;</code> (e.g.
 * <code>Role[]</code> in case of a <code>Role</code> enum). If this is however is not an option due to some design
 * restrictions (e.g. JPA <code>@ElementCollection</code>, etc), then you'd need to create an explicit converter for the
 * enum type like follows:
 * <pre>
 * &#64;FacesConverter("roleConverter")
 * public class RoleConverter extends EnumConverter {
 *	 public RoleConverter() {
 *		 super(Role.class);
 *	 }
 * }
 * </pre>
 * <pre>
 * &lt;h:selectManyCheckbox value="#{bean.selectedRoles}" converter="roleConverter"&gt;
 *	 &lt;f:selectItems value="#{bean.availableRoles}" /&gt;
 * &lt;/h:selectManyCheckbox&gt;
 * </pre>
 * <p>
 * However, creating a new converter for every single enum type, only and only for use in {@link UISelectMany} with a
 * <code>List&lt;E&gt;</code> property, may be a bit clumsy. This generic enum converter is intended to remove the need
 * to create a new enum converter every time.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.GenericEnumConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the multi-selection component holding <code>&lt;f:selectItems&gt;</code>.
 * example:
 * <pre>
 * &lt;h:selectManyCheckbox value="#{bean.selectedEnums}" converter="omnifaces.GenericEnumConverter"&gt;
 *	 &lt;f:selectItems value="#{bean.availableEnums}" /&gt;
 * &lt;/h:selectManyCheckbox&gt;
 * </pre>
 *
 * <p><strong>See also</strong>:
 * <br><a href="https://stackoverflow.com/q/3822058/157882">Use enum in &lt;h:selectManyCheckbox&gt;</a>
 *
 * <h3>JSF 2.3</h3>
 * <p>
 * This converter is not necessary anymore since JSF 2.3 thanks to the fixes in
 * <a href="https://github.com/javaee/javaserverfaces-spec/issues/1422">issue 1422</a>.
 * <pre>
 * &lt;h:selectManyCheckbox value="#{bean.selectedEnums}"&gt;
 *	 &lt;f:selectItems value="#{bean.availableEnums}" /&gt;
 * &lt;/h:selectManyCheckbox&gt;
 * </pre>
 * <p>
 * However, when you're having an input component without a value attribute, and thus the exact type cannot be
 * automatically determined by simply inspecting the return type of the associated getter method, then this converter
 * may be still useful.
 * <pre>
 * &lt;h:selectManyCheckbox converter="omnifaces.GenericEnumConverter"&gt;
 *	 &lt;f:selectItems value="#{bean.availableEnums}" /&gt;
 * &lt;/h:selectManyCheckbox&gt;
 * </pre>
 *
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
@FacesConverter(value = "omnifaces.GenericEnumConverter")
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericEnumConverter implements Converter<Enum> {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ATTRIBUTE_ENUM_TYPE = "GenericEnumConverter.%s";
	private static final String ERROR_NO_ENUM_VALUE = "Given value ''{0}'' is not an enum of type ''{1}''.";
	private static final String ERROR_NO_ENUM_TYPE = "Cannot determine enum type, use standard EnumConverter instead.";

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public String getAsString(FacesContext context, UIComponent component, Enum modelValue) {
		if (modelValue == null) {
			return "-";
		}

		Class<Enum> enumType = modelValue.getDeclaringClass();
		component.getAttributes().put(ATTRIBUTE_ENUM_TYPE, enumType);
		setViewAttribute(format(ATTRIBUTE_ENUM_TYPE, component.getClientId(context)), enumType);
		return modelValue.name();
	}

	@Override
	public Enum getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (isOneOf(submittedValue, null, "", "-")) {
			return null;
		}

		Class<Enum> enumType = Utils.coalesce(
		    getAttribute(component, ATTRIBUTE_ENUM_TYPE),
		    getViewAttribute(format(ATTRIBUTE_ENUM_TYPE, component.getClientId(context)))
		);

		if (enumType == null) {
			try {
				ValueExpression valueExpression = component.getValueExpression(VALUE_ATTRIBUTE);
				Method getter = getMethodReference(context.getELContext(), valueExpression).getMethod();
				enumType = (Class<Enum>) ((ParameterizedType) getter.getGenericReturnType()).getActualTypeArguments()[0];
				component.getAttributes().put(ATTRIBUTE_ENUM_TYPE, requireNonNull(enumType));
			}
			catch (Exception e) {
				throw new ConverterException(createError(ERROR_NO_ENUM_TYPE), e);
			}
		}

		try {
			return Enum.valueOf(enumType, submittedValue);
		}
		catch (IllegalArgumentException e) {
			throw new ConverterException(createError(ERROR_NO_ENUM_VALUE, submittedValue, enumType), e);
		}
	}

}