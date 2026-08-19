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

// The unload listeners are registered on load, so let that task run before dispatching any event.
const initUnload = async (viewScopeId: string) => {
    unload().init(viewScopeId);
    await new Promise(resolve => setTimeout(resolve, 0));
};

// jsdom has no BeforeUnloadEvent, and Event#returnValue is there the legacy boolean alias of the canceled flag, so
// shadow it with the empty string which a real BeforeUnloadEvent carries.
const beforeUnloadEvent = () => {
    const event = new Event("beforeunload", { cancelable: true });
    Object.defineProperty(event, "returnValue", { value: "", writable: true, configurable: true });
    return event;
};

const readBeacon = async () => {
    const blob = getBeaconCalls()[0].data as Blob;
    return new Promise<string>(resolve => { const reader = new FileReader(); reader.onload = () => resolve(reader.result as string); reader.readAsText(blob); });
};

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

    test("sends beacon with correct query on beforeunload event", async () => {
        form = createFacesForm("f1", "/test/action");
        await initUnload("viewScope42");

        window.dispatchEvent(beforeUnloadEvent());

        expect(getBeaconCalls()).toHaveLength(1);
        const call = getBeaconCalls()[0];
        expect(call.url).toContain("/test/action");

        const blob = call.data as Blob;
        expect(blob).toBeInstanceOf(Blob);
        expect(blob.type).toBe("application/x-www-form-urlencoded");
    });

    test("beacon query contains event, id and ViewState parameters", async () => {
        form = createFacesForm("f2", "/test/action");
        await initUnload("vs123");

        window.dispatchEvent(beforeUnloadEvent());

        const text = await readBeacon();
        expect(text).toContain("omnifaces.event=unload");
        expect(text).toContain("id=vs123");
        expect(text).toContain("jakarta.faces.ViewState=");
    });

    test("sends beacon on pagehide event when browser does not support beforeunload", async () => {
        form = createFacesForm("fPagehide", "/test/action");
        await initUnload("vsPagehide");

        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls()).toHaveLength(1);
    });

    test("sends beacon on pagehide event when page is eligible for back/forward cache", async () => {
        form = createFacesForm("fPersisted", "/test/action");
        await initUnload("vsPersisted");

        window.dispatchEvent(new PageTransitionEvent("pagehide", { persisted: true }));

        expect(getBeaconCalls()).toHaveLength(1);
    });

    test("sends beacon only once when both beforeunload and pagehide fire", async () => {
        form = createFacesForm("fBoth", "/test/action");
        await initUnload("vsBoth");

        window.dispatchEvent(beforeUnloadEvent());
        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls()).toHaveLength(1);
    });

    test("holds back beacon until pagehide when the beforeunload event is canceled", async () => {
        form = createFacesForm("fPrevented", "/test/action");
        await initUnload("vsPrevented");

        const event = beforeUnloadEvent();
        event.preventDefault(); // as a leave confirmation of the application does
        window.dispatchEvent(event);
        expect(getBeaconCalls()).toHaveLength(0); // enduser can still cancel the navigation

        window.dispatchEvent(new Event("pagehide"));
        expect(getBeaconCalls()).toHaveLength(1); // navigation is committed
    });

    test("holds back beacon until pagehide when returnValue is set on the beforeunload event", async () => {
        form = createFacesForm("fReturnValue", "/test/action");
        await initUnload("vsReturnValue");

        const event = beforeUnloadEvent();
        (event as BeforeUnloadEvent).returnValue = "You have unsaved changes."; // as a legacy leave confirmation does
        window.dispatchEvent(event);
        expect(getBeaconCalls()).toHaveLength(0);

        window.dispatchEvent(new Event("pagehide"));
        expect(getBeaconCalls()).toHaveLength(1);
    });

    test("does not touch window.onbeforeunload property", async () => {
        const handler = function() {};
        window.onbeforeunload = handler;
        form = createFacesForm("fProperty", "/test/action");
        await initUnload("vsProperty");

        expect(window.onbeforeunload).toBe(handler);

        window.onbeforeunload = null;
    });

    test("does not send beacon when disabled", async () => {
        form = createFacesForm("f3", "/test/action");
        await initUnload("vsDisabled");
        unload().disable();

        window.dispatchEvent(beforeUnloadEvent());
        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls()).toHaveLength(0);
    });

    test("re-enables after disabled unload fires", async () => {
        form = createFacesForm("f4", "/test/action");
        await initUnload("vsReenable");
        unload().disable();

        window.dispatchEvent(new Event("pagehide"));
        // Should have called reenable() internally, so next unload should fire
        window.dispatchEvent(new Event("pagehide"));

        expect(getBeaconCalls()).toHaveLength(1);
    });

    test("subsequent init updates viewScopeId without re-registering listeners", async () => {
        form = createFacesForm("f5", "/test/action");
        await initUnload("firstId");
        await initUnload("secondId");

        window.dispatchEvent(beforeUnloadEvent());

        expect(getBeaconCalls()).toHaveLength(1);
        const text = await readBeacon();
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

    test("sends synchronous XHR POST when sendBeacon is unavailable", async () => {
        const origBeacon = navigator.sendBeacon;
        Object.defineProperty(navigator, "sendBeacon", { value: undefined, configurable: true, writable: true });

        form = createFacesForm("fXhr", "/test/xhr-action");
        await initUnload("vsXhr");

        window.dispatchEvent(beforeUnloadEvent());

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
