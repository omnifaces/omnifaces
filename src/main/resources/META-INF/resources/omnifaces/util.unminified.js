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
	 * Add given event listener on the given event to the given element.
	 * @param {HTMLElement} element HTML element to add event listener to.
	 * @param {string} event The event name.
	 * @param {function} listener The event listener.
	 */
	self.addEventListener = function(element, event, listener) {
		if (element.addEventListener) {
			element.addEventListener(event, listener, false);
		}
		else if (element.attachEvent) { // IE6-8.
			element.attachEvent("on" + event, listener);
		}
	}

	/**
	 * Remove given event listener on the given event from the given element.
	 * @param {HTMLElement} element HTML element to remove event listener from.
	 * @param {string} event The event name.
	 * @param {function} listener The event listener.
	 */
	self.removeEventListener = function(element, event, listener) {
		if (element.removeEventListener) {
			element.removeEventListener(event, listener, false);
		}
		else if (element.detachEvent) { // IE6-8.
			element.detachEvent("on" + event, listener);
		}
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
			OmniFaces.Util.addEventListener(window, "load", listener);
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
	 * Add submit listener to the document and all JSF submit handler functions.
	 * @param {string} functionName The name of the submit function to decorate.
	 */
	self.addSubmitListener = function(listener) {
		addEventListener(document, "submit", listener); // Invoke given listener on any (propagated!) submit event (e.g. h:commandLink and p:commandButton ajax=false).

		if (window.jsf) {
			decorateFacesSubmit(jsf.ajax, "request", listener); // Decorate JSF ajax submit handler to invoke given listener first.
		}

		if (window.mojarra) {
			decorateFacesSubmit(mojarra, "jsfcljs", listener); // Decorate Mojarra h:commandLink submit handler to invoke given listener first.
		}

		if (window.myfaces) {
 			decorateFacesSubmit(myfaces.oam, "submitForm", listener); // Decorate MyFaces h:commandLink submit handler to invoke given listener first.
		}
		
		if (window.PrimeFaces) {
 			decorateFacesSubmit(PrimeFaces, "addSubmitParam", listener); // Decorate PrimeFaces p:commandLink submit handler to invoke given listener first.
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
	
	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Decorate the JavaScript submit function of JSF implementation to invoke the given listener first.
	 * @param {object} facesImpl The JSF implementation script object holding the submit function.
	 * @param {string} functionName The name of the submit function to decorate.
	 * @param {function} listener The name of the submit function to decorate.
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