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
	 * @return Currently active bean storage ID, or null if it does not exist.
	 */
	public UUID getBeanStorageId();

	/**
	 * Returns the bean storage identified by given ID, or null if it does not exist.
	 * @param beanStorageId The bean storage identifier.
	 * @return The bean storage identified by given ID, or null if it does not exist.
	 */
	public BeanStorage getBeanStorage(UUID beanStorageId);

	/**
	 * Sets the given bean storage identified by the given ID.
	 * @param beanStorageId The bean storage identifier.
	 * @param beanStorage The bean storage.
	 */
	public void setBeanStorage(UUID beanStorageId, BeanStorage beanStorage);

    /**
     * Returns {@code true} if bean storage identified by given ID was recently destroyed.
     * @param beanStorageId The bean storage identifier.
     * @return {@code true} if bean storage identified by given ID was recently destroyed.
     * @since 4.6
     */
    public boolean isRecentlyDestroyed(UUID beanStorageId);
}