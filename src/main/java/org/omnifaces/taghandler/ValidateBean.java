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
package org.omnifaces.taghandler;

import static jakarta.faces.component.visit.VisitHint.SKIP_UNRENDERED;
import static jakarta.faces.event.PhaseId.PROCESS_VALIDATIONS;
import static jakarta.faces.event.PhaseId.RESTORE_VIEW;
import static jakarta.faces.event.PhaseId.UPDATE_MODEL_VALUES;
import static jakarta.faces.view.facelets.ComponentHandler.isNew;
import static java.text.MessageFormat.format;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.omnifaces.el.ExpressionInspector.getValueReference;
import static org.omnifaces.util.Beans.unwrapIfNecessary;
import static org.omnifaces.util.Components.VALUE_ATTRIBUTE;
import static org.omnifaces.util.Components.getClosestParent;
import static org.omnifaces.util.ComponentsLocal.forEachComponent;
import static org.omnifaces.util.ComponentsLocal.getCurrentForm;
import static org.omnifaces.util.ComponentsLocal.getLabel;
import static org.omnifaces.util.ComponentsLocal.hasInvokedSubmit;
import static org.omnifaces.util.Events.subscribeToRequestAfterPhase;
import static org.omnifaces.util.Events.subscribeToRequestBeforePhase;
import static org.omnifaces.util.Events.subscribeToViewEvent;
import static org.omnifaces.util.Facelets.getBoolean;
import static org.omnifaces.util.Facelets.getString;
import static org.omnifaces.util.Facelets.getValueExpression;
import static org.omnifaces.util.Faces.getELContext;
import static org.omnifaces.util.Faces.getMessageBundle;
import static org.omnifaces.util.Faces.renderResponse;
import static org.omnifaces.util.Faces.validationFailed;
import static org.omnifaces.util.FacesLocal.evaluateExpressionGet;
import static org.omnifaces.util.FacesLocal.isDevelopment;
import static org.omnifaces.util.Messages.addError;
import static org.omnifaces.util.Messages.addGlobalError;
import static org.omnifaces.util.Reflection.getBaseBeanPropertyPaths;
import static org.omnifaces.util.Reflection.instance;
import static org.omnifaces.util.Reflection.setBeanProperties;
import static org.omnifaces.util.Reflection.toClass;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.csvToList;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Validators.resolveViolatedBasesAndProperties;
import static org.omnifaces.util.Validators.validateBean;

import java.beans.Introspector;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PostValidateEvent;
import jakarta.faces.event.PreValidateEvent;
import jakarta.faces.validator.BeanValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.TagConfig;
import jakarta.faces.view.facelets.TagHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;

import org.omnifaces.eventlistener.BeanValidationEventListener;
import org.omnifaces.util.Reflection.PropertyPath;
import org.omnifaces.util.copier.CloneCopier;
import org.omnifaces.util.copier.Copier;
import org.omnifaces.util.copier.CopyCtorCopier;
import org.omnifaces.util.copier.MultiStrategyCopier;
import org.omnifaces.util.copier.NewInstanceCopier;
import org.omnifaces.util.copier.SerializationCopier;

