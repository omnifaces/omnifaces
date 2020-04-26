/*
 * Copyright 2020 OmniFaces
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

import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 *
 * @author Arjan Tijms.
 * @since 2.1
 */
public class DelegatingVariableMapper extends VariableMapper {

	private final VariableMapper wrapped;
	private final Map<String, ValueExpression> variables = new HashMap<>();

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

	public ValueExpression setWrappedVariable(String name, ValueExpression expression) {
		return wrapped.setVariable(name, expression);
	}

}