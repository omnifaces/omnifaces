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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.omnifaces.test.CDIProviders.resetCDIProvider;
import static org.omnifaces.test.CDIProviders.setCDIProvider;

import java.net.URL;
import java.net.URLClassLoader;

import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Without a Faces context, {@link Servlets#getContext()} obtains the servlet context from CDI. The bean manager is absent when CDI is not available at all, or
 * when its container has not been booted yet, and that must surface as a diagnosable exception rather than as an opaque failure further down the chain.
 */
class ServletsGetContextTest {

    private ClassLoader originalClassLoader;

    @BeforeEach
    void setUp() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();

        // The bean manager is remembered per context class loader, so a fresh one keeps this test blind to what another test has left behind.
        Thread.currentThread().setContextClassLoader(new URLClassLoader("webapp", new URL[0], ServletsGetContextTest.class.getClassLoader()));
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        resetCDIProvider();
    }

    @Test
    @SuppressWarnings("unchecked")
    void absentBeanManagerThrowsIllegalStateException() {
        CDI<Object> cdi = mock(CDI.class);
        when(cdi.getBeanManager()).thenReturn(null);
        setCDIProvider(cdi);

        assertThrows(IllegalStateException.class, Servlets::getContext);
    }

}
