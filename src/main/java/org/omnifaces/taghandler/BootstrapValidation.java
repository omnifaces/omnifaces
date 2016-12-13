/*
 * Copyright 2016 OmniFaces.
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

import static org.omnifaces.el.functions.Converters.joinCollection;
import static org.omnifaces.util.Components.stripIterationIndexFromClientId;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesWrapper;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.PreRenderComponentEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * The <code>&lt;o:bootstrapValidation&gt;</code> allows the developer to show
 * feedback in Bootstrap like-style.
 * 
 * @author Marcelo Barros
 * @since 2.3
 */
public class BootstrapValidation extends TagHandler {

	// Constants
	// ------------------------------------------------------------------------------------------------------

	private static final String ERROR_COMPONENT_NOT_FOUND_IN_VIEW_ROOT = "Component %s not found in ViewRoot";
	private static final String ERROR_PARENT_COMPONENT_NOT_FOUND_IN_VIEW_ROOT = "Parent component not found in ViewRoot";
	private static final String ERROR_INVALID_PARENT = "Parent component of o:bootstrapValidation must be an instance of UIInput.";

	private static final String STYLE_CLASS = "styleClass";										// Default name of the attribute to put the class.
	private static final String DEFAULT_PARENT_ERROR_STYLE_CLASS = "has-danger";				// Default container error class
	private static final String DEFAULT_CONTROL_ERROR_STYLE_CLASS = "form-control-danger";		// Default control error class
	private static final String DEFAULT_PARENT_SUCCESS_STYLE_CLASS = "has-success";				// Default container success class
	private static final String DEFAULT_CONTROL_SUCCESS_STYLE_CLASS = "form-control-success";	// Default control success class 

	// Variables
	// ------------------------------------------------------------------------------------------------------

	private final String PARENT_ERROR_STYLE_CLASS;		// Container error class
	private final String CONTROL_ERROR_STYLE_CLASS;		// Control error class
	private final String PARENT_SUCCESS_STYLE_CLASS;	// Container success class
	private final String CONTROL_SUCCESS_STYLE_CLASS;	// Control success class

	final private String componentStyleClassName;		// Name of the attribute to put the class.
	final private String parentStyleClassName;			// Name of the attribute to put the class.

	final private boolean showSuccess;					// If will render success class
	final private boolean showIcons;					// If will render icon class

	final private ValidateType validate;				// Define witch controls have to be processed
	enum ValidateType {
		all,											// All controls have to be processed
		submited										// Only submited controls have to be processed
	}

	final private RevalidateType revalidate;			// Define whether controls need to be revalidated and what will be revalidated
	enum RevalidateType {
		all,											// Revalidate all
		validator,										// Revalidate only the validators
		required,										// Revalidate only the required
		none											// Not revalidate
	}

	final private String componentClientId;				// The full clientId of the component to be processed
	final private String parentClientId;				// The clientId relative to the component to be processed

	// Constructors
	// ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * 
	 * @param config
	 *            The tag config.
	 */
	public BootstrapValidation(TagConfig config) {
		super(config);

		PARENT_ERROR_STYLE_CLASS = getAttribute("parentErrorStyleClass", DEFAULT_PARENT_ERROR_STYLE_CLASS);
		CONTROL_ERROR_STYLE_CLASS = getAttribute("controlErrorStyleClass", DEFAULT_CONTROL_ERROR_STYLE_CLASS);
		PARENT_SUCCESS_STYLE_CLASS = getAttribute("parentSuccessStyleClass", DEFAULT_PARENT_SUCCESS_STYLE_CLASS);
		CONTROL_SUCCESS_STYLE_CLASS = getAttribute("controlSuccessStyleClass", DEFAULT_CONTROL_SUCCESS_STYLE_CLASS);

		componentStyleClassName = getAttribute("componentStyleClassName", STYLE_CLASS);
		parentStyleClassName = getAttribute("parentStyleClassName", STYLE_CLASS);

		showSuccess = Boolean.parseBoolean(getAttribute("showSuccess", Boolean.toString(false)));
		showIcons = Boolean.parseBoolean(getAttribute("showIcons", Boolean.toString(true)));

		componentClientId = getAttribute("componentClientId", null);
		parentClientId = getAttribute("parentClientId", null);

		validate = Enum.valueOf(ValidateType.class, getAttribute("validate", ValidateType.submited.name()));
		revalidate = Enum.valueOf(RevalidateType.class, getAttribute("revalidate", RevalidateType.none.name()));
	}

	/**
	 * Return the {@link String} value of the attribute. If the attribute is not
	 * set (is <code>null</code>) return the default value.
	 * 
	 * @param localName
	 *            The attribute's name
	 * @param defaultValue
	 *            The default value of the the attribute, in case of the
	 *            attribute is not set (is <code>null</code>).
	 * @return The attribute value.
	 */
	private String getAttribute(final String localName, final String defaultValue) {
		TagAttribute attribute = getAttribute(localName);
		return attribute != null ? attribute.getValue() : defaultValue;
	}

