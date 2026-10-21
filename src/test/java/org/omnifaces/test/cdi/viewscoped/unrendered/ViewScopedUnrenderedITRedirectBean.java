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
package org.omnifaces.test.cdi.viewscoped.unrendered;

import static org.omnifaces.util.Faces.redirect;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.cdi.ViewScoped;

/**
 * Redirects in a view action, which is invoked before the render response. Its pre destroy uses a sibling view scoped bean, so that the test can assert that
 * the bean storage still resolves while its beans are destroyed.
 */
@Named
@ViewScoped
public class ViewScopedUnrenderedITRedirectBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ViewScopedUnrenderedITProbe probe;

    @Inject
    private ViewScopedUnrenderedITSiblingBean sibling;

    @PostConstruct
    public void init() {
        probe.create("viewAction");
    }

    @PreDestroy
    public void destroy() {
        sibling.getId();
        probe.destroy("viewAction");
    }

    public void redirectAway() {
        sibling.getId();
        redirect("ViewScopedUnrenderedIT.xhtml");
    }

}
