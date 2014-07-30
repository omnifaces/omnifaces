/*
 * Copyright 2014 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.util.Nonbinding;
import javax.faces.event.PhaseId;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.omnifaces.cdi.eager.EagerBeansFilter;
import org.omnifaces.cdi.eager.EagerBeansWebListener;

/**
 * <p>
 * The CDI annotation {@link Eager} specifies that a scoped bean is to be eagerly instantiated.
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
 * <p>
 * Currently supported scopes:
 * <ol>
 * <li> CDI {@link RequestScoped}
 * <li> OmniFaces {@link ViewScoped}
 * <li> CDI {@link SessionScoped}
 * <li> CDI {@link ApplicationScoped}
 * </ol>
 *
 * <p>
 * E.g.
 * The following bean will be instantiated during application's startup:
 * <pre>
 * {@literal @}Eager
 * {@literal @}ApplicationScoped
 * public class MyEagerApplicationScopedBean {
 *
 *     {@literal @}PostConstruct
 *     public void init() {
 *         System.out.println("Application scoped init!");
 *     }
 * }
 * </pre>
 * <p>
 * <em>Note: you can also use the stereotype {@link Startup} for this instead.</em>
 * <p>
 * The following bean will be instantiated whenever a session is created:
 * <pre>
 * {@literal @}Eager
 * {@literal @}SessionScoped
 * public class MyEagerSessionScopedBean implements Serializable {
 *
 *     private static final long serialVersionUID = 1L;
 *
 *     {@literal @}PostConstruct
 *     public void init() {
 *         System.out.println("Session scoped init!");
 *     }
 * }
 * </pre>
 * <p>
 * The following bean will be instantiated whenever the URI <code>/components/cache</code> (relatively to the
 * application root) is requested, i.e. when an app is deployed to <code>/myapp</code> at localhost this will correspond to
 * a URL like <code>http://localhost:8080/myapp/components/cache</code>:
 * <pre>
 * {@literal @}Eager(requestURI = "/components/cache")
 * {@literal @}RequestScoped
 * public class MyEagerRequestScopedBean {
 *
 *     {@literal @}PostConstruct
 *     public void init() {
 *         System.out.println("/components/cache requested");
 *     }
 * }
 * </pre>
 *
 * <h3> Compatibility </h3>
 *
 * <p>
 * In some (older) containers, most notably GlassFish 3, the CDI request scope is not available in a {@link ServletRequestListener}
 * (this is actually not spec complicant, as CDI demands this scope to be active then, but it is what it is).
 * <p>
 * Additionally in some containers, most notably GlassFish 3 again, instantiating session scoped beans from a {@link HttpSessionListener}
 * will corrupt "something" in the container. The instantiating of the beans will succeed, but if the session is later accessed an
 * exception like the following will be thrown:
 *
 * <pre>
 * java.lang.IllegalArgumentException: Should never reach here
 *     at org.apache.catalina.connector.SessionTracker.track(SessionTracker.java:168)
 *     at org.apache.catalina.connector.Request.doGetSession(Request.java:2939)
 *     at org.apache.catalina.connector.Request.getSession(Request.java:2583)
 *     at org.apache.catalina.connector.RequestFacade.getSession(RequestFacade.java:920)
 *     at javax.servlet.http.HttpServletRequestWrapper.getSession(HttpServletRequestWrapper.java:259)
 *     at com.sun.faces.context.ExternalContextImpl.getSession(ExternalContextImpl.java:155)
 *     at javax.faces.context.ExternalContextWrapper.getSession(ExternalContextWrapper.java:396)
 *     at javax.faces.context.ExternalContextWrapper.getSession(ExternalContextWrapper.java:396)
 *     ...
 * </pre>
 *
 * If any or both of those problems occur, a filter needs to be installed instead in <code>web.xml</code> as follows:
 *
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;eagerBeansFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.cdi.eager.EagerBeansFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 * &lt;filter-name&gt;eagerBeansFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * &lt;/filter-mapping&gt;
 *</pre>
 *
 * <p>
 * Note that the {@link EagerBeansFilter} will automatically disable the request/session listener by calling
 * {@link EagerBeansWebListener#disable()}.
 *
 *
 * @since 1.8
 * @author Arjan Tijms
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Eager {

	/**
	 * (Required when combined with {@link RequestScoped}) The URI (relative to the root of the web app) for which a
	 * request scoped bean should be instantiated. When this attribute is specified the bean will be instantiated very
	 * early during request processing, namely just before the first Servlet Filter is invoked, but after a SAM.
	 * <p>
	 * JSF services will not be available (yet) when the bean is instantiated this way.
	 * <p>
	 * If both this attribute and {@link Eager#viewId()} is specified, this attribute takes precedence for {@link RequestScoped}.
	 * This attribute <b>can not</b> be used for {@link ViewScoped} beans.
	 */
	@Nonbinding
	String requestURI() default "";

	/**
	 * (Required when combined with {@link RequestScoped} or {@link ViewScoped}) The id of the view for which a request or view scoped bean
	 * should be instantiated. When this attribute is specified the bean will be instantiated during invocation of the
	 * {@link FacesServlet}, namely right after the RESTORE_VIEW phase (see {@link PhaseId#RESTORE_VIEW}).
	 *
	 * <p>
	 * JSF services are available when the bean is instantiated this way.
	 *
	 * <p>
	 * If both this attribute and {@link Eager#requestURI()} is specified and the scope is {@link RequestScoped}, the
	 * <code>requestURI</code> attribute takes precedence. If the scope is {@link ViewScoped} <code>requestURI</code> is ignored and only
	 * this attribute is considered.
	 */
	@Nonbinding
	String viewId() default "";

}