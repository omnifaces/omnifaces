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

import static java.util.Collections.unmodifiableMap;
import static org.omnifaces.util.Servlets.getRequestRelativeURI;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletRequestEvent;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.omnifaces.eventlistener.DefaultServletRequestListener;

/**
 * 
 * @author Arjan Tijms
 * @since 1.8
 *
 */
@WebListener
public class EagerBeansRequestListener extends DefaultServletRequestListener {
	
	private static BeanManager beanManager;
	private static Map<String, List<Bean<?>>> beans;
	
	static void init(BeanManager beanManager, Map<String, List<Bean<?>>> beans) {
		EagerBeansRequestListener.beanManager = beanManager;
		EagerBeansRequestListener.beans = unmodifiableMap(beans);
	}
	
	
	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		if (beans == null || beanManager == null || beans.get(getRequestRelativeURI((HttpServletRequest)sre.getServletRequest())) == null) {
			return;
		}
		
		for (Bean<?> bean : beans.get(getRequestRelativeURI((HttpServletRequest)sre.getServletRequest()))) {
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
		}
	}

}
