/**
 * Highlight/focus.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.Highlight
 */
OmniFaces.Highlight = {

	/**
	 * Add the given error style class to all input elements of the given client IDs and their associated labels.
	 * If doFocus is <code>true</code>, then also set the focus on the first input element. All non-existing input 
	 * elements are ignored.
	 */
	addErrorClass: function(clientIds, styleClass, doFocus) {
		var labels = document.getElementsByTagName('LABEL');
		var labelsByFor = {};

		for ( var i = 0; i < labels.length; i++) {
			var label = labels[i];
			var htmlFor = label.htmlFor;

			if (htmlFor) {
				labelsByFor[htmlFor] = label;
			}
		}
		
		for (var i = 0; i < clientIds.length; i++) {
			var clientId = clientIds[i];
			var element = document.getElementById(clientId);

			if (!element) {
				var elements = document.getElementsByName(clientId); // #21

				if (elements && elements.length) {
					element = elements[0];
				}
			}

			if (element) {
				element.className += ' ' + styleClass;
				var label = labelsByFor[element.id];

				if (label) {
					label.className += ' ' + styleClass;
				}

				if (doFocus) {
					element.focus();
					doFocus = false;
				}
			}
		}
	}
};