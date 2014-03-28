/*
 * Copyright 2014 OmniFaces.
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
 * Deferred script loader.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.DeferredScript
 * @since 1.8
 */
OmniFaces.DeferredScript = (function() {

	var deferredScript = {};
	var deferredScripts = [];
	var loading = false;

	deferredScript.add = function(url, begin, success, error) {
		if (loading) {
			return; // Sorry, too late to accept more.
		}

		if (!deferredScripts.length) {
			addOnloadListener(function() {
				loading = true;
				loadDeferredScript(0);
			});
		}

		deferredScripts.push({ url: url, begin: begin, success: success, error: error });
	}

	function addOnloadListener(listener) {
		if (window.addEventListener) {
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
	}

	function loadDeferredScript(index) {
		if (index < 0 || index >= deferredScripts.length) {
			return; // No such script.
		}

		var deferredScript = deferredScripts[index];
		var script = document.createElement("script");
		var head = document.head || document.documentElement;

		script.async = true;
		script.src = deferredScript.url;
		script.onerror = function() { if (deferredScript.error) deferredScript.error(); };
		script.onload = script.onreadystatechange = function(_, abort) {
			if (abort || !script.readyState || /loaded|complete/.test(script.readyState)) {
				script.onload = script.onreadystatechange = null; // IE memory leak fix.

				if (abort) {
					script.onerror();
				}
				else if (deferredScript.success) {
					deferredScript.success();
				}

				script = null;
				loadDeferredScript(index + 1); // Load next deferred script (regardless of current state).
			}
		};

		if (deferredScript.begin) {
			deferredScript.begin();
		}

		head.insertBefore(script, null); // IE6 has trouble with appendChild.
	}

	return deferredScript;

})();