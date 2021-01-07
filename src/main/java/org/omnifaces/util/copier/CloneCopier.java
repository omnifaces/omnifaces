/*
 * Copyright 2021 OmniFaces
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
import static org.omnifaces.util.Reflection.invokeMethod;

/**
 * Copier that copies an object using the {@link Cloneable} facility.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class CloneCopier implements Copier {

	private static final String ERROR_CANT_CLONE =
			"Can not clone object of type %s since it doesn't implement Cloneable";

	@Override
	public Object copy(Object object) {

		if (!(object instanceof Cloneable)) {
			throw new IllegalStateException(format(ERROR_CANT_CLONE, object.getClass()));
		}

		return invokeMethod(object, "clone");
	}

}
