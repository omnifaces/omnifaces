/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.test.cdi.viewscoped.viewstate;

import static org.omnifaces.cdi.viewscope.ViewScopeManager.isUnloadRequest;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getViewId;
import static org.omnifaces.util.Faces.hasContext;
import static org.omnifaces.util.Messages.addGlobalInfo;

import java.io.Serializable;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped(saveInViewState=true)
public class ViewScopedViewStateITBean implements Serializable {


	private static final long serialVersionUID = 1L;

	private static boolean unloaded;
	private static boolean destroyed;

	private String uuid;

	@PostConstruct
	public void init() {
		uuid = UUID.randomUUID().toString();
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

	public String navigate() {
		addGlobalInfo("navigate ");
		return getViewId();
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

	@Override
	public String toString() {
		return getClass() + "@" + uuid;
	}

}