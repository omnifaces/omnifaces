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
package org.omnifaces.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.omnifaces.util.Reflection;

class ReflectionTest {

    /**
     * {@link Reflection#findMethod(Object, String, Object...)} caches the candidate methods per class, method name and parameter count, which is why the
     * overload selection on the actual parameters must keep happening per call. The parameter types are deliberately disjoint, as the selection returns the
     * first candidate whose parameters are assignable and {@link Class#getDeclaredMethods()} does not guarantee any order.
     */
    @Test
    void testFindMethodSelectsOverloadByActualParameterType() {
        var base = new Overloaded();

        assertEquals(String.class, Reflection.findMethod(base, "call", "foo").getParameterTypes()[0]);
        assertEquals(Integer.class, Reflection.findMethod(base, "call", 1).getParameterTypes()[0]);

        // Repeat the first one, as it would fail when the resolved method was cached instead of the candidate methods.
        assertEquals(String.class, Reflection.findMethod(base, "call", "foo").getParameterTypes()[0]);
    }

    /**
     * The parameter count is part of the cache key, so overloads which only differ in it must keep resolving to their own method.
     */
    @Test
    void testFindMethodSelectsOverloadByParameterCount() {
        var base = new Overloaded();

        assertEquals(1, Reflection.findMethod(base, "call", "foo").getParameterTypes().length);
        assertEquals(2, Reflection.findMethod(base, "call", "foo", "bar").getParameterTypes().length);
        assertEquals(1, Reflection.findMethod(base, "call", "foo").getParameterTypes().length);
    }

    /**
     * An unknown method must keep returning null, also when the very same class was already inspected for another method.
     */
    @Test
    void testFindMethodReturnsNullOnUnknownMethod() {
        var base = new Overloaded();

        assertEquals(String.class, Reflection.findMethod(base, "call", "foo").getParameterTypes()[0]);
        assertNull(Reflection.findMethod(base, "doesNotExist", "foo"));
        assertNull(Reflection.findMethod(base, "call", "foo", "bar", "baz"));
    }

    /**
     * Methods declared in a super class must keep being found, as the candidate methods are collected across the whole class hierarchy.
     */
    @Test
    void testFindMethodFindsInheritedMethod() {
        var base = new Overloaded();

        assertEquals("inherited", Reflection.findMethod(base, "inherited").getName());
        assertEquals("inherited", Reflection.findMethod(base, "inherited").getName());
    }

    private static class Inheritable {

        @SuppressWarnings("unused")
        public String inherited() {
            return "inherited";
        }

    }

    private static class Overloaded extends Inheritable {

        @SuppressWarnings("unused")
        public String call(String value) {
            return "String";
        }

        @SuppressWarnings("unused")
        public String call(Integer value) {
            return "Integer";
        }

        @SuppressWarnings("unused")
        public String call(String value, String other) {
            return "String,String";
        }

    }

}
