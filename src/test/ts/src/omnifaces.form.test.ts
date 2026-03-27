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
 * Tests for the OmniFaces.Form namespace (partial submit).
 *
 * Form auto-initializes via addOnloadListener(init) at module load time.
 * It wraps faces.ajax.request to intercept getViewState for partial submit.
 * We set up window.faces BEFORE loading omnifaces.js and use fake timers to
 * ensure Form.init() finds faces and installs the wrapper.
 */

import { loadOmniFacesJs } from "../test-setup";
import { createFacesForm } from "../test-helpers";


let originalRequest: jest.Mock;
let originalGetViewState: jest.Mock;

beforeAll(() => {
    originalRequest = jest.fn();
    originalGetViewState = jest.fn();

    (window as any).faces = {
        ajax: { request: originalRequest },
        getViewState: originalGetViewState,
    };

    jest.useFakeTimers();
    loadOmniFacesJs();
    jest.runAllTimers();
    jest.useRealTimers();
});

afterAll(() => {
    delete (window as any).faces;
});

describe("OmniFaces.Form: wrapping", () => {

    afterEach(() => {
        // Restore getViewState so the wrapping test doesn't leave stale wrappers.
        (window as any).faces.getViewState = originalGetViewState;
        originalRequest.mockReset();
    });

    test("wraps faces.ajax.request", () => {
        expect((window as any).faces.ajax.request).not.toBe(originalRequest);
        expect(typeof (window as any).faces.ajax.request).toBe("function");
    });

    test("delegates to original faces.ajax.request", () => {
        const source = document.createElement("input");
        source.id = "btn";
        const event = {};
        const options = {};

        (window as any).faces.ajax.request(source, event, options);
        expect(originalRequest).toHaveBeenCalledWith(source, event, options);
    });
});

