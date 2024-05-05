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
			var originalAjaxRequest = jsf.ajax.request;

			jsf.ajax.request = function(source, event, options) {
				var originalGetViewState = jsf.getViewState;

				jsf.getViewState = function(form) {
					var originalViewState = originalGetViewState(form);
					var partialSubmit = form.attributes["data-partialsubmit"];

					if (!partialSubmit || partialSubmit.value != "true") {
						return originalViewState;
					}

					var execute = options ? options.execute : null;

					if (!execute || execute.indexOf("@form") != -1 || execute.indexOf("@all") != -1) {
						return originalViewState;
					}

					var executeIds = [];
					var encodedExecuteIds = [];

					if (execute.indexOf("@none") == -1) {
						executeIds = execute.replace("@this", source.id).split(" ");
						encodedExecuteIds = executeIds.map(encodeURIComponent);
						encodedExecuteIds.push(encodeURIComponent(form.id));
					}

					encodedExecuteIds.push(OmniFaces.VIEW_STATE_PARAM);
					encodedExecuteIds.push(OmniFaces.CLIENT_WINDOW_PARAM);

					var partialViewState = [];

					originalViewState.replace(/([^=&]+)=([^&]*)/g, function(_entry, key, value) {
						if (encodedExecuteIds.indexOf(key) > -1 || containsNamedChild(executeIds, key)) {
							partialViewState.push(key + "=" + value);
						}
					});

					return partialViewState.join("&");
				}

				originalAjaxRequest(source, event, options);
			}
		}

		if (window.PrimeFaces) { // PrimeFaces API.
			// TODO
		}
	}

	function containsNamedChild(executeIds, key) {
		var name = key.replace(/%3A/g, "\\:");

		try {
			for (var i = 0; i < executeIds.length; i++) {
				var parent = document.getElementById(executeIds[i]);

				if (parent && parent.querySelector("[name='" + name + "']")) {
					return true;
				}
			}
		}
		catch (e) {
			if (window.console && console.warn) {
				console.warn("Cannot determine if " + executeIds + " contains child " + name, e);
			}
		}

		return false;		
	}

	// Global initialization ------------------------------------------------------------------------------------------

	Util.addOnloadListener(init);

})(OmniFaces.Util, window);