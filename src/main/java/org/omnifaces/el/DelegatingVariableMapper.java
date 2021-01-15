/*
 * Copyright 2021 OmniFaces
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
import java.util.Objects;

import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

/**
 *
 * @author Arjan Tijms.
 * @since 2.1
 */
public class DelegatingVariableMapper extends VariableMapper {

	private final VariableMapper wrapped;
	private final Map<String, ValueExpression> variables = new HashMap<>();

	/**
	 * Construct delegating variable mapper.
	 * @param wrapped The variable mapper to be wrapped.
	 */
	public DelegatingVariableMapper(VariableMapper wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public ValueExpression resolveVariable(String name) {
		if (name.charAt(0) == '@') {
			return wrapped.resolveVariable(name.substring(1)); // So we can detect a nested DelegatingVariableMapper in resolveWrappedVariable().
		}
		else if (!variables.containsKey(name)) {
			return wrapped.resolveVariable(name);
		}
		else {
			return variables.get(name);
		}
	}

	/**
	 * Resolve wrapped variable of given name.
	 * @param name Name of wrapped variable.
	 * @return Resolved wrapped variable.
	 */
	public ValueExpression resolveWrappedVariable(String name) {
		ValueExpression wrappedVariable = wrapped.resolveVariable(name);
		ValueExpression globalVariable = variables.get(name);

		if (Objects.equals(wrappedVariable, globalVariable)) {
			return null; // Will happen when variable isn't defined, so any global variable needs to be cleared out.
		}

		ValueExpression parentVariable = wrapped.resolveVariable("@" + name);

		if (Objects.equals(wrappedVariable, parentVariable)) {
			return null; // Will happen when DelegatingVariableMapper is nested but variable isn't redefined, so it needs to be cleared out.
		}

		return wrappedVariable;
	}

	@Override
	public ValueExpression setVariable(String name, ValueExpression expression) {
		return variables.put(name, expression);
	}

	/**
	 * Sets wrapped variable of given name with given value expression.
	 * @param name Name of wrapped variable.
	 * @param expression Value expression of wrapped variable.
	 * @return The wrapped variable.
	 */
	public ValueExpression setWrappedVariable(String name, ValueExpression expression) {
		return wrapped.setVariable(name, expression);
	}

}