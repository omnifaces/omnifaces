package org.omnifaces.taghandler;

import java.io.IOException;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

public class AttributeHandler extends TagHandler implements
		javax.faces.view.facelets.AttributeHandler {

	private final class NullValueExpressionWrapper extends ValueExpression {
		private static final long serialVersionUID = 1L;
		private ValueExpression valueExpression;

		private NullValueExpressionWrapper(ValueExpression pValueExpression) {
			valueExpression = pValueExpression;
		}

		@Override
		public boolean isLiteralText() {
			return valueExpression.isLiteralText();
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
			return valueExpression.equals(obj);
		}

		@Override
		public void setValue(ELContext context, Object value) {
			valueExpression.setValue(context, value);
		}

		@Override
		public boolean isReadOnly(ELContext context) {
			return valueExpression.isReadOnly(context);
		}

		@Override
		public Object getValue(ELContext context) {
			// FORCE NULL
			return null;
		}

		@Override
		public Class<?> getType(ELContext context) {
			return valueExpression.getType(context);
		}

		@Override
		public Class<?> getExpectedType() {
			return valueExpression.getExpectedType();
		}
	}

	private final TagAttribute name;

	private final TagAttribute value;

	private final TagAttribute applied;

	public AttributeHandler(TagConfig config) {
		super(config);
		this.name = this.getRequiredAttribute("name");
		this.value = this.getRequiredAttribute("value");
		this.applied = this.getAttribute("applied");
	}

	@Override
	public void apply(final FaceletContext ctx, UIComponent parent)
			throws IOException {
		if (parent == null) {
			throw new TagException(this.tag, "Parent UIComponent was null");
		}

		Boolean isApplied = null;
		if (this.applied == null) {
			isApplied = true;
		} else {
			isApplied = (Boolean) this.applied.getValueExpression(ctx,
					Boolean.class).getValue(ctx);
		}
		if (isApplied) {
			// only process if the parent is new to the tree
			String n = getAttributeName(ctx);
			if (!parent.getAttributes().containsKey(n)) {
				if (this.value.isLiteral()) {
					parent.getAttributes().put(n, this.value.getValue());
				} else {
					parent.setValueExpression(n,
							this.value.getValueExpression(ctx, Object.class));
				}
			}

		} else {
			String n = getAttributeName(ctx);
			if (this.value.isLiteral()) {
				parent.getAttributes().put(n, null);
			} else {
				parent.setValueExpression(n, new NullValueExpressionWrapper(
						value.getValueExpression(ctx, Object.class)));
			}
		}

	}

	@Override
	public String getAttributeName(FaceletContext ctx) {
		return this.name.getValue(ctx);
	}

}
