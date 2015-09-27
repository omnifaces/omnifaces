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
import static java.util.logging.Level.WARNING;
import static org.omnifaces.util.Utils.isAnyEmpty;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletContext;

/**
 * Bean repository via which various types of eager beans can be instantiated on demand.
 *
 * @since 1.8
 * @author Arjan Tijms
 *
 */
@ApplicationScoped
public class EagerBeansRepository {

	private static final Logger logger = Logger.getLogger(EagerBeansRepository.class.getName());

	private static final String POSSIBLY_APPLICATION_SCOPE_NOT_ACTIVE =
		"Could not instantiate eager application scoped beans. Possibly the CDI application scope is not active."
			+ " This is known to be the case in certain Tomcat and Jetty based configurations.";

	private static EagerBeansRepository instance;

	@Inject
	private BeanManager beanManager;

	private List<Bean<?>> applicationScopedBeans;
	private List<Bean<?>> sessionScopedBeans;
	private Map<String, List<Bean<?>>> requestScopedBeansViewId;
	private Map<String, List<Bean<?>>> requestScopedBeansRequestURI;

	public static EagerBeansRepository getInstance() { // Awkward workaround for it being unavailable via @Inject in listeners in Tomcat+OWB and Jetty.
		if (instance == null) {
			instance = org.omnifaces.config.BeanManager.INSTANCE.getReference(EagerBeansRepository.class);
		}

		return instance;
	}

	public static void instantiateApplicationScopedAndRegisterListener(ServletContext servletContext) {
		try {
			getInstance().instantiateApplicationScoped();
		}
		catch (Exception e) {
			logger.log(WARNING, format(POSSIBLY_APPLICATION_SCOPE_NOT_ACTIVE), e);
		}

		if (instance == null || instance.hasAnySessionOrRequestScopedBeans()) {
			servletContext.addListener(EagerBeansWebListener.class);
		}
	}

	public void instantiateApplicationScoped() {
		if (isAnyEmpty(applicationScopedBeans, beanManager)) {
			return;
		}

		instantiateBeans(applicationScopedBeans);
	}

	public void instantiateSessionScoped() {
		if (isAnyEmpty(sessionScopedBeans, beanManager)) {
			return;
		}

		instantiateBeans(sessionScopedBeans);
	}

	public void instantiateByRequestURI(String relativeRequestURI) {
		instantiateRequestScopedBeans(requestScopedBeansRequestURI, relativeRequestURI);
	}

	public void instantiateByViewID(String viewId) {
		instantiateRequestScopedBeans(requestScopedBeansViewId, viewId);
	}

	private void instantiateRequestScopedBeans(Map<String, List<Bean<?>>> beans, String key) {
		if (isAnyEmpty(beans, beanManager) || !beans.containsKey(key)) {
			return;
		}

		instantiateBeans(beans.get(key));
	}

	private void instantiateBeans(List<Bean<?>> beans) {
		for (Bean<?> bean : beans) {
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
		}
	}

	void setApplicationScopedBeans(List<Bean<?>> applicationScopedBeans) {
		this.applicationScopedBeans = applicationScopedBeans;
	}

	void setSessionScopedBeans(List<Bean<?>> sessionScopedBeans) {
		this.sessionScopedBeans = sessionScopedBeans;
	}

	void setRequestScopedBeansViewId(Map<String, List<Bean<?>>> requestScopedBeansViewId) {
		this.requestScopedBeansViewId = requestScopedBeansViewId;
	}

	void setRequestScopedBeansRequestURI(Map<String, List<Bean<?>>> requestScopedBeansRequestURI) {
		this.requestScopedBeansRequestURI = requestScopedBeansRequestURI;
	}

	boolean hasAnySessionOrRequestScopedBeans() {
		return !(isEmpty(sessionScopedBeans) && isEmpty(requestScopedBeansRequestURI));
	}

}