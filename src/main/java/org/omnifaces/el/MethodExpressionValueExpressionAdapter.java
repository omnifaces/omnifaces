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
package org.omnifaces.el;

import static java.util.logging.Level.FINEST;
import static org.omnifaces.util.Utils.unmodifiableSet;

import java.util.Set;
import java.util.logging.Logger;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.ValueExpression;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.validator.ValidatorException;

/**
 * This MethodExpression wraps a ValueExpression.
 * <p>
 * With this wrapper a value expression can be used where a method expression is expected.
 * The return value of the method execution will be the value represented by the value expression.
 *
 * @author Arjan Tijms
 *
 */
public class MethodExpressionValueExpressionAdapter extends MethodExpression {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(MethodExpressionValueExpressionAdapter.class.getName());

	private static final Set<Class<? extends Throwable>> EXCEPTIONS_TO_UNWRAP = unmodifiableSet(
		MethodNotFoundException.class, // Needed for proper action listener error handling.
		ConverterException.class, // Needed for proper conversion error handling.
		ValidatorException.class); // Needed for proper validation error handling.

	private final ValueExpression valueExpression;

	/**
	 * Construct method expression which adapts the given value expression.
	 * @param valueExpression Value expression to be adapted to method expression.
	 */
	public MethodExpressionValueExpressionAdapter(ValueExpression valueExpression) {
		this.valueExpression = valueExpression;
	}

	@Override
	public Object invoke(ELContext context, Object[] params) {
		try {
			return valueExpression.getValue(new ValueToInvokeElContext(context, params));
		}
		catch (ELException e) {
			for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
				if (EXCEPTIONS_TO_UNWRAP.contains(cause.getClass())) {
					throw (RuntimeException) cause;
				}
			}

			throw e;
		}
	}

	@Override
	public MethodInfo getMethodInfo(ELContext context) {
		return ExpressionInspector.getMethodReference(context, valueExpression);
	}

	@Override
	public boolean isLiteralText() {
		return false;
	}

	@Override
	public int hashCode() {
		return valueExpression.hashCode();
	}

	@Override
	public String getExpressionString() {
		return valueExpression.getExpressionString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof MethodExpressionValueExpressionAdapter) {
			return ((MethodExpressionValueExpressionAdapter)obj).getValueExpression().equals(valueExpression);
		}

		return false;
	}

	/**
	 * Returns the underlying value expression.
	 * @return The underlying value expression.
	 */
	public ValueExpression getValueExpression() {
		return valueExpression;
	}

	/**
	 * Custom ELContext implementation that wraps a given ELContext to be able to provide a custom
	 * ElResolver.
	 *
	 */
	static class ValueToInvokeElContext extends ELContextWrapper {

		// The parameters provided by the client that calls the EL method expression, as opposed to those
		// parameters that are bound to the expression when it's created in EL (like #{bean.myMethod(param1, param2)}).
		private final Object[] callerProvidedParameters;

		public ValueToInvokeElContext(ELContext elContext, Object[] callerProvidedParameters) {
			super(elContext);
			this.callerProvidedParameters = callerProvidedParameters;
		}

		@Override
		public ELResolver getELResolver() {
			return new ValueToInvokeElResolver(super.getELResolver(), callerProvidedParameters);
		}
	}

	/**
	 * Custom EL Resolver that turns calls for value expression calls (getValue) into method expression calls (invoke).
	 *
	 */
	static class ValueToInvokeElResolver extends ELResolverWrapper {

		// Null should theoretically be accepted, but some EL implementations want an empty array.
		private static final Object[] EMPTY_PARAMETERS = new Object[0];
		private final Object[] callerProvidedParameters;

		public ValueToInvokeElResolver(ELResolver elResolver, Object[] callerProvidedParameters) {
			super(elResolver);
			this.callerProvidedParameters = callerProvidedParameters;
		}

		@Override
		public Object getValue(ELContext context, Object base, Object property) {

			// If base is null, we're resolving it. Base should always be resolved as a value expression.
			if (base == null) {
				return super.getValue(context, null, property);
			}

			// Turn getValue calls into invoke.

			// Note 1: We can not directly delegate to invoke() here, since otherwise chained expressions
			// "like base.value.value.expression" will not resolve correctly.
			//
			// Note 2: Some EL implementations call getValue when invoke should be called already. This typically happens when the
			// method expression takes no parameters, but is specified with parentheses, e.g. "#{mybean.doAction()}".
			try {
				return super.getValue(context, base, property);
			} catch (PropertyNotFoundException ignore) {
				logger.log(FINEST, "Ignoring thrown exception; there is really no clean way to distinguish a ValueExpression from a MethodExpression.", ignore);

				try {
					return super.invoke(context, base, property, null, callerProvidedParameters != null ? callerProvidedParameters : EMPTY_PARAMETERS);
				} catch (MethodNotFoundException e) {
					// Wrap into new ELException since down the call chain, ElExceptions might be caught, unwrapped one level and then wrapped in
					// a new ELException. Without wrapping here, we'll then loose this exception.
					throw new ELException(e.getMessage(), e);
				}
			}
		}
	}
}
