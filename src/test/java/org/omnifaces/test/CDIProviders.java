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
package org.omnifaces.test;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Installs and uninstalls the CDI provider which backs {@link CDI#current()}, so that tests can supply a mocked CDI instance. The provider is JVM wide state,
 * hence {@link #resetCDIProvider()} must run in an <code>@AfterEach</code> of every test which installs one.
 *
 * @author Bauke Scholtz
 */
public final class CDIProviders {

    private CDIProviders() {
        throw new AssertionError();
    }

    /**
     * Installs a CDI provider returning the given CDI instance, or one throwing {@link IllegalStateException} as the CDI API itself does when there is none,
     * when the given CDI instance is <code>null</code>.
     *
     * @param cdi The CDI instance to be returned by {@link CDI#current()}, if any.
     */
    public static void setCDIProvider(CDI<Object> cdi) {
        CDI.setCDIProvider(() -> {
            if (cdi == null) {
                throw new IllegalStateException("Unable to access CDI");
            }

            return cdi;
        });
    }

    /**
     * Uninstalls the CDI provider, so that the JVM wide state of the CDI API is left behind clean for other tests.
     */
    public static void resetCDIProvider() {
        CDIProviderResetter.reset();
    }

    /**
     * The CDI API does not offer any way to uninstall a programmatically set CDI provider, but the field holding it is protected static and thus accessible to
     * subclasses.
     *
     * @author Bauke Scholtz
     */
    private abstract static class CDIProviderResetter extends CDI<Object> {

        static void reset() {
            configuredProvider = null;
        }

    }

}
