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
package org.omnifaces.cdi.validator;

import static org.omnifaces.util.BeansLocal.getReference;
import static org.omnifaces.util.BeansLocal.resolveExact;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.application.Application;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.inject.Inject;

import org.omnifaces.application.OmniApplication;
import org.omnifaces.application.OmniApplicationFactory;

/**
 * <p>
 * The <code>@FacesValidator</code> is by default not eligible for dependency injection by <code>@Inject</code> nor <code>@EJB</code>.
 * It that only when the <code>managed=true</code> attribute is set. But this doesn't support setting custom attributes.
 * OmniFaces solves this by implicitly making all {@link FacesValidator} instances eligible for dependency injection
 * <strong>without any further modification</strong>. In order to utilize OmniFaces managed validator, simply remove the
 * Faces native <code>managed=true</code> attribute.
 * <p>
 * The {@link ValidatorManager} provides access to all {@link FacesValidator} annotated {@link Validator} instances
 * which are made eligible for CDI.
 *
 * <h2>bean-discovery-mode</h2>
 * <p>
 * Since CDI 1.1, when having a CDI 1.1 compatible <code>beans.xml</code>, by default only classes with an
 * explicit CDI managed bean scope annotation will be registered for dependency injection support. In order to cover
 * {@link FacesValidator} annotated classes as well, you need to explicitly set <code>bean-discovery-mode="all"</code>
 * attribute in <code>beans.xml</code>. This was not necessary in Mojarra versions older than 2.2.9 due to an
 * <a href="https://stackoverflow.com/q/29458023/157882">oversight</a>. If you want to keep the default of
 * <code>bean-discovery-mode="annotated"</code>, then you need to add {@link Dependent} annotation to the validator class.
 *
 * <h2>AmbiguousResolutionException</h2>
 * <p>
 * In case you have a {@link FacesValidator} annotated class extending another {@link FacesValidator} annotated class
 * which in turn extends a standard validator, then you may with <code>bean-discovery-mode="all"</code> face an
 * {@link AmbiguousResolutionException}. This can be solved by placing {@link Specializes} annotation on the subclass.
 *
 * <h2>JSF 2.3 compatibility</h2>
 * <p>
 * JSF 2.3 introduced two new features for validators: parameterized validators and managed validators.
 * When the validator is parameterized as in <code>implements Validator&lt;T&gt;</code>, then you need to use
 * at least OmniFaces 3.1 wherein the incompatibility was fixed. When the validator is managed with the
 * <code>managed=true</code> attribute set on the {@link FacesValidator} annotation, then the validator won't be
 * managed by OmniFaces and will continue to work fine for Faces. But the &lt;o:validator&gt; tag won't be able to
 * set attributes on it.
 *
 * @author Radu Creanga {@literal <rdcrng@gmail.com>}
 * @author Bauke Scholtz
 * @see OmniApplication
 * @see OmniApplicationFactory
 * @since 1.6
 */
@ApplicationScoped
@SuppressWarnings("rawtypes")
public class ValidatorManager {

    // Dependencies ---------------------------------------------------------------------------------------------------

    @Inject
    private BeanManager manager;
    private Map<String, Bean<Validator>> validatorsById = new HashMap<>();

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns the validator instance associated with the given validator ID,
     * or <code>null</code> if there is none.
     * @param application The involved Faces application.
     * @param validatorId The validator ID of the desired validator instance.
     * @return the validator instance associated with the given validator ID,
     * or <code>null</code> if there is none.
     */
    public Validator createValidator(Application application, String validatorId) {
        var validator = application.createValidator(validatorId);
        var bean = validatorsById.get(validatorId);

        if (bean == null && !validatorsById.containsKey(validatorId)) {

            if (isUnmanaged(validator)) {
                bean = resolve(validator.getClass(), validatorId);
            }

            validatorsById.put(validatorId, bean);
        }

        return bean != null ? getReference(manager, bean) : validator;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static boolean isUnmanaged(Validator validator) {
        if (validator == null) {
            return false;
        }

        var annotation = validator.getClass().getAnnotation(FacesValidator.class);

        if (annotation == null) {
            return false;
        }

        return !annotation.managed();
    }

    @SuppressWarnings("unchecked")
    private Bean<Validator> resolve(Class<? extends Validator> validatorClass, String validatorId) {

        // First try by class.
        var bean = (Bean<Validator>) resolveExact(manager, validatorClass);

        if (bean == null) {
            var annotation = validatorClass.getAnnotation(FacesValidator.class);

            if (annotation != null) {
                // Then by own annotation, if any.
                bean = (Bean<Validator>) resolveExact(manager, validatorClass, annotation);
            }

            if (bean == null) {
                // Else by fabricated annotation literal.
                bean = (Bean<Validator>) resolveExact(manager, validatorClass, new FacesValidator() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return FacesValidator.class;
                    }
                    @Override
                    public String value() {
                        return validatorId;
                    }
                    @Override
                    public boolean managed() {
                        return false;
                    }
                    @Override
                    public boolean isDefault() {
                        return false;
                    }
                });
            }
        }

        return bean;
    }

}