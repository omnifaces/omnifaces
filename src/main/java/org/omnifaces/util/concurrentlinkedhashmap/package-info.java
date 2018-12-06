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
/**
 * This package contains an implementation of a bounded
 * {@link java.util.concurrent.ConcurrentMap} data structure.
 * <p>
 * {@link org.omnifaces.util.concurrentlinkedhashmap.EvictionListener} provides the
 * ability to be notified when an entry is evicted from the map. An eviction
 * occurs when the entry was automatically removed due to the map exceeding a
 * capacity threshold. It is not called when an entry was explicitly removed.
 * <p>
 * The {@link org.omnifaces.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap}
 * class supplies an efficient, scalable, thread-safe, bounded map. As with the
 * <tt>Java Collections Framework</tt> the "Concurrent" prefix is used to
 * indicate that the map is not governed by a single exclusion lock.
 *
 * @see <a href="https://github.com/ben-manes/concurrentlinkedhashmap">
 *      https://github.com/ben-manes/concurrentlinkedhashmap</a>
 */
package org.omnifaces.util.concurrentlinkedhashmap;
