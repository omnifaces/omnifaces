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
 * Tests for the OmniFaces.Unload namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { createFacesForm, installMockBeacon, uninstallMockBeacon, getBeaconCalls, installMockXHR, uninstallMockXHR, lastXHR } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const unload = () => OmniFaces.Unload as Record<string, Function>;

describe("OmniFaces.Unload.init", () => {

    let form: HTMLFormElement | undefined;

    beforeEach(() => {
        installMockBeacon();
    });

    afterEach(() => {
        form?.remove();
        uninstallMockBeacon();
    });

    test("does nothing when XMLHttpRequest is not supported", () => {
        const origXHR = window.XMLHttpRequest;
        delete (window as unknown as Record<string, unknown>).XMLHttpRequest;

        expect(() => unload().init("viewScope1")).not.toThrow();

        (window as unknown as Record<string, unknown>).XMLHttpRequest = origXHR;
    });

    test("does nothing when no Faces form exists", () => {
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        expect(() => unload().init("viewScope2")).not.toThrow();
        errorSpy.mockRestore();
    });
});

describe("OmniFaces.Unload.disable and reenable", () => {

    test("disable sets internal disabled flag", () => {
        expect(() => unload().disable()).not.toThrow();
    });

    test("reenable clears internal disabled flag", () => {
        unload().disable();
        expect(() => unload().reenable()).not.toThrow();
    });
});

describe("OmniFaces.Unload: beacon on unload", () => {

    let form: HTMLFormElement | undefined;

    beforeEach(() => {
        installMockBeacon();
    });

    afterEach(() => {
        form?.remove();
        uninstallMockBeacon();
    });

    test("sends beacon with correct query on pagehide event", () => {
        form = createFacesForm("f1", "/test/action");
        unload().init("viewScope42");

        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls().length).toBe(1);
        const call = getBeaconCalls()[0];
        expect(call.url).toContain("/test/action");

        const blob = call.data as Blob;
        expect(blob).toBeInstanceOf(Blob);
        expect(blob.type).toBe("application/x-www-form-urlencoded");
    });

    test("beacon query contains event, id and ViewState parameters", async () => {
        form = createFacesForm("f2", "/test/action");
        unload().init("vs123");

        window.dispatchEvent(new Event("pagehide"));

        const blob = getBeaconCalls()[0].data as Blob;
        const text = await new Promise<string>(resolve => { const r = new FileReader(); r.onload = () => resolve(r.result as string); r.readAsText(blob); });
        expect(text).toContain("omnifaces.event=unload");
        expect(text).toContain("id=vs123");
        expect(text).toContain("jakarta.faces.ViewState=");
    });

    test("sends beacon on pagehide event when page is eligible for back/forward cache", () => {
        form = createFacesForm("fPersisted", "/test/action");
        unload().init("vsPersisted");

        window.dispatchEvent(new PageTransitionEvent("pagehide", { persisted: true }));

        expect(getBeaconCalls().length).toBe(1);
    });

    test("does not send beacon on beforeunload event", () => {
        form = createFacesForm("fBeforeUnload", "/test/action");
        unload().init("vsBeforeUnload");

        window.dispatchEvent(new Event("beforeunload"));

        expect(getBeaconCalls().length).toBe(0);
    });

    test("does not touch window.onbeforeunload property", () => {
        const handler = function() {};
        window.onbeforeunload = handler;
        form = createFacesForm("fProperty", "/test/action");
        unload().init("vsProperty");

        expect(window.onbeforeunload).toBe(handler);

        window.onbeforeunload = null;
    });

    test("does not send beacon when disabled", () => {
        form = createFacesForm("f3", "/test/action");
        unload().init("vsDisabled");
        unload().disable();

        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls().length).toBe(0);
    });

    test("re-enables after disabled unload fires", () => {
        form = createFacesForm("f4", "/test/action");
        unload().init("vsReenable");
        unload().disable();

        window.dispatchEvent(new Event("pagehide"));
        // Should have called reenable() internally, so next unload should fire
        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls().length).toBe(1);
    });

    test("subsequent init updates viewScopeId without re-registering listeners", async () => {
        form = createFacesForm("f5", "/test/action");
        unload().init("firstId");
        unload().init("secondId");

        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls().length).toBe(1);
        const blob = getBeaconCalls()[0].data as Blob;
        const text = await new Promise<string>(resolve => { const r = new FileReader(); r.onload = () => resolve(r.result as string); r.readAsText(blob); });
        expect(text).toContain("id=secondId");
        expect(text).not.toContain("id=firstId");
    });
});

describe("OmniFaces.Unload: XHR fallback on unload", () => {

    let form: HTMLFormElement | undefined;

    beforeEach(() => {
        installMockXHR();
    });

    afterEach(() => {
        form?.remove();
        uninstallMockXHR();
    });

    test("sends synchronous XHR POST when sendBeacon is unavailable", () => {
        const origBeacon = navigator.sendBeacon;
        Object.defineProperty(navigator, "sendBeacon", { value: undefined, configurable: true, writable: true });

        form = createFacesForm("fXhr", "/test/xhr-action");
        unload().init("vsXhr");

        window.dispatchEvent(new Event("pagehide"));

        const xhr = lastXHR();
        expect(xhr).toBeDefined();
        expect(xhr.method).toBe("POST");
        expect(xhr.url).toContain("/test/xhr-action");
        expect(xhr.async).toBe(false);
        expect(xhr.requestHeaders["X-Requested-With"]).toBe("XMLHttpRequest");
        expect(xhr.requestHeaders["Content-Type"]).toBe("application/x-www-form-urlencoded");
        expect(xhr.body).toContain("omnifaces.event=unload");
        expect(xhr.body).toContain("id=vsXhr");

        Object.defineProperty(navigator, "sendBeacon", { value: origBeacon, configurable: true, writable: true });
    });
});
