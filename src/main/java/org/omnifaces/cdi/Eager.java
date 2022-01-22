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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.util.Nonbinding;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.omnifaces.cdi.eager.EagerBeansPhaseListener;
import org.omnifaces.cdi.eager.EagerBeansRepository;
import org.omnifaces.cdi.eager.EagerBeansWebListener;
import org.omnifaces.cdi.eager.EagerExtension;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link Eager} specifies that a scoped bean is to be eagerly instantiated.
 * <p>
 * JSF's own native managed beans are being deprecated in favor of CDI managed beans. One feature that those native JSF
 * managed beans had that's not directly available for CDI managed beans is the ability to eagerly instantiate
 * application scoped beans.
 * <p>
 * OmniFaces fills this void and even goes one step further by introducing the <code>@Eager</code> annotation
 * that can be applied to <code>@RequestScoped</code>, <code>@ViewScoped</code>,
 * <code>@SessionScoped</code> and <code>@ApplicationScoped</code> beans. This causes these beans to be instantiated
 * automatically at the start of each such scope instead of on demand when a bean is first referenced.
 * <p>
 * In case of <code>@RequestScoped</code> and <code>@ViewScoped</code> beans instantiation happens per request URI / view
 * and an extra attribute is required for specifying this.
 *
 * <h3>Supported scopes</h3>
 *
 * <p>
 * Currently supported scopes:
 * <ol>
 * <li> CDI {@link RequestScoped}
 * <li> CDI {@link javax.faces.view.ViewScoped}
 * <li> OmniFaces {@link ViewScoped}
 * <li> CDI {@link SessionScoped}
 * <li> CDI {@link ApplicationScoped}
 * </ol>
 *
 * <h3>Usage</h3>
 *
 * <p>
 * E.g.
 * The following bean will be instantiated during application's startup:
 * <pre>
 * &#64;Eager
 * &#64;ApplicationScoped
 * public class MyEagerApplicationScopedBean {
 *
 *     &#64;PostConstruct
 *     public void init() {
 *         System.out.println("Application scoped init!");
 *     }
 * }
 * </pre>
 * <p>
 * <em>Note: you can also use the stereotype <code>&#64;</code>{@link Startup} for this instead.</em>
 * <p>
 * The following bean will be instantiated whenever a session is created:
 * <pre>
 * &#64;Eager
 * &#64;SessionScoped
 * public class MyEagerSessionScopedBean implements Serializable {
 *
 *     private static final long serialVersionUID = 1L;
 *
 *     &#64;PostConstruct
 *     public void init() {
 *         System.out.println("Session scoped init!");
 *     }
 * }
 * </pre>
 * <p>
 * The following bean will be instantiated whenever the URI <code>/components/cache</code> (relatively to the
 * application root) is requested, i.e. when an app is deployed to <code>/myapp</code> at localhost this will correspond to
 * a URL like <code>https://example.com/myapp/components/cache</code>:
 * <pre>
 * &#64;Eager(requestURI = "/components/cache")
 * &#64;RequestScoped
 * public class MyEagerRequestScopedBean {
 *
 *     &#64;PostConstruct
 *     public void init() {
 *         System.out.println("/components/cache requested");
 *     }
 * }
 * </pre>
 *
 * <h3><code>FacesContext</code> in <code>&#64;PostConstruct</code></h3>
 *
 * <p>
 * When using <code>&#64;Eager</code> or <code>&#64;Eager(requestURI)</code>, be aware that the {@link FacesContext} is
 * <strong>not</strong> available in the <code>&#64;PostConstruct</code>. Reason is, the {@link FacesServlet} isn't
 * invoked yet at the moment <code>&#64;Eager</code> bean is constructed. Only in <code>&#64;Eager(viewId)</code>, the
 * {@link FacesContext} is available in the <code>&#64;PostConstruct</code>.
 * <p>
 * In case you need information from {@link HttpServletRequest}, {@link HttpSession} and/or {@link ServletContext}, then
 * you can just <code>&#64;Inject</code> it right away. Also, all other CDI managed beans are available the usual way
 * via <code>&#64;Inject</code>, as long as they do also not depend on {@link FacesContext} in their
 * <code>&#64;PostConstruct</code>.
 *
 * @since 1.8
 * @author Arjan Tijms
 * @see EagerExtension
 * @see EagerBeansRepository
 * @see EagerBeansPhaseListener
 * @see EagerBeansWebListener
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Eager {

	/**
	 * (Required when combined with {@link RequestScoped}) The URI (relative to the root of the web app) for which a
	 * request scoped bean should be instantiated. When this attribute is specified the bean will be instantiated very
	 * early during request processing, namely just before the first Servlet Filter is invoked, but after a SAM.
	 * <p>
	 * JSF services will not be available (yet) when the bean is instantiated this way.
	 * <p>
	 * If both this attribute and {@link Eager#viewId()} is specified, this attribute takes precedence for {@link RequestScoped}.
	 * This attribute <b>can not</b> be used for <code>ViewScoped</code> beans.
	 *
	 * @return The request URI relative to the context root.
	 */
	@Nonbinding
	String requestURI() default "";

	/**
	 * (Required when combined with {@link RequestScoped} or <code>ViewScoped</code>) The id of the view for which a request or view scoped bean
	 * should be instantiated. When this attribute is specified the bean will be instantiated during invocation of the
	 * {@link FacesServlet}, namely right after the RESTORE_VIEW phase (see {@link PhaseId#RESTORE_VIEW}).
	 *
	 * <p>
	 * JSF services are available when the bean is instantiated this way.
	 *
	 * <p>
	 * If both this attribute and {@link Eager#requestURI()} is specified and the scope is {@link RequestScoped}, the
	 * <code>requestURI</code> attribute takes precedence. If the scope is <code>ViewScoped</code> <code>requestURI</code> is ignored and only
	 * this attribute is considered.
	 *
	 * @return The view ID.
	 */
	@Nonbinding
	String viewId() default "";

}