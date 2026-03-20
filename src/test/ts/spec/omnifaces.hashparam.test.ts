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
 * Tests for the OmniFaces.HashParam namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { createFacesForm, installMockFaces, uninstallMockFaces } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const hashParam = () => OmniFaces.HashParam as Record<string, Function>;

describe("OmniFaces.HashParam.init", () => {

    let form: HTMLFormElement;

    afterEach(() => {
        form?.remove();
        uninstallMockFaces();
        window.location.hash = "";
    });

    test("does not throw when no Faces form exists", () => {
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        expect(() => hashParam().init("hashParamId")).not.toThrow();
        errorSpy.mockRestore();
    });

    test("sends hash params on init when hash is present", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        window.location.hash = "#foo=bar";
        hashParam().init("hp1");

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const args = mockFaces.ajax.request.mock.calls[0];
        expect(args[2].execute).toBe("hp1");
        expect(args[2]["omnifaces.event"]).toBe("setHashParamValues");
        expect(args[2].hash).toBe("foo=bar");
    });

    test("does not send hash params on init when hash is empty", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        window.location.hash = "";
        hashParam().init("hp2");
        expect(mockFaces.ajax.request).not.toHaveBeenCalled();
    });
});

describe("OmniFaces.HashParam.update", () => {

    beforeEach(() => {
        window.location.hash = "";
    });

    afterEach(() => {
        window.location.hash = "";
    });

    test("adds parameter to empty hash", () => {
        hashParam().update("name", "value");
        // Either history.pushState or location.hash should have been called
        // The result should contain the parameter
        const hash = window.location.hash.replace(/^#/, "");
        expect(hash).toContain("name=value");
    });

    test("removes parameter when value is empty", () => {
        window.location.hash = "#name=value&other=keep";
        hashParam().update("name", "");
        const hash = window.location.hash.replace(/^#/, "");
        expect(hash).not.toContain("name=");
        expect(hash).toContain("other=keep");
    });

    test("updates existing parameter", () => {
        window.location.hash = "#name=old";
        hashParam().update("name", "new");
        const hash = window.location.hash.replace(/^#/, "");
        expect(hash).toContain("name=new");
    });

    test("does not trigger setHashParamValues during update", () => {
        const mockFaces = installMockFaces();
        const form = createFacesForm();
        hashParam().init("hpUpdate");
        mockFaces.ajax.request.mockClear();

        hashParam().update("key", "val");
        // The updating flag should prevent setHashParamValues from firing
        expect(mockFaces.ajax.request).not.toHaveBeenCalled();

        form.remove();
        uninstallMockFaces();
    });
});

describe("OmniFaces.HashParam: hashchange event", () => {

    let form: HTMLFormElement;

    afterEach(() => {
        form?.remove();
        uninstallMockFaces();
        window.location.hash = "";
    });

    test("hashchange event triggers setHashParamValues", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        window.location.hash = "";
        hashParam().init("hpHashChange");
        mockFaces.ajax.request.mockClear();

        // Simulate hash change
        window.location.hash = "#newparam=newval";
        window.dispatchEvent(new Event("hashchange"));

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const args = mockFaces.ajax.request.mock.calls[0];
        expect(args[2]["omnifaces.event"]).toBe("setHashParamValues");
        expect(args[2].hash).toBe("newparam=newval");
    });

    test("hashchange does not trigger during programmatic update", () => {
        const mockFaces = installMockFaces();
        form = createFacesForm();
        hashParam().init("hpNoTrigger");
        mockFaces.ajax.request.mockClear();

        // update() sets updating=true, so hashchange during it should not trigger
        // This is already tested in the "does not trigger setHashParamValues during update" test
        // but here we verify the hashchange event path specifically
        hashParam().update("key", "val");
        // If hashchange fires synchronously from location.hash change, it should be suppressed
        expect(mockFaces.ajax.request).not.toHaveBeenCalled();
    });
});
