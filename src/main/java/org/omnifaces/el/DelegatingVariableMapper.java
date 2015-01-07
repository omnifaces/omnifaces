package org.omnifaces.el;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

public class DelegatingVariableMapper extends VariableMapper {

    private final VariableMapper variableMapper;

    private Map<String, ValueExpression> variables = new HashMap<>();

    public DelegatingVariableMapper(VariableMapper variableMapper) {
        this.variableMapper = variableMapper;
    }

    public ValueExpression resolveVariable(String variable) {
        ValueExpression valueExpression = variables.get(variable);

        if (valueExpression == null) {
            valueExpression = variableMapper.resolveVariable(variable);
        }

        return valueExpression;
    }

    public ValueExpression setVariable(String variable, ValueExpression expression) {
        return variables.put(variable, expression);
    }
}