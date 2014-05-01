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
package org.omnifaces.component.messages;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlMessages;
import javax.faces.context.FacesContext;

import org.omnifaces.renderer.MessagesRenderer;
import org.omnifaces.util.Messages;
import org.omnifaces.util.State;

/**
 * The <code>&lt;o:messages&gt;</code> is a component that extends the standard <code>&lt;h:messages&gt;</code> with
 * the following new features:
 * <dl>
 * <dt>Multiple <code>for</code> components</dt>
 * <dd>Possibility to specify multiple client IDs space separated in the <code>for</code> attribute. The example below
 * would only display messages for <code>input1</code> and <code>input3</code>:
 * <pre><code>
 * &lt;h:form&gt;
 *   &lt;o:messages for="input1 input3" /&gt;
 *   &lt;h:inputText id="input1" /&gt;
 *   &lt;h:inputText id="input2" /&gt;
 *   &lt;h:inputText id="input3" /&gt;
 *   &lt;h:inputText id="input4" /&gt;
 * &lt;/h:form&gt;
 * </code></pre>
 * It can even refer non-input components which in turn contains input components. The example below would only display
 * messages for <code>input1</code> and <code>input2</code>:
 * <pre><code>
 * &lt;h:form&gt;
 *   &lt;o:messages for="inputs" /&gt;
 *   &lt;h:panelGroup id="inputs"&gt;
 *     &lt;h:inputText id="input1" /&gt;
 *     &lt;h:inputText id="input2" /&gt;
 *   &lt;/h:panelGroup&gt;
 *   &lt;h:inputText id="input3" /&gt;
 *   &lt;h:inputText id="input4" /&gt;
 * &lt;/h:form&gt;
 * </code></pre>
 * You can even combine them. The example below would only display messages for <code>input1</code>,
 * <code>input2</code> and <code>input4</code>.
 * <pre><code>
 * &lt;h:form&gt;
 *   &lt;o:messages for="inputs input4" /&gt;
 *   &lt;h:panelGroup id="inputs"&gt;
 *     &lt;h:inputText id="input1" /&gt;
 *     &lt;h:inputText id="input2" /&gt;
 *   &lt;/h:panelGroup&gt;
 *   &lt;h:inputText id="input3" /&gt;
 *   &lt;h:inputText id="input4" /&gt;
 * &lt;/h:form&gt;
 * </code></pre>
 * </dd>
 * <dt>Displaying single message</dt>
 * <dd>Show a single custom message whenever the component has received any faces message. This is particularly useful
 * when you want to display a global message in case any of the in <code>for</code> specified components has a faces
 * message. For example:
 * <pre><code>
 * &lt;o:messages for="form" message="There are validation errors. Please fix them." /&gt;
 * &lt;h:form id="form"&gt;
 *   &lt;h:inputText id="input1" /&gt;&lt;h:message for="input1" /&gt;
 *   &lt;h:inputText id="input2" /&gt;&lt;h:message for="input2" /&gt;
 *   &lt;h:inputText id="input3" /&gt;&lt;h:message for="input3" /&gt;
 * &lt;/h:form&gt;
 * </code></pre>
 * </dd>
 * <dt>HTML escaping</dt>
 * <dd>Control HTML escaping by the new <code>escape</code> attribute.
 * <pre><code>
 * &lt;o:messages escape="false" /&gt;
 * </code></pre>
 * Beware of potential XSS attack holes when user-controlled input is redisplayed through messages!
 * </dd>
 * <dt>Iteration markup control</dt>
 * <dd>Control iteration markup fully by the new <code>var</code> attribute which sets the current {@link FacesMessage}
 * in the request scope and disables the default table/list rendering. For example,
 * <pre><code>
 * &lt;dl&gt;
 *   &lt;o:messages var="message"&gt;
 *     &lt;dt&gt;#{message.severity}&lt;/dt&gt;
 *     &lt;dd title="#{message.detail}"&gt;#{message.summary}&lt;/dd&gt;
 *   &lt;/o:messages&gt;
 * &lt;/dl&gt;
 * </code></pre>
 * Note: the iteration is by design completely stateless. It's therefore not recommended to nest form components inside
 * the <code>&lt;o:messages&gt;</code> component. It should be used for pure output only, like as the standard
 * <code>&lt;h:messages&gt;</code>. Plain output links are however no problem. Also note that the <code>message</code>
 * and <code>escape</code> attributes have in this case no effect. With a single message, there's no point of
 * iteration. As to escaping, just use <code>&lt;h:outputText escape="false"&gt;</code> the usual way.
 * </dd>
 * </dl>
 * <p>
 * Design notice: the component class is named <code>OmniMessages</code> instead of <code>Messages</code> to avoid
 * confusion with the {@link Messages} utility class.
 *
 * @author Bauke Scholtz
 * @since 1.5
 */
@FacesComponent(OmniMessages.COMPONENT_TYPE)
public class OmniMessages extends HtmlMessages {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.messages.OmniMessages";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String ERROR_EXPRESSION_DISALLOWED =
		"A value expression is disallowed on 'var' attribute of OmniMessages.";

	private enum PropertyKeys {
		// Cannot be uppercased. They have to exactly match the attribute names.
		var, message, escape;
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link OmniMessages} component whereby the renderer type is set to
	 * {@link MessagesRenderer#RENDERER_TYPE}.
	 */
	public OmniMessages() {
		setRendererType(MessagesRenderer.RENDERER_TYPE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

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
	 * An override which delegates directly to {@link #encodeChildren(FacesContext)}.
	 */
	@Override
	public void encodeAll(FacesContext context) throws IOException {
		encodeChildren(context);
	}

	// Attribute getters/setters --------------------------------------------------------------------------------------

	/**
	 * Returns the name of the request attribute which exposes the current faces message.
	 * @return The name of the request attribute which exposes the current faces message.
	 */
	public String getVar() {
		return state.get(PropertyKeys.var);
	}

	/**
	 * Sets the name of the request attribute which exposes the current faces message.
	 * @param var The name of the request attribute which exposes the current faces message.
	 */
	public void setVar(String var) {
		state.put(PropertyKeys.var, var);
	}

	/**
	 * Returns the single INFO message to be shown instead when this component has any faces message.
	 * @return The single INFO message to be shown instead when this component has any faces message.
	 * @since 1.6
	 */
	public String getMessage() {
		return state.get(PropertyKeys.message);
	}

	/**
	 * Sets the single INFO message to be shown instead when this component has any faces message.
	 * @param message The single INFO message to be shown instead when this component has any faces message.
	 * @since 1.6
	 */
	public void setMessage(String message) {
		state.put(PropertyKeys.message, message);
	}

	/**
	 * Returns whether the message detail and summary should be HTML-escaped. Defaults to <code>true</code>.
	 * @return Whether the message detail and summary should be HTML-escaped.
	 */
	public Boolean isEscape() {
		return state.get(PropertyKeys.escape, true);
	}

	/**
	 * Sets whether the message detail and summary should be HTML-escaped.
	 * @param escape Whether the message detail and summary should be HTML-escaped.
	 */
	public void setEscape(Boolean escape) {
		state.put(PropertyKeys.escape, escape);
	}

}