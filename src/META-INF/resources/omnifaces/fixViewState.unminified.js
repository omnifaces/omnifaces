/*
 * Copyright 2013 OmniFaces.
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
 * <p>Fix JSF view state if necessary. In Mojarra, it get lost on certain forms during certain ajax updates (e.g. 
 * updating content which in turn contains another form). When this script is loaded <em>after</em> the standard jsf.js
 * script containing standars JSF ajax API, then it will be automatically applied on all JSF ajax requests.
 * <pre>
 * &lt;h:outputScript library="javax.faces" name="jsf.js" target="head" /&gt;
 * &lt;h:outputScript library="omnifaces" name="fixViewState.js" target="head" /&gt;
 * </pre>
 * <p>This script also recognizes jQuery ajax API as used by some component libraries such as PrimeFaces, it will then
 * be automatically applied on all jQuery ajax requests.
 * <pre>
 * &lt;h:outputScript library="primefaces" name="jquery/jquery.js" target="head" /&gt;
 * &lt;h:outputScript library="omnifaces" name="fixViewState.js" target="head" /&gt;
 * </pre>
 * <p>Explicit declaration of jsf.js or jquery.js is not necessary. In that case you need to put the 
 * <code>&lt;h:outputScript&gt;</code> tag inside the <code>&lt;h:body&gt;</code> to ensure that it's loaded 
 * <em>after</em> the JSF and/or jQuery script.
 * <p>In case your JSF component library doesn't utilize standard JSF nor jQuery ajax API, but a proprietary one, and
 * exposes the missing view state problem, then you can still apply this script manually during the "complete" event of
 * the ajax request whereby the concrete <code>XMLHttpRequest</code> instance is available as some argument as follows:
 * <pre>
 * function someOncompleteCallbackFunction(xhr) {
 *     OmniFaces.FixViewState.apply(xhr.responseXML);
 * });
 * </pre>
 * <p>This was scheduled to be fixed in JSF 2.2 spec, however it was postponed to JSF 2.3. Note that this fix is not
 * necessary for MyFaces as they have internally already fixed it for long in their jsf.js.
 * 
 * @author Bauke Scholtz
 * @link https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-790
 * @since 1.7
 */
OmniFaces.FixViewState = (function() {

	var fixViewState = {};
	var viewStateParam = "javax.faces.ViewState";
	var viewStateRegex = new RegExp("^([\\w]+:)?" + viewStateParam.replace(/\./g, "\\.") + "(:[0-9]+)?$");

	/**
	 * Apply the "fix view state" on the current document based on the given XML response.
	 */
	fixViewState.apply = function(responseXML) {
		if (typeof responseXML === "undefined") {
			return;
		}

		var viewState = getViewState(responseXML);

		if (!viewState) {
			return;
		}

		for (var i = 0; i < document.forms.length; i++) {
			var form = document.forms[i];

			if (form.method == "post") {
				if (!hasViewState(form)) {
					createViewState(form, viewState);
				}
			}
			else { // PrimeFaces also adds them to GET forms!
				removeViewState(form);
			}
		}
	};

	/**
	 * Get the view state value from the given XML response.
	 */
	function getViewState(responseXML) {
		var updates = responseXML.getElementsByTagName("update");

		for (var i = 0; i < updates.length; i++) {
			var update = updates[i];

			if (viewStateRegex.exec(update.getAttribute("id"))) {
				return update.firstChild.nodeValue;
			}
		}

		return null;
	}

	/**
	 * Returns whether the given form has already a view state hidden field.
	 */
	function hasViewState(form) {
		for (var i = 0; i < form.elements.length; i++) {
			if (form.elements[i].name == viewStateParam) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Create view state hidden field and add it to given form.
	 */
	function createViewState(form, viewState) {
		var hidden;

		try {
			hidden = document.createElement("<input name='" + viewStateParam + "'>"); // IE6-8.
		} catch(e) {
			hidden = document.createElement("input");
			hidden.setAttribute("name", viewStateParam);
		}

		hidden.setAttribute("type", "hidden");
		hidden.setAttribute("value", viewState);
		hidden.setAttribute("autocomplete", "off");
		form.appendChild(hidden);
	}

	/**
	 * Remove view state hidden field from given form.
	 */
	function removeViewState(form) {
		for (var i = 0; i < form.elements.length; i++) {
			var element = form.elements[i];

			if (element.name == viewStateParam) {
				element.parentNode.removeChild(element);
			}
		}
	}
	
	return fixViewState;

})();

// Global initialization for standard JSF ajax API.
if (typeof jsf !== "undefined") {
	jsf.ajax.addOnEvent(function(data) {
		if (data.status == "success") {
			OmniFaces.FixViewState.apply(data.responseXML);
		}
	});
}

// Global initialization for jQuery ajax API.
if (typeof jQuery !== "undefined") {
	jQuery(document).ajaxComplete(function(event, xhr, options) {
		OmniFaces.FixViewState.apply(xhr.responseXML);
	});
}