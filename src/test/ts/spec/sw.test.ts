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
 * Tests for the OmniFaces service worker (sw.unminified.js).
 *
 * The sw.js runs in a ServiceWorkerGlobalScope, not a Window. We test it by:
 * 1. Reading the source and substituting template variables
 * 2. Wrapping in a function scope to avoid const redeclaration issues across tests
 * 3. Mocking the Service Worker APIs (caches, clients, self.addEventListener)
 * 4. Executing the script and capturing registered event listeners
 */

import fs from "fs";
import path from "path";

const SW_JS = path.resolve(__dirname, "../../../../src/main/resources/META-INF/resources/omnifaces/sw.unminified.js");

interface MockCache {
    addAll: jest.Mock;
    match: jest.Mock;
}

interface SwEnv {
    listeners: Record<string, Function>;
    mockCache: MockCache;
    cacheOpen: jest.Mock;
    cacheKeys: jest.Mock;
    cacheDelete: jest.Mock;
    clientsMatchAll: jest.Mock;
    fetchMock: jest.Mock;
    navigatorMock: { onLine: boolean };
    cachesMatch: jest.Mock;
}

function loadServiceWorker(
    cacheVersion = "abc123",
    cacheableResources = '["/app.js", "/style.css"]',
    offlineResource = '"/offline.html"'
): SwEnv {
    const listeners: Record<string, Function> = {};
    const mockCache: MockCache = {
        addAll: jest.fn().mockResolvedValue(undefined),
        match: jest.fn().mockResolvedValue(undefined),
    };
    const cacheOpen = jest.fn().mockResolvedValue(mockCache);
    const cacheKeys = jest.fn().mockResolvedValue([]);
    const cacheDelete = jest.fn().mockResolvedValue(true);
    const clientsMatchAll = jest.fn().mockResolvedValue([]);
    const fetchMock = jest.fn();
    const navigatorMock = { onLine: true };
    const cachesMatch = jest.fn().mockResolvedValue(undefined);

    let source = fs.readFileSync(SW_JS, "utf-8");
    source = source
        .replaceAll("$cacheVersion", cacheVersion)
        .replaceAll("$cacheableResources", cacheableResources)
        .replaceAll("$offlineResource", offlineResource);

    // Wrap in IIFE to avoid const redeclaration across tests, and inject mocked globals.
    const wrappedSource = `
        (function(self, caches, clients, navigator, fetch) {
            var addEventListener = function(event, handler) { listeners[event] = handler; };
            self.addEventListener = addEventListener;
            self.clients = clients;
            self.location = { origin: "http://localhost" };
            ${source.replace(/\bself\.addEventListener\b/g, "addEventListener")}
        })(selfMock, cachesMock, clientsMock, navigatorMock, fetchMock);
    `;

    // Execute via Function to provide controlled scope
    const fn = new Function(
        "listeners", "selfMock", "cachesMock", "clientsMock", "navigatorMock", "fetchMock",
        wrappedSource
    );

    fn(
        listeners,
        { location: { origin: "http://localhost" } },
        { open: cacheOpen, keys: cacheKeys, match: cachesMatch, delete: cacheDelete },
        { matchAll: clientsMatchAll },
        navigatorMock,
        fetchMock
    );

    return { listeners, mockCache, cacheOpen, cacheKeys, cacheDelete, clientsMatchAll, fetchMock, navigatorMock, cachesMatch };
}

describe("sw.js: install event", () => {

    test("registers install event listener", () => {
        const env = loadServiceWorker();
        expect(env.listeners["install"]).toBeDefined();
    });

    test("opens cache with correct name on install", async () => {
        const env = loadServiceWorker("v1");
        const waitUntilPromise = new Promise<void>(resolve => {
            env.listeners["install"]({
                waitUntil: (p: Promise<unknown>) => p.then(() => resolve()),
            });
        });
        await waitUntilPromise;

        expect(env.cacheOpen).toHaveBeenCalledWith("omnifaces.v1");
        expect(env.mockCache.addAll).toHaveBeenCalledWith(["/app.js", "/style.css"]);
    });
});

