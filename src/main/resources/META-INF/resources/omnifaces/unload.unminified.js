/*
 * Copyright 2018 OmniFaces
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
 * Fire "unload" event to server side via synchronous XHR when the window is about to be unloaded as result of a 
 * non-submit event, so that e.g. any view scoped beans will immediately be destroyed when enduser refreshes page,
 * or navigates away, or closes browser.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.ViewScopeManager
 * @since 2.2
 */
OmniFaces.Unload = (function(Util, navigator, window, document) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var ERROR_MISSING_FORM = "OmniFaces @ViewScoped: cannot find a JSF form in the document."
		+ " Unload will not work. Either add a JSF form, or use @RequestScoped instead.";

	// Private static fields ------------------------------------------------------------------------------------------

	var id;
	var disabled;
	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Initialize the unload event listener on the current document. This will check if XHR is supported and if the
	 * current document has a JSF form with a view state element. If so, then register the <code>unload</code> event to
	 * send a beacon or synchronous XHR request with the OmniFaces view scope ID and the JSF view state value as
	 * parameters. Also register the all JSF <code>submit</code> events to disable the unload event listener.
	 * @param {string} viewScopeId The OmniFaces view scope ID.
	 */
	self.init = function(viewScopeId) {
		if (!window.XMLHttpRequest) {
			return; // Native XHR not supported (IE6/7 not supported). End of story. Let session expiration do its job.
		}

		if (id == null) {
			var form = Util.getFacesForm();

			if (!form) {
				if ((!window.jsf || jsf.getProjectStage() == "Development") && window.console && console.error) {
					console.error(ERROR_MISSING_FORM);
				}

				return;
			}

			Util.addEventListener(window, window.onbeforeunload ? "unload" : "beforeunload", function() { // If there's no user-defined onbeforeunload handler, let's perform job during beforeunload instead, which is more reliable.
				if (disabled) {
					self.reenable(); // Just in case some custom JS explicitly triggered submit event while staying in same DOM.
					return;
				}

				try {
					var url = form.action;
					var query = OmniFaces.EVENT + "=unload&id=" + id + "&" + OmniFaces.VIEW_STATE_PARAM + "=" + encodeURIComponent(form[OmniFaces.VIEW_STATE_PARAM].value);
					var contentType = "application/x-www-form-urlencoded";

					if (navigator.sendBeacon) {
						// Synchronous XHR is deprecated during unload event, modern browsers offer Beacon API for this which will basically fire-and-forget the request.
						navigator.sendBeacon(url, new Blob([query], {type: contentType}));
					}
					else {
						var xhr = new XMLHttpRequest();
						xhr.open("POST", url, false);
						xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
						xhr.setRequestHeader("Content-Type", contentType);
						xhr.send(query);
					}
				}
				catch (e) {
					// Fail silently. You never know.
				}
			});

			Util.addSubmitListener(function() {
				self.disable(); // Disable unload event on any submit event.
			});
		}

		id = viewScopeId;
		disabled = false;
	}

	/**
	 * Disable the unload event listener on the current document.
	 * It will automatically be re-enabled when the DOM has not changed during the unload event.
	 */
	self.disable = function() {
		disabled = true;
	}

	/**
	 * Re-enable the unload event listener on the current document.
	 */
	self.reenable = function() {
		disabled = false;
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util, navigator, window, document);