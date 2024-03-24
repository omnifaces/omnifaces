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
package org.omnifaces.validator;

import java.util.Objects;

import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.Validator;

/**
 * <p>
 * By default, Faces validators run on every request, regardless of whether the submitted value has changed or not. In
 * case of validation against the DB on complex objects which are already stored in the model in a broader scope, such
 * as the view scope, this may result in unnecessarily expensive service/DAO calls. In such case, you'd like to perform
 * the expensive service/DAO call only when the submitted value is really changed as compared to the model value.
 * <p>
 * This validator offers you a template to do it transparently. To use it, just change your validators from:
 * <pre>
 * public class YourValidator implements Validator&lt;YourEntity&gt; {
 *
 *     public void validate(FacesContext context, UIComponent component, YourEntity submittedValue) {
 *         // ...
 *     }
 *
 * }
 * </pre>
 * <p>to
 * <pre>
 * public class YourValidator extends ValueChangeValidator&lt;YourEntity&gt; {
 *
 *     public void validateChangedObject(FacesContext context, UIComponent component, YourEntity submittedValue) {
 *         // ...
 *     }
 *
 * }
 * </pre>
 * So, essentially, just replace <code>implements Validator</code> by <code>extends ValueChangeValidator</code> and
 * rename the method from <code>validate</code> to <code>validateChangedObject</code>.
 *
 * @author Juliano
 * @author Bauke Scholtz
 * @since 1.7
 */
public abstract class ValueChangeValidator<T> implements Validator<T> {

    /**
     * If the component is an instance of {@link EditableValueHolder} and its old object value is equal to the
     * submitted value, then return immediately from the method and don't perform any validation. Otherwise, invoke
     * {@link #validateChangedObject(FacesContext, UIComponent, Object)} which may in turn do the necessary possibly
     * expensive DAO operations.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void validate(FacesContext context, UIComponent component, T submittedValue) {
        if (component instanceof EditableValueHolder) {
            T newValue = submittedValue;
            T oldValue = (T) ((EditableValueHolder) component).getValue();

            if (Objects.equals(newValue, oldValue)) {
                return;
            }
        }

        validateChangedObject(context, component, submittedValue);
    }

    /**
     * Use this method instead of {@link #validate(FacesContext, UIComponent, Object)} if you intend to perform the
     * validation only when the submitted value is really changed as compared to the model value.
     * @param context The involved faces context.
     * @param component The involved UI component.
     * @param submittedValue The submitted value.
     */
    public abstract void validateChangedObject(FacesContext context, UIComponent component, T submittedValue);

}