	/**
	 * The parent component (or that indicated in componentClientId attribute)
	 * has to be an instance of {@link UIInput}. It will register a
	 * {@link PreRenderComponentEvent} for the container of the parent component
	 * (or that indicated in parentClientId attribute). If the attribute
	 * validate indicate that only submitted have to be processed, then register
	 * a {@link PreValidateEvent} to store the id submitted.
	 */
	@Override
	public void apply(final FaceletContext faceletContext, UIComponent originalComponent) throws IOException {

		final FacesContext facesContext = faceletContext.getFacesContext();

		UIComponent component = originalComponent;

		if (componentClientId != null) {

			component = component.findComponent(componentClientId);

			if (component == null)
				component = facesContext.getViewRoot().findComponent(componentClientId);
		}

		if (component == null)
			throw new FaceletException(String.format(ERROR_COMPONENT_NOT_FOUND_IN_VIEW_ROOT, componentClientId));

		if (!(component instanceof UIInput))
			throw new IllegalArgumentException(ERROR_INVALID_PARENT);

		if (component instanceof UIInput) {

			final boolean isNewComponent = ComponentHandler.isNew(component);

			BootstrapValidationListener bootstrapValidationListener = null;

			if (ValidateType.submited.equals(validate)) {
				if ((bootstrapValidationListener = isListenerFor(component, PreValidateEvent.class)) == null) {
					bootstrapValidationListener = getNewBootstrapValidationListener();
					component.subscribeToEvent(PreValidateEvent.class, bootstrapValidationListener);
				}
			}

			if (!isNewComponent) {

				final UIComponent parent = parentClientId == null ? component.getParent()
						: originalComponent.findComponent(parentClientId);

				if (parent == null)
					throw new FaceletException(ERROR_PARENT_COMPONENT_NOT_FOUND_IN_VIEW_ROOT);

				if (bootstrapValidationListener == null) {
					bootstrapValidationListener = getNewBootstrapValidationListener();
				}

				if (bootstrapValidationListener.clientId == null) {
					bootstrapValidationListener.clientId = stripIterationIndexFromClientId(component.getClientId());
				}

				if (isListenerFor(parent, PreRenderComponentEvent.class) == null) {
					parent.subscribeToEvent(PreRenderComponentEvent.class, bootstrapValidationListener);
				}
			}
		}
	}

	/**
	 * @return A new {@link BootstrapValidationListener} referencing this
	 *         instance of {@link BootstrapValidation}.
	 */
	private BootstrapValidationListener getNewBootstrapValidationListener() {
		return new BootstrapValidationListener(this);
	}

	/**
	 * Check if the component has a registered listener of the listenerClass
	 * type.
	 * 
	 * @param component
	 * @param listenerClass
	 * @return The listener of the type requested or null if it not found.
	 */
	private <T extends ComponentSystemEvent> BootstrapValidationListener isListenerFor(final UIComponent component,
			Class<T> listenerClass) {

		List<SystemEventListener> listenersForEventClass = component.getListenersForEventClass(listenerClass);
		if (listenersForEventClass != null) {

			for (SystemEventListener systemEventListener : listenersForEventClass) {
				if (systemEventListener != null) {

					@SuppressWarnings("unchecked")
					FacesWrapper<ComponentSystemEventListener> facesWrapper = (FacesWrapper<ComponentSystemEventListener>) systemEventListener;

					ComponentSystemEventListener componentSystemEventListener = facesWrapper.getWrapped();
					if (componentSystemEventListener instanceof BootstrapValidationListener)
						return (BootstrapValidationListener) componentSystemEventListener;
				}
			}
		}

		return null;
	}

	/**
	 * Add a styleClass to a component, if not exists.
	 * 
	 * @param component
	 * @param styleClassName
	 *            The string representing the attribute name. Gennerally
	 *            'styleClass'.
	 * @param styleClass
	 *            The class to be added to component.
	 */
	private static void putStyleClassIfNotExists(UIComponent component, String styleClassName, String styleClass) {

		Map<String, Object> attributes = component.getAttributes();
		String styleClassAttribute = (String) attributes.get(styleClassName);

		Set<String> listStyleClass;

		if (styleClassAttribute != null)
			listStyleClass = new LinkedHashSet<>(Arrays.asList(styleClassAttribute.split("\\s")));
		else
			listStyleClass = new LinkedHashSet<>();

		if (!listStyleClass.contains(styleClass))
			listStyleClass.add(styleClass);

		attributes.put(styleClassName, joinCollection(listStyleClass, " "));
	}
	
	/**
	 * Remove a styleClass to a component, if it exists.
	 * 
	 * @param component
	 * @param styleClassName
	 *            The string representing the attribute name. Gennerally
	 *            'styleClass'.
	 * @param styleClass
	 *            The class to be removed for the component.
	 */
	private static void removeStyleClassIfExists(UIComponent component, String styleClassName, String styleClass) {

		Map<String, Object> attributes = component.getAttributes();
		String styleClassAttribute = (String) attributes.get(styleClassName);

		if (styleClassAttribute != null) {
			Set<String> listStyleClass = new LinkedHashSet<>(Arrays.asList(styleClassAttribute.split("\\s")));

			if (listStyleClass.contains(styleClass))
				listStyleClass.remove(styleClass);

			attributes.put(styleClassName, joinCollection(listStyleClass, " "));
		}
	}

