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
 * Tests for the OmniFaces.Push namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { installMockWebSocket, uninstallMockWebSocket, lastWS, getWSInstances } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const push = () => OmniFaces.Push as Record<string, Function>;

// ---- init ----

describe("OmniFaces.Push.init", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("creates socket for new channel", () => {
        push().init(false, "", "testChannel?token=abc", null, null, null, null, {}, false);
        expect(() => push().open("testChannel")).not.toThrow();
    });

    test("does not create duplicate socket for same channel", () => {
        push().init(false, "", "dupChannel?t=1", null, null, null, null, {}, false);
        push().init(false, "", "dupChannel?t=2", null, null, null, null, {}, false);
        push().open("dupChannel");
        expect(getWSInstances().length).toBe(1);
    });

    test("autoconnect=true opens socket immediately", () => {
        push().init(false, "", "autoChannel", null, null, null, null, {}, true);
        expect(getWSInstances().length).toBe(1);
    });

    test("autoconnect=false does not open socket", () => {
        push().init(false, "", "noAutoChannel", null, null, null, null, {}, false);
        expect(getWSInstances().length).toBe(0);
    });

    test("calls onclose with -1 when WebSocket is not supported", () => {
        const origWS = window.WebSocket;
        delete (window as unknown as Record<string, unknown>).WebSocket;

        const closeCalls: unknown[][] = [];
        push().init(false, "", "noWsChannel", null, null, null,
            (code: number, channel: string) => closeCalls.push([code, channel]),
            {}, false);

        expect(closeCalls.length).toBe(1);
        expect(closeCalls[0][0]).toBe(-1);
        expect(closeCalls[0][1]).toBe("noWsChannel");

        (window as unknown as Record<string, unknown>).WebSocket = origWS;
    });
});

// ---- init: host URL resolution ----

describe("OmniFaces.Push.init: host URL resolution", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("empty host defaults to window.location.host", () => {
        push().init(false, "", "hostTest1", null, null, null, null, {}, true);
        expect(lastWS().url).toContain(window.location.host);
        expect(lastWS().url).toContain("/omnifaces.socket/");
    });

    test("host starting with / prepends window.location.host", () => {
        push().init(false, "/myapp", "hostTest2", null, null, null, null, {}, true);
        expect(lastWS().url).toContain(window.location.host + "/myapp");
    });

    test("host starting with : prepends window.location.hostname", () => {
        push().init(false, ":8443/myapp", "hostTest3", null, null, null, null, {}, true);
        expect(lastWS().url).toContain(window.location.hostname + ":8443/myapp");
    });

    test("full host is used as-is", () => {
        push().init(false, "example.com:8080/ctx", "hostTest4", null, null, null, null, {}, true);
        expect(lastWS().url).toContain("example.com:8080/ctx/omnifaces.socket/");
    });

    test("URL includes channel URI", () => {
        push().init(false, "", "myChannel?token=xyz", null, null, null, null, {}, true);
        expect(lastWS().url).toContain("myChannel?token=xyz");
    });
});

// ---- open ----

describe("OmniFaces.Push.open", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("throws for unknown channel", () => {
        expect(() => push().open("unknownChannel")).toThrow("Unknown channel: unknownChannel");
    });

    test("creates WebSocket on open", () => {
        push().init(false, "", "openTest", null, null, null, null, {}, false);
        push().open("openTest");
        expect(getWSInstances().length).toBe(1);
    });

    test("does not create second WebSocket if already open", () => {
        push().init(false, "", "openTest2", null, null, null, null, {}, false);
        push().open("openTest2");
        lastWS().simulateOpen();
        push().open("openTest2");
        expect(getWSInstances().length).toBe(1);
    });
});

// ---- close ----

describe("OmniFaces.Push.close", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("throws for unknown channel", () => {
        expect(() => push().close("unknownChannel")).toThrow("Unknown channel: unknownChannel");
    });

    test("closes an open socket", () => {
        push().init(false, "", "closeTest", null, null, null, null, {}, false);
        push().open("closeTest");
        lastWS().simulateOpen();
        expect(() => push().close("closeTest")).not.toThrow();
        expect(lastWS().readyState).toBe(3);
    });

    test("close before open does not throw", () => {
        push().init(false, "", "closeTest2", null, null, null, null, {}, false);
        expect(() => push().close("closeTest2")).not.toThrow();
    });
});

// ---- onopen callback ----

