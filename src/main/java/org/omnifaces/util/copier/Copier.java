/*
 * Copyright 2019 OmniFaces
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
package org.omnifaces.util.copier;

/**
 * Interface that is to be implement by classes that know how to copy an object.
 * <p>
 * This contract makes no guarantee about the level of copying that is done.
 * Copies can be deep, shallow, just a new instance of the same type or anything in between. 
 * It generally depends on the exact purpose of the copied object what level of copying is needed, and
 * different implementations of this interface can facilitate for this difference.
 * 
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public interface Copier {

	/**
	 * Return an object that's logically a copy of the given object.
	 * <p>
	 * 
	 * @param object the object to be copied
	 * @return a copy of the given object
	 */
	Object copy(Object object);
	
}
