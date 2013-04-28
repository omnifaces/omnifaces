/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.validator;

import static javax.validation.Validation.byDefaultProvider;
import static org.omnifaces.util.Components.getCurrentComponent;

import java.util.Locale;

import javax.validation.MessageInterpolator;

import org.omnifaces.util.Components;
import org.omnifaces.util.Faces;


/**
 * Bean Validation message interpolator that is able to insert the label of the component via which
 * validation failed <em>into</em> the validation message. For this the special placeholder <code>{jsf.label}</code>
 * can be used. This {@link MessageInterpolator} will replace every occurrence of that with the label of the
 * <em>current</em> component.
 * <p>
 * Note that Bean Validation is not only called from within JSF, and as such JSF might not be available. If JSF
 * is not available occurrences of <code>{jsf.label}</code> will be replaced by "nothing" (the empty string).
 * The user should take care that messages are compatible with both situations if needed.
 * </p>
 * <p>
 * This message interpolator is <em>NOT</em> needed for putting a component label before or after a bean validation
 * message. That functionality is already provided by JSF itself via the <code>javax.faces.validator.BeanValidator.MESSAGE</code>
 * key in any resource bundle known to JSF.
 * </p>
 * 
 * 
 * <h3>Example</h3>
 * In <code>ValidationMessages.properties</code>
 * <pre>javax.validation.constraints.Size.message = The size of {jsf.label} must be between {min} and {max} characters</pre>
 * 
 * <h3>Installation</h3>
 * Create a <code>META-INF/validation.xml</code> file with the following contents:
 * 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;validation-config
 *	xmlns="http://jboss.org/xml/ns/javax/validation/configuration"
 *	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *	xsi:schemaLocation="http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.0.xsd"&gt;
 *
 *	&lt;message-interpolator&gt;org.omnifaces.validator.JsfLabelMessageInterpolator&lt;/message-interpolator&gt;
 *
 * &lt;/validation-config&gt;
 * </pre>
 * 
 * @since 1.5
 * @author Arjan Tijms
 *
 */
public class JsfLabelMessageInterpolator implements MessageInterpolator {
	
	private final MessageInterpolator wrapped;

	public JsfLabelMessageInterpolator() {
		wrapped = byDefaultProvider().configure().getDefaultMessageInterpolator();
	}
	
	@Override
	public String interpolate(String messageTemplate, Context context) {
		return wrapped.interpolate(messageTemplate, context);
	}
	
	@Override
	public String interpolate(String messageTemplate, Context context, Locale locale) {
		String message = wrapped.interpolate(messageTemplate, context, locale);
		
		if (message.contains("{jsf.label}")) {
			
			String label = "";
			if (Faces.getContext() != null) {
				label = Components.getLabel(getCurrentComponent());
			}
			
			message = message.replace("{jsf.label}", label);
		}
		
		return message;
	}
	
}