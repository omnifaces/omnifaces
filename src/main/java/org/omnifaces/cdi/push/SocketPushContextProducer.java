/*
 * Copyright 2016 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.cdi.push;

import static org.omnifaces.util.Beans.getQualifier;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

/**
 * <p>
 * Producer for injecting a {@link PushContext} as defined by the {@link Push} annotation.
 *
 * @author Bauke Scholtz
 * @see Push
 * @since 2.3
 */
@Dependent
public class SocketPushContextProducer {

	// Variables ------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unused") // Workaround for OpenWebBeans not properly passing it as produce() method argument.
	@Inject
	private InjectionPoint injectionPoint;

	@Inject
	private SocketChannelManager manager;

	// Actions --------------------------------------------------------------------------------------------------------

	@Produces
	@Push
	public PushContext produce(InjectionPoint injectionPoint) {
		Push push = getQualifier(injectionPoint, Push.class);
		String channel = push.channel().isEmpty() ? injectionPoint.getMember().getName() : push.channel();
		return new SocketPushContext(channel, manager);
	}

}