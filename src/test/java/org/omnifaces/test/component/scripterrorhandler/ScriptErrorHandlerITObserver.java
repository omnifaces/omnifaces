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
package org.omnifaces.test.component.scripterrorhandler;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.omnifaces.cdi.ScriptError;

@ApplicationScoped
public class ScriptErrorHandlerITObserver {

    private static final Logger logger = Logger.getLogger(ScriptErrorHandlerITObserver.class.getName());

    private volatile ScriptError lastError;

    public void onScriptError(@Observes ScriptError error) {
        logger.info(error.toString());
        lastError = error;
    }

    public ScriptError getLastError() {
        return lastError;
    }

    public void reset() {
        lastError = null;
    }

}
