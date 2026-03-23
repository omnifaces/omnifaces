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
package org.omnifaces.servlet;

import static jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.cdi.ScriptError;
import org.omnifaces.component.script.ScriptErrorHandler;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Servlets;

/**
 * <p>
 * This servlet receives script error reports sent by <code>&lt;o:scriptErrorHandler&gt;</code> via
 * <code>navigator.sendBeacon()</code> and fires them as {@link ScriptError} CDI events.
 *
 * @author Bauke Scholtz
 * @see ScriptErrorHandler
 * @see ScriptError
 * @since 5.2
 */
public class ScriptErrorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** The URI of the script error handler servlet. */
    public static final String URI = "/omnifaces.scripterror";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var scriptError = new ScriptError(
            request.getParameter("pageURL"),
            request.getParameter("errorMessage"),
            request.getParameter("sourceURL"),
            request.getParameter("lineNumber"),
            request.getParameter("columnNumber"),
            request.getParameter("errorName"),
            request.getParameter("errorStack"),
            Servlets.getRemoteAddr(request),
            Servlets.getUserAgent(request),
            request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null
        );

        Beans.fireEvent(scriptError);
        response.setStatus(SC_NO_CONTENT);
    }

}
