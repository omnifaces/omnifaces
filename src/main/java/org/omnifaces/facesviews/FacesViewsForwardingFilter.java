/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.facesviews;

import static javax.faces.application.ProjectStage.Development;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_PATH_INFO;
import static org.omnifaces.facesviews.FacesViews.FACES_VIEWS_ORIGINAL_SERVLET_PATH;
import static org.omnifaces.facesviews.FacesViews.getExtensionAction;
import static org.omnifaces.facesviews.FacesViews.getExtensionlessURLWithQuery;
import static org.omnifaces.facesviews.FacesViews.getMappedResources;
import static org.omnifaces.facesviews.FacesViews.getMultiViewsWelcomeFile;
import static org.omnifaces.facesviews.FacesViews.getPathAction;
import static org.omnifaces.facesviews.FacesViews.getReverseMappedResources;
import static org.omnifaces.facesviews.FacesViews.isMultiViewsEnabled;
import static org.omnifaces.facesviews.FacesViews.isResourceInPublicPath;
import static org.omnifaces.facesviews.FacesViews.scanAndStoreViews;
import static org.omnifaces.facesviews.FacesViews.stripWelcomeFilePrefix;
import static org.omnifaces.util.Faces.getApplicationFromFactory;
import static org.omnifaces.util.ResourcePaths.getExtension;
import static org.omnifaces.util.ResourcePaths.isExtensionless;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;
import static org.omnifaces.util.Servlets.getRequestRelativeURI;
import static org.omnifaces.util.Servlets.redirectPermanent;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.omnifaces.filter.HttpFilter;

/**
 * This filter makes sure extensionless requests arrive at the FacesServlet using an extension on which that Servlet is
 * mapped, and that non-extensionless requests are handled according to a set preference.
 * <p>
 * A filter like this is needed for extensionless requests, since the FacesServlet does not take into account any other
 * mapping than prefix- and extension (suffix) mapping.
 * <p>
 * For a guide on FacesViews, please see the <a href="package-summary.html">package summary</a>.
 *
 * @author Arjan Tijms
 * @see FacesViews
 * @see ExtensionAction
 * @see PathAction
 * @see UriExtensionRequestWrapper
 */
public class FacesViewsForwardingFilter extends HttpFilter {

	private ExtensionAction extensionAction;
	private PathAction pathAction;

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();

