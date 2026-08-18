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
package org.omnifaces.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnifaces.util.Beans.isProxy;

import org.junit.jupiter.api.Test;

/**
 * Beans.isProxy falls back to the de-facto standard proxy class name for CDI implementations whose proxy interface is unknown, so the fixtures below are
 * deliberately named after what Weld and OWB generate.
 */
class BeansTest {

    static class Bean {
    }

    static class Bean$$WeldProxy {
    }

    static class BeanProxy$$Enhanced {
    }

    static class BEAN$$WELDPROXY {
    }

    static class BeanClientProxy {
    }

    static class Bean$$Enhanced {
    }

    static class $$Proxy {
    }

    @Test
    void classNameWithMarkerBeforeProxyIsProxy() {
        assertTrue(isProxy(Bean$$WeldProxy.class));
    }

    @Test
    void classNameWithProxyBeforeMarkerIsProxy() {
        assertTrue(isProxy(BeanProxy$$Enhanced.class));
    }

    @Test
    void classNameIsMatchedCaseInsensitively() {
        assertTrue(isProxy(BEAN$$WELDPROXY.class));
    }

    @Test
    void classNameWithoutMarkerIsNotProxy() {
        assertFalse(isProxy(BeanClientProxy.class));
    }

    @Test
    void classNameWithoutProxyIsNotProxy() {
        assertFalse(isProxy(Bean$$Enhanced.class));
    }

    @Test
    void generatedPartMustBePrecededByBeanClassName() {
        assertFalse(isProxy($$Proxy.class));
    }

    @Test
    void plainClassNameIsNotProxy() {
        assertFalse(isProxy(Bean.class));
    }

    @Test
    void instanceIsCheckedByItsOwnClass() {
        assertTrue(isProxy(new Bean$$WeldProxy()));
        assertFalse(isProxy(new Bean()));
    }

    @Test
    void nullIsNotProxy() {
        assertFalse(isProxy(null));
    }

}
