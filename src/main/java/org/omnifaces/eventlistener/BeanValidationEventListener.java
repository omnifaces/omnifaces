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
package org.omnifaces.eventlistener;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UICommand;
import javax.faces.component.UIInput;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.BeanValidator;
import javax.faces.validator.Validator;

/**
 * Overrides {@link BeanValidator#setValidationGroups(String)} for all components in the current view. This allows to
 * temporarily use different validationGroups or disabling validation if a specific {@link UICommand} or {@link UIInput}
 * has invoked the form submit.
 *
 * @author Adrian Gygax
 * @author Bauke Scholtz
 * @since 1.3
 */
public class BeanValidationEventListener implements SystemEventListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ATTRIBUTE_ORIGINAL_VALIDATION_GROUPS =
		"BeanValidationEventListener.originalValidationGroups";
	private static final Logger LOGGER =
		Logger.getLogger(BeanValidationEventListener.class.getName());
	private static final String LOG_VALIDATION_GROUPS_OVERRIDDEN =
		"Validation groups for component with id '%s' overriden from '%s' to '%s'";

	// Variables ------------------------------------------------------------------------------------------------------

	private String validationGroups;
	private boolean disabled;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct an instance of bean validation event listener based on the given validation groups and disabled state.
	 * @param validationGroups The validation groups.
	 * @param disabled The disabled state.
	 */
	public BeanValidationEventListener(String validationGroups, boolean disabled) {
		this.validationGroups = validationGroups;
		this.disabled = disabled;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Only listens to {@link UIInput} components which have a {@link javax.faces.validator.BeanValidator} assigned.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return source instanceof UIInput && getBeanValidator((EditableValueHolder) source) != null;
	}

	/**
	 * Handle the {@link PreValidateEvent} and {@link PostValidateEvent}.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (event instanceof PreValidateEvent) {
			handlePreValidate((UIInput) ((ComponentSystemEvent) event).getComponent());
		}
		else if (event instanceof PostValidateEvent) {
			handlePostValidate((UIInput) ((ComponentSystemEvent) event).getComponent());
		}
	}

	/**
	 * Replaces the original value of {@link BeanValidator#getValidationGroups()} with the value from the tag attribute.
	 */
	private void handlePreValidate(UIInput component) {
		final BeanValidator beanValidator = getBeanValidator(component);
		final String validationGroups = disabled ? NoValidationGroup.class.getName() : this.validationGroups;
		final String originalValidationGroups = beanValidator.getValidationGroups();

		if (originalValidationGroups != null) {
			component.getAttributes().put(ATTRIBUTE_ORIGINAL_VALIDATION_GROUPS, originalValidationGroups);
		}

		beanValidator.setValidationGroups(validationGroups);

		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer(String.format(LOG_VALIDATION_GROUPS_OVERRIDDEN,
				component.getClientId(), originalValidationGroups, validationGroups));
		}
	}

	/**
	 * Restores the original value of {@link BeanValidator#getValidationGroups()}.
	 */
	private void handlePostValidate(UIInput component) {
		final BeanValidator beanValidator = getBeanValidator(component);
		final String originalValidationGroups = (String) component.getAttributes().remove(ATTRIBUTE_ORIGINAL_VALIDATION_GROUPS);
		beanValidator.setValidationGroups(originalValidationGroups);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Obtain the bean validator instance of the given editable value holder component.
	 */
	private static BeanValidator getBeanValidator(EditableValueHolder component) {
		Validator[] validators = component.getValidators();

		for (Validator validator : validators) {
			if (validator instanceof BeanValidator) {
				return (BeanValidator) validator;
			}
		}

		return null;
	}

	/**
	 * Dummy validation group to disable any validation.
	 */
	private static interface NoValidationGroup {
		//
	}

}