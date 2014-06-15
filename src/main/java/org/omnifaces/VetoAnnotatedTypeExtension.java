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
package org.omnifaces;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * This CDI extension should tell the CDI implementation to register from the org.omnifaces package only the classes
 * from org.omnifaces.cdi and org.omnifaces.showcase subpackages as CDI managed beans.
 *
 * @author Bauke Scholtz
 * @since 1.6.1
 */
public class VetoAnnotatedTypeExtension implements Extension {

	public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> type) {
		Package _package = type.getAnnotatedType().getJavaClass().getPackage();

		if (_package == null) {
			return;
		}

		String packageName = _package.getName();

		if (packageName.startsWith("org.omnifaces.")
			&& !packageName.startsWith("org.omnifaces.cdi.")
			&& !packageName.startsWith("org.omnifaces.showcase."))
		{
			type.veto();
		}
	}

}