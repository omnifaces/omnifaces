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
package org.omnifaces.test.cdi.viewscoped.unrendered;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.omnifaces.cdi.viewscope.ViewScopeStorageInSession;
import org.omnifaces.util.Beans;

/**
 * Exposes to the test, which runs outside the server, how many view scopes of the session of the request are held by OmniFaces and by the Faces implementation,
 * and how many view scoped beans of this test were created and destroyed. Each value is printed on its own line as <code>name=value</code>; -1 means that it is
 * not available.
 */
@WebServlet("/probe")
public class ViewScopedUnrenderedITServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String[] BEAN_NAMES = { "main", "viewAction", "preRenderView", "build", "postback", "errorPage", "sibling", "jstl", "afterForm" };
    private static final String MOJARRA_ACTIVE_VIEW_MAPS = "com.sun.faces.application.view.activeViewMaps";
    private static final String[] MYFACES_VIEW_SCOPE_HOLDERS = {
        "org.apache.myfaces.cdi.view.ViewScopeBeanHolder", // MyFaces 3.x
        "org.apache.myfaces.cdi.view.ViewScopeContextualStorageHolder" // MyFaces 4.x
    };

    @Inject
    private ViewScopedUnrenderedITProbe probe;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        StringBuilder output = new StringBuilder();
        output.append("activeViewScopes=").append(session == null ? -1 : getActiveViewScopes()).append('\n');
        output.append("viewMaps=").append(session == null ? -1 : getViewMaps(session)).append('\n');

        for (String name : BEAN_NAMES) {
            output.append("created.").append(name).append('=').append(probe.getCreated(name)).append('\n');
            output.append("destroyed.").append(name).append('=').append(probe.getDestroyed(name)).append('\n');
            output.append("destroyedWithoutFacesContext.").append(name).append('=').append(probe.getDestroyedWithoutFacesContext(name)).append('\n');
        }

        response.getWriter().print(output);
    }

    private static int getActiveViewScopes() {
        ViewScopeStorageInSession storage = Beans.getInstance(ViewScopeStorageInSession.class, false);

        if (storage == null) {
            return 0;
        }

        try {
            Field field = ViewScopeStorageInSession.class.getDeclaredField("activeViewScopes");
            field.setAccessible(true);
            return ((Map<?, ?>) field.get(storage)).size();
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int getViewMaps(HttpSession session) {
        Object mojarraViewMaps = session.getAttribute(MOJARRA_ACTIVE_VIEW_MAPS);

        if (mojarraViewMaps instanceof Map) {
            return ((Map<?, ?>) mojarraViewMaps).size();
        }

        for (String holderClassName : MYFACES_VIEW_SCOPE_HOLDERS) {
            try {
                Class<?> holderClass = Class.forName(holderClassName);
                Object holder = Beans.getInstance(holderClass, false);
                return holder == null ? 0 : ((Map<?, ?>) holderClass.getMethod("getStorageMap").invoke(holder)).size();
            }
            catch (ClassNotFoundException e) {
                continue;
            }
            catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        return mojarraViewMaps == null ? 0 : -1;
    }

}
