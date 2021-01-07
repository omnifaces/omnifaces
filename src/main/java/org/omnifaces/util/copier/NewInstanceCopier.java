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

import static org.omnifaces.util.Reflection.instance;

/**
 * Copier that doesn't actually copy an object fully, but just returns a new instance of the same type.
 * <p>
 * The object that is to be copied has to implement a public default constructor.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class NewInstanceCopier implements Copier {

	@Override
	public Object copy(Object object) {
		try {
			return instance(object.getClass());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
