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
 * Script param handling.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.ScriptParam
 * @since 3.6
 */
OmniFaces.ScriptParam = (function(Util) {

	// Private static fields ------------------------------------------------------------------------------------------

	var self = {};

	// Public static functions ----------------------------------------------------------------------------------------

	/**
	 * On page load, send all evaluated script results to the bean.
	 */
	self.run = function(scriptParamId, scripts) {
		var form = Util.getFacesForm();

		if (!form) {
			return;
		}
		
		var params = {};

		for (var clientId in scripts) {
			params[clientId] = JSON.stringify(clone(scripts[clientId]));
		}

		params["execute"] = scriptParamId;
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

})(OmniFaces.Util);