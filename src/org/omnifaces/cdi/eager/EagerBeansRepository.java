/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.cdi.eager;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 
 * @author Arjan Tijms
 * @since 1.8
 *
 */
@Named
@ApplicationScoped
public class EagerBeansRepository implements HideForTomcatEagerBeansRepository {

	@Inject
	private BeanManager beanManager;

	private Map<String, List<Bean<?>>> requestScopedBeansViewId;
	private Map<String, List<Bean<?>>> requestScopedBeansRequestURI;

	public void instantiateByRequestURI(String relativeRequestURI) {
		instantiateRequestScopedBeans(requestScopedBeansRequestURI, relativeRequestURI);
	}
	
	public void instantiateByViewID(String viewId) {
		instantiateRequestScopedBeans(requestScopedBeansViewId, viewId);
	}
	
	private void instantiateRequestScopedBeans(Map<String, List<Bean<?>>> beans, String key) {
		if (beans == null || beanManager == null || beans.get(key) == null) {
			return;
		}
		
		instantiateBeans(beans.get(key));
	}
	
	private void instantiateBeans(List<Bean<?>> beans) {
		for (Bean<?> bean : beans) {
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
		}
	}
	
	public void setRequestScopedBeansViewId(Map<String, List<Bean<?>>> requestScopedBeansViewId) {
		this.requestScopedBeansViewId = requestScopedBeansViewId;
	}

	public void setRequestScopedBeansRequestURI(Map<String, List<Bean<?>>> requestScopedBeansRequestURI) {
		this.requestScopedBeansRequestURI = requestScopedBeansRequestURI;
	}

}