describe("OmniFaces.Push: onopen callback", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("onopen is called with channel on first connect", () => {
        const calls: unknown[] = [];
        push().init(false, "", "onOpenCh", (ch: string) => calls.push(ch), null, null, null, {}, false);
        push().open("onOpenCh");
        lastWS().simulateOpen();
        expect(calls).toEqual(["onOpenCh"]);
    });

    test("onopen is NOT called on reconnect", () => {
        const calls: unknown[] = [];
        push().init(false, "", "onOpenReconn", (ch: string) => calls.push(ch), null, null, null, {}, false);
        push().open("onOpenReconn");
        lastWS().simulateOpen();
        expect(calls.length).toBe(1);

        // Simulate unexpected close (triggers reconnect)
        lastWS().simulateClose(1006);
        expect(calls.length).toBe(1);
    });

    test("onopen resolves string function name from window", () => {
        (window as any).__testPushOnOpen = jest.fn();
        push().init(false, "", "onOpenStr", "__testPushOnOpen", null, null, null, {}, false);
        push().open("onOpenStr");
        lastWS().simulateOpen();
        expect((window as any).__testPushOnOpen).toHaveBeenCalledWith("onOpenStr");
        delete (window as any).__testPushOnOpen;
    });

    test("onopen with null does not throw", () => {
        push().init(false, "", "onOpenNull", null, null, null, null, {}, false);
        push().open("onOpenNull");
        expect(() => lastWS().simulateOpen()).not.toThrow();
    });
});

// ---- onmessage callback ----

describe("OmniFaces.Push: onmessage callback", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("onmessage receives parsed data, channel and raw event", () => {
        const calls: unknown[][] = [];
        push().init(false, "", "msgCh1",
            null, (message: unknown, channel: string, event: unknown) => calls.push([message, channel, event]),
            null, null, {}, false);
        push().open("msgCh1");
        lastWS().simulateOpen();
        lastWS().simulateMessage("hello");

        expect(calls.length).toBe(1);
        expect(calls[0][0]).toBe("hello");
        expect(calls[0][1]).toBe("msgCh1");
        expect(calls[0][2]).toBeDefined();
    });

    test("onmessage receives object data", () => {
        const calls: unknown[] = [];
        push().init(false, "", "msgCh2",
            null, (message: unknown) => calls.push(message),
            null, null, {}, false);
        push().open("msgCh2");
        lastWS().simulateOpen();
        lastWS().simulateMessage({ key: "value" });
        expect(calls[0]).toEqual({ key: "value" });
    });

    test("onmessage with null does not throw", () => {
        push().init(false, "", "msgCh3", null, null, null, null, {}, false);
        push().open("msgCh3");
        lastWS().simulateOpen();
        expect(() => lastWS().simulateMessage("test")).not.toThrow();
    });
});

// ---- behaviors ----

describe("OmniFaces.Push: behaviors", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("behavior function is invoked when message matches key", () => {
        const called: string[] = [];
        const behaviors = {
            update: [() => called.push("update1"), () => called.push("update2")],
        };
        push().init(false, "", "behCh1", null, null, null, null, behaviors, false);
        push().open("behCh1");
        lastWS().simulateOpen();
        lastWS().simulateMessage("update");
        expect(called).toEqual(["update1", "update2"]);
    });

    test("behavior function is NOT invoked when message does not match", () => {
        const called: string[] = [];
        const behaviors = {
            update: [() => called.push("update")],
        };
        push().init(false, "", "behCh2", null, null, null, null, behaviors, false);
        push().open("behCh2");
        lastWS().simulateOpen();
        lastWS().simulateMessage("delete");
        expect(called).toEqual([]);
    });

    test("empty behaviors object does not cause errors", () => {
        push().init(false, "", "behCh3", null, null, null, null, {}, false);
        push().open("behCh3");
        lastWS().simulateOpen();
        expect(() => lastWS().simulateMessage("anything")).not.toThrow();
    });
});

// ---- onerror callback (reconnect attempt) ----

describe("OmniFaces.Push: onerror callback", () => {

    beforeEach(() => {
        jest.useFakeTimers();
        installMockWebSocket();
    });
    afterEach(() => {
        uninstallMockWebSocket();
        jest.useRealTimers();
    });

    test("onerror is called on unexpected close with reconnect pending", () => {
        const errors: unknown[][] = [];
        push().init(false, "", "errCh1",
            null, null,
            (code: number, channel: string, event: unknown) => errors.push([code, channel, event]),
            null, {}, false);
        push().open("errCh1");
        lastWS().simulateOpen();
        lastWS().simulateClose(1006, "Abnormal");

        expect(errors.length).toBe(1);
        expect(errors[0][0]).toBe(1006);
        expect(errors[0][1]).toBe("errCh1");
    });

    test("reconnect creates new WebSocket after interval", () => {
        push().init(false, "", "errCh2", null, null, null, null, {}, false);
        push().open("errCh2");
        lastWS().simulateOpen();
        expect(getWSInstances().length).toBe(1);

        lastWS().simulateClose(1006, "Abnormal");
        jest.advanceTimersByTime(0); // First reconnect at 0ms (500 * 0)
        expect(getWSInstances().length).toBe(2);
    });
});

// ---- onclose callback (final close) ----

