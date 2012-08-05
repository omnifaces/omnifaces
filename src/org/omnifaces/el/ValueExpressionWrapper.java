package org.omnifaces.el;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.FacesWrapper;

public class ValueExpressionWrapper extends ValueExpression implements FacesWrapper<ValueExpression>  {

	private static final long serialVersionUID = -6864367137534689191L;
	
	private ValueExpression valueExpression;
	
	public ValueExpressionWrapper(ValueExpression valueExpression) {
		this.valueExpression = valueExpression;
	}
	
	@Override
	public boolean equals(Object obj) {
		return getWrapped().equals(obj);
	}
	
	@Override
	public Class<?> getExpectedType() {
		return getWrapped().getExpectedType();
	}
	
	@Override
	public String getExpressionString() {
		return getWrapped().getExpressionString();
	}
	
	@Override
	public Class<?> getType(ELContext context) {
		return getWrapped().getType(context);
	}
	
	@Override
	public Object getValue(ELContext context) {
		return getWrapped().getValue(context);
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode();
	}
	
	@Override
	public boolean isLiteralText() {
		return getWrapped().isLiteralText();
	}
	
	@Override
	public boolean isReadOnly(ELContext context) {
		return getWrapped().isReadOnly(context);
	}
	
	@Override
	public void setValue(ELContext context, Object value) {
		getWrapped().setValue(context, value);
	}
	
	@Override
	public ValueReference getValueReference(ELContext context) {
		return getWrapped().getValueReference(context);
	}
	
	@Override
	public ValueExpression getWrapped() {
		return valueExpression;
	}
	
}
