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
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * The <strong>&lt;o:massAttribute&gt;</strong> sets an attribute of the given name and value on all nested components,
 * if they don't already have an attribute set. On boolean attributes like <code>disabled</code>, <code>readonly</code>
 * and <code>rendered</code>, any literal (static) attribute value will be ignored and overridden. Only if they have
 * already a value expression <code>#{...}</code> as attribute value, then it won't be overridden. This is a technical
 * limitation specifically for boolean attributes as they don't default to <code>null</code>.
 * <p>
 * For example, the following setup
 * <pre>
 * &lt;o:massAttribute name="disabled" value="true"&gt;
 *     &lt;h:inputText id="input1" /&gt;
 *     &lt;h:inputText id="input2" disabled="true" /&gt;
 *     &lt;h:inputText id="input3" disabled="false" /&gt;
 *     &lt;h:inputText id="input4" disabled="#{true}" /&gt;
 *     &lt;h:inputText id="input5" disabled="#{false}" /&gt;
 * &lt;/o:massAttribute&gt;
 * </pre>
 * will set the <code>disabled="true"</code> attribute in <code>input1</code>, <code>input2</code> and
 * <code>input3</code> as those are the only components <strong>without</strong> a value expression on the boolean attribute.
 * <p>
 * As another general example without booleans, the following setup
 * <pre>
 * &lt;o:massAttribute name="styleClass" value="#{component.valid ? '' : 'error'}"&gt;
 *     &lt;h:inputText id="input1" /&gt;
 *     &lt;h:inputText id="input2" styleClass="some" /&gt;
 *     &lt;h:inputText id="input3" styleClass="#{'some'}" /&gt;
 *     &lt;h:inputText id="input4" styleClass="#{null}" /&gt;
 * &lt;/o:massAttribute&gt;
 * </pre>
 * will only set the <code>styleClass="#{component.valid ? '' : 'error'}"</code> attribute in <code>input1</code> as
 * that's the only component on which the attribute is absent.
 * Do note that the specified EL expression will actually be evaluated on a per-component basis.
 * <p>
 * To target a specific component (super)class, use the <code>target</code> attribute. The below example skips labels
 * (as that would in the below example otherwise fail because they don't have a <code>valid</code> property):
 * <pre>
 * &lt;o:massAttribute name="styleClass" value="#{component.valid ? '' : 'error'}" target="javax.faces.component.UIInput"&gt;
 *     &lt;h:outputLabel for="input1" /&gt;
 *     &lt;h:inputText id="input1" /&gt;
 *     &lt;h:outputLabel for="input2" /&gt;
 *     &lt;h:inputText id="input2" /&gt;
 *     &lt;h:outputLabel for="input3" /&gt;
 *     &lt;h:inputText id="input3" /&gt;
 * &lt;/o:massAttribute&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
public class MassAttribute extends TagHandler {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Set<String> ILLEGAL_NAMES = unmodifiableSet("id", "binding");
	private static final String ERROR_ILLEGAL_NAME = "The 'name' attribute may not be set to 'id' or 'binding'.";
	private static final String ERROR_UNAVAILABLE_TARGET = "The 'target' attribute must represent a valid class name."
		+ " Encountered '%s' which cannot be found in the classpath.";
	private static final String ERROR_INVALID_TARGET = "The 'target' attribute must represent an UIComponent class."
		+ " Encountered '%s' which is not an UIComponent class.";

	// Properties -----------------------------------------------------------------------------------------------------

	private String name;
	private TagAttribute value;
	private Class<UIComponent> targetClass;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	@SuppressWarnings("unchecked")
	public MassAttribute(TagConfig config) {
		super(config);
		name = getRequiredAttribute("name").getValue();

		if (ILLEGAL_NAMES.contains(name)) {
			throw new IllegalArgumentException(ERROR_ILLEGAL_NAME);
		}

		value = getRequiredAttribute("value");
		TagAttribute target = getAttribute("target");

		if (target != null) {
			String className = target.getValue();
			Class<?> cls = null;

			try {
				cls = Class.forName(className);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(String.format(ERROR_UNAVAILABLE_TARGET, className));
			}

			if (!UIComponent.class.isAssignableFrom(cls)) {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_TARGET, cls));
			}

			targetClass = (Class<UIComponent>) cls;
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		nextHandler.apply(context, parent);

		if (ComponentHandler.isNew(parent)) {
			applyMassAttribute(context, parent.getChildren());
		}
	}

	private void applyMassAttribute(FaceletContext context, List<UIComponent> children) {
		for (UIComponent component : children) {
			if (targetClass == null || targetClass.isAssignableFrom(component.getClass())) {
				if (component.getValueExpression(name) == null) {
					Object literalValue = component.getAttributes().get(name);

					if (literalValue == null || literalValue instanceof Boolean) {
						Class<?> type = (literalValue == null) ? Object.class : Boolean.class;
						component.setValueExpression(name, value.getValueExpression(context, type));
					}
				}
			}

			applyMassAttribute(context, component.getChildren());
		}
	}

}