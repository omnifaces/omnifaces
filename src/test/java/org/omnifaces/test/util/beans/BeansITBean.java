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
package org.omnifaces.test.util.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;

import org.omnifaces.util.Beans;

@Named
@ApplicationScoped
public class BeansITBean {

    private ClassLoader contextClassLoader;
    private BeanManager manager;

    public boolean isContextClassLoaderStable() {
        var currentContextClassLoader = Thread.currentThread().getContextClassLoader();

        if (contextClassLoader == null) {
            contextClassLoader = currentContextClassLoader;
        }

        return contextClassLoader == currentContextClassLoader;
    }

    public boolean isManagerRemembered() {
        var currentManager = Beans.getManager();

        if (manager == null) {
            manager = currentManager;
        }

        return manager == currentManager;
    }

}
