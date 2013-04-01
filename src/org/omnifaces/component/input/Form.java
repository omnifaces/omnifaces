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
import static java.lang.Boolean.TRUE;
import static java.util.regex.Pattern.quote;
import static org.omnifaces.component.input.Form.PropertyKeys.includeRequestParams;
import static org.omnifaces.component.input.Form.PropertyKeys.includeViewParams;
import static org.omnifaces.util.Utils.decodeURL;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.FacesComponent;
import javax.faces.component.UICommand;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewParameter;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.view.ViewMetadata;
import javax.servlet.http.HttpServletRequest;

import org.omnifaces.taghandler.IgnoreValidationFailed;
import org.omnifaces.util.State;

/**
 * <strong>Form</strong> is a component that extends the standard {@link UIForm} and provides a way to keep view
 * parameters in the request URL after a post-back and offers in combination with the
 * <code>&lt;o:ignoreValidationFailed&gt;</code> tag on an {@link UICommand} component the possibility to ignore
 * validation failures so that the invoke action phase will be executed anyway.
 * <p>
 * You can use it the same way as <code>&lt;h:form&gt;</code>, you only need to change <code>h:</code> to
 * <code>o:</code>.
 *
 * <h4>Include View Params</h4>
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
 * To solve this, this component offers an attribute <code>includeViewParams</code> that will optionally include all
 * view parameters, in exactly the same way that this can be done for <code>&lt;h:link&gt;</code> and
 * <code>&lt;h:button&gt;</code>.
 *
 * <h4>Ignore Validation Failed</h4>
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
public class Form extends UIForm {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.Form";

	enum PropertyKeys {
		includeViewParams,
		includeRequestParams
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private final State state = new State(getStateHelper());

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void processValidators(FacesContext context) {
		if (isIgnoreValidationFailed(context)) {
			super.processValidators(new IgnoreValidationFailedFacesContext(context));
		}
		else {
			super.processValidators(context);
		}
	}

	@Override
	public void processUpdates(FacesContext context) {
		if (isIgnoreValidationFailed(context)) {
			super.processUpdates(new IgnoreValidationFailedFacesContext(context));
		}
		else {
			super.processUpdates(context);
		}
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (isIncludeRequestParams()) {
			super.encodeBegin(new ActionURLDecorator(context, includeRequestParams));
		}
		else if (isIncludeViewParams()) {
			super.encodeBegin(new ActionURLDecorator(context, includeViewParams));
		} else {
			super.encodeBegin(context);
		}
	}

	private boolean isIgnoreValidationFailed(FacesContext context) {
		return context.getAttributes().get(IgnoreValidationFailed.class.getName()) == TRUE;
	}

	
	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Return whether or not the view parameters should be encoded into the form's action URL.
	 */
	public Boolean isIncludeViewParams() {
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
	 * Return whether or not the request parameters should be encoded into the form's action URL.
	 * @since 1.5
	 */
	public Boolean isIncludeRequestParams() {
		return state.get(includeRequestParams, FALSE);
	}

	/**
	 * Set whether or not the request parameters should be encoded into the form's action URL.
	 *
	 * @param includeRequestParams
	 *            The state of the switch for encoding request parameters
	 * @since 1.5
	 */
	public void setIncludeRequestParams(boolean includeRequestParams) {
		state.put(PropertyKeys.includeRequestParams, includeRequestParams);
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
		private final PropertyKeys type;


		public ActionURLDecorator(FacesContext facesContext, PropertyKeys type) {
			this.facesContext = facesContext;
			this.type = type;
		}

		@Override
		public Application getApplication() {
			return new ApplicationWrapper() {

				private final Application application = ActionURLDecorator.super.getApplication();

				@Override
				public ViewHandler getViewHandler() {
					final ApplicationWrapper outer = this;

					return new ViewHandlerWrapper() {

						private final ViewHandler viewHandler = outer.getWrapped().getViewHandler();

						/**
						 * The actual method we're decorating in order to include the view parameters into the action
						 * URL.
						 */
						@Override
						public String getActionURL(FacesContext context, String viewId) {
							return context.getExternalContext().encodeBookmarkableURL(
								super.getActionURL(context, viewId),
								type == includeRequestParams? getRequestParameterMap(context) : getViewParameterMap(context)
							);
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

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Gets parameters associated with the {@link UIViewParameter}s as a request-parameter like map of Strings.
	 * <p>
	 * In the returned map, keys represent the parameter name, while the value is a list of one of more values
	 * associated with that parameter name.
	 *
	 * @param context
	 * @return Map with parameters. An empty map will be returned if there are no parameters.
	 */
	private static Map<String, List<String>> getViewParameterMap(FacesContext context) {
		Collection<UIViewParameter> viewParameters = ViewMetadata.getViewParameters(context.getViewRoot());
		if (viewParameters.isEmpty()) {
			return Collections.<String, List<String>> emptyMap();
		}

		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		for (UIViewParameter viewParameter : viewParameters) {
			String value = viewParameter.getStringValue(context);

			if (value != null) {
				// #138: <f:viewParam> doesn't support multiple values anyway, so having multiple <f:viewParam> on the
				// same request parameter shouldn't end up in repeated parameters in action URL.
				parameters.put(viewParameter.getName(), Collections.singletonList(value));
			}
		}

		return parameters;
	}
	
	/**
	 * Gets parameters from the URL query (aka GET parameters) as a request-parameter like map of Strings.
	 * <p>
	 * In the returned map, keys represent the parameter name, while the value is a list of one of more values
	 * associated with that parameter name.
	 * <p>
	 * Note this method returns ONLY the URL query parameters, as opposed to {@link ExternalContext#getRequestParameterValuesMap()} which
	 * contains both URL (GET) parameters and body (POST) parameters. 
	 *
	 * @param context
	 * @return Map with parameters. An empty map will be returned if there are no parameters.
	 * @since 1.5
	 */
	private static Map<String, List<String>> getRequestParameterMap(FacesContext context) {
		
		String queryString = ((HttpServletRequest) context.getExternalContext().getRequest()).getQueryString();
		if (isEmpty(queryString)) {
			return Collections.<String, List<String>> emptyMap();
		}
		
		String[] keyValueParameters = queryString.split(quote("&"));
		
		Map<String, List<String>> parameters = new LinkedHashMap<String, List<String>>();
		for (String keyValueParameter : keyValueParameters) {
			
			if (keyValueParameter.contains("=")) {
				String[] keyAndValue = keyValueParameter.split(quote("="));
				String key = decodeURL(keyAndValue[0]);
				String value = "";
				if (keyAndValue.length > 1 && !isEmpty(keyAndValue[1])) {
					value = decodeURL(keyAndValue[1]);
				}
			
				List<String> values = parameters.get(key);
				if (values == null) {
					values = new ArrayList<String>();
					parameters.put(key, values);
				}
			
				values.add(value);
			}
		}
		
		return parameters;
	}

}