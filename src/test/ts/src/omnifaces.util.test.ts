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
 * Tests for the OmniFaces.Util namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { createFacesForm } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const util = () => OmniFaces.Util as Record<string, Function>;

// ---- addEventListener / removeEventListener ----

describe("OmniFaces.Util.addEventListener", () => {

    test("adds event listener to target", () => {
        const target = document.createElement("div");
        const listener = jest.fn();
        util().addEventListener(target, "click", listener);
        target.dispatchEvent(new Event("click"));
        expect(listener).toHaveBeenCalledTimes(1);
    });

    test("handles space-separated event names", () => {
        const target = document.createElement("div");
        const listener = jest.fn();
        util().addEventListener(target, "click input", listener);
        target.dispatchEvent(new Event("click"));
        target.dispatchEvent(new Event("input"));
        expect(listener).toHaveBeenCalledTimes(2);
    });

    test("trims whitespace around event names", () => {
        const target = document.createElement("div");
        const listener = jest.fn();
        util().addEventListener(target, "  click  ", listener);
        target.dispatchEvent(new Event("click"));
        expect(listener).toHaveBeenCalledTimes(1);
    });

    test("handles multiple spaces between event names", () => {
        const target = document.createElement("div");
        const listener = jest.fn();
        util().addEventListener(target, "click   input", listener);
        target.dispatchEvent(new Event("click"));
        target.dispatchEvent(new Event("input"));
        expect(listener).toHaveBeenCalledTimes(2);
    });
});

describe("OmniFaces.Util.removeEventListener", () => {

    test("removes previously added event listener", () => {
        const target = document.createElement("div");
        const listener = jest.fn();
        util().addEventListener(target, "click", listener);
        util().removeEventListener(target, "click", listener);
        target.dispatchEvent(new Event("click"));
        expect(listener).not.toHaveBeenCalled();
    });

    test("removes multiple event listeners at once", () => {
        const target = document.createElement("div");
        const listener = jest.fn();
        util().addEventListener(target, "click input", listener);
        util().removeEventListener(target, "click input", listener);
        target.dispatchEvent(new Event("click"));
        target.dispatchEvent(new Event("input"));
        expect(listener).not.toHaveBeenCalled();
    });
});

// ---- addOnloadListener ----

describe("OmniFaces.Util.addOnloadListener", () => {

    afterEach(() => {
        delete (window as any).jQuery;
    });

    test("uses jQuery when available", () => {
        const listener = jest.fn();
        (window as any).jQuery = jest.fn();
        util().addOnloadListener(listener);
        expect((window as any).jQuery).toHaveBeenCalledWith(listener);
        expect(listener).not.toHaveBeenCalled(); // jQuery delegates, doesn't call directly
    });

    test("uses setTimeout when document is complete and no jQuery", () => {
        jest.useFakeTimers();
        const listener = jest.fn();
        // jsdom readyState is typically "complete"
        Object.defineProperty(document, "readyState", { value: "complete", writable: true, configurable: true });
        util().addOnloadListener(listener);
        expect(listener).not.toHaveBeenCalled(); // Deferred via setTimeout
        jest.runAllTimers();
        expect(listener).toHaveBeenCalledTimes(1);
        jest.useRealTimers();
    });

    test("adds window load listener when document is not complete and no jQuery", () => {
        Object.defineProperty(document, "readyState", { value: "loading", writable: true, configurable: true });
        const listener = jest.fn();
        util().addOnloadListener(listener);
        window.dispatchEvent(new Event("load"));
        expect(listener).toHaveBeenCalledTimes(1);
        Object.defineProperty(document, "readyState", { value: "complete", writable: true, configurable: true });
    });
});

// ---- addSubmitListener ----

