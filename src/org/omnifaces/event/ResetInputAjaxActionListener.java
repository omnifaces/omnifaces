package org.omnifaces.event;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

/**
 * Use this action listener when you want to partially (ajax) render input fields which are not executed during submit,
 * but which are possibly in an invalidated state because of a validation failure during a previous request. Those
 * input fields will be resetted so that they are not in an invalidated state anymore.
 * <p>
 * How does it work? First, here are some JSF facts:
 * <ul>
 * <li>When JSF validation succeeds for a particular input component during the validations phase, then the submitted
 * value is set to <code>null</code> and the validated value is set as local value of the input component.
 * <li>When JSF validation fails for a particular input component during the validations phase, then the submitted
 * value is kept in the input component.
 * <li>When at least one input component is invalid after the validations phase, then JSF will not update the model
 * values for any of the input components JSF will directly proceed to render response phase.
 * <li>When JSF renders input components, then it will first test if the submitted value is not <code>null</code> and
 * then display it, else if the local value is not <code>null</code> and then display it, else it will display the
 * model value.
 * </ul>
 * <p>
 * So, when the validation has failed for a particular form submit and you happen to need to update the values of input
 * fields by a different ajax action or even a different ajax form (e.g. populating a field depending on a dropdown
 * selection or on the result of some modal dialog form, etc), then you basically need to reset the target input
 * components in order to get JSF to display the edited model value. Otherwise JSF will still display its local value
 * as it was during the validation failure and keep them in an invalidated state.
 * <p>
 * The {@link ResetInputAjaxActionListener} is designed to solve exactly this problem. There are basically two ways to
 * use it:
 * <ul>
 * <li><p><i>Either</i> register it as <code>&lt;action-listener&gt;</code> in <tt>faces-config.xml</tt>. It'll be applied
 * to every single ajax action throughout the webapp, including the standalone <code>&lt;f:ajax&gt;</code> actions on
 * <code>UIInput</code> components such as <code>&lt;h:selectOneMenu&gt;</code>.
 * <pre>
 * &lt;application&gt;
 *   &lt;action-listener&gt;org.omnifaces.event.ResetInputAjaxActionListener&lt;/action-listener&gt;
 * &lt;/application&gt;
 * </pre>
 * <li><p><i>Or</i> register it as <code>&lt;f:actionListener&gt;</code> on the invidivual <code>UICommand</code>
 * components where this action listener is absolutely necessary to solve the concrete problem. It is <b>not</b>
 * possible to register it on the standalone <code>&lt;f:ajax&gt;</code> actions on <code>UIInput</code> components.
 * <pre>
 * &lt;h:commandButton&gt;
 *   &lt;f:ajax listener="#{bean.updateOtherInputs}" render="otherInputs" /&gt;
 *   &lt;f:actionListener type="org.omnifaces.event.ResetInputAjaxActionListener" /&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 * </ul>
 * <p>This works with standard JSF, PrimeFaces and RichFaces actions. Only for RichFaces there's a reflection hack,
 * because its <code>ExtendedPartialViewContextImpl</code> <i>always</i> returns an empty collection for render IDs.
 * See also <a href="https://issues.jboss.org/browse/RF-11112">RF issue 11112</a>.
 *
 * @author Bauke Scholtz
 * @link http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1060
 */
