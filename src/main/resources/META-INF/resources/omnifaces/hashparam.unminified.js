/*
 * Copyright 2018 OmniFaces
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
 * Hash param handling.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.HashParam
 * @since 3.2
 */
OmniFaces.HashParam = (function(Util, window) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var ERROR_MISSING_FORM = "OmniFaces HashParam: cannot find a JSF form in the document."
		+ " Updating hash parameter will not work. Either add a JSF form, or use ViewParam instead.";

	// Private static fields ------------------------------------------------------------------------------------------

	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * On page load, 
	 */
	self.init = function(clientId) {
		if (!!window.location.hash) {
			
			var form = Util.getFacesForm();
			
			if (!form) {
				if (window.jsf && jsf.getProjectStage() == "Development" && window.console && console.error) {
					console.error(ERROR_MISSING_FORM);
				}

				return;
			}

			var params = { clientId: clientId, hash: window.location.hash.substring(1) };
			params[OmniFaces.EVENT] = "setHashParamValues";
			jsf.ajax.request(form, null, params);
		}
	}

	/**
	 * Update a parameter in current hash string, which simulates a query string.
	 * @param {string} name The name of the parameter to update. If it doesn't exist, then it will be added.
	 * @param {string} value The value of the parameter to update. If it is falsey, then it will be removed.
	 */
	self.update = function(name, value) {
		var hash = window.location.hash;

		if (!!hash && hash.charAt(0) == '#') {
			hash = hash.substring(1);
		}

		window.location.hash = Util.updateParameter(hash, name, value);
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util, window);