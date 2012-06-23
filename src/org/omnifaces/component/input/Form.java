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

import static org.omnifaces.component.input.Form.PropertyKeys.includeViewParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewParameter;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.view.ViewMetadata;

/**
 * <strong>Form</strong> is a component that extends the standard {@link UIForm} and provides a way to keep view parameters in the request URL after a
 * post-back.
 * <p>
 * The standard UIForm doesn't put the original view parameters in the action URL that's used for the post-back. Instead, it relies on those view
 * parameters to be stored in the state associated with the standard {@link UIViewParameter}. Via this state those parameters are invisibly re-applied
 * after every post-back.
 * <p>
 * The disadvantage of this invisible retention of view parameters is that the user doesn't see them anymore in the address bar of the browser that is
 * used to interact with the faces application. Copy-pasting the URL from the address bar or refreshing the page by hitting enter inside the address
 * bar will therefore not always yield the expected results.
 * <p>
 * To solve this, this component offers an attribute <code>includeViewParams</code> that will optionally include all view parameters, in exactly the
 * same way that this can be done for <code>&lt;h:link&gt;<code> and <code>&lt;h:button&gt;<code>.
 * <p>
 * You can use it the same way as <code>&lt;f:form&gt;</code>, you only need to change <code>h:</code> to <code>o:</code>.
 * 
 * @since 1.1
 * @author Arjan Tijms
 */
@FacesComponent(Form.COMPONENT_TYPE)
public class Form extends UIForm {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.Form";

	enum PropertyKeys {
		includeViewParams
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (isIncludeViewParams()) {
			super.encodeBegin(new ActionURLDecorator(context));
		} else {
			super.encodeBegin(context);
		}
	}

	/**
	 * Return whether or not the view parameters should be encoded into the form's action URL.
	 */
	public boolean isIncludeViewParams() {
		return (Boolean) getStateHelper().eval(includeViewParams, false);
	}

	/**
	 * Set whether or not the view parameters should be encoded into the form's action URL.
	 * 
	 * @param includeViewParams
	 *            The state of the switch for encoding view parameters
	 */
	public void setIncludeViewParams(boolean includeViewParams) {
		getStateHelper().put(PropertyKeys.includeViewParams, includeViewParams);
	}

	/**
	 * Helper class used for creating a FacesContext with a decorated FacesContext -&gt; Application -&gt; ViewHandler -&gt; getActionURL.
	 * 
	 * @author Arjan Tijms
	 * 
	 */
	static class ActionURLDecorator extends FacesContextWrapper {

		private final FacesContext facesContext;

		public ActionURLDecorator(FacesContext facesContext) {
			this.facesContext = facesContext;
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
						 * The actual method we're decorating in order to include the view parameters into the action URL.
						 */
						@Override
						public String getActionURL(FacesContext context, String viewId) {
							return context.getExternalContext().encodeBookmarkableURL(
								super.getActionURL(context, viewId),
						        getViewParameterMap(context)
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

	/**
	 * Gets parameters associated with the {@link UIViewParameter}s as a request-parameter like map of Strings.
	 * <p>
	 * In the returned map, keys represent the parameter name, while the value is a list of one of more values associated with that parameter name.
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

			String key = viewParameter.getName();
			String value = viewParameter.getStringValue(context);

			if (value != null) {
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
