/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * Highlight/focus.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.Highlight
 */
OmniFaces.Highlight = (function(Util, document) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var DATA_HIGHLIGHT_CLASS = "data-omnifaces-highlight-class";
	var DATA_HIGHLIGHT_LABEL = "data-omnifaces-highlight-label";

	// Private static fields ------------------------------------------------------------------------------------------

	var labelsByFor;
	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Apply the highlight. Add the given error style class to all input elements of the given client IDs and their
	 * associated labels. If doFocus is <code>true</code>, then also set the focus on the first input element. All
	 * non-existing input elements are ignored.
	 * @param {string[]} clientIds Array of client IDs of elements to highlight.
	 * @param {string} styleClass CSS style class to be set on the elements and the associated label elements.
	 * @param {boolean} doFocus Whether or not to put focus on the first highlighted element.
	 */
	self.apply = function(clientIds, styleClass, doFocus) {
		labelsByFor = getLabelsByFor();

		for (var i = 0; i < clientIds.length; i++) {
			var input = getElementByIdOrName(clientIds[i]);

			if (input) {
				input.className += " " + styleClass;
				input.setAttribute(DATA_HIGHLIGHT_CLASS, styleClass);
				var label = labelsByFor[input.id];

				if (label) {
					label.className += " " + styleClass;
					input.setAttribute(DATA_HIGHLIGHT_LABEL, label);
				}

				if (doFocus) {
					input.focus();
					doFocus = false;
				}

				Util.addEventListener(input, "input", removeHighlight);
			}
		}
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Return a mapping of all <code>label</code> elements keyed by their <code>for</code> attribute.
	 * @return {Object} A mapping of all <code>label</code> elements keyed by their <code>for</code> attribute.
	 */
	function getLabelsByFor() {
		var labels = document.getElementsByTagName("LABEL");
		var labelsByFor = {};

		for ( var i = 0; i < labels.length; i++) {
			var label = labels[i];
			var htmlFor = label.htmlFor;

			if (htmlFor) {
				labelsByFor[htmlFor] = label;
			}
		}

		return labelsByFor;
	}

	/**
	 * Returns an element by ID or name.
	 * @param {string} Client ID.
	 * @return {HTMLElement} HTML element identified by given client ID. 
	 */
	function getElementByIdOrName(clientId) {
		var element = document.getElementById(clientId);

		if (!element) {
			var elements = document.getElementsByName(clientId); // #21

			if (elements && elements.length) {
				element = elements[0];
			}
		}

		return element;
	}

	/**
	 * Remove the highlight. Remove the error style class from involved input element and its associated label.
	 * @param {Event} The input event.
	 */
	function removeHighlight() {
		var input = this;
		Util.removeEventListener(input, "input", removeHighlight);
		var styleClass = input.getAttribute(DATA_HIGHLIGHT_CLASS);

		if (styleClass) {
			input.removeAttribute(DATA_HIGHLIGHT_CLASS);
			var regex = new RegExp(" " + styleClass, "g");
			input.className = input.className.replace(regex, "");
			var label = input.getAttribute(DATA_HIGHLIGHT_LABEL);

			if (label) {
				input.removeAttribute(DATA_HIGHLIGHT_LABEL);
				label = labelsByFor[input.id];
				label.className = label.className.replace(regex, "");
			}
		}
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util, document);