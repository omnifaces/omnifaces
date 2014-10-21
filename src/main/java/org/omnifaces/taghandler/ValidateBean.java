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

import static javax.faces.event.PhaseId.PROCESS_VALIDATIONS;
import static javax.faces.view.facelets.ComponentHandler.isNew;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.getCurrentForm;
import static org.omnifaces.util.Components.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToViewBeforePhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Platform.getBeanValidator;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.toClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.validation.ConstraintViolation;

import org.omnifaces.eventlistener.BeanValidationEventListener;
import org.omnifaces.util.Callback;

/**
 * <p>
 * The <code>&lt;o:validateBean&gt;</code> allows the developer to control bean validation on a per-{@link UICommand}
 * or {@link UIInput} component basis. The standard <code>&lt;f:validateBean&gt;</code> only allows that on a per-form
 * or a per-request basis (by using multiple tags and conditional EL expressions in its attributes) which may end up in
 * boilerplate code.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
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
 * @see BeanValidationEventListener
 */
public class ValidateBean extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"Parent component of o:validateBean must be an instance of UICommand or UIInput.";
	
	 private static final Class<?>[] CLASS_ARRAY = new Class<?>[0];

	// Variables ------------------------------------------------------------------------------------------------------

	private TagAttribute validationGroups;
	private TagAttribute disabled;
	private TagAttribute value;
	
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
		if (value == null && !(parent instanceof UICommand || parent instanceof UIInput)) {
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);
		}

		if (!isNew(parent)) {
			return;
		}

		final String validationGroups = this.validationGroups != null ? this.validationGroups.getValue(context) : null;
		final boolean disabled = this.disabled != null ? this.disabled.getBoolean(context) : false;
		
		final Object value = this.value.getObject(context);
		
		if (value != null) {
			final List<Class<?>> groups = toClasses(validationGroups);
	
			Callback.Void callback = new Callback.Void() {
	            @Override
	            public void invoke() {
	
					// Check if any form has been submitted at all
					UIForm submittedForm = getCurrentForm();
					if (submittedForm == null) {
						return;
					}
	           
	                // A form has been submitted, get the form we're nested in
	                UIForm targetForm = getTargetForm(parent);
	               
	                // Check if the form that was submitted is the same one as we're nested in
	                if (submittedForm.equals(targetForm)) {
	                    Set<ConstraintViolation<?>> violations = validate(groups);
	                    
	                    if (!violations.isEmpty()) {
		                    FacesContext context = FacesContext.getCurrentInstance();
		                    
		                    for (ConstraintViolation<?> violation : violations) {
		    					context.addMessage(targetForm.getClientId(context), createError(violation.getMessage(), ""));
		    				}
	                    }
	                }
	            }
	        };
	        
	        subscribeToViewBeforePhase(PROCESS_VALIDATIONS, callback);
		} else {
			subscribeToViewBeforePhase(PROCESS_VALIDATIONS, new Callback.Void() {
	
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
	
	private List<Class<?>> toClasses(String validationGroups) {
		final List<Class<?>> groups = new ArrayList<>();
		
		for (String type : csvToList(validationGroups)) {
			groups.add(toClass(type));
		}
		
		return groups;
	}
	
	private UIForm getTargetForm(UIComponent parent) {
		 if (parent instanceof UIForm) {
             return (UIForm) parent;
         } 
		 
         return getClosestParent(parent, UIForm.class);
	}
	
	private Set<ConstraintViolation<?>> validate(List<Class<?>> groups) {
		@SuppressWarnings("rawtypes")
        Set violationsRaw = getBeanValidator().validate(value, groups.toArray(CLASS_ARRAY));

        @SuppressWarnings("unchecked")
        Set<ConstraintViolation<?>> violations = violationsRaw;
        
        return violations;
	}

}