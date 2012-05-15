/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.component.output;

import static org.omnifaces.util.Faces.*;
import static org.omnifaces.util.Utils.*;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.omnifaces.util.Components;
import org.omnifaces.util.Faces;

/**
 * <strong>OutputLabel</strong> is a component that extends the standard {@link HtmlOutputLabel} and provides support for
 * automatically setting its value as the label of the component identified by its <code>for</code> attribute (if any).
 * <p>
 * You can use it the same way as <code>&lt;h:outputLabel&gt;</code>, you only need to change <code>h:</code> into
 * <code>o:</code>.
 *
 * @author Arjan Tijms
 */
@FacesComponent(OutputLabel.COMPONENT_TYPE)
public class OutputLabel extends HtmlOutputLabel implements SystemEventListener {

    public static final String COMPONENT_TYPE = "org.omnifaces.component.output.OutputLabel";

    private static final String ERROR_FOR_COMPONENT_NOT_FOUND =
		"A component with Id '%s' as specified by the for attribute of the OutputLabel with Id '%s' could not be found.";

    public OutputLabel() {
        if (!isPostback()) {
            Faces.getViewRoot().subscribeToViewEvent(PreRenderViewEvent.class, this);
        }
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return source instanceof UIViewRoot;
    }

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        if (!isPostback()) {
            String forValue = (String) getAttributes().get("for");
            if (!isEmpty(forValue)) {
                UIComponent forComponent = Components.findComponentRelatively(this, forValue);

                if (forComponent == null) {
                	throw new IllegalArgumentException(String.format(ERROR_FOR_COMPONENT_NOT_FOUND, forValue, this.getId()));
                }

                // To be sure, check if the target component doesn't have a label already. This
                // is unlikely, since otherwise people have no need to use this outputLabel component
                // but check to be sure.
                if (Components.getOptionalLabel(forComponent) == null) {
                    ValueExpression valueExpression = getValueExpression("value");
                    if (valueExpression != null) {
                        forComponent.setValueExpression("label", valueExpression);
                    } else {
                        forComponent.getAttributes().put("label", getValue());
                    }
                }
            }
        }

    }
}