describe("sw.js: activate event", () => {

    test("registers activate event listener", () => {
        const env = loadServiceWorker();
        expect(env.listeners["activate"]).toBeDefined();
    });

    test("deletes old omnifaces caches but keeps current", async () => {
        const env = loadServiceWorker("currentV");
        env.cacheKeys.mockResolvedValue(["omnifaces.oldV", "omnifaces.currentV", "other-cache"]);

        const waitUntilPromise = new Promise<void>(resolve => {
            env.listeners["activate"]({
                waitUntil: (p: Promise<unknown>) => p.then(() => resolve()),
            });
        });
        await waitUntilPromise;

        expect(env.cacheDelete).toHaveBeenCalledWith("omnifaces.oldV");
        expect(env.cacheDelete).not.toHaveBeenCalledWith("omnifaces.currentV");
        expect(env.cacheDelete).not.toHaveBeenCalledWith("other-cache");
    });
});

describe("sw.js: fetch event", () => {

    test("registers fetch event listener", () => {
        const env = loadServiceWorker();
        expect(env.listeners["fetch"]).toBeDefined();
    });
});

function createFetchEvent(url: string, method = "GET", mode = "navigate"): { request: { url: string; method: string; mode: string }; respondWith: jest.Mock } {
    return {
        request: { url, method, mode },
        respondWith: jest.fn(),
    };
}

