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
OmniFaces.Util = {

	// Public static functions ----------------------------------------------------------------------------------------
		
	/**
	 * Register the given function as window onload listener function.
	 * @param {function} listener The function to be invoked during window load event.
	 */
	addOnloadListener: function(listener) {
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
	},

	/**
	 * Add given event listener on the given event to the given element.
	 * @param {HTMLElement} HTML element to add event listener to.
	 * @param {string} The event name.
	 * @param {function} The event listener.
	 */
	addEventListener: function(element, event, listener) {
		if (element.addEventListener) {
			element.addEventListener(event, listener, false);
		}
		else if (element.attachEvent) { // IE6-8.
			element.attachEvent("on" + event, listener);
		}
	},

	/**
	 * Remove given event listener on the given event from the given element.
	 * @param {HTMLElement} HTML element to remove event listener from.
	 * @param {string} The event name.
	 * @param {function} The event listener.
	 */
	removeEventListener: function(element, event, listener) {
		if (element.removeEventListener) {
			element.removeEventListener(event, listener, false);
		}
		else if (element.detachEvent) { // IE6-8.
			element.detachEvent("on" + event, listener);
		}
	},

	/**
	 * If given function is actually not a function, then try to interpret it as name of a global function.
	 * If it still doesn't resolve to anything, then return a NOOP function.
	 * @param {Object} fn Can be function, or string representing function name, or undefined.
	 */
	resolveFunction: function(fn) {
		return (typeof fn !== "function") && (fn = window[fn] || function(){}), fn;
	}
	
};