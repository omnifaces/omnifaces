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

import static org.omnifaces.util.Faces.evaluateExpressionGet;

import javax.faces.convert.Converter;

/**
 * An abstraction of converter provider. Concrete converter provider implementations (such as the one from CDI) must
 * store themselves in the EL scope under the {@link #NAME}.
 *
 * @author Bauke Scholtz
 * @see OmniApplication
 * @since 1.6
 */
public abstract class ConverterProvider {

	// Constants ------------------------------------------------------------------------------------------------------

	/**
	 * The name on which the converter provider implementation should be stored in the EL scope.
	 */
	public static final String NAME = "omnifaces_ConverterProvider";
	private static final String EL_NAME = String.format("#{%s}", NAME);

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the converter instance associated with the given converter ID,
	 * or <code>null</code> if there is none.
	 * @param converterId The converter ID of the desired converter instance.
	 * @return the converter instance associated with the given converter ID,
	 * or <code>null</code> if there is none.
	 */
	public abstract Converter createConverter(String converterId);

	/**
	 * Returns the converter instance associated with the given converter for-class,
	 * or <code>null</code> if there is none.
	 * @param converterForClass The converter for-class of the desired converter instance.
	 * @return the converter instance associated with the given converter for-class,
	 * or <code>null</code> if there is none.
	 */
	public abstract Converter createConverter(Class<?> converterForClass);

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the converter provider implementation from the EL context,
	 * or <code>null</code> if there is none.
	 * @return The converter provider implementation from the EL context,
	 * or <code>null</code> if there is none.
	 */
	public static ConverterProvider getInstance() {
		return evaluateExpressionGet(EL_NAME);
	}

}