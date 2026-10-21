/*
 * Copyright OmniFaces
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
package org.omnifaces.test.cdi.viewscoped.unrendered;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;

/**
 * Counts the view scoped beans of this test which were created and destroyed, by name, and which of them were destroyed
 * without a faces context.
 */
@ApplicationScoped
public class ViewScopedUnrenderedITProbe {

	private final Map<String, AtomicInteger> created = new ConcurrentHashMap<>();
	private final Map<String, AtomicInteger> destroyed = new ConcurrentHashMap<>();
	private final Map<String, AtomicInteger> destroyedWithoutFacesContext = new ConcurrentHashMap<>();

	public void create(String name) {
		created.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
	}

	public void destroy(String name) {
		destroyed.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();

		if (FacesContext.getCurrentInstance() == null) {
			destroyedWithoutFacesContext.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
		}
	}

	public int getCreated(String name) {
		return get(created, name);
	}

	public int getDestroyed(String name) {
		return get(destroyed, name);
	}

	public int getDestroyedWithoutFacesContext(String name) {
		return get(destroyedWithoutFacesContext, name);
	}

	private static int get(Map<String, AtomicInteger> counts, String name) {
		AtomicInteger count = counts.get(name);
		return count == null ? 0 : count.get();
	}

}
