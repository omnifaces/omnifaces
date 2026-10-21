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
package org.omnifaces.test.util.events;

import static javax.faces.event.PhaseId.RENDER_RESPONSE;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@Named
@RequestScoped
public class EventsITBean {

	public static final int CALLBACK_COUNT = 20;

	private final List<Integer> invocations = new ArrayList<>();

	public void subscribeToRequestBeforeRenderResponse() {
		for (int i = 0; i < CALLBACK_COUNT; i++) {
			int index = i;
			subscribeToRequestBeforePhase(RENDER_RESPONSE, () -> invocations.add(index));
		}
	}

	public List<Integer> getInvocations() {
		return invocations;
	}

}
