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

import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.addBeforePhaseListener;
import static org.omnifaces.util.Events.subscribeToViewEvent;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.event.PhaseId;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.eventlistener.BeanValidationEventListener;
import org.omnifaces.util.Callback;

/**
 * The <code>&lt;o:validateBean&gt;</code> allows the developer to control bean validation on a per-{@link UICommand}
 * or {@link UIInput} component basis. The standard <code>&lt;f:validateBean&gt;</code> only allows that on a per-form
 * or a per-request basis (by using multiple tags and conditional EL expressions in its attributes) which may end up in
 * boilerplate code.
 * <p>
 * Usage examples:
 * <pre>
 * &lt;h:commandButton value="submit" action="#{bean.submit}"&gt;
 *     &lt;o:validateBean validationGroups="javax.validation.groups.Default,com.example.MyGroup"/&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}"
 *     &lt;o:validateBean disabled="true" /&gt;
 *     &lt;f:ajax execute="@form" listener="#{bean.itemChanged}" render="@form" /&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 */
public class ValidateBean extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"Parent component of o:validateBean must be an instance of UICommand or UIInput.";

	// Variables ------------------------------------------------------------------------------------------------------

	private TagAttribute validationGroups;
	private TagAttribute disabled;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ValidateBean(TagConfig config) {
		super(config);
		validationGroups = getAttribute("validationGroups");
		disabled = getAttribute("disabled");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 *
	 */
	@Override
	public void apply(FaceletContext context, final UIComponent parent) throws IOException {
		if (!(parent instanceof UICommand || parent instanceof UIInput)) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		final String validationGroups = this.validationGroups != null ? this.validationGroups.getValue(context) : null;
		final boolean disabled = this.disabled != null ? this.disabled.getBoolean(context) : false;
		addBeforePhaseListener(PhaseId.PROCESS_VALIDATIONS, new Callback.Void() {

			@Override
			public void invoke() {
				if (hasInvokedSubmit(parent)) {
					SystemEventListener listener = new BeanValidationEventListener(validationGroups, disabled);
					subscribeToViewEvent(PreValidateEvent.class, listener);
					subscribeToViewEvent(PostValidateEvent.class, listener);
				}
			}
		});
	}

}