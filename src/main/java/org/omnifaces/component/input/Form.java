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
package org.omnifaces.component.input;

import static java.lang.Boolean.FALSE;
import static org.omnifaces.component.input.Form.PropertyKeys.includeRequestParams;
import static org.omnifaces.component.input.Form.PropertyKeys.includeViewParams;
import static org.omnifaces.component.input.Form.PropertyKeys.useRequestURI;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getRequestURI;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;

import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.FacesComponent;
import javax.faces.component.UICommand;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewParameter;
import javax.faces.component.html.HtmlForm;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;

import org.omnifaces.taghandler.IgnoreValidationFailed;
import org.omnifaces.util.Components;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:form&gt;</code> is a component that extends the standard <code>&lt;h:form&gt;</code> and provides a
 * way to keep view or request parameters in the request URL after a post-back and offers in combination with the
 * <code>&lt;o:ignoreValidationFailed&gt;</code> tag on an {@link UICommand} component the possibility to ignore
 * validation failures so that the invoke action phase will be executed anyway. This component also supports adding
 * query string parameters to the action URL via nested <code>&lt;f:param&gt;</code> and <code>&lt;o:param&gt;</code>.
 * <p>
 * You can use it the same way as <code>&lt;h:form&gt;</code>, you only need to change <code>h:</code> to
 * <code>o:</code>.
 *
 * <h3>Include View Params</h3>
 * <p>
 * The standard {@link UIForm} doesn't put the original view parameters in the action URL that's used for the post-back.
 * Instead, it relies on those view parameters to be stored in the state associated with the standard
 * {@link UIViewParameter}. Via this state those parameters are invisibly re-applied after every post-back.
 * <p>
 * The disadvantage of this invisible retention of view parameters is that the user doesn't see them anymore in the
 * address bar of the browser that is used to interact with the faces application. Copy-pasting the URL from the address
 * bar or refreshing the page by hitting enter inside the address bar will therefore not always yield the expected
 * results.
 * <p>
 * To solve this, this component offers an attribute <code>includeViewParams="true"</code> that will optionally include
 * all view parameters, in exactly the same way that this can be done for <code>&lt;h:link&gt;</code> and
 * <code>&lt;h:button&gt;</code>.
 * <pre>
 * &lt;o:form includeViewParams="true"&gt;
 * </pre>
 * <p>
 * This setting is ignored when <code>includeRequestParams="true"</code> or <code>useRequestURI="true"</code> is used.
 *
 * <h3>Include Request Params</h3>
 * <p>
 * As an alternative to <code>includeViewParams</code>, you can use <code>includeRequestParams="true"</code> to
 * optionally include the current GET request query string.
 * <pre>
 * &lt;o:form includeRequestParams="true"&gt;
 * </pre>
 * <p>
 * This setting overrides the <code>includeViewParams</code>.
 * This setting is ignored when <code>useRequestURI="true"</code> is used.
 *
 * <h3>Use request URI</h3>
 * <p>
 * As an alternative to <code>includeViewParams</code> and <code>includeRequestParams</code>, you can use
 * <code>useRequestURI="true"</code> to use the current request URI, including with the GET request query string, if
 * any. This is particularly useful if you're using FacesViews or forwarding everything to 1 page. Otherwise, by default
 * the current view ID will be used.
 * <pre>
 * &lt;o:form useRequestURI="true"&gt;
 * </pre>
 * <p>
 * This setting overrides the <code>includeViewParams</code> and <code>includeRequestParams</code>.
 *
 * <h3>Add query string parameters to action URL</h3>
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
 * This can be used in combination with <code>useRequestURI</code>, <code>includeViewParams</code> and
 * <code>includeRequestParams</code>. The <code>&lt;f|o:param&gt;</code> will override any included view or request
 * parameters on the same name. To conditionally add or override, use the <code>disabled</code> attribute of
 * <code>&lt;f|o:param&gt;</code>.
 * <p>
 * The support was added in OmniFaces 2.2.
 *
 * <h3>Ignore Validation Failed</h3>
 * <p>
 * In order to properly use the <code>&lt;o:ignoreValidationFailed&gt;</code> tag on an {@link UICommand} component, its
 * parent <code>&lt;h:form&gt;</code> component has to be replaced by this <code>&lt;o:form&gt;</code> component.
 * See also {@link IgnoreValidationFailed}.
 *
 * @since 1.1
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
@FacesComponent(Form.COMPONENT_TYPE)
public class Form extends HtmlForm {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.Form";

