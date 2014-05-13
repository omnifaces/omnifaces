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

/**
 * Specifies that a scoped bean is to be eagerly instantiated.
 * <p>
 * Currently supported scopes:
 * <ol>
 * <li> {@link RequestScoped}
 * <li> {@link ViewScoped}
 * <li> {@link SessionScoped}
 * <li> {@link ApplicationScoped}
 * </ol>
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