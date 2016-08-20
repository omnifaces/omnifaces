/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.taghandler;

import static java.lang.Boolean.TRUE;
import static javax.faces.event.PhaseId.RESTORE_VIEW;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.util.Callback;

/**
 * <p>
 * The <code>&lt;o:skipValidators&gt;</code> taghandler allows the developer to entirely skip validation when
 * executing an {@link UICommand} or {@link ClientBehaviorHolder} action. This taghandler must be placed inside an
 * {@link UICommand} or {@link ClientBehaviorHolder} component (client behavior holder components are those components
 * supporting <code>&lt;f:ajax&gt;</code>).
 *
 * <h3>Usage</h3>
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
	public void apply(FaceletContext context, final UIComponent parent) throws IOException {
		if (!(parent instanceof UICommand || parent instanceof ClientBehaviorHolder)) {
			throw new IllegalStateException(ERROR_INVALID_PARENT);
		}

		FacesContext facesContext = context.getFacesContext();

		if (!(ComponentHandler.isNew(parent) && facesContext.isPostback() && facesContext.getCurrentPhaseId() == RESTORE_VIEW)) {
			return;
		}

		// We can't use hasInvokedSubmit() before the component is added to view, because the client ID isn't available.
		// Hence, we subscribe this check to after phase of restore view.
		subscribeToRequestAfterPhase(RESTORE_VIEW, new Callback.Void() { @Override public void invoke() {
			processSkipValidators(parent);
		}});
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

		private Map<String, Object> required = new HashMap<>();
		private Map<String, Validator[]> allValidators = new HashMap<>();

		@Override
		public boolean isListenerForSource(Object source) {
			return source instanceof UIInput;
		}

		@Override
		public void processEvent(SystemEvent event) throws AbortProcessingException {
			UIInput input = (UIInput) event.getSource();
			String clientId = input.getClientId();

			if (event instanceof PreValidateEvent) {
				ValueExpression requiredExpression = input.getValueExpression("required");
				required.put(clientId, (requiredExpression != null) ? requiredExpression : input.isRequired());
				input.setRequired(false);
				Validator[] validators = input.getValidators();
				allValidators.put(clientId, validators);

				for (Validator validator : validators) {
					input.removeValidator(validator);
				}
			}
			else if (event instanceof PostValidateEvent) {
				Object requiredValue = required.remove(clientId);

				if (requiredValue instanceof ValueExpression) {
					input.setValueExpression("required", (ValueExpression) requiredValue);
				}
				else {
					input.setRequired(TRUE.equals(requiredValue));
				}

				for (Validator validator : allValidators.remove(clientId)) {
					input.addValidator(validator);
				}
			}
		}

	}

}
