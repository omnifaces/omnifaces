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
package org.omnifaces.component.input;

import static jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.omnifaces.component.input.Form.PropertyKeys.includeRequestParams;
import static org.omnifaces.component.input.Form.PropertyKeys.partialSubmit;
import static org.omnifaces.component.input.Form.PropertyKeys.useRequestURI;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.Components.getParams;
import static org.omnifaces.util.FacesLocal.getRequestAttribute;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getRequestURI;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.formatURLWithQueryString;

import java.io.IOException;

import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationWrapper;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.application.ViewHandlerWrapper;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.html.HtmlForm;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.FacesContextWrapper;

import org.omnifaces.taghandler.IgnoreValidationFailed;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:form&gt;</code> is a component that extends the standard <code>&lt;h:form&gt;</code> and submits to
 * exactly the request URI with query string as seen in browser's address. Standard JSF <code>&lt;h:form&gt;</code>
 * submits to the view ID and does not include any query string parameters or path parameters and may therefore fail
 * in cases when the form is submitted to a request scoped bean which relies on the same initial query string parameters
 * or path parameters still being present in the request URI. This is particularly useful if you're using FacesViews or
 * forwarding everything to 1 page.
 * <p>
 * Additionally, it offers in combination with the <code>&lt;o:ignoreValidationFailed&gt;</code> tag on an
 * {@link UICommand} component the possibility to ignore validation failures so that the invoke action phase will be
 * executed anyway.
 * <p>
 * Since version 2.1 this component also supports adding query string parameters to the action URL via nested
 * <code>&lt;f:param&gt;</code> and <code>&lt;o:param&gt;</code>.
 * <p>
 * Since version 3.0, it will also during ajax requests automatically send only the form data which actually need to
 * be processed as opposed to the entire form, based on the <code>execute</code> attribute of any nested
 * <code>&lt;f:ajax&gt;</code>. This feature is similar to <code>partialSubmit</code> feature of PrimeFaces.
 * This will reduce the request payload when used in large forms such as editable tables.
 * <p>
 * You can use it the same way as <code>&lt;h:form&gt;</code>, you only need to change <code>h:</code> to
 * <code>o:</code>.
 *
 * <h2>Use request URI</h2>
 * <p>
 * This was available since version 1.6, but since version 3.0, this has become enabled by default. So just using
 * <code>&lt;o:form&gt;</code> will already submit to the exact request URI with query string as seen in browser's
 * address bar. In order to turn off this behavior, set <code>useRequestURI</code> attribute to <code>false</code>.
 * <pre>
 * &lt;o:form useRequestURI="false"&gt;
 * </pre>
 *
 * <h2>Include request params</h2>
 * <p>
 * When you want to include request parameters only instead of the entire request URI with query string, set the
 * <code>includeRequestParams</code> attribute to <code>true</code>. This will implicitly set <code>useRequestURI</code>
 * attribute to <code>false</code>.
 * <pre>
 * &lt;o:form includeRequestParams="true"&gt;
 * </pre>
 *
 * <h2>Partial submit</h2>
 * <p>
 * This is the default behavior. So just using <code>&lt;o:form&gt;</code> will already cause the
 * <code>&lt;f:ajax&gt;</code> to send only the form data which actually need to be processed. In order to turn off this
 * behavior, set <code>partialSubmit</code> attribute to <code>false</code>.
 * <pre>
 * &lt;o:form partialSubmit="false"&gt;
 * </pre>
 *
 * <h2>Add query string parameters to action URL</h2>
 * <p>
 * The standard {@link UIForm} doesn't support adding query string parameters to the action URL. This component offers
 * this possibility via nested <code>&lt;f:param&gt;</code> and <code>&lt;o:param&gt;</code>.
 * <pre>
 * &lt;o:form&gt;
 *     &lt;f:param name="somename" value="somevalue" /&gt;
 *     ...
 * &lt;/o:form&gt;
 * </pre>
 * <p>
 * The <code>&lt;f|o:param&gt;</code> will override any included view or request parameters on the same name. To conditionally add
 * or override, use the <code>disabled</code> attribute of <code>&lt;f|o:param&gt;</code>.
 * <p>
 * The support was added in OmniFaces 2.2.
 *
 * <h2>Ignore Validation Failed</h2>
 * <p>
 * In order to properly use the <code>&lt;o:ignoreValidationFailed&gt;</code> tag on an {@link UICommand} component, its
 * parent <code>&lt;h:form&gt;</code> component has to be replaced by this <code>&lt;o:form&gt;</code> component.
 * See also {@link IgnoreValidationFailed}.
 *
 *
 * @since 1.1
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
@FacesComponent(Form.COMPONENT_TYPE)
@ResourceDependency(library=OMNIFACES_LIBRARY_NAME, name=OMNIFACES_SCRIPT_NAME, target="head")
public class Form extends HtmlForm {

	// Constants ------------------------------------------------------------------------------------------------------

	/** The component type, which is {@value org.omnifaces.component.input.Form#COMPONENT_TYPE}. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.Form";

	enum PropertyKeys {

		useRequestURI,
		includeRequestParams,
		partialSubmit,
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());
	private boolean ignoreValidationFailed;

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void processValidators(FacesContext context) {
		if (isIgnoreValidationFailed()) {
			super.processValidators(new IgnoreValidationFailedFacesContext(context));
		}
		else {
			super.processValidators(context);
		}
	}

	@Override
	public void processUpdates(FacesContext context) {
		if (isIgnoreValidationFailed()) {
			super.processUpdates(new IgnoreValidationFailedFacesContext(context));
		}
		else {
			super.processUpdates(context);
		}
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (isPartialSubmit()) {
			getPassThroughAttributes().put("data-partialsubmit", "true");
		}

		super.encodeBegin(new ActionURLDecorator(context, this));
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns whether or not the request parameters should be encoded into the form's action URL.
	 * @return Whether or not the request parameters should be encoded into the form's action URL.
	 * @since 1.5
	 */
	public boolean isIncludeRequestParams() {
		return state.get(includeRequestParams, FALSE);
	}

