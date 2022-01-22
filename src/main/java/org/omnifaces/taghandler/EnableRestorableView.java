/*
 * Copyright OmniFaces
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
package org.omnifaces.taghandler;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.omnifaces.util.Faces.setApplicationAttribute;
import static org.omnifaces.util.FacesLocal.getApplicationAttribute;

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

import org.omnifaces.viewhandler.OmniViewHandler;

/**
 * <p>
 * The <code>&lt;o:enableRestorableView&gt;</code> taghandler instructs the view handler to recreate the entire view
 * whenever the view has been expired, i.e. whenever {@link ViewHandler#restoreView(FacesContext, String)} returns
 * <code>null</code> and the current request is a postback. This effectively prevents {@link ViewExpiredException} on
 * the view. This tag needs to be placed in <code>&lt;f:metadata&gt;</code> of the view.
 * <p>
 * There are however technical design limitations: the recreated view is <b>exactly</b> the same as during the initial
 * request. In other words, the view has lost its state. Any modifications which were made after the original initial
 * request, either by taghandlers or (ajax) conditionally rendered components based on some view or even session
 * scoped variables, are completely lost. Thus, the view should be designed that way that it can be used with a request
 * scoped bean. You <em>can</em> use it with a view scoped bean, but then you should add a <code>@PostConstruct</code>
 * which checks if the request is a postback and then fill the missing bean properties based on request parameters.
 *
 * <h3>Usage</h3>
 * <p>
 * To enable the restorable view, just add the <code>&lt;enableRestorableView&gt;</code> to the view metadata.
 * <pre>
 * &lt;f:metadata&gt;
 *     &lt;o:enableRestorableView/&gt;
 * &lt;/f:metadata&gt;
 * </pre>
 *
 * <h3>Mojarra's new stateless mode</h3>
 * <p>
 * Since Mojarra 2.1.19, about 2 months after OmniFaces introduced the <code>&lt;o:enableRestorableView&gt;</code>,
 * it's possible to enable a stateless mode on the view by simply setting its <code>transient</code> attribute to
 * <code>true</code>:
 * <pre>
 * &lt;f:view transient="true"&gt;
 *     ...
 * &lt;/f:view&gt;
 * </pre>
 * <p>
 * This goes actually a step further than <code>&lt;o:enableRestorableView&gt;</code> as no state would be saved at all.
 * However, on those kind of pages where <code>&lt;o:enableRestorableView&gt;</code> would work just fine, this
 * statelessness should not form any problem at all. So, if you have at least Mojarra 2.1.19 at hands, use the
 * <code>transient="true"</code> instead.
 *
 * @author Bauke Scholtz
 * @since 1.3
 * @see OmniViewHandler
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
		setApplicationAttribute(EnableRestorableView.class.getName(), TRUE);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Enable the current view to be restorable. This basically sets a specific view attribute which the
	 * {@link OmniViewHandler} could intercept on.
	 * @throws IllegalStateException When given parent is not an instance of {@link UIViewRoot}.
	 */
	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		if (!(parent instanceof UIViewRoot)) {
			throw new IllegalStateException(
				format(ERROR_INVALID_PARENT, parent != null ? parent.getClass().getName() : null));
		}

		if (!ComponentHandler.isNew(parent)) {
			return;
		}

		parent.getAttributes().put(EnableRestorableView.class.getName(), TRUE);
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns true if given view is null, and this is a postback, and {@link EnableRestorableView} has been activated.
	 * @param context The involved faces context.
	 * @param view The involved view.
	 * @return true if given view is null, and this is a postback, and {@link EnableRestorableView} has been activated.
	 */
	public static boolean isRestorableViewRequest(FacesContext context, UIViewRoot view) {
		return view == null
			&& context.isPostback()
			&& TRUE.equals(getApplicationAttribute(context, EnableRestorableView.class.getName()));
	}

	/**
	 * Returns true if given view indeed contains {@link EnableRestorableView}.
	 * @param view The involved view.
	 * @return true if given view indeed contains {@link EnableRestorableView}.
	 */
	public static boolean isRestorableView(UIViewRoot view) {
		return TRUE.equals(view.getAttributes().get(EnableRestorableView.class.getName()));
	}

}