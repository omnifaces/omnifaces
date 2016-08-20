/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.util;

import java.io.Serializable;

import javax.faces.component.StateHelper;

/**
 * Helper class for StateHelper that uses generic type-inference to make code that uses the StateHelper slightly less verbose.
 *
 * @since 1.1
 * @author Arjan Tijms
 */
public class State {

	private final StateHelper stateHelper;

	public State(StateHelper stateHelper) {
		this.stateHelper = stateHelper;
	}

	/**
	 * Attempts to find a value associated with the specified key in the component's state.
	 * <p>
	 * See {@link StateHelper#eval(Serializable)}
	 *
	 * @param <T> The expected return type.
	 * @param key the name of the value in component's state
	 * @return The value associated with the specified key.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Serializable key) {
		return (T) stateHelper.eval(key);
	}

	/**
	 * Attempts to find a value associated with the specified key in the component's state.
	 * <p>
	 * See {@link StateHelper#eval(Serializable, Object)}
	 *
	 * @param <T> The expected return type.
	 * @param key the name of the value in component's state
	 * @param defaultValue the value to return if no value is found in the call to get()
	 * @return The value associated with the specified key, or the given default value if no value is found.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Serializable key, Object defaultValue) {
		return (T) stateHelper.eval(key, defaultValue);
	}

	/**
	 * Puts a value in the component's state and returns the previous value.
	 * Note that the previous value has to be of the same type as the value
	 * being set. If this is unwanted, use the original StateHelper.
	 * <p>
	 * See {@link StateHelper#put(Serializable, Object)}
	 *
	 * @param <T> The expected value and return type.
	 * @param key The name of the value in component's state.
	 * @param value The value to put in component's state.
	 * @return The previous value, if any.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T put(Serializable key, T value) {
		return (T) stateHelper.put(key, value);
	}

}