	/**
	 * Set whether or not the request parameters should be encoded into the form's action URL.
	 * @param includeRequestParams Whether or not the request parameters should be encoded into the form's action URL.
	 * @since 1.5
	 */
	public void setIncludeRequestParams(boolean includeRequestParams) {
		state.put(PropertyKeys.includeRequestParams, includeRequestParams);
	}

	/**
	 * Returns whether the request URI should be used as form's action URL. Defaults to <code>true</code>.
	 * This setting is ignored when <code>includeRequestParams</code> is set to <code>true</code>.
	 * @return Whether the request URI should be used as form's action URL.
	 * @since 1.6
	 */
	public boolean isUseRequestURI() {
		return state.get(useRequestURI, TRUE);
	}

	/**
	 * Set whether the request URI should be used as form's action URL.
	 * This setting is ignored when <code>includeRequestParams</code> is set to <code>true</code>.
	 * @param useRequestURI Whether the request URI should be used as form's action URL.
	 * @since 1.6
	 */
	public void setUseRequestURI(boolean useRequestURI) {
		state.put(PropertyKeys.useRequestURI, useRequestURI);
	}

	/**
	 * Returns whether or not the form should ignore validation fail (and thus proceed to update model/invoke action).
	 * @return Whether or not the form should ignore validation fail.
	 * @since 2.1
	 */
	public boolean isIgnoreValidationFailed() {
		return ignoreValidationFailed;
	}

	/**
	 * Set whether the form should ignore validation fail.
	 * @param ignoreValidationFailed Whether the form should ignore validation fail.
	 * @since 2.1
	 */
	public void setIgnoreValidationFailed(boolean ignoreValidationFailed) {
		this.ignoreValidationFailed = ignoreValidationFailed;
	}

	/**
	 * Returns whether to send only the form data which actually need to be processed as opposed to the entire form. Defaults to <code>true</code>.
	 * @return Whether to send only the form data which actually need to be processed as opposed to the entire form.
	 * @since 3.0
	 */
	public boolean isPartialSubmit() {
		return state.get(partialSubmit, TRUE);
	}

	/**
	 * Set whether to send only the form data which actually need to be processed as opposed to the entire form.
	 * @param partialSubmit Whether to send only the form data which actually need to be processed as opposed to the entire form.
	 * @since 3.0
	 */
	public void setPartialSubmit(boolean partialSubmit) {
		state.put(PropertyKeys.partialSubmit, partialSubmit);
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * FacesContext wrapper which performs NOOP during {@link FacesContext#validationFailed()} and
	 * {@link FacesContext#renderResponse()}.
	 *
	 * @author Bauke Scholtz
	 */
	private static class IgnoreValidationFailedFacesContext extends FacesContextWrapper {

		public IgnoreValidationFailedFacesContext(FacesContext wrapped) {
			super(wrapped);
		}

		@Override
		public void validationFailed() {
			// NOOP.
		}

		@Override
		public void renderResponse() {
			// NOOP.
		}
	}

	/**
	 * Helper class used for creating a FacesContext with a decorated FacesContext -&gt; Application -&gt; ViewHandler
	 * -&gt; getActionURL.
	 *
	 * @author Arjan Tijms
	 */
	private static class ActionURLDecorator extends FacesContextWrapper {

		private Form form;

		public ActionURLDecorator(FacesContext wrapped, Form form) {
			super(wrapped);
			this.form = form;
		}

		@Override
		public Application getApplication() {
			return new ActionURLDecoratorApplication(getWrapped().getApplication(), form);
		}
	}

	private static class ActionURLDecoratorApplication extends ApplicationWrapper {

		private Form form;

		public ActionURLDecoratorApplication(Application wrapped, Form form) {
			super(wrapped);
			this.form = form;
		}

		@Override
		public ViewHandler getViewHandler() {
			return new ActionURLDecoratorViewHandler(getWrapped().getViewHandler(), form);
		}
	}

	private static class ActionURLDecoratorViewHandler extends ViewHandlerWrapper {

		private Form form;

		public ActionURLDecoratorViewHandler(ViewHandler wrapped, Form form) {
			super(wrapped);
			this.form = form;
		}

		/**
		 * The actual method we're decorating in order to either include the view parameters into the
		 * action URL, or include the request parameters into the action URL, or use request URI as
		 * action URL. Any <code>&lt;f|o:param&gt;</code> nested in the form component will be included
		 * in the query string, overriding any existing view or request parameters on same name.
		 */
		@Override
		public String getActionURL(FacesContext context, String viewId) {
			String actionURL = form.isUseRequestURI() && !form.isIncludeRequestParams() ? getActionURL(context) : getWrapped().getActionURL(context, viewId);
			String queryString = toQueryString(getParams(form, form.isUseRequestURI() || form.isIncludeRequestParams(), false));
			return formatURLWithQueryString(actionURL, queryString);
		}

		private String getActionURL(FacesContext context) {
			String actionURL = (getRequestAttribute(context, ERROR_REQUEST_URI) != null) ? getRequestContextPath(context) : getRequestURI(context);
			return actionURL.isEmpty() ? "/" : actionURL;
		}
	}

}
