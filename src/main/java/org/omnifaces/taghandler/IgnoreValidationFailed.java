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

import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToViewBeforePhase;
import static org.omnifaces.util.Faces.setContextAttribute;

import java.io.IOException;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.component.input.Form;
import org.omnifaces.util.Callback;

/**
 * <p>
 * The <code>&lt;o:ignoreValidationFailed&gt;</code> taghandler allows the developer to ignore validation failures when
 * executing an {@link UICommand} action. This taghandler must be placed inside an {@link UICommand} component and the
 * parent {@link UIForm} must be an <code>&lt;o:form&gt;</code>. When executing an ajax action, make sure that the
 * parent {@link UIForm} is also included in the <code>&lt;f:ajax execute&gt;</code>.
 *
 * <h3>Usage</h3>
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
	 * This will create a {@link PhaseListener} for the before phase of {@link PhaseId#PROCESS_VALIDATIONS} which will
	 * in turn check if the parent {@link UICommand} component has been invoked during the postback and if so, then
	 * set a context attribute which informs the {@link Form} to ignore the validation.
	 */
	@Override
	public void apply(FaceletContext context, final UIComponent parent) throws IOException {
		if (!(parent instanceof UICommand)) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		if (ComponentHandler.isNew(parent)) {
			subscribeToViewBeforePhase(PhaseId.PROCESS_VALIDATIONS, new Callback.Void() {

				@Override
				public void invoke() {
					if (hasInvokedSubmit(parent)) {
						setContextAttribute(IgnoreValidationFailed.class.getName(), true);
					}
				}
			});
		}
		else {
			if (getClosestParent(parent, Form.class) == null) {
				throw new IllegalArgumentException(ERROR_INVALID_FORM);
			}
		}
	}

}