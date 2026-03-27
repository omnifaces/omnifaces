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
 * Tests for the OmniFaces.InputFile namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { installMockFaces, uninstallMockFaces } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const inputFile = () => OmniFaces.InputFile as Record<string, Function>;

describe("OmniFaces.InputFile.validate", () => {

    let input: HTMLInputElement;
    let messageDiv: HTMLDivElement;
    let form: HTMLFormElement;
    let mockFaces: Record<string, any>;

    beforeEach(() => {
        mockFaces = installMockFaces();

        form = document.createElement("form");
        form.enctype = "multipart/form-data";

        input = document.createElement("input");
        input.type = "file";
        input.id = "fileInput";
        form.appendChild(input);
        document.body.appendChild(form);

        messageDiv = document.createElement("div");
        messageDiv.id = "fileMessage";
        document.body.appendChild(messageDiv);
    });

    afterEach(() => {
        form?.remove();
        messageDiv?.remove();
        uninstallMockFaces();
        delete (window as any).mojarra;
    });

    test("returns true when FileReader is not supported", () => {
        const origFileReader = window.FileReader;
        delete (window as unknown as Record<string, unknown>).FileReader;

        const result = inputFile().validate(new Event("change"), input, "fileMessage", 1024);
        expect(result).toBe(true);

        (window as unknown as Record<string, unknown>).FileReader = origFileReader;
    });

    test("returns true when all files are within maxsize", () => {
        const file = new File(["small"], "small.txt", { type: "text/plain" });
        Object.defineProperty(input, "files", { value: [file], writable: true, configurable: true });

        const result = inputFile().validate(new Event("change"), input, "fileMessage", 1024);
        expect(result).toBe(true);
    });

    test("returns false when a file exceeds maxsize", () => {
        const largeContent = "x".repeat(2000);
        const file = new File([largeContent], "large.txt", { type: "text/plain" });
        Object.defineProperty(input, "files", { value: [file], writable: true, configurable: true });

        const result = inputFile().validate(new Event("change"), input, "fileMessage", 1024);
        expect(result).toBe(false);
    });

    test("triggers faces.ajax.request when file exceeds maxsize", () => {
        const largeContent = "x".repeat(2000);
        const file = new File([largeContent], "large.txt", { type: "text/plain" });
        Object.defineProperty(input, "files", { value: [file], writable: true, configurable: true });

        inputFile().validate(new Event("change"), input, "fileMessage", 1024);

        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const args = mockFaces.ajax.request.mock.calls[0];
        expect(args[0]).toBe("fileInput"); // source is input id
        const params = args[2];
        expect(params.fileName).toBe("large.txt");
        expect(params["omnifaces.event"]).toBe("validationFailed");
    });

    test("clears message element before validation", () => {
        messageDiv.innerHTML = "Previous error";
        const file = new File(["ok"], "ok.txt", { type: "text/plain" });
        Object.defineProperty(input, "files", { value: [file], writable: true, configurable: true });

        inputFile().validate(new Event("change"), input, "fileMessage", 1024);
        expect(messageDiv.innerHTML).toBe("");
    });

    test("changes form enctype for Mojarra when file exceeds maxsize", () => {
        (window as any).mojarra = {};
        const largeContent = "x".repeat(2000);
        const file = new File([largeContent], "large.txt", { type: "text/plain" });
        Object.defineProperty(input, "files", { value: [file], writable: true, configurable: true });

        inputFile().validate(new Event("change"), input, "fileMessage", 1024);

        // After validation, enctype should be restored
        expect(form.enctype).toBe("multipart/form-data");
    });

    test("returns true when files list is empty", () => {
        Object.defineProperty(input, "files", { value: [], writable: true, configurable: true });
        const result = inputFile().validate(new Event("change"), input, "fileMessage", 1024);
        expect(result).toBe(true);
    });

    test("validates only the first oversized file and returns false", () => {
        const small = new File(["ok"], "small.txt", { type: "text/plain" });
        const large1 = new File(["x".repeat(2000)], "large1.txt", { type: "text/plain" });
        const large2 = new File(["x".repeat(3000)], "large2.txt", { type: "text/plain" });
        Object.defineProperty(input, "files", { value: [small, large1, large2], writable: true, configurable: true });

        const result = inputFile().validate(new Event("change"), input, "fileMessage", 1024);
        expect(result).toBe(false);
        // Only one ajax request for the first oversized file
        expect(mockFaces.ajax.request).toHaveBeenCalledTimes(1);
        const params = mockFaces.ajax.request.mock.calls[0][2];
        expect(params.fileName).toBe("large1.txt");
    });
});
