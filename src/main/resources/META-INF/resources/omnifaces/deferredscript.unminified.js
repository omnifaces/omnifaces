/*
 * Copyright 2021 OmniFaces
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
 * Deferred script loader.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.DeferredScript
 * @since 1.8
 */
OmniFaces.DeferredScript = (function(Util) {

	// Private static fields ------------------------------------------------------------------------------------------

	var deferredScripts = [];
	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * Add a deferred script to the loader and registers the onload listener to load the first deferred script.
	 * @param {string} url Required; The URL of the deferred script.
	 * @param {function} begin Optional; Function to invoke before deferred script is loaded.
	 * @param {function} success Optional; Function to invoke after deferred script is successfully loaded.
	 * @param {function} error Optional; Function to invoke when loading of deferred script failed.
	 */
	self.add = function(url, begin, success, error) {
		deferredScripts.push({
			url: url, 
			begin: begin, 
			success: success, 
			error: error
		});

		if (deferredScripts.length == 1) {
			Util.addOnloadListener(function() {
				loadDeferredScript(0);
			});
		}
	}

	// Private static functions ---------------------------------------------------------------------------------------

	/**
	 * Load the deferred script of the given index. When loaded, then it will implicitly load the next deferred script.
	 * @param {int} index The index of the deferred script to be loaded. If no one exists, then the method returns.
	 */
	function loadDeferredScript(index) {
		if (index < 0 || index >= deferredScripts.length) {
			return; // No such script.
		}

		var deferredScript = deferredScripts[index];
		var completeFunction = function() {
			loadDeferredScript(index + 1);
		};

		Util.loadScript(deferredScript.url, null, deferredScript.begin, deferredScript.success, deferredScript.error, completeFunction);
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util);