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
		if (document.readyState === "complete") {
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
	 * Get the first JSF form containing view state param from the current document.
	 * @return {HTMLFormElement} The first JSF form of the current document.
	 */
	self.getFacesForm = function() {
		for (var i = 0; i < document.forms.length; i++) {
			if (document.forms[i]["javax.faces.ViewState"]) {
				return document.forms[i];
			}
		}

		return null;
	}

	/**
	 * Update a parameter in given query string in application/x-www-url-encoded format.
	 * @param {string} query The query string.
	 * @param {string} name The name of the parameter to update. If it doesn't exist, then it will be added.
	 * @param {string} value The value of the parameter to update. If it is falsey, then it will be removed.
	 */
	self.updateParameter = function(query, name, value) {
		var re = new RegExp("(^|[?&#])" + name + "=.*?([&#]|$)", "i");

		if (value) {
			var parameter = name + "=" + encodeURIComponent(value);

			if (!query) {
				query = parameter;
			}
			else if (query.match(re)) {
				query = query.replace(re, "$1" + parameter + "$2");
			}
			else {
				query += "&" + parameter;
			}
		}
		else {
			query = query.replace(re, "$2");
		}

		if (query.charAt(0) == "&") {
			query = query.substring(1);
		}
		
		return query;
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