/**
 * <p>
 * The <code>&lt;o:validateBean&gt;</code> allows the developer to control bean validation on a per-{@link UICommand}
 * or {@link UIInput} component basis, as well as validating a given bean at the class level.
 *
 * <p>
 * The standard <code>&lt;f:validateBean&gt;</code> only allows validation control on a per-form
 * or a per-request basis (by using multiple tags and conditional EL expressions in its attributes) which may end up in
 * boilerplate code.
 *
 * <p>
 * The standard <code>&lt;f:validateBean&gt;</code> also, despite its name, does not actually have any facilities to
 * validate a bean at all.
 *
 * <h2>Usage</h2>
 * <p>
 * Some examples
 *
 * <p>
 * <b>Control bean validation per component</b>
 * <pre>
 * &lt;h:commandButton value="submit" action="#{bean.submit}"&gt;
 *     &lt;o:validateBean validationGroups="jakarta.validation.groups.Default,com.example.MyGroup" /&gt;
 * &lt;/h:commandButton&gt;
 * </pre>
 * <pre>
 * &lt;h:selectOneMenu value="#{bean.selectedItem}"&gt;
 *     &lt;f:selectItems value="#{bean.availableItems}" /&gt;
 *     &lt;o:validateBean disabled="true" /&gt;
 *     &lt;f:ajax execute="@form" listener="#{bean.itemChanged}" render="@form" /&gt;
 * &lt;/h:selectOneMenu&gt;
 * </pre>
 *
 * <p>
 * <b>Validate a bean at the class level</b>
 * <pre>
 * &lt;h:inputText value="#{bean.product.item}" /&gt;
 * &lt;h:inputText value="#{bean.product.order}" /&gt;
 *
 * &lt;o:validateBean value="#{bean.product}" /&gt;
 * </pre>
 *
 * <p>
 * <b>Since OmniFaces 3.8, nested properties are also supported with <code>@jakarta.validation.Valid</code> cascade</b>
 * <pre>
 * &lt;h:inputText value="#{bean.product.item}" /&gt;
 * &lt;h:inputText value="#{bean.product.order}" /&gt;
 *
 * &lt;o:validateBean value="#{bean}" /&gt;
 * </pre>
 * <p>
 * Whereby the <code>product</code> property looks like this:</p>
 * <pre>
 * &#64;Valid
 * private Product product;
 * </pre>
 *
 * <p>
 * When using <code>&lt;o:validateBean method="validateCopy" /&gt;</code> (which is the default), then only beans, lists,
 * maps and arrays are considered as nested properties and the copied bean will be autopopulated with defaults. If this
 * fails, then consider creating a custom copier as instructed in next section.
 *
 * <h2>Class level validation details</h2>
 * <p>
 * In order to validate a bean at the class level, all values from input components should first be actually set on that bean
 * and only thereafter should the bean be validated. This however does not play well with the Faces approach where a model
 * is only updated when validation passes. But for class level validation we seemingly can not validate until the model
 * is updated. To break this tie, a <em>copy</em> of the model bean is made first, and then values are stored in this copy
 * and validated there. If validation passes, the original bean is updated.
 *
 * <p>
 * A bean is copied using the following strategies (in the order indicated):
 * <ol>
 * <li> <b>Cloning</b> - Bean must implement the {@link Cloneable} interface and support cloning according to the rules of that interface. See {@link CloneCopier}
 * <li> <b>Serialization</b> - Bean must implement the {@link Serializable} interface and support serialization according to the rules of that interface. See {@link SerializationCopier}
 * <li> <b>Copy constructor</b> - Bean must have an additional constructor (next to the default constructor) taking a single argument of its own
 *      type that initializes itself with the values of that passed in type. See {@link CopyCtorCopier}
 * <li> <b>New instance</b> - Bean should have a public no arguments (default) constructor. Every official JavaBean satisfies this requirement. Note
 *      that in this case no copy is made of the original bean, but just a new instance is created. See {@link NewInstanceCopier}
 * </ol>
 *
 * <p>
 * If the above order is not ideal, or if an custom copy strategy is needed (e.g. when it's only needed to copy a few fields for the validation)
 * a strategy can be supplied explicitly via the <code>copier</code> attribute. The value of this attribute can be any of the build-in copier implementations
 * given above, or can be a custom implementation of the {@link Copier} interface.
 * <p>
 * If the copying strategy is not possible due to technical limitations, then you could set <code>method</code>
 * attribute to <code>"validateActual"</code>.
 * <pre>
 * &lt;o:validateBean value="#{bean.product}" method="validateActual" /&gt;
 * </pre>
 * <p>
 * This will update the model values and run the validation after update model values phase instead of the validations
 * phase. The disadvantage is that the invalid values remain in the model and that the action method is anyway invoked.
 * You would need an additional check for {@link FacesContext#isValidationFailed()} in the action method to see if it
 * has failed or not.
 *
 * <h2>Faces messages</h2>
 * <p>
 * By default, the faces message is added with client ID of the parent {@link UIForm}.
 * <pre>
 * &lt;h:form id="formId"&gt;
 *     ...
 *     &lt;h:message for="formId" /&gt;
 *     &lt;o:validateBean ... /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * The faces message can also be shown for all invalidated components using <code>showMessageFor="@all"</code>.
 * <pre>
 * &lt;h:form&gt;
 *     &lt;h:inputText id="foo" /&gt;
 *     &lt;h:message for="foo" /&gt;
 *     &lt;h:inputText id="bar" /&gt;
 *     &lt;h:message for="bar" /&gt;
 *     ...
 *     &lt;o:validateBean ... showMessageFor="@all" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * The faces message can also be shown as global message using <code>showMessageFor="@global"</code>.
 * <pre>
 * &lt;h:form&gt;
 *     ...
 *     &lt;o:validateBean ... showMessageFor="@global" /&gt;
 * &lt;/h:form&gt;
 * &lt;h:messages globalOnly="true" /&gt;
 * </pre>
 * <p>
 * The faces message can also be shown for specific components referenced by a space separated collection of their
 * client IDs in <code>showMessageFor</code> attribute.
 * <pre>
 * &lt;h:form&gt;
 *     &lt;h:inputText id="foo" /&gt;
 *     &lt;h:message for="foo" /&gt;
 *     &lt;h:inputText id="bar" /&gt;
 *     &lt;h:message for="bar" /&gt;
 *     ...
 *     &lt;o:validateBean ... showMessageFor="foo bar" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * The faces message can also be shown for components which match {@link jakarta.validation.ConstraintViolation#getPropertyPath() Property
 * Path of the ConstraintViolation} using <code>showMessageFor="@violating"</code>, and when no matching component can
 * be found, the message will fallback to being added with client ID of the parent {@link UIForm}.
 * <pre>
 * &lt;h:form id="formId"&gt;
 *     ...
 *     &lt;!-- Unmatched messages shown here: --&gt;
 *     &lt;h:message for="formId" /&gt;
 *     ...
 *     &lt;h:inputText id="foo" value="#{bean.product.item}" /&gt;
 *
 *     &lt;!-- Messages where ConstraintViolation PropertyPath is "item" are shown here: --&gt;
 *     &lt;h:message for="foo" /&gt;
 *     ...
 *     &lt;o:validateBean ... value="#{bean.product}" showMessageFor="@violating" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * The <code>showMessageFor</code> attribute is new since OmniFaces 2.6 and it defaults to <code>@form</code>. The
 * <code>showMessageFor</code> attribute does by design not have any effect when <code>validateMethod="actual"</code>
 * is used.
 *
 * <h3>Message format</h3>
 * <p>
 * The faces message uses a predefined message format, which corresponds to the value of {@link BeanValidator#MESSAGE_ID}
 * in the message bundle. The default message format of <code>{1}: {0}</code> prepends the labels of all the validated
 * fields. This is useful in the case of validating a single bean property, but sometimes confusing in the case of
 * validating a bean with many properties.
 * <p>
 * In a form containing properties like <i>First Name</i>, <i>Last Name</i>, <i>Address</i>, <i>Zip Code</i>, and
 * <i>Phone Number</i> where at the bean level, at least one of the name fields must be non-null, overriding the message
 * format can help make a more clear error message.
 * <p>
 * This can be done by overriding the {@link BeanValidator#MESSAGE_ID} line in the message bundle:
 * <pre>
 * jakarta.faces.validator.BeanValidator.MESSAGE = Errors encountered: {0}
 * </pre>
 * <p>
 * However, this change affects all bean validation messages site-wide. In case you'd like to fine-tune the bean
 * validation message on a per-<code>&lt;o:validateBean&gt;</code>-basis, then you can since OmniFaces 3.12 use the
 * <code>messageFormat</code> attribute. Any <code>{0}</code> placeholder will be substituted with the error message
 * and any <code>{1}</code> placeholder will be substituted with the labels of all validated fields.
 * <pre>
 * &lt;!-- Displays: "First Name, Last Name, Address, Zip Code, Phone Number: First Name and Last Name cannot both be null" --&gt;
 * &lt;o:validateBean /&gt;
 *
 * &lt;!-- Displays: "Errors encountered: First Name and Last Name cannot both be null" --&gt;
 * &lt;o:validateBean messageFormat="Errors encountered: {0}" /&gt;"
 * </pre>
 *
 * @author Bauke Scholtz
 * @author Arjan Tijms
 * @author Andre Wachsmuth
 * @see BeanValidationEventListener
 */
