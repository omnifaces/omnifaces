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
 * Tests for the OmniFaces.DeferredScript namespace.
 *
 * DeferredScript uses module-level state (deferredScripts array) that persists across tests
 * within the same jsdom instance. The onload listener is only registered on the first add() call.
 * Util.loadScript defers via addOnloadListener → setTimeout (since document.readyState is "complete"),
 * requiring jest.runAllTimers() to process nested timeouts.
 */

import { loadOmniFacesJs } from "../test-setup";

beforeAll(() => loadOmniFacesJs());

const deferredScript = () => OmniFaces.DeferredScript as Record<string, Function>;

describe("OmniFaces.DeferredScript.add", () => {

    test("creates script element with correct src via loadScript", () => {
        jest.useFakeTimers();
        deferredScript().add("/ds-first.js", "", "", null, null, null);
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/ds-first.js']");
        expect(script).not.toBeNull();
    });

    test("sets crossorigin and integrity attributes on created script", () => {
        // Must complete previous script's onload to continue the chain
        const prevScript = document.head.querySelector("script[src='/ds-first.js']") as HTMLScriptElement;

        jest.useFakeTimers();
        deferredScript().add("/ds-attrs.js", "use-credentials", "sha384-abc", null, null, null);

        // Fire previous script's onload to trigger the chain
        prevScript?.onload?.(new Event("load"));
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/ds-attrs.js']") as HTMLScriptElement;
        expect(script).not.toBeNull();
        expect(script.getAttribute("crossorigin")).toBe("use-credentials");
        expect(script.getAttribute("integrity")).toBe("sha384-abc");
    });

    test("calls begin callback when script loads in chain", () => {
        const prevScript = document.head.querySelector("script[src='/ds-attrs.js']") as HTMLScriptElement;

        jest.useFakeTimers();
        const begin = jest.fn();
        deferredScript().add("/ds-begin.js", "", "", begin, null, null);

        prevScript?.onload?.(new Event("load"));
        jest.runAllTimers();
        jest.useRealTimers();

        expect(begin).toHaveBeenCalledTimes(1);
    });

    test("calls success and complete on script onload", () => {
        const prevScript = document.head.querySelector("script[src='/ds-begin.js']") as HTMLScriptElement;

        jest.useFakeTimers();
        const success = jest.fn();
        deferredScript().add("/ds-success.js", "", "", null, success, null);

        prevScript?.onload?.(new Event("load"));
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/ds-success.js']") as HTMLScriptElement;
        expect(script).not.toBeNull();

        // Fire this script's onload
        script.onload?.(new Event("load"));
        expect(success).toHaveBeenCalledTimes(1);
    });

    test("calls error callback on script onerror and continues chain", () => {
        const prevScript = document.head.querySelector("script[src='/ds-success.js']") as HTMLScriptElement;

        jest.useFakeTimers();
        const error = jest.fn();
        deferredScript().add("/ds-error.js", "", "", null, null, error);
        deferredScript().add("/ds-after-error.js", "", "", null, null, null);

        prevScript?.onload?.(new Event("load"));
        jest.runAllTimers();
        jest.useRealTimers();

        const errorScript = document.head.querySelector("script[src='/ds-error.js']") as HTMLScriptElement;
        expect(errorScript).not.toBeNull();

        // Simulate error on this script — should call error callback and continue to next
        jest.useFakeTimers();
        errorScript.onerror?.(new Event("error"));
        jest.runAllTimers();
        jest.useRealTimers();

        expect(error).toHaveBeenCalledTimes(1);

        const afterErrorScript = document.head.querySelector("script[src='/ds-after-error.js']");
        expect(afterErrorScript).not.toBeNull();
    });
});
