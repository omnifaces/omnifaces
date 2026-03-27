///
/// Copyright OmniFaces
///
/// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
/// the License. You may obtain a copy of the License at
///
///     https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
/// an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
/// specific language governing permissions and limitations under the License.
///

/**
 * Tests for the OmniFaces.GraphicImage namespace (lazy image loading).
 *
 * GraphicImage auto-initializes via addOnloadListener(initLazyImages) at module load time.
 * Since the module is loaded once in beforeAll with no lazy images in the DOM, initLazyImages
 * returns early (no images to process). The IntersectionObserver or scroll/resize listeners
 * are never registered. These tests verify the selector logic and image attribute manipulation
 * used by the lazy loading mechanism.
 */

import { loadOmniFacesJs } from "../test-setup";

beforeAll(() => loadOmniFacesJs());

describe("OmniFaces.GraphicImage: lazy image selector", () => {

    afterEach(() => {
        document.querySelectorAll("img").forEach(img => img.remove());
    });

    test("matches images with src, data-src, and data-lazy attributes", () => {
        const img = document.createElement("img");
        img.src = "/placeholder.gif";
        img.dataset.src = "/real.jpg";
        img.dataset.lazy = "true";
        document.body.appendChild(img);

        const matches = document.querySelectorAll("img[src][data-src][data-lazy]");
        expect(matches.length).toBe(1);
        expect(matches[0]).toBe(img);
    });

    test("does not match images without data-src", () => {
        const img = document.createElement("img");
        img.src = "/normal.jpg";
        document.body.appendChild(img);

        const matches = document.querySelectorAll("img[src][data-src][data-lazy]");
        expect(matches.length).toBe(0);
    });

    test("does not match images without data-lazy", () => {
        const img = document.createElement("img");
        img.src = "/placeholder.gif";
        img.dataset.src = "/real.jpg";
        document.body.appendChild(img);

        const matches = document.querySelectorAll("img[src][data-src][data-lazy]");
        expect(matches.length).toBe(0);
    });

    test("matches multiple lazy images", () => {
        for (let i = 0; i < 3; i++) {
            const img = document.createElement("img");
            img.src = "/placeholder.gif";
            img.dataset.src = `/real-${i}.jpg`;
            img.dataset.lazy = "true";
            document.body.appendChild(img);
        }

        const matches = document.querySelectorAll("img[src][data-src][data-lazy]");
        expect(matches.length).toBe(3);
    });

    test("loadLazyImage pattern: data-src replaces src, data attributes are removed", () => {
        // Verify the pattern used by loadLazyImage:
        // img.src = data.src; delete data.src; delete data.lazy;
        const img = document.createElement("img");
        img.src = "/placeholder.gif";
        img.dataset.src = "/real-image.jpg";
        img.dataset.lazy = "true";
        document.body.appendChild(img);

        // Simulate what loadLazyImage does
        const data = img.dataset;
        if (data.lazy && data.src) {
            img.src = data.src;
        }
        delete data.src;
        delete data.lazy;

        expect(img.src).toContain("/real-image.jpg");
        expect(img.dataset.src).toBeUndefined();
        expect(img.dataset.lazy).toBeUndefined();

        // Image should no longer match the lazy selector
        const matches = document.querySelectorAll("img[src][data-src][data-lazy]");
        expect(matches.length).toBe(0);
    });
});
