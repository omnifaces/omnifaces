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
package org.omnifaces.util;

import static java.util.stream.Collectors.toSet;
import static org.omnifaces.util.FacesLocal.getFlash;
import static org.omnifaces.util.Messages.create;
import static org.omnifaces.util.Messages.createError;
import static org.omnifaces.util.Messages.createFatal;
import static org.omnifaces.util.Messages.createInfo;
import static org.omnifaces.util.Messages.createWarn;
import static org.omnifaces.util.Utils.stream;

import java.util.Iterator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

/**
 * <p>
 * Collection of utility methods for the Faces API with respect to working with {@link FacesMessage}.
 * <p>
 * The difference with {@link Messages} is that no one method of {@link MessagesLocal} obtains the {@link FacesContext}
 * from the current thread by {@link FacesContext#getCurrentInstance()}. This job is up to the caller. This is more
 * efficient in situations where multiple utility methods needs to be called at the same time. Invoking
 * {@link FacesContext#getCurrentInstance()} is at its own an extremely cheap operation, however as it's to be obtained
 * as a {@link ThreadLocal} variable, it's during the call still blocking all other running threads for some nanoseconds
 * or so.
 *
 * @author Bauke Scholtz
 * @since 4.2
 */
public final class MessagesLocal {

    // Constructors ---------------------------------------------------------------------------------------------------

    private MessagesLocal() {
        // Hide constructor.
    }

    // Shortcuts - add message ----------------------------------------------------------------------------------------

    /**
     * @see Messages#add(String, FacesMessage)
     */
    public static void add(FacesContext context, String clientId, FacesMessage message) {
        context.addMessage(clientId, message);
    }

    /**
     * @see Messages#add(jakarta.faces.application.FacesMessage.Severity, String, String, Object...)
     */
    public static void add(FacesContext context, FacesMessage.Severity severity, String clientId, String message, Object... params) {
        add(context, clientId, create(severity, message, params));
    }

    /**
     * @see Messages#addInfo(String, String, Object...)
     */
    public static void addInfo(FacesContext context, String clientId, String message, Object... params) {
        add(context, clientId, createInfo(message, params));
    }

    /**
     * @see Messages#addWarn(String, String, Object...)
     */
    public static void addWarn(FacesContext context, String clientId, String message, Object... params) {
        add(context, clientId, createWarn(message, params));
    }

    /**
     * @see Messages#addError(String, String, Object...)
     */
    public static void addError(FacesContext context, String clientId, String message, Object... params) {
        add(context, clientId, createError(message, params));
    }

    /**
     * @see Messages#addFatal(String, String, Object...)
     */
    public static void addFatal(FacesContext context, String clientId, String message, Object... params) {
        add(context, clientId, createFatal(message, params));
    }

    // Shortcuts - add global message ---------------------------------------------------------------------------------

    /**
     * @see Messages#addGlobal(FacesMessage)
     */
    public static void addGlobal(FacesContext context, FacesMessage message) {
        add(context, null, message);
    }

    /**
     * @see Messages#addGlobal(jakarta.faces.application.FacesMessage.Severity, String, Object...)
     */
    public static void addGlobal(FacesContext context, FacesMessage.Severity severity, String message, Object... params) {
        addGlobal(context, create(severity, message, params));
    }

    /**
     * @see Messages#addGlobalInfo(String, Object...)
     */
    public static void addGlobalInfo(FacesContext context, String message, Object... params) {
        addGlobal(context, createInfo(message, params));
    }

    /**
     * @see Messages#addGlobalWarn(String, Object...)
     */
    public static void addGlobalWarn(FacesContext context, String message, Object... params) {
        addGlobal(context, createWarn(message, params));
    }

    /**
     * @see Messages#addGlobalError(String, Object...)
     */
    public static void addGlobalError(FacesContext context, String message, Object... params) {
        addGlobal(context, createError(message, params));
    }

    /**
     * @see Messages#addGlobalFatal(String, Object...)
     */
    public static void addGlobalFatal(FacesContext context, String message, Object... params) {
        addGlobal(context, createFatal(message, params));
    }

    // Shortcuts - add flash message ----------------------------------------------------------------------------------

    /**
     * @see Messages#addFlash(String, FacesMessage)
     */
    public static void addFlash(FacesContext context, String clientId, FacesMessage message) {
        getFlash(context).setKeepMessages(true);
        add(context, clientId, message);
    }

    /**
     * @see Messages#addFlash(jakarta.faces.application.FacesMessage.Severity, String, String, Object...)
     */
    public static void addFlash(FacesContext context, FacesMessage.Severity severity, String clientId, String message, Object... params) {
        addFlash(context, clientId, create(severity, message, params));
    }

    /**
     * @see Messages#addFlashInfo(String, String, Object...)
     */
    public static void addFlashInfo(FacesContext context, String clientId, String message, Object... params) {
        addFlash(context, clientId, createInfo(message, params));
    }

    /**
     * @see Messages#addFlashWarn(String, String, Object...)
     */
    public static void addFlashWarn(FacesContext context, String clientId, String message, Object... params) {
        addFlash(context, clientId, createWarn(message, params));
    }

