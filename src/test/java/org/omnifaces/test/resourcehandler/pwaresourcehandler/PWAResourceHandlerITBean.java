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
package org.omnifaces.test.resourcehandler.pwaresourcehandler;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class PWAResourceHandlerITBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final AtomicInteger INSTANCES = new AtomicInteger(); // #707

    @PostConstruct
    public void init() {
        INSTANCES.incrementAndGet();
    }

    public void submit() {
        // NOOP
    }

    public int getInstances() {
        return INSTANCES.get();
    }

}