package org.omnifaces.el;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

import org.omnifaces.util.Hacks;

/**
 * Special purpose value expression that wraps a method expression for which a Method instance is created
 * whenever the getValue method is called.
 *
 * @author Arjan Tijms
 * @since 1.4
 */
public final class ValueExpressionMethodWrapper extends ValueExpression {

	private static final long serialVersionUID = 891954866066788234L;

	private MethodExpression methodExpression;

	public ValueExpressionMethodWrapper(MethodExpression methodExpression) {
		this.methodExpression = methodExpression;
	}

	@Override
	public Object getValue(ELContext context) {
		return Hacks.methodExpressionToStaticMethod(context, methodExpression);
	}

	@Override
	public void setValue(ELContext context, Object value) {
		throw new PropertyNotWritableException();
	}

	@Override
	public boolean isReadOnly(ELContext context) {
		return true;
	}

	@Override
	public Class<?> getType(ELContext context) {
		return methodExpression.getClass();
	}

	@Override
	public Class<?> getExpectedType() {
		return methodExpression.getClass();
	}

	@Override
	public String getExpressionString() {
		return methodExpression.toString();
	}

	@Override
	public boolean equals(Object object) {
		// Basic checks.
		if (object == this) {
			return true;
		}
		if (object == null || object.getClass() != getClass()) {
			return false;
		}

		// Property checks.
		ValueExpressionMethodWrapper other = (ValueExpressionMethodWrapper) object;
		if (methodExpression == null ? other.methodExpression != null : !methodExpression.equals(other.methodExpression)) {
			return false;
		}

		// All passed.
		return true;
	}

	@Override
	public int hashCode() {
		return methodExpression.hashCode();
	}

	@Override
	public boolean isLiteralText() {
		return true;
	}

}