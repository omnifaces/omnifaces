/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.util;

import java.util.Map;
import java.util.Map.Entry;

import org.omnifaces.context.OmniPartialViewContext;

/**
 * Ajax utility class. It are mainly shortcuts to the current {@link OmniPartialViewContext} instance.
 *
 * @author Bauke Scholtz
 * @since 1.2
 */
public final class Ajax {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_ARGUMENTS_LENGTH = "The arguments length must be even. Encountered %d items.";
	private static final String ERROR_ARGUMENT_TYPE = "The argument name must be a String. Encountered type '%s'.";

	// Constructors ---------------------------------------------------------------------------------------------------

	private Ajax() {
		// Hide constructor.
	}

	// Shortcuts ------------------------------------------------------------------------------------------------------

	/**
	 * Update the given client IDs in the current ajax response.
	 * @param clientIds The client IDs to be updated.
	 */
	public static void update(String... clientIds) {
		Faces.addRenderIds(clientIds);
	}

	/**
	 * Execute the given scripts on complete of the current ajax response.
	 * @param scripts The scripts to be executed.
	 */
	public static void oncomplete(String... scripts) {
		OmniPartialViewContext context = OmniPartialViewContext.getCurrentInstance();

		for (String script : scripts) {
			context.addCallbackScript(script);
		}
	}

	/**
	 * Add the given data argument to the current ajax response.
	 * @param name The argument name.
	 * @param value The argument value.
	 */
	public static void data(String name, Object value) {
		OmniPartialViewContext.getCurrentInstance().addArgument(name, value);
	}

	/**
	 * Add the given data arguments to the current ajax response. The arguments length must be even. Every first and
	 * second argument is considered the name and value pair. The name must always be a {@link String}.
	 * @param namesValues The argument names and values.
	 * @throws IllegalArgumentException When the arguments length is not even, or when a name is not a string.
	 */
	public static void data(Object... namesValues) {
		if (namesValues.length % 2 != 0) {
			throw new IllegalArgumentException(String.format(ERROR_ARGUMENTS_LENGTH, namesValues.length));
		}

		OmniPartialViewContext context = OmniPartialViewContext.getCurrentInstance();

		for (int i = 0; i < namesValues.length; i+= 2) {
			if (!(namesValues[i] instanceof String)) {
				String type = namesValues[i] != null ? namesValues[i].getClass().getName() : "null";
				throw new IllegalArgumentException(String.format(ERROR_ARGUMENT_TYPE, type));
			}

			context.addArgument((String) namesValues[i], namesValues[i + 1]);
		}
	}

	/**
	 * Add the given mapping of data arguments to the current ajax response.
	 * @param data The mapping of data arguments.
	 */
	public static void data(Map<String, Object> data) {
		OmniPartialViewContext context = OmniPartialViewContext.getCurrentInstance();

		for (Entry<String, Object> entry : data.entrySet()) {
			context.addArgument(entry.getKey(), entry.getValue());
		}
	}

}