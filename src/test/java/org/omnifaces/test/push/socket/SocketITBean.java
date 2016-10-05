/*
 * Copyright 2016 OmniFaces
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
package org.omnifaces.test.push.socket;

import static java.lang.System.nanoTime;
import static org.omnifaces.util.Messages.addGlobalInfo;

import java.util.Set;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

@Named
@RequestScoped
public class SocketITBean {

	@Inject @Push
	private PushContext applicationScoped;

	@Inject @Push
	private PushContext sessionScoped;

	@Inject @Push
	private PushContext viewScoped;

	@Inject @Push
	private PushContext userTargeted;

	public void pushApplicationScoped() {
		String timestamp = String.valueOf(nanoTime());
		Set<Future<Void>> sent = applicationScoped.send(timestamp);
		addGlobalInfo("{0},{1}", sent.size(), timestamp);
	}

	public void pushSessionScoped() {
		String timestamp = String.valueOf(nanoTime());
		Set<Future<Void>> sent = sessionScoped.send(timestamp);
		addGlobalInfo("{0},{1}", sent.size(), timestamp);
	}

	public void pushViewScoped() {
		String timestamp = String.valueOf(nanoTime());
		Set<Future<Void>> sent = viewScoped.send(timestamp);
		addGlobalInfo("{0},{1}", sent.size(), timestamp);
	}

	public void pushUserTargeted(String recipientUser) {
		String timestamp = String.valueOf(nanoTime());
		Set<Future<Void>> sent = userTargeted.send(timestamp, recipientUser);
		addGlobalInfo("{0},{1}", sent.size(), timestamp);
	}

}