package org.omnifaces.cdi.eager;

import static java.util.Collections.unmodifiableMap;
import static javax.faces.event.PhaseId.RESTORE_VIEW;
import static org.omnifaces.util.Faces.getViewId;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.event.PhaseEvent;

import org.omnifaces.eventlistener.DefaultPhaseListener;

public class InstantiateEagerBeansListener extends DefaultPhaseListener {
	
	private static final long serialVersionUID = -7252366571645029385L;
	
	private static BeanManager beanManager;
	private static Map<String, List<Bean<?>>> beans;

	public InstantiateEagerBeansListener() {
		super(RESTORE_VIEW);
	}
	
	@Override
	public void afterPhase(PhaseEvent event) {
		if (beans == null || beanManager == null || beans.get(getViewId()) == null) {
			return;
		}
		
		for (Bean<?> bean : beans.get(getViewId())) {
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
		}
	}
	
	static void init(BeanManager beanManager, Map<String, List<Bean<?>>> beans) {
		InstantiateEagerBeansListener.beanManager = beanManager;
		InstantiateEagerBeansListener.beans = unmodifiableMap(beans);
	}
	
}
