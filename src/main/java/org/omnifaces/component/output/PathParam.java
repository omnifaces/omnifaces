/*
 * Copyright 2017 OmniFaces
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

import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * The <code>&lt;o:pathParam&gt;</code> is a component that extends the OmniFaces {@link Param} to support 
 * <code>MultiViews</code> feature of {@link org.omnifaces.facesviews.FacesViews} by rendering the supplied parameters of such components 
 * as <code>&lt;o:link&gt;</code> and <code>&lt;o:button&gt;</code> as path parameters and not as query parameters 
 * as otherwise will be produced by <code>&lt;o:param&gt;</code> and <code>&lt;f:param&gt;</code>. 
 * The component supports {@link Converter} to convert the supplied value to string, if necessary.
 * <p>
 * The component has built-in support for a {@link Converter} by usual means via the <code>converter</code> 
 * attribute of the tag, or the nested <code>&lt;f:converter&gt;</code> tag, or just automatically if a converter is 
 * already registered for the target class via <code>@FacesConverter(forClass)</code>.
 * <p>
 * It can be also be used in a same way as <code>&lt;f:param&gt;</code> if it is either declared as a child of 
 * standard components like <code>&lt;h:link&gt;</code> and <code>&lt;h:button&gt;</code> or when it is a child of 
 * <code>&lt;o:link&gt;</code> and <code>&lt;o:button&gt;</code> and its <code>basic</code> attribute is set to true.
 * <p>
 * This component doesn't support returning encoded output of children as parameter value in case no value is present and 
 * this feature is provided by {@link Param} component.
 * <p>
 * The component specifies two additional non-required attributes to the ones defined in <code>&lt;o:param&gt;</code>
 * to fine-tune rendering of path parameters:
 * <ul>
 * <li>
 * <code>basic</code>, non-required attribute that defines whether the component must be rendered as path parameter (false) 
 * or query parameter (true), default value - false.
 * </li>
 * <li>
 * <code>index</code>, non-required attribute that defines relative rendering location of the component.
 * </li>
 * </ul>
 * <p>
 * Rendering of this component is done by evaluating the <code>value</code> attribute of the component if it 
 * must be rendered as a path parameter and traditionally by evaluating the <code>name</code> and <code>value</code> 
 * attributes of the component if it must be rendered as a query parameter.
 * <p>
 * Rendering location can be specified explicitly by defining <code>index</code> attribute and components with equal 
 * value of this attribute will be rendered in the order they were defined. If this attribute is left unspecified then 
 * all of the components with unspecified <code>index</code> will be rendered in the order they were defined and by 
 * the mechanism specified by <code>location</code> attribute of <code>&lt;o:link&gt;</code> and 
 * <code>&lt;o:button&gt;</code>, or after all of the other parameters with specified indices by default. 
 * Location only specifies relative context of rendering, i.e.: two parameters with indices "1" and "3" will be rendered 
 * in exactly the same way as two parameters with indices "5" and "7" if no other parameters are present. Negative
 * values are also supported.
 * <p>
 * Example of its usage follows. The first parameter must be rendered as a path parameter at a relative location, "1", 
 * the second parameter will be rendered after all other path parameters (default), the third parameter will be rendered 
 * as a query parameter with the specified name and the fourth parameter won't be rendered at all, as no name was set.
 * <pre>
 * &lt;o:link value="Link" outcome="multiview-supported-path"&gt;
 *     &lt;o:pathParam&gt; value="first" index="1"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; value="no-index"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; name="basic" value="basic" basic="true"&lt;/o:pathParam&gt;
 *     &lt;o:pathParam&gt; value="not-rendered" basic="true"&lt;/o:pathParam&gt;
 * &lt;/o:link&gt;
 * </pre>
 * The code above will be rendered, assuming default settings, as: 
 * <code>&lt;a id="..." name="..." href="/context-path/multiview-supported-path/first/no-index?basic=basic"&gt;Link&lt;/a&gt;</code>
 * 
 * @author Sergey Kuntsel
 * @since 3.0
 * @see Link
 * @see Button
 */
@FacesComponent(PathParam.COMPONENT_TYPE)
public class PathParam extends Param {

    // Constants
    public static final String COMPONENT_TYPE = "org.omnifaces.component.output.PathParam";

    // Properties
    private enum PropertyKeys {
        basic, // treat path parameter as standard parameter
        index; // index to render path parameter at
    }

    /**
     * Return basic property that defines whether the component must be rendered as path parameter (false) 
     * or query parameter (true), default value - false.
     * 
     * @return basic property.
     */
    public Boolean getBasic() { return (Boolean)getStateHelper().eval(PropertyKeys.basic, Boolean.FALSE); }
    
    /**
     * Set basic property that defines whether the component must be rendered as path parameter (false) 
     * or query parameter (true), default value - false.
     * 
     * @param basic property value to set.
     */
    public void setBasic(Boolean basic) { getStateHelper().put(PropertyKeys.basic, basic); }

    /**
     * Return index property that defines relative rendering location of the component.
     * 
     * @return index property.
     */
    public Integer getIndex() { return (Integer)getStateHelper().eval(PropertyKeys.index); }

    /**
     * Set index property that defines relative rendering location of the component.
     * 
     * @param index property value to set.
     */
    public void setIndex(Integer index) { getStateHelper().put(PropertyKeys.index, index); }

    // Actions
    @Override
    public Object getValue() {
        FacesContext context = getFacesContext();
        Converter converter = getConverter();
        Object value = getLocalValue();

        if (converter == null && value != null) {
            converter = context.getApplication().createConverter(value.getClass());
        }

        if (converter != null) {
            return converter.getAsString(context, this, value);
        } else {
            return value;
        }
    }

}
