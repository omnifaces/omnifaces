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
 * Tests for the OmniFaces.ScriptParam namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { createFacesForm, installMockFaces, uninstallMockFaces } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const scriptParam = () => OmniFaces.ScriptParam as Record<string, Function>;

describe("OmniFaces.ScriptParam.run", () => {

    let form: HTMLFormElement;
    let mockFaces: Record<string, any>;

    beforeEach(() => {
        mockFaces = installMockFaces();
        form = createFacesForm();
    });

    afterEach(() => {
        form?.remove();
        uninstallMockFaces();
    });

    test("sends script results to server via faces.ajax.request", () => {
        scriptParam().run("spId", { "param:name": "hello" });

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const args = mockFaces.ajax.request.mock.calls[0];
        expect(args[0]).toBe(form);
        expect(args[1]).toBeNull();
        expect(args[2].execute).toBe("spId");
        expect(args[2]["omnifaces.event"]).toBe("setScriptParamValues");
    });

    test("clones primitive values as-is", () => {
        scriptParam().run("spId", { "p1": "stringValue" });

        const params = mockFaces.ajax.request.mock.calls[0][2];
        expect(JSON.parse(params["p1"])).toBe("stringValue");
    });

    test("clones object values excluding functions and nested objects", () => {
        const objectValue = { name: "test", count: 42, fn: () => {}, nested: { deep: true } };
        scriptParam().run("spId", { "p1": objectValue as any });

        const params = mockFaces.ajax.request.mock.calls[0][2];
        const cloned = JSON.parse(params["p1"]);
        expect(cloned.name).toBe("test");
        expect(cloned.count).toBe(42);
        expect(cloned.fn).toBeUndefined();
        expect(cloned.nested).toBeUndefined();
    });

    test("does not send when no Faces form exists", () => {
        form.remove();
        const errorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
        scriptParam().run("spId", { "p1": "value" });
        expect(mockFaces.ajax.request).not.toHaveBeenCalled();
        errorSpy.mockRestore();
        form = null as any; // Prevent afterEach from trying to remove again
    });

    test("handles multiple script params", () => {
        scriptParam().run("spId", { "p1": "val1", "p2": 42, "p3": true });

        const params = mockFaces.ajax.request.mock.calls[0][2];
        expect(JSON.parse(params["p1"])).toBe("val1");
        expect(JSON.parse(params["p2"])).toBe(42);
        expect(JSON.parse(params["p3"])).toBe(true);
    });

    test("handles null/undefined primitives", () => {
        scriptParam().run("spId", { "p1": null as any });

        const params = mockFaces.ajax.request.mock.calls[0][2];
        expect(JSON.parse(params["p1"])).toBeNull();
    });
});
