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

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.omnifaces.util.BeansLocal.getAnnotation;
import static org.omnifaces.util.BeansLocal.getReference;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.faces.view.ViewScoped;

import org.omnifaces.cdi.Eager;

/**
 * CDI extension that collects beans annotated with <code>&#64;</code>{@link Eager}. After deployment
 * collected beans are transferred to the {@link EagerBeansRepository}.
 *
 * @author Arjan Tijms
 * @since 1.8
 *
 */
public class EagerExtension implements Extension {

	private static final Logger logger = Logger.getLogger(EagerExtension.class.getName());

	// Private constants ----------------------------------------------------------------------------------------------

	private static final String MISSING_REQUEST_URI_OR_VIEW_ID =
		"Bean %s with scope %s was annotated with @Eager, but required attribute 'requestURI' or 'viewId' is missing."
			+ " Bean will not be eagerly instantiated.";

	private static final String MISSING_VIEW_ID =
		"Bean %s with scope %s was annotated with @Eager, but required attribute 'viewId' is missing."
			+ " Bean will not be eagerly instantiated.";

	private static final String ERROR_EAGER_UNAVAILABLE =
		"@Eager is unavailable. The EagerBeansRepository could not be obtained from CDI bean manager.";

	// Variables ------------------------------------------------------------------------------------------------------

	private List<Bean<?>> applicationScopedBeans = new ArrayList<>();
	private List<Bean<?>> sessionScopedBeans = new ArrayList<>();

	private Map<String, List<Bean<?>>> requestScopedBeansViewId = new HashMap<>();
	private Map<String, List<Bean<?>>> requestScopedBeansRequestURI = new HashMap<>();

	// Actions --------------------------------------------------------------------------------------------------------

	public <T> void collect(@Observes ProcessBean<T> event, BeanManager beanManager) {

		Annotated annotated = event.getAnnotated();
		Eager eager = getAnnotation(beanManager, annotated, Eager.class);

		if (eager != null) {

			Bean<?> bean = event.getBean();

			if (getAnnotation(beanManager, annotated, ApplicationScoped.class) != null) {
				applicationScopedBeans.add(bean);
			} else if (getAnnotation(beanManager, annotated, SessionScoped.class) != null) {
				sessionScopedBeans.add(bean);
			} else if (getAnnotation(beanManager, annotated, RequestScoped.class) != null) {
				addRequestScopedBean(eager, bean);
			} else if (getAnnotation(beanManager, annotated, ViewScoped.class) != null) {
				addViewScopedBean(eager, bean);
			} else if (getAnnotation(beanManager, annotated, org.omnifaces.cdi.ViewScoped.class) != null) {
				addOmniViewScopedBean(eager, bean);
			}
		}
	}

	public void load(@Observes AfterDeploymentValidation event, BeanManager beanManager) {

		EagerBeansRepository eagerBeansRepository = getReference(beanManager, EagerBeansRepository.class);

		if (eagerBeansRepository == null) {
			logger.warning(ERROR_EAGER_UNAVAILABLE);
			return;
		}

		if (!applicationScopedBeans.isEmpty()) {
			eagerBeansRepository.setApplicationScopedBeans(unmodifiableList(applicationScopedBeans));
		}

		if (!sessionScopedBeans.isEmpty()) {
			eagerBeansRepository.setSessionScopedBeans(unmodifiableList(sessionScopedBeans));
		}

		if (!requestScopedBeansRequestURI.isEmpty()) {
			eagerBeansRepository.setRequestScopedBeansRequestURI(unmodifiableMap(requestScopedBeansRequestURI));
		}

		if (!requestScopedBeansViewId.isEmpty()) {
			eagerBeansRepository.setRequestScopedBeansViewId(unmodifiableMap(requestScopedBeansViewId));
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	private void addRequestScopedBean(Eager eager, Bean<?> bean) {
		if (!isEmpty(eager.requestURI())) {
			getRequestScopedBeansByRequestURI(eager.requestURI()).add(bean);
		} else if (!isEmpty(eager.viewId())) {
			getRequestScopedBeansByViewId(eager.viewId()).add(bean);
		} else {
			logger.severe(format(MISSING_REQUEST_URI_OR_VIEW_ID, bean.getBeanClass().getName(), RequestScoped.class.getName()));
		}
	}

	private void addViewScopedBean(Eager eager, Bean<?> bean) {
		if (!isEmpty(eager.viewId())) {
			getRequestScopedBeansByViewId(eager.viewId()).add(bean);
		} else {
			logger.severe(format(MISSING_VIEW_ID, bean.getBeanClass().getName(), ViewScoped.class.getName()));
		}
	}

	private void addOmniViewScopedBean(Eager eager, Bean<?> bean) {
		if (!isEmpty(eager.viewId())) {
			getRequestScopedBeansByViewId(eager.viewId()).add(bean);
		} else {
			logger.severe(format(MISSING_VIEW_ID, bean.getBeanClass().getName(), org.omnifaces.cdi.ViewScoped.class.getName()));
		}
	}

	private List<Bean<?>> getRequestScopedBeansByViewId(String viewId) {
		List<Bean<?>> beans = requestScopedBeansViewId.get(viewId);
		if (beans == null) {
			beans = new ArrayList<>();
			requestScopedBeansViewId.put(viewId, beans);
		}

		return beans;
	}

	private List<Bean<?>> getRequestScopedBeansByRequestURI(String requestURI) {
		List<Bean<?>> beans = requestScopedBeansRequestURI.get(requestURI);
		if (beans == null) {
			beans = new ArrayList<>();
			requestScopedBeansRequestURI.put(requestURI, beans);
		}

		return beans;
	}

}