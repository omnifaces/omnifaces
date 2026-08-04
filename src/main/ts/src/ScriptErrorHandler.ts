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

/**
 * Catch uncaught JavaScript errors and unhandled promise rejections and send them to the server via
 * <code>navigator.sendBeacon()</code>. This script is used by <code>&lt;o:scriptErrorHandler&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.ScriptErrorHandler
 * @since 5.2
 */
export namespace ScriptErrorHandler {

    // Private static fields ------------------------------------------------------------------------------------------

    const config = {} as { endpointURL: string; ignoreSelector: string | null; maxRecentErrors: number; errorExpiry: number };
    const recentErrors: { key: string; time: number }[] = [];

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Initialize the script error handler. This registers <code>window.onerror</code> and
     * <code>unhandledrejection</code> listeners that send error details to the server.
     * @param endpointURL The full URL of the script error handler servlet endpoint.
     * @param ignoreSelector Optional CSS selector; if a matching element exists in the document, the error will not be reported.
     * @param maxRecentErrors Maximum number of recent errors to keep for deduplication.
     * @param errorExpiry Time in milliseconds after which a recent error entry expires.
     */
    export function init(endpointURL: string, ignoreSelector: string | null, maxRecentErrors: number, errorExpiry: number) {
        config.endpointURL = endpointURL;
        config.ignoreSelector = ignoreSelector;
        config.maxRecentErrors = maxRecentErrors;
        config.errorExpiry = errorExpiry;

        window.onerror = (message, source, line, column, error) => {
            send({
                pageURL: window.location.href,
                errorMessage: String(message),
                errorName: error?.name ?? null,
                errorStack: error?.stack ?? null,
                sourceURL: source ?? null,
                lineNumber: line != null ? String(line) : null,
                columnNumber: column != null ? String(column) : null
            });

            return false; // Allow the browser's default error handler to continue, e.g. logging to console.
        };

        window.addEventListener("unhandledrejection", (event: PromiseRejectionEvent) => {
            const reason = event.reason;

            send({
                pageURL: window.location.href,
                errorMessage: reason instanceof Error ? reason.message : String(reason),
                errorName: reason instanceof Error ? reason.name : "UnhandledRejection",
                errorStack: reason instanceof Error ? reason.stack ?? null : null
            });
        });
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Send error details to the server via <code>navigator.sendBeacon()</code> with deduplication.
     * @param params The error detail parameters.
     */
    function send(params: Record<string, string | null>) {
        try {
            if (config.ignoreSelector && document.querySelector(config.ignoreSelector)) {
                return;
            }

            const key = `${params.errorMessage}${params.sourceURL}${params.lineNumber}`;
            const now = Date.now();

            while (recentErrors.length > 0 && now - recentErrors[0].time > config.errorExpiry) {
                recentErrors.shift();
            }

            if (recentErrors.some(entry => entry.key === key)) {
                return;
            }

            if (recentErrors.length >= config.maxRecentErrors) {
                recentErrors.shift();
            }

            recentErrors.push({ key, time: now });

            const data = new URLSearchParams();

            for (const [name, value] of Object.entries(params)) {
                if (value != null) {
                    data.append(name, value);
                }
            }

            navigator.sendBeacon(config.endpointURL, data);
        }
        catch (e) {
            // Fail silently. We don't want to cause additional errors while reporting errors.
        }
    }

}
