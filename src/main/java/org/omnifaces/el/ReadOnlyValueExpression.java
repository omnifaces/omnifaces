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

import jakarta.el.ELContext;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;

import org.omnifaces.util.FunctionalInterfaces.SerializableSupplier;

/**
 * Implementation of a read only value expression that can be used when the value is not yet available at construction time, or when the
 * value would be too expensive to create and it's not yet clear if the expression will actually be evaluated.
 *
 * <p>
 * A callback (lambda in Java 8) that obtains the value can be provided, or the getValue() method can be overridden.
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public class ReadOnlyValueExpression extends ValueExpression {

    private static final long serialVersionUID = 1L;

    private SerializableSupplier<Object> callback;
    private Class<?> expectedType;

    /**
     * Construct a read only value expression.
     * @param expectedType The type the result of the expression will be coerced to after evaluation.
     * @param callback The functional interface that will be called when the value expression is resolved.
     */
    public ReadOnlyValueExpression(Class<?> expectedType, SerializableSupplier<Object> callback) {
        this(expectedType);
        this.callback = callback;
    }

    /**
     * Construct a read only value expression.
     * @param expectedType The type the result of the expression will be coerced to after evaluation.
     */
    public ReadOnlyValueExpression(Class<?> expectedType) {
        this.expectedType = expectedType;
    }

    /**
     * Construct a read only value expression.
     */
    public ReadOnlyValueExpression() {
        //
    }

    @Override
    public Object getValue(ELContext context) {
        if (callback != null) {
            return callback.get();
        }

        return null;
    }

    @Override
    public void setValue(ELContext context, Object value) {
        throw new PropertyNotWritableException();
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        return true;
    }

    @Override
    public Class<?> getType(ELContext context) {
        var value = getValue(context);
        return value == null ? null : value.getClass();
    }

    @Override
    public Class<?> getExpectedType() {
        return expectedType;
    }

    @Override
    public String getExpressionString() {
        return null;
    }

    @Override
    public boolean equals(Object object) {
        // Basic checks.
        if (object == this) {
            return true;
        }
        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        // Property checks.
        var other = (ReadOnlyValueExpression) object;
        var value = getValue(null);
        var otherValue = other.getValue(null);
        if (value == null ? otherValue != null : !value.equals(otherValue)) {
            return false;
        }

        // All passed.
        return true;
    }

    @Override
    public int hashCode() {
        var value = getValue(null);
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public boolean isLiteralText() {
        return true;
    }

    /**
     * Returns the functional interface that will be called when the value expression is resolved
     *
     * @return the functional interface that will be called when the value expression is resolved
     * @since 2.1
     */
    public SerializableSupplier<Object> getCallback() {
        return callback;
    }

    /**
     * Sets the functional interface that will be called when the value expression is resolved
     *
     * @param callbackReturning functional interface returning what the value expression will return
     * @since 2.1
     */
    public void setCallback(SerializableSupplier<Object> callback) {
        this.callback = callback;
    }

}