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
 * Tests for the OmniFaces.ScriptErrorHandler namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { installMockBeacon, uninstallMockBeacon, getBeaconCalls, lastBeaconCall } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const scriptErrorHandler = () => OmniFaces.ScriptErrorHandler as Record<string, Function>;

describe("OmniFaces.ScriptErrorHandler.init", () => {

    beforeEach(() => {
        installMockBeacon();
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
    });

    test("sets window.onerror handler", () => {
        scriptErrorHandler().init("/error-endpoint", null, 10, 60000);
        expect(window.onerror).not.toBeNull();
        expect(typeof window.onerror).toBe("function");
    });

    test("registers unhandledrejection listener", () => {
        const addEventSpy = jest.spyOn(window, "addEventListener");
        scriptErrorHandler().init("/error-endpoint", null, 10, 60000);
        expect(addEventSpy).toHaveBeenCalledWith("unhandledrejection", expect.any(Function));
        addEventSpy.mockRestore();
    });
});

describe("OmniFaces.ScriptErrorHandler: error reporting", () => {

    beforeEach(() => {
        installMockBeacon();
        scriptErrorHandler().init("/error-endpoint", null, 10, 60000);
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
    });

    test("sends error details via navigator.sendBeacon", () => {
        window.onerror!("Test error", "test.js", 42, 10, new Error("Test error"));

        expect(getBeaconCalls().length).toBe(1);
        expect(lastBeaconCall().url).toBe("/error-endpoint");
    });

    test("sends correct error parameters", () => {
        const error = new Error("Something failed");
        error.stack = "Error: Something failed\n    at test.js:42:10";
        window.onerror!("Something failed", "test.js", 42, 10, error);

        const beacon = lastBeaconCall();
        const data = beacon.data as URLSearchParams;
        expect(data.get("errorMessage")).toBe("Something failed");
        expect(data.get("sourceURL")).toBe("test.js");
        expect(data.get("lineNumber")).toBe("42");
        expect(data.get("columnNumber")).toBe("10");
        expect(data.get("errorName")).toBe("Error");
        expect(data.get("errorStack")).toContain("Something failed");
    });

    test("returns false from onerror to allow default browser handling", () => {
        const result = window.onerror!("Test error", "test.js", 1, 1, new Error("Test error"));
        expect(result).toBe(false);
    });

    test("omits null parameters from beacon data", () => {
        window.onerror!("Test error", null as any, null as any, null as any, null as any);

        const data = lastBeaconCall().data as URLSearchParams;
        expect(data.has("errorMessage")).toBe(true);
        expect(data.has("sourceURL")).toBe(false);
    });
});

describe("OmniFaces.ScriptErrorHandler: deduplication", () => {

    beforeEach(() => {
        installMockBeacon();
        scriptErrorHandler().init("/error-endpoint", null, 10, 60000);
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
    });

    test("deduplicates identical errors", () => {
        window.onerror!("Same error", "same.js", 10, 1, new Error("Same error"));
        window.onerror!("Same error", "same.js", 10, 1, new Error("Same error"));

        expect(getBeaconCalls().length).toBe(1);
    });

    test("reports different errors separately", () => {
        window.onerror!("Error A", "a.js", 10, 1, new Error("Error A"));
        window.onerror!("Error B", "b.js", 20, 1, new Error("Error B"));

        expect(getBeaconCalls().length).toBe(2);
    });

    test("reports same message from different source/line as separate errors", () => {
        window.onerror!("Same msg", "a.js", 10, 1, new Error("Same msg"));
        window.onerror!("Same msg", "a.js", 20, 1, new Error("Same msg"));

        expect(getBeaconCalls().length).toBe(2);
    });
});

describe("OmniFaces.ScriptErrorHandler: ignoreSelector", () => {

    beforeEach(() => {
        installMockBeacon();
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
        document.querySelectorAll(".ignore-errors").forEach(el => el.remove());
    });

    test("suppresses error when matching element exists", () => {
        scriptErrorHandler().init("/error-endpoint", ".ignore-errors", 10, 60000);

        const marker = document.createElement("div");
        marker.className = "ignore-errors";
        document.body.appendChild(marker);

        window.onerror!("Suppressed error", "test.js", 1, 1, new Error("Suppressed error"));
        expect(getBeaconCalls().length).toBe(0);
    });

    test("reports error when no matching element exists", () => {
        scriptErrorHandler().init("/error-endpoint", ".ignore-errors", 10, 60000);

        window.onerror!("Reported error", "test.js", 1, 1, new Error("Reported error"));
        expect(getBeaconCalls().length).toBe(1);
    });
});

