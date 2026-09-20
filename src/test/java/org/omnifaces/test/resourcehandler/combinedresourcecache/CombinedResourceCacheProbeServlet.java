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
package org.omnifaces.test.resourcehandler.combinedresourcecache;

import static org.omnifaces.util.cache.CacheInstancePerScopeProvider.DEFAULT_CACHE_PARAM_NAME;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.omnifaces.util.cache.TimeToLiveCache;

/**
 * Reports the entry count of the application scoped cache, so that a test can observe how HTTP requests grow it.
 */
@WebServlet("/probe")
public class CombinedResourceCacheProbeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final int ABSENT = -1;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.getWriter().write(String.valueOf(getApplicationCacheSize()));
    }

    private int getApplicationCacheSize() {
        var cache = getServletContext().getAttribute(DEFAULT_CACHE_PARAM_NAME);

        if (cache == null) {
            return ABSENT;
        }

        try {
            var field = TimeToLiveCache.class.getDeclaredField("cacheStore");
            field.setAccessible(true);
            return ((Map<?, ?>) field.get(cache)).size();
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

}
