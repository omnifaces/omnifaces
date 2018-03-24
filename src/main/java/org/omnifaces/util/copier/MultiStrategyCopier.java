/*
 * Copyright 2018 OmniFaces
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

import static java.util.Arrays.asList;
import static java.util.logging.Level.FINE;

import java.util.List;
import java.util.logging.Logger;

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

	private static final Logger logger = Logger.getLogger(MultiStrategyCopier.class.getName());

	private static final List<Copier> COPIERS = asList( // Note: copier instances used here must be thread-safe!
		new CloneCopier(), new SerializationCopier(), new CopyCtorCopier(), new NewInstanceCopier()
	);

	@Override
	public Object copy(Object object) {

		for (Copier copier : COPIERS) {

			try {
				return copier.copy(object);
			} catch (Exception ignore) {
				logger.log(FINE, "Ignoring thrown exception; next copier will be tried and there is a fallback to IllegalStateException.", ignore);
				continue;
			}

		}

		throw new IllegalStateException("Can't copy object of type " + object.getClass() + ". No copier appeared to be capable of copying it.");
	}

}
