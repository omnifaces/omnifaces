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
package org.omnifaces.cdi.viewscope;

import java.util.UUID;

import jakarta.faces.context.FacesContext;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;

/**
 * Interface for view scope bean storage.
 *
 * @author Bauke Scholtz
 * @see ViewScoped
 * @see ViewScopeManager
 * @since 2.6
 */
public interface ViewScopeStorage {

    /**
     * Returns currently active bean storage ID, or null if it does not exist.
     * <p>
     * The default implementation call {@link #setBeanStorage(FacesContext, UUID, BeanStorage)}
     * passing the current {@link FacesContext}
     * @return Currently active bean storage ID, or null if it does not exist.
     */
    default UUID getBeanStorageId() {
        var context = FacesContext.getCurrentInstance();
        return getBeanStorageId(context);
    }

    /**
     * Returns currently active bean storage ID, or null if it does not exist.
     * @param context The involved {@link FacesContext}
     * @return Currently active bean storage ID, or null if it does not exist.
     * @since 4.6
     */
    UUID getBeanStorageId(FacesContext context);

    /**
     * Returns the bean storage identified by given ID, or null if it does not exist.
     * <p>
     * The default implementation call {@link #setBeanStorage(FacesContext, UUID, BeanStorage)}
     * passing the current {@link FacesContext}
     * @param beanStorageId The bean storage identifier.
     * @return The bean storage identified by given ID, or null if it does not exist.
     */
    default BeanStorage getBeanStorage(UUID beanStorageId) {
        var context = FacesContext.getCurrentInstance();
        return getBeanStorage(context, beanStorageId);
    }

    /**
     * Returns the bean storage identified by given ID, or null if it does not exist.
     * @param context The involved {@link FacesContext}
     * @param beanStorageId The bean storage identifier.
     * @return The bean storage identified by given ID, or null if it does not exist.
     * @since 4.6
     */
    BeanStorage getBeanStorage(FacesContext context, UUID beanStorageId);

    /**
     * Sets the given bean storage identified by the given ID.
     * <p>
     * The default implementation call {@link #setBeanStorage(FacesContext, UUID, BeanStorage)}
     * passing the current {@link FacesContext}
     * @param beanStorageId The bean storage identifier.
     * @param beanStorage The bean storage.
     */
    default void setBeanStorage(UUID beanStorageId, BeanStorage beanStorage) {
        var context = FacesContext.getCurrentInstance();
        setBeanStorage(context, beanStorageId, beanStorage);
    }

    /**
     * Sets the given bean storage identified by the given ID.
     * @param context The involved {@link FacesContext}
     * @param beanStorageId The bean storage identifier.
     * @param beanStorage The bean storage.
     * @since 4.6
     */
    void setBeanStorage(FacesContext context, UUID beanStorageId, BeanStorage beanStorage);

}