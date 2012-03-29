package org.omnifaces.component.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

/**
 * <strong>ValidateOrder</strong> validates if the values of the given <code>UIInput</code> components are in the order
 * from least to greatest without duplicates, exactly as specified in the <code>components</code> attribute. The default
 * message is
 * <blockquote>{0}: Please fill out the values of all those fields in order</blockquote>
 * <p>
 * For general usage instructions, refer {@link ValidateMultipleFields} documentation.
 * <p>
 * This validator has the additional requirement that the to-be-validated values must implement {@link Comparable}.
 * This validator throws an {@link IllegalArgumentException} when one or more of the values do not implement it.
 *
 * @author Bauke Scholtz
 */
@FacesComponent(ValidateOrder.COMPONENT_TYPE)
public class ValidateOrder extends ValidateMultipleFields {

	// Public constants -----------------------------------------------------------------------------------------------

	/** The standard component type. */
	public static final String COMPONENT_TYPE = "org.omnifaces.component.validator.ValidateOrder";

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String DEFAULT_MESSAGE = "{0}: Please fill out the values of all those fields in order";
	private static final String ERROR_VALUES_NOT_COMPARABLE = "All values must implement java.lang.Comparable.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The default constructor sets the default message.
	 */
	public ValidateOrder() {
		super(DEFAULT_MESSAGE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Validate if all values are in specified order.
	 */
	@Override
	protected boolean validateValues(FacesContext context, List<UIInput> components, List<Object> values) {
		try {
			return new ArrayList<Object>(new TreeSet<Object>(values)).equals(values);
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException(ERROR_VALUES_NOT_COMPARABLE, e);
		}
	}

}