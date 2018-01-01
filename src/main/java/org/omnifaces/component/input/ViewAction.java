/*
 * Copyright 2018 OmniFaces
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

import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewAction;
import javax.faces.event.FacesEvent;

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
 * <h3>Usage</h3>
 * <p>
 * You can use it the same way as <code>&lt;f:viewAction&gt;</code>, you only need to change <code>f:</code> to
 * <code>o:</code>.
 * <pre>
 * &lt;o:viewAction action="otherpage" if="#{bean.property eq null}" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 2.2
 */
@FacesComponent(ViewAction.COMPONENT_TYPE)
public class ViewAction extends UIViewAction {

	// Public constants -----------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.ViewAction";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Only broadcast the action event when {@link UIViewAction#isRendered()} returns <code>true</code>. The default
	 * implementation will always broadcast. The {@link UIViewAction#isRendered()} is by default only considered during
	 * {@link #decode(javax.faces.context.FacesContext)}.
	 */
	@Override
	public void broadcast(FacesEvent event) {
		if (super.isRendered()) {
			super.broadcast(event);
		}
	}

	/**
	 * Returns <code>true</code> if the <code>immediate="true"</code> attribute is <strong>not</strong> set, otherwise
	 * delegate to super, hereby maintaining the original behavior of <code>immediate="true"</code>.
	 */
	@Override
	public boolean isRendered() {
		return !isImmediate() || super.isRendered();
	}

}