describe("sw.js: fetch handler behavior", () => {

    test("ignores cross-origin requests", () => {
        const env = loadServiceWorker();
        const event = createFetchEvent("http://other-origin.com/resource.js");
        env.listeners["fetch"](event);
        expect(event.respondWith).not.toHaveBeenCalled();
    });

    test("ignores same-origin GET that is not navigate and not a resource", () => {
        const env = loadServiceWorker();
        const event = createFetchEvent("http://localhost/api/data", "GET", "cors");
        env.listeners["fetch"](event);
        expect(event.respondWith).not.toHaveBeenCalled();
    });

    test("responds to navigate GET requests (network-first)", async () => {
        const env = loadServiceWorker();
        const mockResponse = { status: 200 };
        env.fetchMock.mockResolvedValue(mockResponse);
        env.cachesMatch.mockResolvedValue(undefined);

        const event = createFetchEvent("http://localhost/page.html", "GET", "navigate");
        env.listeners["fetch"](event);

        expect(event.respondWith).toHaveBeenCalledTimes(1);
        const result = await event.respondWith.mock.calls[0][0];
        expect(result).toBe(mockResponse);
    });

    test("responds to resource GET requests with cache-first", async () => {
        const env = loadServiceWorker();
        const cachedResponse = { status: 200, body: "cached" };
        env.cachesMatch.mockResolvedValue(cachedResponse);
        env.fetchMock.mockResolvedValue({ status: 200, body: "fresh" });

        const event = createFetchEvent("http://localhost/jakarta.faces.resource/style.css", "GET", "cors");
        env.listeners["fetch"](event);

        expect(event.respondWith).toHaveBeenCalledTimes(1);
        const result = await event.respondWith.mock.calls[0][0];
        expect(result).toBe(cachedResponse);
    });

    test("resource GET falls back to network when not cached", async () => {
        const env = loadServiceWorker();
        const freshResponse = { status: 200, body: "fresh" };
        env.cachesMatch.mockResolvedValue(undefined);
        env.fetchMock.mockResolvedValue(freshResponse);

        const event = createFetchEvent("http://localhost/jakarta.faces.resource/app.js", "GET", "cors");
        env.listeners["fetch"](event);

        expect(event.respondWith).toHaveBeenCalledTimes(1);
        const result = await event.respondWith.mock.calls[0][0];
        expect(result).toBe(freshResponse);
    });

    test("strips v parameter from URL before cache matching", async () => {
        const env = loadServiceWorker();
        const mockResponse = { status: 200 };
        env.fetchMock.mockResolvedValue(mockResponse);
        env.cachesMatch.mockResolvedValue(undefined);

        const event = createFetchEvent("http://localhost/page.html?v=1.0&other=keep", "GET", "navigate");
        env.listeners["fetch"](event);

        expect(event.respondWith).toHaveBeenCalledTimes(1);
        // The URL passed to caches.match should not contain the v parameter
        const matchedUrl = env.cachesMatch.mock.calls[0][0];
        expect(matchedUrl).not.toContain("v=1.0");
        expect(matchedUrl).toContain("other=keep");
    });

    test("POST online sends online event", async () => {
        const mockClient = { postMessage: jest.fn() };
        const env = loadServiceWorker();
        env.clientsMatchAll.mockResolvedValue([mockClient]);
        env.navigatorMock.onLine = true;

        const event = createFetchEvent("http://localhost/form-submit", "POST", "navigate");
        env.listeners["fetch"](event);

        // POST does not call respondWith
        expect(event.respondWith).not.toHaveBeenCalled();

        // Wait for async clientsMatchAll
        await new Promise(resolve => setTimeout(resolve, 10));

        expect(mockClient.postMessage).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "omnifaces.event",
                name: "omnifaces.online",
            })
        );
    });

    test("POST offline sends offline event", async () => {
        const mockClient = { postMessage: jest.fn() };
        const env = loadServiceWorker();
        env.clientsMatchAll.mockResolvedValue([mockClient]);
        env.navigatorMock.onLine = false;

        const event = createFetchEvent("http://localhost/form-submit", "POST", "navigate");
        env.listeners["fetch"](event);

        expect(event.respondWith).not.toHaveBeenCalled();

        await new Promise(resolve => setTimeout(resolve, 10));

        expect(mockClient.postMessage).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "omnifaces.event",
                name: "omnifaces.offline",
            })
        );
    });

    test("navigate GET sends online event on successful fetch", async () => {
        const mockClient = { postMessage: jest.fn() };
        const env = loadServiceWorker();
        env.clientsMatchAll.mockResolvedValue([mockClient]);
        env.fetchMock.mockResolvedValue({ status: 200 });
        env.cachesMatch.mockResolvedValue(undefined);

        const event = createFetchEvent("http://localhost/page.html", "GET", "navigate");
        env.listeners["fetch"](event);

        // Wait for the respondWith promise chain to complete
        await event.respondWith.mock.calls[0][0];
        await new Promise(resolve => setTimeout(resolve, 10));

        expect(mockClient.postMessage).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "omnifaces.event",
                name: "omnifaces.online",
            })
        );
    });

    test("navigate GET sends offline event and returns offlineResource on network failure when offline", async () => {
        const mockClient = { postMessage: jest.fn() };
        const offlinePage = { status: 200, body: "offline page" };
        const env = loadServiceWorker("v1", '["/app.js"]', '"/offline.html"');
        env.clientsMatchAll.mockResolvedValue([mockClient]);
        env.navigatorMock.onLine = false;
        env.fetchMock.mockRejectedValue(new Error("Network error"));
        env.cachesMatch.mockImplementation((url: string) => {
            if (url === "/offline.html") {
                return Promise.resolve(offlinePage);
            }
            return Promise.resolve(undefined);
        });

        const event = createFetchEvent("http://localhost/page.html", "GET", "navigate");
        env.listeners["fetch"](event);

        const result = await event.respondWith.mock.calls[0][0];
        expect(result).toBe(offlinePage);

        await new Promise(resolve => setTimeout(resolve, 10));

        expect(mockClient.postMessage).toHaveBeenCalledWith(
            expect.objectContaining({
                type: "omnifaces.event",
                name: "omnifaces.offline",
            })
        );
    });

    test("navigate GET throws error on network failure when online (transient error)", async () => {
        const env = loadServiceWorker();
        env.navigatorMock.onLine = true;
        const networkError = new Error("Transient network error");
        env.fetchMock.mockRejectedValue(networkError);
        env.cachesMatch.mockResolvedValue(undefined);

        const event = createFetchEvent("http://localhost/page.html", "GET", "navigate");
        env.listeners["fetch"](event);

        await expect(event.respondWith.mock.calls[0][0]).rejects.toThrow("Transient network error");
    });

    test("resource GET returns cached response on network failure when offline", async () => {
        const env = loadServiceWorker();
        const cachedResponse = { status: 200, body: "cached resource" };
        env.navigatorMock.onLine = false;
        env.fetchMock.mockRejectedValue(new Error("Network error"));
        env.cachesMatch.mockResolvedValue(cachedResponse);

        const event = createFetchEvent("http://localhost/jakarta.faces.resource/style.css", "GET", "cors");
        env.listeners["fetch"](event);

        const result = await event.respondWith.mock.calls[0][0];
        expect(result).toBe(cachedResponse);
    });
});
