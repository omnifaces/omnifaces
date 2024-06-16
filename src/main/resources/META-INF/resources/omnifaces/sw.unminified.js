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
 * <li><code>$cacheableResources</code> - JS array representing URLs of cacheable resources.</li>
 * <li><code>$offlineResource</code> - JS string representing URL of offline resource.</li>
 * </ul>
 * 
 * @author Bauke Scholtz
 * @since 3.7
 * @see PWAResourceHandler
 * @see <a href="https://css-tricks.com/serviceworker-for-offline/">https://css-tricks.com/serviceworker-for-offline/</a>
 */
var cacheName = "omnifaces.4.1"; // Should be bumped every time this sw.unminified.js logic is changed.
var cacheableResources = $cacheableResources;
var offlineResource = $offlineResource;

/**
 * Add all cacheable resources.
 */
self.addEventListener("install", function(event) {
    event.waitUntil(caches.open(cacheName).then(function(event) {
        return event.addAll(cacheableResources);
    }));
});

/**
 * Offline-aware fetch.
 */
self.addEventListener("fetch", function(event) {
    var request = event.request;
    var method = request.method;
    var url = request.url.replace(new RegExp("([?&])v=.*?([&#]|$)", "i"), "$2"); // Strips the v= parameter indicating the cache bust version.
    var sendEvent = function(name, detail) {
        self.clients.matchAll().then(function(clients) {
            clients.forEach(function(client) {
                client.postMessage({
                    type: "omnifaces.event",
                    name: name,
                    detail: detail
                });
            });
        });
    };

    function sendOnlineEvent() {
        sendEvent("omnifaces.online", {
            method: method,
            url: url
        });
    }

    function sendOfflineEvent(error) {
        sendEvent("omnifaces.offline", {
            method: method,
            url: url,
            error: error
        });
    }

    if (method == "GET") {
        var navigated = event.request.mode == "navigate";
        var resource = url.indexOf("/jakarta.faces.resource/") > -1;

        if (navigated || resource) {
            event.respondWith(caches.match(url).then(function(cached) {
                var fetched = fetch(request).then(fetchedFromNetwork, unableToResolve).catch(unableToResolve);
                return navigated ? fetched : (cached || fetched);

                function fetchedFromNetwork(response) {
                    if (navigated) {
                        sendOnlineEvent();
                    }
                    
                    return response;
                }
                
                function unableToResolve(error) {
                    if (navigated) {
                        sendOfflineEvent(error);
                        
                        if (offlineResource) {
                            return caches.match(offlineResource);
                        }
                    }
                    
                    return cached;
                }
            }));
        }
    }
    else if (method == "POST") { // Do not cache! Merely check if online or offline. This works with Faces because its POST requests are by default postback.
        fetch(url + (url.indexOf("?") > -1 ? "&" : "?") + "omnifaces.event=sw.js").then(sendOnlineEvent, sendOfflineEvent).catch(sendOfflineEvent);
    }
});

/**
 * Prune old caches.
 */
self.addEventListener("activate", function(event) {
    event.waitUntil(caches.keys().then(function(keys) {
        return Promise.all(keys.filter(function(key) {
            return key.startsWith("omnifaces.") && key != cacheName;
        }).map(function(key) {
            return caches.delete(key);
        }));
    }));
});
