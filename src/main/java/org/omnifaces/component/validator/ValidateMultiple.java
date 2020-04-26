/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.component.validator;

import java.util.List;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.omnifaces.validator.MultiFieldValidator;

/**
 * <p>
 * The <code>&lt;o:validateMultiple&gt;</code> allows the developer to validate multiple fields by either a custom
 * validator method:
 * <pre>
 * &lt;o:validateMultiple id="myId" components="foo bar baz" validator="#{bean.someMethod}" /&gt;
 * &lt;h:message for="myId" /&gt;
 * &lt;h:inputText id="foo" /&gt;
 * &lt;h:inputText id="bar" /&gt;
 * &lt;h:inputText id="baz" /&gt;
 * </pre>
 * <p>whereby the method has the following signature (method name is free to your choice):
 * <pre>
 * public boolean someMethod(FacesContext context, List&lt;UIInput&gt; components, List&lt;Object&gt; values) {
 *     // ...
 * }
 * </pre>
 * <p>Or, by a managed bean instance which implements the {@link MultiFieldValidator} interface:
 * <pre>
 * &lt;o:validateMultiple id="myId" components="foo bar baz" validator="#{validateValuesBean}" /&gt;
 * &lt;h:message for="myId" /&gt;
 * &lt;h:inputText id="foo" /&gt;
 * &lt;h:inputText id="bar" /&gt;
 * &lt;h:inputText id="baz" /&gt;
 * </pre>
 * <pre>
 * &#64;ManagedBean
 * &#64;RequestScoped
 * public class ValidateValuesBean implements MultiFieldValidator {
 *     &#64;Override
 *     public boolean validateValues(FacesContext context, List&lt;UIInput&gt; components, List&lt;Object&gt; values) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <h3>Design notice</h3>
 * <p>
 * Note that this validator does <strong>not</strong> throw {@link ValidatorException}, but returns a boolean! Message
 * handling and invalidation job is up to the {@link ValidateMultipleFields} implementation who will call this method.
 * You can customize the message by the <code>message</code> attribute of the tag. Refer {@link ValidateMultipleFields}
 * documentation for general usage instructions.
 *
 * @author Juliano Marques
 * @author Bauke Scholtz
 * @since 1.7
 * @see ValidateMultipleHandler
 * @see ValidateMultipleFields
 * @see ValidatorFamily
 * @see MultiFieldValidator
 */
@FacesComponent(ValidateMultiple.COMPONENT_TYPE)
public class ValidateMultiple extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateMultiple";

	// Private constants ----------------------------------------------------------------------------------------------

	private enum PropertyKeys {
		validateMethod
	}

	// Vars -----------------------------------------------------------------------------------------------------------

	private MultiFieldValidator validator;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Invoke the validator and return its outcome.
	 */
	@Override
	public boolean validateValues(FacesContext context, List<UIInput> components, List<Object> values) {
		if (validator != null) {
			return validator.validateValues(context, components, values);
		}
		else {
			ELContext elContext = context.getELContext();
			return (Boolean) getValidateMethod().invoke(elContext, new Object[] { context, components, values });
		}
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns the validator instance.
	 * @return The validator instance.
	 */
	public MultiFieldValidator getValidator() {
		return validator;
	}

	/**
	 * Sets the validator instance.
	 * @param validator The validator instance.
	 */
	public void setValidator(MultiFieldValidator validator) {
		this.validator = validator;
	}

	/**
	 * Returns the validator method expression.
	 * @return The validator method expression.
	 */
	public MethodExpression getValidateMethod() {
		return (MethodExpression) getStateHelper().eval(PropertyKeys.validateMethod);
	}

	/**
	 * Sets the validator method expression.
	 * @param validateMethod The validator method expression.
	 */
	public void setValidateMethod(MethodExpression validateMethod) {
		getStateHelper().put(PropertyKeys.validateMethod, validateMethod);
	}

}