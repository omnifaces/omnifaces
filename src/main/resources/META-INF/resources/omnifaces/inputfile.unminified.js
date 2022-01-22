/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/**
 * Input file client side validator (so far only maxsize is validated).
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.InputFile
 * @since 2.5
 */
OmniFaces.InputFile = (function(window, document) {

	// Private static fields ------------------------------------------------------------------------------------------

	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Validate size of selected files of given o:inputFile against given maxsize.
	 * The faces message will be triggered server side.
	 * @param {Event} event Required; The involved DOM event (expected: 'change').
	 * @param {HTMLInputElement} inputFile Required; The involved o:inputFile.
	 * @param {string} messageClientId Required; The client ID of involved h:message.
	 * @param {number} maxsize Required; The maximum size for each selected file.
	 */
	self.validate = function(event, inputFile, messageClientId, maxsize) {
		if (!window.FileReader) {
			return true; // File API not supported (IE6-9). End of story. Let standard JSF code continue.
		}

		document.getElementById(messageClientId).innerHTML = ""; // Clear out any previously rendered message.

		for (var i = 0; i < inputFile.files.length; i++) {
			var file = inputFile.files[i];

			if (file.size > maxsize) {
				var fileName = file.name;
				var originalEnctype;

				if (window.mojarra) { // Mojarra doesn't add custom params when using iframe transport.
					originalEnctype = inputFile.form.enctype;
					inputFile.form.enctype = "application/x-www-form-urlencoded";
				}

				// Clear out selected files. Note: inputFile.value = null doesn't work in IE.
				inputFile.type = "text";
				inputFile.type = "file";
				
				var params = { fileName: fileName };
				params[OmniFaces.EVENT] = "validationFailed";
				jsf.ajax.request(inputFile.id, event, params);

				if (originalEnctype) {
					inputFile.form.enctype = originalEnctype;
				}

				return false;
			}
		}
		
		return true;
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(window, document);