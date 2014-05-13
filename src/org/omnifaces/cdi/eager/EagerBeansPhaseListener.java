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

import static javax.faces.event.PhaseId.RESTORE_VIEW;
import static org.omnifaces.util.Faces.getViewId;

import javax.faces.event.PhaseEvent;

import org.omnifaces.config.BeanManager;
import org.omnifaces.eventlistener.DefaultPhaseListener;

/**
 * A PhaseListener that instantiates eager request scoped beans by JSF view ID.
 * <p>
 * This instantiates beans relatively late during request processing but at a point
 * that JSF and the view root corresponding to the current view id are available to the
 * bean.
 *
 * @since 1.8
 * @author Arjan Tijms
 *
 */
public class EagerBeansPhaseListener extends DefaultPhaseListener {

	private static final long serialVersionUID = -7252366571645029385L;

	public EagerBeansPhaseListener() {
		super(RESTORE_VIEW);
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		BeansInstantiator eagerBeansRepository = BeanManager.INSTANCE.getReference(BeansInstantiator.class);

		if (eagerBeansRepository != null) {
			eagerBeansRepository.instantiateByViewID(getViewId());
		}
	}

}