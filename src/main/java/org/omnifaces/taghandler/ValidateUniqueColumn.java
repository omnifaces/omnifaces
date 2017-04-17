/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.taghandler;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.Components.getLabel;
import static org.omnifaces.util.Faces.getELContext;
import static org.omnifaces.util.Messages.addError;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * <p>
 * The <code>&lt;o:validateUniqueColumn&gt;</code> validates if the given {@link UIInput} component in an {@link UIData}
 * component has an unique value throughout all rows, also those not visible by pagination. This validator works
 * directly on the data model and may therefore not work as expected if the data model does not represent
 * <strong>all</strong> available rows of the {@link UIData} component (e.g. when there's means of lazy loading).
 * <p>
 * The default message is
 * <blockquote>{0}: Please fill out an unique value for the entire column. Duplicate found in row {1}</blockquote>
 *
 * <h3>Usage</h3>
 * <p>
 * Usage example:
 * <pre>
 * &lt;h:dataTable value="#{bean.items}" var="item"&gt;
 *     &lt;h:column&gt;
 *         &lt;h:inputText value="#{item.value}"&gt;
 *             &lt;o:validateUniqueColumn /&gt;
 *         &lt;/h:inputText&gt;
 *     &lt;/h:column&gt;
 * &lt;/h:dataTable&gt;
 * </pre>
 * <p>
 * In an invalidating case, only the first row on which the value is actually changed (i.e. the value change event has
 * been fired on the input component in the particular row) will be marked invalid and a faces message will be added
 * on the client ID of the input component in the particular row. The default message can be changed by the
 * <code>message</code> attribute. Any "{0}" placeholder in the message will be substituted with the label of the
 * input component. Any "{1}" placeholder in the message will be substituted with the 1-based row index of the data
 * model. Note that this does not take pagination into account and that this needs if necessary to be taken care of in
 * the custom message yourself.
 * <pre>
 * &lt;o:validateUniqueColumn message="Duplicate value!" /&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 1.3
 */
public class ValidateUniqueColumn extends TagHandler implements ValueChangeListener {

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String DEFAULT_MESSAGE =
		"{0}: Please fill out an unique value for the entire column. Duplicate found in row {1}";

	private static final String ERROR_INVALID_PARENT =
		"Parent component of o:validateUniqueColumn must be an instance of UIInput. Encountered invalid type '%s'.";
	private static final String ERROR_INVALID_PARENT_PARENT =
		"Parent component of o:validateUniqueColumn must be enclosed in an UIData component.";

	// Properties -----------------------------------------------------------------------------------------------------

	private ValueExpression message;
	private ValueExpression disabled;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ValidateUniqueColumn(TagConfig config) {
		super(config);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the component is new, check if it's an instance of {@link UIInput} and then register this tag as a value
	 * change listener on it. If the component is not new, check if there's an {@link UIData} parent.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!ComponentHandler.isNew(parent)) {
			if (getClosestParent(parent, UIData.class) == null) {
				throw new IllegalArgumentException(ERROR_INVALID_PARENT_PARENT);
			}

			return;
		}

		if (!(parent instanceof UIInput)) {
			throw new IllegalArgumentException(format(ERROR_INVALID_PARENT, parent.getClass().getName()));
		}

		// Get the tag attributes as value expressions instead of the immediately evaluated values. This allows us to
		// re-evaluate them on a per-row basis which in turn allows the developer to use the currently iterated row
		// object in the message and/or the disabled attribute.
		message = getValueExpression("message", context);
		disabled = getValueExpression("disabled", context);

		// This validator is registered as a value change listener, which thus does only a full UIData tree visit when
		// the value is really changed. If it were a normal validator, then if would have performed the same visit on
		// every single row which would have been very inefficient.
		((UIInput) parent).addValueChangeListener(this);
	}

	/**
	 * Get the value of the tag attribute associated with the given attribute name as a value expression.
	 */
	private ValueExpression getValueExpression(String attributeName, FaceletContext context) {
		TagAttribute attribute = getAttribute(attributeName);

		if (attribute != null) {
			return attribute.getValueExpression(context, Object.class);
		}

		return null;
	}

	/**
	 * When this tag is not disabled, the input value is changed, the input component is valid and the input component's
	 * local value is not null, then check for a duplicate value by visiting all rows of the parent {@link UIData}
	 * component.
	 */
	@Override
	public void processValueChange(ValueChangeEvent event) {
		if (isDisabled()) {
			return;
		}

		UIInput input = (UIInput) event.getComponent();

		if (!input.isValid() || input.getLocalValue() == null) {
			return;
		}

		UIData table = getClosestParent(input, UIData.class);
		int originalRows = table.getRows();
		table.setRows(0); // We want to visit all rows.

		FacesContext context = FacesContext.getCurrentInstance();
		UniqueColumnValueChecker checker = new UniqueColumnValueChecker(table, input);
		table.visitTree(VisitContext.createVisitContext(context), checker);
		table.setRows(originalRows);

		if (checker.isDuplicate()) {
			input.setValid(false);
			context.validationFailed();
			addError(input.getClientId(context), getMessage(), getLabel(input), checker.getDuplicateIndex() + 1);
		}
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns the runtime evaluated value of the message attribute.
	 * @return The runtime evaluated value of the message attribute.
	 */
	public String getMessage() {
		return getValue(message, DEFAULT_MESSAGE);
	}

	/**
	 * Returns the runtime evaluated value of the disabled attribute.
	 * @return The runtime evaluated value of the disabled attribute.
	 */
	public boolean isDisabled() {
		if (disabled == null) {
			return false;
		}

		if (disabled.isLiteralText()) {
			return parseBoolean(disabled.getExpressionString());
		}

		return getValue(disabled, false);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the evaluated value of the given value expression, or the given default value if the given value
	 * expression itself or its evaluated value is <code>null</code>.
	 * @param expression The value expression to return the value for.
	 * @param defaultValue The default value to return if the value expression itself or its evaluated value is
	 * <code>null</code>.
	 * @return The evaluated value of the given value expression, or the given default value if the given value
	 * expression itself or its evaluated value is <code>null</code>.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getValue(ValueExpression expression, T defaultValue) {
		if (expression != null) {
			T value = (T) expression.getValue(getELContext());

			if (value != null) {
				return value;
			}
		}

		return defaultValue;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * The unique column value checker as tree visit callback.
	 * @author Bauke Scholtz
	 */
	private static class UniqueColumnValueChecker implements VisitCallback {

		private UIData table;
		private int rowIndex;
		private UIInput input;
		private Object value;
		private boolean duplicate;
		private int duplicateIndex;

		public UniqueColumnValueChecker(UIData table, UIInput input) {
			this.table = table;
			rowIndex = table.getRowIndex();
			this.input = input;
			value = input.getLocalValue();
		}

		@Override
		public VisitResult visit(VisitContext context, UIComponent target) {
			// Yes, this check does look a bit strange, but really physically the very same single UIInput component is
			// been reused in all rows of the UIData component. It's only its internal state which changes on a per-row
			// basis, as would happen during the tree visit. Those changes are reflected in the "input" reference.
			if (target == input && rowIndex != table.getRowIndex()
				&& input.isValid() && value.equals(input.getLocalValue()))
			{
				duplicate = true;
				duplicateIndex = table.getRowIndex();
				return VisitResult.COMPLETE;
			}

			return VisitResult.ACCEPT;
		}

		public boolean isDuplicate() {
			return duplicate;
		}

		public int getDuplicateIndex() {
			return duplicateIndex;
		}

	}

}