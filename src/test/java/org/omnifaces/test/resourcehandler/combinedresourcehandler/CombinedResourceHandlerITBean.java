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
package org.omnifaces.test.resourcehandler.combinedresourcehandler;

import java.io.Serializable;

import jakarta.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Hacks;

@Named
@ViewScoped // @RequestScoped was been sufficient, this is however explicitly @ViewScoped in order to also test #457.
public class CombinedResourceHandlerITBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public void rebuild() {
        Faces.setViewRoot(Faces.getViewId());
    }

    public boolean isFacesJsAvailable() {
        return Hacks.isFacesScriptResourceAvailable();
    }

}