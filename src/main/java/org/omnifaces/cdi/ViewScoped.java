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
import javax.faces.context.FacesContext;

import org.omnifaces.application.ViewScopeEventListener;
import org.omnifaces.cdi.viewscope.ViewScopeContext;
import org.omnifaces.cdi.viewscope.ViewScopeExtension;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.viewhandler.RestorableViewHandler;

/**
 * <p>
 * The CDI view scope annotation, with more optimal handling of bean destroy as compared to standard JSF one. Just use
 * it the usual way as all other CDI scopes. Watch out with IDE autocomplete on import that you don't accidentally
 * import standard JSF's own one.
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
 * <p>
 * In effects, this CDI view scope annotation has exactly the same lifecycle as JSF's own view scope. Only the bean
 * destroy is more optimal handled. In standard JSF, the {@link PreDestroy} annotated method on a CDI view scoped bean
 * isn't <em>immediately</em> invoked in all cases when the view scope ends. For detail, see the following JSF issues
 * related to the matter:
 * <ul>
 * <li><a href="https://java.net/jira/browse/JAVASERVERFACES-1351">Mojarra issue 1351</a>
 * <li><a href="https://java.net/jira/browse/JAVASERVERFACES-1839">Mojarra issue 1839</a>
 * <li><a href="https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-905">JSF spec issue 905</a>
 * </ul>
 * <p>
 * Summarized, it's only <em>immediately</em> invoked when the view is either explicitly changed by a non-null/void
 * navigation on a postback, or when the view is explicitly rebuilt by {@link FacesContext#setViewRoot(UIViewRoot)}.
 * It's not <em>immediately</em> invoked on a GET navigation, nor a close of browser tab/window. In JSF 2.0/2.1, it's
 * even not afterwards invoked on session expire (JSF 2.2 does).
 * <p>
 * This CDI view scope annotation not only guarantees that the {@link PreDestroy} annotated method is also invoked on
 * session expire, but it also hooks on the browser <code>beforeunload</code> event so that the bean destroy is yet more
 * optimally handled. I.e. when the user navigates away by GET, or closes the browser tab/window, then the
 * {@link PreDestroy} annotated method will instantly be invoked. This trick is done by a synchronous XHR request via
 * an automatically included helper script <code>omnifaces:unload.js</code>.
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
 * bean is evicted from the LRU map, then its {@link PreDestroy} will also guaranteed to be invoked.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see ViewScopeExtension
 * @see ViewScopeContext
 * @see ViewScopeManager
 * @see ViewScopeEventListener
 * @see RestorableViewHandler
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