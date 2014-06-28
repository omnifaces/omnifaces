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
package org.omnifaces.component.validator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.ResourceBundle.getBundle;
import static org.omnifaces.util.Faces.getLocale;
import static org.omnifaces.util.Faces.getMessageBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectBoolean;
import javax.faces.context.FacesContext;

import org.omnifaces.util.Components;
import org.omnifaces.util.Messages;
import org.omnifaces.util.State;
import org.omnifaces.validator.MultiFieldValidator;

/**
 * Base class which is to be shared between all multi field validators. The implementors have to call the super
 * constructor with the default message. The implementors have to override the
 * {@link #validateValues(FacesContext, List, List)} method to perform the actual validation.
 * <hr>
 * <h3>General usage of all multiple field validators</h3>
 * <p>
 * This validator must be placed inside the same <code>UIForm</code> as the <code>UIInput</code> components in question.
 * The <code>UIInput</code> components must be referenced by a space separated collection of their client IDs in the
 * <code>components</code> attribute. This validator can be placed anywhere in the form, but keep in mind that the
 * components will be validated in the order as they appear in the form. So if this validator is been placed before all
 * of the components, then it will be executed before any of the component's own validators. If this validator fails,
 * then the component's own validators will not be fired. If this validator is been placed after all of the components,
 * then it will be executed after any of the component's own validators. If any of them fails, then this validator
 * will not be exeucted. It is not recommended to put this validator somewhere in between the referenced components as
 * the resulting behaviour may be confusing. Put this validator either before or after all of the components, depending
 * on how you would like to prioritize the validation.
 * <pre>
 * &lt;o:validateMultipleFields id="myId" components="foo bar baz" /&gt;
 * &lt;h:message for="myId" /&gt;
 * &lt;h:inputText id="foo" /&gt;
 * &lt;h:inputText id="bar" /&gt;
 * &lt;h:inputText id="baz" /&gt;
 * </pre>
 * <p>
 * By default, in an invalidating case, all of the referenced components will be marked invalid and a faces message will
 * be added on the client ID of this validator component. The default message can be changed by the <code>message</code>
 * attribute. Any "{0}" placeholder in the message will be substituted with a comma separated string of labels of the
 * referenced input components.
 * <pre>
 * &lt;o:validateMultipleFields components="foo bar baz" message="{0} are wrong!" /&gt;
 * </pre>
 * <p>
 * You can also change the default message in the message bundle file as identified by
 * <code>&lt;application&gt;&lt;message-bundle&gt;</code> in <code>faces-config.xml</code>. The message key is just
 * the component type as identified by <code>COMPONENT_TYPE</code> constant of the validator component. For example,
 * {@link ValidateAll} has a {@link ValidateAll#COMPONENT_TYPE} value of
 * <code>org.omnifaces.component.validator.ValidateAll</code>. Use exactly this value as message bundle key:
 * <pre>
 * org.omnifaces.component.validator.ValidateAll = {0} are wrong!
 * </pre>
 * <p>
 * You can use <code>invalidateAll="false"</code> to mark only those components which are actually invalid as invalid.
 * In case of for example "input all" or "input all or none" validation, that would be only the fields which are left
 * empty.
 * <pre>
 * &lt;o:validateMultipleFields components="foo bar baz" message="{0} are wrong!" invalidateAll="false" /&gt;
 * </pre>
 * <p>
 * The faces message can also be shown for all of the referenced components using <code>showMessageFor="@all"</code>.
 * <pre>
 * &lt;o:validateMultipleFields components="foo bar baz" message="This is wrong!" showMessageFor="@all" /&gt;
 * &lt;h:inputText id="foo" /&gt;
 * &lt;h:message for="foo" /&gt;
 * &lt;h:inputText id="bar" /&gt;
 * &lt;h:message for="bar" /&gt;
 * &lt;h:inputText id="baz" /&gt;
 * &lt;h:message for="baz" /&gt;
 * </pre>
 * <p>
 * The faces message can also be shown for only the invalidated components using <code>showMessageFor="@invalid"</code>.
 * <pre>
 * &lt;o:validateMultipleFields components="foo bar baz" message="This is wrong!" showMessageFor="@invalid" /&gt;
 * </pre>
 * <p>
 * The faces message can also be shown for specific components referenced by a space separated collection of their
 * client IDs in <code>showMessageFor</code> attribute.
 * <pre>
 * &lt;o:validateMultipleFields components="foo bar baz" message="This is wrong!" showMessageFor="foo baz" /&gt;
 * </pre>
 * <p>
 * The <code>showMessageFor</code> attribute defaults to <code>@this</code>.
 * <p>
 * The validator can be disabled by the <code>disabled</code> attribute. It accepts a request based EL expression.
 * <pre>
 * &lt;o:validateMultipleFields components="foo bar baz" disabled="#{param.validationDisabled}" /&gt;
 * </pre>
 * <p>
 * There is a read-only <code>validationFailed</code> attribute which can be used to determine if the validation by
 * this component has failed.
 * <pre>
 * &lt;o:validateMultipleFields id="myId" binding="#{myId}" components="foo bar baz" /&gt;
 * &lt;h:panelGroup rendered="#{myId.validationFailed}"&gt;
 *     Validation has failed! &lt;h:message for="myId" /&gt;
 * &lt;/h:panelGroup&gt;
 * </pre>
 * <p>
 * TODO: support for immediate="true".
 *
 * @author Bauke Scholtz
 */
