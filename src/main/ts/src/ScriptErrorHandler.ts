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

    // "Constant" fields ----------------------------------------------------------------------------------------------

    const MAX_BEACON_LENGTH = 60000; // navigator.sendBeacon() silently refuses a larger payload.
    const TRUNCATION_MARKER = "...";
    const TRIMMABLE_PARAMS = ["errorStack", "errorMessage"]; // In the order they are sacrificed to stay within the maximum.

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
                errorMessage: toErrorMessage(message, error),
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
                errorMessage: toReasonMessage(reason),
                errorName: reason instanceof Error ? reason.name : "UnhandledRejection",
                errorStack: reason instanceof Error ? reason.stack ?? null : null
            });
        });
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Returns the error message of the given <code>window.onerror</code> arguments. The message is a string when the
     * handler is assigned as property, but a single <code>ErrorEvent</code> carrying the message when it is invoked as
     * event listener, which is how another error handler forwarding to this one would call it. The handler is a public
     * global which anything may call with anything, so every remaining shape falls back to the event type and then to
     * a plain stringification, which cannot throw.
     * @param message The message argument of the onerror handler.
     * @param error The error argument of the onerror handler, if any.
     */
    function toErrorMessage(message: Event | string, error?: Error): string {
        if (typeof message === "string") {
            return message;
        }

        if (error) {
            return error.message;
        }

        if (message instanceof ErrorEvent && message.message) {
            return message.message;
        }

        if (message instanceof Event) {
            return `${message.constructor.name}: ${message.type}`;
        }

        return String(message);
    }

    /**
     * Returns the error message of the given unhandled rejection reason. A rejection commonly carries a plain object
     * holding the interesting detail, which is therefore serialized rather than stringified to "[object Object]".
     * @param reason The reason of the unhandled rejection.
     */
    function toReasonMessage(reason: any): string {
        if (reason instanceof Error) {
            return reason.message;
        }

        if (typeof reason !== "object" || reason === null) {
            return String(reason); // A string reason must stay unquoted.
        }

        try {
            return JSON.stringify(reason) ?? String(reason);
        }
        catch (e) {
            return String(reason); // A circular structure or a BigInt cannot be serialized.
        }
    }

    /**
     * Returns the given error detail parameters as beacon payload, skipping the ones without a value.
     * @param params The error detail parameters.
     */
    function toBeaconData(params: Record<string, string | null>): URLSearchParams {
        const data = new URLSearchParams();

        for (const [name, value] of Object.entries(params)) {
            if (value != null) {
                data.append(name, value);
            }
        }

        return data;
    }

    /**
     * Trims the given error detail parameters so that the encoded payload stays within {@link MAX_BEACON_LENGTH}.
     * Cutting as many characters as the payload is over is always enough, as URL encoding only ever expands.
     * @param params The error detail parameters.
     */
    function trimToMaxLength(params: Record<string, string | null>) {
        for (const name of TRIMMABLE_PARAMS) {
            const excess = toBeaconData(params).toString().length - MAX_BEACON_LENGTH;

            if (excess <= 0) {
                return;
            }

            const value = params[name];

            if (value != null) {
                params[name] = value.substring(0, Math.max(0, value.length - excess - TRUNCATION_MARKER.length)) + TRUNCATION_MARKER;
            }
        }
    }

    /**
     * Send error details to the server via <code>navigator.sendBeacon()</code> with deduplication.
     * @param params The error detail parameters.
     */
    function send(params: Record<string, string | null>) {
        try {
            if (config.ignoreSelector && document.querySelector(config.ignoreSelector)) {
                return;
            }

            trimToMaxLength(params);

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
            navigator.sendBeacon(config.endpointURL, toBeaconData(params));
        }
        catch (e) {
            // Fail silently. We don't want to cause additional errors while reporting errors.
        }
    }

}
