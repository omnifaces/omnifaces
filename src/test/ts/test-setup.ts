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
 * Shared test configuration and helpers for omnifaces.js tests.
 */

import fs from "fs";
import path from "path";

declare global {
    var OmniFaces: Record<string, unknown>;
}

export const OMNIFACES_JS = path.resolve(__dirname, "../../../target/classes/META-INF/resources/omnifaces/omnifaces.js");

/**
 * Load the omnifaces.js bundle into jsdom.
 * Call this in beforeAll().
 */
export function loadOmniFacesJs(): void {
    const source = fs.readFileSync(OMNIFACES_JS, "utf-8");
    const script = document.createElement("script");
    script.textContent = source;
    document.head.appendChild(script);
}
