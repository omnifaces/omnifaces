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

import java.io.Serializable;

import jakarta.enterprise.event.Observes;

import org.omnifaces.component.script.ScriptErrorHandler;

/**
 * <p>
 * This CDI event will be fired by {@link ScriptErrorHandler} when an uncaught JavaScript error or unhandled promise
 * rejection is caught in the client. An application scoped CDI bean can <code>&#64;</code>{@link Observes} this event.
 * <p>
 * For detailed usage instructions, see {@link ScriptErrorHandler} javadoc.
 *
 * <h2>Usage</h2>
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
 *
 * @author Bauke Scholtz
 * @see ScriptErrorHandler
 * @since 5.2
 *
 * @param pageURL The URL of the page where the error occurred, as obtained from <code>window.location.href</code>.
 * @param errorMessage The error message, as obtained from the first argument of <code>window.onerror</code>
 * or the <code>reason</code> of an <code>unhandledrejection</code> event.
 * @param sourceURL The URL of the script file where the error originated, as obtained from the second argument of
 * <code>window.onerror</code>, or <code>null</code> for unhandled promise rejections.
 * @param lineNumber The line number in the script where the error occurred, as obtained from the third argument of
 * <code>window.onerror</code>, or <code>null</code> for unhandled promise rejections.
 * @param columnNumber The column number in the script where the error occurred, as obtained from the fourth argument of
 * <code>window.onerror</code>, or <code>null</code> for unhandled promise rejections.
 * @param errorName The error type name (e.g. "TypeError", "ReferenceError", "UnhandledRejection"), as obtained from
 * the <code>name</code> property of the <code>Error</code> object, or <code>null</code> if unavailable.
 * @param errorStack The JavaScript stack trace, as obtained from the <code>stack</code> property of the
 * <code>Error</code> object, or <code>null</code> if unavailable.
 * @param remoteAddr The remote address of the client, taking proxy headers (<code>X-Forwarded-For</code> etc.) into account.
 * @param userAgent The User-Agent header of the request.
 * @param userPrincipal The name of the authenticated user principal, or <code>null</code> if not authenticated.
 */
public record ScriptError(
    String pageURL,
    String errorMessage,
    String sourceURL,
    String lineNumber,
    String columnNumber,
    String errorName,
    String errorStack,
    String remoteAddr,
    String userAgent,
    String userPrincipal
) implements Serializable {}
