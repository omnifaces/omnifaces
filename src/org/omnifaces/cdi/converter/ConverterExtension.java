/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi.converter;

import static org.omnifaces.cdi.converter.ConverterExtension.Helper.getFacesConverterAnnotation;
import static org.omnifaces.cdi.converter.ConverterExtension.Helper.getFacesConverterAnnotationValue;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.omnifaces.application.ConverterProvider;

/**
 * Collect all classes having {@link FacesConverter} annotation by their ID and/or for-class.
 *
 * @author Radu Creanga <rdcrng@gmail.com>
 * @author Bauke Scholtz
 * @see ConverterProvider
 * @since 1.6
 */
public class ConverterExtension implements Extension {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_DUPLICATE_ID =
		"Registering converter '%s' failed, duplicates converter ID '%s' of other converter '%s'.";
	private static final String ERROR_DUPLICATE_FORCLASS =
		"Registering converter '%s' failed, duplicates for-class '%s' of other converter '%s'.";

	// Properties -----------------------------------------------------------------------------------------------------

	private Map<String, Bean<Converter>> convertersByID = new HashMap<String, Bean<Converter>>();
	private Map<Class<?>, Bean<Converter>> convertersByForClass = new HashMap<Class<?>, Bean<Converter>>();

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Check if the processed {@link Converter} instance has the {@link FacesConverter} annotation and if so, then
	 * collect it by its ID and/or for-class.
	 * @param converter The processed {@link Converter} instance.
	 */
	protected void processConverters(@Observes ProcessManagedBean<Converter> converter) {
		Annotation annotation = getFacesConverterAnnotation(converter.getAnnotatedBeanClass());

		if (annotation == null) {
			return;
		}

		Bean<Converter> bean = converter.getBean();
		String converterId = getFacesConverterAnnotationValue(annotation);

		if (!"".equals(converterId)) {
			Bean<Converter> previousBean = convertersByID.put(converterId, bean);

			if (previousBean != null) {
				converter.addDefinitionError(new IllegalArgumentException(String.format(
					ERROR_DUPLICATE_ID, bean.getBeanClass(), converterId, previousBean.getBeanClass())));
			}
		}

		Class<?> converterForClass = Helper.getFacesConverterAnnotationForClass(annotation);

		if (converterForClass != Object.class) {
			Bean<Converter> previousBean = convertersByForClass.put(converterForClass, bean);

			if (previousBean != null) {
				converter.addDefinitionError(new IllegalArgumentException(String.format(
					ERROR_DUPLICATE_FORCLASS, bean.getBeanClass(), converterForClass, previousBean.getBeanClass())));
			}
		}
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns a mapping of all registered {@link FacesConverter}s by their converter ID.
	 * @return A mapping of all registered {@link FacesConverter}s by their converter ID.
	 */
	public Map<String, Bean<Converter>> getConvertersByID() {
		return convertersByID;
	}

	/**
	 * Returns a mapping of all registered {@link FacesConverter}s by their for-class.
	 * @return A mapping of all registered {@link FacesConverter}s by their for-class.
	 */
	public Map<Class<?>, Bean<Converter>> getConvertersByForClass() {
		return convertersByForClass;
	}

	// Nested classes -------------------------------------------------------------------------------------------------

	/**
	 * This ugly nested class should prevent Mojarra's AnnotationScanner from treating this class as an actual
	 * FacesConverter because of the presence of the javax.faces.convert.FacesConverter signature in class file bytes.
	 * This would otherwise only produce a confusing warning like this in Tomcat:
	 * <pre>
	 * SEVERE: Unable to load annotated class: org.omnifaces.cdi.converter.ConverterExtension,
	 * reason: java.lang.NoClassDefFoundError: javax/enterprise/inject/spi/Extension
	 * </pre>
	 */
	static final class Helper {

		public static Annotation getFacesConverterAnnotation(AnnotatedType<?> type) {
			return type.getAnnotation(FacesConverter.class);
		}

		public static String getFacesConverterAnnotationValue(Annotation facesConverterAnnotation) {
			return ((FacesConverter) facesConverterAnnotation).value();
		}

		public static Class<?> getFacesConverterAnnotationForClass(Annotation facesConverterAnnotation) {
			return ((FacesConverter) facesConverterAnnotation).forClass();
		}

	}

}