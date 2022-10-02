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

import static org.omnifaces.util.Faces.getViewRoot;

import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.faces.component.UIViewRoot;

import org.omnifaces.cdi.BeanStorage;
import org.omnifaces.cdi.ViewScoped;

/**
 * Stores view scoped bean instances in Faces view state itself.
 *
 * @author Bauke Scholtz
 * @see ViewScoped
 * @see ViewScopeManager
 * @since 2.6
 */
@Dependent // This class is supposed to be entirely stateless.
public class ViewScopeStorageInViewState implements ViewScopeStorage {

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public UUID getBeanStorageId() {
		return (UUID) getViewRoot().getAttributes().get(getClass().getName());
	}

	@Override
	public BeanStorage getBeanStorage(UUID id) {
		return (BeanStorage) getViewRoot().getAttributes().get(id.toString());
	}

	@Override
	public void setBeanStorage(UUID beanStorageId, BeanStorage beanStorage) {
		UIViewRoot viewRoot = getViewRoot();

		if (!viewRoot.initialStateMarked()) {
			viewRoot.markInitialState(); // Forces Faces to start recording changes in view state.
		}

		Map<String, Object> viewState = viewRoot.getAttributes();
		viewState.put(getClass().getName(), beanStorageId);
		viewState.put(beanStorageId.toString(), beanStorage);
	}

}