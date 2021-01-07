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

import static jakarta.faces.event.PhaseId.RESTORE_VIEW;
import static java.lang.Boolean.TRUE;
import static org.omnifaces.util.Components.createValueExpression;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.el.ValueExpression;
import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PostValidateEvent;
import jakarta.faces.event.PreValidateEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.validator.Validator;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.component.validator.ValidateMultipleFields;

/**
 * <p>
 * The <code>&lt;o:skipValidators&gt;</code> taghandler allows the developer to entirely skip validation when
 * executing an {@link UICommand} or {@link ClientBehaviorHolder} action. This taghandler must be placed inside an
 * {@link UICommand} or {@link ClientBehaviorHolder} component (client behavior holder components are those components
 * supporting <code>&lt;f:ajax&gt;</code>).
 *
 * <h2>Usage</h2>
 * <p>
 * For example, when adding a new row to the data table, you'd like to not immediately validate all empty rows.
 * <pre>
 * &lt;h:form&gt;
 *     &lt;h:dataTable value="#{bean.items}" var="item"&gt;
 *         &lt;h:column&gt;
 *             &lt;h:inputText value="#{item.value}" required="true" /&gt;
 *         &lt;/h:column&gt;
 *     &lt;/h:dataTable&gt;
 *     &lt;h:commandButton value="add new row" action="#{bean.add}"&gt;
 *         &lt;o:skipValidators /&gt;
 *     &lt;/h:commandButton&gt;
 *     &lt;h:commandButton value="save all data" action="#{bean.save}" /&gt;
 *     &lt;h:messages /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * Note that converters will still run and that model values will still be updated. This behavior is by design.
 *
 * @author Michele Mariotti
 * @author Bauke Scholtz
 * @since 2.3
 */
public class SkipValidators extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String UIINPUT_REQUIRED_PROPERTY = "required";
	private static final String UIINPUT_DISABLED_PROPERTY = "disabled";

	private static final String ERROR_INVALID_PARENT =
		"Parent component of o:skipValidators must be an instance of UICommand or ClientBehaviorHolder.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public SkipValidators(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the parent component is an instance of {@link UICommand} or {@link ClientBehaviorHolder}, and is new, and
	 * we're in the restore view phase of a postback, then delegate to {@link #processSkipValidators(UIComponent)}.
	 * @throws IllegalStateException When the parent component is not an instance of {@link UICommand} or
	 * {@link ClientBehaviorHolder}.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!(parent instanceof UICommand || parent instanceof ClientBehaviorHolder)) {
			throw new IllegalStateException(ERROR_INVALID_PARENT);
		}

		FacesContext facesContext = context.getFacesContext();

		if (!(ComponentHandler.isNew(parent) && facesContext.isPostback() && facesContext.getCurrentPhaseId() == RESTORE_VIEW)) {
			return;
		}

		// We can't use hasInvokedSubmit() before the component is added to view, because the client ID isn't available.
		// Hence, we subscribe this check to after phase of restore view.
		subscribeToRequestAfterPhase(RESTORE_VIEW, () -> processSkipValidators(parent));
	}

	/**
	 * Check if the given component has been invoked during the current request and if so, then register the skip
	 * validators event listener which removes the validators during {@link PreValidateEvent} and restores them during
	 * {@link PostValidateEvent}.
	 * @param parent The parent component of this tag.
	 */
	protected void processSkipValidators(UIComponent parent) {
		if (!hasInvokedSubmit(parent)) {
			return;
		}

		SkipValidatorsEventListener listener = new SkipValidatorsEventListener();
		subscribeToViewEvent(PreValidateEvent.class, listener);
		subscribeToViewEvent(PostValidateEvent.class, listener);
	}

	/**
	 * Remove validators during prevalidate and restore them during postvalidate.
	 */
	static class SkipValidatorsEventListener implements SystemEventListener {

		private Map<String, Object> attributes = new HashMap<>();
		private Map<String, Validator<?>[]> allValidators = new HashMap<>();

		@Override
		public boolean isListenerForSource(Object source) {
			return source instanceof UIInput || source instanceof ValidateMultipleFields;
		}

		@Override
		public void processEvent(SystemEvent event) {
			UIComponent source = (UIComponent) event.getSource();

			if (source instanceof UIInput) {
				processEventForUIInput(event, (UIInput) source);
			}
			else if (source instanceof ValidateMultipleFields) {
				processEventForValidateMultipleFields(event, (ValidateMultipleFields) source);
			}
		}

		private void processEventForUIInput(SystemEvent event, UIInput input) {
			String clientId = input.getClientId();

			if (event instanceof PreValidateEvent) {
				ValueExpression requiredExpression = input.getValueExpression(UIINPUT_REQUIRED_PROPERTY);

				if (requiredExpression != null) {
					attributes.put(clientId, requiredExpression);
					input.setValueExpression(UIINPUT_REQUIRED_PROPERTY, createValueExpression("#{false}", Boolean.class));
				}
				else {
					attributes.put(clientId, input.isRequired());
					input.setRequired(false);
				}

				Validator<?>[] validators = input.getValidators();
				allValidators.put(clientId, validators);

				for (Validator<?> validator : validators) {
					input.removeValidator(validator);
				}
			}
			else if (event instanceof PostValidateEvent) {
				for (Validator<?> validator : allValidators.remove(clientId)) {
					input.addValidator(validator);
				}

				Object requiredValue = attributes.remove(clientId);

				if (requiredValue instanceof ValueExpression) {
					input.setValueExpression(UIINPUT_REQUIRED_PROPERTY, (ValueExpression) requiredValue);
				}
				else {
					input.setRequired(TRUE.equals(requiredValue));
				}
			}
		}

		private void processEventForValidateMultipleFields(SystemEvent event, ValidateMultipleFields validator) {
			String clientId = validator.getClientId();

			if (event instanceof PreValidateEvent) {
				ValueExpression disabledExpression = validator.getValueExpression(UIINPUT_DISABLED_PROPERTY);
				attributes.put(clientId, (disabledExpression != null) ? disabledExpression : validator.isDisabled());
				validator.setDisabled(true);
			}
			else if (event instanceof PostValidateEvent) {
				Object disabledValue = attributes.remove(clientId);

				if (disabledValue instanceof ValueExpression) {
					validator.setValueExpression(UIINPUT_DISABLED_PROPERTY, (ValueExpression) disabledValue);
				}
				else {
					validator.setDisabled(TRUE.equals(disabledValue));
				}
			}
		}

	}

}
