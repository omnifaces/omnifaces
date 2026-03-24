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
 * Shared mock XMLHttpRequest, WebSocket, ServiceWorker, Beacon and DOM helpers for omnifaces.js tests.
 */

import { VIEW_STATE_PARAM } from "./test-constants";

// ---- Mock XMLHttpRequest ----

export interface MockXHRInstance {
    method: string | null;
    url: string | null;
    async: boolean;
    requestHeaders: Record<string, string>;
    body: string | null;
    readyState: number;
    status: number;
    responseText: string;
    responseXML: Document | null;
    onreadystatechange: ((this: XMLHttpRequest, ev: Event) => void) | null;
    open(method: string, url: string, async?: boolean): void;
    setRequestHeader(name: string, value: string): void;
    send(body?: string | null): void;
    /** Test helper: simulate server response. */
    respond(status: number, responseText: string, responseXML?: string): void;
}

let xhrInstances: MockXHRInstance[];
let OriginalXHR: typeof XMLHttpRequest;

export function installMockXHR(): void {
    xhrInstances = [];
    OriginalXHR = window.XMLHttpRequest;

    (window as unknown as Record<string, unknown>).XMLHttpRequest = function MockXHR(this: MockXHRInstance) {
        this.method = null;
        this.url = null;
        this.async = true;
        this.requestHeaders = {};
        this.body = null;
        this.readyState = 0;
        this.status = 0;
        this.responseText = "";
        this.responseXML = null;
        this.onreadystatechange = null;

        this.open = function (method: string, url: string, async = true) {
            this.method = method;
            this.url = url;
            this.async = async;
            this.readyState = 1;
        };

        this.setRequestHeader = function (name: string, value: string) {
            this.requestHeaders[name] = value;
        };

        this.send = function (body?: string | null) {
            this.body = body ?? null;
        };

        this.respond = function (status: number, responseText: string, responseXML?: string) {
            this.status = status;
            this.responseText = responseText;
            if (responseXML) {
                this.responseXML = new DOMParser().parseFromString(responseXML, "application/xml");
            }
            this.readyState = 4;
            if (this.onreadystatechange) {
                this.onreadystatechange.call(this as unknown as XMLHttpRequest, new Event("readystatechange"));
            }
        };

        xhrInstances.push(this);
    } as unknown as typeof XMLHttpRequest;
}

export function uninstallMockXHR(): void {
    window.XMLHttpRequest = OriginalXHR;
}

export function lastXHR(): MockXHRInstance {
    return xhrInstances[xhrInstances.length - 1];
}

export function getXHRInstances(): MockXHRInstance[] {
    return xhrInstances;
}

// ---- Mock WebSocket ----

export interface MockWebSocketInstance {
    url: string;
    readyState: number;
    onopen: ((event: Event) => void) | null;
    onmessage: ((event: { data: string }) => void) | null;
    onclose: ((event: { code: number; reason: string }) => void) | null;
    close(): void;
    /** Test helper: simulate open event. */
    simulateOpen(): void;
    /** Test helper: simulate message event. */
    simulateMessage(data: unknown): void;
    /** Test helper: simulate close event. */
    simulateClose(code: number, reason?: string): void;
}

let wsInstances: MockWebSocketInstance[];
let OriginalWebSocket: typeof WebSocket;

export function installMockWebSocket(): void {
    wsInstances = [];
    OriginalWebSocket = window.WebSocket;

    (window as unknown as Record<string, unknown>).WebSocket = function MockWebSocket(this: MockWebSocketInstance, url: string) {
        this.url = url;
        this.readyState = 0; // CONNECTING
        this.onopen = null;
        this.onmessage = null;
        this.onclose = null;

        this.close = function () {
            this.readyState = 3; // CLOSED
            if (this.onclose) {
                this.onclose({ code: 1000, reason: "" });
            }
        };

        this.simulateOpen = function () {
            this.readyState = 1; // OPEN
            if (this.onopen) {
                this.onopen(new Event("open"));
            }
        };

        this.simulateMessage = function (data: unknown) {
            if (this.onmessage) {
                this.onmessage({ data: JSON.stringify(data) });
            }
        };

        this.simulateClose = function (code: number, reason = "") {
            this.readyState = 3; // CLOSED
            if (this.onclose) {
                this.onclose({ code, reason });
            }
        };

        wsInstances.push(this);
    } as unknown as typeof WebSocket;
}

export function uninstallMockWebSocket(): void {
    window.WebSocket = OriginalWebSocket;
}

export function lastWS(): MockWebSocketInstance {
    return wsInstances[wsInstances.length - 1];
}

export function getWSInstances(): MockWebSocketInstance[] {
    return wsInstances;
}

// ---- Mock EventSource ----

export interface MockEventSourceInstance {
    url: string;
    readyState: number;
    onopen: ((event: Event) => void) | null;
    onmessage: ((event: { data: string }) => void) | null;
    onerror: ((event: Event) => void) | null;
    addEventListener(type: string, listener: (event: MessageEvent) => void): void;
    close(): void;
    /** Test helper: simulate open event. */
    simulateOpen(): void;
    /** Test helper: simulate message event. */
    simulateMessage(data: unknown): void;
    /** Test helper: simulate a named "close" event from the server with the given code. */
    simulateCloseEvent(code: number): void;
    /** Test helper: simulate error event with CONNECTING readyState (transient error, browser will retry). */
    simulateError(): void;
    /** Test helper: simulate error event with CLOSED readyState (server closed the connection). */
    simulateServerClose(): void;
}

let esInstances: MockEventSourceInstance[];
let OriginalEventSource: typeof EventSource;

