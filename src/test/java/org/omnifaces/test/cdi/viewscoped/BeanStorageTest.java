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
package org.omnifaces.test.cdi.viewscoped;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.Concurrency.testThreadSafety;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.BeanStorage;

class BeanStorageTest {

    @Test
    void testgetBeanThreadSafety() {
        var beanStorage = new BeanStorage(10);
        var outerBeanCounter = new AtomicInteger();
        var innerBeanCounter = new AtomicInteger();
        var outerBean = new Contextual<>() {

            Contextual<Object> innerBean = new Contextual<>() {

                @Override
                public Object create(CreationalContext<Object> context) {
                    innerBeanCounter.incrementAndGet();

                    return new Serializable() {

                        private static final long serialVersionUID = 1L;

                    };
                }

                @Override
                public void destroy(Object instance, CreationalContext<Object> context) {
                    // NOOP.
                }

            };

            @Override
            public Object create(CreationalContext<Object> context) {
                outerBeanCounter.incrementAndGet();
                beanStorage.getBean(innerBean, context);

                return new Serializable() {

                    private static final long serialVersionUID = 1L;

                };
            }

            @Override
            public void destroy(Object instance, CreationalContext<Object> context) {
                // NOOP.
            }

        };

        var context = new CreationalContext<>() {

            @Override
            public void push(Object incompleteInstance) {
                // NOOP.
            }

            @Override
            public void release() {
                // NOOP.
            }

        };

        testThreadSafety(i -> beanStorage.getBean(outerBean, context));

        assertAll(
            () -> assertEquals(1, outerBeanCounter.get()),
            () -> assertEquals(1, innerBeanCounter.get())
        );
    }

}
