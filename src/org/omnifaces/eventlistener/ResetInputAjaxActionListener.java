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
package org.omnifaces.eventlistener;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.AjaxBehaviorListener;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.SystemEventListener;

import org.omnifaces.util.Components;

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
 * values for any of the input components. JSF will directly proceed to render response phase.
 * <li>When JSF renders input components, then it will first test if the submitted value is not <code>null</code> and
 * then display it, else if the local value is not <code>null</code> and then display it, else it will display the
 * model value.
 * <li>As long as you're interacting with the same JSF view, you're dealing with the same component state.
 * </ul>
 * <p>
 * So, when the validation has failed for a particular form submit and you happen to need to update the values of input
 * fields by a different ajax action or even a different ajax form (e.g. populating a field depending on a dropdown
 * selection or the result of some modal dialog form, etc), then you basically need to reset the target input
 * components in order to get JSF to display the model value which was edited during invoke action. Otherwise JSF will
 * still display its local value as it was during the validation failure and keep them in an invalidated state.
 * <p>
 * The {@link ResetInputAjaxActionListener} is designed to solve exactly this problem. There are basically three ways
 * to configure and use it:
 * <ul>
 * <li><p>Register it as <code>&lt;phase-listener&gt;</code> in <tt>faces-config.xml</tt>. It'll be applied
 * to <strong>every single</strong> ajax action throughout the webapp, on both <code>UIInput</code> and
 * <code>UICommand</code> components.
 * <pre>
 * &lt;lifecycle&gt;
 *   &lt;phase-listener&gt;org.omnifaces.eventlistener.ResetInputAjaxActionListener&lt;/phase-listener&gt;
 * &lt;/lifecycle&gt;
 * </pre>
 * <li><p><i>Or</i> register it as <code>&lt;action-listener&gt;</code> in <tt>faces-config.xml</tt>. It'll
 * <strong>only</strong> be applied to ajax actions which are invoked by an <code>UICommand</code> component such as
 * <code>&lt;h:commandButton&gt;</code> and <code>&lt;h:commandLink&gt;</code>.
 * <pre>
 * &lt;application&gt;
 *   &lt;action-listener&gt;org.omnifaces.eventlistener.ResetInputAjaxActionListener&lt;/action-listener&gt;
 * &lt;/application&gt;
 * </pre>
 * <li><p><i>Or</i> register it as <code>&lt;f:actionListener&gt;</code> on the invidivual <code>UICommand</code>
 * components where this action listener is absolutely necessary to solve the concrete problem. Note that it isn't
 * possible to register it on the individual <code>UIInput</code> components using the standard JSF tags.
 * <pre>
 * &lt;h:commandButton value="Update" action="#{bean.updateOtherInputs}"&gt;
 *   &lt;f:ajax execute="currentInputs" render="otherInputs" /&gt;
 *   &lt;f:actionListener type="org.omnifaces.eventlistener.ResetInputAjaxActionListener" /&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 * </ul>
 * <p>
 * This works with standard JSF, PrimeFaces and RichFaces actions. Only for RichFaces there's a reflection hack,
 * because its <code>ExtendedPartialViewContextImpl</code> <i>always</i> returns an empty collection for render IDs.
 * See also <a href="https://issues.jboss.org/browse/RF-11112">RF issue 11112</a>.
 * <p>
 * Design notice: being a phase listener was mandatory in order to be able to hook on every single ajax action as
 * standard JSF API does not (seem to?) offer any ways to register some kind of {@link AjaxBehaviorListener} in an
 * application wide basis, let alone on a per <code>&lt;f:ajax&gt;</code> tag basis, so that it also get applied to
 * ajax actions in <code>UIInput</code> components. There are ways with help of {@link SystemEventListener}, but it
 * ended up to be too clumsy.
 *
 * @author Bauke Scholtz
 * @link http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1060
 */
@SuppressWarnings("unchecked") // For the cast on Collection<String> in getRenderIds().
public class ResetInputAjaxActionListener extends DefaultPhaseListener implements ActionListener {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = -5317382021715077662L;

	private static final Set<VisitHint> VISIT_HINTS = EnumSet.of(VisitHint.SKIP_TRANSIENT);
	private static final String ERROR_RF_PVC_HACK =
		"Cannot obtain componentRenderIds property of RichFaces ExtendedPartialViewContextImpl instance '%s'.";

	// Variables ------------------------------------------------------------------------------------------------------

	private ActionListener wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new reset input ajax action listener. This constructor will be used when specifying the action
	 * listener by <code>&lt;f:actionListener&gt;</code> or when registering as <code>&lt;phase-listener&gt;</code> in
	 * <tt>faces-config.xml</tt>.
	 */
	public ResetInputAjaxActionListener() {
		this(null);
	}

