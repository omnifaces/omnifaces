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

import static java.lang.Boolean.FALSE;
import static org.omnifaces.component.output.Link.PropertyKeys.includeRequestParams;

import java.io.IOException;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.html.HtmlOutcomeTargetLink;
import jakarta.faces.context.FacesContext;

import org.omnifaces.component.ActionURLDecorator;
import org.omnifaces.util.State;

/**
 * <p>
 * The <code>&lt;o:link&gt;</code> is a component that extends the standard <code>&lt;h:link&gt;</code> and allows
 * including the request query string parameters of the current URL into the link's target URL. Standard Faces
 * <code>&lt;h:link&gt;</code> does not include any query string parameters which are not declared as view parameters.
 * This is particularly useful if you expect some state in the target page and don't want to repeat
 * <code>&lt;f|o:param&gt;</code> all over place.
 * <p>
 * You can use it the same way as <code>&lt;h:link&gt;</code>, you only need to change <code>h:</code> to
 * <code>o:</code>.
 *
 * <h2>Include request params</h2>
 * <p>
 * When you want to include request query string parameters of the current URL into the link's target URL, set the
 * <code>includeRequestParams</code> attribute to <code>true</code>.
 * <pre>
 * &lt;o:link value="Go to some page with same query string" outcome="some-page" includeRequestParams="true"&gt;
 * </pre>
 *
 * @since 4.5
 * @author Bauke Scholtz
 * @see ActionURLDecorator
 */
@FacesComponent(Link.COMPONENT_TYPE)
public class Link extends HtmlOutcomeTargetLink {

    // Constants ------------------------------------------------------------------------------------------------------

    /** The component type, which is {@value org.omnifaces.component.output.Link#COMPONENT_TYPE}. */
    public static final String COMPONENT_TYPE = "org.omnifaces.component.output.Link";

    enum PropertyKeys {
        includeRequestParams
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private final State state = new State(getStateHelper());

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        super.encodeBegin(new ActionURLDecorator(context, this, false, isIncludeRequestParams()));
    }

    // Getters/setters ------------------------------------------------------------------------------------------------

    /**
     * Returns whether or not the request parameters should be encoded into the form's action URL.
     * @return Whether or not the request parameters should be encoded into the form's action URL.
     */
    public boolean isIncludeRequestParams() {
        return state.get(includeRequestParams, FALSE);
    }

    /**
     * Set whether or not the request parameters should be encoded into the form's action URL.
     * @param includeRequestParams Whether or not the request parameters should be encoded into the form's action URL.
     */
    public void setIncludeRequestParams(boolean includeRequestParams) {
        state.put(PropertyKeys.includeRequestParams, includeRequestParams);
    }
}
