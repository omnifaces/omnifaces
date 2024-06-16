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
package org.omnifaces.test.exceptionhandler.fullajaxexceptionhandler;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named
@RequestScoped
public class FullAjaxExceptionHandlerITBean {

    public void throwDuringInvokeApplication() {
        throw new RuntimeException("throwDuringInvokeApplication");
    }

    public Object getThrowDuringUpdateModelValues() {
        return null;
    }

    public Object setThrowDuringUpdateModelValues(@SuppressWarnings("unused") Object input) {
        throw new RuntimeException("throwDuringUpdateModelValues");
    }

    public Object getThrowDuringRenderResponse() {
        throw new RuntimeException("throwDuringRenderResponse");
    }

}