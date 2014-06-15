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
package org.omnifaces.taghandler;

import java.io.IOException;

import javax.faces.application.ViewExpiredException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.viewhandler.RestorableViewHandler;

/**
 * <p>
 * The <code>&lt;o:enableRestorableView&gt;</code> instructs the view handler to recreate the entire view whenever the
 * view has been expired, i.e. whenever {@link ViewHandler#restoreView(FacesContext, String)} returns <code>null</code>
 * and the current request is a postback. This effectively prevents {@link ViewExpiredException} on the view. This tag
 * needs to be placed in <code>&lt;f:metadata&gt;</code> of the view.
 * <p>
 * There are however technical design limitations: the recreated view is exactly the same as during the initial request,
 * so any modifications which are made thereafter, either by taghandlers or conditionally rendered components based on
 * some view or even session scoped variables, are completely lost. In order to recreate exactly the desired view, you
 * would need to make sure that those modifications are made based on request scoped variables (read: request
 * parameters) instead of view or session scoped variables. In other words, the state of the restorable view should not
 * depend on view or session scoped managed beans, but purely on request scoped managed beans.
 * <p>
 * To enable the restorable view, just add the <code>&lt;enableRestorableView&gt;</code> to the view metadata.
 * <pre>
 * &lt;f:metadata&gt;
 *   &lt;o:enableRestorableView/&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see RestorableViewHandler
 */
public class EnableRestorableView extends TagHandler {

    // Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_PARENT =
		"EnableRestorableView must be a child of UIViewRoot. Encountered parent of type '%s'."
			+ " It is recommended to enclose o:enableRestorableView in f:metadata.";

    // Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public EnableRestorableView(TagConfig config) {
		super(config);
	}

    // Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Enable the current view to be restorable. This basically sets a specific view attribute which the
	 * {@link RestorableViewHandler} could intercept on.
	 * @throws IllegalArgumentException When given parent is not an instance of {@link UIViewRoot}.
	 */
	@Override
	public void apply(FaceletContext context, final UIComponent parent) throws IOException {
		if (!(parent instanceof UIViewRoot)) {
			throw new IllegalArgumentException(
				String.format(ERROR_INVALID_PARENT, parent != null ? parent.getClass().getName() : null));
		}

		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		parent.getAttributes().put(EnableRestorableView.class.getName(), true);
	}

}