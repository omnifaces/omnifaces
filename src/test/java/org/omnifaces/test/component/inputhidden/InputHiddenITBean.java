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
package org.omnifaces.test.component.inputhidden;

import static org.omnifaces.util.Messages.addGlobalInfo;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named
@RequestScoped
public class InputHiddenITBean {

    private boolean toggled;
    private String readonly = "readonly";

    public void toggle() {
        this.toggled = !toggled;
    }

    public void submit() {
        addGlobalInfo("submitted");
    }

    public void validate(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIComponent component, Object value) {
        addGlobalInfo("validated " + value);
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isToggled() {
        return toggled;
    }

    public String getReadonly() { // No setter!
        return readonly;
    }

}