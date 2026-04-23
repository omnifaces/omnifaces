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

import { Util } from "./Util";

/**
 * Lazy image loader.
 *
 * @author Bauke Scholtz
 * @see org.omnifaces.component.output.GraphicImage
 * @see https://css-tricks.com/the-complete-guide-to-lazy-loading-images/
 * @since 3.10
 */
export namespace GraphicImage {

    // Private static functions ---------------------------------------------------------------------------------------

    /**
     * Register a viewport intersection listener that loads each lazy image rendered by <code>o:graphicImage</code> as it enters the viewport. No-op when the
     * current document contains no pending lazy images.
     */
    function initLazyImages() {
        if (getLazyImages().length == 0) {
            return;
        }

        Util.addIntersectionListener(getLazyImages, loadLazyImage);
    }

    /**
     * Returns the lazy images rendered by <code>o:graphicImage</code> which still need loading. Loaded images no longer carry the <code>data-lazy</code> and
     * <code>data-src</code> attributes and are therefore filtered out by the selector.
     */
    function getLazyImages(): NodeListOf<HTMLImageElement> {
        return document.querySelectorAll("img[src][data-src][data-lazy]");
    }

    /**
     * Promote the given lazy image's <code>data-src</code> to <code>src</code>, triggering the actual image load, and clear both <code>data-src</code> and
     * <code>data-lazy</code> attributes so the image is no longer matched by {@link getLazyImages}.
     */
    function loadLazyImage(img: HTMLImageElement) {
        const data = img.dataset;

        if (data.lazy && data.src) {
            img.src = data.src;
        }

        delete data.src;
        delete data.lazy;
    }

    // Global initialization ------------------------------------------------------------------------------------------

    Util.addOnloadListener(initLazyImages);

}
