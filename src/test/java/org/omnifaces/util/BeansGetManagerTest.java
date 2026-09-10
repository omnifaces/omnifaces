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

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Obtaining the bean manager is not necessarily cheap. Weld resolves it based on the class which invoked {@link CDI}, and therefore walks the entire stack
 * trace of the current thread on every single {@link CDI#getBeanManager()} call, see <code>org.jboss.weld.AbstractCDI#getCallingClassName()</code>. On top of
 * that, {@link CDI#current()} instantiates a brand new CDI object on every call in a servlet environment, see
 * <code>org.jboss.weld.environment.servlet.WeldProvider</code>, and the CDI API itself invokes <code>CDIProvider#getCDI()</code> twice per
 * {@link CDI#current()} call. As OmniFaces obtains the bean manager on nearly every {@link Beans} invocation, and thus many times per request, this is
 * measurable in Weld 6. The tests below therefore count how often the CDI API is actually consulted, which is a deterministic measure, as opposed to elapsed
 * time.
 */
class BeansGetManagerTest {

    private static final String JNDI_NAME_BEAN_MANAGER = "java:comp/BeanManager";

    private ClassLoader originalClassLoader;
    private Map<ClassLoader, BeanManager> beanManagersPerClassLoader;
    private AtomicInteger beanManagerLookups;
    private BeanManager beanManager;

    @BeforeEach
    void setUp() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();

        // Every test runs in its own class loader so that it can never observe the bean manager which another test has left behind.
        Thread.currentThread().setContextClassLoader(newWebAppClassLoader());

        beanManagersPerClassLoader = new HashMap<>();
        beanManagerLookups = new AtomicInteger();
        beanManager = beanManagerOf(Thread.currentThread().getContextClassLoader());
        setCDIProvider(mockCDI(this::beanManagerOf));
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        CDIProviderResetter.reset();
        System.clearProperty(INITIAL_CONTEXT_FACTORY);
        TestInitialContextFactory.context = null;
    }

    @Test
    void managerIsObtainedFromCDI() {
        assertSame(beanManager, Beans.getManager());
    }

    /**
     * This is the actual regression test: the CDI API must be consulted only once, because implementations such as Weld walk the stack trace of the current
     * thread on every single call.
     */
    @Test
    void managerIsObtainedFromCDIOnlyOnce() {
        for (var i = 0; i < 100; i++) {
            assertSame(beanManager, Beans.getManager());
        }

        assertEquals(1, beanManagerLookups.get(), "CDI#getBeanManager() must be consulted only once for the same class loader");
    }

    /**
     * OmniFaces may be deployed in a class loader which is shared by multiple web applications, e.g. when it sits in the <code>/lib</code> of an EAR. Each of
     * them has its own bean manager, so the outcome may never be shared among class loaders.
     */
    @Test
    void managerIsNotSharedAmongClassLoaders() {
        var thisWebAppClassLoader = Thread.currentThread().getContextClassLoader();
        var otherWebAppClassLoader = newWebAppClassLoader();
        var managerOfThisWebApp = Beans.getManager();

        Thread.currentThread().setContextClassLoader(otherWebAppClassLoader);
        var managerOfOtherWebApp = Beans.getManager();
        assertNotSame(managerOfThisWebApp, managerOfOtherWebApp);

        Thread.currentThread().setContextClassLoader(thisWebAppClassLoader);
        assertSame(managerOfThisWebApp, Beans.getManager());

        Thread.currentThread().setContextClassLoader(otherWebAppClassLoader);
        assertSame(managerOfOtherWebApp, Beans.getManager());
    }

    /**
     * An absent bean manager, as can happen when the CDI container has not been booted yet, may never be remembered, otherwise the bean manager would stay
     * absent for the remainder of the application's lifetime.
     */
    @Test
    void absentManagerIsNotRemembered() {
        var available = new AtomicBoolean(false);
        setCDIProvider(mockCDI(classLoader -> available.get() ? beanManagerOf(classLoader) : null));

        assertNull(Beans.getManager());

        available.set(true);
        assertSame(beanManager, Beans.getManager());
    }

    /**
     * This merely guards the existing JNDI fallback for environments where <code>CDI.current()</code> is unavailable, e.g. Tomcat without any CDI
     * implementation deployed as a library.
     */
    @Test
    void managerIsObtainedFromJNDIWhenCDIIsUnavailable() throws NamingException {
        var jndiContext = mock(Context.class);
        when(jndiContext.lookup(JNDI_NAME_BEAN_MANAGER)).thenReturn(beanManager);
        TestInitialContextFactory.context = jndiContext;
        System.setProperty(INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
        setCDIProvider(null);

        assertSame(beanManager, Beans.getManager());
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static ClassLoader newWebAppClassLoader() {
        return new URLClassLoader("webapp", new URL[0], BeansGetManagerTest.class.getClassLoader());
    }

    private BeanManager beanManagerOf(ClassLoader classLoader) {
        return beanManagersPerClassLoader.computeIfAbsent(classLoader, key -> mock(BeanManager.class));
    }

    /**
     * Returns a CDI mock which resolves the bean manager the same way as Weld does; based on the caller, which it obtains from the stack trace of the current
     * thread on every single call.
     */
    @SuppressWarnings("unchecked")
    private CDI<Object> mockCDI(Function<ClassLoader, BeanManager> beanManagerResolver) {
        CDI<Object> mockedCDI = mock(CDI.class);
        when(mockedCDI.getBeanManager()).thenAnswer(invocation -> {
            beanManagerLookups.incrementAndGet();
            getCallingClassName();
            return beanManagerResolver.apply(Thread.currentThread().getContextClassLoader());
        });
        return mockedCDI;
    }

    /**
     * This is in essence what <code>org.jboss.weld.AbstractCDI#getCallingClassName()</code> does on every single {@link CDI#getBeanManager()} call.
     */
    private static String getCallingClassName() {
        for (var element : Thread.currentThread().getStackTrace()) {
            if (Beans.class.getName().equals(element.getClassName())) {
                return element.getClassName();
            }
        }

        return null;
    }

    /**
     * Installs a CDI provider returning the given CDI instance, or one throwing {@link IllegalStateException} as the CDI API itself does when there is none,
     * when the given CDI instance is <code>null</code>.
     */
    private static void setCDIProvider(CDI<Object> cdi) {
        CDI.setCDIProvider(() -> {
            if (cdi == null) {
                throw new IllegalStateException("Unable to access CDI");
            }

            return cdi;
        });
    }

    // Inner classes --------------------------------------------------------------------------------------------------

    /**
     * The CDI API does not offer any way to uninstall a programmatically set CDI provider, but the field holding it is protected static and thus accessible to
     * subclasses, so that the JVM wide state of the CDI API can be left behind clean for other tests.
     */
    private abstract static class CDIProviderResetter extends CDI<Object> {

        static void reset() {
            configuredProvider = null;
        }

    }

    /**
     * Serves the mocked JNDI context to {@link javax.naming.InitialContext}.
     */
    public static class TestInitialContextFactory implements InitialContextFactory {

        private static Context context;

        @Override
        public Context getInitialContext(Hashtable<?, ?> environment) {
            return context;
        }

    }

}
