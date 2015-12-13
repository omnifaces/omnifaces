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
		else if (window.addEventListener) {
			window.addEventListener("load", listener, false);
		}
		else if (window.attachEvent) {
			window.attachEvent("onload", listener);
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
	 * If given function is actually not a function, then try to interpret it as name of a global function.
	 * If it still doesn't resolve to anything, then return a NOOP function.
	 * @param {Object} fn Can be function, or string representing function name, or undefined.
	 */
	resolveFunction: function(fn) {
		return (typeof fn !== "function") && (fn = window[fn] || function(){}), fn;
	}
	
};