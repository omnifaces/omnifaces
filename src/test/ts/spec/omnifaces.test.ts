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
 * Tests for the top-level OmniFaces namespace exposed by omnifaces.js.
 */

import { loadOmniFacesJs } from "../test-setup";

beforeAll(() => loadOmniFacesJs());

describe("OmniFaces namespace", () => {

    const EXPECTED_MEMBERS: Record<string, string> = {
        // Constants
        EVENT: "string",
        VIEW_STATE_PARAM: "string",
        CLIENT_WINDOW_PARAM: "string",
        // Namespaces
        Util: "object",
        Highlight: "object",
        DeferredScript: "object",
        Unload: "object",
        Push: "object",
        InputFile: "object",
        Form: "object",
        HashParam: "object",
        ScriptParam: "object",
        ServiceWorker: "object",
        GraphicImage: "object",
        ScriptErrorHandler: "object",
    };

    test("exposes exactly the expected public members", () => {
        const actualKeys = Object.keys(OmniFaces).sort();
        const expectedKeys = Object.keys(EXPECTED_MEMBERS).sort();
        expect(actualKeys).toEqual(expectedKeys);
    });

    test("each member has the expected type", () => {
        for (const [key, expectedType] of Object.entries(EXPECTED_MEMBERS)) {
            expect(typeof OmniFaces[key]).toBe(expectedType);
        }
    });
});

describe("OmniFaces constants", () => {
    test("EVENT is 'omnifaces.event'", () => {
        expect(OmniFaces.EVENT).toBe("omnifaces.event");
    });

    test("VIEW_STATE_PARAM is 'jakarta.faces.ViewState'", () => {
        expect(OmniFaces.VIEW_STATE_PARAM).toBe("jakarta.faces.ViewState");
    });

    test("CLIENT_WINDOW_PARAM is 'jakarta.faces.ClientWindow'", () => {
        expect(OmniFaces.CLIENT_WINDOW_PARAM).toBe("jakarta.faces.ClientWindow");
    });
});

describe("OmniFaces.Util namespace", () => {

    const EXPECTED_FUNCTIONS = [
        "addEventListener",
        "removeEventListener",
        "addOnloadListener",
        "addSubmitListener",
        "resolveFunction",
        "getFacesForm",
        "updateParameter",
        "loadScript",
    ];

    test("exposes exactly the expected public functions", () => {
        const util = OmniFaces.Util as Record<string, unknown>;
        const actualKeys = Object.keys(util).sort();
        expect(actualKeys).toEqual([...EXPECTED_FUNCTIONS].sort());
    });

    test("all members are functions", () => {
        const util = OmniFaces.Util as Record<string, unknown>;
        for (const name of EXPECTED_FUNCTIONS) {
            expect(typeof util[name]).toBe("function");
        }
    });
});

describe("OmniFaces.Highlight namespace", () => {

    const EXPECTED_MEMBERS: Record<string, string> = {
        DATA_HIGHLIGHT_CLASS: "string",
        DATA_HIGHLIGHT_LABEL: "string",
        apply: "function",
    };

    test("exposes exactly the expected public members", () => {
        const highlight = OmniFaces.Highlight as Record<string, unknown>;
        const actualKeys = Object.keys(highlight).sort();
        const expectedKeys = Object.keys(EXPECTED_MEMBERS).sort();
        expect(actualKeys).toEqual(expectedKeys);
    });

    test("each member has the expected type", () => {
        const highlight = OmniFaces.Highlight as Record<string, unknown>;
        for (const [key, expectedType] of Object.entries(EXPECTED_MEMBERS)) {
            expect(typeof highlight[key]).toBe(expectedType);
        }
    });
});

describe("OmniFaces.Push namespace", () => {

    const EXPECTED_FUNCTIONS = ["init", "open", "close"];

    test("exposes exactly the expected public functions", () => {
        const push = OmniFaces.Push as Record<string, unknown>;
        const actualKeys = Object.keys(push).sort();
        expect(actualKeys).toEqual([...EXPECTED_FUNCTIONS].sort());
    });

    test("all members are functions", () => {
        const push = OmniFaces.Push as Record<string, unknown>;
        for (const name of EXPECTED_FUNCTIONS) {
            expect(typeof push[name]).toBe("function");
        }
    });
});

describe("OmniFaces.Unload namespace", () => {
    test("exposes init, disable, reenable functions", () => {
        const unload = OmniFaces.Unload as Record<string, unknown>;
        expect(typeof unload.init).toBe("function");
        expect(typeof unload.disable).toBe("function");
        expect(typeof unload.reenable).toBe("function");
    });
});

describe("OmniFaces.InputFile namespace", () => {
    test("exposes validate function", () => {
        const inputFile = OmniFaces.InputFile as Record<string, unknown>;
        expect(typeof inputFile.validate).toBe("function");
    });
});

describe("OmniFaces.DeferredScript namespace", () => {
    test("exposes add function", () => {
        const ds = OmniFaces.DeferredScript as Record<string, unknown>;
        expect(typeof ds.add).toBe("function");
    });
});

describe("OmniFaces.HashParam namespace", () => {
    test("exposes init and update functions", () => {
        const hp = OmniFaces.HashParam as Record<string, unknown>;
        expect(typeof hp.init).toBe("function");
        expect(typeof hp.update).toBe("function");
    });
});

describe("OmniFaces.ScriptParam namespace", () => {
    test("exposes run function", () => {
        const sp = OmniFaces.ScriptParam as Record<string, unknown>;
        expect(typeof sp.run).toBe("function");
    });
});

describe("OmniFaces.ServiceWorker namespace", () => {
    test("exposes init function", () => {
        const sw = OmniFaces.ServiceWorker as Record<string, unknown>;
        expect(typeof sw.init).toBe("function");
    });
});

describe("OmniFaces.ScriptErrorHandler namespace", () => {
    test("exposes init function", () => {
        const seh = OmniFaces.ScriptErrorHandler as Record<string, unknown>;
        expect(typeof seh.init).toBe("function");
    });
});