describe("OmniFaces.ScriptErrorHandler: maxRecentErrors", () => {

    beforeEach(() => {
        installMockBeacon();
        scriptErrorHandler().init("/error-endpoint", null, 3, 60000);
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
    });

    test("evicts oldest entry when maxRecentErrors is exceeded", () => {
        // Fill up to max
        window.onerror!("Error 1", "a.js", 1, 1, new Error("Error 1"));
        window.onerror!("Error 2", "b.js", 2, 1, new Error("Error 2"));
        window.onerror!("Error 3", "c.js", 3, 1, new Error("Error 3"));
        expect(getBeaconCalls().length).toBe(3);

        // New error should evict oldest and be reported
        window.onerror!("Error 4", "d.js", 4, 1, new Error("Error 4"));
        expect(getBeaconCalls().length).toBe(4);
    });
});

describe("OmniFaces.ScriptErrorHandler: errorExpiry", () => {

    beforeEach(() => {
        installMockBeacon();
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
    });

    test("re-reports error after expiry period", () => {
        scriptErrorHandler().init("/error-endpoint", null, 10, 100); // 100ms expiry

        window.onerror!("Expiring error", "exp.js", 1, 1, new Error("Expiring error"));
        expect(getBeaconCalls().length).toBe(1);

        // Duplicate within expiry
        window.onerror!("Expiring error", "exp.js", 1, 1, new Error("Expiring error"));
        expect(getBeaconCalls().length).toBe(1);

        // Advance time past expiry
        const origDateNow = Date.now;
        Date.now = () => origDateNow() + 200;
        window.onerror!("Expiring error", "exp.js", 1, 1, new Error("Expiring error"));
        expect(getBeaconCalls().length).toBe(2);
        Date.now = origDateNow;
    });
});

// PromiseRejectionEvent is not available in jsdom, so polyfill it for testing.
if (typeof PromiseRejectionEvent === "undefined") {
    (globalThis as any).PromiseRejectionEvent = class PromiseRejectionEvent extends Event {
        readonly reason: any;
        readonly promise: Promise<any>;
        constructor(type: string, init: { reason?: any; promise: Promise<any> }) {
            super(type);
            this.reason = init.reason;
            this.promise = init.promise;
        }
    };
}

describe("OmniFaces.ScriptErrorHandler: unhandledrejection", () => {

    beforeEach(() => {
        installMockBeacon();
        scriptErrorHandler().init("/error-endpoint", null, 10, 60000);
    });

    afterEach(() => {
        uninstallMockBeacon();
        window.onerror = null;
    });

    test("sends Error reason with message, name, and stack", () => {
        const error = new TypeError("Cannot read property 'x' of null");

        window.dispatchEvent(new PromiseRejectionEvent("unhandledrejection", {
            reason: error,
            promise: Promise.reject(error).catch(() => {}),
        }));

        expect(getBeaconCalls().length).toBe(1);
        const data = lastBeaconCall().data as URLSearchParams;
        expect(data.get("errorMessage")).toBe("Cannot read property 'x' of null");
        expect(data.get("errorName")).toBe("TypeError");
        expect(data.has("errorStack")).toBe(true);
        expect(data.get("pageURL")).toBe(window.location.href);
    });

    test("sends non-Error reason as string with UnhandledRejection name", () => {
        window.dispatchEvent(new PromiseRejectionEvent("unhandledrejection", {
            reason: "something went wrong",
            promise: Promise.reject("something went wrong").catch(() => {}),
        }));

        expect(getBeaconCalls().length).toBe(1);
        const data = lastBeaconCall().data as URLSearchParams;
        expect(data.get("errorMessage")).toBe("something went wrong");
        expect(data.get("errorName")).toBe("UnhandledRejection");
        expect(data.has("errorStack")).toBe(false);
    });

    test("sends object reason stringified with UnhandledRejection name", () => {
        const reason = { code: 500 };
        window.dispatchEvent(new PromiseRejectionEvent("unhandledrejection", {
            reason,
            promise: Promise.reject(reason).catch(() => {}),
        }));

        expect(getBeaconCalls().length).toBe(1);
        const data = lastBeaconCall().data as URLSearchParams;
        expect(data.get("errorMessage")).toBe("[object Object]");
        expect(data.get("errorName")).toBe("UnhandledRejection");
    });

    test("deduplicates identical rejection messages", () => {
        for (let i = 0; i < 2; i++) {
            window.dispatchEvent(new PromiseRejectionEvent("unhandledrejection", {
                reason: "duplicate rejection",
                promise: Promise.reject("duplicate rejection").catch(() => {}),
            }));
        }

        expect(getBeaconCalls().length).toBe(1);
    });
});
