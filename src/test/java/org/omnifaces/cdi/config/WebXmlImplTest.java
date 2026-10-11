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
package org.omnifaces.cdi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.omnifaces.test.WebappClassLoaders.createWebappClassLoader;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omnifaces.util.Servlets;

/**
 * An <code>exception-type</code> in <code>web.xml</code> usually refers to an application class, which is not necessarily visible to the class loader of
 * OmniFaces itself, such as in Quarkus dev mode.
 */
class WebXmlImplTest {

    private static final String EXCEPTION_CLASS_NAME = "com.example.WebappException";
    private static final String EXCEPTION_SOURCE = "package com.example; public class WebappException extends RuntimeException {}";

    @TempDir
    Path directory;

    private ClassLoader originalClassLoader;

    @BeforeEach
    void setUp() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    @Test
    void exceptionTypeVisibleToContextClassLoaderOnlyIsMapped() throws Exception {
        var webappClassLoader = createWebappClassLoader(directory, EXCEPTION_CLASS_NAME, EXCEPTION_SOURCE);
        Thread.currentThread().setContextClassLoader(webappClassLoader);

        var webXml = createWebXml(EXCEPTION_CLASS_NAME);
        var exception = (Throwable) webappClassLoader.loadClass(EXCEPTION_CLASS_NAME).getConstructor().newInstance();

        assertEquals("/webapp-error.xhtml", webXml.findErrorPageLocation(exception));
        assertEquals("/error.xhtml", webXml.findErrorPageLocation(new RuntimeException()));
    }

    @Test
    void unknownExceptionTypeFailsInitialization() {
        assertThrows(IllegalStateException.class, () -> createWebXml(EXCEPTION_CLASS_NAME));
    }

    private WebXmlImpl createWebXml(String exceptionType) throws Exception {
        var webXmlFile = directory.resolve("web.xml");
        Files.writeString(webXmlFile, String.format(""
            + "<web-app>"
            + "  <error-page>"
            + "    <exception-type>%s</exception-type>"
            + "    <location>/webapp-error.xhtml</location>"
            + "  </error-page>"
            + "  <error-page>"
            + "    <error-code>500</error-code>"
            + "    <location>/error.xhtml</location>"
            + "  </error-page>"
            + "</web-app>", exceptionType));

        var servletContext = mock(ServletContext.class);
        when(servletContext.getResource("/WEB-INF/web.xml")).thenReturn(webXmlFile.toUri().toURL());

        var webXml = new WebXmlImpl();

        try (var servlets = mockStatic(Servlets.class, CALLS_REAL_METHODS)) {
            servlets.when(Servlets::getContext).thenReturn(servletContext);
            webXml.init();
        }

        return webXml;
    }

}
