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
 * Hash param handling.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.HashParam
 * @since 3.2
 */
export module HashParam {

    // Private static fields ------------------------------------------------------------------------------------------

    let id: string;
    let updating: boolean;

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * On page load, send any hash parameters to the bean.
     */
    export function init(hashParamId: string) {
        id = hashParamId;

        if (!Util.getFacesForm()) {
            return;
        }

        if (!!window.location.hash) {
            setHashParamValues();
        }
        
        Util.addEventListener(window, "hashchange", setHashParamValues);
    }

    /**
     * Update a parameter in current hash string, which simulates a query string.
     * @param name The name of the parameter to update. If it doesn't exist, then it will be added.
     * @param value The value of the parameter to update. If it is falsey, then it will be removed.
     */
    export function update(name: string, value: string) {
        updating = true;
        const location = window.location;
        let oldHashQueryString = location.hash;

        if (!!oldHashQueryString && oldHashQueryString.charAt(0) == '#') {
            oldHashQueryString = oldHashQueryString.substring(1);
        }

        const newHashQueryString = Util.updateParameter(oldHashQueryString, name, value);

        if (newHashQueryString != oldHashQueryString) {
            const history = window.history;
            if (history && history.pushState) {
                const url = location.href.split(/#/, 2)[0] + (newHashQueryString ? "#" : "") + newHashQueryString;
                history.pushState(null, document.title, url);
            }
            if (location.hash != newHashQueryString) { // Not only used when there's no window.history but also as a work around for buggy window.history implementations.
                location.hash = newHashQueryString;
            }
        }

        updating = false;
    }

    // Private static functions ---------------------------------------------------------------------------------------

    function setHashParamValues() {
        if (!updating) {
            const params: Record<string, string> = { execute: id, hash: window.location.hash.substring(1) };
            params[EVENT] = "setHashParamValues";
            const faces = window.faces || window.jsf;
            faces.ajax.request(Util.getFacesForm(), null, params);
        }
    }

}