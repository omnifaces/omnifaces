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
package org.omnifaces.test.component.lazypanel;

import java.io.Serializable;

import jakarta.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.event.LazyPanelEvent;
import org.omnifaces.util.Faces;

@Named
@ViewScoped
public class LazyPanelITBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private int triggerCount;
    private String lastTriggeredClientId;
    private String receivedProductId;
    private String receivedFilter;
    private String receivedDisabledParam;
    private String receivedEmptyParam;

    public void onload(LazyPanelEvent event) {
        triggerCount++;
        lastTriggeredClientId = event.getClientId();
    }

    public void onloadNoArgs() {
        triggerCount++;
        lastTriggeredClientId = "N/A";
    }

    public void onloadWithParams(LazyPanelEvent event) {
        triggerCount++;
        lastTriggeredClientId = event.getClientId();
        receivedProductId = Faces.getRequestParameter("productId");
        receivedFilter = Faces.getRequestParameter("filter");
        receivedDisabledParam = Faces.getRequestParameter("disabledParam");
        receivedEmptyParam = Faces.getRequestParameter("emptyParam");
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public String getLastTriggeredClientId() {
        return lastTriggeredClientId;
    }

    public String getReceivedProductId() {
        return receivedProductId;
    }

    public String getReceivedFilter() {
        return receivedFilter;
    }

    public String getReceivedDisabledParam() {
        return receivedDisabledParam;
    }

    public String getReceivedEmptyParam() {
        return receivedEmptyParam;
    }

}
