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
 * Hash param handling.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.HashParam
 * @since 3.2
 */
OmniFaces.HashParam = (function(Util, window, document) {

	// Private static fields ------------------------------------------------------------------------------------------

	var id;
	var updating;
	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * On page load, send any hash parameters to the bean.
	 */
	self.init = function(hashParamId) {
		id = hashParamId;

		if (!Util.getFacesForm()) {
			return;
		}

		if (!!window.location.hash) {
			setHashParamValues();
		}
		
		Util.addEventListener(window, "hashchange", setHashParamValues);
	}

	/**
	 * Update a parameter in current hash string, which simulates a query string.
	 * @param {string} name The name of the parameter to update. If it doesn't exist, then it will be added.
	 * @param {string} value The value of the parameter to update. If it is falsey, then it will be removed.
	 */
	self.update = function(name, value) {
		updating = true;
		var location = window.location;
		var oldHashQueryString = location.hash;

		if (!!oldHashQueryString && oldHashQueryString.charAt(0) == '#') {
			oldHashQueryString = oldHashQueryString.substring(1);
		}

		var newHashQueryString = Util.updateParameter(oldHashQueryString, name, value);

		if (newHashQueryString != oldHashQueryString) {
			var history = window.history;
			if (history && history.pushState) {
				var url = location.href.split(/#/, 2)[0] + (newHashQueryString ? "#" : "") + newHashQueryString;
				history.pushState(null, document.title, url);
			}
			else {
				location.hash = newHashQueryString;
			}
		}

		updating = false;
	}

	// Private static functions ---------------------------------------------------------------------------------------

	function setHashParamValues() {
		if (!updating) {
			var params = { execute: id, hash: window.location.hash.substring(1) };
			params[OmniFaces.EVENT] = "setHashParamValues";
			var faces = window.faces || window.jsf;
			faces.ajax.request(Util.getFacesForm(), null, params);
		}
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util, window, document);