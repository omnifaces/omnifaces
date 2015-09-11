/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.application;

import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.responseComplete;

import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.ViewMapListener;

import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.config.BeanManager;

/**
 * Listener for JSF view scope destroy events so that view scope manager can be notified.
 *
 * @author Bauke Scholtz
 * @see ViewScopeManager
 * @since 1.6
 */
public class ViewScopeEventListener implements ViewMapListener {

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> if given source is an instance of {@link UIViewRoot}.
	 */
	@Override
	public boolean isListenerForSource(Object source) {
		return (source instanceof UIViewRoot);
	}

	/**
	 * If the event is an instance of {@link PreDestroyViewMapEvent}, which means that the JSF view scope is about to
	 * be destroyed, then find the current instance of {@link ViewScopeManager} and invoke its
	 * {@link ViewScopeManager#preDestroyView()} method.
	 * <p>
	 * Or, if the event is an instance of {@link PostRestoreStateEvent}, which means that the JSF view scope has
	 * recently restored, then check if the current request is an unload request and if so, then destroy the JSF view
	 * scope the same way as described above.
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (event instanceof PreDestroyViewMapEvent) {
			processPreDestroyView();
		}
		else if (event instanceof PostRestoreStateEvent && ViewScopeManager.isUnloadRequest(getContext())) {
			processPreDestroyView();
			responseComplete();
		}
	}

	private void processPreDestroyView() {
		BeanManager.INSTANCE.getReference(ViewScopeManager.class).preDestroyView();
	}

}