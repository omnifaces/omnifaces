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
 * Tests for the OmniFaces.LazyPanel namespace (viewport-triggered ajax rendering).
 * LazyPanel.init() schedules observation via Util.addOnloadListener, which in jsdom
 * (readyState === "complete") defers via setTimeout. Every test therefore uses fake
 * timers and flushes them after init() before asserting observer/listener behavior.
 */

import { loadOmniFacesJs } from "../test-setup";
import { createFacesForm, installMockFaces, uninstallMockFaces } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const lazyPanel = () => OmniFaces.LazyPanel as Record<string, Function>;

type ObserverCallback = (entries: Partial<IntersectionObserverEntry>[], self: IntersectionObserver) => void;

interface MockObserverInstance {
    observed: Element[];
    disconnectCount: number;
    fireIntersection(target: Element): void;
}

let activeObserver: MockObserverInstance | null;
let originalIO: typeof IntersectionObserver | undefined;

function installMockIntersectionObserver(): void {
    activeObserver = null;
    originalIO = window.IntersectionObserver;
    (window as any).IntersectionObserver = class {
        private callback: ObserverCallback;
        private instance: MockObserverInstance;

        constructor(cb: ObserverCallback) {
            this.callback = cb;
            this.instance = {
                observed: [],
                disconnectCount: 0,
                fireIntersection: (target: Element) => {
                    this.callback(
                        [{ isIntersecting: true, target }],
                        this as unknown as IntersectionObserver,
                    );
                },
            };
            activeObserver = this.instance;
        }
        observe(el: Element) { this.instance.observed.push(el); }
        unobserve(_el: Element) { /* not used by LazyPanel */ }
        disconnect() { this.instance.disconnectCount++; }
    };
}

function uninstallMockIntersectionObserver(): void {
    (window as any).IntersectionObserver = originalIO;
    activeObserver = null;
}

describe("OmniFaces.LazyPanel.init (IntersectionObserver path)", () => {

    let form: HTMLFormElement;

    beforeEach(() => {
        jest.useFakeTimers();
        installMockIntersectionObserver();
    });

    afterEach(() => {
        jest.useRealTimers();
        uninstallMockIntersectionObserver();
        form?.remove();
        document.querySelectorAll("[data-lazy-panel]").forEach(el => el.remove());
        uninstallMockFaces();
    });

    test("observes the target element identified by clientId", () => {
        const target = document.createElement("div");
        target.id = "lp1";
        target.setAttribute("data-lazy-panel", "");
        document.body.appendChild(target);

        lazyPanel().init("lp1");
        jest.runAllTimers();

        expect(activeObserver).not.toBeNull();
        expect(activeObserver!.observed).toEqual([target]);
    });

    test("does not create observer when target element is missing", () => {
        lazyPanel().init("doesNotExist");
        jest.runAllTimers();

        expect(activeObserver).toBeNull();
    });

    test("triggers ajax with loadLazyPanel event on intersection", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        const target = document.createElement("div");
        target.id = "lp2";
        target.setAttribute("data-lazy-panel", "");
        document.body.appendChild(target);

        lazyPanel().init("lp2");
        jest.runAllTimers();
        activeObserver!.fireIntersection(target);

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const [source, event, options] = mockFaces.ajax.request.mock.calls[0];
        expect(source).toBe(form);
        expect(event).toBeNull();
        expect(options.execute).toBe("lp2");
        expect(options.render).toBe("lp2");
        expect(options["omnifaces.event"]).toBe("loadLazyPanel");
        expect(options.params).toEqual({});
    });

    test("passes extra params via options.params when init is called with a second arg", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        const target = document.createElement("div");
        target.id = "lpParams";
        target.setAttribute("data-lazy-panel", "");
        document.body.appendChild(target);

        lazyPanel().init("lpParams", { productId: "42", filter: "active" });
        jest.runAllTimers();
        activeObserver!.fireIntersection(target);

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const options = mockFaces.ajax.request.mock.calls[0][2];
        expect(options.params).toEqual({ productId: "42", filter: "active" });
        // Reserved top-level options must stay on top-level, not inside params.
        expect(options.execute).toBe("lpParams");
        expect(options.render).toBe("lpParams");
        expect(options["omnifaces.event"]).toBe("loadLazyPanel");
    });

    test("disconnects observer after first intersection", () => {
        installMockFaces();
        form = createFacesForm();
        const target = document.createElement("div");
        target.id = "lp3";
        target.setAttribute("data-lazy-panel", "");
        document.body.appendChild(target);

        lazyPanel().init("lp3");
        jest.runAllTimers();
        activeObserver!.fireIntersection(target);

        expect(activeObserver!.disconnectCount).toBe(1);
    });

    test("does not trigger ajax when no Faces form is present", () => {
        const mockFaces = installMockFaces();
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        const target = document.createElement("div");
        target.id = "lp4";
        target.setAttribute("data-lazy-panel", "");
        document.body.appendChild(target);

        lazyPanel().init("lp4");
        jest.runAllTimers();
        activeObserver!.fireIntersection(target);

        expect(mockFaces.ajax.request).not.toHaveBeenCalled();
        errorSpy.mockRestore();
    });

    test("multiple init calls register independent observers", () => {
        const a = document.createElement("div");
        a.id = "lpA";
        a.setAttribute("data-lazy-panel", "");
        const b = document.createElement("div");
        b.id = "lpB";
        b.setAttribute("data-lazy-panel", "");
        document.body.append(a, b);

        lazyPanel().init("lpA");
        jest.runAllTimers();
        const firstObserver = activeObserver;
        lazyPanel().init("lpB");
        jest.runAllTimers();
        const secondObserver = activeObserver;

        expect(firstObserver).not.toBe(secondObserver);
        expect(firstObserver!.observed).toEqual([a]);
        expect(secondObserver!.observed).toEqual([b]);
    });
});

describe("OmniFaces.LazyPanel.init (scroll/resize fallback)", () => {

    let form: HTMLFormElement;

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
        form?.remove();
        document.querySelectorAll("[data-lazy-panel]").forEach(el => el.remove());
        uninstallMockFaces();
    });

    function makeTarget(id: string, offsetTop: number): HTMLElement {
        const el = document.createElement("div");
        el.id = id;
        el.setAttribute("data-lazy-panel", "");
        Object.defineProperty(el, "offsetTop", { value: offsetTop, configurable: true });
        document.body.appendChild(el);
        return el;
    }

    test("triggers ajax on scroll when target enters the viewport", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        makeTarget("lpScroll", 500);

        lazyPanel().init("lpScroll");
        jest.runAllTimers(); // Flush onload deferral so the scroll listener is registered.

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const options = mockFaces.ajax.request.mock.calls[0][2];
        expect(options.execute).toBe("lpScroll");
        expect(options["omnifaces.event"]).toBe("loadLazyPanel");
        expect(options.params).toEqual({});
    });

    test("does not trigger ajax when target is below the viewport", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        makeTarget("lpBelow", 5000);

        lazyPanel().init("lpBelow");
        jest.runAllTimers();

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();

        expect(mockFaces.ajax.request).not.toHaveBeenCalled();
    });

    test("removes listeners after first trigger", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        makeTarget("lpOnce", 500);

        lazyPanel().init("lpOnce");
        jest.runAllTimers();

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);

        document.dispatchEvent(new Event("scroll"));
        jest.runAllTimers();
        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
    });
});
