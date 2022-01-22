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
package org.omnifaces.taghandler;

import static java.lang.String.format;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isOneInstanceOf;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

/**
 * <p>
 * The <strong>&lt;o:massAttribute&gt;</strong> sets an attribute of the given name and value on all nested components,
 * if they don't already have an attribute set. On boolean attributes like <code>disabled</code>, <code>readonly</code>
 * and <code>rendered</code>, any literal (static) attribute value will be ignored and overridden. Only if they have
 * already a value expression <code>#{...}</code> as attribute value, then it won't be overridden. This is a technical
 * limitation specifically for boolean attributes as they don't default to <code>null</code>.
 *
 * <h2>Usage</h2>
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
 * To target a specific component (super)class, use the <code>target</code> attribute. The example below skips labels
 * (as that would otherwise fail in the example below because they don't have the <code>valid</code> property):
 * <pre>
 * &lt;o:massAttribute name="styleClass" value="#{component.valid ? '' : 'error'}" target="jakarta.faces.component.UIInput"&gt;
 *     &lt;h:outputLabel for="input1" /&gt;
 *     &lt;h:inputText id="input1" /&gt;
 *     &lt;h:outputLabel for="input2" /&gt;
 *     &lt;h:inputText id="input2" /&gt;
 *     &lt;h:outputLabel for="input3" /&gt;
 *     &lt;h:inputText id="input3" /&gt;
 * &lt;/o:massAttribute&gt;
 * </pre>
 * <p>
 * Since OmniFaces 3.10, the <code>target</code> attribute supports a commaseparated string.
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
	private Class<UIComponent>[] targetClasses;

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
			List<String> classNames = csvToList(target.getValue());
			targetClasses = new Class[classNames.size()];

			for (int i = 0; i < classNames.size(); i++) {
				String className = classNames.get(i);
				Class<?> cls = null;

				try {
					cls = Class.forName(className);
				}
				catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(format(ERROR_UNAVAILABLE_TARGET, className), e);
				}

				if (!UIComponent.class.isAssignableFrom(cls)) {
					throw new IllegalArgumentException(format(ERROR_INVALID_TARGET, cls));
				}

				targetClasses[i] = (Class<UIComponent>) cls;
			}
		}
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		List<UIComponent> oldChildren = new ArrayList<>(parent.getChildren());
		nextHandler.apply(context, parent);

		if (ComponentHandler.isNew(parent)) {
			List<UIComponent> newChildren = new ArrayList<>(parent.getChildren());
			newChildren.removeAll(oldChildren);
			applyMassAttribute(context, newChildren);
		}
	}

	private void applyMassAttribute(FaceletContext context, List<UIComponent> children) {
		for (UIComponent component : children) {
			if ((targetClasses == null || isOneInstanceOf(component.getClass(), targetClasses)) && component.getValueExpression(name) == null) {
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