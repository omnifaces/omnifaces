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

import static java.lang.String.format;
import static org.omnifaces.util.ComponentsLocal.getLabel;
import static org.omnifaces.util.FacesLocal.getMessageBundle;
import static org.omnifaces.util.Messages.createError;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UISelectBoolean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 * <p>
 * The <code>omnifaces.RequiredCheckboxValidator</code> is intented to solve a peculiar problem with
 * <code>required="true"</code> attribute of {@link UISelectBoolean} components like
 * <code>&lt;h:selectBooleanCheckbox&gt;</code>. If you want to require the user to tick the desired checkbox, you would
 * expect that setting <code>required="true"</code> is sufficient. But it is not, the validation wil always pass.
 * <p>
 * As for every other {@link UIInput} component the default <code>required="true"</code> validator would
 * only check if the value is actually filled and been sent to the server side, i.e. the value is not null nor empty.
 * In case of a <code>&lt;h:selectBooleanCheckbox&gt;</code>, which accepts <code>Boolean</code> or <code>boolean</code>
 * properties only, EL will coerce the unchecked value to <code>Boolean.FALSE</code> during apply request values phase
 * right before validations phase. This value is not <code>null</code> nor empty! Thus, the required attribute of the
 * <code>&lt;h:selectBooleanCheckbox&gt;</code> is fairly pointless. It would always pass the validation and thus never
 * display the desired required message in case of an unticked checkbox.
 *
 * <h2>Usage</h2>
 * <p>
 * This validator is available by validator ID <code>omnifaces.RequiredCheckboxValidator</code>. Just specify it as
 * <code>&lt;f:validator&gt;</code> of the boolean selection component:
 * <pre>
 * &lt;h:selectBooleanCheckbox id="agree" value="#{bean.agree}" requiredMessage="You must agree!"&gt;
 *     &lt;f:validator validatorId="omnifaces.RequiredCheckboxValidator" /&gt;
 * &lt;/h:selectBooleanCheckbox&gt;
 * </pre>
 * <p>
 * Since OmniFaces 4.5 it's also available by <code>&lt;o:requiredCheckboxValidator&gt;</code> tag.
 * <pre>
 * &lt;h:selectBooleanCheckbox id="agree" value="#{bean.agree}" requiredMessage="You must agree!"&gt;
 *     &lt;o:requiredCheckboxValidator" /&gt;
 * &lt;/h:selectBooleanCheckbox&gt;
 * </pre>
 * <p>
 * The validator will use the message as specified in <code>requiredMessage</code>. If it's absent, then it will use
 * the default required message as specified in custom <code>&lt;message-bundle&gt;</code> in
 * <code>faces-config.xml</code>. If it's absent, then it will default to
 * <blockquote>{0}: a tick is required"</blockquote>
 *
 * @author Bauke Scholtz
 */
@FacesValidator("omnifaces.RequiredCheckboxValidator")
public class RequiredCheckboxValidator implements Validator<Boolean> {

    // Constants ------------------------------------------------------------------------------------------------------

    private static final String DEFAULT_REQUIRED_MESSAGE = "{0}: a tick is required";

    private static final String ERROR_WRONG_COMPONENT = "RequiredCheckboxValidator must be registered on a component"
        + " of type UISelectBoolean. Encountered component of type '%s'.";

    // Actions --------------------------------------------------------------------------------------------------------

    @Override
    public void validate(FacesContext context, UIComponent component, Boolean value) {
        if (!(component instanceof UISelectBoolean)) {
            throw new IllegalArgumentException(format(ERROR_WRONG_COMPONENT, component.getClass().getName()));
        }

        if (!Boolean.TRUE.equals(value)) {
            var requiredMessage = ((UIInput) component).getRequiredMessage();

            if (requiredMessage == null) {
                var messageBundle = getMessageBundle(context);

                if (messageBundle != null) {
                    requiredMessage = messageBundle.getString(UIInput.REQUIRED_MESSAGE_ID);
                }
            }

            if (requiredMessage == null) {
                requiredMessage = DEFAULT_REQUIRED_MESSAGE;
            }

            throw new ValidatorException(createError(requiredMessage, getLabel(context, component)));
        }
    }

}