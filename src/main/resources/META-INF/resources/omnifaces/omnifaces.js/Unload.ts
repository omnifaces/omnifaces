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

import { EVENT } from "./OmniFaces";
import { VIEW_STATE_PARAM } from "./OmniFaces";
import { Util } from "./Util";

/**
 * Fire "unload" event to server side via keepalive fetch, beacon or synchronous XHR when the window is about to be
 * unloaded as result of a non-submit event, so that e.g. any view scoped beans will immediately be destroyed when
 * enduser refreshes page, or navigates away, or closes browser.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.ViewScopeManager
 * @since 2.2
 */
export module Unload {

    // Private static fields ------------------------------------------------------------------------------------------

    const EVENT_HEADER = "OmniFaces-Event";
    const VIEW_SCOPE_HEADER = "OmniFaces-View-Scope";
    const VIEW_STATE_HEADER = "OmniFaces-View-State";
    // A server side view state ID is far below; a client side view state may exceed the header size limit of the server.
    const MAX_VIEW_STATE_HEADER_LENGTH = 1024;

    let id: string;
    let disabled: boolean;

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Initialize the unload event listener on the current document. This will check if XHR is supported and if the
     * current document has a JSF form with a view state element. If so, then register the <code>unload</code> event to
     * send a beacon or synchronous XHR request with the OmniFaces view scope ID and the JSF view state value as
     * parameters. Also register the all JSF <code>submit</code> events to disable the unload event listener.
     * <p>
     * When the view state is not longer than <code>MAX_VIEW_STATE_HEADER_LENGTH</code>, then the request is sent via
     * keepalive fetch when the browser supports it. The keepalive fetch and the synchronous XHR request then carry the
     * same parameters in request headers as well. The server receives the headers even when the browser aborts the
     * request while its body is still in transit.
     * @param viewScopeId The OmniFaces view scope ID.
     */
    export function init(viewScopeId: string) {
        if (!window.XMLHttpRequest) {
            return; // Native XHR not supported (IE6/7 not supported). End of story. Let session expiration do its job.
        }

        if (id == null) {
            if (!Util.getFacesForm()) {
                return;
            }

            const unloadEvent = ("onbeforeunload" in window && !window.onbeforeunload) ? "beforeunload" : ("onpagehide" in window) ? "pagehide" : "unload";
            Util.addEventListener(window, unloadEvent, function() {
                if (disabled) {
                    reenable(); // Just in case some custom JS explicitly triggered submit event while staying in same DOM.
                    return;
                }

                try {
                    const form = Util.getFacesForm();
                    const url = form.action;
                    const viewState = form[VIEW_STATE_PARAM].value;
                    const query = EVENT + "=unload&id=" + id + "&" + VIEW_STATE_PARAM + "=" + encodeURIComponent(viewState);
                    const contentType = "application/x-www-form-urlencoded";
                    const headers: Record<string, string> = {"Content-Type": contentType};
                    const viewStateFitsInHeader = viewState.length <= MAX_VIEW_STATE_HEADER_LENGTH;

                    if (viewStateFitsInHeader) {
                        headers[EVENT_HEADER] = "unload";
                        headers[VIEW_SCOPE_HEADER] = id;
                        headers[VIEW_STATE_HEADER] = viewState;
                    }

                    const sendBeacon = function() {
                        navigator.sendBeacon(url, new Blob([query], {type: contentType}));
                    };

                    if (viewStateFitsInHeader && window.Request && "keepalive" in Request.prototype) {
                        fetch(url, {method: "POST", keepalive: true, credentials: "same-origin", headers: headers, body: query}).catch(function() {
                            if (navigator.sendBeacon) {
                                sendBeacon();
                            }
                        });
                    }
                    else if (navigator.sendBeacon) {
                        sendBeacon();
                    }
                    else {
                        // Fallback to synchronous XHR, even though all browsers anno 2026 are supposed to support sendBeacon.
                        const xhr = new XMLHttpRequest();
                        xhr.open("POST", url, false);
                        xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");

                        for (const name in headers) {
                            if (headers.hasOwnProperty(name)) {
                                xhr.setRequestHeader(name, headers[name]);
                            }
                        }

                        xhr.send(query);
                    }
                }
                catch (e) {
                    // Fail silently. You never know.
                }
            });

            Util.addSubmitListener(function() {
                disable(); // Disable unload event on any submit event.
            });
        }

        id = viewScopeId;
        disabled = false;
    }

    /**
     * Disable the unload event listener on the current document.
     * It will automatically be re-enabled when the DOM has not changed during the unload event.
     */
    export function disable() {
        disabled = true;
    }

    /**
     * Re-enable the unload event listener on the current document.
     */
    export function reenable() {
        disabled = false;
    }

}