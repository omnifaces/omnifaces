/*
 * Copyright 2017 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.test.cdi.viewscoped;

import static javax.faces.application.ResourceHandler.RESOURCE_IDENTIFIER;
import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.hasContext;
import static org.omnifaces.util.Messages.addGlobalInfo;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Faces;

@Named
@ViewScoped
public class ViewScopedITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static boolean unloaded;
	private static boolean destroyed;

	@PostConstruct
	public void init() {
		checkUnloadedOrDestroyed();
		addGlobalInfo("init ");
	}

	public void checkUnloadedOrDestroyed() {
		if (unloaded) {
			addGlobalInfo("unload ");
			unloaded = false;
		}
		else if (destroyed) {
			addGlobalInfo("destroy ");
			destroyed = false;
		}
	}

	public void submit() {
		checkUnloadedOrDestroyed();
		addGlobalInfo("submit ");
	}

	public void navigate() {
		Object renderedResources = Faces.getContextAttribute(RESOURCE_IDENTIFIER);
		Faces.setViewRoot(Faces.getViewId()); // TODO: replace by return getViewId() once Mojarra 2.3.1 is released. See #4249
		Faces.setContextAttribute(RESOURCE_IDENTIFIER, renderedResources); // TODO: remove once Mojarra 2.3.1 is released. See #4249
		addGlobalInfo("navigate ");
	}

	@PreDestroy
	public void destroy() {
		if (hasContext() && isUnloadRequest(getContext())) {
			unloaded = true;
		}
		else {
			destroyed = true;
		}
	}

}