		try {
			extensionAction = getExtensionAction(servletContext);
			pathAction = getPathAction(servletContext);
		}
		catch (IllegalStateException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException {
		if (!(filterExtensionLess(request, response, chain) || filterExtension(request, response) || filterPublicPath(request, response))) {
			chain.doFilter(request, response);
		}
	}

	/**
	 * A mapped resource request without extension is encountered.
	 * The user setting "dispatchMethod" determines how we handle this.
	 */
	private boolean filterExtensionLess(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		String servletPath = request.getServletPath();

		if (!isExtensionless(servletPath)) {
			return false;
		}

		ServletContext servletContext = getServletContext();
		boolean multiViews = isMultiViewsEnabled(request);
		Map<String, String> resources = getMappedResources(servletContext);
		String normalizedServletPath = stripTrailingSlash(servletPath);
		String resource = normalizedServletPath + (multiViews ? "/*" : "");

		if (getApplicationFromFactory().getProjectStage() == Development && !resources.containsKey(resource)) {
			// Check if the resource was dynamically added by scanning the faces-views location(s) again.
			resources = scanAndStoreViews(servletContext, false);
		}

		if (multiViews && !resources.containsKey(resource)) {
			resource = getMultiViewsWelcomeFile(servletContext);

			if (resource != null) {
				if (request.getPathInfo() != null) {
					servletPath += request.getPathInfo();
				}

				request.setAttribute(FACES_VIEWS_ORIGINAL_PATH_INFO, servletPath);
				request.getRequestDispatcher(resource).forward(request, response);
				return true;
			}
		}

		return filterExtensionLess(request, response, chain, resources, resource, normalizedServletPath);
	}

	private boolean filterExtensionLess(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Map<String, String> resources, String resource, String path) throws IOException, ServletException
	{
		if (resources.containsKey(resource)) {
			if (redirectExtensionLessWelcomeFileToFolderIfNecessary(request, response, path)) {
				return true;
			}

			String servletPathWithExtension = path + getExtension(resources.get(resource));

			if (resources.containsKey(servletPathWithExtension)) {
				filterExtensionLessToExtension(request, response, chain, servletPathWithExtension);
				return true;
			}
			else if (forwardExtensionLessToExtensionIfNecessary(request, response, servletPathWithExtension)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if a welcome file was explicitly requested and if so, redirect back to its parent folder.
	 */
	private boolean redirectExtensionLessWelcomeFileToFolderIfNecessary(HttpServletRequest request, HttpServletResponse response, String normalizedServletPath) {
		if ((getRequestRelativeURI(request) + "/").startsWith(normalizedServletPath + "/")) {
			String servletPath = request.getServletPath();
			String normalizedResource = stripWelcomeFilePrefix(request.getServletContext(), servletPath);

			if (!servletPath.equals(normalizedResource)) {
				String uri = request.getContextPath() + normalizedResource;
				String queryString = request.getQueryString();
				redirectPermanent(response, uri + ((queryString != null) ? "?" + queryString : ""));
				return true;
			}
		}

		return false;
	}

	/**
	 * Continue the chain, but make the request appear to be to the resource with an extension.
	 * This assumes that the FacesServlet has been mapped to something that includes the extensionless request.
	 */
	private void filterExtensionLessToExtension(HttpServletRequest request, HttpServletResponse response, FilterChain chain, String mappedServletPath) throws IOException, ServletException {
		try {
			request.setAttribute(FACES_VIEWS_ORIGINAL_SERVLET_PATH, request.getServletPath());
			String pathInfo = request.getPathInfo();

			if (pathInfo != null) {
				request.setAttribute(FACES_VIEWS_ORIGINAL_PATH_INFO, pathInfo);
			}

			chain.doFilter(new UriExtensionRequestWrapper(request, mappedServletPath), response);
		}
		finally {
			request.removeAttribute(FACES_VIEWS_ORIGINAL_SERVLET_PATH);
			request.removeAttribute(FACES_VIEWS_ORIGINAL_PATH_INFO);
		}
	}

	/**
	 * Forward the resource (view) using its original extension, on which the Facelets Servlet is mapped.
	 * Technically it matters most that the Facelets Servlet picks up the request,
	 * and the exact extension or even prefix is perhaps less relevant.
	 */
	private boolean forwardExtensionLessToExtensionIfNecessary(HttpServletRequest request, HttpServletResponse response, String servletPathWithExtension) throws ServletException, IOException {
		RequestDispatcher requestDispatcher = request.getServletContext().getRequestDispatcher(servletPathWithExtension);

		if (requestDispatcher != null) {
			requestDispatcher.forward(request, response);
			return true;
		}

		return false;
	}

	/**
	 * A mapped resource request with extension is encountered.
	 * The user setting "extensionAction" determines how we handle this.
	 */
	private boolean filterExtension(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String resource = request.getServletPath();
		Map<String, String> resources = getMappedResources(getServletContext());

		if (resources.containsKey(resource)) {
			switch (extensionAction) {
				case REDIRECT_TO_EXTENSIONLESS:
					redirectPermanent(response, getExtensionlessURLWithQuery(request, resource));
					return true;
				case SEND_404:
					response.sendError(SC_NOT_FOUND);
					return true;
				case PROCEED:
					break;
			}
		}

		return false;
	}

	/**
	 * A direct request to one of the public paths (excluding /) from where we scanned resources is encountered.
	 * The user setting "pathAction" determines how we handle this.
	 */
	private boolean filterPublicPath(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String resource = request.getServletPath();

		if (!isResourceInPublicPath(getServletContext(), resource)) {
			return false;
		}

		Map<String, String> reverseResources = getReverseMappedResources(getServletContext());

		if (reverseResources.containsKey(resource)) {
			switch (pathAction) {
				case REDIRECT_TO_SCANNED_EXTENSIONLESS:
					redirectPermanent(response, getExtensionlessURLWithQuery(request, reverseResources.get(resource)));
					return true;
				case SEND_404:
					response.sendError(SC_NOT_FOUND);
					return true;
				case PROCEED:
					break;
			}
		}

		return false;
	}

}