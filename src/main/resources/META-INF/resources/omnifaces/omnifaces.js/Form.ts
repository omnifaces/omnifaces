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

				if (execute.indexOf("@none") == -1) {
					executeIds = execute.replace("@this", params[0].id).split(" ").map(encodeURIComponent);
					executeIds.push(encodeURIComponent(form.id));
				}

				executeIds.push(VIEW_STATE_PARAM);
				executeIds.push(CLIENT_WINDOW_PARAM);

				const partialViewState: string[] = [];
				const encodedSeparatorChar = encodeURIComponent(faces.separatorchar);

				originalViewState.replace(/([^=&]+)=([^&]*)/g, function(_entry: any, key: string, value: string) {
					for (
						let clientId = key; 
						clientId.indexOf(encodedSeparatorChar) > -1;
						clientId = clientId.substring(0, clientId.lastIndexOf(encodedSeparatorChar))) 
					{
						if (executeIds.indexOf(clientId) > -1) {
							partialViewState.push(key + "=" + value);
							break;
						}
					}
				}); 

				return partialViewState.join("&");
			}
		}
	}

	// Global initialization ------------------------------------------------------------------------------------------

	Util.addOnloadListener(init);

}