/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.taghandler;

import java.io.IOException;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.faces.component.UIComponent;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;

import org.omnifaces.el.MethodExpressionValueExpressionAdapter;

/**
 * <p>
 * The <code>&lt;o:methodParam&gt;</code> is a tag handler that can be used to pass a method expression as attribute
 * into a Facelets tag. By default this is not possible, and the expression that's intended to be a method expression
 * will be created and made available as a value expression.
 * <p>
 * This handler wraps a value expression that's actually a method expression by another value expression that returns a
 * method expression that gets the value of first value expression, which as "side-effect" executes the original method
 * expression. This somewhat over-the-top chain of wrapping is done so a method expression can be passed as attribute
 * into a Facelet tag.
 *
 * @author Arjan Tijms
 */
public class MethodParam extends TagHandler {

	private final TagAttribute name;
	private final TagAttribute value;

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public MethodParam(TagConfig config) {
		super(config);
		name = getRequiredAttribute("name");
		value = getRequiredAttribute("value");
	}

	@Override
	public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
		String nameStr = name.getValue(ctx);

		// The original value expression we get inside the Facelets tag, that's actually the method expression passed-in by the user.
		ValueExpression valueExpression = value.getValueExpression(ctx, Object.class);

		// A method expression that wraps the value expression and uses its own invoke method to get the value from the wrapped expression.
		MethodExpression methodExpression = new MethodExpressionValueExpressionAdapter(valueExpression);

		// Using the variable mapper so the expression is scoped to the body of the Facelets tag. Since the variable mapper only accepts
		// value expressions, we once again wrap it by a value expression that directly returns the method expression.
		ValueExpression valueExpressionWrapper = ctx.getExpressionFactory().createValueExpression(methodExpression, MethodExpression.class);

		ctx.getVariableMapper().setVariable(nameStr, valueExpressionWrapper);
	}

}