	/**
	 * Process the events
	 */
	private static class BootstrapValidationListener implements ComponentSystemEventListener, Serializable {

		private static final long serialVersionUID = 1L;

		private Long lastAccessedTime;
		private BootstrapValidation bootstrapValidation;
		private String clientId;
		private Set<String> listClientId = new LinkedHashSet<>();

		public BootstrapValidationListener(BootstrapValidation bootstrapValidation) {
			this.bootstrapValidation = bootstrapValidation;
		}

		@Override
		public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {

			final FacesContext facesContext = FacesContext.getCurrentInstance();

			Long lastAccessedTime = null;
			if (ValidateType.submited.equals(bootstrapValidation.validate)) {
				final HttpSession httpSession = ((HttpServletRequest) facesContext.getExternalContext().getRequest())
						.getSession(false);
				if (httpSession != null) {
					lastAccessedTime = httpSession.getLastAccessedTime();
				}
			}

			if (event instanceof PreValidateEvent) {
				if (lastAccessedTime != null) {
					if (!lastAccessedTime.equals(this.lastAccessedTime)) {
						this.lastAccessedTime = lastAccessedTime;
						listClientId.clear();
					}
				}

				listClientId.add(event.getComponent().getClientId(facesContext));
			} else if (event instanceof PreRenderComponentEvent) {

				UIComponent parent = event.getComponent();
				UIInput input = null;
				{
					UIComponent childComponent = parent.findComponent(clientId);

					if (childComponent == null)
						childComponent = facesContext.getViewRoot().findComponent(clientId);

					input = (UIInput) childComponent;
				}

				if (input == null)
					throw new FaceletException(String.format(ERROR_COMPONENT_NOT_FOUND_IN_VIEW_ROOT, clientId));
				
				if (bootstrapValidation.showIcons) removeStyleClassIfExists(input, bootstrapValidation.componentStyleClassName, bootstrapValidation.CONTROL_ERROR_STYLE_CLASS);
				removeStyleClassIfExists(parent, bootstrapValidation.parentStyleClassName, bootstrapValidation.PARENT_ERROR_STYLE_CLASS);
				if (bootstrapValidation.showIcons) removeStyleClassIfExists(input, bootstrapValidation.componentStyleClassName, bootstrapValidation.CONTROL_SUCCESS_STYLE_CLASS);
				removeStyleClassIfExists(parent, bootstrapValidation.parentStyleClassName, bootstrapValidation.PARENT_SUCCESS_STYLE_CLASS);
				
				if (ValidateType.all.equals(bootstrapValidation.validate) || (ValidateType.submited.equals(bootstrapValidation.validate) && lastAccessedTime != null &&  lastAccessedTime.equals(this.lastAccessedTime) && listClientId.contains(input.getClientId(facesContext)))) {
					
					boolean isValid = input.isValid();
					
					if (isValid && !RevalidateType.none.equals(bootstrapValidation.revalidate))
						isValid = ((!RevalidateType.all.equals(bootstrapValidation.revalidate) && !RevalidateType.required.equals(bootstrapValidation.revalidate)) || validateRequired(facesContext, input)) && ((!RevalidateType.all.equals(bootstrapValidation.revalidate) && !RevalidateType.validator.equals(bootstrapValidation.revalidate)) || validateValidator(facesContext, input)); 
					
					if (!isValid) {
						if (bootstrapValidation.showIcons) putStyleClassIfNotExists(input, bootstrapValidation.componentStyleClassName, bootstrapValidation.CONTROL_ERROR_STYLE_CLASS);
						putStyleClassIfNotExists(parent, bootstrapValidation.parentStyleClassName, bootstrapValidation.PARENT_ERROR_STYLE_CLASS);
					}
					if (bootstrapValidation.showSuccess && isValid) {
						if (bootstrapValidation.showIcons) putStyleClassIfNotExists(input, bootstrapValidation.componentStyleClassName, bootstrapValidation.CONTROL_SUCCESS_STYLE_CLASS);
						putStyleClassIfNotExists(parent, bootstrapValidation.parentStyleClassName, bootstrapValidation.PARENT_SUCCESS_STYLE_CLASS);
					}
				}
			} else
				throw new UnsupportedOperationException();
		}
	}

	private static boolean validateRequired(FacesContext context, UIInput input) {
		return !input.isRequired() || !UIInput.isEmpty(input.getValue());
	}

	private static boolean validateValidator(FacesContext context, UIInput input) {
		Object value = input.getValue();
		for (Validator validator : input.getValidators()) {
			try {
				validator.validate(context, input, value);
			} catch (ValidatorException e) {
				return false;
			}
		}
		return true;
	}
}