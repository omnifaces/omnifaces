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

import static java.lang.System.nanoTime;
import static org.omnifaces.util.Messages.addGlobalInfo;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Future;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class SocketITBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject @Push
	private PushContext applicationScopedServerEvent;

	@Inject @Push
	private PushContext sessionScopedUserTargeted;

	@Inject @Push
	private PushContext viewScopedAjaxAware;

	private String ajaxAwareMessage;

	public void pushApplicationScopedServerEvent() {
		String timestamp = String.valueOf(nanoTime());
		Set<Future<Void>> sent = applicationScopedServerEvent.send(timestamp);
		addGlobalInfo("{0},{1}", sent.size(), timestamp);
	}

	public void pushSessionScopedUserTargeted(String recipientUser) {
		String timestamp = String.valueOf(nanoTime());
		Set<Future<Void>> sent = sessionScopedUserTargeted.send(timestamp, recipientUser);
		addGlobalInfo("{0},{1}", sent.size(), timestamp);
	}

	public void pushViewScopedAjaxAware() {
		ajaxAwareMessage = String.valueOf(nanoTime());
		Set<Future<Void>> sent = viewScopedAjaxAware.send("someAjaxEventName");
		addGlobalInfo("{0},{1}", sent.size(), ajaxAwareMessage);
	}

	public String getAjaxAwareMessage() {
		return ajaxAwareMessage;
	}

}