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
package org.omnifaces.util.copier;

import static java.lang.String.format;
import static org.omnifaces.util.Reflection.findMethod;

import java.lang.reflect.Method;

/**
 * Copier that copies an object using the {@link Cloneable} facility.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class CloneCopier implements Copier {

	private static final String ERROR_MISSING_INTERFACE =
		"Can not clone object of type %s because it doesn't implement Cloneable,"
			+ " you need to make sure that the class implements java.lang.Cloneable";

	private static final String ERROR_INACCESSIBLE_METHOD =
		"Can not clone object of type %s because clone() method is not accessible,"
			+ " you need to make sure that the clone() method is overridden and that it is public instead of protected";

	private static final String ERROR_UNINVOKABLE_METHOD =
		"Can not clone object of type %s because clone() method is not invokable,"
			+ " you need to make sure that invoking the clone() method does not throw an exception";

	@Override
	public Object copy(Object object) {

		if (!(object instanceof Cloneable)) {
			throw new IllegalStateException(format(ERROR_MISSING_INTERFACE, object.getClass()));
		}

		Method method = findMethod(object, "clone");

		if (!method.isAccessible()) {
			throw new IllegalStateException(format(ERROR_INACCESSIBLE_METHOD, object.getClass()));
		}

		try {
			return method.invoke(object);
		}
		catch (Exception e) {
			throw new IllegalStateException(format(ERROR_UNINVOKABLE_METHOD, object.getClass()));
		}
	}

}