public abstract class ValidateMultipleFields extends ValidatorFamily implements MultiFieldValidator {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String DEFAULT_MESSAGE_BUNDLE = "org.omnifaces.component.validator.messages";
	private static final String DEFAULT_SHOWMESSAGEFOR = "@this";
	private static final Boolean DEFAULT_INVALIDATEALL = TRUE;
	private static final Boolean DEFAULT_DISABLED = FALSE;

	private static final String ERROR_MISSING_COMPONENTS =
		"%s attribute 'components' must be specified.";
	private static final String ERROR_UNKNOWN_COMPONENT =
		"%s attribute '%s' must refer existing client IDs. Client ID '%s' cannot be found.";
	private static final String ERROR_INVALID_COMPONENT =
		"%s attribute '%s' must refer UIInput client IDs. Client ID '%s' is of type '%s'.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		components, invalidateAll, message, showMessageFor, disabled;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Properties -----------------------------------------------------------------------------------------------------

	private String defaultMessage;
	private boolean validationFailed;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The default constructor sets the default message and sets the renderer type to <code>null</code>.
	 */
	public ValidateMultipleFields() {
		String componentType = getClass().getAnnotation(FacesComponent.class).value();
		ResourceBundle messageBundle = getMessageBundle();

		if (messageBundle == null) {
			messageBundle = getBundle(DEFAULT_MESSAGE_BUNDLE, getLocale());
		}

		defaultMessage = messageBundle.getString(componentType);
		setRendererType(null);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate our component hierarchy.
	 * @throws IllegalArgumentException When there is no parent of type {@link UIForm}, or when there are any children.
	 */
	@Override
	protected void validateHierarchy() throws IllegalArgumentException {
		Components.validateHasParent(this, UIForm.class);
		Components.validateHasNoChildren(this);
	}

	/**
	 * If the validation is not disabled, collect the components, if it is not empty, then collect their values and
	 * delegate to {@link #validateValues(FacesContext, List, List)}. If it returns <code>false</code>, then mark all
	 * inputs and the faces context invalid and finally delegate to {@link #showMessage(FacesContext, List)} to show
	 * the message.
	 */
	@Override
	protected void validateComponents(FacesContext context) {
		if (isDisabled()) {
			return;
		}

		List<UIInput> inputs = collectComponents();

		if (inputs.isEmpty()) {
			return;
		}

		List<Object> values = collectValues(inputs);

		if (!validateValues(context, inputs, values)) {
			int i = 0;

			for (UIInput input : inputs) {
				input.setValid(!(isInvalidateAll() || shouldInvalidateInput(context, input, values.get(i))));
				i++;
			}

			validationFailed = true;
			context.validationFailed();
			showMessage(context, inputs);
		}
	}

	/**
	 * Collect the input components. Only those which are an instance of {@link UIInput}, are rendered, not disabled nor
	 * readonly will be returned. If at least one of them has already been validated and is been marked invalid, then an
	 * empty collection will be returned.
	 * @return The input components.
	 * @throws IllegalArgumentException When the <code>components</code> attribute is missing, or when it references an
	 * non-existing component, or when it references a non-input component.
	 */
	protected List<UIInput> collectComponents() {
		String components = getComponents();

		if (components.isEmpty()) {
			throw new IllegalArgumentException(String.format(
				ERROR_MISSING_COMPONENTS, getClass().getSimpleName()));
		}

		UIComponent namingContainerParent = getNamingContainer();
		List<UIInput> inputs = new ArrayList<UIInput>();

		for (String clientId : components.split("\\s+")) {
			UIInput input = findInputComponent(namingContainerParent, clientId, PropertyKeys.components);

			if (!Components.isEditable(input)) {
				continue;
			}

			if (!input.isValid()) {
				return Collections.emptyList();
			}

			inputs.add(input);
		}

		return Collections.unmodifiableList(inputs);
	}

	/**
	 * Collect the values of the given input components.
	 * @param inputs The input components to collect values from.
	 * @return The values of the given input components.
	 */
	protected List<Object> collectValues(List<UIInput> inputs) {
		List<Object> values = new ArrayList<Object>(inputs.size());

		for (UIInput input : inputs) {
			Object value = Components.getValue(input);

			if (input instanceof UISelectBoolean && Boolean.FALSE.equals(value)) {
				value = null;
			}

			values.add(value);
		}

		return Collections.unmodifiableList(values);
	}

	/**
	 * Returns whether in in an invalidating case the given input component should be marked invalid. The default
	 * implementation returns <code>true</code>, meaning that <strong>all</strong> input components should be
	 * invalidated in an invalidating case. The overriding implementation may choose to return <code>false</code> for
	 * example when the value is empty, such as {@link ValidateAllOrNone}.
	 * @param context The faces context to work with.
	 * @param input The input component which may need to be invalidated.
	 * @param value The value of the input component.
	 * @since 1.7
	 */
	protected boolean shouldInvalidateInput(FacesContext context, UIInput input, Object value) {
		return true;
	}

	/**
	 * Show the message at the desired place(s) depending on the value of the <code>showMessageFor</code> attribute.
	 * <ul>
	 * <li><code>@this</code>: message will be added to the <code>&lt;h:message&gt;</code> for this component.
	 * <li><code>@all</code>: message will be added to all components as specified in <code>components</code> attribute.
	 * <li>Any other value in a space separated collection will be treated as client ID of {@link UIInput} component.
	 * </ul>
	 * @param context The faces context to work with.
	 * @param inputs The validated input components.
	 */
	protected void showMessage(FacesContext context, List<UIInput> inputs) {
		StringBuilder labels = new StringBuilder();

		for (Iterator<UIInput> iterator = inputs.iterator(); iterator.hasNext();) {
			UIInput input = iterator.next();
			labels.append(Components.getLabel(input));

			if (iterator.hasNext()) {
				labels.append(", ");
			}
		}

		String message = getMessage();
		String showMessageFor = getShowMessageFor();

		if (showMessageFor.equals("@this")) {
			Messages.addError(getClientId(context), message, labels);
		}
		else if (showMessageFor.equals("@all")) {
			for (UIInput input : inputs) {
				Messages.addError(input.getClientId(context), message, labels);
			}
		}
		else if (showMessageFor.equals("@invalid")) {
			for (UIInput input : inputs) {
				if (!input.isValid()) {
					Messages.addError(input.getClientId(context), message, labels);
				}
			}
		}
		else {
			UIComponent namingContainerParent = getNamingContainer();

			for (String clientId : showMessageFor.split("\\s+")) {
				UIInput input = findInputComponent(namingContainerParent, clientId, PropertyKeys.showMessageFor);
				Messages.addError(input.getClientId(context), message, labels);
			}
		}
	}

	private UIInput findInputComponent(UIComponent parent, String clientId, PropertyKeys property) {
		UIComponent found = parent.findComponent(clientId);

		if (found == null) {
			throw new IllegalArgumentException(String.format(
				ERROR_UNKNOWN_COMPONENT, getClass().getSimpleName(), property, clientId));
		}
		else if (!(found instanceof UIInput)) {
			throw new IllegalArgumentException(String.format(
				ERROR_INVALID_COMPONENT, getClass().getSimpleName(), property, clientId, found.getClass().getName()));
		}

		return (UIInput) found;
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the client identifiers of components which are to be validated.
	 * @return The client identifiers of components which are to be validated.
	 */
	public String getComponents() {
		return state.get(PropertyKeys.components, "");
	}

	/**
	 * Sets the client identifiers of components which are to be validated.
	 * @param components The client identifiers of components which are to be validated.
	 */
	public void setComponents(String components) {
		state.put(PropertyKeys.components, components);
	}

	/**
	 * Returns whether to invalidate all fields or only those which are actually invalid as per
	 * {@link #shouldInvalidateInput(FacesContext, UIInput, Object)}
	 * @return Whether to invalidate all fields or only those which are actually invalid.
	 * @since 1.7
	 */
	public Boolean isInvalidateAll() {
		return state.get(PropertyKeys.invalidateAll, DEFAULT_INVALIDATEALL);
	}

	/**
	 * Sets whether to invalidate all fields or only those which are actually invalid as per
	 * {@link #shouldInvalidateInput(FacesContext, UIInput, Object)}
	 * @param invalidateAll Whether to invalidate all fields or only those which are actually invalid.
	 * @since 1.7
	 */
	public void setInvalidateAll(Boolean invalidateAll) {
		state.put(PropertyKeys.invalidateAll, invalidateAll);
	}

	/**
	 * Returns the validation message to be shown.
	 * @return The validation message to be shown.
	 */
	public String getMessage() {
		return state.get(PropertyKeys.message, defaultMessage);
	}

	/**
	 * Sets the validation message to be shown.
	 * @param message The validation message to be shown.
	 */
	public void setMessage(String message) {
		state.put(PropertyKeys.message, message);
	}

	/**
	 * Returns the client identifiers to show the validation message for.
	 * @return The client identifiers to show the validation message for.
	 */
	public String getShowMessageFor() {
		return state.get(PropertyKeys.showMessageFor, DEFAULT_SHOWMESSAGEFOR);
	}

	/**
	 * Sets the client identifiers to show the validation message for.
	 * @param showMessageFor The client identifiers to show the validation message for.
	 */
	public void setShowMessageFor(String showMessageFor) {
		state.put(PropertyKeys.showMessageFor, showMessageFor);
	}

	/**
	 * Returns whether the validation should be disabled or not.
	 * @return Whether the validation should be disabled or not.
	 */
	public Boolean isDisabled() {
		return state.get(PropertyKeys.disabled, DEFAULT_DISABLED);
	}

	/**
	 * Sets whether the validation should be disabled or not.
	 * @param disabled Whether the validation should be disabled or not.
	 */
	public void setDisabled(Boolean disabled) {
		state.put(PropertyKeys.disabled, disabled);
	}

	/**
	 * Returns whether the validation has failed or not.
	 * @return Whether the validation has failed or not.
	 * @since 1.3
	 */
	public boolean isValidationFailed() {
		return validationFailed;
	}

}
