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
OmniFaces.Highlight = {

	/**
	 * Add the given error style class to all input elements of the given client IDs and their associated labels.
	 * If doFocus is <code>true</code>, then also set the focus on the first input element. All non-existing input 
	 * elements are ignored.
	 */
	addErrorClass: function(clientIds, styleClass, doFocus) {
		var labels = document.getElementsByTagName('LABEL');
		var labelsByFor = {};

		for ( var i = 0; i < labels.length; i++) {
			var label = labels[i];
			var htmlFor = label.htmlFor;

			if (htmlFor) {
				labelsByFor[htmlFor] = label;
			}
		}
		
		for (var i = 0; i < clientIds.length; i++) {
			var clientId = clientIds[i];
			var element = document.getElementById(clientId);

			if (!element) {
				var elements = document.getElementsByName(clientId); // #21

				if (elements && elements.length) {
					element = elements[0];
				}
			}

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
};