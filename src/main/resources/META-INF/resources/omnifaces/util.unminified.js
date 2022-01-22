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
 * Utility scripts.
 * 
 * @author Bauke Scholtz
 * @since 2.2
 */
OmniFaces.Util = (function(window, document) {

	// Private static fields ------------------------------------------------------------------------------------------

	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Add given event listener on the given events to the given element.
	 * @param {HTMLElement} element HTML element to add event listener to.
	 * @param {string} events Space separated string of event names.
	 * @param {function} listener The event listener.
	 */
	self.addEventListener = function(element, events, listener) {
		handleEventListener(element, "addEventListener", "attachEvent", events, listener);
	}

	/**
	 * Remove given event listener on the given events from the given element.
	 * @param {HTMLElement} element HTML element to remove event listener from.
	 * @param {string} events Space separated string of event names.
	 * @param {function} listener The event listener.
	 */
	self.removeEventListener = function(element, events, listener) {
		handleEventListener(element, "removeEventListener", "detachEvent", events, listener);
	}

	/**
	 * Register the given function as window onload listener function.
	 * @param {function} listener The function to be invoked during window load event.
	 */
	self.addOnloadListener = function(listener) {
		if (window.jQuery) {
			jQuery(listener);
		}
		else if (document.readyState === "complete") {
			setTimeout(listener);
		}
		else if (window.addEventListener || window.attachEvent) {
			self.addEventListener(window, "load", listener);
		}
		else if (typeof window.onload === "function") {
			var oldListener = window.onload;
			window.onload = function() { oldListener(); listener(); };
		}
		else {
			window.onload = listener;
		}
	}

	/**
	 * Add submit listener to the document and all synchronous JSF submit handler functions.
	 * @param {function} listener The listener to invoke before the (synchronous) submit.
	 */
	self.addSubmitListener = function(listener) {
		self.addEventListener(document, "submit", listener); // Invoke given listener on any (propagated!) submit event (e.g. h:commandLink and p:commandButton ajax=false).

		if (window.mojarra) {
			decorateFacesSubmit(mojarra, "jsfcljs", listener); // Decorate Mojarra h:commandLink submit handler to invoke given listener first.
		}

		if (window.myfaces) {
 			decorateFacesSubmit(myfaces.oam, "submitForm", listener); // Decorate MyFaces h:commandLink submit handler to invoke given listener first.
		}

		if (window.PrimeFaces) {
 			decorateFacesSubmit(PrimeFaces, "addSubmitParam", listener); // Decorate PrimeFaces p:commandLink ajax=false submit handler to invoke given listener first.
		}
	}

	/**
	 * If given function is actually not a function, then try to interpret it as name of a global function.
	 * If it still doesn't resolve to anything, then return a NOOP function.
	 * @param {Object} fn Can be function, or string representing function name, or undefined.
	 */
	self.resolveFunction = function(fn) {
		return (typeof fn !== "function") && (fn = window[fn] || function(){}), fn;
	}

	/**
	 * Load a script.
	 * @param {string} url Required; The URL of the script.
	 * @param {function} begin Optional; Function to invoke before deferred script is loaded.
	 * @param {function} success Optional; Function to invoke after deferred script is successfully loaded.
	 * @param {function} error Optional; Function to invoke when loading of deferred script failed.
	 * @param {function} complete Optional; Function to invoke after deferred script is loaded, regardless of its success/error outcome.
	 */
	self.loadScript = function(url, begin, success, error, complete) {
		var beginFunction = self.resolveFunction(begin);
		var successFunction = self.resolveFunction(success);
		var errorFunction = self.resolveFunction(error);
		var completeFunction = self.resolveFunction(complete);

		var script = document.createElement("script");
		var head = document.head || document.documentElement;

		script.async = true;
		script.src = url;
		script.setAttribute("crossorigin", "anonymous");

		script.onerror = function() {
			errorFunction();
		}

		script.onload = script.onreadystatechange = function(_, abort) {
			if (abort || !script.readyState || /loaded|complete/.test(script.readyState)) {
				script.onload = script.onreadystatechange = null; // IE memory leak fix.

				if (abort) {
					script.onerror();
				}
				else {
					successFunction();
				}

				script = null;
				completeFunction();
			}
		}

		self.addOnloadListener(function() {
			beginFunction();
			head.insertBefore(script, null); // IE6 has trouble with appendChild.
		});
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Handle the given element via the given standard (W3C) and MS (IE6-8) function names to add or remove the given
	 * event listener on the given events.
	 * @param {HTMLElement} element HTML element to be altered.
	 * @param {string} standardFunctionName Standard (W3C) event handler function name to invoke on the given element.
	 * @param {string} msFunctionName MS (IE6-8) event handler function name to invoke on the given element.
	 * @param {string} events Space separated string of event names.
	 * @param {function} listener The event listener to be added or removed on the given element via given functions.
	 */
	function handleEventListener(element, standardFunctionName, msFunctionName, events, listener) {
		var eventParts = events.replace(/^\s+|\s+$/g, "").split(/\s+/);

		for (var i = 0; i < eventParts.length; i++) {
			var event = eventParts[i];

			if (element[standardFunctionName]) {
				element[standardFunctionName](event, listener);
			}
			else if (element[msFunctionName]) { // IE6-8.
				element[msFunctionName]("on" + event, listener);
			}
		}
	}

	/**
	 * Decorate the JavaScript based submit function of the JSF implementation to invoke the given listener first.
	 * @param {object} facesImpl The JSF implementation script object holding the submit function.
	 * @param {string} functionName The name of the JSF submit function to decorate.
	 * @param {function} listener The listener to invoke before the JSF submit function.
	 */
	function decorateFacesSubmit(facesImpl, functionName, listener) {
		var submitFunction = facesImpl[functionName];

		if (submitFunction) {
			facesImpl[functionName] = function() {
				listener();
				return submitFunction.apply(this, arguments);
			};
		}
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(window, document);