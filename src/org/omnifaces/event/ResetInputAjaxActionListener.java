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
 * but which are possibly been in an invalidated state because of a validation failure during a previous request.
 * <p>
 * How does it work and how does this listener solve it? First, here are some JSF facts:
 * <ul>
 * <li>When JSF validation succeeds for a particular input component, the submitted value is set to null and the
 * validated value is set as local value of the input component.
 * <li>When JSF validation fails for a particular input component, the submitted value is kept in the input component.
 * <li>When at least one input component has failed in validation, JSF will not update the model values with the local
 * value of the input components. The whole form submit is invalid and JSF will directly proceed to render response.
 * <li>When JSF renders input components after an invalid form submit, it will first test if the submitted value is not
 * null and then display it, else if the local value is not null, then it will display it, else it will display the
 * model value.
 * </ul>
 * <p>
 * So, when the validation has failed for a particular form and you happen to need to update the values by a different
 * action or even a different form by ajax (e.g. populating a field depending on a dropdown selection or on the result
 * of some modal dialog form, etc), then you basically need to reset the target input components in order to get JSF to
 * display the edited model value. Otherwise JSF will still display its local value as it was during the validation
 * failure and keep them in an invalidated state.
 * <p>
 * The {@link ResetInputAjaxActionListener} is designed to solve exactly this problem. There are basically two ways to
 * use it:
 * <ul>
 * <li><i>Either</i> register it as <code>&lt;action-listener&gt;</code> in <tt>faces-config.xml</tt>. It'll be applied
 * to every single ajax action throughout the webapp, including the standalone <code>&lt;f:ajax&gt;</code> actions on
 * <code>UIInput</code> components such as <code>&lt;h:selectOneMenu&gt;</code>.
 * <pre>
 * &lt;application&gt;
 *   &lt;action-listener&gt;org.omnifaces.event.ResetInputAjaxActionListener&lt;/action-listener&gt;
 * &lt;/application&gt;
 * </pre>
 * <li><i>Or</i> register it as <code>&lt;f:actionListener&gt;</code> on the invidivual <code>UICommand</code>
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
public class ResetInputAjaxActionListener implements ActionListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String RF_PVC_CLASS_NAME = "org.richfaces.context.ExtendedPartialViewContextImpl";
	private static final String RF_PVC_FIELD_NAME = "componentRenderIds";
	private static final String ERROR_RF_PVC_HACK =
		"Cannot obtain componentRenderIds property of RichFaces ExtendedPartialViewContextImpl instance %s";

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * @see ActionListener#processAction(ActionEvent)
	 * @throws FacesException When the current request is not an ajax request, or when <code>componentRenderIds</code>
	 * property cannot be obtained from ExtendedPartialViewContextImpl instance.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void processAction(ActionEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();
		PartialViewContext partialViewContext = context.getPartialViewContext();

		if (!partialViewContext.isAjaxRequest()) {
			return; // Ignore synchronous requests.
		}

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

		if (renderIds.isEmpty()) {
			return; // Nothing to render, thus also nothing to reset.
		}

		UIViewRoot viewRoot = context.getViewRoot();
		Set<EditableValueHolder> inputsToExecute = new HashSet<EditableValueHolder>();
		Set<EditableValueHolder> inputsToRender = new HashSet<EditableValueHolder>();

		for (String executeId : partialViewContext.getExecuteIds()) {
			findEditableValueHolders(viewRoot.findComponent(executeId), inputsToExecute);
		}

		for (String renderId : renderIds) {
			findEditableValueHolders(viewRoot.findComponent(renderId), inputsToRender);
		}

		inputsToRender.removeAll(inputsToExecute); // inputsToRender now contains inputsToReset.

		for (EditableValueHolder inputToReset : inputsToRender) {
			inputToReset.resetValue();
		}
	}

	/**
	 * Find all editable value holder components in the component hierarchy, starting with the given component and fill
	 * the given set with them.
	 * @param component The starting point of the component hierarchy to look for editable value holder components.
	 * @param editableValueHolders The set to be filled with those found editable value holder components.
	 */
	private static void findEditableValueHolders(UIComponent component, Set<EditableValueHolder> editableValueHolders) {
		if (component == null) {
			return; // TODO: shouldn't we throw an exception? This state is not to be expected.
		}

		if (component instanceof EditableValueHolder) {
			editableValueHolders.add((EditableValueHolder) component);
		}

		for (UIComponent child : component.getChildren()) {
			findEditableValueHolders(child, editableValueHolders);
		}
	}

}