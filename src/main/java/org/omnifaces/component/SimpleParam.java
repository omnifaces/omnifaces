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
package org.omnifaces.component;

import static org.omnifaces.util.Components.convertToString;
import static org.omnifaces.util.Faces.getContext;

import jakarta.faces.component.UIParameter;
import jakarta.faces.convert.Converter;

/**
 * This class provides a basic and default implementation of the {@link ParamHolder} interface. Ultimately, this class
 * can be used as a simple key-value pair holder (parameter name-value) which uses an explicit/implicit Faces converter
 * to convert the object value to string.
 *
 * @author Bauke Scholtz
 * @param <T> The type of the value.
 * @since 1.7
 */
public class SimpleParam<T> implements ParamHolder<T> {

    // Properties -----------------------------------------------------------------------------------------------------

    private String name;
    private T value;
    private Converter<T> converter;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public SimpleParam() {
        // NOOP.
    }

    /**
     * Construct a simple param with name and value.
     * @param name The parameter name.
     * @param value The parameter value.
     */
    public SimpleParam(String name, T value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Construct a simple param with name, value and converter.
     * @param name The parameter name.
     * @param value The parameter value.
     * @param converter The converter.
     */
    public SimpleParam(String name, T value, Converter<T> converter) {
        this(name, value);
        this.converter = converter;
    }

    /**
     * Construct a simple param with name and value of given {@link UIParameter} component.
     * @param param The {@link UIParameter} to copy.
     * @throws ClassCastException When actual parameter value is not <code>T</code>.
     * @since 2.2
     */
    @SuppressWarnings("unchecked")
    public SimpleParam(UIParameter param) {
        name = param.getName();

        if (param instanceof ParamHolder) {
            ParamHolder<T> holder = (ParamHolder<T>) param;
            value = holder.getLocalValue();
            converter = holder.getConverter();
        }
        else {
            value = (T) param.getValue();
        }
    }

    /**
     * Construct a simple param with name, value and converter of given {@link ParamHolder} instance.
     * @param param The {@link ParamHolder} to copy.
     * @since 2.2
     */
    public SimpleParam(ParamHolder<T> param) {
        this(param.getName(), param.getLocalValue(), param.getConverter());
    }

    // Getters/setters ------------------------------------------------------------------------------------------------

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the parameter name.
     * @param name The parameter name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public T getLocalValue() {
        return value;
    }

    @Override
    public String getValue() {
        return convertToString(getContext(), this, value);
    }

    /**
     * @throws ClassCastException When actual value is not <code>T</code>.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        this.value = (T) value;
    }

    @Override
    public Converter<T> getConverter() {
        return converter;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setConverter(Converter converter) {
        this.converter = converter;
    }

}