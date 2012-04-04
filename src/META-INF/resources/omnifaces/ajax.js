OmniFaces.Ajax = function() {

	var runOnceOnSuccessCallbacks = [];

	if (typeof jsf !== 'undefined') {
		jsf.ajax.addOnEvent(function(data) {
			if (data.status === 'success') {
				for (var i in runOnceOnSuccessCallbacks) {
					if (runOnceOnSuccessCallbacks.hasOwnProperty(i)) {
						runOnceOnSuccessCallbacks[i].call(null);
					}
				}

				runOnceOnSuccessCallbacks = [];
			}
		});
	}

	return {

		/**
		 * Add a new callback function which will be executed on success of the current ajax response only.
		 */
		addRunOnceOnSuccess: function addRunOnceOnSuccess(callback) {
			if (typeof callback === 'function') {
				runOnceOnSuccessCallbacks[runOnceOnSuccessCallbacks.length] = callback;
			}
			else {
				throw new Error("OmniFaces.Ajax.addRunOnceOnSuccess: The given callback is not a function.");
			}
		}
	}

}();