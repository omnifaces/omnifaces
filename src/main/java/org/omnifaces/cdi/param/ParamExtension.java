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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.omnifaces.util.Reflection.modifyField;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.omnifaces.cdi.InjectionTargetWrapper;
import org.omnifaces.cdi.Param;
import org.omnifaces.util.Beans;

/**
 * CDI extension that works around the fact that CDI insists on doing absolutely
 * guaranteed type safe injections. While generally applaudable this unfortunately
 * impedes writing true generic producers that dynamically do conversion based on
 * the target type.
 * <p>
 * This extension collects the target types of each injection point qualified with
 * the <code>&#64;</code>{@link Param} annotation and dynamically registers Beans that effectively
 * represents producers for each type.
 * <p>
 * Since OmniFaces 3.6, this extension also scans for <code>&#64;</code>{@link Param} without
 * <code>&#64;</code>{@link Inject} and manually takes care of them while creating the bean.
 *
 * @since 2.0
 * @author Arjan Tijms
 */
public class ParamExtension implements Extension {

    private final Set<Type> paramsWithInject = new HashSet<>();

    /**
     * Collect fields annotated with {@link Param}.
     * @param <T> The generic injection target type.
     * @param event The process injection target event.
     */
    public <T> void collectParams(@Observes ProcessInjectionTarget<T> event) {
        Set<AnnotatedField<?>> paramsWithoutInject = new HashSet<>();

        for (AnnotatedField<?> field : event.getAnnotatedType().getFields()) {
            collectParams(field, paramsWithInject, paramsWithoutInject);
        }

        for (AnnotatedConstructor<?> constructor : event.getAnnotatedType().getConstructors()) {
            for (AnnotatedParameter<?> parameter : constructor.getParameters()) {
                collectParams(parameter, paramsWithInject, null); // Without inject CDI won't invoke constructor in first place, so pass empty list.
            }
        }

        processParamsWithoutInject(event, paramsWithoutInject);
    }

    private static void collectParams(Annotated annotated, Set<Type> paramsWithInject, Set<AnnotatedField<?>> paramsWithoutInject) {
        if (annotated.isAnnotationPresent(Param.class)) {
            Type type = annotated.getBaseType();

            if (type instanceof ParameterizedType && ParamValue.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())) {
                return; // Skip ParamValue as it is already handled by RequestParameterProducer.
            }

            if (annotated.isAnnotationPresent(Inject.class) || (annotated instanceof AnnotatedParameter && ((AnnotatedParameter<?>) annotated).getDeclaringCallable().isAnnotationPresent(Inject.class))) {
                paramsWithInject.add(type);
            }
            else if (annotated instanceof AnnotatedField) {
                paramsWithoutInject.add((AnnotatedField<?>) annotated);
            }
        }
    }

    /**
     * Process {@link Param} fields annotated with {@link Inject}.
     * @param event The after bean discovery event.
     */
    public void processParamsWithInject(@Observes AfterBeanDiscovery event) {
        for (Type paramWithInject : paramsWithInject) {
            event.addBean(new DynamicParamValueProducer(paramWithInject));
        }
    }

    /**
    /**
     * Process {@link Param} fields without {@link Inject} annotation.
     * @param <T> The generic injection target type.
     * @param event The process injection target event.
     * @param paramsWithoutInject The {@link Param} fields without {@link Inject} annotation.
     */
    public static <T> void processParamsWithoutInject(ProcessInjectionTarget<T> event, Set<AnnotatedField<?>> paramsWithoutInject) {
        if (!paramsWithoutInject.isEmpty()) {
            event.setInjectionTarget(new ParamInjectionTarget<>(event.getInjectionTarget(), paramsWithoutInject));
        }
    }

    private static final class ParamInjectionTarget<T> extends InjectionTargetWrapper<T> {

        private final Set<AnnotatedField<?>> paramsWithoutInject;

        public ParamInjectionTarget(InjectionTarget<T> wrapped, Set<AnnotatedField<?>> paramsWithoutInject) {
            super(wrapped);
            this.paramsWithoutInject = paramsWithoutInject;
        }

        @Override
        public void inject(T instance, CreationalContext<T> ctx) {
            Class<?> beanClass = Beans.unwrapIfNecessary(instance.getClass());

            for (AnnotatedField<?> paramWithoutInject : paramsWithoutInject) {
                ParamValue<?> paramValue = new ParamProducer().produce(new ParamInjectionPoint(beanClass, paramWithoutInject));
                Field field = paramWithoutInject.getJavaMember();
                modifyField(instance, field, paramValue.getValue());
            }

            super.inject(instance, ctx);
        }

        private static final class ParamInjectionPoint implements InjectionPoint {

            private final Bean<?> bean;
            private final AnnotatedField<?> paramWithoutInject;

            public ParamInjectionPoint(Class<?> beanClass, AnnotatedField<?> paramWithoutInject) {
                this.bean = Beans.resolve(beanClass);
                this.paramWithoutInject = paramWithoutInject;
            }

            @Override
            public Type getType() {
                return paramWithoutInject.getBaseType();
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return stream(paramWithoutInject.getJavaMember().getAnnotations()).filter(annotation -> annotation.annotationType().isAnnotationPresent(Qualifier.class)).collect(toSet());
            }

            @Override
            public Bean<?> getBean() {
                return bean;
            }

            @Override
            public Member getMember() {
                return paramWithoutInject.getJavaMember();
            }

            @Override
            public Annotated getAnnotated() {
                return paramWithoutInject;
            }

            @Override
            public boolean isDelegate() {
                return true;
            }

            @Override
            public boolean isTransient() {
                return true;
            }
        }
    }
}