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
package org.omnifaces.test.cdi.ratelimit;

import static org.omnifaces.util.Messages.addGlobalError;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.cdi.ratelimit.RateLimitExceededException;

@Named
@RequestScoped
public class RateLimitITBean {

    private String exampleApiResponse;

    @Inject
    private RateLimitITService service;

    public void submit() {
        try {
            exampleApiResponse = service.exampleApiRequest();
        }
        catch (RateLimitExceededException e) {
            addGlobalError(e.getMessage());
        }
    }

    public String getExampleApiResponse() {
        return exampleApiResponse;
    }

}