export function installMockEventSource(): void {
    esInstances = [];
    OriginalEventSource = window.EventSource;

    const MockES = function MockEventSource(this: MockEventSourceInstance, url: string) {
        const eventListeners: Record<string, ((event: MessageEvent) => void)[]> = {};

        this.url = url;
        this.readyState = 0; // CONNECTING
        this.onopen = null;
        this.onmessage = null;
        this.onerror = null;

        this.addEventListener = function (type: string, listener: (event: MessageEvent) => void) {
            if (!eventListeners[type]) {
                eventListeners[type] = [];
            }
            eventListeners[type].push(listener);
        };

        this.close = function () {
            this.readyState = 2; // CLOSED
        };

        this.simulateOpen = function () {
            this.readyState = 1; // OPEN
            if (this.onopen) {
                this.onopen(new Event("open"));
            }
        };

        this.simulateMessage = function (data: unknown) {
            if (this.onmessage) {
                this.onmessage({ data: JSON.stringify(data) } as MessageEvent);
            }
        };

        this.simulateCloseEvent = function (code: number) {
            const listeners = eventListeners["close"] || [];
            listeners.forEach(fn => fn({ data: String(code) } as MessageEvent));
        };

        this.simulateError = function () {
            this.readyState = 0; // CONNECTING (browser will retry)
            if (this.onerror) {
                this.onerror(new Event("error"));
            }
        };

        this.simulateServerClose = function () {
            this.readyState = 2; // CLOSED
            if (this.onerror) {
                this.onerror(new Event("error"));
            }
        };

        esInstances.push(this);
    } as unknown as typeof EventSource;

    Object.defineProperties(MockES, {
        CONNECTING: { value: 0 },
        OPEN: { value: 1 },
        CLOSED: { value: 2 },
    });

    (window as unknown as Record<string, unknown>).EventSource = MockES;
}

export function uninstallMockEventSource(): void {
    window.EventSource = OriginalEventSource;
}

export function lastES(): MockEventSourceInstance {
    return esInstances[esInstances.length - 1];
}

export function getESInstances(): MockEventSourceInstance[] {
    return esInstances;
}

// ---- Mock navigator.sendBeacon ----

export interface BeaconCall {
    url: string;
    data: unknown;
}

let beaconCalls: BeaconCall[];
let originalSendBeacon: typeof navigator.sendBeacon;

export function installMockBeacon(): void {
    beaconCalls = [];
    originalSendBeacon = navigator.sendBeacon;
    navigator.sendBeacon = (url: string, data?: BodyInit | null): boolean => {
        beaconCalls.push({ url, data: data ?? null });
        return true;
    };
}

export function uninstallMockBeacon(): void {
    navigator.sendBeacon = originalSendBeacon;
}

export function getBeaconCalls(): BeaconCall[] {
    return beaconCalls;
}

export function lastBeaconCall(): BeaconCall {
    return beaconCalls[beaconCalls.length - 1];
}

// ---- Mock ServiceWorker ----

export interface MockServiceWorkerContainer {
    register: jest.Mock;
    addEventListener: jest.Mock;
    controller: null;
    ready: Promise<unknown>;
    _listeners: Record<string, Function[]>;
    /** Test helper: simulate a message event from the service worker. */
    simulateMessage(data: unknown): void;
}

let originalServiceWorker: ServiceWorkerContainer;

export function installMockServiceWorker(): MockServiceWorkerContainer {
    originalServiceWorker = navigator.serviceWorker;
    const mock: MockServiceWorkerContainer = {
        register: jest.fn().mockResolvedValue({}),
        addEventListener: jest.fn(function (event: string, handler: Function) {
            if (!mock._listeners[event]) {
                mock._listeners[event] = [];
            }
            mock._listeners[event].push(handler);
        }),
        controller: null,
        ready: Promise.resolve({}),
        _listeners: {},
        simulateMessage(data: unknown) {
            const listeners = mock._listeners["message"] || [];
            listeners.forEach(fn => fn({ data }));
        },
    };

    Object.defineProperty(navigator, "serviceWorker", {
        value: mock,
        writable: true,
        configurable: true,
    });

    return mock;
}

export function uninstallMockServiceWorker(): void {
    Object.defineProperty(navigator, "serviceWorker", {
        value: originalServiceWorker,
        writable: true,
        configurable: true,
    });
}

// ---- DOM helpers ----

/**
 * Create a form with a ViewState hidden input, simulating a Faces form.
 */
export function createFacesForm(formId = "testForm", action = "/test/action"): HTMLFormElement {
    const form = document.createElement("form");
    form.id = formId;
    form.method = "post";
    form.action = action;

    const viewState = Object.assign(document.createElement("input"), {
        type: "hidden",
        name: VIEW_STATE_PARAM,
        value: "testViewState123",
    });
    form.appendChild(viewState);

    document.body.appendChild(form);

    // Ensure form["jakarta.faces.ViewState"] works in jsdom (named element access).
    // Some jsdom versions may not support this natively on HTMLFormElement.
    if (!(VIEW_STATE_PARAM in form)) {
        Object.defineProperty(form, VIEW_STATE_PARAM, { value: viewState, configurable: true });
    }

    return form;
}

/**
 * Install a mock faces object on window for modules that depend on it.
 */
export function installMockFaces(): Record<string, any> {
    const facesObj: Record<string, any> = {
        ajax: {
            request: jest.fn(),
        },
        getProjectStage: jest.fn().mockReturnValue("Production"),
        getViewState: jest.fn().mockReturnValue(""),
    };

    (window as any).faces = facesObj;
    return facesObj;
}

export function uninstallMockFaces(): void {
    delete (window as any).faces;
}
