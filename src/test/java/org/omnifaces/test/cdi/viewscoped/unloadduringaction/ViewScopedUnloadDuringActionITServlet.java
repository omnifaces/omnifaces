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
package org.omnifaces.test.cdi.viewscoped.unloadduringaction;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exposes {@link ViewScopedUnloadDuringActionITProbe} to the test, which runs outside the server.
 */
@WebServlet("/probe/*")
public class ViewScopedUnloadDuringActionITServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private ViewScopedUnloadDuringActionITProbe probe;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var command = String.valueOf(request.getPathInfo());

        switch (command) {
            case "/reset":
                probe.reset();
                break;
            case "/awaitActionStarted":
                response.getWriter().print(probe.awaitActionStarted());
                break;
            case "/releaseAction":
                probe.releaseAction();
                break;
            case "/awaitBeanDestroyed":
                response.getWriter().print(probe.awaitBeanDestroyed());
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
