/**
 * Highlight/focus.
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.script.Highlight
 */
OmniFaces.Highlight = {
		
	/**
	 * Add the given error style class to all elements of the given client IDs. If doFocus is <code>true</code>, then
	 * also set the focus on the first element. All non-existing elements are ignored.
	 */
	addErrorClass: function(clientIds, styleClass, doFocus) {
		var focused = !doFocus;

	    for (var i = 0; i < clientIds.length; i++) {
	        var element = document.getElementById(clientIds[i]);

	        if (element) {
	            element.className += ' ' + styleClass;

	            if (!focused) {
	            	element.focus();
	            	focused = true;
	            }
	        }
	    }
	}
};