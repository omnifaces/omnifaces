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

import { Util } from "./Util";

/**
 * Highlight/focus.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.Highlight
 */
export module Highlight {

    // "Constant" fields ----------------------------------------------------------------------------------------------

    export const DATA_HIGHLIGHT_CLASS = "data-omnifaces-highlight-class";
    export const DATA_HIGHLIGHT_LABEL = "data-omnifaces-highlight-label";

    // Private static fields ------------------------------------------------------------------------------------------

    let labelsByFor: Record<string, HTMLLabelElement>;

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Apply the highlight. Add the given error style class to all input elements of the given client IDs and their
     * associated labels. If doFocus is <code>true</code>, then also set the focus on the first input element. All
     * non-existing input elements are ignored.
     * @param clientIds Array of client IDs of elements to highlight.
     * @param styleClass CSS style class to be set on the elements and the associated label elements.
     * @param doFocus Whether or not to put focus on the first highlighted element.
     */
    export function apply(clientIds: string[], styleClass: string, doFocus: boolean) {
        labelsByFor = getLabelsByFor();

        for (let clientId of clientIds) {
            const input = getElementByIdOrName(clientId);

            if (input) {
                input.className += " " + styleClass;
                input.setAttribute(DATA_HIGHLIGHT_CLASS, styleClass);
                const label = labelsByFor[input.id];

                if (label) {
                    label.className += " " + styleClass;
                    input.setAttribute(DATA_HIGHLIGHT_LABEL, "true");
                }

                if (doFocus) {
                    input.focus();
                    doFocus = false;
                }

                Util.addEventListener(input, "click input", removeHighlight);
            }
        }
    }

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Return a mapping of all <code>label</code> elements keyed by their <code>for</code> attribute.
     * @return A mapping of all <code>label</code> elements keyed by their <code>for</code> attribute.
     */
    function getLabelsByFor(): Record<string, HTMLLabelElement> {
        const labels = document.getElementsByTagName("LABEL") as HTMLCollectionOf<HTMLLabelElement>;
        const labelsByFor: Record<string, HTMLLabelElement> = {};

        for (var i = 0; i < labels.length; i++) {
            const label = labels[i];
            const htmlFor = label.htmlFor;

            if (htmlFor) {
                labelsByFor[htmlFor] = label;
            }
        }

        return labelsByFor;
    }

    /**
     * Returns an element by ID or name.
     * @param Client ID.
     * @return HTML element identified by given client ID. 
     */
    function getElementByIdOrName(clientId: string): HTMLElement {
        let element = document.getElementById(clientId);

        if (!element) {
            const elements = document.getElementsByName(clientId); // #21

            if (elements && elements.length) {
                element = elements[0];
            }
        }

        return element;
    }

    /**
     * Remove the highlight. Remove the error style class from involved input element and its associated label.
     */
    function removeHighlight() {
        const input = this;
        Util.removeEventListener(input, "click input", removeHighlight);
        const styleClass = input.getAttribute(DATA_HIGHLIGHT_CLASS);

        if (styleClass) {
            input.removeAttribute(DATA_HIGHLIGHT_CLASS);
            const regex = new RegExp(" " + styleClass, "g");
            input.className = input.className.replace(regex, "");
            let label = input.getAttribute(DATA_HIGHLIGHT_LABEL);

            if (label) {
                input.removeAttribute(DATA_HIGHLIGHT_LABEL);
                label = labelsByFor[input.id];
                label.className = label.className.replace(regex, "");
            }
        }
    }
}