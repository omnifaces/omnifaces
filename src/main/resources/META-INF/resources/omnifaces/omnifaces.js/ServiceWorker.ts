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

import { EVENT } from "./OmniFaces";

/**
 * Service worker register.
 * 
 * @author Bauke Scholtz
 * @since 3.7
 * @see PWAResourceHandler
 */
export module ServiceWorker {
	
	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * On page load, register the service worker.
	 * Also register a message event listener for "omnifaces.event" which in turn delegates to window events.
	 */
	export function init(serviceWorkerUrl: string, serviceWorkerScope: string) {
		if (!navigator.serviceWorker) {
			return; // Service workers not supported. End of story. https://caniuse.com/#feat=serviceworkers
		}

		navigator.serviceWorker.register(serviceWorkerUrl, { scope: serviceWorkerScope });
		navigator.serviceWorker.addEventListener("message", function(event) {
			if (event.data && event.data.type == EVENT) {
				window.dispatchEvent(new CustomEvent(event.data.name, { detail: event.data.detail }));
			}
		});
	}

}