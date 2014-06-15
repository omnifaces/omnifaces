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

import static org.omnifaces.util.Faces.isPostback;

import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewParameter;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;

import org.omnifaces.util.MapWrapper;

/**
 * <strong>ViewParameter</strong> is a component that extends the standard {@link UIViewParameter} and provides a
 * stateless mode of operation and fixes the issue wherein null model values are converted to empty string parameters
 * in query string (e.g. when <code>includeViewParams=true</code>).
 * <p>
 * The standard UIViewParameter implementation calls the model setter again after postback. This is not always desired
 * when being bound to a view scoped bean and can lead to performance problems when combined with an expensive converter.
 * To solve this, this component by default stores the submitted value as a component property instead of in the model
 * (and thus in the view state in case the binding is to a view scoped bean).
 * <p>
 * The standard UIViewParameter implementation calls the converter regardless of whether the evaluated model value is
 * <code>null</code> or not. As converters by specification return an empty string in case of <code>null</code> value,
 * this is being added to the query string as an empty parameter. This is not desired.
 * <p>
 * You can use it the same way as <code>&lt;f:viewParam&gt;</code>, you only need to change <code>f:</code> to
 * <code>o:</code>.
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
@FacesComponent(ViewParam.COMPONENT_TYPE)
public class ViewParam extends UIViewParameter {

	public static final String COMPONENT_TYPE = "org.omnifaces.component.input.ViewParam";

	private String submittedValue;
	private Map<String, Object> attributeInterceptMap;

	@Override
	public void setSubmittedValue(Object submittedValue) {
		this.submittedValue = (String) submittedValue;
	}

	@Override
	public String getSubmittedValue() {
		return submittedValue;
	}

	@Override
	public boolean isRequired() {
		// The request parameter is ignored on postbacks, however it's already present in the view scoped bean.
		// So we can safely skip the required validation on postbacks.
		return !isPostback() && super.isRequired();
	}

	@Override
	public void processDecodes(FacesContext context) {
		// Ignore any request parameters that are present when the postback is done.
		if (!context.isPostback()) {
			super.processDecodes(context);
		}
	}

	@Override
	public void processValidators(FacesContext context) {
		if (!context.isPostback()) {
			super.processValidators(context);
		}
	}

	@Override
	public Map<String, Object> getAttributes() {
		if (attributeInterceptMap == null) {
			attributeInterceptMap = new MapWrapper<String, Object>(super.getAttributes()) {

				private static final long serialVersionUID = -7674000948288609007L;

				@Override
				public Object get(Object key) {
					if ("label".equals(key)) {
						return getLabel();
					}
					else {
						return super.get(key);
					}
				}

				private Object getLabel() {
					// First check if our wrapped Map has the label
					Object label = getWrapped().get("label");
					if (label == null || (label instanceof String && ((String) label).isEmpty())) {

						// Next check if our outer component has a value expression for the label
						ValueExpression labelExpression = ViewParam.this.getValueExpression("label");
						if (labelExpression != null) {
							label = labelExpression.getValue(FacesContext.getCurrentInstance().getELContext());
						}
					}

					// No explicit label defined, default to "name" (which is in many cases the most sane label anyway).
					if (label == null) {
						label = ViewParam.this.getName();
					}

					return label;
				}

			};
		}

		return attributeInterceptMap;
	}

	/**
	 * When there's a value expression and the evaluated model value is <code>null</code>, then just return
	 * <code>null</code> instead of delegating to default implementation which would return an empty string when a
	 * converter is attached.
	 * @since 1.8
	 */
	@Override
	public String getStringValueFromModel(FacesContext context) throws ConverterException {
		ValueExpression ve = getValueExpression("value");
		Object value = (ve != null) ? ve.getValue(context.getELContext()) : null;
		return (value != null) ? super.getStringValueFromModel(context) : null;
	}

}