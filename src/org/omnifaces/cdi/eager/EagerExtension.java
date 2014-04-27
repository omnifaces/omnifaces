package org.omnifaces.cdi.eager;

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

public class EagerExtension implements Extension {

	private Map<String, List<Bean<?>>> requestScopedEagerBeans = new HashMap<String, List<Bean<?>>>();

	public <T> void collect(@Observes ProcessBean<T> event) {
		
		Annotated annotated = event.getAnnotated();
		
		if (annotated.isAnnotationPresent(Eager.class)) {
			
			if (annotated.isAnnotationPresent(RequestScoped.class)) {
				List<Bean<?>> beans = getRequestScopedBeansByViewId(annotated.getAnnotation(Eager.class).viewId());
				beans.add(event.getBean());
			}
		}
	}

	public void load(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		InstantiateEagerBeansListener.init(beanManager, requestScopedEagerBeans);
	}

	private List<Bean<?>> getRequestScopedBeansByViewId(String viewId) {
		List<Bean<?>> beans = requestScopedEagerBeans.get(viewId);
		if (beans == null) {
			beans = new ArrayList<Bean<?>>();
			requestScopedEagerBeans.put(viewId, beans);
		}
		
		return beans;
	}
	
}