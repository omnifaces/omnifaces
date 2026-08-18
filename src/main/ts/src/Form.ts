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

import { CLIENT_WINDOW_PARAM, VIEW_STATE_PARAM } from "./OmniFaces";
import { Util } from "./Util";

/**
 * Form partial submit.
 *
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.Form
 * @since 3.0
 */
export namespace Form {

    // Private static functions ---------------------------------------------------------------------------------------

    function init() {
        const faces = window.faces;

        if (faces) {
            const originalAjaxRequest = faces.ajax.request;

            faces.ajax.request = function(source: HTMLElement | string, event: any, options: any) {
                const originalGetViewState = faces.getViewState;

                faces.getViewState = function(form: HTMLFormElement) {
                    const originalViewState = originalGetViewState(form);

                    if (form.dataset["partialsubmit"] !== "true") {
                        return originalViewState;
                    }

                    const execute = options?.execute;

                    if (!execute || execute.includes("@form") || execute.includes("@all")) {
                        return originalViewState;
                    }

                    let executeIds: string[] = [];
                    let encodedExecuteIds: string[] = [];

                    if (!execute.includes("@none")) {
                        const sourceId = source instanceof HTMLElement ? source.id : source;
                        executeIds = execute.replace("@this", sourceId).split(" ");
                        encodedExecuteIds = executeIds.map(encodeURIComponent);
                    }

                    encodedExecuteIds.push(VIEW_STATE_PARAM, CLIENT_WINDOW_PARAM);

                    const partialViewState: string[] = [];

                    originalViewState.replace(/([^=&]+)=([^&]*)/g, function(_entry: any, key: string, value: string) {
                        if (encodedExecuteIds.includes(key) || containsNamedChild(executeIds, key)) {
                            partialViewState.push(`${key}=${value}`);
                        }
                    });

                    return partialViewState.join("&");
                };

                originalAjaxRequest(source, event, options);
            };
        }
    }

    function containsNamedChild(executeIds: string[], key: string) {
        const name = key.replace(/%3A/g, String.raw`\:`);

        try {
            for (const executeId of executeIds) {
                const parent = document.getElementById(executeId);

                if (parent?.querySelector("[name='" + name + "']")) {
                    return true;
                }
            }
        }
        catch (e) {
            console.warn(`Cannot determine if ${executeIds} contains child ${name}`, e);
        }

        return false;
    }

    // Global initialization ------------------------------------------------------------------------------------------

    Util.addOnloadListener(init);

}
