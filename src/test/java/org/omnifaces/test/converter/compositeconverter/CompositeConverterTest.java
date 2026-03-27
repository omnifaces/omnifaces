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
package org.omnifaces.test.converter.compositeconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.faces.application.Application;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.converter.CompositeConverter;

class CompositeConverterTest {

    private CompositeConverter compositeConverter;
    private FacesContext context;
    private UIComponent component;
    private Application application;

    @BeforeEach
    void setUp() {
        compositeConverter = new CompositeConverter();
        context = mock(FacesContext.class);
        application = mock(Application.class);
        component = mock(UIComponent.class);
        when(context.getApplication()).thenReturn(application);
    }

    @Test
    void testGetAsObject_SequentialOrder() {
        compositeConverter.setConverterIds("trim, uppercase");
        registerConverter("trim", new TrimConverter());
        registerConverter("uppercase", new UppercaseConverter());

        Object result = compositeConverter.getAsObject(context, component, "  hello  ");
        assertEquals("HELLO", result);
    }

    @Test
    void testGetAsString_ReverseOrder() {
        compositeConverter.setConverterIds("prefix, suffix");
        registerConverter("prefix", new WrapperConverter("[PRE]"));
        registerConverter("suffix", new WrapperConverter("[SUF]"));

        Object modelValue = "DATA";
        String result = compositeConverter.getAsString(context, component, modelValue);
        assertEquals("[PRE][SUF]DATA", result);
    }

    @Test
    void testGetAsObject_WithNull() {
        compositeConverter.setConverterIds("trim, uppercase");
        registerConverter("trim", new TrimConverter());
        registerConverter("uppercase", new UppercaseConverter());

        Object result = compositeConverter.getAsObject(context, component, null);
        assertEquals(null, result);
    }

    @Test
    void testGetAsStringWithNull() {
        compositeConverter.setConverterIds("trim, uppercase");
        registerConverter("trim", new TrimConverter());
        registerConverter("uppercase", new UppercaseConverter());

        Object result = compositeConverter.getAsString(context, component, null);
        assertEquals("", result);
    }

    @Test
    void testMissingAttribute_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> compositeConverter.getAsObject(context, component, "test"));
    }

    @Test
    void testUnknownConverter_ThrowsException() {
        compositeConverter.setConverterIds("exists, nonExistent");
        registerConverter("exists", new TrimConverter());
        when(application.createConverter("nonExistent")).thenReturn(null);
        assertThrows(Exception.class, () -> compositeConverter.getAsObject(context, component, "test"));
    }

    // --- Helpers & Mock Converters ---

    private void registerConverter(String id, Converter<?> converter) {
        when(application.createConverter(id)).thenReturn(converter);
    }

    private static class TrimConverter implements Converter<Object> {

        @Override
        public Object getAsObject(FacesContext c, UIComponent comp, String val) {
            return val == null ? null : val.trim();
        }

        @Override
        public String getAsString(FacesContext c, UIComponent comp, Object val) {
            return val == null ? "" : val.toString();
        }

    }

    private static class UppercaseConverter implements Converter<Object> {

        @Override
        public Object getAsObject(FacesContext c, UIComponent comp, String val) {
            return val == null ? null : val.toUpperCase();
        }

        @Override
        public String getAsString(FacesContext c, UIComponent comp, Object val) {
            return val == null ? "" : val.toString();
        }

    }

    private static class WrapperConverter implements Converter<Object> {

        private final String tag;

        public WrapperConverter(String tag) {
            this.tag = tag;
        }

        @Override
        public Object getAsObject(FacesContext c, UIComponent comp, String val) {
            return tag + val;
        }

        @Override
        public String getAsString(FacesContext c, UIComponent comp, Object val) {
            return tag + val;
        }

    }

}
