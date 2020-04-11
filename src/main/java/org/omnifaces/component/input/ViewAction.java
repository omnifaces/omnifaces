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

import static org.omnifaces.util.Messages.addFlashGlobalWarn;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIViewAction;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.ExternalContextWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.FacesEvent;

import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:viewAction&gt;</code> is a component that extends the standard <code>&lt;f:viewAction&gt;</code> and
 * changes the <code>if</code> attribute to be evaluated during <code>INVOKE_APPLICATION</code> phase instead of the
 * <code>APPLY_REQUEST_VALUES</code> phase. This allows developers to let the <code>if</code> attribute check the
 * converted and validated model values before performing the view action, which results in much more intuitive behavior.
 * <p>
 * In below example, the <code>FooConverter</code> may convert a non-null parameter to <code>null</code> without causing
 * a validation or conversion error, and the intent is to redirect the current page to <code>otherpage.xhtml</code> when
 * the converted result is <code>null</code>.
 * <pre>
 * &lt;f:viewParam name="foo" value="#{bean.foo}" converter="fooConverter" /&gt;
 * &lt;f:viewAction action="otherpage" if="#{bean.foo eq null}" /&gt;
 * </pre>
 * <p>
 * This is however not possible with standard <code>&lt;f:viewAction&gt;</code> as it evaluates the <code>if</code>
 * attribute already before the conversion has taken place. This component solves that by postponing the evaluation of
 * the <code>if</code> attribute to the <code>INVOKE_APPLICATION</code> phase.
 * <pre>
 * &lt;f:viewParam name="foo" value="#{bean.foo}" converter="fooConverter" /&gt;
 * &lt;o:viewAction action="otherpage" if="#{bean.foo eq null}" /&gt;
 * </pre>
 * <p>
 * Only when you set <code>immediate="true"</code>, then it will behave the same as the standard
 * <code>&lt;f:viewAction&gt;</code>.
 *
 * <h2>Usage</h2>
 * <p>
 * You can use it the same way as <code>&lt;f:viewAction&gt;</code>, you only need to change <code>f:</code> to
 * <code>o:</code>.
 * <pre>
 * &lt;o:viewAction action="otherpage" if="#{bean.property eq null}" /&gt;
 * </pre>
 *
 * <h2>Messaging</h2>
 * <p>
 * You can use the <code>message</code> attribute to add a global flash warning message.
 * <pre>
 * &lt;o:viewAction ... message="Please use a valid link from within the site" /&gt;
 * </pre>
 * <p>
 * Note that the message will only be shown when the redirect has actually taken place. The support was added in
 * OmniFaces 3.2.
 *
 * @author Bauke Scholtz
 * @since 2.2
 */
@FacesComponent(ViewAction.COMPONENT_TYPE)
public class ViewAction extends UIViewAction {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.ViewAction";

	enum PropertyKeys {
		message
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Only broadcast the action event when {@link UIViewAction#isRendered()} returns <code>true</code>. The default
	 * implementation will always broadcast. The {@link UIViewAction#isRendered()} is by default only considered during
	 * {@link #decode(jakarta.faces.context.FacesContext)}.
	 * <p>
	 * If the action event performs any redirect, then add any {@link #getMessage()} as a global flash warning message.
	 */
	@Override
	public void broadcast(FacesEvent event) {
		if (super.isRendered()) {
			String message = getMessage();
			super.broadcast(isEmpty(message) ? event : new RedirectMessageEvent(event, message));
		}
	}

	private static class RedirectMessageEvent extends ActionEvent {

		private static final long serialVersionUID = 1L;

		private FacesEvent wrapped;
		private String message;

		public RedirectMessageEvent(FacesEvent wrapped, String message) {
			super(wrapped.getComponent());
			this.wrapped = wrapped;
			this.message = message;
		}

		@Override
		public FacesContext getFacesContext() {
			return new RedirectMessageFacesContext(wrapped.getFacesContext(), message);
		}
	}

	private static class RedirectMessageFacesContext extends FacesContextWrapper {

		private String message;

		public RedirectMessageFacesContext(FacesContext wrapped, String message) {
			super(wrapped);
			this.message = message;
		}

		@Override
		public ExternalContext getExternalContext() {
			return new RedirectMessageExternalContext(getWrapped().getExternalContext(), message);
		}
	}

	private static class RedirectMessageExternalContext extends ExternalContextWrapper {

		private String message;

		public RedirectMessageExternalContext(ExternalContext wrapped, String message) {
			super(wrapped);
			this.message = message;
		}

		@Override
		public void redirect(String url) throws IOException {
			addFlashGlobalWarn(message);
			super.redirect(url);
		}
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if the <code>immediate="true"</code> attribute is <strong>not</strong> set, otherwise
	 * delegate to super, hereby maintaining the original behavior of <code>immediate="true"</code>.
	 */
	@Override
	public boolean isRendered() {
		return !isImmediate() || super.isRendered();
	}

	/**
	 * Returns the global flash warning message to be shown in the redirected page.
	 * @return The global flash warning message to be shown in the redirected page.
	 * @since 3.2
	 */
	public String getMessage() {
		return state.get(PropertyKeys.message);
	}

	/**
	 * Sets the global flash warning message to be shown in the redirected page.
	 * @param message The global flash warning message to be shown in the redirected page.
	 * @since 3.2
	 */
	public void setMessage(String message) {
		state.put(PropertyKeys.message, message);
	}

}