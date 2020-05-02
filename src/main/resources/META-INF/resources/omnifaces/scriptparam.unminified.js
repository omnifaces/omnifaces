/*
 * Copyright 2020 OmniFaces
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
 * @see org.omnifaces.component.input.ScriptParam
 * @since 3.
 */
OmniFaces.ScriptParam = (function(Util, window, document) {

	// "Constant" fields ----------------------------------------------------------------------------------------------

	var ERROR_MISSING_FORM = "OmniFaces ScriptParam: cannot find a JSF form in the document."
		+ " Setting script parameters in bean will not work.";

	// Private static fields ------------------------------------------------------------------------------------------

	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * On page load, send all evaluated script results to the bean.
	 */
	self.run = function(scriptParamId, scripts) {
		var form = Util.getFacesForm();

		if (!form) {
			if ((!window.jsf || jsf.getProjectStage() == "Development") && window.console && console.error) {
				console.error(ERROR_MISSING_FORM);
			}
			
			return;
		}
		
		for (var clientId in scripts) {
			scripts[clientId] = JSON.stringify(clone(scripts[clientId]));
		}

		var params = { execute: scriptParamId, params: scripts };
		params[OmniFaces.EVENT] = "setScriptParamValues";
		jsf.ajax.request(form, null, params);
	}

	// Private static functions ---------------------------------------------------------------------------------------

	function clone(object) {
		if (!(object instanceof Object)) {
			return object;
		}

		var clone = {};
		
		for (var property in object) { 
			object[property] instanceof Function || object[property] instanceof Object || (clone[property] = object[property]);
		}

		return clone;
	}

	// Expose self to public ------------------------------------------------------------------------------------------

	return self;

})(OmniFaces.Util, window, document);