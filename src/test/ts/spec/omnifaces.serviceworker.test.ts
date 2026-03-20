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
 * Tests for the OmniFaces.ServiceWorker namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { installMockServiceWorker, uninstallMockServiceWorker, MockServiceWorkerContainer } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const serviceWorker = () => OmniFaces.ServiceWorker as Record<string, Function>;

describe("OmniFaces.ServiceWorker.init", () => {

    let mockSW: MockServiceWorkerContainer;

    afterEach(() => {
        uninstallMockServiceWorker();
    });

    test("registers service worker with URL and scope", () => {
        mockSW = installMockServiceWorker();
        serviceWorker().init("/sw.js", "/app/");
        expect(mockSW.register).toHaveBeenCalledWith("/sw.js", { scope: "/app/" });
    });

    test("adds message event listener", () => {
        mockSW = installMockServiceWorker();
        serviceWorker().init("/sw.js", "/");
        expect(mockSW.addEventListener).toHaveBeenCalledWith("message", expect.any(Function));
    });

    test("dispatches window event on omnifaces.event message", () => {
        mockSW = installMockServiceWorker();
        serviceWorker().init("/sw.js", "/");

        const handler = jest.fn();
        window.addEventListener("omnifaces.online", handler);

        mockSW.simulateMessage({
            type: "omnifaces.event",
            name: "omnifaces.online",
            detail: { method: "GET", url: "/page" },
        });

        expect(handler).toHaveBeenCalledTimes(1);
        const event = handler.mock.calls[0][0] as CustomEvent;
        expect(event.detail).toEqual({ method: "GET", url: "/page" });

        window.removeEventListener("omnifaces.online", handler);
    });

    test("ignores messages without omnifaces.event type", () => {
        mockSW = installMockServiceWorker();
        serviceWorker().init("/sw.js", "/");

        const handler = jest.fn();
        window.addEventListener("someEvent", handler);

        mockSW.simulateMessage({ type: "other.event", name: "someEvent" });
        expect(handler).not.toHaveBeenCalled();

        window.removeEventListener("someEvent", handler);
    });

    test("ignores messages with no data", () => {
        mockSW = installMockServiceWorker();
        serviceWorker().init("/sw.js", "/");
        expect(() => mockSW.simulateMessage(null)).not.toThrow();
    });

    test("does nothing when service workers are not supported", () => {
        const origSW = navigator.serviceWorker;
        Object.defineProperty(navigator, "serviceWorker", {
            value: undefined,
            writable: true,
            configurable: true,
        });

        expect(() => serviceWorker().init("/sw.js", "/")).not.toThrow();

        Object.defineProperty(navigator, "serviceWorker", {
            value: origSW,
            writable: true,
            configurable: true,
        });
    });
});
