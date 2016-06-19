/*
 * Copyright 2013 OmniFaces.
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

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.enterprise.context.NormalScope;
import javax.faces.component.UIViewRoot;

import org.omnifaces.cdi.viewscope.ViewScopeContext;
import org.omnifaces.cdi.viewscope.ViewScopeEventListener;
import org.omnifaces.cdi.viewscope.ViewScopeExtension;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.context.OmniExternalContext;
import org.omnifaces.context.OmniExternalContextFactory;
import org.omnifaces.viewhandler.RestorableViewHandler;

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
 * is done by a synchronous XHR request via an automatically included helper script <code>omnifaces:unload.js</code>.
 * There's however a small caveat: on slow network and/or poor server hardware, there may be a noticeable lag between
 * the enduser action of unloading the page and the desired result. If this is undesireable, then better stick to JSF
 * 2.2's own view scope annotations and accept the postponed destroy.
 * <p>
 * Since OmniFaces 2.3, the unload has been further improved to also physically remove the associated JSF view state
 * from JSF implementation's internal LRU map in case of server side state saving, hereby further decreasing the risk
 * at <code>ViewExpiredException</code> on the other views which were created/opened earlier. As side effect of this
 * change, the <code>&#64;PreDestroy</code> annotated method of any standard JSF view scoped beans referenced in the
 * same view as the OmniFaces CDI view scoped bean will also guaranteed be invoked on browser unload.
 * <p>
 * In a nutshell: if you're on JSF 2.0/2.1, and you can't upgrade to JSF 2.2, and you want the
 * <code>&#64;PreDestroy</code> to be invoked on sesison expire too, then use OmniFaces 1.6+ with this view scope
 * annotation. Or, if you're on JSF 2.2 already, and you want the <code>&#64;PreDestroy</code> to be invoked on browser
 * unload too, then use OmniFaces 2.2+ with this view scope annotation.
 * <p>
 * Related JSF issues:
 * <ul>
 * <li><a href="https://java.net/jira/browse/JAVASERVERFACES-1351">Mojarra issue 1351</a>
 * <li><a href="https://java.net/jira/browse/JAVASERVERFACES-1839">Mojarra issue 1839</a>
 * <li><a href="https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-905">JSF spec issue 905</a>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>
 * Just use it the usual way as all other CDI scopes. Watch out with IDE autocomplete on import that you don't
 * accidentally import standard JSF's own one.
 * <pre>
 * import javax.inject.Named;
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
 * Under the covers, CDI managed beans with this scope are via {@link ViewScopeManager} stored in the session scope by
 * an {@link UUID} based key which is referenced in JSF's own view map as available by {@link UIViewRoot#getViewMap()}.
 * They are not stored in the JSF view map itself as that would be rather expensive in case of client side state saving.
 *
 * <h3>Configuration</h3>
 * <p>
 * By default, the maximum number of active view scopes is hold in a LRU map with a default size equal to the first
 * non-null value of the following context parameters:
 * <ul>
 * <li>{@value org.omnifaces.cdi.viewscope.ViewScopeManager#PARAM_NAME_MAX_ACTIVE_VIEW_SCOPES} (OmniFaces)</li>
 * <li>{@value org.omnifaces.cdi.viewscope.ViewScopeManager#PARAM_NAME_MOJARRA_NUMBER_OF_VIEWS} (Mojarra-specific)</li>
 * <li>{@value org.omnifaces.cdi.viewscope.ViewScopeManager#PARAM_NAME_MYFACES_NUMBER_OF_VIEWS} (MyFaces-specific)</li>
 * </ul>
 * <p>If none of those context parameters are present, then a default size of
 * {@value org.omnifaces.cdi.viewscope.ViewScopeManager#DEFAULT_MAX_ACTIVE_VIEW_SCOPES} will be used. When a view scoped
 * bean is evicted from the LRU map, then its <code>&#64;PreDestroy</code> will also guaranteed to be invoked.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see ViewScopeExtension
 * @see ViewScopeContext
 * @see ViewScopeManager
 * @see ViewScopeEventListener
 * @see BeanStorage
 * @see RestorableViewHandler
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
	//
}