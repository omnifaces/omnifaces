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
var OmniFaces = OmniFaces || {};

/**
 * Highlight/focus.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.Highlight
 */
OmniFaces.Highlight = (function(document) {

	// Private static fields ------------------------------------------------------------------------------------------

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
		var labelsByFor = getLabelsByFor();

		for (var i = 0; i < clientIds.length; i++) {
			var element = getElementByIdOrName(clientIds[i]);

			if (element) {
				element.className += ' ' + styleClass;
				var label = labelsByFor[element.id];

				if (label) {
					label.className += ' ' + styleClass;
				}

				if (doFocus) {
					element.focus();
					doFocus = false;
				}
			}
		}
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Return a mapping of all <code>label</code> elements keyed by their <code>for</code> attribute.
	 * @return {Object} A mapping of all <code>label</code> elements keyed by their <code>for</code> attribute.
	 */
	function getLabelsByFor() {
		var labels = document.getElementsByTagName('LABEL');
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
	
	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(document);