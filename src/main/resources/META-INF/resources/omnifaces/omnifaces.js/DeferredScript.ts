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

import { Util } from "./Util";

/**
 * Deferred script loader.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.DeferredScript
 * @since 1.8
 */
export module DeferredScript {

    // Private static fields ------------------------------------------------------------------------------------------

    const deferredScripts: Script[] = [];

    // Private static classes -----------------------------------------------------------------------------------------

    class Script {

        // Private fields ---------------------------------------------------------------------------------------------

        readonly url: string;
        readonly crossorigin: string;
        readonly integrity: string;
        readonly begin: Function;
        readonly success: Function;
        readonly error: Function;

        // Constructor ------------------------------------------------------------------------------------------------

        constructor(url: string, crossorigin: string, integrity: string, begin: Function, success: Function, error: Function) {
            this.url = url;
            this.crossorigin = crossorigin;
            this.integrity = integrity;
            this.begin = begin;
            this.success = success;
            this.error = error;
        }
    }

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Add a deferred script to the loader and registers the onload listener to load the first deferred script.
     * @param url Required; The URL of the deferred script.
     * @param crossorigin Optional; The crossorigin of the deferred script. Defaults to "anonymous".
     * @param integrity Optional; The integrity of the deferred script. Defaults to "".
     * @param begin Optional; Function to invoke before deferred script is loaded.
     * @param success Optional; Function to invoke after deferred script is successfully loaded.
     * @param error Optional; Function to invoke when loading of deferred script failed.
     */
    export function add(url: string, crossorigin: string, integrity: string, begin: Function, success: Function, error: Function) {
        deferredScripts.push(new Script(url, crossorigin, integrity, begin, success, error));

        if (deferredScripts.length == 1) {
            Util.addOnloadListener(function() {
                loadDeferredScript(0);
            });
        }
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Load the deferred script of the given index. When loaded, then it will implicitly load the next deferred script.
     * @param index The index of the deferred script to be loaded. If no one exists, then the method returns.
     */
    function loadDeferredScript(index: number) {
        if (index < 0 || index >= deferredScripts.length) {
            return; // No such script.
        }

        const deferredScript = deferredScripts[index];
        const completeFunction = function() {
            loadDeferredScript(index + 1);
        };

        Util.loadScript(deferredScript.url, deferredScript.crossorigin, deferredScript.integrity, deferredScript.begin, deferredScript.success, deferredScript.error, completeFunction);
    }

}