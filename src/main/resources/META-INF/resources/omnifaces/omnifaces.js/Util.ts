///
/// Copyright OmniFaces
///
/// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
/// the License. You may obtain a copy of the License at
///
///     https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
/// an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
/// specific language governing permissions and limitations under the License.
///

import { VIEW_STATE_PARAM } from "./OmniFaces";

/**
 * Utility scripts.
 * 
 * @author Bauke Scholtz
 * @since 2.2
 */
export module Util {

    // "Constant" fields ----------------------------------------------------------------------------------------------

    const ERROR_MISSING_FORM = "OmniFaces: Cannot find a Faces form in the document. Please add one.";

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Add given event listener on the given events to the given target.
     * @param target Target to add event listener to.
     * @param events Space separated string of event names.
     * @param listener The event listener.
     */
    export function addEventListener(target: EventTarget, events: string, listener: Function) {
        handleEventListener(target, "addEventListener", events, listener);
    }

    /**
     * Remove given event listener on the given events from the given target.
     * @param target Target to remove event listener from.
     * @param events Space separated string of event names.
     * @param listener The event listener.
     */
    export function removeEventListener(target: EventTarget, events: string, listener: Function) {
        handleEventListener(target, "removeEventListener", events, listener);
    }

    /**
     * Register the given function as window onload listener function.
     * @param listener The function to be invoked during window load event.
     */
    export function addOnloadListener(listener: Function) {
        if (window.jQuery) {
            window.jQuery(listener);
        }
        else if (document.readyState === "complete") {
            setTimeout(listener);
        }
        else {
            addEventListener(window, "load", listener);
        }
    }

    /**
     * Add submit listener to the document and all synchronous Faces submit handler functions.
     * @param listener The listener to invoke before the (synchronous) submit.
     */
    export function addSubmitListener(listener: Function) {
        addEventListener(document, "submit", listener); // Invoke given listener on any (propagated!) submit event (e.g. h:commandLink and p:commandButton ajax=false).

        if (window.mojarra) {
            decorateFacesSubmit(window.mojarra, "cljs", listener); // Decorate Mojarra h:commandLink submit handler to invoke given listener first.
        }

        if (window.myfaces) {
             decorateFacesSubmit(window.myfaces.oam, "submitForm", listener); // Decorate MyFaces h:commandLink submit handler to invoke given listener first.
        }

        if (window.PrimeFaces) {
             decorateFacesSubmit(window.PrimeFaces, "addSubmitParam", listener); // Decorate PrimeFaces p:commandLink ajax=false submit handler to invoke given listener first.
        }
    }

    /**
     * If given function is actually not a function, then try to interpret it as name of a global function.
     * If it still doesn't resolve to anything, then return a NOOP function.
     * @param fn Can be function, or string representing function name, or undefined.
     */
    export function resolveFunction(fn: any) {
        return (typeof fn !== "function") && (fn = window[fn] || function(){}), fn;
    }

    /**
     * Get the first Faces form containing view state param from the current document.
     * @return The first Faces form of the current document.
     */
    export function getFacesForm(): HTMLFormElement | null {
        for (let i = 0; i < document.forms.length; i++) {
            const form = document.forms[i];

            if (form[VIEW_STATE_PARAM]) {
                return form;
            }
        }

        const faces = window.faces;

        if ((!faces || faces.getProjectStage() == "Development") && window.console && console.error) {
            console.error(ERROR_MISSING_FORM);
        }

        return null;
    }

    /**
     * Update a parameter in given query string in application/x-www-url-encoded format.
     * @param query The query string.
     * @param name The name of the parameter to update. If it doesn't exist, then it will be added.
     * @param value The value of the parameter to update. If it is falsey, then it will be removed.
     */
    export function updateParameter(query: string, name: string, value: string) {
        const re = new RegExp("(^|[?&#])" + name + "=.*?([&#]|$)", "i");

        if (value) {
            const parameter = name + "=" + encodeURIComponent(value);

            if (!query) {
                query = parameter;
            }
            else if (query.match(re)) {
                query = query.replace(re, "$1" + parameter + "$2");
            }
            else {
                query += "&" + parameter;
            }
        }
        else {
            query = query.replace(re, "$2");
        }

        if (query.charAt(0) == "&") {
            query = query.substring(1);
        }

        return query;
    }

    /**
     * Load a script.
     * @param url Required; The URL of the script.
     * @param crossorigin Optional; The crossorigin of the script. Defaults to "anonymous".
     * @param integrity Optional; The integrity of the script. Defaults to "".
     * @param begin Optional; Function to invoke before deferred script is loaded.
     * @param success Optional; Function to invoke after deferred script is successfully loaded.
     * @param error Optional; Function to invoke when loading of deferred script failed.
     * @param complete Optional; Function to invoke after deferred script is loaded, regardless of its success/error outcome.
     */
    export function loadScript(url: string, crossorigin: string, integrity: string, begin: Function, success: Function, error: Function, complete: Function) {
        const beginFunction = resolveFunction(begin);
        const successFunction = resolveFunction(success);
        const errorFunction = resolveFunction(error);
        const completeFunction = resolveFunction(complete);

        const script = document.createElement("script");
        const head = document.head || document.documentElement;

        script.async = true;
        script.src = url;
        script.setAttribute("crossorigin", crossorigin || "anonymous");
        script.setAttribute("integrity", integrity || "");

        script.onerror = function() {
            errorFunction();
            completeFunction();
        }

        script.onload = function() {
            successFunction();
            completeFunction();
        }

        addOnloadListener(function() {
            beginFunction();
            head.insertBefore(script, null); // IE6 has trouble with appendChild.
        });
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Handle the given target via the given function names to add or remove the given event listener on the given events.
     * @param target Target to be altered.
     * @param functionName Standard (W3C) event handler function name to invoke on the given target.
     * @param events Space separated string of event names.
     * @param listener The event listener to be added or removed on the given target via given functions.
     */
    function handleEventListener(target: any, functionName: string, events: string, listener: Function) {
        const eventParts = events.replace(/^\s+|\s+$/g, "").split(/\s+/);

        for (let event of eventParts) {
            if (target[functionName]) {
                target[functionName](event, listener);
            }
        }
    }

    /**
     * Decorate the JavaScript based submit function of the Faces implementation to invoke the given listener first.
     * @param {object} facesImpl The Faces implementation script object holding the submit function.
     * @param {string} functionName The name of the Faces submit function to decorate.
     * @param {Function} listener The listener to invoke before the Faces submit function.
     */
    function decorateFacesSubmit(facesImpl: any, functionName: string, listener: Function) {
        const submitFunction = facesImpl[functionName];

        if (submitFunction) {
            facesImpl[functionName] = function() {
                listener();
                return submitFunction.apply(this, arguments);
            };
        }
    }
}
