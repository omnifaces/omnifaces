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
package org.omnifaces.component.script;

import static java.lang.String.format;
import static org.omnifaces.config.OmniFaces.OMNIFACES_LIBRARY_NAME;
import static org.omnifaces.config.OmniFaces.OMNIFACES_SCRIPT_NAME;
import static org.omnifaces.util.FacesLocal.getRequestContextPath;
import static org.omnifaces.util.FacesLocal.getServletContext;
import static org.omnifaces.util.Utils.escapeJS;

import java.io.IOException;

import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;
import jakarta.servlet.ServletContext;

import org.omnifaces.cdi.ScriptError;
import org.omnifaces.config.OmniFaces;
import org.omnifaces.servlet.ScriptErrorServlet;
import org.omnifaces.util.Beans;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:scriptErrorHandler&gt;</code> is a component that catches uncaught JavaScript errors and unhandled
 * promise rejections on the client and sends them to the server via <code>navigator.sendBeacon()</code>, where they are
 * fired as {@link ScriptError} CDI events. This allows server-side logging of client-side JavaScript errors without
 * any additional endpoint boilerplate.
 *
 *
 * <h2 id="usage-client"><a href="#usage-client">Usage (client)</a></h2>
 * <p>
 * Just put the component in the <code>&lt;h:head&gt;</code> of the master template. It will automatically move itself to
 * <code>head</code> even when placed elsewhere, but placing it in <code>&lt;h:head&gt;</code> makes the intent explicit.
 * <pre>
 * &lt;h:head&gt;
 *     &lt;o:scriptErrorHandler /&gt;
 * &lt;/h:head&gt;
 * </pre>
 * <p>
 * This will register <code>window.onerror</code> and <code>unhandledrejection</code> event listeners which report
 * any uncaught client-side errors to the server. You can optionally specify a CSS selector via
 * <code>ignoreSelector</code> to suppress error reporting when a matching element exists in the document (e.g. on
 * error pages where errors are expected).
 * <pre>
 * &lt;o:scriptErrorHandler ignoreSelector="body.errorPage" /&gt;
 * </pre>
 *
 *
 * <h2 id="usage-server"><a href="#usage-server">Usage (server)</a></h2>
 * <p>
 * Create a CDI bean (typically application scoped) that observes the {@link ScriptError} event.
 * <pre>
 * &#64;ApplicationScoped
 * public class ScriptErrorObserver {
 *
 *     private static final Logger logger = Logger.getLogger(ScriptErrorObserver.class.getName());
 *
 *     public void onScriptError(&#64;Observes ScriptError error) {
 *         logger.warning(error.toString());
 *     }
 * }
 * </pre>
 * <p>
 * The {@link ScriptError} event provides the following information:
 * <ul>
 * <li>{@link ScriptError#pageURL()}: the URL of the page where the error occurred.</li>
 * <li>{@link ScriptError#errorMessage()}: the error message.</li>
 * <li>{@link ScriptError#sourceURL()}: the URL of the script file where the error occurred.</li>
 * <li>{@link ScriptError#lineNumber()}: the line number in the script.</li>
 * <li>{@link ScriptError#columnNumber()}: the column number in the script.</li>
 * <li>{@link ScriptError#errorName()}: the error type (e.g. "TypeError", "ReferenceError").</li>
 * <li>{@link ScriptError#errorStack()}: the full stack trace.</li>
 * <li>{@link ScriptError#remoteAddr()}: the remote address.</li>
 * <li>{@link ScriptError#userAgent()}: the User-Agent header.</li>
 * <li>{@link ScriptError#userPrincipal()}: the authenticated user principal name.</li>
 * </ul>
 *
 *
 * <h2 id="deduplication"><a href="#deduplication">Deduplication</a></h2>
 * <p>
 * To prevent flooding the server with repeated errors (e.g. from a <code>requestAnimationFrame</code> loop), the
 * client-side script deduplicates errors by message, source URL, and line number. The deduplication queue size and
 * expiry time can be configured via the <code>maxRecentErrors</code> and <code>errorExpiry</code> attributes.
 *
 * @author Bauke Scholtz
 * @see ScriptError
 * @see ScriptErrorServlet
 * @since 5.2
 */
@FacesComponent(value = ScriptErrorHandler.COMPONENT_TYPE, namespace = OmniFaces.OMNIFACES_NAMESPACE)
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
@ResourceDependency(library = OMNIFACES_LIBRARY_NAME, name = OMNIFACES_SCRIPT_NAME, target = "head")
public class ScriptErrorHandler extends ScriptFamily {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.script.ScriptErrorHandler#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.script.ScriptErrorHandler";

    // Private constants ----------------------------------------------------------------------------------------------

    private static final int DEFAULT_MAX_RECENT_ERRORS = 100;
    private static final int DEFAULT_ERROR_EXPIRY = 60000;

    private static final String SCRIPT_INIT = "OmniFaces.ScriptErrorHandler.init('%s',%s,%s,%s);";

    private static final String ERROR_SERVLET_NOT_REGISTERED =
        "ScriptErrorHandler requires a CDI observer for ScriptError, but none was found."
        + " Register a bean with a @Observes ScriptError method. See also the javadoc of <o:scriptErrorHandler>.";

    private enum PropertyKeys {
        // Cannot be uppercased. They have to exactly match the attribute names.
        ignoreSelector, maxRecentErrors, errorExpiry;
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private static boolean servletRegistered;

    private final State state = new State(getStateHelper());

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Move this component to head, so that error handlers are registered as early as possible.
     * @throws IllegalArgumentException When the {@link ScriptErrorServlet} is not registered because no CDI observer
     * for {@link ScriptError} was found.
     */
    @Override
    public void processEvent(ComponentSystemEvent event) {
        if (event instanceof PostAddToViewEvent) {
            var context = event.getFacesContext();
            validateServletRegistered(context);
            context.getViewRoot().addComponentResource(context, this, "head");
        }
    }

    /**
     * Render the initialization script.
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        var endpointURL = getRequestContextPath(context) + ScriptErrorServlet.URI;
        var ignoreSelector = getIgnoreSelector();
        var maxRecentErrors = getMaxRecentErrors();
        var errorExpiry = getErrorExpiry();

        var script = format(SCRIPT_INIT,
            endpointURL,
            ignoreSelector != null ? "'" + escapeJS(ignoreSelector, true) + "'" : "null",
            maxRecentErrors,
            errorExpiry);

        context.getResponseWriter().write(script);
    }

    // Attribute getters/setters --------------------------------------------------------------------------------------

    /**
     * Returns the CSS selector for elements whose presence suppresses error reporting.
     * When an element matching this selector exists in the document, errors will not be sent to the server.
     * For example, <code>body.errorPage</code> to suppress reporting on error pages having <code>&lt;h:body styleClass="errorPage"&gt;</code>.
     * @return The CSS selector, or <code>null</code>.
     */
    public String getIgnoreSelector() {
        return state.get(PropertyKeys.ignoreSelector);
    }

    /**
     * Sets the CSS selector for elements whose presence suppresses error reporting.
     * When an element matching this selector exists in the document, errors will not be sent to the server.
     * @param ignoreSelector The CSS selector.
     */
    public void setIgnoreSelector(String ignoreSelector) {
        state.put(PropertyKeys.ignoreSelector, ignoreSelector);
    }

    /**
     * Returns the maximum number of recent errors to keep for deduplication. Defaults to {@value #DEFAULT_MAX_RECENT_ERRORS}.
     * @return The maximum number of recent errors.
     */
    public int getMaxRecentErrors() {
        return state.get(PropertyKeys.maxRecentErrors, DEFAULT_MAX_RECENT_ERRORS);
    }

    /**
     * Sets the maximum number of recent errors to keep for deduplication.
     * Defaults to {@value #DEFAULT_MAX_RECENT_ERRORS}.
     * @param maxRecentErrors The maximum number of recent errors.
     */
    public void setMaxRecentErrors(int maxRecentErrors) {
        state.put(PropertyKeys.maxRecentErrors, maxRecentErrors);
    }

    /**
     * Returns the time in milliseconds after which a recent error entry expires. Defaults to {@value #DEFAULT_ERROR_EXPIRY} (1 minute).
     * @return The error expiry time in milliseconds.
     */
    public int getErrorExpiry() {
        return state.get(PropertyKeys.errorExpiry, DEFAULT_ERROR_EXPIRY);
    }

    /**
     * Sets the time in milliseconds after which a recent error entry expires for deduplication.
     * Defaults to {@value #DEFAULT_ERROR_EXPIRY} (1 minute).
     * @param errorExpiry The error expiry time in milliseconds.
     */
    public void setErrorExpiry(int errorExpiry) {
        state.put(PropertyKeys.errorExpiry, errorExpiry);
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static void validateServletRegistered(FacesContext context) {
        if (!servletRegistered) {
            if (!getServletContext(context).getServletRegistrations().containsKey(ScriptErrorServlet.class.getName())) {
                throw new IllegalArgumentException(ERROR_SERVLET_NOT_REGISTERED);
            }

            servletRegistered = true;
        }
    }

    /**
     * Register the script error handler servlet if it has not already been registered and there is at least one CDI
     * observer for {@link ScriptError}.
     * @param servletContext The involved servlet context.
     */
    public static void registerServletIfNecessary(ServletContext servletContext) {
        if (servletContext.getServletRegistrations().containsKey(ScriptErrorServlet.class.getName())) {
            return;
        }

        if (Beans.getManager().resolveObserverMethods(new ScriptError(null, null, null, null, null, null, null, null, null, null)).isEmpty()) {
            return;
        }

        var registration = servletContext.addServlet(ScriptErrorServlet.class.getName(), ScriptErrorServlet.class);

        if (registration != null) {
            registration.addMapping(ScriptErrorServlet.URI);
        }
    }

}
