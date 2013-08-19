/*
 * Copyright 2013 OmniFaces.
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
package org.omnifaces.cdi.viewscope;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;

import org.omnifaces.cdi.ViewScoped;

/**
 * Register the CDI view scope context.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see ViewScoped
 * @see ViewScopeContext
 * @see ViewScopeManager
 * @since 1.6
 */
public class ViewScopeExtension implements Extension {


    // Variables ------------------------------------------------------------------------------------------------------

	private Bean<ViewScopeManager> bean;

	// Actions --------------------------------------------------------------------------------------------------------

    /**
     * Obtain an instance of the view scope manager bean.
     * @param event The process managed bean event.
     */
    protected void processManagedBeans(@Observes ProcessManagedBean<ViewScopeManager> event) {
        bean = event.getBean();
    }

	/**
	 * Register a new view scope context, wrapping the given bean manager and view scope manager bean, in the current
	 * CDI context.
	 * @param event The after bean discovery event.
	 * @param bm The bean manager.
	 */
	protected void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager bm) {
		event.addContext(new ViewScopeContext(bm, bean));
	}

}