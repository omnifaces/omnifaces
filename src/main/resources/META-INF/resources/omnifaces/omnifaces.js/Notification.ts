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
        requireInteraction?: boolean;
        silent?: boolean;
        icon?: string;
        onclick?: Function;
        onclose?: Function;
        onerror?: Function;
        onpermissionchange?: Function;
        onunsupported?: Function;
    }

    // Private static fields ------------------------------------------------------------------------------------------

    const channels = new Map<string, InitOptions>();
    let eventHandlersRegistered = false;

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Initialize notification support for a specific channel.
     * Called from the component's encodeChildren().
     * @param channel The channel name.
     * @param options Default notification options for this channel.
     */
    export function init(channel: string, options: InitOptions) {
        channels.set(channel, options);

        if (!window.Notification || !navigator.serviceWorker) {
            Util.resolveFunction(options.onunsupported)();
            return;
        }

        registerEventHandlers();
    }

    /**
     * Request notification permission. Must be called from a user gesture (click handler).
     * @param channel The channel name to notify on permission change.
     * @return A promise resolving to "granted", "denied", or "default".
     */
    export async function requestPermission(channel?: string): Promise<string> {
        if (!window.Notification) {
            return Promise.resolve("denied");
        }

        return window.Notification.requestPermission().then(function(permission) {
            if (channel) {
                Util.resolveFunction(channels.get(channel)?.onpermissionchange)(permission);
            }
            else {
                channels.forEach(function(options) {
                    Util.resolveFunction(options.onpermissionchange)(permission);
                });
            }

            return permission;
        });
    }

    /**
     * Show a notification programmatically.
     * @param channel The channel name.
     * @param title The notification title.
     * @param body Optional notification body text.
     * @param url Optional URL to navigate to when the notification is clicked.
     */
    export function show(channel: string, title: string, body?: string, url?: string) {
        if (!channel) {
            throw new Error("OmniFaces.Notification.show: channel is required.");
        }

        if (!title) {
            throw new Error("OmniFaces.Notification.show: title is required.");
        }

        if (!window.Notification || window.Notification.permission !== "granted" || !navigator.serviceWorker) {
            return;
        }

        const defaults = channels.get(channel) ?? {};

        navigator.serviceWorker.ready.then(function(registration) {
            registration.showNotification(title, {
                tag: defaults.tag,
                body,
                requireInteraction: defaults.requireInteraction,
                silent: defaults.silent,
                icon: defaults.icon,
                data: { channel, url: url ?? undefined }
            });
        });
    }

    /**
     * Handle a server-pushed notification message. This is the hardwired SSE onmessage handler for
     * <code>&lt;o:notification&gt;</code>.
     * @param message The message object with title, body, and optional url.
     * @param channel The channel name, passed by Push.ts as the second onmessage argument.
     */
    export function handle(message: { title: string; body?: string; url?: string }, channel: string) {
        show(channel, message.title, message.body, message.url);
    }

    /**
     * Get the current notification permission status.
     * @return "granted", "denied", "default", or "unsupported".
     */
    export function getPermission(): string {
        return window.Notification ? window.Notification.permission : "unsupported";
    }

    // Private static functions ---------------------------------------------------------------------------------------

    function registerEventHandlers() {
        if (eventHandlersRegistered) {
            return;
        }

        eventHandlersRegistered = true;

        window.addEventListener("omnifaces.notificationclick", function(event: CustomEvent) {
            const options = channels.get(event.detail.data?.channel);
            Util.resolveFunction(options?.onclick)(event);
        });

        window.addEventListener("omnifaces.notificationclose", function(event: CustomEvent) {
            const options = channels.get(event.detail.data?.channel);
            Util.resolveFunction(options?.onclose)(event);
        });
    }

}
