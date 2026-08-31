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

import java.util.Set;
import java.util.concurrent.Future;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

@Named
@RequestScoped
public class SocketTransientViewITBean {

    @Inject
    @Push
    private PushContext transientViewApplicationScoped;

    public void push() {
        String timestamp = String.valueOf(nanoTime());
        Set<Future<Void>> sent = transientViewApplicationScoped.send(timestamp);
        addGlobalInfo("{0},{1}", sent.size(), timestamp);
    }

}