	enum PropertyKeys {
		includeViewParams,
		includeRequestParams,
		useRequestURI
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
		super.encodeBegin(new ActionURLDecorator(context, this));
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns whether or not the view parameters should be encoded into the form's action URL.
	 * @return Whether or not the view parameters should be encoded into the form's action URL.
	 */
	public boolean isIncludeViewParams() {
		return state.get(includeViewParams, FALSE);
	}

	/**
	 * Set whether or not the view parameters should be encoded into the form's action URL.
	 *
	 * @param includeViewParams
	 *            The state of the switch for encoding view parameters
	 */
	public void setIncludeViewParams(boolean includeViewParams) {
		state.put(PropertyKeys.includeViewParams, includeViewParams);
	}

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
	 *
	 * @param includeRequestParams
	 *            The state of the switch for encoding request parameters.
	 * @since 1.5
	 */
	public void setIncludeRequestParams(boolean includeRequestParams) {
		state.put(PropertyKeys.includeRequestParams, includeRequestParams);
	}

	/**
	 * Returns whether or not the request URI should be used as form's action URL.
	 * @return Whether or not the request URI should be used as form's action URL.
	 * @since 1.6
	 */
	public boolean isUseRequestURI() {
		return state.get(useRequestURI, FALSE);
	}

	/**
	 * Set whether or not the request URI should be used as form's action URL.
	 *
	 * @param useRequestURI
	 *            The state of the switch for using request URI.
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
	 * Set whether or not the form should ignore validation fail.
	 * @param ignoreValidationFailed Whether or not the form should ignore validation fail.
	 * @since 2.1
	 */
	public void setIgnoreValidationFailed(boolean ignoreValidationFailed) {
		this.ignoreValidationFailed = ignoreValidationFailed;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * FacesContext wrapper which performs NOOP during {@link FacesContext#validationFailed()} and
	 * {@link FacesContext#renderResponse()}.
	 *
	 * @author Bauke Scholtz
	 */
	static class IgnoreValidationFailedFacesContext extends FacesContextWrapper {

		private FacesContext wrapped;

		public IgnoreValidationFailedFacesContext(FacesContext wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public void validationFailed() {
			// NOOP.
		}

		@Override
		public void renderResponse() {
			// NOOP.
		}

		@Override
		public FacesContext getWrapped() {
			return wrapped;
		}

	}

	/**
	 * Helper class used for creating a FacesContext with a decorated FacesContext -&gt; Application -&gt; ViewHandler
	 * -&gt; getActionURL.
	 *
	 * @author Arjan Tijms
	 */
	static class ActionURLDecorator extends FacesContextWrapper {

		private final FacesContext facesContext;
		private final Form form;


		public ActionURLDecorator(FacesContext facesContext, Form form) {
			this.facesContext = facesContext;
			this.form = form;
		}

		@Override
		public Application getApplication() {
			return new ApplicationWrapper() {

				private final Application application = ActionURLDecorator.super.getApplication();

				@Override
				public ViewHandler getViewHandler() {
					return new ViewHandlerWrapper() {

						private final ViewHandler viewHandler = application.getViewHandler();

						/**
						 * The actual method we're decorating in order to either include the view parameters into the
						 * action URL, or include the request parameters into the action URL, or use request URI as
						 * action URL. Any <code>&lt;f|o:param&gt;</code> nested in the form component will be included
						 * in the query string, overriding any existing view or request parameters on same name.
						 */
						@Override
						public String getActionURL(FacesContext context, String viewId) {
							String url = form.isUseRequestURI() ? getActionURL(context) : super.getActionURL(context, viewId);
							String queryString = toQueryString(Components.getParams(form, form.isUseRequestURI() || form.isIncludeRequestParams(), form.isIncludeViewParams()));
							return isEmpty(queryString) ? url : url + (url.contains("?") ? "&" : "?") + queryString;
						}

						private String getActionURL(FacesContext context) {
 							String requestURI = getRequestURI(context);
							String contextPath = getRequestContextPath(context);

							// Request URI may refer /WEB-INF when request is dispatched to an error page.
							String actionURL = requestURI.startsWith(contextPath + "/WEB-INF/") ? contextPath : requestURI;
							return actionURL.isEmpty() ? "/" : actionURL;
						}

						@Override
						public ViewHandler getWrapped() {
							return viewHandler;
						}
					};
				}

				@Override
				public Application getWrapped() {
					return application;
				}
			};
		}

		@Override
		public FacesContext getWrapped() {
			return facesContext;
		}
	}

}