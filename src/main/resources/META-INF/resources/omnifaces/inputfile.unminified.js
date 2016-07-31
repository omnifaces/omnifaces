/*
 * Copyright 2016 OmniFaces.
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
 * Input file client side validator (so far only maxsize is validated).
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.InputFile
 * @since 2.5
 */
OmniFaces.InputFile = (function(window) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var VIEW_STATE_PARAM = "javax.faces.ViewState";

	// Private static fields ------------------------------------------------------------------------------------------

	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * @param {Event} event Required; The involved DOM event (expected: 'change').
	 * @param {HTMLInputElement} inputFile Required; The input file element to be validated.
	 */
	self.validate = function(event, inputFile) {
		if (!window.FileReader) {
			return; // File API not supported, end of story. Server side will validate.
		}

		if (!inputFile.value) {
			return; // No files selected.
		}

		var files = inputFile.files;
		var maxsize = parseInt(inputFile.dataset.maxsize);

		for (var i = 0; i < files.length; i++) {
			var size = files[i].size;

			if (size > maxsize) {
				var fileName = files[i].name;
				var originalEnctype = inputFile.form.enctype;
				inputFile.form.enctype = null;
				inputFile.value = null;
				jsf.ajax.request(inputFile.id, event, { "omnifaces.event": "validationFailed", fileName: fileName });
				inputFile.form.enctype = originalEnctype;
				return false;
			}
		}
		
		return true;
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(window);