    /**
     * @see Messages#addFlashError(String, String, Object...)
     */
    public static void addFlashError(FacesContext context, String clientId, String message, Object... params) {
        addFlash(context, clientId, createError(message, params));
    }

    /**
     * @see Messages#addFlashFatal(String, String, Object...)
     */
    public static void addFlashFatal(FacesContext context, String clientId, String message, Object... params) {
        addFlash(context, clientId, createFatal(message, params));
    }

    // Shortcuts - add global flash message ---------------------------------------------------------------------------

    /**
     * @see Messages#addFlashGlobal(FacesMessage)
     */
    public static void addFlashGlobal(FacesContext context, FacesMessage message) {
        addFlash(context, null, message);
    }

    /**
     * @see Messages#addFlashGlobalInfo(String, Object...)
     */
    public static void addFlashGlobalInfo(FacesContext context, String message, Object... params) {
        addFlashGlobal(context, createInfo(message, params));
    }

    /**
     * @see Messages#addFlashGlobalWarn(String, Object...)
     */
    public static void addFlashGlobalWarn(FacesContext context, String message, Object... params) {
        addFlashGlobal(context, createWarn(message, params));
    }

    /**
     * @see Messages#addFlashGlobalError(String, Object...)
     */
    public static void addFlashGlobalError(FacesContext context, String message, Object... params) {
        addFlashGlobal(context, createError(message, params));
    }

    /**
     * @see Messages#addFlashGlobalFatal(String, Object...)
     */
    public static void addFlashGlobalFatal(FacesContext context, String message, Object... params) {
        addFlashGlobal(context, createFatal(message, params));
    }

    // Shortcuts - check messages -------------------------------------------------------------------------------------

    /**
     * @see Messages#isEmpty()
     */
    public static boolean isEmpty(FacesContext context) {
        return context.getMessageList().isEmpty();
    }

    /**
     * @see Messages#isEmpty(String)
     */
    public static boolean isEmpty(FacesContext context, String clientId) {
        return context.getMessageList(clientId).isEmpty();
    }

    /**
     * @see Messages#isGlobalEmpty()
     */
    public static boolean isGlobalEmpty(FacesContext context) {
        return isEmpty(context, null);
    }

    // Shortcuts - clear messages -------------------------------------------------------------------------------------

    /**
     * @see Messages#clear(jakarta.faces.application.FacesMessage.Severity, String...)
     */
    public static boolean clear(FacesContext context, FacesMessage.Severity severity, String... clientIds) {
        if (Utils.isEmpty(clientIds)) {
            return clear(context.getMessages(), severity);
        }
        else {
            return stream(clientIds).map(clientId -> clear(context.getMessages(clientId), severity)).collect(toSet()).contains(true);
        }
    }

    private static boolean clear(Iterator<FacesMessage> iterator, FacesMessage.Severity severity) {
        boolean atLeastOneCleared = false;

        while (iterator.hasNext()) {
            FacesMessage facesMessage = iterator.next();

            if (severity == null || severity.equals(facesMessage.getSeverity())) {
                iterator.remove();
                atLeastOneCleared = true;
            }
        }

        return atLeastOneCleared;
    }

    /**
     * @see Messages#clear(String...)
     */
    public static boolean clear(FacesContext context, String... clientIds) {
        return clear(context, null, clientIds);
    }

    /**
     * @see Messages#clearInfo(String...)
     */
    public static boolean clearInfo(FacesContext context, String... clientIds) {
        return clear(context, FacesMessage.SEVERITY_INFO, clientIds);
    }

    /**
     * @see Messages#clearWarn(String...)
     */
    public static boolean clearWarn(FacesContext context, String... clientIds) {
        return clear(context, FacesMessage.SEVERITY_WARN, clientIds);
    }

    /**
     * @see Messages#clearError(String...)
     */
    public static boolean clearError(FacesContext context, String... clientIds) {
        return clear(context, FacesMessage.SEVERITY_ERROR, clientIds);
    }

    /**
     * @see Messages#clearFatal(String...)
     */
    public static boolean clearFatal(FacesContext context, String... clientIds) {
        return clear(context, FacesMessage.SEVERITY_FATAL, clientIds);
    }

    /**
     * @see Messages#clearGlobal(jakarta.faces.application.FacesMessage.Severity)
     */
    public static boolean clearGlobal(FacesContext context, FacesMessage.Severity severity) {
        return clear(context, severity, (String) null);
    }

    /**
     * @see Messages#clearGlobal()
     */
    public static boolean clearGlobal(FacesContext context) {
        return clear(context, (FacesMessage.Severity) null, (String) null);
    }

    /**
     * @see Messages#clearGlobalInfo()
     */
    public static boolean clearGlobalInfo(FacesContext context) {
        return clearInfo(context, (String) null);
    }

    /**
     * @see Messages#clearGlobalWarn()
     */
    public static boolean clearGlobalWarn(FacesContext context) {
        return clearWarn(context, (String) null);
    }

    /**
     * @see Messages#clearGlobalError()
     */
    public static boolean clearGlobalError(FacesContext context) {
        return clearError(context, (String) null);
    }

    /**
     * @see Messages#clearGlobalFatal()
     */
    public static boolean clearGlobalFatal(FacesContext context) {
        return clearFatal(context, (String) null);
    }

}