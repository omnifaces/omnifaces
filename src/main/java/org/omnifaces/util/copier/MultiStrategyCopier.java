/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.util.copier;

import static java.util.Arrays.asList;

import java.util.List;

/**
 * Copier that copies an object trying a variety of strategies until one succeeds.
 * <p>
 * The strategies that will be attempted in order are:
 * <ol>
 * <li> Cloning
 * <li> Serialization
 * <li> Copy constructor
 * <li> New instance
 * </ol>
 *
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class MultiStrategyCopier implements Copier {

	private final static List<Copier> copiers = asList( // Note: copier instances used here must be thread-safe!
		new CloneCopier(), new SerializationCopier(), new CopyCtorCopier(), new NewInstanceCopier()
	);

	@Override
	public Object copy(Object object) {

		for (Copier copier : copiers) {

			try {
				return copier.copy(object);
			} catch (Exception ignore) {
				continue;
			}

		}

		throw new IllegalStateException("Can't copy object of type " + object.getClass() + ". No copier appeared to be capable of copying it.");
	}

}
