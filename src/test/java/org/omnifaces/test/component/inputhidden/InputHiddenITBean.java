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

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@Named
@RequestScoped
public class InputHiddenITBean {

    private boolean toggled;

    public void toggle() {
        this.toggled = !toggled;
    }

    public void submit() {
        addGlobalInfo("submitted");
    }

    public void setToggled(boolean toggled) {
		this.toggled = toggled;
	}

    public boolean isToggled() {
        return toggled;
    }

}