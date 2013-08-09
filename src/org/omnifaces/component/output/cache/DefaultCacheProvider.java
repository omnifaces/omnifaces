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
package org.omnifaces.component.output.cache;

import org.omnifaces.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * A default cache provider that will be used by the OmniFaces Cache component if no explicit provider has been
 * configured.
 * <p>
 * This will create a Cache instance that uses a repackaged {@link ConcurrentLinkedHashMap} for the actual implementation.
 * <p>
 * <b>See:</b> <a href="http://code.google.com/p/concurrentlinkedhashmap">http://code.google.com/p/concurrentlinkedhashmap</a>
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public class DefaultCacheProvider extends CacheInstancePerScopeProvider {

	@Override
	protected Cache createCache(Integer timeToLive, Integer maxCapacity) {
		return new DefaultCache(timeToLive, maxCapacity);
	}

}
