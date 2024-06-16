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
package org.omnifaces.cdi.converter;

import static jakarta.faces.convert.Converter.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME;
import static java.lang.Boolean.parseBoolean;
import static org.omnifaces.util.BeansLocal.getReference;
import static org.omnifaces.util.BeansLocal.resolveExact;
import static org.omnifaces.util.Faces.getInitParameter;
import static org.omnifaces.util.Reflection.findConstructor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.faces.application.Application;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.DateTimeConverter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

import org.omnifaces.application.OmniApplication;
import org.omnifaces.application.OmniApplicationFactory;

/**
 * <p>
 * The <code>@FacesConverter</code> is by default not eligible for dependency injection by <code>@Inject</code> nor <code>@EJB</code>.
 * It that only when the <code>managed=true</code> attribute is set. But this doesn't support setting custom attributes.
 * OmniFaces solves this by implicitly making all {@link FacesConverter} instances eligible for dependency injection
 * <strong>without any further modification</strong>. In order to utilize OmniFaces managed converter, simply remove the
 * Faces native <code>managed=true</code> attribute.
 * <p>
 * The {@link ConverterManager} provides access to all {@link FacesConverter} annotated {@link Converter} instances
 * which are made eligible for CDI.
 *
 * <h2>bean-discovery-mode</h2>
 * <p>
 * Since CDI 1.1, when having a CDI 1.1 compatible <code>beans.xml</code>, by default only classes with an
 * explicit CDI managed bean scope annotation will be registered for dependency injection support. In order to cover
 * {@link FacesConverter} annotated classes as well, you need to explicitly set <code>bean-discovery-mode="all"</code>
 * attribute in <code>beans.xml</code>. This was not necessary in Mojarra versions older than 2.2.9 due to an
 * <a href="https://stackoverflow.com/q/29458023/157882">oversight</a>. If you want to keep the default of
 * <code>bean-discovery-mode="annotated"</code>, then you need to add {@link Dependent} annotation to the converter class.
 *
 * <h2>AmbiguousResolutionException</h2>
 * <p>
 * In case you have a {@link FacesConverter} annotated class extending another {@link FacesConverter} annotated class
 * which in turn extends a standard converter, then you may with <code>bean-discovery-mode="all"</code> face an
 * {@link AmbiguousResolutionException}. This can be solved by placing {@link Specializes} annotation on the subclass.
 *
 * <h2>Converters with special Class constructor</h2>
 * <p>
 * By default, CDI only instantiates beans via the default constructor. In case a converter for a class is created,
 * and the returned converter does not have a default constructor, or has a single argument constructor that takes a
 * {@link Class} instance, then this converter will <strong>not</strong> be made eligible for CDI. This change was added
 * in OmniFaces 2.6 as per <a href="https://github.com/omnifaces/omnifaces/issues/25">issue 25</a>.
 *
 * <h2>JSF 2.3 compatibility</h2>
 * <p>
 * JSF 2.3 introduced two new features for converters: parameterized converters and managed converters.
 * When the converter is parameterized as in <code>implements Converter&lt;T&gt;</code>, then you need to use
 * at least OmniFaces 3.1 wherein the incompatibility was fixed. When the converter is managed with the
 * <code>managed=true</code> attribute set on the {@link FacesConverter} annotation, then the converter won't be
 * managed by OmniFaces and will continue to work fine for Faces. But the &lt;o:converter&gt; tag won't be able to
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
public class ConverterManager {

    // Dependencies ---------------------------------------------------------------------------------------------------

    @Inject
    private BeanManager manager;
    private Map<String, Bean<Converter>> convertersById = new HashMap<>();
    private Map<Class<?>, Bean<Converter>> convertersByForClass = new HashMap<>();
    private TimeZone dateTimeConverterDefaultTimeZone;

    // Init -----------------------------------------------------------------------------------------------------------

    /**
     * Initialize {@link Converter#DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME}.
     */
    @PostConstruct
    public void init() {
        dateTimeConverterDefaultTimeZone =
            parseBoolean(getInitParameter(DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE_PARAM_NAME))
                ? TimeZone.getDefault()
                : null;
    }

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Returns the converter instance associated with the given converter ID,
     * or <code>null</code> if there is none.
     * @param application The involved Faces application.
     * @param converterId The converter ID of the desired converter instance.
     * @return the converter instance associated with the given converter ID,
     * or <code>null</code> if there is none.
     */
    public Converter createConverter(Application application, String converterId) {
        var converter = application.createConverter(converterId);
        var bean = convertersById.get(converterId);

        if (bean == null && !convertersById.containsKey(converterId)) {

            if (isUnmanaged(converter)) {
                bean = resolve(converter.getClass(), converterId, Object.class);
            }

            convertersById.put(converterId, bean);
        }

        if (bean != null) {
            converter = getReference(manager, bean);

            if (converter != null) {
                setDefaultPropertiesIfNecessary(converter);
            }
        }

        return converter;
    }

    /**
     * Returns the converter instance associated with the given converter for-class,
     * or <code>null</code> if there is none.
     * @param application The involved Faces application.
     * @param converterForClass The converter for-class of the desired converter instance.
     * @return the converter instance associated with the given converter for-class,
     * or <code>null</code> if there is none.
     */
    public Converter createConverter(Application application, Class<?> converterForClass) {
        var converter = application.createConverter(converterForClass);
        var bean = convertersByForClass.get(converterForClass);

        if (bean == null && !convertersByForClass.containsKey(converterForClass)) {

            if (isUnmanaged(converter)) {
                var converterClass = converter.getClass();

                if (findConstructor(converterClass) != null && findConstructor(converterClass, Class.class) == null) {
                    bean = resolve(converterClass, "", converterForClass);
                }
            }

            convertersByForClass.put(converterForClass, bean);
        }

        if (bean != null) {
            converter = getReference(manager, bean);

            if (converter != null) {
                setDefaultPropertiesIfNecessary(converter);
            }
        }

        return converter;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    private static boolean isUnmanaged(Converter converter) {
        if (converter == null) {
            return false;
        }

        var annotation = converter.getClass().getAnnotation(FacesConverter.class);

        if (annotation == null) {
            return false;
        }

        return !annotation.managed();
    }

    @SuppressWarnings("unchecked")
    private Bean<Converter> resolve(Class<? extends Converter> converterClass, String converterId, Class<?> converterForClass) {

        // First try by class.
        var bean = (Bean<Converter>) resolveExact(manager, converterClass);

        if (bean == null) {
            var annotation = converterClass.getAnnotation(FacesConverter.class);

            if (annotation != null) {
                // Then by own annotation, if any.
                bean = (Bean<Converter>) resolveExact(manager, converterClass, annotation);
            }

            if (bean == null) {
                // Else by fabricated annotation literal.
                bean = (Bean<Converter>) resolveExact(manager, converterClass, new FacesConverter() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return FacesConverter.class;
                    }
                    @Override
                    public String value() {
                        return converterId;
                    }
                    @Override
                    public boolean managed() {
                        return false;
                    }
                    @Override
                    public Class forClass() {
                        return converterForClass;
                    }
                });
            }
        }

        return bean;
    }

    private void setDefaultPropertiesIfNecessary(Converter converter) {
        if (converter instanceof DateTimeConverter dateTimeConverter && dateTimeConverterDefaultTimeZone != null) {
            dateTimeConverter.setTimeZone(dateTimeConverterDefaultTimeZone);
        }
    }

}