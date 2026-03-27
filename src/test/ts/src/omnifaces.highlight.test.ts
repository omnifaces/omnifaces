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
 * Tests for the OmniFaces.Highlight namespace.
 */

import { loadOmniFacesJs } from "../test-setup";

beforeAll(() => loadOmniFacesJs());

const highlight = () => OmniFaces.Highlight as Record<string, any>;

describe("OmniFaces.Highlight constants", () => {
    test("DATA_HIGHLIGHT_CLASS is 'data-omnifaces-highlight-class'", () => {
        expect(highlight().DATA_HIGHLIGHT_CLASS).toBe("data-omnifaces-highlight-class");
    });

    test("DATA_HIGHLIGHT_LABEL is 'data-omnifaces-highlight-label'", () => {
        expect(highlight().DATA_HIGHLIGHT_LABEL).toBe("data-omnifaces-highlight-label");
    });
});

describe("OmniFaces.Highlight.apply", () => {

    let input: HTMLInputElement;
    let label: HTMLLabelElement;

    beforeEach(() => {
        input = document.createElement("input");
        input.id = "myInput";
        input.type = "text";
        document.body.appendChild(input);

        label = document.createElement("label");
        label.htmlFor = "myInput";
        document.body.appendChild(label);
    });

    afterEach(() => {
        input?.remove();
        label?.remove();
    });

    test("adds styleClass to input element", () => {
        highlight().apply(["myInput"], "error", false);
        expect(input.classList.contains("error")).toBe(true);
    });

    test("sets DATA_HIGHLIGHT_CLASS attribute on input", () => {
        highlight().apply(["myInput"], "error", false);
        expect(input.getAttribute("data-omnifaces-highlight-class")).toBe("error");
    });

    test("adds styleClass to associated label", () => {
        highlight().apply(["myInput"], "error", false);
        expect(label.classList.contains("error")).toBe(true);
    });

    test("sets DATA_HIGHLIGHT_LABEL attribute when label exists", () => {
        highlight().apply(["myInput"], "error", false);
        expect(input.getAttribute("data-omnifaces-highlight-label")).toBe("true");
    });

    test("focuses first element when doFocus is true", () => {
        const focusSpy = jest.spyOn(input, "focus");
        highlight().apply(["myInput"], "error", true);
        expect(focusSpy).toHaveBeenCalledTimes(1);
    });

    test("focuses only the first element when multiple clientIds", () => {
        const input2 = document.createElement("input");
        input2.id = "myInput2";
        input2.type = "text";
        document.body.appendChild(input2);

        const focusSpy1 = jest.spyOn(input, "focus");
        const focusSpy2 = jest.spyOn(input2, "focus");

        highlight().apply(["myInput", "myInput2"], "error", true);

        expect(focusSpy1).toHaveBeenCalledTimes(1);
        expect(focusSpy2).not.toHaveBeenCalled();
        input2.remove();
    });

    test("does not focus when doFocus is false", () => {
        const focusSpy = jest.spyOn(input, "focus");
        highlight().apply(["myInput"], "error", false);
        expect(focusSpy).not.toHaveBeenCalled();
    });

    test("skips non-existing clientIds", () => {
        expect(() => highlight().apply(["nonExistent", "myInput"], "error", false)).not.toThrow();
        expect(input.classList.contains("error")).toBe(true);
    });

    test("handles empty clientIds array", () => {
        expect(() => highlight().apply([], "error", false)).not.toThrow();
    });

    test("applies to multiple inputs", () => {
        const input2 = document.createElement("input");
        input2.id = "myInput2";
        input2.type = "text";
        document.body.appendChild(input2);

        highlight().apply(["myInput", "myInput2"], "error", false);

        expect(input.classList.contains("error")).toBe(true);
        expect(input2.classList.contains("error")).toBe(true);
        input2.remove();
    });

    test("finds element by name when not found by id", () => {
        const namedInput = document.createElement("input");
        namedInput.setAttribute("name", "namedField");
        namedInput.type = "text";
        document.body.appendChild(namedInput);

        highlight().apply(["namedField"], "error", false);
        expect(namedInput.classList.contains("error")).toBe(true);
        namedInput.remove();
    });

    test("does not add styleClass to label when no label exists for input", () => {
        label.remove();
        const orphanInput = document.createElement("input");
        orphanInput.id = "orphan";
        orphanInput.type = "text";
        document.body.appendChild(orphanInput);

        highlight().apply(["orphan"], "error", false);
        expect(orphanInput.classList.contains("error")).toBe(true);
        expect(orphanInput.hasAttribute("data-omnifaces-highlight-label")).toBe(false);
        orphanInput.remove();
    });
});

describe("OmniFaces.Highlight: remove highlight on click/input", () => {

    let input: HTMLInputElement;
    let label: HTMLLabelElement;

    beforeEach(() => {
        input = document.createElement("input");
        input.id = "clickInput";
        input.type = "text";
        document.body.appendChild(input);

        label = document.createElement("label");
        label.htmlFor = "clickInput";
        document.body.appendChild(label);
    });

    afterEach(() => {
        input?.remove();
        label?.remove();
    });

    test("removes styleClass from input on click", () => {
        highlight().apply(["clickInput"], "error", false);
        expect(input.classList.contains("error")).toBe(true);

        input.dispatchEvent(new Event("click"));
        expect(input.classList.contains("error")).toBe(false);
    });

    test("removes styleClass from input on input event", () => {
        highlight().apply(["clickInput"], "error", false);
        input.dispatchEvent(new Event("input"));
        expect(input.classList.contains("error")).toBe(false);
    });

    test("removes styleClass from associated label on click", () => {
        highlight().apply(["clickInput"], "error", false);
        expect(label.classList.contains("error")).toBe(true);

        input.dispatchEvent(new Event("click"));
        expect(label.classList.contains("error")).toBe(false);
    });

    test("removes DATA_HIGHLIGHT_CLASS attribute on click", () => {
        highlight().apply(["clickInput"], "error", false);
        input.dispatchEvent(new Event("click"));
        expect(input.hasAttribute("data-omnifaces-highlight-class")).toBe(false);
    });

    test("removes DATA_HIGHLIGHT_LABEL attribute on click", () => {
        highlight().apply(["clickInput"], "error", false);
        input.dispatchEvent(new Event("click"));
        expect(input.hasAttribute("data-omnifaces-highlight-label")).toBe(false);
    });

    test("click listener is removed after first trigger", () => {
        highlight().apply(["clickInput"], "error", false);
        input.dispatchEvent(new Event("click"));
        // Re-apply manually
        input.classList.add("error");
        input.dispatchEvent(new Event("click"));
        // Should still have the class because listener was removed
        expect(input.classList.contains("error")).toBe(true);
    });
});
