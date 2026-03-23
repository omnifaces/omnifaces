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
package org.omnifaces.test.push.sse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.cdi.push.SseEvent;
import org.omnifaces.cdi.push.SseEvent.Closed;
import org.omnifaces.cdi.push.SseEvent.Opened;

@ApplicationScoped
public class SseITObserver {

    @Inject @Push(sse=true)
    private PushContext applicationScopedServerEvent;

    private volatile String closedChannels = "";

    public void onopen(@Observes @Opened SseEvent event) {
        String channel = event.getChannel();
        applicationScopedServerEvent.send("opened:" + channel);
    }

    public void onclose(@Observes @Closed SseEvent event) {
        String channel = event.getChannel();
        closedChannels += "|" + channel + "|";
        applicationScopedServerEvent.send("closed:" + channel);
    }

    public String getClosedChannels() {
        return closedChannels;
    }
}