describe("OmniFaces.Push: onclose callback", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("onclose is called when server sends Expired reason", () => {
        const closes: unknown[][] = [];
        push().init(false, "", "clExpired",
            null, null, null,
            (code: number, channel: string, event: unknown) => closes.push([code, channel, event]),
            {}, false);
        push().open("clExpired");
        lastWS().simulateOpen();
        lastWS().simulateClose(1000, "Expired");

        expect(closes.length).toBe(1);
        expect(closes[0][0]).toBe(1000);
        expect(closes[0][1]).toBe("clExpired");
    });

    test("onclose is called when server sends Unknown channel (code 1008)", () => {
        const closes: unknown[][] = [];
        push().init(false, "", "clUnknown",
            null, null, null,
            (code: number, channel: string) => closes.push([code, channel]),
            {}, false);
        push().open("clUnknown");
        lastWS().simulateOpen();
        lastWS().simulateClose(1008, "Unknown channel");

        expect(closes.length).toBe(1);
        expect(closes[0][0]).toBe(1008);
    });

    test("code 1000 without Expired reason triggers reconnect, not onclose", () => {
        const closes: unknown[][] = [];
        const errors: unknown[][] = [];
        push().init(false, "", "clNoExpired",
            null, null,
            (code: number, channel: string) => errors.push([code, channel]),
            (code: number, channel: string) => closes.push([code, channel]),
            {}, false);
        push().open("clNoExpired");
        lastWS().simulateOpen();
        lastWS().simulateClose(1000, "");

        expect(closes.length).toBe(0);
        expect(errors.length).toBe(1);
    });
});

// ---- reconnect behavior ----

describe("OmniFaces.Push: reconnect behavior", () => {

    beforeEach(() => {
        jest.useFakeTimers();
        installMockWebSocket();
    });
    afterEach(() => {
        uninstallMockWebSocket();
        jest.useRealTimers();
    });

    test("reconnect interval increases cumulatively (500ms * attempt)", () => {
        push().init(false, "", "rcInterval", null, null, null, null, {}, false);
        push().open("rcInterval");
        lastWS().simulateOpen();

        // First disconnect: delay = 500 * 0 = 0ms
        lastWS().simulateClose(1006);
        jest.advanceTimersByTime(0);
        expect(getWSInstances().length).toBe(2);

        // Second disconnect: delay = 500 * 1 = 500ms
        lastWS().simulateClose(1006);
        jest.advanceTimersByTime(499);
        expect(getWSInstances().length).toBe(2);
        jest.advanceTimersByTime(1);
        expect(getWSInstances().length).toBe(3);
    });

    test("stops reconnecting after 25 attempts and calls onclose", () => {
        const closes: unknown[][] = [];
        push().init(false, "", "rcMax",
            null, null, null,
            (code: number, channel: string) => closes.push([code, channel]),
            {}, false);
        push().open("rcMax");
        lastWS().simulateOpen();

        for (let i = 0; i < 25; i++) {
            lastWS().simulateClose(1006);
            jest.advanceTimersByTime(500 * i);
        }

        lastWS().simulateClose(1006);
        expect(closes.length).toBe(1);
        expect(closes[0][0]).toBe(1006);
    });

    test("successful reconnect resets attempt counter", () => {
        push().init(false, "", "rcReset", null, null, null, null, {}, false);
        push().open("rcReset");
        lastWS().simulateOpen();

        lastWS().simulateClose(1006);
        jest.advanceTimersByTime(0);
        expect(getWSInstances().length).toBe(2);

        lastWS().simulateOpen(); // Reset counter

        lastWS().simulateClose(1006);
        jest.advanceTimersByTime(0); // Should reconnect at 0ms again
        expect(getWSInstances().length).toBe(3);
    });

    test("explicit close prevents reconnect", () => {
        push().init(false, "", "rcExplicit", null, null, null, null, {}, false);
        push().open("rcExplicit");
        lastWS().simulateOpen();

        push().close("rcExplicit");
        jest.advanceTimersByTime(10000);
        expect(getWSInstances().length).toBe(1);
    });
});

// ---- function resolution ----

describe("OmniFaces.Push: function resolution", () => {

    beforeEach(() => installMockWebSocket());
    afterEach(() => uninstallMockWebSocket());

    test("nonexistent string callback resolves to noop (no error)", () => {
        push().init(false, "", "resNoop", "nonExistentGlobalFn", null, null, null, {}, false);
        push().open("resNoop");
        expect(() => lastWS().simulateOpen()).not.toThrow();
    });

    test("undefined callback resolves to noop (no error)", () => {
        push().init(false, "", "resUndef", undefined, undefined, undefined, undefined, {}, false);
        push().open("resUndef");
        lastWS().simulateOpen();
        expect(() => lastWS().simulateMessage("test")).not.toThrow();
    });
});
