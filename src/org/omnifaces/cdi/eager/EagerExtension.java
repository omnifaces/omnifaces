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

import static org.omnifaces.util.Beans.getInstance;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import org.omnifaces.cdi.Eager;

/**
 * 
 * @author Arjan Tijms
 * @since 1.8
 *
 */
public class EagerExtension implements Extension {

	private Map<String, List<Bean<?>>> requestScopedBeansViewId = new HashMap<String, List<Bean<?>>>();
	private Map<String, List<Bean<?>>> requestScopedBeansRequestURI = new HashMap<String, List<Bean<?>>>();

	public <T> void collect(@Observes ProcessBean<T> event) {
		
		Annotated annotated = event.getAnnotated();
		
		if (annotated.isAnnotationPresent(Eager.class)) {
			
			Eager eager = annotated.getAnnotation(Eager.class);
			Bean<?> bean = event.getBean();
			
			if (annotated.isAnnotationPresent(RequestScoped.class)) {
				
				if (!isEmpty(eager.requestURI())) {
					getRequestScopedBeansByRequestURI(eager.requestURI()).add(bean);
				} else if (!isEmpty(eager.viewId())) {
					getRequestScopedBeansByViewId(eager.viewId()).add(bean);
				}
			}
		}
	}

	public void load(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		
		EagerBeansRepository eagerBeansRepository = getInstance(beanManager, EagerBeansRepository.class);
		
		if (!requestScopedBeansRequestURI.isEmpty()) {
			eagerBeansRepository.setRequestScopedBeansRequestURI(requestScopedBeansRequestURI);
		}
		
		if (!requestScopedBeansViewId.isEmpty()) {
			eagerBeansRepository.setRequestScopedBeansViewId(requestScopedBeansViewId);
		}
	}

	private List<Bean<?>> getRequestScopedBeansByViewId(String viewId) {
		List<Bean<?>> beans = requestScopedBeansViewId.get(viewId);
		if (beans == null) {
			beans = new ArrayList<Bean<?>>();
			requestScopedBeansViewId.put(viewId, beans);
		}
		
		return beans;
	}
	
	private List<Bean<?>> getRequestScopedBeansByRequestURI(String requestURI) {
		List<Bean<?>> beans = requestScopedBeansRequestURI.get(requestURI);
		if (beans == null) {
			beans = new ArrayList<Bean<?>>();
			requestScopedBeansRequestURI.put(requestURI, beans);
		}
		
		return beans;
	}
	
}