describe("OmniFaces.Util.addSubmitListener", () => {

    afterEach(() => {
        delete (window as any).mojarra;
        delete (window as any).myfaces;
        delete (window as any).PrimeFaces;
    });

    test("adds submit event listener to document", () => {
        const listener = jest.fn();
        util().addSubmitListener(listener);
        document.dispatchEvent(new Event("submit"));
        expect(listener).toHaveBeenCalledTimes(1);
    });

    test("decorates Mojarra cljs when mojarra is present", () => {
        const originalCljs = jest.fn();
        (window as any).mojarra = { cljs: originalCljs };
        const listener = jest.fn();
        util().addSubmitListener(listener);

        (window as any).mojarra.cljs("form1", "param1");
        expect(listener).toHaveBeenCalledTimes(1);
        expect(originalCljs).toHaveBeenCalledWith("form1", "param1");
    });

    test("decorates MyFaces oam.submitForm when myfaces is present", () => {
        const originalSubmitForm = jest.fn();
        (window as any).myfaces = { oam: { submitForm: originalSubmitForm } };
        const listener = jest.fn();
        util().addSubmitListener(listener);

        (window as any).myfaces.oam.submitForm("form1");
        expect(listener).toHaveBeenCalledTimes(1);
        expect(originalSubmitForm).toHaveBeenCalledWith("form1");
    });

    test("decorates PrimeFaces addSubmitParam when PrimeFaces is present", () => {
        const originalAddSubmitParam = jest.fn();
        (window as any).PrimeFaces = { addSubmitParam: originalAddSubmitParam };
        const listener = jest.fn();
        util().addSubmitListener(listener);

        (window as any).PrimeFaces.addSubmitParam("form1", { key: "value" });
        expect(listener).toHaveBeenCalledTimes(1);
        expect(originalAddSubmitParam).toHaveBeenCalledWith("form1", { key: "value" });
    });

    test("skips decoration when Faces impl submit function does not exist", () => {
        (window as any).mojarra = {}; // No cljs function
        const listener = jest.fn();
        expect(() => util().addSubmitListener(listener)).not.toThrow();
    });
});

// ---- resolveFunction ----

describe("OmniFaces.Util.resolveFunction", () => {

    afterEach(() => {
        delete (window as any).__testFunc;
    });

    test("returns the function if already a function", () => {
        const fn = () => {};
        expect(util().resolveFunction(fn)).toBe(fn);
    });

    test("resolves string to global function", () => {
        const fn = () => "resolved";
        (window as any).__testFunc = fn;
        expect(util().resolveFunction("__testFunc")).toBe(fn);
    });

    test("returns NOOP for nonexistent global function name", () => {
        const resolved = util().resolveFunction("nonExistentFunction");
        expect(typeof resolved).toBe("function");
        expect(resolved()).toBeUndefined(); // NOOP
    });

    test("returns NOOP for undefined", () => {
        const resolved = util().resolveFunction(undefined);
        expect(typeof resolved).toBe("function");
        expect(resolved()).toBeUndefined();
    });

    test("returns NOOP for null", () => {
        const resolved = util().resolveFunction(null);
        expect(typeof resolved).toBe("function");
        expect(resolved()).toBeUndefined();
    });
});

// ---- getFacesForm ----

describe("OmniFaces.Util.getFacesForm", () => {

    let form: HTMLFormElement;

    afterEach(() => {
        form?.remove();
        delete (window as any).faces;
    });

    test("returns form containing ViewState input", () => {
        form = createFacesForm();
        const result = util().getFacesForm();
        expect(result).toBe(form);
    });

    test("returns null and logs error when no Faces form exists", () => {
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        const result = util().getFacesForm();
        expect(result).toBeNull();
        expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining("Cannot find a Faces form"));
        errorSpy.mockRestore();
    });

    test("returns null and logs error when faces is present and project stage is Development", () => {
        (window as any).faces = { getProjectStage: () => "Development" };
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        const result = util().getFacesForm();
        expect(result).toBeNull();
        expect(errorSpy).toHaveBeenCalled();
        errorSpy.mockRestore();
    });

    test("returns null without logging when faces is present and project stage is Production", () => {
        (window as any).faces = { getProjectStage: () => "Production" };
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        const result = util().getFacesForm();
        expect(result).toBeNull();
        expect(errorSpy).not.toHaveBeenCalled();
        errorSpy.mockRestore();
    });

    test("returns first form with ViewState when multiple forms exist", () => {
        const form1 = document.createElement("form");
        document.body.appendChild(form1);
        form = createFacesForm("facesForm");
        const result = util().getFacesForm();
        expect(result).toBe(form);
        form1.remove();
    });
});

// ---- updateParameter ----

describe("OmniFaces.Util.updateParameter", () => {

    test("adds parameter to empty query string", () => {
        const result = util().updateParameter("", "name", "value");
        expect(result).toBe("name=value");
    });

    test("updates existing parameter", () => {
        const result = util().updateParameter("name=old", "name", "new");
        expect(result).toBe("name=new");
    });

    test("adds parameter to existing query string", () => {
        const result = util().updateParameter("a=1", "b", "2");
        expect(result).toContain("a=1");
        expect(result).toContain("b=2");
    });

    test("removes parameter when value is falsey", () => {
        const result = util().updateParameter("a=1&b=2", "a", "");
        expect(result).not.toContain("a=");
        expect(result).toContain("b=2");
    });

    test("encodes special characters", () => {
        const result = util().updateParameter("", "key", "a&b=c");
        expect(result).toContain("key=a%26b%3Dc");
    });

    test("handles parameter with no value in existing query", () => {
        const result = util().updateParameter("a=1&b=2&c=3", "b", "updated");
        expect(result).toContain("b=updated");
    });
});

