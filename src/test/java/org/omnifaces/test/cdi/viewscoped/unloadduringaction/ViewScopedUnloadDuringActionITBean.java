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
package org.omnifaces.test.cdi.viewscoped.unloadduringaction;

import static org.omnifaces.util.Faces.setRequestAttribute;

import java.io.Serializable;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class ViewScopedUnloadDuringActionITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ViewScopedUnloadDuringActionITProbe probe;

	private final String id = UUID.randomUUID().toString();
	private volatile boolean destroyed;

	/**
	 * The outcome is set as request attribute, as rendering a property of this bean would resolve a new instance when
	 * this one is destroyed.
	 */
	public void action() {
		probe.startAction(id);
		setRequestAttribute("destroyedDuringAction", destroyed);
	}

	@PreDestroy
	public void destroy() {
		destroyed = true;
		probe.destroyBean(id);
	}

	public String getId() {
		return id;
	}

}
