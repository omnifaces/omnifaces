/*
 * Copyright 2014 OmniFaces.
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

import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * This handler sets an attribute of the given name and value on all nested components, if they don't already have an
 * attribute set. On boolean attributes like <code>disabled</code>, <code>readonly</code> and <code>rendered</code>,
 * any literal (static) attribute value will be ignored and overridden. Only if they have already a value expression as
 * attribute value, then it won't be overridden. This is a technical limitation as they don't default to <code>null</code>.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
public class MassAttribute extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Set<String> ILLEGAL_NAMES = unmodifiableSet("id", "binding");
	private static final String ERROR_ILLEGAL_NAME = "The 'name' attribute may not be set to 'id' or 'binding'.";

	// Properties -----------------------------------------------------------------------------------------------------

	private String name;
	private TagAttribute value;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public MassAttribute(TagConfig config) {
		super(config);
		name = getRequiredAttribute("name").getValue();

		if (ILLEGAL_NAMES.contains(name)) {
			throw new IllegalArgumentException(ERROR_ILLEGAL_NAME);
		}

		value = getRequiredAttribute("value");
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		nextHandler.apply(context, parent);
		applyMassAttribute(context, parent.getChildren());
	}

	private void applyMassAttribute(FaceletContext context, List<UIComponent> children) {
		for (UIComponent component : children) {
			if (component.getValueExpression(name) == null) {
				Object literalValue = component.getAttributes().get(name);

				if (literalValue == null || literalValue instanceof Boolean) {
					Class<?> type = (literalValue == null) ? Object.class : Boolean.class;
					component.setValueExpression(name, value.getValueExpression(context, type));
				}
			}

			applyMassAttribute(context, component.getChildren());
		}
	}

}