// ---- loadScript ----

describe("OmniFaces.Util.loadScript", () => {

    afterEach(() => {
        const scripts = document.head.querySelectorAll("script[src]");
        scripts.forEach(s => s.remove());
    });

    test("creates script element with correct attributes", () => {
        jest.useFakeTimers();
        util().loadScript("/test.js", "anonymous", "sha256-abc", null, null, null, null);
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/test.js']") as HTMLScriptElement;
        expect(script).not.toBeNull();
        expect(script.async).toBe(true);
        expect(script.getAttribute("crossorigin")).toBe("anonymous");
        expect(script.getAttribute("integrity")).toBe("sha256-abc");
    });

    test("omits crossorigin and integrity attributes when empty", () => {
        jest.useFakeTimers();
        util().loadScript("/test.js", "", "", null, null, null, null);
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/test.js']") as HTMLScriptElement;
        expect(script.hasAttribute("crossorigin")).toBe(false);
        expect(script.hasAttribute("integrity")).toBe(false);
    });

    test("calls begin function before appending script", () => {
        jest.useFakeTimers();
        const begin = jest.fn();
        util().loadScript("/test.js", "", "", begin, null, null, null);
        jest.runAllTimers();
        jest.useRealTimers();

        expect(begin).toHaveBeenCalledTimes(1);
    });

    test("calls success and complete on script load", () => {
        jest.useFakeTimers();
        const success = jest.fn();
        const complete = jest.fn();
        util().loadScript("/test.js", "", "", null, success, null, complete);
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/test.js']") as HTMLScriptElement;
        script.onload?.(new Event("load"));

        expect(success).toHaveBeenCalledTimes(1);
        expect(complete).toHaveBeenCalledTimes(1);
    });

    test("calls error and complete on script error", () => {
        jest.useFakeTimers();
        const error = jest.fn();
        const complete = jest.fn();
        util().loadScript("/test.js", "", "", null, null, error, complete);
        jest.runAllTimers();
        jest.useRealTimers();

        const script = document.head.querySelector("script[src='/test.js']") as HTMLScriptElement;
        script.onerror?.(new Event("error"));

        expect(error).toHaveBeenCalledTimes(1);
        expect(complete).toHaveBeenCalledTimes(1);
    });

    test("resolves string function names for callbacks", () => {
        jest.useFakeTimers();
        (window as any).__testBegin = jest.fn();
        util().loadScript("/test.js", "", "", "__testBegin", null, null, null);
        jest.runAllTimers();
        jest.useRealTimers();

        expect((window as any).__testBegin).toHaveBeenCalledTimes(1);
        delete (window as any).__testBegin;
    });
});

// ---- addIntersectionListener ----

describe("OmniFaces.Util.addIntersectionListener (IntersectionObserver path)", () => {

    type ObserverCallback = (entries: Partial<IntersectionObserverEntry>[], self: IntersectionObserver) => void;
    let observerCallback: ObserverCallback;
    let observed: Element[];
    let unobserved: Element[];
    let disconnectCount: number;
    let originalIO: typeof IntersectionObserver | undefined;

    beforeEach(() => {
        observed = [];
        unobserved = [];
        disconnectCount = 0;
        originalIO = window.IntersectionObserver;
        (window as any).IntersectionObserver = class {
            constructor(cb: ObserverCallback) {
                observerCallback = cb;
            }
            observe(el: Element) { observed.push(el); }
            unobserve(el: Element) { unobserved.push(el); }
            disconnect() { disconnectCount++; }
        };
    });

    afterEach(() => {
        (window as any).IntersectionObserver = originalIO;
        document.body.innerHTML = "";
    });

    test("observes each target returned by getTargets", () => {
        const a = document.createElement("div");
        const b = document.createElement("div");
        document.body.append(a, b);

        util().addIntersectionListener(() => [a, b], jest.fn());

        expect(observed).toEqual([a, b]);
    });

    test("invokes callback and unobserves intersecting target when once is false", () => {
        const target = document.createElement("div");
        document.body.appendChild(target);
        const onIntersect = jest.fn();

        util().addIntersectionListener(() => [target], onIntersect);
        observerCallback([{ isIntersecting: true, target }], { disconnect: () => disconnectCount++, unobserve: (el: Element) => unobserved.push(el) } as any);

        expect(onIntersect).toHaveBeenCalledWith(target);
        expect(unobserved).toEqual([target]);
        expect(disconnectCount).toBe(0);
    });

    test("disconnects observer after first intersection when once is true", () => {
        const target = document.createElement("div");
        document.body.appendChild(target);
        const onIntersect = jest.fn();

        util().addIntersectionListener(() => [target], onIntersect, { once: true });
        observerCallback([{ isIntersecting: true, target }], { disconnect: () => disconnectCount++, unobserve: (el: Element) => unobserved.push(el) } as any);

        expect(onIntersect).toHaveBeenCalledWith(target);
        expect(disconnectCount).toBe(1);
        expect(unobserved).toEqual([]);
    });

    test("ignores entries where isIntersecting is false", () => {
        const target = document.createElement("div");
        document.body.appendChild(target);
        const onIntersect = jest.fn();

        util().addIntersectionListener(() => [target], onIntersect);
        observerCallback([{ isIntersecting: false, target }], { disconnect: () => disconnectCount++, unobserve: (el: Element) => unobserved.push(el) } as any);

        expect(onIntersect).not.toHaveBeenCalled();
        expect(unobserved).toEqual([]);
        expect(disconnectCount).toBe(0);
    });
});

