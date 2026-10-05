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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.viewscope.ViewScopeStorageInSession.ActiveBeanStorages;

/**
 * When the current HTTP request releases a bean storage, then the beans destroyed by that release must still resolve the
 * bean storage via the current HTTP request, so that a <code>&#64;PreDestroy</code> which references another view scoped
 * bean of the same view does not create a new view scope. After the release, the current HTTP request must no longer
 * resolve it.
 */
class ActiveBeanStoragesTest {

	@Test
	void beanStorageIsResolvableDuringReleaseOnly() {
		ActiveBeanStorages activeBeanStorages = new ActiveBeanStorages();
		UUID beanStorageId = UUID.randomUUID();
		AtomicReference<BeanStorage> resolvedDuringRelease = new AtomicReference<>();
		BeanStorage beanStorage = new BeanStorage(1) {
			private static final long serialVersionUID = 1L;

			@Override
			public void release() {
				resolvedDuringRelease.set(activeBeanStorages.getBeanStorage(beanStorageId));
			}
		};

		activeBeanStorages.acquire(beanStorageId, beanStorage);
		activeBeanStorages.release(beanStorageId);

		assertSame(beanStorage, resolvedDuringRelease.get(), "resolvable during release");
		assertNull(activeBeanStorages.getBeanStorage(beanStorageId), "not resolvable after release");
	}

}
