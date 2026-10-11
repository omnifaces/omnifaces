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
package org.omnifaces.util.cache;

import static java.util.Collections.emptyEnumeration;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.omnifaces.test.WebappClassLoaders.createWebappClassLoader;
import static org.omnifaces.util.cache.CacheInitializer.CACHE_PROVIDER_INIT_PARAM_NAME;
import static org.omnifaces.util.cache.CacheInitializer.loadProviderAndRegisterFilter;

import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The cache provider in the context parameter usually refers to an application class, which is not necessarily visible to the class loader of OmniFaces itself,
 * such as in Quarkus dev mode.
 */
class CacheInitializerTest {

    private static final String CACHE_PROVIDER_CLASS_NAME = "com.example.WebappCacheProvider";
    private static final String CACHE_PROVIDER_SOURCE = "package com.example; public class WebappCacheProvider extends org.omnifaces.util.cache.DefaultCacheProvider {}";

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
    void cacheProviderVisibleToContextClassLoaderOnlyIsLoaded() throws Exception {
        URLClassLoader webappClassLoader = createWebappClassLoader(directory, CACHE_PROVIDER_CLASS_NAME, CACHE_PROVIDER_SOURCE);
        Thread.currentThread().setContextClassLoader(webappClassLoader);
        ServletContext servletContext = createServletContext(CACHE_PROVIDER_CLASS_NAME);

        loadProviderAndRegisterFilter(servletContext);

        assertSame(webappClassLoader.loadClass(CACHE_PROVIDER_CLASS_NAME), CacheFactory.getCacheProvider(servletContext).getClass());
    }

    @Test
    void unknownCacheProviderIsRejected() {
        ServletContext servletContext = createServletContext(CACHE_PROVIDER_CLASS_NAME);

        assertThrows(IllegalStateException.class, () -> loadProviderAndRegisterFilter(servletContext));
    }

    private static ServletContext createServletContext(String cacheProvider) {
        Map<String, Object> attributes = new HashMap<>();

        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(), new Class[] { ServletContext.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getInitParameter" :
                    return CACHE_PROVIDER_INIT_PARAM_NAME.equals(args[0]) ? cacheProvider : null;
                case "getInitParameterNames" :
                    return emptyEnumeration();
                case "getAttribute" :
                    return attributes.get(args[0]);
                case "setAttribute" :
                    return attributes.put((String) args[0], args[1]);
                default :
                    return null;
            }
        });
    }

}