describe("OmniFaces.Util.addIntersectionListener (scroll/resize fallback)", () => {

    let originalIO: typeof IntersectionObserver | undefined;

    beforeEach(() => {
        jest.useFakeTimers();
        originalIO = window.IntersectionObserver;
        (window as any).IntersectionObserver = undefined;
        Object.defineProperty(window, "innerHeight", { value: 1000, writable: true, configurable: true });
        Object.defineProperty(window, "pageYOffset", { value: 0, writable: true, configurable: true });
    });

    afterEach(() => {
        jest.useRealTimers();
        (window as any).IntersectionObserver = originalIO;
        document.body.innerHTML = "";
    });

    function makeTarget(offsetTop: number): HTMLElement {
        const el = document.createElement("div");
        Object.defineProperty(el, "offsetTop", { value: offsetTop, configurable: true });
        document.body.appendChild(el);
        return el;
    }

    test("fires callback for targets below threshold on scroll and removes listeners when once triggers", () => {
        const target = makeTarget(500);
        const onIntersect = jest.fn();

        util().addIntersectionListener(() => [target], onIntersect, { once: true });

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(onIntersect).toHaveBeenCalledWith(target);

        // Listeners must be removed: a second scroll should not re-fire.
        onIntersect.mockClear();
        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(onIntersect).not.toHaveBeenCalled();
    });

    test("does not fire when target is beyond viewport threshold", () => {
        makeTarget(5000);
        const onIntersect = jest.fn();

        util().addIntersectionListener(() => Array.from(document.querySelectorAll("div")) as HTMLElement[], onIntersect);

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(onIntersect).not.toHaveBeenCalled();
    });

    test("keeps listening while getTargets() still returns pending targets (non-once)", () => {
        // Simulate GraphicImage-style mutation: getTargets filters out elements marked "done".
        const a = makeTarget(100);
        const b = makeTarget(5000); // Not yet in viewport
        const getTargets = () => Array.from(document.querySelectorAll<HTMLElement>("div:not([data-done])"));
        const onIntersect = jest.fn((el: HTMLElement) => el.setAttribute("data-done", ""));

        util().addIntersectionListener(getTargets, onIntersect);

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(onIntersect).toHaveBeenCalledWith(a);
        expect(onIntersect).toHaveBeenCalledTimes(1);

        // Bring b into viewport and scroll again — listeners must still be attached.
        Object.defineProperty(b, "offsetTop", { value: 200, configurable: true });
        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(onIntersect).toHaveBeenCalledWith(b);
        expect(onIntersect).toHaveBeenCalledTimes(2);

        // Now getTargets() is empty — a final scroll must be a no-op and listeners must be removed.
        onIntersect.mockClear();
        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(onIntersect).not.toHaveBeenCalled();
    });

    test("throttles rapid scroll events via setTimeout", () => {
        const target = makeTarget(100);
        const onIntersect = jest.fn();

        util().addIntersectionListener(() => [target], onIntersect, { once: true });

        document.dispatchEvent(new Event("scroll"));
        document.dispatchEvent(new Event("scroll"));
        document.dispatchEvent(new Event("scroll"));
        expect(onIntersect).not.toHaveBeenCalled(); // Still throttled

        jest.runAllTimers();
        expect(onIntersect).toHaveBeenCalledTimes(1);
    });
});
