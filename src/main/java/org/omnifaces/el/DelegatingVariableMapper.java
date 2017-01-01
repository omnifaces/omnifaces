/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.el;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 *
 * @author Arjan Tijms.
 * @since 2.1
 */
public class DelegatingVariableMapper extends VariableMapper {

	private static volatile boolean resolveWrappedVariable;
	private final VariableMapper wrapped;
	private Map<String, ValueExpression> variables = new HashMap<>();

	public DelegatingVariableMapper(VariableMapper wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public ValueExpression resolveVariable(String name) {
		if (variables.containsKey(name)) {
			return resolveWrappedVariable ? null : variables.get(name);
		}

		return wrapped.resolveVariable(name);
	}

	public ValueExpression resolveWrappedVariable(String name) {
		try {
			resolveWrappedVariable = true;
			return wrapped.resolveVariable(name);
		}
		finally {
			resolveWrappedVariable = false;
		}
	}

	@Override
	public ValueExpression setVariable(String name, ValueExpression expression) {
		return variables.put(name, expression);
	}

}