	/**
	 * Construct a new reset input ajax action listener around the given wrapped action listener. This constructor
	 * will be used when registering as <code>&lt;action-listener&gt;</code> in <tt>faces-config.xml</tt>.
	 * @param wrapped The wrapped action listener.
	 */
	public ResetInputAjaxActionListener(ActionListener wrapped) {
		super(PhaseId.INVOKE_APPLICATION);
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Delegate to the {@link #processAction(ActionEvent)} method when this action listener is been registered as a
	 * phase listener so that it get applied on <strong>all</strong> ajax requests.
	 * @see #processAction(ActionEvent)
	 */
	@Override
	public void beforePhase(PhaseEvent event) {
		processAction(null);
	}

	/**
	 * <p>Handle the reset input action as follows, only and only if the current request is an ajax request and the
	 * {@link PartialViewContext#getRenderIds()} does not return an empty collection nor is the same as
	 * {@link PartialViewContext#getExecuteIds()}:
	 * <ul>
	 * <li>Collect all {@link EditableValueHolder} components based on {@link PartialViewContext#getRenderIds()}.
	 * <li>Remove all components covered by {@link PartialViewContext#getExecuteIds()} from this collection.
	 * <li>Invoke {@link EditableValueHolder#resetValue()} on the remaining components of this collection.
	 * </ul>
	 * @throws IllegalArgumentException When one of the client IDs resolved to a <code>null</code> component. This
	 * would however indicate a bug in the concrete {@link PartialViewContext} implementation which is been used.
	 */
	@Override
	public void processAction(ActionEvent event) throws AbortProcessingException {
		FacesContext context = FacesContext.getCurrentInstance();
		PartialViewContext partialViewContext = context.getPartialViewContext();

		if (partialViewContext.isAjaxRequest()) {
			Collection<String> renderIds = getRenderIds(partialViewContext);
			Collection<String> executeIds = partialViewContext.getExecuteIds();

			if (!renderIds.isEmpty() && !renderIds.containsAll(executeIds)) {
				final Set<EditableValueHolder> inputs = new HashSet<EditableValueHolder>();

				// First find all to be rendered inputs in the current view and add them to the set.
				findAndAddEditableValueHolders(VisitContext.createVisitContext(
					context, renderIds, VISIT_HINTS), context.getViewRoot(), inputs);

				// Then find all executed inputs in the current form and remove them from the set.
				findAndRemoveEditableValueHolders(VisitContext.createVisitContext(
					context, executeIds, VISIT_HINTS), Components.getCurrentForm(), inputs);

				// The set now contains inputs which are to be rendered, but which are not been executed. Reset them.
				for (EditableValueHolder input : inputs) {
					input.resetValue();
				}
			}
		}

		if (wrapped != null && event != null) {
			wrapped.processAction(event);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Helper method with RichFaces4 hack to return the proper render IDs from the given partial view context.
	 * @param partialViewContext The partial view context to return the render IDs for.
	 * @return The render IDs.
	 */
	private static Collection<String> getRenderIds(PartialViewContext partialViewContext) {
		Collection<String> renderIds = partialViewContext.getRenderIds();

		// WARNING: START OF HACK! ------------------------------------------------------------------------------------
		// HACK for RichFaces4 because its ExtendedPartialViewContextImpl class doesn't return its componentRenderIds
		// property on getRenderIds() call when the action is executed using a RichFaces-specific command button/link.
		// See also https://issues.jboss.org/browse/RF-11112
		if (renderIds.isEmpty()
			&& partialViewContext.getClass().getName().equals("org.richfaces.context.ExtendedPartialViewContextImpl"))
		{
			try {
				Field componentRenderIds = partialViewContext.getClass().getDeclaredField("componentRenderIds");
				componentRenderIds.setAccessible(true);
				renderIds = (Collection<String>) componentRenderIds.get(partialViewContext);

				if (renderIds == null) {
					renderIds = Collections.emptyList();
				}
			}
			catch (Exception e) {
				throw new FacesException(String.format(ERROR_RF_PVC_HACK, partialViewContext), e);
			}
		}
		// END OF HACK ------------------------------------------------------------------------------------------------

		return renderIds;
	}

	/**
	 * Find all editable value holder components in the component hierarchy, starting with the given component and
	 * add them to the given set.
	 * @param context The visit context to work with.
	 * @param component The starting point of the component hierarchy to look for editable value holder components.
	 * @param inputs The set to add the found editable value holder components to.
	 */
	private static void findAndAddEditableValueHolders
		(VisitContext context, final UIComponent component, final Set<EditableValueHolder> inputs)
	{
		component.visitTree(context, new VisitCallback() {
			@Override
			public VisitResult visit(VisitContext context, UIComponent target) {
				if (target instanceof EditableValueHolder) {
					inputs.add((EditableValueHolder) target);
				}
				else if (context.getIdsToVisit() != VisitContext.ALL_IDS) {
					// Render ID didn't point an EditableValueHolder. Visit all children as well.
					findAndAddEditableValueHolders(VisitContext.createVisitContext(
						context.getFacesContext(), null, context.getHints()), target, inputs);
				}

				return VisitResult.ACCEPT;
			}
		});
	}

	/**
	 * Find all editable value holder components in the component hierarchy, starting with the given component and
	 * remove them from the given set.
	 * @param context The visit context to work with.
	 * @param component The starting point of the component hierarchy to look for editable value holder components.
	 * @param inputs The set to remove the found editable value holder components from.
	 */
	private static void findAndRemoveEditableValueHolders
		(VisitContext context, final UIComponent component, final Set<EditableValueHolder> inputs)
	{
		component.visitTree(context, new VisitCallback() {
			@Override
			public VisitResult visit(VisitContext context, UIComponent target) {
				if (target instanceof EditableValueHolder) {
					inputs.remove(target);
				}
				else if (context.getIdsToVisit() != VisitContext.ALL_IDS) {
					// Execute ID didn't point an EditableValueHolder. Visit all children as well.
					findAndRemoveEditableValueHolders(VisitContext.createVisitContext(
						context.getFacesContext(), null, context.getHints()), target, inputs);
				}

				return VisitResult.ACCEPT;
			}
		});
	}

}