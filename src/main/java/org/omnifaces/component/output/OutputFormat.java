/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.component.output;

import java.io.IOException;
import java.io.StringWriter;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlOutputFormat;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:outputFormat&gt;</code> is a component that extends the standard <code>&lt;h:outputFormat&gt;</code>
 * with support for capturing the output and exposing it into the request scope by the variable name as specified by the
 * <code>var</code> attribute.
 * <p>
 * You can use it the same way as <code>&lt;h:outputFormat&gt;</code>, you only need to change <code>h:</code> into
 * <code>o:</code> to get the extra support for <code>var</code> attribute. Here's are some usage examples:
 * <pre>
 * &lt;o:outputFormat value="#{i18n['link.title']}" var="_link_title"&gt;
 *     &lt;f:param value="#{bean.foo}" /&gt;
 *     &lt;f:param value="#{bean.bar}" /&gt;
 * &lt;/o:outputFormat&gt;
 * &lt;h:commandLink value="#{i18n['link.value']}" title="#{_link_title}" /&gt;
 * </pre>
 * <pre>
 * &lt;o:outputFormat value="#{bean.number}" var="_percentage"&gt;
 *     &lt;f:convertNumber type="percent" /&gt;
 * &lt;/o:outputFormat&gt;
 * &lt;div title="Percentage: #{_percentage}" /&gt;
 * </pre>
 * <p>
 * Make sure that the <code>var</code> attribute value doesn't conflict with any of existing variable names in the
 * current EL scope, such as managed bean names. It would be a good naming convention to start their names with
 * <code>_</code>.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
@FacesComponent(OutputFormat.COMPONENT_TYPE)
public class OutputFormat extends HtmlOutputFormat {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.OutputFormat";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"A value expression is disallowed on 'var' attribute of OutputFormat.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		var;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * An override which checks if this isn't been invoked on <code>var</code> attribute.
	 * Finally it delegates to the super method.
	 * @throws IllegalArgumentException When this value expression is been set on <code>var</code> attribute.
	 */
	@Override
	public void setValueExpression(String name, ValueExpression binding) {
		if (PropertyKeys.var.toString().equals(name)) {
			throw new IllegalArgumentException(ERROR_EXPRESSION_DISALLOWED);
		}

		super.setValueExpression(name, binding);
	}

	/**
	 * If the <code>var</code> attribute is set, start capturing the output.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (getVar() != null) {
			ResponseWriter originalResponseWriter = context.getResponseWriter();
			StringWriter buffer = new StringWriter();
			context.setResponseWriter(originalResponseWriter.cloneWithWriter(buffer));
			context.getAttributes().put(this + "_writer", originalResponseWriter);
			context.getAttributes().put(this + "_buffer", buffer);
		}

		super.encodeBegin(context);
	}

	/**
	 * If the <code>var</code> attribute is set, stop capturing the output and expose it in request scope by the
	 * <code>var</code> attribute value as variable name.
	 */
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		super.encodeEnd(context);

		if (getVar() != null) {
			ResponseWriter originalResponseWriter = (ResponseWriter) context.getAttributes().remove(this + "_writer");
			StringWriter buffer = (StringWriter) context.getAttributes().remove(this + "_buffer");
			context.setResponseWriter(originalResponseWriter);
			context.getExternalContext().getRequestMap().put(getVar(), buffer.toString());
		}
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the variable name which exposes the captured output into the request scope.
	 * @return The variable name which exposes the captured output into the request scope.
	 */
	public String getVar() {
		return state.get(PropertyKeys.var);
	}

	/**
	 * Sets the variable name which exposes the captured output into the request scope.
	 * @param var The variable name which exposes the captured output into the request scope.
	 */
	public void setVar(String var) {
		state.put(PropertyKeys.var, var);
	}

}