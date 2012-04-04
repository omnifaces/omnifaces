OmniFaces.Ajax = function() {

	var onloadCallbacks = [];

	if (typeof jsf !== 'undefined') {
		jsf.ajax.addOnEvent(function(data) {
			if (data.status === 'success') {
				for (var i in onloadCallbacks) {
					if (onloadCallbacks.hasOwnProperty(i)) {
						onloadCallbacks[i].call(null);
					}
				}

				onloadCallbacks = [];
			}
		});
	}

	return {

		/**
		 * Add a new onload callback function which will also be executed on success of every ajax response.
		 */
		addOnload: function addOnload(callback) {
			if (typeof callback === 'function') {
				onloadCallbacks[onloadCallbacks.length] = callback;
			}
			else {
				throw new Error("OmniFaces.Ajax.addOnload: The given callback is not a function.");
			}
		}
	}

}();