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
package org.omnifaces.cdi.viewscope;

import static org.omnifaces.util.Beans.getReference;

import javax.faces.component.UIViewRoot;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.ViewMapListener;

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
	 */
	@Override
	public void processEvent(SystemEvent event) throws AbortProcessingException {
		if (event instanceof PreDestroyViewMapEvent) {
			getReference(ViewScopeManager.class).preDestroyView();
		}
	}

}