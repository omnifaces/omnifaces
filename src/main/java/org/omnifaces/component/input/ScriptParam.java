/*
 * Copyright 2020 OmniFaces
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

import static java.lang.String.format;
import static org.omnifaces.el.ExpressionInspector.getValueReference;
import static org.omnifaces.util.FacesLocal.getRequestParameter;
import static org.omnifaces.util.FacesLocal.getScriptParameters;
import static org.omnifaces.util.Reflection.invokeMethods;

import java.util.HashSet;
import java.util.Set;

import jakarta.el.ValueExpression;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;

import org.omnifaces.cdi.PostScriptParam;
import org.omnifaces.util.Faces;

/**
 * <p>
 * The <code>&lt;o:scriptParam&gt;</code> is a component that extends the standard <code>&lt;f:viewParam&gt;</code>
 * with support for setting results of client-side evaluated JavaScript code in bean.
 *
 * <h2>Usage</h2>
 * <p>
 * It's similar to the <code>&lt;f:viewParam&gt;</code>.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:scriptParam script="new Date().getTimezoneOffset()" value="#{bean.clientTimeZoneOffset}" /&gt;
 *     &lt;o:scriptParam script="window.screen.width" value="#{bean.clientScreenWidth}" /&gt;
 *     &lt;o:scriptParam script="someFunctionName()" value="#{bean.resultOfSomeFunctionName}" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * <p>
 * You can use the <code>render</code> attribute to declare which components should be updated when a script parameter
 * has been set.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:scriptParam script="foo()" value="#{bean.resultOfFoo}" render="fooResult" /&gt;
 * &lt;/f:metadata&gt;
 * ...
 * &lt;h:body&gt;
 *     ...
 *     &lt;h:panelGroup id="fooResult"&gt;
 *         &lt;ui:fragment rendered="#{not empty bean.resultOfFoo}"&gt;
 *             The result of foo() script is: #{bean.resultOfFoo}
 *         &lt;/ui:fragment&gt;
 *     &lt;/h:panelGroup&gt;
 *     ...
 * &lt;/h:body&gt;
 * </pre>
 * <p>
 * Note that as it extends from the standard <code>&lt;f:viewParam&gt;</code>, its built-in conversion and validation
 * functionality is also supported on this component. So, the following is also possible:
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:scriptParam script="window.navigator" value="#{bean.clientNavigator}" /&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 * With a <code>clientNavigator</code> being an instance of <code>jakarta.json.JsonObject</code>:
 * <pre>
 * private JsonObject clientNavigator;
 * </pre>
 * And this converter:
 * <pre>
 * package com.example;
 *
 * import java.io.StringReader;
 * import jakarta.faces.component.UIComponent;
 * import jakarta.faces.context.FacesContext;
 * import jakarta.faces.convert.Converter;
 * import jakarta.faces.convert.ConverterException;
 * import jakarta.faces.convert.FacesConverter;
 * import jakarta.json.Json;
 * import jakarta.json.JsonObject;
 *
 * &#64;FacesConverter(forClass = JsonObject.class)
 * public class JsobObjectConverter implements Converter&lt;JsonObject&gt; {
 *
 *     &#64;Override
 *     public String getAsString(FacesContext context, UIComponent component, JsonObject modelValue) {
 *         if (modelValue == null) {
 *             return "";
 *         }
 *
 *         return modelValue.toString();
 *     }
 *
 *     &#64;Override
 *     public JsonObject getAsObject(FacesContext context, UIComponent component, String submittedValue) {
 *         if (submittedValue == null || submittedValue.isEmpty()) {
 *             return null;
 *         }
 *
 *         try {
 *             return Json.createReader(new StringReader(submittedValue)).readObject();
 *         }
 *         catch (Exception e) {
 *             throw new ConverterException("Not a valid JSON object", e);
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Events</h2>
 * <p>
 * When the script params have been set, then any method with the {@link PostScriptParam} annotation will be fired:
 * <pre>
 * &#64;PostScriptParam
 * public void initScriptParams() {
 *     // ...
 * }
 * </pre>
 * <p>
 * This is useful in case you want to preload the model for whatever is rendered by
 * <code>&lt;o:scriptParam render&gt;</code>.
 *
 * @author Bauke Scholtz
 * @since 3.6
 * @see OnloadParam
 * @see PostScriptParam
 * @see Faces#getScriptParameters()
 */
@FacesComponent(ScriptParam.COMPONENT_TYPE)
public class ScriptParam extends OnloadParam {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.input.ScriptParam#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.ScriptParam";

	/** The omnifaces event value, which is {@value org.omnifaces.component.input.ScriptParam#EVENT_VALUE}. */
	public static final String EVENT_VALUE = "setScriptParamValues";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String SCRIPT_INIT = "OmniFaces.ScriptParam.run('%s', %s)";

	private enum PropertyKeys {
		SCRIPT;
		@Override public String toString() { return name().toLowerCase(); }
	}

	// Init -----------------------------------------------------------------------------------------------------------

	@Override
	protected String getInitScript(FacesContext context) {
		StringBuilder scripts = new StringBuilder("{");

		for (ScriptParam scriptParam : getScriptParameters(context)) {
			scripts.append("'").append(scriptParam.getClientId()).append("':").append(scriptParam.getScript()).append(',');
		}

		scripts.append("}");

		return format(SCRIPT_INIT, getClientId(), scripts);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	protected String getEventValue(FacesContext context) {
		return EVENT_VALUE;
	}

	@Override
	protected void decodeAll(FacesContext context) {
		Set<Object> beans = new HashSet<>();

		for (ScriptParam scriptParam : getScriptParameters(context)) {
			String value = getRequestParameter(context, scriptParam.getClientId());
			scriptParam.decodeImmediately(context, value);
			ValueExpression valueExpression = scriptParam.getValueExpression("value");

			if (valueExpression != null) {
				beans.add(getValueReference(context.getELContext(), valueExpression).getBase());
			}
		}

		for (Object bean : beans) {
			invokeMethods(bean, PostScriptParam.class);
		}
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the script to be evaluated.
	 * @return The script to be evaluated.
	 */
	public String getScript() {
		return state.get(PropertyKeys.SCRIPT);
	}

	/**
	 * Sets the script to be evaluated.
	 * @param script The script to be evaluated.
	 */
	public void setScript(String script) {
		state.put(PropertyKeys.SCRIPT, script);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if the current request is triggered by a script param request.
	 * I.e. if it is initiated by <code>OmniFaces.ScriptParam.setScriptParamValues()</code> script which runs on page load.
	 * @param context The involved faces context.
	 * @return <code>true</code> if the current request is triggered by a script param request.
	 */
	public static boolean isScriptParamRequest(FacesContext context) {
		return isOnloadParamRequest(context, EVENT_VALUE);
	}

}
