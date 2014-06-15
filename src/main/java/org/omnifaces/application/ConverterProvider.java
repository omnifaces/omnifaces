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
package org.omnifaces.application;

import javax.faces.application.Application;
import javax.faces.convert.Converter;

/**
 * An abstraction of converter provider, so that JSF can obtain the concrete instance without a CDI dependency.
 *
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
public interface ConverterProvider {

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the converter instance associated with the given converter ID,
	 * or <code>null</code> if there is none.
	 * @param application The involved JSF application.
	 * @param converterId The converter ID of the desired converter instance.
	 * @return the converter instance associated with the given converter ID,
	 * or <code>null</code> if there is none.
	 */
	public Converter createConverter(Application application, String converterId);

	/**
	 * Returns the converter instance associated with the given converter for-class,
	 * or <code>null</code> if there is none.
	 * @param application The involved JSF application.
	 * @param converterForClass The converter for-class of the desired converter instance.
	 * @return the converter instance associated with the given converter for-class,
	 * or <code>null</code> if there is none.
	 */
	public Converter createConverter(Application application, Class<?> converterForClass);

}