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

import java.beans.ConstructorProperties;
import static java.util.Collections.emptyMap;
import static org.omnifaces.util.Exceptions.is;
import static org.omnifaces.util.JNDI.JNDI_NAME_PREFIX_ENV_ENTRY;
import static org.omnifaces.util.JNDI.guessJNDIName;
import static org.omnifaces.util.Reflection.toClassOrNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.unmodifiableMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_APPLICATION;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_GLOBAL;
import static org.omnifaces.util.JNDI.JNDI_NAMESPACE_PREFIX;

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
         * to be passed into InitialContext()
         */
        private final Map<String, String> environment;
        /**
         * Used in construction of portable JNDI names usually java:module
         * (default)
         */
        private final String namespace;
        /**
         * whether to disable cache. Default is false
         */
        private final boolean noCaching;
        /**
         * whether to cache remote EJBs, usually false
         */
        private final boolean cacheRemote;

	private final transient Lazy<InitialContext> initialContext;
	private final transient Lock initialContextLock;
	private final transient Lazy<Map<String, Object>> jndiObjectCache;
	private final transient Lazy<Class<? extends Annotation>> remoteAnnotation;

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
		String jndiName = namespace + "/" + guessJNDIName(beanClass);
		boolean remote = remoteAnnotation.get() != null && beanClass.isAnnotationPresent(remoteAnnotation.get());
		return getJNDIObject(jndiName, remote && !cacheRemote);
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
		jndiObjectCache.get().clear();
	}

        /**
         * Utility method used in matching fiends to EJB injection points,
         * it uses this locator's namespace to try to find appropriate JNDI object to use
         * for injection
         *
         * @param fieldName
         * @return JNDI name for the field
         */
        public String prependNamespaceIfNecessary(String fieldName) {
 		return fieldName.startsWith(JNDI_NAMESPACE_PREFIX) ? fieldName : (namespace + "/" + fieldName);
 	}

	Map<String, Object> getJNDIObjectCache() {
		return jndiObjectCache.get();
	}

	private InitialContext createInitialContext() {
		try {
			if (environment.isEmpty()) {
				return new InitialContext();
			}
			else {
				return new InitialContext(new Hashtable<>(environment));
			}
		}
		catch (NamingException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getJNDIObject(String jndiName, boolean noCaching) {
		if (noCaching || noCaching) {
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
		return toBuilder().build();
	}

        /*
                The following are derived from de-lomboked builder pattern objects for this class
        */
        @ConstructorProperties({"environment", "portableNamePrefix", "noCaching", "cacheRemote"})
        JNDIObjectLocator(final Map<String, String> environment, final String portableNamePrefix, final boolean noCaching, final boolean cacheRemote) {
                this.environment = environment;
                this.namespace = portableNamePrefix;
                this.noCaching = noCaching;
                this.cacheRemote = cacheRemote;
		initialContext = new Lazy<>(this::createInitialContext);
		initialContextLock = new ReentrantLock();
		jndiObjectCache = new Lazy<>(() -> this.noCaching ? emptyMap() : new ConcurrentHashMap<>());
		remoteAnnotation = new Lazy<>(() -> toClassOrNull("javax.ejb.Remote"));
        }

        public static class JNDIObjectLocatorBuilder {
                private ArrayList<String> environmentKey;
                private ArrayList<String> environmentValue;
                private boolean namespaceSet;
                private String namespaceValue;
                private boolean noCaching;
                private boolean cacheRemote;

                JNDIObjectLocatorBuilder() {
                }

                /**
		 * Adds an environment property.
		 * @param environmentKey The key of the new environment property.
		 * @param environmentValue The value of the new environment property.
		 * @return This builder.
		 * @throws NullPointerException When key or value is null.
		 * @throws IllegalStateException When environment property is already set in this builder or when this builder is already build.
		 */
                public JNDIObjectLocatorBuilder environment(final String environmentKey, final String environmentValue) {
                        requireNonNull(environmentKey, "key");
                        requireNonNull(environmentValue, "value");
                        if (this.environmentKey == null) {
                                this.environmentKey = new ArrayList<>();
                                this.environmentValue = new ArrayList<>();
                        }
                        this.environmentKey.add(environmentKey);
                        this.environmentValue.add(environmentValue);
                        return this;
                }

        	/**
		 * Specifies the environment to be passed into {@link InitialContext}. The default is <code>null</code>.
		 * @param environment The environment.
		 * @return This builder.
		 * @throws NullPointerException When given environment is null.
		 * @throws IllegalStateException When environment is already set in this builder or when this builder is already build.
		 */
                public JNDIObjectLocatorBuilder environment(final Map<? extends String, ? extends String> environment) {
                        if (environment == null) {
                                throw new NullPointerException("environment cannot be null");
                        }
                        if (this.environmentKey == null) {
                                this.environmentKey = new ArrayList<>();
                                this.environmentValue = new ArrayList<>();
                        }
                        for (final Entry<? extends String, ? extends String> entry : environment.entrySet()) {
                                this.environmentKey.add(entry.getKey());
                                this.environmentValue.add(entry.getValue());
                        }
                        return this;
                }

                /**
                 * Clears all environment
                 * @return this
                 */
                public JNDIObjectLocatorBuilder clearEnvironment() {
                        if (this.environmentKey != null) {
                                this.environmentKey.clear();
                                this.environmentValue.clear();
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
                public JNDIObjectLocatorBuilder namespace(final String namespace) {
                        requireNonNull(namespace, "namespace");
                        this.namespaceValue = namespace;
                        namespaceSet = true;
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
                        this.noCaching = true;
                        return this;
                }

		/**
		 * Specifies to cache remote enterprise beans. The default is <code>false</code>.
		 * @return This builder.
		 * @throws IllegalStateException When cacheRemote is already set in this builder or when this builder is already build.
		 */
                public JNDIObjectLocatorBuilder cacheRemote() {
                        this.cacheRemote = true;
                        return this;
                }

                /**
		 * Builds the {@link JNDIObjectLocator}.
		 * @return The {@link JNDIObjectLocator}.
		 * @throws IllegalStateException When this builder is already build.
		 */
                public JNDIObjectLocator build() {
                        Map<String, String> environment;
                        switch (this.environmentKey == null ? 0 : this.environmentKey.size()) {
                                case 0:
                                        environment = Collections.emptyMap();
                                        break;
                                case 1:
                                        environment = Collections.singletonMap(this.environmentKey.get(0), this.environmentValue.get(0));
                                        break;
                                default:
                                        environment = new LinkedHashMap<>(this.environmentKey.size() < 1073741824 ? 1 + this.environmentKey.size() +
                                                (this.environmentKey.size() - 3) / 3 : Integer.MAX_VALUE);
                                        for (int ii = 0; ii < this.environmentKey.size(); ii++) {
                                                environment.put(this.environmentKey.get(ii), this.environmentValue.get(ii));
                                        }
                                        environment = unmodifiableMap(environment);
                        }
                        String namespaceValue = this.namespaceValue;
                        if (!this.namespaceSet) {
                                namespaceValue = JNDI.JNDI_NAMESPACE_MODULE;
                        }
                        return new JNDIObjectLocator(environment, namespaceValue, this.noCaching, this.cacheRemote);
                }

                @Override
                public String toString() {
                        return "JNDIObjectLocator.JNDIObjectLocatorBuilder(environment$key=" + this.environmentKey + ", environment$value=" +
                                this.environmentValue + ", portableNamePrefix$value=" + this.namespaceValue + ", noCaching=" + this.noCaching +
                                ", cacheRemote=" + this.cacheRemote + ")";
                }
        }

        /**
         *
         * @return new instance of the builder
         */
        public static JNDIObjectLocatorBuilder builder() {
                return new JNDIObjectLocatorBuilder();
        }

        /**
         * @return new builder that's based on this object
         */
        public JNDIObjectLocatorBuilder toBuilder() {
                final JNDIObjectLocatorBuilder builder = new JNDIObjectLocatorBuilder().namespace(this.namespace);
                if (noCaching) {
                        builder.noCaching();
                }
                if (cacheRemote) {
                        builder.cacheRemote();
                }
                if (this.environment != null) {
                        builder.environment(this.environment);
                }
                return builder;
        }
}