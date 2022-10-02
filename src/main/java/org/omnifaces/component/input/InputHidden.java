/*
 * Copyright OmniFaces
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
package org.omnifaces.component.input;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.html.HtmlInputHidden;
import jakarta.faces.context.FacesContext;

/**
 * <p>
 * The <code>&lt;o:inputHidden&gt;</code> is a component that extends the standard <code>&lt;h:inputHidden&gt;</code>
 * and changes the behavior to immediately convert, validate and update during apply request values phase, regardless of
 * any conversion/validation errors on other <code>UIInput</code> components within the same form. The standard
 * <code>&lt;h:inputHidden&gt;</code> follows the same lifecycle as other <code>UIInput</code> components which is in
 * the end unintuive as hidden input fields are usually under control of the developer.
 * <p>
 * Use case 1: Imagine a form with a <code>&lt;h:inputHidden&gt;</code> and a <code>&lt;h:inputText&gt;</code>. The
 * hidden input holds some prepopulated value of a request scoped bean which is intended to be passed through to the
 * request scoped bean instance of the next request. However, when conversion/validation fails on the text input, then
 * the hidden input won't update the bean property and then becomes empty. I.e. the original value gets permanently lost.
 * This can be bypassed by using ajax to update only specific fields, but this will fail when the update of the hidden
 * input is actually needed (e.g. because the value can possibly be adjusted in action/listener method).
 * <p>
 * Use case 2: Imagine a form with an <code>UIInput</code> or <code>UICommand</code> component whose
 * <code>rendered</code> attribute relies on a request scoped bean property which is retained for the next request
 * through a <code>&lt;h:inputHidden&gt;</code>. However, when Faces needs to decode the <code>UIInput</code> or
 * <code>UICommand</code> component during the postback, the <code>rendered</code> attribute has defaulted back to
 * <code>false</code> because the <code>&lt;h:inputHidden&gt;</code> hasn't yet updated the request scoped bean property
 * yet.
 * <p>
 * This behavior cannot be achieved by using <code>immediate="true"</code> on <code>&lt;h:inputHidden&gt;</code>. It
 * would only move the conversion/validation into the apply request values phase. The model still won't be updated on
 * time.
 *
 * <h2>Usage</h2>
 * <p>
 * You can use it the same way as <code>&lt;h:inputHidden&gt;</code>, you only need to change <code>h:</code> into
 * <code>o:</code> to get the "immediate v2.0" behavior.
 * <pre>
 * &lt;h:form&gt;
 *     &lt;o:inputHidden value="#{bean.hidden}" /&gt;
 *     ...
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * When using ajax, don't forget to make sure that the component is also covered by the <code>execute</code> attribute.
 *
 * @author Bauke Scholtz
 * @since 3.7
 */
@FacesComponent(InputHidden.COMPONENT_TYPE)
public class InputHidden extends HtmlInputHidden {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.input.InputHidden#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.InputHidden";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * This override performs decode, validate and update at once.
	 */
	@Override
	public void decode(FacesContext context) {
		super.decode(context);
		validate(context);

		if (isValid()) {
			updateModel(context);
		}
	}

	/**
	 * This override which does effectively nothing prevents Faces from performing validation for second time.
	 */
	@Override
	public void processValidators(FacesContext context) {
		// NOOP.
	}

	/**
	 * This override which does effectively nothing prevents Faces from performing update for second time.
	 */
	@Override
	public void processUpdates(FacesContext context) {
		// NOOP.
	}

}