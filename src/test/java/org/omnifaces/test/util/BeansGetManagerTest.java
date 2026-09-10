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

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omnifaces.util.Beans;

/**
 * Obtaining the bean manager is not necessarily cheap. Weld resolves it based on the class which invoked {@link CDI}, and
 * therefore walks the entire stack trace of the current thread on every single {@link CDI#getBeanManager()} call. As
 * OmniFaces obtains the bean manager on nearly every {@link Beans} invocation, and thus many times per request, it may be
 * obtained only once per web application. The tests below therefore count how often the CDI API is actually consulted,
 * which is a deterministic measure, as opposed to elapsed time.
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
		setCDIProvider(new TestCDI(this::beanManagerOf));
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
	 * This is the actual regression test: the CDI API may be consulted only once, because implementations such as Weld walk
	 * the stack trace of the current thread on every single call.
	 */
	@Test
	void managerIsObtainedFromCDIOnlyOnce() {
		for (int i = 0; i < 100; i++) {
			assertSame(beanManager, Beans.getManager());
		}

		assertEquals(1, beanManagerLookups.get(), "CDI#getBeanManager() must be consulted only once for the same class loader");
	}

	/**
	 * OmniFaces may be deployed in a class loader which is shared by multiple web applications, e.g. when it sits in the
	 * <code>/lib</code> of an EAR. Each of them has its own bean manager, so the outcome may never be shared among class loaders.
	 */
	@Test
	void managerIsNotSharedAmongClassLoaders() {
		ClassLoader thisWebAppClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader otherWebAppClassLoader = newWebAppClassLoader();
		BeanManager managerOfThisWebApp = Beans.getManager();

		Thread.currentThread().setContextClassLoader(otherWebAppClassLoader);
		BeanManager managerOfOtherWebApp = Beans.getManager();
		assertNotSame(managerOfThisWebApp, managerOfOtherWebApp);

		Thread.currentThread().setContextClassLoader(thisWebAppClassLoader);
		assertSame(managerOfThisWebApp, Beans.getManager());

		Thread.currentThread().setContextClassLoader(otherWebAppClassLoader);
		assertSame(managerOfOtherWebApp, Beans.getManager());
	}

	/**
	 * The web application which the bean manager belongs to is identified by the context class loader, so when there is none,
	 * then there is nothing to remember it for.
	 */
	@Test
	void managerIsNotRememberedWithoutContextClassLoader() {
		Thread.currentThread().setContextClassLoader(null);

		assertSame(beanManagerOf(null), Beans.getManager());
		assertSame(beanManagerOf(null), Beans.getManager());
		assertEquals(2, beanManagerLookups.get(), "CDI#getBeanManager() must be consulted on every call");
	}

	/**
	 * The bean manager of a web application which is being destroyed may not be retained, otherwise its class loader can never
	 * be garbage collected when OmniFaces sits in a class loader which outlives it, e.g. in the <code>/lib</code> of an EAR.
	 */
	@Test
	void managerIsForgottenWhenWebAppIsDestroyed() {
		assertSame(beanManager, Beans.getManager());

		Beans.forgetManager();

		assertSame(beanManager, Beans.getManager());
		assertEquals(2, beanManagerLookups.get(), "CDI#getBeanManager() must be consulted again after the web application is destroyed");
	}

	/**
	 * An absent bean manager, as can happen when the CDI container has not been booted yet, may never be remembered, otherwise
	 * the bean manager would stay absent for the remainder of the application's lifetime.
	 */
	@Test
	void absentManagerIsNotRemembered() {
		AtomicBoolean available = new AtomicBoolean(false);
		setCDIProvider(new TestCDI(classLoader -> available.get() ? beanManagerOf(classLoader) : null));

		assertNull(Beans.getManager());

		available.set(true);
		assertSame(beanManager, Beans.getManager());
	}

	/**
	 * This merely guards the existing JNDI fallback for environments where <code>CDI.current()</code> is unavailable, e.g.
	 * Tomcat without any CDI implementation deployed as a library.
	 */
	@Test
	void managerIsObtainedFromJNDIWhenCDIIsUnavailable() {
		TestInitialContextFactory.context = newJNDIContext(beanManager);
		System.setProperty(INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
		setCDIProvider(null);

		assertSame(beanManager, Beans.getManager());
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private static ClassLoader newWebAppClassLoader() {
		return new URLClassLoader(new URL[0], BeansGetManagerTest.class.getClassLoader());
	}

	private BeanManager beanManagerOf(ClassLoader classLoader) {
		return beanManagersPerClassLoader.computeIfAbsent(classLoader, key -> newBeanManager());
	}

	/**
	 * Returns a bean manager which does nothing at all, as the tests are only interested in its identity.
	 */
	private static BeanManager newBeanManager() {
		return (BeanManager) newStub(BeanManager.class, (proxy, method, args) -> null);
	}

	/**
	 * Returns a JNDI context which serves the given bean manager on the bean manager name, and nothing on any other name.
	 */
	private static Context newJNDIContext(BeanManager beanManager) {
		return (Context) newStub(Context.class, (proxy, method, args) ->
			"lookup".equals(method.getName()) && JNDI_NAME_BEAN_MANAGER.equals(args[0]) ? beanManager : null);
	}

	/**
	 * Returns a stub of the given interface which delegates to the given handler, except for the {@link Object} methods, so
	 * that it can safely be collected, compared and printed.
	 */
	private static Object newStub(Class<?> type, StubHandler handler) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, (proxy, method, args) -> {
			switch (method.getName()) {
				case "hashCode": return System.identityHashCode(proxy);
				case "equals": return proxy == args[0];
				case "toString": return type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
				default: return handler.handle(proxy, method, args);
			}
		});
	}

	/**
	 * Installs a CDI provider returning the given CDI instance, or one throwing {@link IllegalStateException} as the CDI API
	 * itself does when there is none, when the given CDI instance is <code>null</code>.
	 */
	private static void setCDIProvider(CDI<Object> cdi) {
		CDI.setCDIProvider(() -> {
			if (cdi == null) {
				throw new IllegalStateException("Unable to access CDI");
			}

			return cdi;
		});
	}

	/**
	 * This is in essence what <code>org.jboss.weld.AbstractCDI#getCallingClassName()</code> does on every single
	 * {@link CDI#getBeanManager()} call.
	 */
	private static String getCallingClassName() {
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if (Beans.class.getName().equals(element.getClassName())) {
				return element.getClassName();
			}
		}

		return null;
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	@FunctionalInterface
	private interface StubHandler {
		Object handle(Object proxy, java.lang.reflect.Method method, Object[] args);
	}

	/**
	 * A CDI which resolves the bean manager the same way as Weld does; based on the caller, which it obtains from the stack
	 * trace of the current thread on every single call.
	 */
	private class TestCDI extends CDI<Object> {

		private final Function<ClassLoader, BeanManager> beanManagerResolver;

		private TestCDI(Function<ClassLoader, BeanManager> beanManagerResolver) {
			this.beanManagerResolver = beanManagerResolver;
		}

		@Override
		public BeanManager getBeanManager() {
			beanManagerLookups.incrementAndGet();
			getCallingClassName();
			return beanManagerResolver.apply(Thread.currentThread().getContextClassLoader());
		}

		@Override
		public Instance<Object> select(Annotation... qualifiers) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isUnsatisfied() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAmbiguous() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void destroy(Object instance) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Object> iterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get() {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * The CDI API does not offer any way to uninstall a programmatically set CDI provider, but the field holding it is
	 * protected static and thus accessible to subclasses, so that the JVM wide state of the CDI API can be left behind clean
	 * for other tests.
	 */
	private abstract static class CDIProviderResetter extends CDI<Object> {

		static void reset() {
			configuredProvider = null;
		}

	}

	/**
	 * Serves the stubbed JNDI context to {@link javax.naming.InitialContext}.
	 */
	public static class TestInitialContextFactory implements InitialContextFactory {

		private static Context context;

		@Override
		public Context getInitialContext(Hashtable<?, ?> environment) {
			return context;
		}

	}

}
