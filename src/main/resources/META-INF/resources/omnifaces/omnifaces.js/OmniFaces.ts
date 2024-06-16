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
 * The OmniFaces namespace.
 * 
 * @author Bauke Scholtz
 */
export namespace OmniFaces {}

export const EVENT = "omnifaces.event";
export const VIEW_STATE_PARAM = "jakarta.faces.ViewState";
export const CLIENT_WINDOW_PARAM = "jakarta.faces.ClientWindow";

export { Util } from "./Util";
export { Highlight } from "./Highlight";
export { DeferredScript } from "./DeferredScript";
export { Unload } from "./Unload";
export { Push } from "./Push";
export { InputFile } from "./InputFile";
export { Form } from "./Form";
export { HashParam } from "./HashParam";
export { ScriptParam } from "./ScriptParam";
export { ServiceWorker } from "./ServiceWorker";
export { GraphicImage } from "./GraphicImage";

/**
 * Declare optional global vars for the OmniFaces namespace which is needed by some modules; tsc needs this info.
 */
declare global {
    interface Window { 
        faces: any,
        mojarra: any, 
        myfaces: any, 
        PrimeFaces: any, 
        jQuery: any 
    }
}
