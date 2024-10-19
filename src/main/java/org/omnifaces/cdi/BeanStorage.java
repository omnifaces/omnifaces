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
package org.omnifaces.cdi;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.PassivationCapable;

import org.omnifaces.util.Beans;
import org.omnifaces.util.BeansLocal;
import org.omnifaces.util.Utils;

/**
 * CDI bean storage. This class is theoretically reusable for multiple CDI scopes. It's currently however only used by
 * the OmniFaces CDI view scope.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @since 1.6
 */
public class BeanStorage implements Serializable {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Properties -----------------------------------------------------------------------------------------------------

    private final ConcurrentHashMap<String, Serializable> beans;
    private final ReentrantLock lock = new ReentrantLock();

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct a new CDI bean storage with the given initial capacity of the map holding all beans.
     * @param initialCapacity The initial capacity of the map holding all beans.
     */
    public BeanStorage(int initialCapacity) {
        beans = new ConcurrentHashMap<>(initialCapacity);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns the bean associated with the given context, or if there is none, then create one with the given creational context.
     * @param <T> The generic bean type.
     * @param type The contextual type of the CDI managed bean.
     * @return The bean associated with given context and creational context.
     * @since 4.5
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Contextual<T> type, CreationalContext<T> context) {
        return (T) beans.computeIfAbsent(getBeanId(type), $ -> (Serializable) type.create(context));
    }

    /**
     * Returns the bean associated with the given context, or <code>null</code> if there is none.
     * @param <T> The generic bean type.
     * @param type The contextual type of the CDI managed bean.
     * @return The bean associated with the given context, or <code>null</code> if there is none.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Contextual<T> type) {
        return (T) beans.get(getBeanId(type));
    }

    /**
     * Returns the bean identifier of the given type.
     */
    private static String getBeanId(Contextual<?> type) {
        return type instanceof PassivationCapable passivationCapable ? passivationCapable.getId() : type.getClass().getName();
    }

    /**
     * Destroy all beans managed so far.
     */
    public synchronized void destroyBeans() { // Not sure if synchronization is absolutely necessary. Just to be on safe side.
        final var manager = Beans.getManager();
        // Not sure if synchronization is absolutely necessary. Just to be on safe side.
        Utils.executeAtomically(lock, () -> {
            beans.values().forEach(bean -> BeansLocal.destroy(manager, bean));
            beans.clear();
        });
    }

}