describe("OmniFaces.Form: partial submit getViewState", () => {

    let form: HTMLFormElement | undefined;

    beforeEach(() => {
        // Restore getViewState to the original mock so each test starts without
        // chained wrappers from previous faces.ajax.request calls.
        (window as any).faces.getViewState = originalGetViewState;
    });

    afterEach(() => {
        form?.remove();
        originalGetViewState.mockReset();
        originalRequest.mockReset();
    });

    test("returns original view state when partialsubmit is not set", () => {
        form = createFacesForm("formNoPS", "/test");
        originalGetViewState.mockReturnValue("field1=val1&field2=val2");

        const source = document.createElement("input");
        source.id = "src1";

        // Trigger the wrapped request which wraps getViewState
        (window as any).faces.ajax.request(source, null, { execute: "src1" });

        // The wrapper should have called getViewState on the form.
        // Since partialsubmit is not set, it should return original.
        const wrappedGetViewState = (window as any).faces.getViewState;
        const result = wrappedGetViewState(form);
        expect(result).toBe("field1=val1&field2=val2");
    });

    test("filters view state when partialsubmit is true", () => {
        form = createFacesForm("formPS", "/test");
        form.dataset["partialsubmit"] = "true";

        // Create a container with child inputs for the execute target
        const panel = document.createElement("div");
        panel.id = "panel1";
        const input1 = document.createElement("input");
        input1.name = "panel1:input1";
        panel.appendChild(input1);
        form.appendChild(panel);

        const encodedViewState = encodeURIComponent("jakarta.faces.ViewState") + "=viewStateValue";
        const encodedClientWindow = encodeURIComponent("jakarta.faces.ClientWindow") + "=cwValue";
        originalGetViewState.mockReturnValue(
            "panel1%3Ainput1=hello&other%3Afield=world&" + encodedViewState + "&" + encodedClientWindow
        );

        const source = document.createElement("input");
        source.id = "btn";

        (window as any).faces.ajax.request(source, null, { execute: "panel1" });

        const wrappedGetViewState = (window as any).faces.getViewState;
        const result = wrappedGetViewState(form);

        // Should include panel1:input1 (child of execute target), ViewState, and ClientWindow
        expect(result).toContain("panel1%3Ainput1=hello");
        expect(result).toContain(encodeURIComponent("jakarta.faces.ViewState"));
        expect(result).toContain(encodeURIComponent("jakarta.faces.ClientWindow"));
        // Should NOT include other:field
        expect(result).not.toContain("other%3Afield");
    });

    test("returns original view state when execute includes @form", () => {
        form = createFacesForm("formAtForm", "/test");
        form.dataset["partialsubmit"] = "true";
        originalGetViewState.mockReturnValue("all=data");

        const source = document.createElement("input");
        source.id = "s1";

        (window as any).faces.ajax.request(source, null, { execute: "@form" });
        const result = (window as any).faces.getViewState(form);
        expect(result).toBe("all=data");
    });

    test("returns original view state when execute includes @all", () => {
        form = createFacesForm("formAtAll", "/test");
        form.dataset["partialsubmit"] = "true";
        originalGetViewState.mockReturnValue("all=data");

        const source = document.createElement("input");
        source.id = "s2";

        (window as any).faces.ajax.request(source, null, { execute: "@all" });
        const result = (window as any).faces.getViewState(form);
        expect(result).toBe("all=data");
    });

    test("returns original view state when execute is undefined", () => {
        form = createFacesForm("formNoExec", "/test");
        form.dataset["partialsubmit"] = "true";
        originalGetViewState.mockReturnValue("all=data");

        const source = document.createElement("input");
        source.id = "s3";

        (window as any).faces.ajax.request(source, null, {});
        const result = (window as any).faces.getViewState(form);
        expect(result).toBe("all=data");
    });

    test("@none execute produces only ViewState and ClientWindow", () => {
        form = createFacesForm("formNone", "/test");
        form.dataset["partialsubmit"] = "true";

        const encodedVS = encodeURIComponent("jakarta.faces.ViewState") + "=vs";
        const encodedCW = encodeURIComponent("jakarta.faces.ClientWindow") + "=cw";
        originalGetViewState.mockReturnValue(
            "field1=val1&" + encodedVS + "&" + encodedCW
        );

        const source = document.createElement("input");
        source.id = "s4";

        (window as any).faces.ajax.request(source, null, { execute: "@none" });
        const result = (window as any).faces.getViewState(form);

        expect(result).toContain(encodeURIComponent("jakarta.faces.ViewState"));
        expect(result).toContain(encodeURIComponent("jakarta.faces.ClientWindow"));
        expect(result).not.toContain("field1");
    });

    test("@this replaces with source element id", () => {
        form = createFacesForm("formThis", "/test");
        form.dataset["partialsubmit"] = "true";

        const sourceInput = document.createElement("input");
        sourceInput.id = "myButton";
        sourceInput.name = "myButton";
        form.appendChild(sourceInput);

        const encodedVS = encodeURIComponent("jakarta.faces.ViewState") + "=vs";
        originalGetViewState.mockReturnValue(
            "myButton=click&other=data&" + encodedVS
        );

        (window as any).faces.ajax.request(sourceInput, null, { execute: "@this" });
        const result = (window as any).faces.getViewState(form);

        expect(result).toContain("myButton=click");
        expect(result).not.toContain("other=data");
    });

    test("source as string id works for @this replacement", () => {
        form = createFacesForm("formThisStr", "/test");
        form.dataset["partialsubmit"] = "true";

        const encodedVS = encodeURIComponent("jakarta.faces.ViewState") + "=vs";
        originalGetViewState.mockReturnValue(
            "btnId=click&other=data&" + encodedVS
        );

        (window as any).faces.ajax.request("btnId", null, { execute: "@this" });
        const result = (window as any).faces.getViewState(form);

        expect(result).toContain("btnId=click");
        expect(result).not.toContain("other=data");
    });
});
