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
package org.omnifaces.cdi.param;

import static java.util.Collections.emptySet;
import static org.omnifaces.util.Beans.getCurrentInjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.faces.convert.Converter;
import jakarta.faces.validator.Validator;

import org.omnifaces.cdi.Param;

/**
 * Dynamic CDI producer used to work around CDI's restriction to create true generic producers.
 * <p>
 * This dynamic producer calls through to the "real" producer for <code>&#64;</code>{@link Param}
 * annotated injection points.
 *
 * @see ParamExtension
 * @see ParamProducer
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
@Typed
public class DynamicParamValueProducer implements Bean<Object>, PassivationCapable {

    private final Set<Type> types;

    /**
     * Construct dynamic param value producer for given type.
     * @param type Type to construct dynamic param value producer for.
     */
    public DynamicParamValueProducer(Type type) {
        types = Set.of(type, Object.class);
    }

    @Override
    public Class<?> getBeanClass() {
        return ParamProducer.class;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Object create(CreationalContext<Object> creationalContext) {
        var injectionPoint = getCurrentInjectionPoint(creationalContext);
        var paramValue = new ParamProducer().produce(injectionPoint);
        return paramValue.getValue();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Set.of(new DefaultParamAnnotationLiteral());
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return emptySet();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext) {
        // NOOP
    }

    @Override
    public String getId() {
        return DynamicParamValueProducer.class.getName() + "_" + (types != null ? types.toString() : "");
    }

    /**
     * {@link AnnotationLiteral} for {@link Param}.
     */
    @SuppressWarnings("all")
    public static class DefaultParamAnnotationLiteral extends AnnotationLiteral<Param> implements Param {
        private static final long serialVersionUID = 1L;

        private static final String EMPTY_STRING = "";
        private static final String[] EMPTY_STRING_ARRAY = {};
        private static final Class<? extends Validator>[] EMPTY_VALIDATOR_ARRAY = new Class[0];
        private static final Attribute[] EMPTY_ATTRIBUTE_ARRAY = {};

        @Override
        public String name() {
            return EMPTY_STRING;
        }

        @Override
        public int pathIndex() {
            return -1;
        }

        @Override
        public String label() {
            return EMPTY_STRING;
        }

        @Override
        public String converter() {
            return EMPTY_STRING;
        }

        @Override
        public boolean required() {
            return false;
        }

        @Override
        public String[] validators() {
            return EMPTY_STRING_ARRAY;
        }

        @Override
        public Class<? extends Converter> converterClass() {
            return Converter.class;
        }

        @Override
        public Class<? extends Validator>[] validatorClasses() {
            return EMPTY_VALIDATOR_ARRAY;
        }

        @Override
        public Attribute[] converterAttributes() {
            return EMPTY_ATTRIBUTE_ARRAY;
        }

        @Override
        public Attribute[] validatorAttributes() {
            return EMPTY_ATTRIBUTE_ARRAY;
        }

        @Override
        public String converterMessage() {
            return EMPTY_STRING;
        }

        @Override
        public String validatorMessage() {
            return EMPTY_STRING;
        }

        @Override
        public String requiredMessage() {
            return EMPTY_STRING;
        }

        @Override
        public boolean disableBeanValidation() {
            return false;
        }

        @Override
        public boolean overrideGlobalBeanValidationDisabled() {
            return false;
        }

        @Override
        public boolean globalMessage() {
            return false;
        }
    }

}