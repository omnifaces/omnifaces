/*
 * Copyright 2017 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.el.functions;

import static org.omnifaces.util.Reflection.toClass;

/**
 * Collection of EL functions for objects.
 *
 * @author Bauke Scholtz
 */
public final class Objects {

	// Constructors ---------------------------------------------------------------------------------------------------

	private Objects() {
		// Hide constructor.
	}

	// Utility --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the first non-<code>null</code> object from the provided two objects. So, if the first object is not
	 * <code>null</code>, then it will be returned, otherwise the second object will be returned.
	 * @param first The first object.
	 * @param second The second object.
	 * @return The first non-<code>null</code> object from the provided two objects.
	 */
	public static Object coalesce(Object first, Object second) {
		return first != null ? first : second;
	}

	/**
	 * Returns <code>true</code> if given object is an instance of the class as identified by given class name.
	 * @param className Fully qualified class name of the class for which a class object needs to be created.
	 * @param object The object to be checked if it is an instance of the created class object.
	 * @return <code>true</code> if given object is an instance of the class as identified by given class name.
	 * @since 3.0
	 */
	public static boolean isInstance(String className, Object object) {
		return object != null && toClass(className).isInstance(object);
	}

}