/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.cdi;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.util.Nonbinding;
import jakarta.faces.component.UIViewRoot;

import org.omnifaces.cdi.viewscope.ViewScopeContext;
import org.omnifaces.cdi.viewscope.ViewScopeEventListener;
import org.omnifaces.cdi.viewscope.ViewScopeExtension;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.cdi.viewscope.ViewScopeStorage;
import org.omnifaces.cdi.viewscope.ViewScopeStorageInSession;
import org.omnifaces.cdi.viewscope.ViewScopeStorageInViewState;
import org.omnifaces.context.OmniExternalContext;
import org.omnifaces.context.OmniExternalContextFactory;
import org.omnifaces.viewhandler.OmniViewHandler;

/**
 * <p>
 * The CDI view scope annotation, with more optimal handling of bean destroy as compared to standard JSF one.
 * <p>
 * In standard JSF 2.0/2.1, the <code>&#64;</code>{@link PreDestroy} annotated method on a view scoped bean was never
 * invoked when the session expires. Since OmniFaces 1.6, this CDI view scope annotation will guarantee that the
 * <code>&#64;PreDestroy</code> annotated method is also invoked on session expire. Since JSF 2.2, this problem is
 * solved on native JSF view scoped beans, hereby making this annotation superflous in JSF 2.2.
 * <p>
 * However, there may be cases when it's desirable to immediately destroy a view scoped bean as well when the browser
 * <code>unload</code> event is invoked. I.e. when the user navigates away by GET, or closes the browser tab/window.
 * None of the both JSF 2.2 view scope annotations support this. Since OmniFaces 2.2, this CDI view scope annotation
 * will guarantee that the <code>&#64;PreDestroy</code> annotated method is also invoked on browser unload. This trick
 * is done by <code>navigator.sendBeacon</code> via an automatically included helper script <code>omnifaces:unload.js</code>.
 * For browsers not supporting <code>navigator.sendBeacon</code>, it will fallback to a synchronous XHR request.
 * <p>
 * Since OmniFaces 2.3, the unload has been further improved to also physically remove the associated JSF view state
 * from JSF implementation's internal LRU map in case of server side state saving, hereby further decreasing the risk
 * at <code>ViewExpiredException</code> on the other views which were created/opened earlier. As side effect of this
 * change, the <code>&#64;PreDestroy</code> annotated method of any standard JSF view scoped beans referenced in the
 * same view as the OmniFaces CDI view scoped bean will also guaranteed be invoked on browser unload.
 * <p>
 * Since OmniFaces 2.6, this annotation got a new attribute: <code>saveInViewState</code>. When using client side state
 * saving, this attribute can be set to <code>true</code> in order to force JSF to store whole view scoped bean
 * instances annotated with this annotation in the JSF view state instead of in the HTTP session. For more detail, see
 * the {@link #saveInViewState()}.
 * <p>
 * In a nutshell: if you're on JSF 2.0/2.1, and you can't upgrade to JSF 2.2, and you want the
 * <code>&#64;PreDestroy</code> to be invoked on sesison expire too, then use OmniFaces 1.6+ with this view scope
 * annotation. Or, if you're on JSF 2.2 already, and you want the <code>&#64;PreDestroy</code> to be invoked on browser
 * unload too, then use OmniFaces 2.2+ with this view scope annotation. Or, if you want to store whole view scoped beans
 * in the JSF view state when using client side state saving, then use OmniFaces 2.6+ with this view scope annotation
 * and the <code>saveInViewState</code> attribute set to <code>true</code>.
 * <p>
 * Related JSF issues:
 * <ul>
 * <li><a href="https://github.com/eclipse-ee4j/mojarra/issues/1355">Mojarra issue 1355</a>
 * <li><a href="https://github.com/eclipse-ee4j/mojarra/issues/1843">Mojarra issue 1843</a>
 * <li><a href="https://github.com/javaee/javaserverfaces-spec/issues/905">JSF spec issue 905</a>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Just use it the usual way as all other CDI scopes. Watch out with IDE autocomplete on import that you don't
 * accidentally import standard JSF's own one.
 * <pre>
 * import jakarta.inject.Named;
 * import org.omnifaces.cdi.ViewScoped;
 *
 * &#64;Named
 * &#64;ViewScoped
 * public class OmniCDIViewScopedBean implements Serializable {}
 * </pre>
 * <p>
 * Please note that the bean <strong>must</strong> implement {@link Serializable}, otherwise the CDI implementation
 * will throw an exception about the bean not being passivation capable.
 * <p>
 * Under the covers, CDI managed beans with this scope are via {@link ViewScopeManager} by default stored in the session
 * scope by an {@link UUID} based key which is referenced in JSF's own view map as available by
 * {@link UIViewRoot#getViewMap()}. They are not stored in the JSF view state itself as that would be rather expensive
 * in case of client side state saving.
 * <p>
 * In case you are using client side state saving by having the <code>jakarta.faces.STATE_SAVING_METHOD</code> context
 * parameter set to <code>true</code> along with a valid <code>jsf/ClientSideSecretKey</code> in <code>web.xml</code>
 * as below,
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;jakarta.faces.STATE_SAVING_METHOD&lt;/param-name&gt;
 *     &lt;param-value&gt;client&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;env-entry&gt;
 *     &lt;env-entry-name&gt;jsf/ClientSideSecretKey&lt;/env-entry-name&gt;
 *     &lt;env-entry-type&gt;java.lang.String&lt;/env-entry-type&gt;
 *     &lt;env-entry-value&gt;&lt;!-- See https://stackoverflow.com/q/35102645/157882 --&gt;&lt;/env-entry-value&gt;
 * &lt;/env-entry&gt;
 * </pre>
 * <p>
 * And you explicitly want to store the whole view scoped bean instance in the JSF view state, then set the annotation's
 * <code>saveInViewState</code> attribute to <code>true</code>.
 * <pre>
 * import jakarta.inject.Named;
 * import org.omnifaces.cdi.ViewScoped;
 *
 * &#64;Named
 * &#64;ViewScoped(saveInViewState=true)
 * public class OmniCDIViewScopedBean implements Serializable {}
 * </pre>
 * <p>
 * It's very important that you understand that this setting has potentially a major impact in the size of the JSF view
 * state, certainly when the view scoped bean instance holds "too much" data, such as a collection of entities for a
 * data table, and that such beans will in fact <strong>never</strong> expire as they are stored entirely in the
 * <code>jakarta.faces.ViewState</code> hidden input field in the HTML page. Moreover, the
 * <code>&#64;</code>{@link PreDestroy} annotated method on such bean will explicitly <strong>never</strong> be invoked,
 * even not on an unload as it's quite possible to save or cache the page source and re-execute it at a (much) later
 * moment.
 * <p>
 * It's therefore strongly recommended to use this setting only on a view scoped bean instance which is exclusively used
 * to keep track of the dynamically controlled form state, such as <code>disabled</code>, <code>readonly</code> and
 * <code>rendered</code> attributes which are controlled by ajax events.
 * <p>
 * This setting is NOT recommended when using server side state saving. It has basically no effect and it only adds
 * unnecessary serialization overhead. The system will therefore throw an {@link IllegalStateException} on such
 * condition.
 *
 * <h2>Configuration</h2>
 * <p>
 * By default, the maximum number of active view scopes is hold in a LRU map in HTTP session with a default size equal
 * to the first non-null value of the following context parameters:
 * <ul>
 * <li>{@value org.omnifaces.cdi.viewscope.ViewScopeManager#PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES} (OmniFaces)</li>
 * <li>{@value org.omnifaces.cdi.viewscope.ViewScopeManager#PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS} (Mojarra-specific)</li>
 * <li>{@value org.omnifaces.cdi.viewscope.ViewScopeManager#PARAM_NAME_MYFACES_NUMBER_OF_VIEWS} (MyFaces-specific)</li>
 * </ul>
 * <p>If none of those context parameters are present, then a default size of
 * {@value org.omnifaces.cdi.viewscope.ViewScopeManager#DEFAULT_MAX_ACTIVE_VIEW_SCOPES} will be used. When a view scoped
 * bean is evicted from the LRU map, then its <code>&#64;PreDestroy</code> will also guaranteed to be invoked.
 * <p>
 * This setting has no effect when <code>saveInViewState</code> attribute is set to <code>true</code>.
 *
 * <h2>Using window.onbeforeunload</h2>
 * <p>
 * If you have a custom <code>onbeforeunload</code> handler, then it's strongly recommended to use plain vanilla JS
 * <code>window.onbeforeunload = function</code> instead of e.g. jQuery <code>$(window).on("beforeunload", function)</code>
 * or DOM <code>window.addEventListener("beforeunload", function)</code> for this. This way the <code>@ViewScoped</code>
 * unload can detect it and take it into account and continue to work properly. Otherwise the view scoped bean will
 * still be destroyed in background even when the user cancels and decides to stay in the same page.
 * <p>
 * Below is a kickoff example how to properly register it, assuming jQuery is available, and that "stateless" forms
 * and inputs (for which you don't want to trigger the unsaved data warning) have the class <code>stateless</code> set:
 * <pre>
 * $(document).on("change", "form:not(.stateless) :input:not(.stateless)", function() {
 *     $("body").data("unsavedchanges", true);
 * });
 * OmniFaces.Util.addSubmitListener(function() { // This hooks on Mojarra/MyFaces/PrimeFaces ajax submit events too.
 *     $("body").data("unsavedchanges", false);
 * });
 * window.onbeforeunload = function() {
 *     return $("body").data("unsavedchanges") ? "You have unsaved data. Are you sure you wish to leave this page?" : null;
 * };
 * </pre>
 *
 * <h2>Using download links</h2>
 * <p>
 * If you have a synchronous download link as in <code>&lt;a href="/path/to/file.ext"&gt;</code>, then the unload will
 * also be triggered. For HTML5-capable browsers it's sufficient to add the <code>download</code> attribute representing
 * the file name you'd like to use in the client specific "Save As" dialogue.
 * <pre>
 * &lt;a href="/path/to/file.ext" download="file.ext"&gt;download&lt;/a&gt;
 * </pre>
 * <p>
 * When this attribute is present, then the browser won't anymore trigger the unload event. In case your target browser
 * does not <a href="https://caniuse.com/#feat=download">support</a> it, then you'd need to explicitly disable the
 * OmniFaces unload event as follows:
 * <pre>
 * &lt;a href="/path/to/file.ext" onclick="OmniFaces.Unload.disable();"&gt;download&lt;/a&gt;
 * </pre>
 * <p>
 * An alternative is to explicitly open the download in a new tab/window. Decent browsers, even these not supporting the
 * <code>download</code> attribute, will usually automatically close the newly opened tab/window when a response with
 * <code>Content-Disposition: attachment</code> is received.
 * <pre>
 * &lt;a href="/path/to/file.ext" target="_blank"&gt;download&lt;/a&gt;
 * </pre>
 *
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see ViewScopeExtension
 * @see ViewScopeContext
 * @see ViewScopeManager
 * @see ViewScopeStorage
 * @see ViewScopeStorageInSession
 * @see ViewScopeStorageInViewState
 * @see ViewScopeEventListener
 * @see BeanStorage
 * @see OmniViewHandler
 * @see OmniExternalContext
 * @see OmniExternalContextFactory
 * @since 1.6
 */
@Inherited
@Documented
@NormalScope(passivating = true)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface ViewScoped {

	/**
	 * <p>
	 * Sets whether to save the view scoped bean instance in JSF view state instead of in HTTP session. By default,
	 * concrete view scoped bean instances are saved in HTTP session, also when client side state saving is enabled.
	 * This means, when the HTTP session expires, then the view scoped bean instances will also implicitly expire and
	 * be newly recreated upon a request in a new session. This may be undesirable when using client side state saving
	 * as it's intuitively expected that concrete view scoped beans are also saved in JSF view state.
	 *
	 * @return Whether to save the view scoped bean instance in JSF view state instead of in HTTP session.
	 * @throws IllegalStateException When enabled while not using client side state saving.
	 * @since 2.6
	 */
	@Nonbinding	boolean saveInViewState() default false;

}