OmniFaces.Ajax = function() {

	var runOnceOnSuccessCallbacks = [];

	var executeRunOnceOnSuccessCallbacks = function executeRunOnceOnSuccessCallbacks(data) {
		if (data.status === 'success') {
			for (var i in runOnceOnSuccessCallbacks) {
				if (runOnceOnSuccessCallbacks.hasOwnProperty(i)) {
					runOnceOnSuccessCallbacks[i].call(null);
				}
			}

			runOnceOnSuccessCallbacks = [];
		}
	};
	
	return {

		/**
		 * Add a new callback function which will be executed on success of the current ajax response only.
		 */
		addRunOnceOnSuccess: function addRunOnceOnSuccess(callback) {
			if (typeof callback === 'function') {
				if (!runOnceOnSuccessCallbacks.length) {
					jsf.ajax.addOnEvent(executeRunOnceOnSuccessCallbacks);
				}

				runOnceOnSuccessCallbacks[runOnceOnSuccessCallbacks.length] = callback;
			}
			else {
				throw new Error("OmniFaces.Ajax.addRunOnceOnSuccess: The given callback is not a function.");
			}
		}
	};

}();