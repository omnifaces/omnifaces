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
package org.omnifaces.test.cdi.viewscoped.predestroydependency;

import static org.omnifaces.util.Faces.getViewId;
import static org.omnifaces.util.Faces.setViewRoot;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class ViewScopedPreDestroyDependencyITBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Set<String> DEPENDENCY_IDS_SEEN_ON_DESTROY = ConcurrentHashMap.newKeySet();

    @Inject
    private ViewScopedPreDestroyDependencyITDependency dependency;

    public void navigate() {
        setViewRoot(getViewId());
    }

    @PreDestroy
    public void destroy() {
        DEPENDENCY_IDS_SEEN_ON_DESTROY.add(dependency.getId());
    }

    public String getDependencyId() {
        return dependency.getId();
    }

    public int getDependencyCreatedCount() {
        return ViewScopedPreDestroyDependencyITDependency.getCreatedCount();
    }

    public String getDependencyIdsSeenOnDestroy() {
        return String.join(" ", DEPENDENCY_IDS_SEEN_ON_DESTROY);
    }

}
