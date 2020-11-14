/*
 * Copyright 2020 OmniFaces
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

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.omnifaces.util.Beans.isRemoteEJB;
import static org.omnifaces.util.Exceptions.is;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_APPLICATION;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_GLOBAL;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_MODULE;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_PREFIX;
import static org.omnifaces.util.JNDI.JNDI_NAME_PREFIX_ENV_ENTRY;
import static org.omnifaces.util.JNDI.guessJNDIName;
import static org.omnifaces.util.Utils.coalesce;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * JNDIObjectLocator is used to centralize JNDI lookups. It minimizes the overhead of JNDI lookups by caching the objects it looks up.
 * <p>
 * Implements the ServiceLocator design pattern.
 * <p>
 * Major features are:
 * <ul>
 * <li>thread-safe
 * <li>immutable
 * <li>serializable
 * <li>selectively disables the cache if objects are remote
 * </ul>
 * <p>
 * Example:
 * <pre>
 * {@code
 * locator = JNDIObjectLocator.builder().build();
 * MyEJB myEJB1 = locator.getObject(MyEJB.class);
 * MyEJB myEJB2 = locator.getObject("java:module/MyEJB");
 * }
 * </pre>
 * <p>
 * <a href="https://github.com/flowlogix/flowlogix/blob/master/jakarta-ee/jee-examples/src/main/java/com/flowlogix/examples/JndiExample.java" target="_blank">Example Code (GitHub)</a>
 *
 * @author Lenny Primak
 * @since 3.9
 */
