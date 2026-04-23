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
import { Util } from "./Util";

/**
 * Lazy panel handling.
 *
 * @author Bauke Scholtz
 * @see org.omnifaces.component.output.LazyPanel
 * @since 5.3
 */
export namespace LazyPanel {

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Schedules a viewport intersection listener on the lazy panel placeholder wrapper with the given client id. As soon as the wrapper intersects the viewport,
     * a single <code>loadLazyPanel</code> ajax request is fired to trigger server-side rendering of its children.
     * @param clientId The client id of the lazy panel component and the DOM id of the placeholder wrapper.
     * @param extraParams Optional map of extra request parameters to send along with the ajax request, typically generated from nested <code>&lt;f:param&gt;</code>
     *   or <code>&lt;o:param&gt;</code> children. Passed through as <code>options.params</code> so that they land in the request args without colliding with
     *   the reserved <code>execute</code>/<code>render</code>/etc. faces options.
     */
    export function init(clientId: string, extraParams?: Record<string, unknown>) {
        Util.addOnloadListener(() => observe(clientId, extraParams));
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Register a one-shot intersection listener on the element with the given client id. No-op when the element does not exist.
     */
    function observe(clientId: string, extraParams?: Record<string, unknown>) {
        const target = document.getElementById(clientId);

        if (!target) {
            return;
        }

        Util.addIntersectionListener(() => [target], () => trigger(clientId, extraParams), { once: true });
    }

    /**
     * Fire the <code>loadLazyPanel</code> ajax request for the given client id so the server can render the children. No-op when there is no Faces form on the
     * page; an error will be logged by {@link Util.getFacesForm} in that case.
     */
    function trigger(clientId: string, extraParams?: Record<string, unknown>) {
        const form = Util.getFacesForm();

        if (!form) {
            return;
        }

        const options: Record<string, unknown> = { execute: clientId, render: clientId, params: extraParams ?? {} };
        options[EVENT] = "loadLazyPanel";
        window.faces.ajax.request(form, null, options);
    }

}
