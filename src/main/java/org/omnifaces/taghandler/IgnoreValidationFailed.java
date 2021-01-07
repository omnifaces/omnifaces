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
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;

import java.io.IOException;

import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.component.input.Form;

/**
 * <p>
 * The <code>&lt;o:ignoreValidationFailed&gt;</code> taghandler allows the developer to ignore validation failures when
 * executing an {@link UICommand} action. This taghandler must be placed inside an {@link UICommand} component and the
 * parent {@link UIForm} must be an <code>&lt;o:form&gt;</code>. When executing an ajax action, make sure that the
 * parent {@link UIForm} is also included in the <code>&lt;f:ajax execute&gt;</code>.
 *
 * <h2>Usage</h2>
 * <p>
 * For example:
 * <pre>
 * &lt;o:form&gt;
 *     ...
 *     &lt;h:commandButton value="save valid data" action="#{bean.saveValidData}"&gt;
 *         &lt;o:ignoreValidationFailed /&gt;
 *         &lt;f:ajax execute="@form" /&gt;
 *     &lt;/h:commandButton&gt;
 * &lt;/o:form&gt;
 * </pre>
 * <p>
 * Note that the model values will (obviously) only be updated for components which have actually passed the validation.
 * Also the validation messages will still be displayed. If you prefer to not display them, then you'd need to exclude
 * them from rendering by <code>&lt;f:ajax render&gt;</code>, or to put a proper condition in the <code>rendered</code>
 * attribute.
 *
 * @author Bauke Scholtz
 * @see Form
 */
public class IgnoreValidationFailed extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"Parent component of o:ignoreValidationFailed must be an instance of UICommand.";
	private static final String ERROR_INVALID_FORM =
		"Parent form of o:ignoreValidationFailed must be an o:form, not h:form.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public IgnoreValidationFailed(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the parent component is an instance of {@link UICommand} and is new and we're in the restore view phase of
	 * a postback, then delegate to {@link #processIgnoreValidationFailed(UICommand)}.
	 * @throws IllegalStateException When the parent component is not an instance of {@link UICommand}.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!(parent instanceof UICommand)) {
			throw new IllegalStateException(ERROR_INVALID_PARENT);
		}

		FacesContext facesContext = context.getFacesContext();

		if (!(ComponentHandler.isNew(parent) && facesContext.isPostback() && facesContext.getCurrentPhaseId() == RESTORE_VIEW)) {
			return;
		}

		// We can't use hasInvokedSubmit() before the component is added to view, because the client ID isn't available.
		// Hence, we subscribe this check to after phase of restore view.
		subscribeToRequestAfterPhase(RESTORE_VIEW, () -> processIgnoreValidationFailed((UICommand) parent));
	}

	/**
	 * Check if the given command component has been invoked during the current request and if so, then instruct the
	 * parent <code>&lt;o:form&gt;</code> to ignore the validation.
	 * @param command The command component.
	 * @throws IllegalStateException When the given command component is not inside a <code>&lt;o:form&gt;</code>.
	 */
	protected void processIgnoreValidationFailed(UICommand command) {
		if (!hasInvokedSubmit(command)) {
			return;
		}

		Form form = getClosestParent(command, Form.class);

		if (form == null) {
			throw new IllegalStateException(ERROR_INVALID_FORM);
		}

		form.setIgnoreValidationFailed(true);
	}

}