public class JNDIObjectLocator implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns the builder of the {@link JNDIObjectLocator}.
	 * @return The builder of the {@link JNDIObjectLocator}.
	 */
	public static JNDIObjectLocatorBuilder builder() {
		return new JNDIObjectLocatorBuilder();
	}

	/**
	 * The builder of the {@link JNDIObjectLocator}.
	 */
	public static class JNDIObjectLocatorBuilder implements Serializable {
                private static final long serialVersionUID = 1L;

		private Map<String, String> environment;
		private String namespace;
		private Boolean noCaching;
		private Boolean cacheRemote;
		private transient JNDIObjectLocator build;

		/**
		 * Specifies the environment to be passed into {@link InitialContext}. The default is <code>null</code>.
		 * @param environment The environment.
		 * @return This builder.
		 * @throws NullPointerException When given environment is null.
		 * @throws IllegalStateException When environment is already set in this builder or when this builder is already build.
		 */
		public JNDIObjectLocatorBuilder environment(Map<String, String> environment) {
			requireNonNull(environment, "environment");

			if (build != null || this.environment != null) {
				throw new IllegalStateException();
			}

			this.environment = environment;
			return this;
		}

		/**
		 * Adds an environment property.
		 * @param key The key of the new environment property.
		 * @param value The value of the new environment property.
		 * @return This builder.
		 * @throws NullPointerException When key or value is null.
		 * @throws IllegalStateException When environment property is already set in this builder or when this builder is already build.
		 */
		public JNDIObjectLocatorBuilder environment(String key, String value) {
			requireNonNull(key, "key");
			requireNonNull(value, "value");

			if (environment == null) {
				environment = new Hashtable<>();
			}

			if (build != null || environment.put(key, value) != null) {
				throw new IllegalStateException();
			}

			return this;
		}

		/**
		 * Adds initial host environment property.
		 * @param initialHost The initial host environment property.
		 * @return This builder.
		 * @throws IllegalStateException When initial host is already set in this builder or when this builder is already build.
		 * @throws NullPointerException When value is null.
		 */
		public JNDIObjectLocatorBuilder initialHost(String initialHost) {
			return environment("org.omg.CORBA.ORBInitialHost", initialHost);
		}

		/**
		 * Adds initial port environment property.
		 * @param initialPort The initial port environment property.
		 * @return This builder.
		 * @throws IllegalStateException When initial port is already set in this builder or when this builder is already build.
		 */
		public JNDIObjectLocatorBuilder initialPort(int initialPort) {
			return environment("org.omg.CORBA.ORBInitialPort", Integer.toString(initialPort));
		}

		/**
		 * Specifies the default namespace to be used in construction of portable JNDI names. The default is <code>java:module</code>.
		 * @param namespace The namespace.
		 * @return This builder.
		 * @throws IllegalStateException When namespace is already set in this builder or when this builder is already build.
		 * @throws NullPointerException When given namespace is null.
		 */
		public JNDIObjectLocatorBuilder namespace(String namespace) {
			requireNonNull(namespace, "namespace");

			if (build != null || this.namespace != null) {
				throw new IllegalStateException();
			}

			this.namespace = namespace;
			return this;
		}

		/**
		 * Specifies that the default namespace to be used in construction of portable JNDI names must be <code>java:global</code> instead of <code>java:module</code>.
		 * @return This builder.
		 * @throws IllegalStateException When namespace is already set in this builder.
		 */
		public JNDIObjectLocatorBuilder global() {
			return namespace(JNDI_NAMESPACE_GLOBAL);
		}

		/**
		 * Specifies that the default namespace to be used in construction of portable JNDI names must be <code>java:app</code> instead of <code>java:module</code>.
		 * @return This builder.
		 * @throws IllegalStateException When namespace is already set in this builder.
		 */
		public JNDIObjectLocatorBuilder app() {
			return namespace(JNDI_NAMESPACE_APPLICATION);
		}

		/**
		 * Specifies to disable cache. The default is <code>false</code>.
		 * @return This builder.
		 * @throws IllegalStateException When noCaching is already set in this builder or when this builder is already build.
		 */
		public JNDIObjectLocatorBuilder noCaching() {
			if (build != null || noCaching != null) {
				throw new IllegalStateException();
			}

			noCaching = true;
			return this;
		}

		/**
		 * Specifies to cache remote enterprise beans. The default is <code>false</code>.
		 * @return This builder.
		 * @throws IllegalStateException When cacheRemote is already set in this builder or when this builder is already build.
		 */
		public JNDIObjectLocatorBuilder cacheRemote() {
			if (build != null || cacheRemote != null) {
				throw new IllegalStateException();
			}

			cacheRemote = true;
			return this;
		}

		/**
		 * Builds the {@link JNDIObjectLocator}.
		 * @return The {@link JNDIObjectLocator}.
		 * @throws IllegalStateException When this builder is already build.
		 */
		public JNDIObjectLocator build() {
			if (build != null) {
				throw new IllegalStateException();
			}

			environment = coalesce(environment, emptyMap());
			namespace = coalesce(namespace, JNDI_NAMESPACE_MODULE);
			noCaching = coalesce(noCaching, Boolean.FALSE);
			cacheRemote = coalesce(cacheRemote, Boolean.FALSE);
			build = new JNDIObjectLocator(this);

			return build;
		}
	}

	private final JNDIObjectLocatorBuilder builder;
	private final transient Lazy<InitialContext> initialContext;
	private final transient Lock initialContextLock;
	private final transient Map<Class<?>, Entry<String, Boolean>> jndiNameAndRemoteFlagCache;
	private final transient Lazy<Map<String, Object>> jndiObjectCache;

        @SuppressWarnings("unchecked")
	private JNDIObjectLocator(JNDIObjectLocatorBuilder builder) {
		this.builder = builder;
		initialContext = new Lazy<>(this::createInitialContext);
		initialContextLock = new ReentrantLock();
		jndiNameAndRemoteFlagCache = new ConcurrentHashMap<>();
                jndiObjectCache = new Lazy<>(() -> builder.noCaching ? Collections.EMPTY_MAP : new ConcurrentHashMap<>());
	}

	/**
	 * Same as {@link JNDI#getEnvEntry(String)}, except that this is cached.
	 * @param <T> The expected return type.
	 * @param name the environment entry name relative to "java:comp/env".
	 * @return The environment entry value associated with the given name, or <code>null</code> if there is none.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 * @see JNDI#getEnvEntry(String)
	 */
	public <T> T getEnvEntry(String name) {
		return getObject(JNDI_NAME_PREFIX_ENV_ENTRY + "/" + name);
	}

	/**
	 * Returns an object from JNDI based on beanClass.
	 * Uses portable object names and convention to derive appropriate JNDI name.
	 * @param <T> Object type.
	 * @param beanClass Type of object to look up in JNDI.
	 * @return Resulting object, or <code>null</code> if there is none.
	 */
	public <T> T getObject(Class<T> beanClass) {
		Entry<String, Boolean> jndiNameAndRemoteFlag = jndiNameAndRemoteFlagCache.computeIfAbsent(beanClass, this::computeJNDINameAndRemoteFlag);
		return getJNDIObject(prependNamespaceIfNecessary(jndiNameAndRemoteFlag.getKey()),
                        jndiNameAndRemoteFlag.getValue() && !builder.cacheRemote);
	}

	/**
	 * Returns an object based on JNDI name.
	 * @param <T> The expected return type.
	 * @param jndiName The JNDI name of the object to be retrieved.
	 * @return The named object, or <code>null</code> if there is none.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	public <T> T getObject(String jndiName) {
		return getJNDIObject(jndiName, false);
	}

	/**
	 * Return an object based on JNDI name, bypassing the cache.
	 * @param <T> The expected return type.
	 * @param jndiName The JNDI name of the object to be retrieved.
	 * @return The named object, or <code>null</code> if there is none.
	 * @throws ClassCastException When <code>T</code> is of wrong type.
	 */
	public <T> T getObjectNoCache(String jndiName) {
		return getJNDIObject(jndiName, true);
	}

	/**
	 * Clears object cache.
	 */
	public void clearCache() {
		if (jndiObjectCache != null) {
                        jndiObjectCache.get().clear();
		}
	}

        Map<String, Object> getJndiObjectCache() {
            return jndiObjectCache.get();
        }

	private InitialContext createInitialContext() {
		try {
			if (builder.environment.isEmpty()) {
				return new InitialContext();
			}
			else {
				return new InitialContext(new Hashtable<>(builder.environment));
			}
		}
		catch (NamingException e) {
			throw new IllegalStateException(e);
		}
	}

	private Entry<String, Boolean> computeJNDINameAndRemoteFlag(Class<?> beanClass) {
		return new SimpleEntry<>(guessJNDIName(beanClass), isRemoteEJB(beanClass));
	}

	public String prependNamespaceIfNecessary(String jndiName) {
		return jndiName.startsWith(JNDI_NAMESPACE_PREFIX) ? jndiName : (builder.namespace + "/" + jndiName);
	}

	@SuppressWarnings("unchecked")
	private <T> T getJNDIObject(String jndiName, boolean noCaching) {
		if (noCaching || builder.noCaching) {
			return this.lookup(jndiName);
		}
		else {
			return (T) jndiObjectCache.get().computeIfAbsent(jndiName, this::lookup);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T lookup(String name) {
		initialContextLock.lock();
		boolean shouldClearCache = false;

		try {
			return (T) initialContext.get().lookup(name);
		}
		catch (NamingException e) {
			if (is(e, NameNotFoundException.class)) {
				return null;
			}
			else {
				shouldClearCache = true;
				throw new IllegalStateException(e);
			}
		}
		finally {
			initialContextLock.unlock();

			if (shouldClearCache) {
				clearCache();
			}
		}
	}

	/**
	 * This deals with transient final fields correctly.
	 */
	private Object readResolve() {
		return builder.build();
	}

}