public class ValidateBean extends TagHandler {

    // Constants ------------------------------------------------------------------------------------------------------


    private static final Logger logger = Logger.getLogger(ValidateBean.class.getName());

    private static final String DEFAULT_SHOWMESSAGEFOR = "@form";
    private static final String ERROR_MISSING_FORM =
        "o:validateBean must be nested in an UIForm.";
    private static final String ERROR_INVALID_PARENT =
        "o:validateBean parent must be an instance of UIInput or UICommand.";
    private static final String WARN_UNDISPLAYED_VIOLATION =
        "o:validateBean could not display violation message '%s' for property path '%s'.";

    // Enums ----------------------------------------------------------------------------------------------------------

    private enum ValidateMethod {
        validateCopy, validateActual;

        public static ValidateMethod of(String name) {
            if (isEmpty(name)) {
                return validateCopy;
            }

            return valueOf(name);
        }
    }

    // Variables ------------------------------------------------------------------------------------------------------

    private ValueExpression value;
    private boolean disabled;
    private ValidateMethod method;
    private String groups;
    private String copier;
    private String showMessageFor;
    private String messageFormat;

    // Constructors ---------------------------------------------------------------------------------------------------

    /**
     * The tag constructor.
     * @param config The tag config.
     */
    public ValidateBean(TagConfig config) {
        super(config);
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * If the parent component has the <code>value</code> attribute or is an instance of {@link UICommand} or
     * {@link UIInput} and is new and we're in the restore view phase of a postback, then delegate to
     * {@link #processValidateBean(FacesContext, UIComponent)}.
     * @throws IllegalArgumentException When the <code>value</code> attribute is absent and the parent component is not
     * an instance of {@link UICommand} or {@link UIInput}.
     */
    @Override
    public void apply(FaceletContext context, UIComponent parent) throws IOException {
        if (getAttribute(VALUE_ATTRIBUTE) == null && !(parent instanceof UICommand) && !(parent instanceof UIInput)) {
            throw new IllegalArgumentException(ERROR_INVALID_PARENT);
        }

        var facesContext = context.getFacesContext();

        if (!isNew(parent) || !facesContext.isPostback() || facesContext.getCurrentPhaseId() != RESTORE_VIEW) {
            return;
        }

        value = getValueExpression(context, getAttribute(VALUE_ATTRIBUTE), Object.class);
        disabled = getBoolean(context, getAttribute("disabled"));
        method = ValidateMethod.of(getString(context, getAttribute("method")));
        groups = getString(context, getAttribute("validationGroups"));
        copier = getString(context, getAttribute("copier"));
        showMessageFor = coalesce(getString(context, getAttribute("showMessageFor")), DEFAULT_SHOWMESSAGEFOR);
        messageFormat = getString(context, getAttribute("messageFormat"));

        // We can't use getCurrentForm() or hasInvokedSubmit() before the component is added to view, because the client ID isn't available.
        // Hence, we subscribe this check to after phase of restore view.
        subscribeToRequestAfterPhase(RESTORE_VIEW, () -> processValidateBean(facesContext, parent));
    }

    /**
     * Check if the given component has participated in submitting the current form or action and if so, then perform
     * the bean validation depending on the attributes set.
     * @param context The involved faces context.
     * @param component The involved component.
     * @throws IllegalStateException When the parent form is missing.
     */
    protected void processValidateBean(FacesContext context, UIComponent component) {
        var form = component instanceof UIForm ? (UIForm) component : getClosestParent(component, UIForm.class);

        if (form == null) {
            throw new IllegalStateException(ERROR_MISSING_FORM);
        }

        if (!form.equals(getCurrentForm(context)) || !(component instanceof UIForm) && !hasInvokedSubmit(context, component)) {
            return;
        }

        Object bean = null;

        if (value != null) {
            var found = new Object[1];
            forEachComponent(context).fromRoot(form).invoke(target -> found[0] = value.getValue(getELContext()));
            bean = found[0];
        }

        if (bean == null) {
            validateForm();
            return;
        }

        if (!disabled) {
            if (method == ValidateMethod.validateActual) {
                validateActualBean(form, bean);
            }
            else {
                validateCopiedBean(form, bean);
            }
        }
    }

    /**
     * After update model values phase, validate actual bean. But don't proceed to render response on fail.
     */
    private void validateActualBean(UIForm form, Object bean) {
        var validateActualBean = new ValidateBeanCallback() { @Override public void invoke() {
            var context = FacesContext.getCurrentInstance();
            validate(context, form, bean, unwrapIfNecessary(bean), new HashSet<>(0), false);
        }};

        subscribeToRequestAfterPhase(UPDATE_MODEL_VALUES, validateActualBean);
    }

    /**
     * Before validations phase of current request, collect all client IDs and bean properties.
     *
     * After validations phase of current request, create a copy of the bean, set all collected properties there,
     * then validate copied bean and proceed to render response on fail.
     */
    private void validateCopiedBean(UIForm form, Object bean) {
        var collectedClientIds = new HashSet<String>();
        var collectedProperties = new HashMap<PropertyPath, Object>();
        var knownBaseProperties = getBaseBeanPropertyPaths(bean, this::isValidAnnotationPresent);
        var collectBeanProperties = new ValidateBeanCallback() { @Override public void invoke() {
            var context = FacesContext.getCurrentInstance();
            forEachInputWithMatchingBase(context, form, knownBaseProperties.keySet(), input -> addCollectingValidator(input, collectedClientIds, collectedProperties, knownBaseProperties));
        }};

        var checkConstraints = new ValidateBeanCallback() { @Override public void invoke() {
            var context = FacesContext.getCurrentInstance();
            forEachInputWithMatchingBase(context, form, knownBaseProperties.keySet(), ValidateBean::removeCollectingValidator);
            var copiedBean = getCopier(context, copier).copy(unwrapIfNecessary(bean));
            setBeanProperties(copiedBean, collectedProperties);
            validate(context, form, bean, copiedBean, collectedClientIds, true);
        }};

        subscribeToRequestBeforePhase(PROCESS_VALIDATIONS, collectBeanProperties);
        subscribeToRequestAfterPhase(PROCESS_VALIDATIONS, checkConstraints);
    }

    /**
     * Returns true if the property associated with given getter method is annotated with {@link Valid}.
     */
    private boolean isValidAnnotationPresent(Method getter) {
        if (getter.isAnnotationPresent(Valid.class)) {
            return true;
        }

        var beanClass = getter.getDeclaringClass();

        if (beanClass.isAnnotationPresent(Valid.class)) {
            return true;
        }

        var propertyName = Introspector.decapitalize(getter.getName().replaceFirst("get", ""));
        var property = stream(beanClass.getDeclaredFields()).filter(field -> field.getName().equals(propertyName)).findFirst().orElse(null);

        if (property == null) {
            return false;
        }

        if (property.isAnnotationPresent(Valid.class)) {
            return true;
        }

        if (property.getAnnotatedType() instanceof AnnotatedParameterizedType) {
            for (var type : ((AnnotatedParameterizedType) property.getAnnotatedType()).getAnnotatedActualTypeArguments()) {
                if (type.isAnnotationPresent(Valid.class)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Before validations phase of current request, subscribe the {@link BeanValidationEventListener} to validate the form based on groups.
     */
    private void validateForm() {
        var validateForm = new ValidateBeanCallback() { @Override public void invoke() {
            var listener = new BeanValidationEventListener(groups, disabled);
            subscribeToViewEvent(PreValidateEvent.class, listener);
            subscribeToViewEvent(PostValidateEvent.class, listener);
        }};

        subscribeToRequestBeforePhase(PROCESS_VALIDATIONS, validateForm);
    }

    @SuppressWarnings({ "rawtypes" })
    private void validate(FacesContext context, UIForm form, Object actualBean, Object validableBean, Set<String> clientIds, boolean renderResponseOnFail) {
        var groupClasses = new ArrayList<Class>();

        for (var group : csvToList(groups)) {
            groupClasses.add(toClass(group));
        }

        var violations = validateBean(validableBean, groupClasses.toArray(new Class[groupClasses.size()]));

        if (!violations.isEmpty()) {
            if ("@violating".equals(showMessageFor)) {
                invalidateInputsByPropertyPathAndShowMessages(context, form, actualBean, violations, messageFormat);
            }
            else if (showMessageFor.charAt(0) != '@') {
                invalidateInputsByShowMessageForAndShowMessages(context, form, violations, showMessageFor, messageFormat);
            }
            else {
                invalidateInputsByClientIdsAndShowMessages(context, form, violations, clientIds, showMessageFor, messageFormat);
            }

            if (context.isValidationFailed() && renderResponseOnFail) {
                context.renderResponse();
            }
        }
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static void forEachInputWithMatchingBase(FacesContext context, UIComponent form, Set<Object> bases, String property, Consumer<UIInput> callback) {
        forEachComponent(context)
            .fromRoot(form)
            .ofTypes(UIInput.class)
            .withHints(SKIP_UNRENDERED/*, SKIP_ITERATION*/) // SKIP_ITERATION fails in Apache EL (Tomcat 8.0.32 tested) but works in Oracle EL.
            .<UIInput>invoke(input -> {
                var valueExpression = input.getValueExpression(VALUE_ATTRIBUTE);

                if (valueExpression != null) {
                    var valueReference = getValueReference(context.getELContext(), valueExpression);
                    var referencedBase = valueReference.getBase();
                    var referencedProperty = valueReference.getProperty();

                    if (bases.contains(referencedBase) && (property == null || property.equals(referencedProperty))) {
                        callback.accept(input);
                    }
                    else if (property == null && referencedBase instanceof List && referencedProperty instanceof Integer) {
                        var referencedItem = ((List<?>) referencedBase).get((Integer) referencedProperty);

                        if (bases.contains(referencedItem)) {
                            callback.accept(input);
                        }
                    }
                }
            });
    }

    private static void forEachInputWithMatchingBase(FacesContext context, UIComponent form, Set<Object> bases, Consumer<UIInput> callback) {
        forEachInputWithMatchingBase(context, form, bases, null, callback);
    }

    private static void addCollectingValidator(UIInput input, Set<String> collectedClientIds, Map<PropertyPath, Object> collectedProperties, Map<Object, PropertyPath> knownBaseProperties) {
        input.addValidator(new CollectingValidator(collectedClientIds, collectedProperties, knownBaseProperties));
    }

    private static void removeCollectingValidator(UIInput input) {
        Validator<?> collectingValidator = null;

        for (var validator : input.getValidators()) {
            if (validator instanceof CollectingValidator) {
                collectingValidator = validator;
                break;
            }
        }

        if (collectingValidator != null) {
            input.removeValidator(collectingValidator);
        }
    }

    private static Copier getCopier(FacesContext context, String copierName) {
        Copier copier = null;

        if (!isEmpty(copierName)) {
            var expressionResult = evaluateExpressionGet(context, copierName);

            if (expressionResult instanceof Copier) {
                copier = (Copier) expressionResult;
            }
            else if (expressionResult instanceof String) {
                copier = instance((String) expressionResult);
            }
        }

        if (copier == null) {
            copier = new MultiStrategyCopier();
        }

        return copier;
    }

    private static void invalidateInputsByPropertyPathAndShowMessages(FacesContext context, UIForm form, Object bean, Set<ConstraintViolation<?>> violations, String messageFormat) {
        var undisplayed = new HashSet<>(violations);

        for (var violation : violations) {
            for (var baseAndProperty : resolveViolatedBasesAndProperties(bean, violation)) {
                boolean[] displayed = { false };

                forEachInputWithMatchingBase(context, form, singleton(baseAndProperty.getKey()), baseAndProperty.getValue(), input -> {
                    context.validationFailed();
                    input.setValid(false);
                    var clientId = input.getClientId(context);
                    addError(clientId, formatMessage(violation.getMessage(), getLabel(context, input), messageFormat));
                    undisplayed.remove(violation);
                    displayed[0] = true;
                });

                if (displayed[0]) {
                    break;
                }
            }
        }

        if (isDevelopment(context)) {
            for (var violation : undisplayed) {
                logger.log(WARNING, String.format(WARN_UNDISPLAYED_VIOLATION, violation.getMessage(), violation.getPropertyPath()));
            }
        }
    }

    private static void invalidateInputsByShowMessageForAndShowMessages(FacesContext context, UIForm form, Set<ConstraintViolation<?>> violations, String showMessageFor, String messageFormat) {
        for (var forId : showMessageFor.split("\\s+")) {
            var component = form.findComponent(forId);
            context.validationFailed();

            if (component instanceof UIInput) {
                ((UIInput) component).setValid(false);
            }

            var clientId = component.getClientId(context);
            addErrors(clientId, violations, getLabel(context, component), messageFormat);
        }
    }

    private static void invalidateInputsByClientIdsAndShowMessages(final FacesContext context, UIForm form, Set<ConstraintViolation<?>> violations, Set<String> clientIds, String showMessageFor, String messageFormat) {
        context.validationFailed();
        var labels = new StringBuilder();

        if (!clientIds.isEmpty()) {
            forEachComponent(context).fromRoot(form).havingIds(clientIds).<UIInput>invoke(input -> {
                input.setValid(false);

                if (labels.length() > 0) {
                    labels.append(", ");
                }

                labels.append(getLabel(context, input));
            });
        }

        showMessages(context, form, violations, clientIds, labels.toString(), showMessageFor, messageFormat);
    }

    private static void showMessages(FacesContext context, UIForm form, Set<ConstraintViolation<?>> violations, Set<String> clientIds, String labels, String showMessagesFor, String messageFormat) {
        if ("@form".equals(showMessagesFor)) {
            var formId = form.getClientId(context);
            addErrors(formId, violations, labels, messageFormat);
        }
        else if ("@all".equals(showMessagesFor)) {
            for (var clientId : clientIds) {
                addErrors(clientId, violations, labels, messageFormat);
            }
        }
        else if ("@global".equals(showMessagesFor)) {
            for (var violation : violations) {
                addGlobalError(formatMessage(violation.getMessage(), labels, messageFormat));
            }
        }
        else {
            for (var clientId : showMessagesFor.split("\\s+")) {
                addErrors(clientId, violations, labels, messageFormat);
            }
        }
    }

    private static void addErrors(String clientId, Set<ConstraintViolation<?>> violations, String labels, String messageFormat) {
        for (var violation : violations) {
            addError(clientId, formatMessage(violation.getMessage(), labels, messageFormat));
        }
    }

    private static String formatMessage(String message, String label, String messageFormat) {
        if (!isEmpty(label)) {
            var pattern = messageFormat;

            if (pattern == null) {
                var messageBundle = getMessageBundle();

                if (messageBundle != null && messageBundle.containsKey(BeanValidator.MESSAGE_ID)) {
                    pattern = messageBundle.getString(BeanValidator.MESSAGE_ID);
                }
            }

            if (pattern != null) {
                return format(pattern, message, label);
            }
        }

        return message;
    }

    // Nested classes -------------------------------------------------------------------------------------------------

    private static final class CollectingValidator implements Validator<Object> {

        private final Set<String> collectedClientIds;
        private final Map<PropertyPath, Object> collectedProperties;
        private final Map<Object, PropertyPath> knownBaseProperties;

        public CollectingValidator(Set<String> collectedClientIds, Map<PropertyPath, Object> collectedProperties, Map<Object, PropertyPath> knownBaseProperties) {
            this.collectedClientIds = collectedClientIds;
            this.collectedProperties = collectedProperties;
            this.knownBaseProperties = knownBaseProperties;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void validate(FacesContext context, UIComponent component, Object value) {
            var valueExpression = component.getValueExpression(VALUE_ATTRIBUTE);

            if (valueExpression != null) {
                collectedClientIds.add(component.getClientId(context));
                var valueReference = getValueReference(context.getELContext(), valueExpression);
                var property = (Comparable<? extends Serializable>) valueReference.getProperty();
                var basePath = knownBaseProperties.get(valueReference.getBase());
                var path = basePath != null ? basePath.with(property) : PropertyPath.of(property);
                collectedProperties.put(path, value);
            }
        }
    }

    // Callbacks ------------------------------------------------------------------------------------------------------

    private abstract static class ValidateBeanCallback implements Runnable {

        @Override
        public void run() {
            try {
                invoke();
            }
            catch (Exception e) {
                // Explicitly log since exceptions in PhaseListeners will be largely swallowed and ignored by Faces runtime.
                logger.log(SEVERE, "Exception occured while doing validation.", e);

                // Set validation failed and proceed to render response.
                validationFailed();
                renderResponse();

                throw new FacesException(e); // Rethrow, but Faces runtime will do little with it.
            }

        }

        public abstract void invoke();
    }

}