@SuppressWarnings("unchecked") // For the cast on Collection<String>.
public class ResetInputAjaxActionListener implements ActionListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String RF_PVC_CLASS_NAME = "org.richfaces.context.ExtendedPartialViewContextImpl";
	private static final String RF_PVC_FIELD_NAME = "componentRenderIds";

	private static final String ERROR_RF_PVC_HACK =
		"Cannot obtain componentRenderIds property of RichFaces ExtendedPartialViewContextImpl instance '%s'.";
	private static final String ERROR_INVALID_CLIENTID =
		"The %s contains an invalid client ID '%s'. Resolved to null.";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * @see ActionListener#processAction(ActionEvent)
	 * @throws FacesException When the current request is not an ajax request, or when <code>componentRenderIds</code>
	 * property cannot be obtained from ExtendedPartialViewContextImpl instance.
	 */
	@Override
	public void processAction(ActionEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();
		PartialViewContext partialViewContext = context.getPartialViewContext();

		if (!partialViewContext.isAjaxRequest()) {
			return; // Ignore synchronous requests. All executed and rendered inputs are the same anyway.
		}

		Collection<String> renderIds = getRenderIds(partialViewContext);

		if (renderIds.isEmpty()) {
			return; // Nothing to render, thus also nothing to reset.
		}

		UIViewRoot viewRoot = context.getViewRoot();
		Set<EditableValueHolder> inputs = new HashSet<EditableValueHolder>();

		// First find and add all to be rendered inputs to the set.
		for (String renderId : renderIds) {
			findAndAddEditableValueHolders(findComponent(viewRoot, renderId, "renderIds"), inputs);
		}

		// Then find and remove all executed inputs from the set.
		for (String executeId : partialViewContext.getExecuteIds()) {
			findAndRemoveEditableValueHolders(findComponent(viewRoot, executeId, "executeIds"), inputs);
		}

		// The set now contains inputs which are to be rendered, but which are not been executed. Reset them.
		for (EditableValueHolder input : inputs) {
			input.resetValue();
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Helper method with RichFaces4 hack to return the proper render IDs from the given partial view context.
	 * @param partialViewContext The partial view context to return the render IDs for.
	 * @return The render IDs.
	 */
	private Collection<String> getRenderIds(PartialViewContext partialViewContext) {
		Collection<String> renderIds = partialViewContext.getRenderIds();

		// WARNING: START OF HACK! ------------------------------------------------------------------------------------
		// HACK for RichFaces4 because its ExtendedPartialViewContextImpl class doesn't return its componentRenderIds
		// property on getRenderIds() call when the action is executed using a RichFaces-specific command button/link.
		// See also https://issues.jboss.org/browse/RF-11112
		if (renderIds.isEmpty() && partialViewContext.getClass().getName().equals(RF_PVC_CLASS_NAME)) {
			try {
				Field componentRenderIds = partialViewContext.getClass().getDeclaredField(RF_PVC_FIELD_NAME);
				componentRenderIds.setAccessible(true);
				renderIds = (Collection<String>) componentRenderIds.get(partialViewContext);
			}
			catch (Exception e) {
				throw new FacesException(String.format(ERROR_RF_PVC_HACK, partialViewContext), e);
			}
		}
		// END OF HACK ------------------------------------------------------------------------------------------------

		return renderIds;
	}

	/**
	 * Helper method to find component and throw an exception if it is <code>null</code>.
	 * @param viewRoot The view root to find the component in.
	 * @param clientId The cliend ID of the component to be found.
	 * @param collectionName "renderIds" or "executeIds".
	 */
	private static UIComponent findComponent(UIViewRoot viewRoot, String clientId, String collectionName) {
		UIComponent component = viewRoot.findComponent(clientId);

		if (component == null) {
			// This should normally not occur at this point, but you never know with some JSF impls/libraries.
			throw new IllegalArgumentException(String.format(ERROR_INVALID_CLIENTID, collectionName, clientId));
		}

		return component;
	}

	/**
	 * Find all editable value holder components in the component hierarchy, starting with the given component and
	 * add them to the given set.
	 * @param component The starting point of the component hierarchy to look for editable value holder components.
	 * @param set The set to add the found editable value holder components to.
	 */
	private static void findAndAddEditableValueHolders(UIComponent component, Set<EditableValueHolder> set) {
		if (component instanceof EditableValueHolder) {
			set.add((EditableValueHolder) component);
		}

		for (UIComponent child : component.getChildren()) {
			findAndAddEditableValueHolders(child, set);
		}
	}

	/**
	 * Find all editable value holder components in the component hierarchy, starting with the given component and
	 * remove them from the given set.
	 * @param component The starting point of the component hierarchy to look for editable value holder components.
	 * @param set The set to remove the found editable value holder components from.
	 */
	private static void findAndRemoveEditableValueHolders(UIComponent component, Set<EditableValueHolder> set) {
		if (component instanceof EditableValueHolder) {
			set.remove(component);
		}

		for (UIComponent child : component.getChildren()) {
			findAndRemoveEditableValueHolders(child, set);
		}
	}

}