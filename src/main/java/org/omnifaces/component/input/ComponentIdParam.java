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
package org.omnifaces.component.input;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;

import org.omnifaces.component.input.componentidparam.ConditionalWriterListener;

/**
 * <strong>ComponentIdParam</strong> is a component that allows component ids to be provided as request parameters causing only components with
 * matching ids to be rendered.
 * <p>
 * Both simple component ids as well as client ids are supported. Components can be rendered without their parents having to be rendered. As such,
 * e.g. single rows appearing in a table can be rendered without any of the surrounding markup appearing in the response.
 * <p>
 * The intended usage of this component is to allow client-side scripts to request markup for specific components via a GET request (as opposed to
 * AJAX based post-backs).
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
@FacesComponent(ComponentIdParam.COMPONENT_TYPE)
public class ComponentIdParam extends ViewParam {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.ComponentIdParam";

	private enum PropertyKeys {
		componentIdName, clientIdName, renderChildren
	}

	@Override
	public void decode(FacesContext context) {

		List<String> componentIds = getRequestValues(context, PropertyKeys.componentIdName);
		List<String> clientIds = getRequestValues(context, PropertyKeys.clientIdName);
		boolean renderChildren = getBooleanAttribute(PropertyKeys.renderChildren);

		// Installs a PhaseListener on the view root that will replace the response writer before
		// and after rendering with one that only renders when the current component has one of the Ids
		// that we receive from the request here.
		if (!componentIds.isEmpty() || !clientIds.isEmpty()) {
			context.getViewRoot().addPhaseListener(new ConditionalWriterListener(context, componentIds, clientIds, renderChildren));
		}
	}

	@Override
	public void processValidators(FacesContext context) {
		// NOOP. This component doesn't have a model value anyway.
	}

	@Override
	public void processUpdates(FacesContext context) {
		// NOOP. This component doesn't have a model value anyway.
	}

	/**
	 * Gets the list of request values for the request parameter names identified by the value of the given property key.
	 *
	 * @param context
	 *            FacesContext for the request we are processing
	 * @param propertyKey
	 *            property that holds the request parameter name for which values are returned.
	 * @return All values in the request corresponding to the given parameter name
	 */
	private List<String> getRequestValues(FacesContext context, PropertyKeys propertyKey) {
		String componentIdName = (String) getAttributes().get(propertyKey.name());
		if (componentIdName != null) {
			String[] values = context.getExternalContext().getRequestParameterValuesMap().get(componentIdName);
			if (values != null) {
				return Arrays.asList(values);
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Gets the boolean value for the given property key. Defaults to true if no value is defined.
	 *
	 * @param propertyKey
	 * @return false if the boolean attribute is present and not equal to "true", true otherwise.
	 */
	private boolean getBooleanAttribute(PropertyKeys propertyKey) {
		String attribute = (String) getAttributes().get(propertyKey.name());
		if (attribute == null) {
			return true;
		}

		return Boolean.valueOf(attribute);
	}

}
