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
package org.omnifaces.component.output;

import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:conditionalComment&gt;</code> component renders a conditional comment. Conditional
 * comments are an IE specific feature which enables the developer to (out)comment blocks of HTML depending on whether
 * the client is using IE and if so even which version. They are often seen in combination with CSS stylesheets like so:
 * <pre>
 * &lt;!--[if lte IE 7]&gt;
 *     &lt;link rel="stylesheet" href="ie6-ie7.css" /&gt;
 * &lt;![endif]--&gt;
 * </pre>
 * <p>
 * However, Facelets renders the comment's contents HTML-escaped which makes it unusable.
 * <pre>
 * &lt;!--[if lte IE 7]&amp;gt;
 *     &amp;lt;link rel=&amp;quot;stylesheet&amp;quot; href=&amp;quot;ie6-ie7.css&amp;quot; /&amp;gt;
 * &amp;lt;![endif]--&gt;
 * </pre>
 * <p>
 * Also, if <code>javax.faces.FACELETS_SKIP_COMMENTS</code> context param is
 * set to <code>true</code> then it will even not be rendered at all. You would need to workaround this with an ugly
 * <code>&lt;h:outputText escape="false"&gt;</code>.
 * <pre>
 * &lt;h:outputText
 *     value="&amp;lt;!--[if lte IE 7]&amp;gt;&amp;lt;link rel=&amp;quot;stylesheet&amp;quot; href=&amp;quot;ie6-ie7.css&amp;quot; /&amp;gt;&amp;lt;![endif]--&amp;gt;"
 *     escape="false" /&gt;
 * </pre>
 * <p>This component is designed to solve this problem.
 * <pre>
 * &lt;o:conditionalComment if="lte IE 7"&gt;
 *     &lt;link rel="stylesheet" href="ie6-ie7.css" /&gt;
 * &lt;/o:conditionalComment&gt;
 * </pre>
 * <p>Note that you cannot use this with <code>&lt;h:outputStylesheet&gt;</code> as it would implicitly be relocated as
 * direct child of <code>&lt;h:head&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see OutputFamily
 */
@FacesComponent(ConditionalComment.COMPONENT_TYPE)
public class ConditionalComment extends OutputFamily {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.ConditionalComment";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_MISSING_IF =
		"ConditionalComment attribute 'if' must be specified.";

	private enum PropertyKeys {
		IF;
		@Override public String toString() { return name().toLowerCase(); }
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// UIComponent overrides ------------------------------------------------------------------------------------------

	/**
	 * @throws IllegalArgumentException When <code>if</code> attribute is not specified.
	 */
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		String condition = getIf();

		if (isEmpty(condition)) {
			throw new IllegalArgumentException(ERROR_MISSING_IF);
		}

		ResponseWriter writer = context.getResponseWriter();
		writer.write("<!--[if ");
		writer.write(condition);
		writer.write("]>");
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.write("<![endif]-->");
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the if condition.
	 * @return The if condition.
	 */
	public String getIf() {
		return state.get(PropertyKeys.IF);
	}

	/**
	 * Sets the if condition.
	 * @param condition The if condition.
	 */
	public void setIf(String condition) {
		state.put(PropertyKeys.IF, condition);
	}

}