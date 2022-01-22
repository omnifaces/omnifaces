/*
 * Copyright OmniFaces
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
package org.omnifaces.test.push.socket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.push.SocketEvent;
import org.omnifaces.cdi.push.SocketEvent.Closed;
import org.omnifaces.cdi.push.SocketEvent.Opened;

@ApplicationScoped
public class SocketITObserver {

	@Inject @Push
	private PushContext applicationScopedServerEvent;

	public void onopen(@Observes @Opened SocketEvent event) {
		String channel = event.getChannel();

		if (!"applicationScopedServerEvent".equals(channel)) { // Comes too late in Payara. Not really relevant anyway.
			applicationScopedServerEvent.send("opened:" + channel);
		}
	}

	public void onclose(@Observes @Closed SocketEvent event) {
		String channel = event.getChannel();

		if (!"applicationScopedServerEvent".equals(channel)) { // Comes too late in Payara. Not really relevant anyway.
			applicationScopedServerEvent.send("closed:" + channel);
		}
	}

}