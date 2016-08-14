package org.omnifaces.facesviews;

/**
 * The mode of the view handler with respect to constructing an action URL.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @since 1.5
 * @see FacesViews
 * @see FacesViewsViewHandler
 */
public enum ViewHandlerMode {

	/**
	 * Takes the outcome from the parent view handler and strips the extension from it.
	 * <p>
	 * This is the default value.
	 */
	STRIP_EXTENSION_FROM_PARENT,

	/**
	 * The {@link FacesViewsViewHandler} constructs the action URL itself, but takes the query parameters (if any)
	 * from the outcome of the parent view handler.
	 */
	BUILD_WITH_PARENT_QUERY_PARAMETERS

}