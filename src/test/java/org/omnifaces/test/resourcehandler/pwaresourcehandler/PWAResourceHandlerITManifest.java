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
package org.omnifaces.test.resourcehandler.pwaresourcehandler;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;

import org.omnifaces.resourcehandler.WebAppManifest;

@ApplicationScoped
public class PWAResourceHandlerITManifest extends WebAppManifest {

    @Override
    public String getName() {
        return "PWAResourceHandlerIT";
    }

    @Override
    public Collection<ImageResource> getIcons() {
        return asList(ImageResource.of("icon.png", Size.SIZE_512));
    }

    @Override
    protected Collection<String> getCacheableViewIds() {
    	return asList("/PWAResourceHandlerIT.xhtml");
    }

    @Override
    public String getLang() {
    	return Locale.ENGLISH.getLanguage();
    }

}