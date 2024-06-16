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

import { EVENT } from "./OmniFaces";

/**
 * Input file client side validator (so far only maxsize is validated).
 * 
 * @author Bauke Scholtz
 * @see org.omnifaces.component.input.InputFile
 * @since 2.5
 */
export module InputFile {

    // Public static functions ----------------------------------------------------------------------------------------

    /**
     * Validate size of selected files of given o:inputFile against given maxsize.
     * The faces message will be triggered server side.
     * @param event Required; The involved DOM event (expected: 'change').
     * @param inputFile Required; The involved o:inputFile.
     * @param messageClientId Required; The client ID of involved h:message.
     * @param maxsize Required; The maximum size for each selected file.
     */
    export function validate(event: Event, inputFile: HTMLInputElement, messageClientId: string, maxsize: number) {
        if (!window.FileReader) {
            return true; // File API not supported (IE6-9). End of story. Let standard Faces code continue.
        }

        document.getElementById(messageClientId).innerHTML = ""; // Clear out any previously rendered message.

        for (var i = 0; i < inputFile.files.length; i++) {
            const file = inputFile.files[i];

            if (file.size > maxsize) {
                const fileName = file.name;
                let originalEnctype: string;

                if (window.mojarra) { // Mojarra doesn't add custom params when using iframe transport.
                    originalEnctype = inputFile.form.enctype;
                    inputFile.form.enctype = "application/x-www-form-urlencoded";
                }

                // Clear out selected files. Note: inputFile.value = null doesn't work in IE.
                inputFile.type = "text";
                inputFile.type = "file";
                
                const params: Record<string, string> = { fileName: fileName };
                params[EVENT] = "validationFailed";
                const faces = window.faces;
                faces.ajax.request(inputFile.id, event, params);

                if (originalEnctype) {
                    inputFile.form.enctype = originalEnctype;
                }

                return false;
            }
        }
        
        return true;
    }

}