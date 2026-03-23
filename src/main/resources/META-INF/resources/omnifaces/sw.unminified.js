/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/**
 * Service worker template. Following variables must be substituted:
 * <ul>
 * <li><code>$cacheVersion</code> - hex hash of cacheable resources, used as cache version.</li>
 * <li><code>$cacheableResources</code> - JS array representing URLs of cacheable resources.</li>
 * <li><code>$offlineResource</code> - JS string representing URL of offline resource.</li>
 * </ul>
 * 
 * @author Bauke Scholtz
 * @since 3.7
 * @see PWAResourceHandler
 * @see <a href="https://css-tricks.com/serviceworker-for-offline/">https://css-tricks.com/serviceworker-for-offline/</a>
 */
const cacheName = "omnifaces.$cacheVersion";
const cacheableResources = $cacheableResources;
const offlineResource = $offlineResource;

/**
 * Add all cacheable resources.
 */
self.addEventListener("install", function(event) {
    event.waitUntil(caches.open(cacheName).then(cache => cache.addAll(cacheableResources)));
});

/**
 * Offline-aware fetch.
 */
self.addEventListener("fetch", function(event) {
    const request = event.request;
    const requestURL = new URL(request.url);

    if (requestURL.origin !== self.location.origin) {
        return; // Not our resource.
    }

    requestURL.searchParams.delete('v'); // Removes the v= parameter usually indicating the cache bust version (VersionedResourceHandler, OmniVersionResourceHandler, PrimeResourceHandler, etc).
    const url = requestURL.toString();
    const method = request.method;
    const sendEvent = (name, detail) => {
        self.clients.matchAll().then(clients => {
            clients.forEach(client => {
                client.postMessage({
                    type: "omnifaces.event",
                    name,
                    detail
                });
            });
        });
    };

    const sendOnlineEvent = () => {
        sendEvent("omnifaces.online", { method, url });
    };

    const sendOfflineEvent = (error) => {
        sendEvent("omnifaces.offline", { method, url, error });
    };

    if (method == "GET") {
        const navigated = event.request.mode == "navigate";
        const resource = url.includes("/jakarta.faces.resource/");

        if (navigated || resource) {
            event.respondWith(caches.match(url).then(function(cached) {
                const fetched = fetch(request).then(fetchedFromNetwork, unableToResolve).catch(unableToResolve);
                return navigated ? fetched : (cached || fetched);

                function fetchedFromNetwork(response) {
                    if (navigated) {
                        sendOnlineEvent();
                    }

                    return response;
                }

                function unableToResolve(error) {
                    if (!navigator.onLine) {
                        if (navigated) {
                            sendOfflineEvent(error);

                            if (offlineResource) {
                                return caches.match(offlineResource);
                            }
                        }

                        return cached;
                    }
                    else { // Let browser handle the error; don't evict cache as this may be a transient network issue.
                        throw error;
                    }
                }
            }));
        }
    }
    else if (method == "POST") { // Do not cache! Merely fire online/offline events. This works with Faces because its POST requests are by default postback.
        if (navigator.onLine) {
            sendOnlineEvent();
        }
        else {
            sendOfflineEvent(new Error("The network connection has been lost."));
        }
    }
});

/**
 * Handle notification click events; relay to client pages.
 */
self.addEventListener("notificationclick", function(event) {
    event.notification.close();

    self.clients.matchAll({ includeUncontrolled: true, type: "window" }).then(clients => {
        clients.forEach(client => {
            client.postMessage({
                type: "omnifaces.event",
                name: "omnifaces.notificationclick",
                detail: { tag: event.notification.tag, data: event.notification.data }
            });
        });
    });

    if (event.notification.data && event.notification.data.url) {
        event.waitUntil(self.clients.openWindow(event.notification.data.url));
    }
});

/**
 * Handle notification close events; relay to client pages.
 */
self.addEventListener("notificationclose", function(event) {
    self.clients.matchAll({ includeUncontrolled: true, type: "window" }).then(clients => {
        clients.forEach(client => {
            client.postMessage({
                type: "omnifaces.event",
                name: "omnifaces.notificationclose",
                detail: { tag: event.notification.tag, data: event.notification.data }
            });
        });
    });
});

/**
 * Prune old caches.
 */
self.addEventListener("activate", function(event) {
    event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith("omnifaces.") && key !== cacheName).map(key => caches.delete(key)))));
});
