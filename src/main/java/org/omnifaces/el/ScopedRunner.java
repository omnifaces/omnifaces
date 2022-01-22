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
package org.omnifaces.el;

import java.util.HashMap;
import java.util.Map;

import jakarta.faces.context.FacesContext;

/**
 * This class helps in letting code run within its own scope. Such scope is defined by specific variables being
 * available to EL within it. The request scope is used to store the variables.
 *
 * @author Arjan Tijms
 *
 */
public class ScopedRunner {

	private FacesContext context;
	private Map<String, Object> scopedVariables;
	private Map<String, Object> previousVariables = new HashMap<>();

	/**
	 * Construct a scoped runner.
	 * @param context The involved faces context.
	 */
	public ScopedRunner(FacesContext context) {
		this(context, new HashMap<>());
	}

	/**
	 * Construct a scoped runner.
	 * @param context The involved faces context.
	 * @param scopedVariables Initial scoped variables.
	 */
	public ScopedRunner(FacesContext context, Map<String, Object> scopedVariables) {
		this.context = context;
		this.scopedVariables = scopedVariables;
	}

	/**
	 * Adds the given scoped variable to this instance. Can be used in a builder-pattern.
	 *
	 * @param key the key name of the variable
	 * @param value the value of the variable
	 * @return this ScopedRunner, so adding variables and finally calling invoke can be chained.
	 */
	public ScopedRunner with(String key, Object value) {
		scopedVariables.put(key, value);
		return this;
	}

	/**
	 * Invokes the callback within the scope of the variables being given in the constructor.
	 * @param callback The callback.
	 */
	public void invoke(Runnable callback) {
		try {
			setNewScope();
			callback.run();
		} finally {
			restorePreviousScope();
		}
	}

	private void setNewScope() {
		previousVariables.clear();

		Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
		for (Map.Entry<String, Object> entry : scopedVariables.entrySet()) {
			Object previousVariable = requestMap.put(entry.getKey(), entry.getValue());
			if (previousVariable != null) {
				previousVariables.put(entry.getKey(), previousVariable);
			}
		}
	}

	private void restorePreviousScope() {
		try {
			Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
			for (Map.Entry<String, Object> entry : scopedVariables.entrySet()) {
				Object previousVariable = previousVariables.get(entry.getKey());
				if (previousVariable != null) {
					requestMap.put(entry.getKey(), previousVariable);
				} else {
					requestMap.remove(entry.getKey());
				}
			}
		} finally {
			previousVariables.clear();
		}
	}

	/**
	 * Invokes the callback within the scope of the given variable.
	 * @param context The involved faces context.
	 * @param key the key name of the variable
	 * @param value the value of the variable
	 * @param callback The callback.
	 * @since 3.0
	 */
	public static void forEach(FacesContext context, String key, Object value, Runnable callback) {
		new ScopedRunner(context).with(key, value).invoke(callback::run);
	}

}
