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

import { VIEW_STATE_PARAM } from "./OmniFaces";
import { CLIENT_WINDOW_PARAM } from "./OmniFaces";
import { Util } from "./Util";

/**
 * Form partial submit.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.Form
 * @since 3.0
 */
export module Form {

	// Private static functions ---------------------------------------------------------------------------------------

	function init() {
		const faces = window.faces || window.jsf;

		if (faces) { // Standard JSF API.
			const originalGetViewState = faces.getViewState;

			faces.getViewState = function(form: HTMLFormElement) {
				const originalViewState = originalGetViewState(form);

				if (form.dataset["partialsubmit"] != "true") {
					return originalViewState;
				}

				const params = faces.ajax.request.arguments;
				const execute = params ? params[2].execute : null;

				if (!execute || execute.indexOf("@form") != -1 || execute.indexOf("@all") != -1) {
					return originalViewState;
				}

				let executeIds: string[] = [];
				let encodedExecuteIds: string[] = [];
				let implicitExecuteIds: string[] = [];

				if (execute.indexOf("@none") == -1) {
					executeIds = execute.replace("@this", params[0].id).split(" ");
					encodedExecuteIds = executeIds.map(encodeURIComponent);
					implicitExecuteIds.push(encodeURIComponent(form.id));
				}

				implicitExecuteIds.push(VIEW_STATE_PARAM);
				implicitExecuteIds.push(CLIENT_WINDOW_PARAM);

				const partialViewState: string[] = [];

				originalViewState.replace(/([^=&]+)=([^&]*)/g, function(_entry: any, key: string, value: string) {
					if (implicitExecuteIds.indexOf(key) > -1 || encodedExecuteIds.indexOf(key) > -1 || containsNamedChild(executeIds, key)) {
						partialViewState.push(key + "=" + value);
					}
				}); 

				return partialViewState.join("&");
			}
		}
	}

	function containsNamedChild(executeIds: string[], key: string) {
		var name = key.replace("%3A", "\\:");

		try {
			for (let executeId of executeIds) {
				var parent = document.getElementById(executeId);

				if (parent && parent.querySelector("[name='" + name + "']")) {
					return true;
				}
			}
		}
		catch (e) {
			console.warn("Cannot determine if " + executeIds + " contains child " + name, e);
		}

		return false;		
	}

	// Global initialization ------------------------------------------------------------------------------------------

	Util.addOnloadListener(init);

}