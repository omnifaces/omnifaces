/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of general utility methods for working with collections.
 *
 * @author Arjan Tijms
 */
public final class Collections {
	
	/**
	 * Returns whether the specified array object is really an array.
	 * <p>Note that <code>null</code> is never considered an array.</p>
	 * 
	 * @param array the array to be tested
	 * @return <code>true</code> if the argument represents an array class, <code>false</code> otherwise.
	 */
	public static boolean isArray(Object array) {
		return array != null && array.getClass().isArray();
	}

	/**
     * Returns the specified array object as a <code>List</code>.
     *
     * @param array the array
     * @return a list consisting out of the elements in the array
     * @exception IllegalArgumentException in case the argument is not really an actual array
     */
	public static List<Object> arrayObjectToList(Object array) {
		
		int arrayLength = Array.getLength(array); // throws if no array
		List<Object> list = new ArrayList<Object>(arrayLength);
		for (int i = 0; i < arrayLength; i++) {
			list.add(Array.get(array, i));
		}
		
		return list;
	}
	
	/**
	 * Returns whether the specified array is empty. 
	 * <p>Note that <code>null</code> is considered to be empty.
	 * 
	 * @param array array the array to be tested
	 * @return <code>true</code> if the array is empty, <code>false</code> otherwise.
	 */
	public static boolean isEmpty(Object[] array) {
		return array == null || array.length == 0;	
	}
	
}
