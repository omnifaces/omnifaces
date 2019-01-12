/*
 * Copyright 2019 OmniFaces
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
 * Form partial submit.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.Form
 * @since 3.0
 */
OmniFaces.Form = (function(Util, window) {

	// Private static functions ---------------------------------------------------------------------------------------

	function init() {
		if (window.jsf) { // Standard JSF API.
			var originalGetViewState = jsf.getViewState;

			jsf.getViewState = function(form) {
				var originalViewState = originalGetViewState(form);

				if (form.attributes["data-partialsubmit"] != "true") {
					return originalViewState;
				}

				var execute = jsf.ajax.request.arguments ? jsf.ajax.request.arguments[2].execute : null;

				if (!execute || execute.indexOf("@form") != -1 || execute.indexOf("@all") != -1) {
					return originalViewState;
				}

				var executeIds = [];

				if (execute.indexOf("@none") == -1) {
					executeIds = execute.replace("@this", jsf.ajax.request.arguments[0].id).split(" ").map(encodeURIComponent);
					executeIds.push(encodeURIComponent(form.id));
				}

				executeIds.push(OmniFaces.VIEW_STATE_PARAM);
				executeIds.push(OmniFaces.CLIENT_WINDOW_PARAM);

				var partialViewState = [];

				originalViewState.replace(/([^=&]+)=([^&]*)/g, function(entry, key, value) {
					if (executeIds.indexOf(key) > -1) {
						partialViewState.push(key + "=" + value);
					}
				}); 

				return partialViewState.join("&");
			}
		}

		if (window.PrimeFaces) { // PrimeFaces API.
			// TODO
		}
	}

	// Global initialization ------------------------------------------------------------------------------------------

	Util.addOnloadListener(init);

})(OmniFaces.Util, window);