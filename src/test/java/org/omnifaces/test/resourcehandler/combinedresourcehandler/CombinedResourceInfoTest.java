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
package org.omnifaces.test.resourcehandler.combinedresourcehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.omnifaces.util.Utils.serializeURLSafe;
import static org.omnifaces.util.Utils.unserializeURLSafe;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.omnifaces.resourcehandler.CombinedResourceInfo;

/**
 * Verifies that a manipulated (i.e. not server-issued) combined resource ID cannot make the combined resource handler
 * serve arbitrary resources, inflate without bound, or grow its cache. Only structurally valid IDs which exclusively
 * reference stylesheet and script resources are accepted, and they are not retained in the cache.
 */
class CombinedResourceInfoTest {

	@Test
	void forgedIdReferencingCombinableResourceIsAccepted() {
		assertNotNull(CombinedResourceInfo.get(serializeURLSafe("style.css")), "stylesheet is combinable");
		assertNotNull(CombinedResourceInfo.get(serializeURLSafe("library:script.js")), "script is combinable");
	}

	@Test
	void forgedIdReferencingNonCombinableResourceIsRejected() {
		assertNull(CombinedResourceInfo.get(serializeURLSafe("page.xhtml")), "facelets file is not combinable");
		assertNull(CombinedResourceInfo.get(serializeURLSafe("library:image.png")), "image is not combinable");
		assertNull(CombinedResourceInfo.get(serializeURLSafe("style.css|page.xhtml")), "one non-combinable resource rejects the whole ID");
	}

	@Test
	void malformedIdIsRejected() {
		assertNull(CombinedResourceInfo.get("this-is-not-a-valid-id"), "malformed ID is rejected");
	}

	@Test
	void inflationBombIsRejected() {
		String bomb = serializeURLSafe(repeat("style.css|", 10000)); // Compresses tiny but inflates to ~100 KB, over the 64 KiB cap.
		assertThrows(IllegalArgumentException.class, () -> unserializeURLSafe(bomb), "over-large inflation is rejected");
		assertNull(CombinedResourceInfo.get(bomb), "combined resource with over-large inflation is rejected");
	}

	@Test
	void forgedIdIsNotCached() throws Exception {
		Map<?, ?> cache = getCache();
		int size = cache.size();
		assertNotNull(CombinedResourceInfo.get(serializeURLSafe("uncached.css")), "structurally valid ID is served");
		assertEquals(size, cache.size(), "forged ID is not retained in the cache");
	}

	private static Map<?, ?> getCache() throws Exception {
		Field field = CombinedResourceInfo.class.getDeclaredField("CACHE");
		field.setAccessible(true);
		return (Map<?, ?>) field.get(null);
	}

	private static String repeat(String string, int count) {
		StringBuilder builder = new StringBuilder(string.length() * count);

		for (int i = 0; i < count; i++) {
			builder.append(string);
		}

		return builder.toString();
	}

}
