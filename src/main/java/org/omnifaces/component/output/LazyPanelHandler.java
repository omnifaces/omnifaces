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
package org.omnifaces.component.output;

import static java.util.logging.Level.FINEST;

import java.util.Arrays;
import java.util.logging.Logger;

import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.faces.view.facelets.ComponentConfig;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagAttribute;

import org.omnifaces.event.LazyPanelEvent;

/**
 * A handler for {@link LazyPanel} component, which will take care of setting the <code>listener</code> attribute.
 *
 * @author Bauke Scholtz
 * @since 5.3
 */
public class LazyPanelHandler extends ComponentHandler {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(LazyPanelHandler.class.getName());

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * Construct the tag handler for {@link LazyPanel} component.
     *
     * @param config The component config.
     */
    public LazyPanelHandler(ComponentConfig config) {
        super(config);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Delegate to super and then try to get a {@link MethodExpression} representing a method optionally accepting the {@link LazyPanelEvent} and set it as
     * <code>listener</code> property of current {@link LazyPanel} component.
     */
    @Override
    public void setAttributes(FaceletContext context, Object component) {
        super.setAttributes(context, component);
        var lazyPanel = (LazyPanel) component;
        var attribute = getAttribute("listener");

        if (attribute != null) {
            try {
                lazyPanel.setListener(getAndValidateMethodExpression(context, attribute, new Class[0]));
            }
            catch (Exception ignore) {
                logger.log(
                    FINEST, "Ignoring thrown exception; there is really no clean way to distinguish method expression with or without argument beforehand.",
                    ignore
                );

                try {
                    lazyPanel.setListener(getAndValidateMethodExpression(context, attribute, new Class[] { LazyPanelEvent.class }));
                }
                catch (Exception e) {
                    throw new IllegalArgumentException(
                        "o:lazyPanel listener attribute must be a method expression optionally taking LazyPanelEvent argument", e
                    );
                }
            }
        }
    }

    private static MethodExpression getAndValidateMethodExpression(FaceletContext context, TagAttribute attribute, Class<?>[] arguments) {
        var methodExpression = attribute.getMethodExpression(context, Object.class, arguments);

        if (!Arrays.equals(arguments, methodExpression.getMethodInfo(context).getParamTypes())) {
            throw new MethodNotFoundException();
        }

        return methodExpression;
    }

}
