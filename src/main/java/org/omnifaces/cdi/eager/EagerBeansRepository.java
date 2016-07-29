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
import static org.omnifaces.util.Beans.getReference;
import static org.omnifaces.util.Utils.isAnyEmpty;
import static org.omnifaces.util.Utils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.omnifaces.util.Utils;

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

	private static final String MISSING_REQUEST_URI_OR_VIEW_ID =
		"Bean '%s' was annotated with @Eager, but required attribute 'requestURI' or 'viewId' is missing."
			+ " Bean will not be eagerly instantiated.";
	private static final String MISSING_VIEW_ID =
		"Bean '%s' was annotated with @Eager, but required attribute 'viewId' is missing."
			+ " Bean will not be eagerly instantiated.";
	private static final String WARNING_POSSIBLY_APPLICATION_SCOPE_NOT_ACTIVE =
		"Could not instantiate eager application scoped beans. Possibly the CDI application scope is not active."
			+ " This is known to be the case in certain Tomcat and Jetty based configurations.";

	private static volatile EagerBeansRepository instance;

	@Inject
	private BeanManager beanManager;
	private EagerBeans eagerBeans;

	public static EagerBeansRepository getInstance() { // Awkward workaround for it being unavailable via @Inject in listeners in Tomcat+OWB and Jetty.
		if (instance == null) {
			instance = getReference(EagerBeansRepository.class);
		}

		return instance;
	}

	public static void instantiateApplicationScopedAndRegisterListener(ServletContext servletContext) {
		if (getInstance() != null && instance.hasAnyApplicationScopedBeans()) {
			try {
				instance.instantiateApplicationScoped();
			}
			catch (Exception e) {
				logger.log(WARNING, format(WARNING_POSSIBLY_APPLICATION_SCOPE_NOT_ACTIVE), e);
				instance = null; // Trigger to add listeners anyway as it may be available at later point.
			}
		}

		if (instance == null || instance.hasAnySessionOrRequestURIBeans()) {
			servletContext.addListener(EagerBeansWebListener.class);
		}
	}

	protected void setEagerBeans(EagerBeans eagerBeans) {
		this.eagerBeans = eagerBeans;
	}

	protected boolean hasAnyApplicationScopedBeans() {
		return eagerBeans != null && !isEmpty(eagerBeans.applicationScoped);
	}

	protected boolean hasAnySessionOrRequestURIBeans() {
		return eagerBeans != null && (!isEmpty(eagerBeans.sessionScoped) || !isEmpty(eagerBeans.byRequestURI));
	}

	protected boolean hasAnyViewIdBeans() {
		return eagerBeans != null && !isEmpty(eagerBeans.byViewId);
	}

	public void instantiateApplicationScoped() {
		instantiateBeans(eagerBeans.applicationScoped);
	}

	public void instantiateSessionScoped() {
		instantiateBeans(eagerBeans.sessionScoped);
	}

	public void instantiateByRequestURI(String relativeRequestURI) {
		instantiateBeans(eagerBeans.byRequestURI, relativeRequestURI);
	}

	public void instantiateByViewID(String viewId) {
		instantiateBeans(eagerBeans.byViewId, viewId);
	}

	private void instantiateBeans(Map<String, List<Bean<?>>> beansByKey, String key) {
		if (isAnyEmpty(beansByKey, key)) {
			return;
		}

		instantiateBeans(beansByKey.get(key));
	}

	private void instantiateBeans(List<Bean<?>> beans) {
		if (isAnyEmpty(beans, beanManager)) {
			return;
		}

		for (Bean<?> bean : beans) {
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
		}
	}

	static class EagerBeans {

		private List<Bean<?>> applicationScoped = new ArrayList<>();
		private List<Bean<?>> sessionScoped = new ArrayList<>();
		private Map<String, List<Bean<?>>> byViewId = new ConcurrentHashMap<>();
		private Map<String, List<Bean<?>>> byRequestURI = new ConcurrentHashMap<>();

		void addApplicationScoped(Bean<?> bean) {
			applicationScoped.add(bean);
		}

		void addSessionScoped(Bean<?> bean) {
			sessionScoped.add(bean);
		}

		void addByViewId(Bean<?> bean, String viewId) {
			if (!Utils.isEmpty(viewId)) {
				getByViewId(viewId).add(bean);
			}
			else {
				logger.severe(format(MISSING_VIEW_ID, bean.getBeanClass().getName()));
			}
		}

		void addByRequestURIOrViewId(Bean<?> bean, String requestURI, String viewId) {
			if (!Utils.isEmpty(requestURI)) {
				getByRequestURI(requestURI).add(bean);
			}
			else if (!Utils.isEmpty(viewId)) {
				getByViewId(viewId).add(bean);
			}
			else {
				logger.severe(format(MISSING_REQUEST_URI_OR_VIEW_ID, bean.getBeanClass().getName()));
			}
		}

		private List<Bean<?>> getByViewId(String viewId) {
			List<Bean<?>> beans = byViewId.get(viewId);

			if (beans == null) {
				beans = new ArrayList<>();
				byViewId.put(viewId, beans);
			}

			return beans;
		}

		private List<Bean<?>> getByRequestURI(String requestURI) {
			List<Bean<?>> beans = byRequestURI.get(requestURI);

			if (beans == null) {
				beans = new ArrayList<>();
				byRequestURI.put(requestURI, beans);
			}

			return beans;
		}

		public boolean isEmpty() {
			return applicationScoped.isEmpty() && sessionScoped.isEmpty() && byViewId.isEmpty() && byRequestURI.isEmpty();
		}

	}

}