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
import java.util.Objects;

import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

/**
 * <p>
 * A variable mapper which holds the variables of one tag file invocation on top of a wrapped one.
 * <p>
 * A variable can be declared, which marks it as a tag attribute of that tag file invocation, or set, which is the ordinary variable mapper contract as used by
 * <code>&lt;c:set&gt;</code> and <code>&lt;ui:param&gt;</code>. Only a declared variable is shielded from a nested tag file invocation, and only a declared
 * variable also remembers the value which it inherited at the moment of declaring, its boundary value.
 * <p>
 * A nested instance therefore treats an inherited value as not inherited when it equals either the declared value or the boundary value of an enclosing
 * instance. Both are looked up through the wrapped mapper under a prefixed name, so that they are also found when the Faces implementation puts its own
 * variable mapper in between, which Mojarra does as soon as the nested tag file is passed an attribute of its own. The boundary value is what makes this work
 * on MyFaces as well, whose variable mapper is dynamic and answers for the tag file which is currently being applied.
 *
 * @author Arjan Tijms.
 * @since 2.1
 */
public class DelegatingVariableMapper extends VariableMapper {

    private static final char BOUNDARY_PREFIX = '@';
    private static final char DECLARED_PREFIX = '#';

    private final VariableMapper wrapped;
    private final Map<String, ValueExpression> variables = new HashMap<>();
    private final Map<String, ValueExpression> boundaries = new HashMap<>();

    /**
     * Construct delegating variable mapper.
     *
     * @param wrapped The variable mapper to be wrapped.
     */
    public DelegatingVariableMapper(VariableMapper wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ValueExpression resolveVariable(String name) {
        switch (name.charAt(0)) {
            case BOUNDARY_PREFIX :
                return isDeclared(name) ? boundaries.get(name.substring(1)) : wrapped.resolveVariable(name);
            case DECLARED_PREFIX :
                return isDeclared(name) ? variables.get(name.substring(1)) : wrapped.resolveVariable(name);
            default :
                return variables.containsKey(name) ? variables.get(name) : wrapped.resolveVariable(name);
        }
    }

    private boolean isDeclared(String prefixedName) {
        return boundaries.containsKey(prefixedName.substring(1));
    }

    /**
     * Returns the value which this mapper inherits for the given name, or <code>null</code> when that value is merely the declaration of an enclosing mapper
     * and therefore not inherited at all.
     *
     * @param name Name of the variable.
     * @return The inherited value, or <code>null</code> when there is none.
     */
    public ValueExpression resolveWrappedVariable(String name) {
        ValueExpression inheritedVariable = resolveInheritedVariable(name);

        if (
            Objects.equals(inheritedVariable, wrapped.resolveVariable(DECLARED_PREFIX + name))
                || Objects.equals(inheritedVariable, wrapped.resolveVariable(BOUNDARY_PREFIX + name))
        ) {
            return null;
        }

        return inheritedVariable;
    }

    private ValueExpression resolveInheritedVariable(String name) {
        return wrapped instanceof DelegatingVariableMapper
            ? ((DelegatingVariableMapper) wrapped).resolveVariableSkippingDeclarations(name)
            : wrapped.resolveVariable(name);
    }

    private ValueExpression resolveVariableSkippingDeclarations(String name) {
        return !boundaries.containsKey(name) && variables.containsKey(name)
            ? variables.get(name)
            : resolveInheritedVariable(name);
    }

    /**
     * Declares the given name as a tag attribute of the tag file invocation which this mapper represents, remembering the value which it inherits at this
     * moment, so that a nested tag file invocation can recognize the declaration as not being its own.
     *
     * @param name Name of the variable.
     * @param expression Value expression of the variable.
     * @return The previous value expression of the variable.
     */
    public ValueExpression declareVariable(String name, ValueExpression expression) {
        boundaries.put(name, resolveInheritedVariable(name));
        return variables.put(name, expression);
    }

    @Override
    public ValueExpression setVariable(String name, ValueExpression expression) {
        return variables.put(name, expression);
    }

}
