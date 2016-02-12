/*
 * Copyright 2015 OmniFaces.
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
 * <p>Fire "unload" event to server side via synchronous XHR when the window is about to be unloaded as result of a 
 * non-submit event, so that e.g. any view scoped beans will immediately be destroyed when enduser refreshes page,
 * or navigates away, or closes browser.
 * <p>This script is automatically included when an <code>org.omnifaces.cdi.ViewScoped</code> managed bean is created
 * during render response, otherwise the one already in <code>omnifaces.js</code> will be used.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.cdi.ViewScopeManager
 * @since 2.2
 */
OmniFaces.Unload = (function(window, document) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var VIEW_STATE_PARAM = "javax.faces.ViewState";

	// Private static fields ------------------------------------------------------------------------------------------

	var disabled = false;
	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Initialize the unload event listener on the current document. This will check if XHR is supported and if the
	 * current document has a JSF view state element. If so, then register the <code>beforeunload</code> event to send
	 * a synchronous XHR request with the OmniFaces view scope ID and the JSF view state value as parameters. Also
	 * register the <code>submit</code> event to disable the unload event listener.
	 * @param {string} id The OmniFaces view scope ID.
	 */
	self.init = function(id) {
		if (!window.XMLHttpRequest) {
			return; // Native XHR not supported (IE6/7 not supported). End of story. Let session expiration do its job.
		}

		var viewState = getViewState();

		if (!viewState) {
			return; // No JSF form in the document? Why is it referencing a view scoped bean then? ;)
		}

		addEventListener(window, "beforeunload", function() {
			if (disabled) {
				disabled = false; // Just in case some custom JS explicitly triggered submit event while staying in same DOM.
				return;
			}

			try {
				var xhr = new XMLHttpRequest();
				xhr.open("POST", window.location.href.split(/[?#;]/)[0], false);
				xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
				xhr.send("omnifaces.event=unload&id=" + id + "&" + VIEW_STATE_PARAM + "=" + encodeURIComponent(viewState));
			}
			catch (e) {
				// Fail silently. You never know.
			}
		});

		addEventListener(document, "submit", function() {
			self.disable(); // Disable unload event on any (propagated!) submit event.
		});
	}

	/**
	 * Disable the unload event listener on the current document.
	 * It will be re-enabled when the DOM has not changed during the unload event.
	 */
	self.disable = function() {
		disabled = true;
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Get the view state value from the current document.
	 * @return {string} The view state value from the current document.
	 */
	function getViewState() {
		for (var i = 0; i < document.forms.length; i++) {
			var viewStateElement = document.forms[i][VIEW_STATE_PARAM];

			if (viewStateElement) {
				return viewStateElement.value;
			}
		}

		return null;
	}

	/**
	 * Add an event listener on the given event to the given element.
	 * @param {HTMLElement} HTML element to add event listener to.
	 * @param {string} The event name.
	 * @param {function} The event listener.
	 */
	function addEventListener(element, event, listener) {
		if (element.addEventListener) {
			element.addEventListener(event, listener, false);
		}
		else if (element.attachEvent) { // IE6-8.
			element.attachEvent("on" + event, listener);
		}
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(window, document);