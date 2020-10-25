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

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import javax.ejb.Remote;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Lombok;
import lombok.NonNull;
import lombok.Singular;
import lombok.SneakyThrows;
import static org.omnifaces.util.JNDI.ENV_ENTRY_PREFIX;

/**
 * JNDIObjectLocator is used to centralize JNDI lookups. It minimizes the overhead of JNDI lookups by caching the
 * objects it looks up.
 * <p>
 * Implements the ServiceLocator design pattern
 *
 * <pre>
 *
 * Major features are:
 * - thread-safe
 * - immutable
 * - serializable
 * - selectively disables the cache if objects are remote
 *
 * Examples:
 *
 * {@code locator = JNDIObjectLocator.builder().build();
 * MyEJB myEJB1 = locator.getObject(MyEJB.class);
 * MyEJB myEJB2 = locator.getObject("java:module/MyEJB");
 * }
 * </pre>
 *
 * <a href="https://github.com/flowlogix/flowlogix/blob/master/jakarta-ee/jee-examples/src/main/java/com/flowlogix/examples/JndiExample.java"
 * target="_blank">Example Code (GitHub)</a>
 *
 * @author Lenny Primak
 */
@Builder(toBuilder = true)
public class JNDIObjectLocator implements Serializable {
    /**
     * naming convention suffix for remote beans
     */
    public static final String REMOTE = "REMOTE";
    /**
     * naming convention suffix for local beans
     */
    public static final String LOCAL = "LOCAL";
    static final String PORTABLE_NAME_PREFIX = "java:module";
    /**
     * pattern matcher for stripping local or remote suffix from beans
     */
    @SuppressWarnings("checkstyle:ConstantName")
    public static final Pattern StripInterfaceSuffixPattern = Pattern.compile(LOCAL + "|" + REMOTE, Pattern.CASE_INSENSITIVE);

    private static final long serialVersionUID = 2L;

    /**
     * to be passed into InitialContext()
     */
    private final @Singular("environment") Map<String, String> environment;
    /**
     * Used in construction of portable JNDI names
     * usually java:module (default)
     */
    private final @Builder.Default String portableNamePrefix = PORTABLE_NAME_PREFIX;
    /**
     * whether to disable cache. Default is false
     */
    private final boolean noCaching;
    /**
     * whether to cache remote EJBs, usually false
     */
    private final boolean cacheRemote;

    @Getter(AccessLevel.PACKAGE)
    private final transient Map<String, Object> jndiObjectCache = new ConcurrentHashMap<>();
    private final transient Lazy<InitialContext> initialContext = new Lazy<>(this::createInitialContext);
    private final transient Lock initialContextLock = new ReentrantLock();

    /**
     * Returns an object from JNDI based on beanClass
     * Uses portable object names and convention to derive appropriate JNDI name
     *
     * @param <T> object type
     * @param beanClass type of object to look up in JNDI
     * @return resulting object
     * @throws NamingException (sneaky-throws)
     */
    @SneakyThrows(NamingException.class)
    public <T> T getObject(Class<T> beanClass) {
        boolean remote = beanClass.isAnnotationPresent(Remote.class);
        String name = guessByType(beanClass.getName());
        return getObject(prependPortableName(name), remote && !cacheRemote);
    }

    /**
     * @see JNDI#getEnvEntry(String)
     * @param <T> object return type
     * @param name environment entry name
     * @return entry
     */
    @SneakyThrows(NamingException.class)
    public <T> T getEnvEntry(String name) {
        return getObject(ENV_ENTRY_PREFIX + name);
    }

    /**
     * Returns an object based on JNDI name
     *
     * @param <T>
     * @param jndiName
     * @return
     * @throws NamingException
     */
    public <T> T getObject(String jndiName) throws NamingException {
        return getObject(jndiName, false);
    }

    /**
     * Return an object based on JNDI name,
     * uses the caching parameter to control whether this object is cached
     *
     * @param <T>
     * @param jndiName
     * @param noCaching
     * @return
     * @throws NamingException
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String jndiName, boolean noCaching) throws NamingException {
        return (T) getJNDIObject(jndiName, noCaching);
    }

    /**
     * clears object cache
     */
    public void clearCache() {
        jndiObjectCache.clear();
    }

    /**
     * converts class name into java portable JNDI lookup name
     *
     * @param lookupname
     * @return portable JNDI name
     */
    public String prependPortableName(String lookupname) {
        //convert to jndi name
        if (!lookupname.startsWith("java:")) {
            lookupname = portableNamePrefix + "/" + lookupname;
        }
        return lookupname;
    }

    /**
     * adds initial host property to builder
     *
     * @param builder
     * @param initialHost
     * @return builder
     */
    public static JNDIObjectLocatorBuilder initialHost(JNDIObjectLocatorBuilder builder, @NonNull String initialHost) {
        return builder.environment("org.omg.CORBA.ORBInitialHost", initialHost);
    }

    /**
     * adds initial port property to builder
     *
     * @param builder
     * @param initialPort
     * @return builder
     */
    public static JNDIObjectLocatorBuilder initialPort(JNDIObjectLocatorBuilder builder, int initialPort) {
        return builder.environment("org.omg.CORBA.ORBInitialPort", Integer.toString(initialPort));
    }

    /**
     * returns JNDI name based on the class (type) name
     *
     * @param type name of the class
     * @return JNDI lookup name
     */
    public static String guessByType(String type) {
        String lookupname = type.substring(type.lastIndexOf(".") + 1);
        // support naming convention that strips Local/Remote from the
        // end of an interface class to try to determine the actual bean name,
        // to avoid @EJB(beanName="myBeanName"), and just use plain old @EJB
        String uc = lookupname.toUpperCase();
        if (uc.endsWith(LOCAL) || uc.endsWith(REMOTE)) {
            lookupname = StripInterfaceSuffixPattern.matcher(lookupname).replaceFirst("");
        }
        return lookupname + "!" + type;
    }

    @SneakyThrows(NamingException.class)
    private InitialContext createInitialContext() {
        if (environment.isEmpty()) {
            return new InitialContext();
        } else {
            return new InitialContext(new Hashtable<>(environment));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getJNDIObject(String jndiName, boolean noCaching) throws NamingException {
        if (noCaching || this.noCaching) {
            initialContextLock.lock();
            try {
                return (T) initialContext.get().lookup(jndiName);
            } finally {
                initialContextLock.unlock();
            }
        }

        T jndiObject = (T) jndiObjectCache.computeIfAbsent(jndiName, (key) -> {
            initialContextLock.lock();
            boolean shouldClearCache = false;
            try {
                return (T) initialContext.get().lookup(jndiName);
            } catch (NamingException ex) {
                shouldClearCache = true;
                throw Lombok.sneakyThrow(ex);
            } finally {
                initialContextLock.unlock();
                if (shouldClearCache) {
                    clearCache();
                }
            }
        });

        return jndiObject;
    }

    /**
     * this deals with transient final fields correctly
     *
     * @return new object
     */
    private Object readResolve() {
        return toBuilder().build();
    }
}
