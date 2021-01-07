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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Copier that copies an object using its copy constructor.
 * <p>
 * A copy constructor is a constructor that takes an object of the same type as the object that's
 * to be constructed. This constructor then initializes itself using the values of this other instance.
 * 
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class CopyCtorCopier implements Copier {
	
	@Override
	public Object copy(Object object) {
		
		try {
			Constructor<? extends Object> copyConstructor = object.getClass().getConstructor(object.getClass());
			
			return copyConstructor.newInstance(object);
			
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
		
	}

}
