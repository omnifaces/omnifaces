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

	deferredScript.add = function(url, begin, success, error) {
		deferredScripts.push({ url: url, begin: begin, success: success, error: error });

		if (deferredScripts.length == 1) {
			OmniFaces.Util.addOnloadListener(function() {
				loadDeferredScript(0);
			});
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
		script.onerror = function() {
			if (deferredScript.error) {
				deferredScript.error();
			}
		};
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