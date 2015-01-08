/*
 * Copyright 2015 OmniFaces.
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

	private final VariableMapper variableMapper;
	private Map<String, ValueExpression> variables = new HashMap<>();

	public DelegatingVariableMapper(VariableMapper variableMapper) {
		this.variableMapper = variableMapper;
	}

	@Override
	public ValueExpression resolveVariable(String variable) {
		ValueExpression valueExpression = variables.get(variable);

		if (valueExpression == null) {
			valueExpression = variableMapper.resolveVariable(variable);
		}

		return valueExpression;
	}

	@Override
	public ValueExpression setVariable(String variable, ValueExpression expression) {
		return variables.put(variable, expression);
	}

}