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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;

import org.omnifaces.eventlistener.DefaultHttpSessionListener;

/**
 * A WebListener that instantiates eager session scoped beans.
 * 
 * @since 1.8
 * @author Arjan Tijms
 *
 */
@WebListener
public class EagerBeansSessionListener extends DefaultHttpSessionListener {
	
	public static final String SESSION_CREATED = "org.omnifaces.eager.SESSION_CREATED";
	
	private static boolean enabled = true;
	private static boolean recordSessionCreated = false;

	@Inject
	private BeansInstantiator eagerBeansRepository;
	
	
	// Methods --------------------------------------------------------------------------------------------------------

	public static void setEnabled(boolean enabled) {
		EagerBeansSessionListener.enabled = enabled;
	}
	
	public static void setRecordSessionCreated(boolean recordSessionCreated) {
		EagerBeansSessionListener.recordSessionCreated = recordSessionCreated;
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		if (eagerBeansRepository != null && enabled) {
			eagerBeansRepository.instantiateSessionScoped();
		}
		
		if (recordSessionCreated) {
			// Record a marker manually. session#isNew not entirely accurate for our purpose
			se.getSession().setAttribute(SESSION_CREATED, new AtomicBoolean(true));
		}
	}
	
}
