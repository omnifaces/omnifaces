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
 * Manage browser notifications via the Web Notifications API using the service worker.
 * This script is used by <code>&lt;o:notification&gt;</code>.
 *
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.push.Notification
 * @since 5.2
 */
export namespace Notification {

    // Private types --------------------------------------------------------------------------------------------------

    interface InitOptions {
        tag?: string;
        icon?: string;
        requireInteraction?: boolean;
        silent?: boolean;
        onclick?: Function;
        onclose?: Function;
        onerror?: Function;
        onpermissionchange?: Function;
        onunsupported?: Function;
    }

    interface ShowOptions {
        body?: string;
        icon?: string;
        tag?: string;
        requireInteraction?: boolean;
        silent?: boolean;
        data?: any;
    }

    // Private static fields ------------------------------------------------------------------------------------------

    let defaultOptions: InitOptions = {};
    let eventHandlersRegistered = false;

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Initialize notification support.
     * Called from the component's encodeChildren().
     * @param options Default notification options.
     */
    export function init(options: InitOptions) {
        defaultOptions = options;

        if (!("Notification" in window) || !navigator.serviceWorker) {
            Util.resolveFunction(options.onunsupported)();
            return;
        }

        registerEventHandlers();
    }

    /**
     * Request notification permission. Must be called from a user gesture (click handler).
     * @return A promise resolving to "granted", "denied", or "default".
     */
    export async function requestPermission(): Promise<string> {
        if (!window.Notification) {
            return Promise.resolve("denied");
        }

        return window.Notification.requestPermission().then(function(permission) {
            Util.resolveFunction(defaultOptions.onpermissionchange)(permission);
            return permission;
        });
    }

    /**
     * Show a notification programmatically.
     * @param title The notification title.
     * @param options Optional notification options.
     */
    export function show(title: string, options?: ShowOptions) {
        if (!window.Notification || window.Notification.permission !== "granted" || !navigator.serviceWorker) {
            return;
        }

        navigator.serviceWorker.ready.then(function(registration) {
            registration.showNotification(title, {
                body: options?.body,
                icon: options?.icon ?? defaultOptions.icon,
                tag: options?.tag ?? defaultOptions.tag,
                requireInteraction: options?.requireInteraction ?? defaultOptions.requireInteraction,
                silent: options?.silent ?? defaultOptions.silent,
                data: options?.data
            });
        });
    }

    /**
     * Show a notification from a server-pushed message. This is the hardwired SSE onmessage handler for
     * <code>&lt;o:notification&gt;</code>.
     * @param message The message object with title, body, and optional url.
     */
    export function showMessage(message: { title: string; body?: string; url?: string }) {
        show(message.title, {
            body: message.body,
            data: message.url ? { url: message.url } : undefined
        });
    }

    /**
     * Get the current notification permission status.
     * @return "granted", "denied", "default", or "unsupported".
     */
    export function getPermission(): string {
        return ("Notification" in window) ? window.Notification.permission : "unsupported";
    }

    // Private static functions ---------------------------------------------------------------------------------------

    function registerEventHandlers() {
        if (eventHandlersRegistered) {
            return;
        }

        eventHandlersRegistered = true;

        window.addEventListener("omnifaces.notificationclick", function(event: CustomEvent) {
            Util.resolveFunction(defaultOptions.onclick)(event);
        });

        window.addEventListener("omnifaces.notificationclose", function(event: CustomEvent) {
            Util.resolveFunction(defaultOptions.onclose)(event);
        });
    }

}
