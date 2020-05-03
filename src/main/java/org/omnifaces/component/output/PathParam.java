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
package org.omnifaces.component.output;

import static org.omnifaces.util.FacesLocal.createConverter;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;

import org.omnifaces.facesviews.FacesViews;
import org.omnifaces.facesviews.FacesViewsViewHandler;

/**
 * The <code>&lt;o:pathParam&gt;</code> is a component that extends the OmniFaces {@link Param} to support
 * <code>MultiViews</code> feature of {@link FacesViews}. It is done by rendering the supplied parameters of such components
 * as <code>&lt;h:link&gt;</code> and <code>&lt;h:button&gt;</code> among others as path parameters and not as
 * query parameters as otherwise will be produced by <code>&lt;o:param&gt;</code> and <code>&lt;f:param&gt;</code> tags.
 * <p>
 * The component has built-in support for a {@link Converter} to convert the supplied value to string by usual means via
 * the <code>converter</code> attribute of the tag, or the nested <code>&lt;f:converter&gt;</code> tag, or just automatically
 * if a converter is already registered for the target class via <code>@FacesConverter(forClass)</code>.
 * <p>
 * This component doesn't support returning encoded output of its children as parameter value in case no value is present.
 * This feature is provided by {@link Param} component instead.
 * <p>
 * Also, the name attribute must not be specified for this component as it already evaluates to a predefined one.
 * <p>
 * This component is used to create bookmarkable URLs via standard outcome target components that take into account
 * <code>&lt;o:pathParam&gt;</code> tags nested in the components. The path parameters will be rendered
 * in the order they were declared for a view id that is defined as a multi view and if the view was
 * not defined as a multi view then they won't be rendered at all. Additionally, declaring path parameters
 * for a non-multi view will be logged as a warning and a faces warning message will be added for any
 * stage different from <code>Production</code>.
 * <p>
 * In the following example the link to the multi view page will be rendered with two path parameters:
 * <pre>
 * &lt;h:link value="Link" outcome="multiview-supported-path"&gt;
 *     &lt;o:pathParam value="first" /&gt;
 *     &lt;o:pathParam value="second" /&gt;
 * &lt;/h:link&gt;
 * </pre>
 * The code above will be rendered as:
 * <code>&lt;a id="..." name="..." href="/context-path/multiview-supported-path/first/second"&gt;Link&lt;/a&gt;</code>.
 * The path parameters will be available via <code>@Inject @Param(pathIndex=0) private String first;</code> and
 * <code>@Inject @Param(pathIndex=1) private String second;</code> the usual way.
 *
 * @author Sergey Kuntsel
 * @param <T> The type of the value.
 * @since 3.6
 * @see FacesViews
 * @see FacesViewsViewHandler#getBookmarkableURL(FacesContext, String, java.util.Map, boolean)
 */
@FacesComponent(PathParam.COMPONENT_TYPE)
public class PathParam<T> extends Param<T> {

	// Constants
	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.PathParam";
	public static final String PATH_PARAM_NAME_ATTRIBUTE_VALUE = "org.omnifaces.pathparam";

	// Actions
	@Override
	@SuppressWarnings("unchecked")
	public String getValue() {
		FacesContext context = getFacesContext();
		Converter<T> converter = getConverter();
		Object value = getLocalValue();

		if (converter == null && value != null) {
			converter = createConverter(context, value.getClass());
		}

		if (converter != null) {
			return converter.getAsString(context, this, (T) value);
		}

		return value != null ? value.toString() : null;
	}

	@Override
	public void setName(String name) {
		// Do nothing.
	}

	@Override
	public String getName() {
		return PATH_PARAM_NAME_ATTRIBUTE_VALUE; // Always return a predefined name.
	}

}
