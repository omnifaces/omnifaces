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
package org.omnifaces.test.cdi.viewscoped.concurrent;

import static org.omnifaces.util.Faces.getRequestParameter;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class ViewScopedConcurrentITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private ViewScopedConcurrentITLatch latch;

	private String param;

	/**
	 * The view scope is registered in the session before this runs, so awaiting here guarantees that all concurrent
	 * requests have registered theirs before any of them proceeds to update the model and render the response. The
	 * session creating request carries no <code>param</code> and must therefore not await.
	 */
	@PostConstruct
	public void init() {
		if (getRequestParameter("param") != null) {
			latch.awaitOtherRequests();
		}
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

}
