/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.test.util;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.omnifaces.util.Lazy;

/**
 * Tests Lazy initializer class
 *
 * @author Lenny Primak
 */
public class TestLazy {

	private final AtomicInteger numCreations = new AtomicInteger();

	@Before
	public void before() {
		numCreations.set(0);
	}

	class Expensive {
		Expensive() {
			numCreations.incrementAndGet();
		}
	}

	@Test
	public void lazy() {
		Expensive expensive = new Expensive();
		Lazy<Expensive> cheap = new Lazy<>(Expensive::new);
		assertEquals(1, numCreations.get());
		assertEquals(Expensive.class, cheap.get().getClass());
		assertEquals(2, numCreations.get());
		assertEquals(Expensive.class, cheap.get().getClass());
		assertEquals(2, numCreations.get());
	}
}