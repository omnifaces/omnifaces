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
 * Tests for the OmniFaces.Unload namespace in a browser supporting keepalive fetch. As long as the view state is short
 * enough for a request header, the unload metadata then travels in request headers as well, because the server receives
 * the headers even when the client aborts while the request body is still in transit. The body stays the same, so a
 * server reading only the body keeps working. The paths for browsers without keepalive fetch are covered by
 * omnifaces.unload.test.ts, as jsdom has no fetch.
 */

import { loadOmniFacesJs } from "../test-setup";
import { MAX_VIEW_STATE_HEADER_LENGTH, VIEW_STATE_PARAM } from "../test-constants";
import {
    createFacesForm, installMockBeacon, uninstallMockBeacon, getBeaconCalls, installMockFetch, uninstallMockFetch, resetFetchCalls, getFetchCalls,
} from "../test-helpers";

beforeAll(() => {
    installMockFetch();
    loadOmniFacesJs();
});

afterAll(() => uninstallMockFetch());

const unload = () => OmniFaces.Unload as Record<string, Function>;

// The unload listeners are registered on load, so let that task run before dispatching any event.
const initUnload = async (viewScopeId: string) => {
    unload().init(viewScopeId);
    await new Promise(resolve => setTimeout(resolve, 0));
};

const setViewState = (form: HTMLFormElement, value: string) => {
    (form.elements.namedItem(VIEW_STATE_PARAM) as HTMLInputElement).value = value;
};

const unloadRequest = () => {
    expect(getFetchCalls()).toHaveLength(1);
    const { url, init } = getFetchCalls()[0];
    const entries = Object.entries(init.headers as Record<string, string>);
    const headers = Object.fromEntries(entries.map(([name, value]) => [name.toLowerCase(), value]));
    return { url, init, headers };
};

const readBody = (body: unknown) => {
    if (!(body instanceof Blob)) {
        return Promise.resolve(String(body));
    }

    return new Promise<string>(resolve => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.readAsText(body);
    });
};

const contentType = (body: unknown, headers: Record<string, string>) =>
    headers["content-type"] ?? (body instanceof Blob ? body.type : body instanceof URLSearchParams ? "application/x-www-form-urlencoded" : undefined);

describe("OmniFaces.Unload: keepalive fetch on unload", () => {

    let form: HTMLFormElement | undefined;

    beforeEach(() => {
        resetFetchCalls();
        installMockBeacon();
    });

    afterEach(() => {
        form?.remove();
        uninstallMockBeacon();
    });

    test("sends keepalive POST with same origin credentials to form action instead of beacon", async () => {
        form = createFacesForm("fKeepalive", "/test/action");
        await initUnload("vsKeepalive");

        window.dispatchEvent(new Event("pagehide"));

        const { url, init } = unloadRequest();
        expect(url).toContain("/test/action");
        expect(init.method).toBe("POST");
        expect(init.keepalive).toBe(true);
        expect(init.credentials).toBe("same-origin");
        expect(getBeaconCalls()).toHaveLength(0);
    });

    test("carries event, view scope ID and view state in request headers", async () => {
        form = createFacesForm("fHeaders", "/test/action");
        const viewState = (form.elements.namedItem(VIEW_STATE_PARAM) as HTMLInputElement).value;
        await initUnload("vsHeaders");

        window.dispatchEvent(new Event("pagehide"));

        const { headers } = unloadRequest();
        expect(headers["omnifaces-event"]).toBe("unload");
        expect(headers["omnifaces-view-scope"]).toBe("vsHeaders");
        expect(headers["omnifaces-view-state"]).toBe(viewState);
    });

    test("carries event, view scope ID and view state in form encoded request body as well", async () => {
        form = createFacesForm("fBody", "/test/action");
        await initUnload("vsBody");

        window.dispatchEvent(new Event("pagehide"));

        const { init, headers } = unloadRequest();
        expect(contentType(init.body, headers)).toBe("application/x-www-form-urlencoded");
        const body = await readBody(init.body);
        expect(body).toContain("omnifaces.event=unload");
        expect(body).toContain("id=vsBody");
        expect(body).toContain(VIEW_STATE_PARAM + "=");
    });

    test("still sends keepalive fetch when view state is exactly as long as allowed in a request header", async () => {
        form = createFacesForm("fMaxLength", "/test/action");
        setViewState(form, "x".repeat(MAX_VIEW_STATE_HEADER_LENGTH));
        await initUnload("vsMaxLength");

        window.dispatchEvent(new Event("pagehide"));

        expect(unloadRequest().headers["omnifaces-view-state"]).toHaveLength(MAX_VIEW_STATE_HEADER_LENGTH);
    });

    test("sends beacon without request headers when view state is too long for a request header", async () => {
        form = createFacesForm("fTooLong", "/test/action");
        setViewState(form, "x".repeat(MAX_VIEW_STATE_HEADER_LENGTH + 1));
        await initUnload("vsTooLong");

        window.dispatchEvent(new Event("pagehide"));

        expect(getFetchCalls()).toHaveLength(0);
        expect(getBeaconCalls()).toHaveLength(1);
    });

    test("sends only once when both beforeunload and pagehide fire", async () => {
        form = createFacesForm("fOnce", "/test/action");
        await initUnload("vsOnce");

        const event = new Event("beforeunload", { cancelable: true });
        Object.defineProperty(event, "returnValue", { value: "", writable: true, configurable: true });
        window.dispatchEvent(event);
        window.dispatchEvent(new Event("pagehide"));

        expect(getFetchCalls()).toHaveLength(1);
    });

    test("falls back to beacon when keepalive fetch rejects", async () => {
        resetFetchCalls(true);
        const unhandled = jest.fn();
        process.on("unhandledRejection", unhandled);
        form = createFacesForm("fReject", "/test/action");
        await initUnload("vsReject");

        window.dispatchEvent(new Event("pagehide"));
        await new Promise(resolve => setTimeout(resolve, 0));

        process.off("unhandledRejection", unhandled);
        expect(getFetchCalls()).toHaveLength(1);
        expect(getBeaconCalls()).toHaveLength(1);
        expect(unhandled).not.toHaveBeenCalled();
    });
});
