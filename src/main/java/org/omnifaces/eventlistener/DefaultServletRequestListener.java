/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.eventlistener;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;


/**
 * Default implementation for the ServletRequestListener interface.
 *
 * @author Arjan Tijms
 * @since 1.8
 */
public abstract class DefaultServletRequestListener implements ServletRequestListener {
	
	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		// NOOP
	}
	
	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		// NOOP
	}

}
