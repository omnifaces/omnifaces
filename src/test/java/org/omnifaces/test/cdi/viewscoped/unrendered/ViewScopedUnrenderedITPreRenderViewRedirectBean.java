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
package org.omnifaces.test.cdi.viewscoped.unrendered;

import static org.omnifaces.util.Faces.redirect;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

/**
 * Redirects in a pre render view listener, which runs before the JSF implementation checks whether the response is
 * complete.
 */
@Named
@ViewScoped
public class ViewScopedUnrenderedITPreRenderViewRedirectBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ViewScopedUnrenderedITProbe probe;

	@PostConstruct
	public void init() {
		probe.create("preRenderView");
	}

	@PreDestroy
	public void destroy() {
		probe.destroy("preRenderView");
	}

	public void redirectAway() {
		redirect("ViewScopedUnrenderedIT.xhtml");
	}

}
