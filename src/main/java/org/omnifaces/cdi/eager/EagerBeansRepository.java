/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.cdi.eager;

import static java.lang.String.format;
import static java.util.logging.Level.SEVERE;
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

import org.omnifaces.util.BeansLocal;
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

	private static EagerBeansRepository instance;

	@Inject
	private BeanManager beanManager;
	private EagerBeans eagerBeans;

	public static EagerBeansRepository getInstance() { // Awkward workaround for it being unavailable via @Inject in listeners in Tomcat+OWB and Jetty.
		if (instance == null) {
			instance = getReference(EagerBeansRepository.class);
		}

		return instance;
	}

	protected void setEagerBeans(EagerBeans eagerBeans) {
		this.eagerBeans = eagerBeans;
	}

	public static void instantiateApplicationScopedAndRegisterListenerIfNecessary(ServletContext servletContext) {
		try {
			if (getInstance() != null && instance.hasAnyApplicationScopedBeans()) { // #318: getInstance() should stay in try block.
				instance.instantiateApplicationScoped();
			}
		}
		catch (Exception e) {
			logger.log(WARNING, format(WARNING_POSSIBLY_APPLICATION_SCOPE_NOT_ACTIVE), e);
			instance = null; // Trigger to add listeners anyway as it may be available at later point.
		}

		if (instance == null || instance.hasAnySessionOrRequestURIBeans()) {
			servletContext.addListener(EagerBeansWebListener.class);
		}
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

	public boolean instantiateSessionScoped() {
		return eagerBeans != null && instantiateBeans(eagerBeans.sessionScoped);
	}

	public boolean instantiateByRequestURI(String relativeRequestURI) {
		return eagerBeans != null && instantiateBeans(eagerBeans.byRequestURI, relativeRequestURI);
	}

	public void instantiateByViewID(String viewId) {
		instantiateBeans(eagerBeans.byViewId, viewId);
	}

	private boolean instantiateBeans(Map<String, List<Bean<?>>> beansByKey, String key) {
		if (isAnyEmpty(beansByKey, key)) {
			return false;
		}

		instantiateBeans(beansByKey.get(key));
		return true;
	}

	private boolean instantiateBeans(List<Bean<?>> beans) {
		if (isAnyEmpty(beans, beanManager)) {
			return false;
		}

		for (Bean<?> bean : beans) {
			BeansLocal.getInstance(beanManager, bean, true).toString();
		}

		return true;
	}

	protected static class EagerBeans {

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
			else if (logger.isLoggable(SEVERE)) {
				logger.log(SEVERE, format(MISSING_VIEW_ID, bean.getBeanClass().getName()));
			}
		}

		void addByRequestURIOrViewId(Bean<?> bean, String requestURI, String viewId) {
			if (!Utils.isEmpty(requestURI)) {
				getByRequestURI(requestURI).add(bean);
			}
			else if (!Utils.isEmpty(viewId)) {
				getByViewId(viewId).add(bean);
			}
			else if (logger.isLoggable(SEVERE)) {
				logger.log(SEVERE, format(MISSING_REQUEST_URI_OR_VIEW_ID, bean.getBeanClass().getName()));
			}
		}

		private List<Bean<?>> getByViewId(String viewId) {
			return byViewId.computeIfAbsent(viewId, k -> new ArrayList<>());
		}

		private List<Bean<?>> getByRequestURI(String requestURI) {
			return byRequestURI.computeIfAbsent(requestURI, k -> new ArrayList<>());
		}

		public boolean isEmpty() {
			return applicationScoped.isEmpty() && sessionScoped.isEmpty() && byViewId.isEmpty() && byRequestURI.isEmpty();
		}

	}

}