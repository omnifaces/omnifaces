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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * MethodInfo compares only the method name, the return type and the parameter types, which is not enough to tell two references to the same method on different
 * beans apart.
 */
class MethodReferenceTest {

    public static class Bean {

        public String getValue(String argument) {
            return argument;
        }

    }

    private static Method getValue() throws Exception {
        return Bean.class.getMethod("getValue", String.class);
    }

    @Test
    void referencesToTheSameMethodOnTheSameBaseAreEqual() throws Exception {
        var base = new Bean();

        assertEquals(new MethodReference(base, getValue()), new MethodReference(base, getValue()), "method reference");
        assertEquals(new MethodReference(base, getValue()).hashCode(), new MethodReference(base, getValue()).hashCode(), "hash code");
    }

    @Test
    void referencesToTheSameMethodOnDifferentBasesAreNotEqual() throws Exception {
        assertNotEquals(new MethodReference(new Bean(), getValue()), new MethodReference(new Bean(), getValue()), "method reference");
    }

    @Test
    void referencesToTheSameMethodWithDifferentActualParametersAreNotEqual() throws Exception {
        var base = new Bean();
        var first = new MethodReference(base, getValue(), new Object[] { "first" }, true);
        var second = new MethodReference(base, getValue(), new Object[] { "second" }, true);

        assertNotEquals(first, second, "method reference");
    }

    @Test
    void referencesToTheSameMethodOnDifferentBasesRemainDistinctInASet() throws Exception {
        var references = List.of(new MethodReference(new Bean(), getValue()), new MethodReference(new Bean(), getValue()));

        assertEquals(2, new HashSet<>(references).size(), "distinct references");
    }

}
