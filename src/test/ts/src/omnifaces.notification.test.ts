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
 * Tests for the OmniFaces.Notification namespace.
 */

import { loadOmniFacesJs } from "../test-setup";
import { installMockServiceWorker, uninstallMockServiceWorker, MockServiceWorkerContainer } from "../test-helpers";

beforeAll(() => loadOmniFacesJs());

const notification = () => OmniFaces.Notification as Record<string, Function>;

// -- Helpers --

let mockSW: MockServiceWorkerContainer;
let originalNotification: typeof window.Notification;
let showNotificationMock: jest.Mock;

function installMockNotification(permission: NotificationPermission = "granted") {
    originalNotification = window.Notification;
    showNotificationMock = jest.fn();

    const mockRegistration = { showNotification: showNotificationMock };
    mockSW = installMockServiceWorker();
    mockSW.ready = Promise.resolve(mockRegistration);

    const mockNotificationClass = jest.fn() as unknown as typeof window.Notification;
    Object.defineProperty(mockNotificationClass, "permission", { value: permission, configurable: true });
    mockNotificationClass.requestPermission = jest.fn().mockResolvedValue(permission);

    Object.defineProperty(window, "Notification", {
        value: mockNotificationClass,
        writable: true,
        configurable: true,
    });
}

function uninstallMockNotification() {
    Object.defineProperty(window, "Notification", {
        value: originalNotification,
        writable: true,
        configurable: true,
    });
    uninstallMockServiceWorker();
}

// -- Tests --

describe("OmniFaces.Notification.init", () => {

    afterEach(() => {
        uninstallMockNotification();
    });

    test("initializes without error", () => {
        installMockNotification("granted");
        notification().init("ch1", { icon: "/icon.png" });
    });

    test("calls onunsupported when Notification API is missing", () => {
        const onunsupported = jest.fn();
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        notification().init("ch1", { onunsupported });

        expect(onunsupported).toHaveBeenCalled();

        Object.defineProperty(window, "Notification", {
            value: origNotification,
            writable: true,
            configurable: true,
        });
        uninstallMockServiceWorker();
    });

    test("calls onunsupported when service worker is missing", () => {
        const onunsupported = jest.fn();
        originalNotification = window.Notification;

        const mockNotificationClass = jest.fn() as unknown as typeof window.Notification;
        Object.defineProperty(mockNotificationClass, "permission", { value: "granted", configurable: true });
        Object.defineProperty(window, "Notification", {
            value: mockNotificationClass,
            writable: true,
            configurable: true,
        });

        const origSW = navigator.serviceWorker;
        Object.defineProperty(navigator, "serviceWorker", {
            value: undefined,
            writable: true,
            configurable: true,
        });

        notification().init("ch1", { onunsupported });

        expect(onunsupported).toHaveBeenCalled();

        Object.defineProperty(navigator, "serviceWorker", {
            value: origSW,
            writable: true,
            configurable: true,
        });
        Object.defineProperty(window, "Notification", {
            value: originalNotification,
            writable: true,
            configurable: true,
        });
    });
});

describe("OmniFaces.Notification.show", () => {

    afterEach(() => {
        uninstallMockNotification();
    });

    test("shows notification with title and body", async () => {
        installMockNotification("granted");
        notification().init("ch1", { tag: "custom" });
        notification().show("ch1", "Test Title", "Test body");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Test Title", expect.objectContaining({
            body: "Test body",
            tag: "custom",
        }));
    });

    test("uses component defaults for icon, requireInteraction, silent", async () => {
        installMockNotification("granted");
        notification().init("ch1", { icon: "/default.png", requireInteraction: true, silent: true });
        notification().show("ch1", "Title", "Body");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Title", expect.objectContaining({
            icon: "/default.png",
            requireInteraction: true,
            silent: true,
        }));
    });

    test("includes channel and url in data", async () => {
        installMockNotification("granted");
        notification().init("ch1", {});
        notification().show("ch1", "Click me", "Opens a link.", "https://example.com");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Click me", expect.objectContaining({
            data: { channel: "ch1", url: "https://example.com" },
        }));
    });

    test("includes channel in data without url", async () => {
        installMockNotification("granted");
        notification().init("ch1", {});
        notification().show("ch1", "Title", "Body");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Title", expect.objectContaining({
            data: { channel: "ch1", url: undefined },
        }));
    });

    test("uses correct defaults for each channel", async () => {
        installMockNotification("granted");
        notification().init("ch1", { tag: "tag1", icon: "/icon1.png" });
        notification().init("ch2", { tag: "tag2", icon: "/icon2.png" });

        notification().show("ch1", "Title 1", "Body 1");
        notification().show("ch2", "Title 2", "Body 2");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Title 1", expect.objectContaining({
            tag: "tag1",
            icon: "/icon1.png",
        }));
        expect(showNotificationMock).toHaveBeenCalledWith("Title 2", expect.objectContaining({
            tag: "tag2",
            icon: "/icon2.png",
        }));
    });

    test("does nothing when permission is not granted", async () => {
        installMockNotification("denied");
        notification().init("ch1", {});
        notification().show("ch1", "Title", "Body");

        await mockSW.ready;

        expect(showNotificationMock).not.toHaveBeenCalled();
    });

    test("shows notification with title only", async () => {
        installMockNotification("granted");
        notification().init("ch1", {});
        notification().show("ch1", "Title Only");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Title Only", expect.objectContaining({
            body: undefined,
        }));
    });

    test("does nothing when Notification API is missing", () => {
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        notification().init("ch1", {});
        expect(() => notification().show("ch1", "Title")).not.toThrow();

        Object.defineProperty(window, "Notification", {
            value: origNotification,
            writable: true,
            configurable: true,
        });
        uninstallMockServiceWorker();
    });
});

describe("OmniFaces.Notification.requestPermission", () => {

    afterEach(() => {
        uninstallMockNotification();
    });

    test("returns permission and calls onpermissionchange for specific channel", async () => {
        installMockNotification("granted");
        const onpermissionchange = jest.fn();
        notification().init("ch1", { onpermissionchange });

        const result = await notification().requestPermission("ch1");

        expect(result).toBe("granted");
        expect(onpermissionchange).toHaveBeenCalledWith("granted");
    });

    test("calls onpermissionchange for all channels when no channel specified", async () => {
        installMockNotification("granted");
        const onpermissionchange1 = jest.fn();
        const onpermissionchange2 = jest.fn();
        notification().init("ch1", { onpermissionchange: onpermissionchange1 });
        notification().init("ch2", { onpermissionchange: onpermissionchange2 });

        const result = await notification().requestPermission();

        expect(result).toBe("granted");
        expect(onpermissionchange1).toHaveBeenCalledWith("granted");
        expect(onpermissionchange2).toHaveBeenCalledWith("granted");
    });

    test("returns denied when Notification API is missing", async () => {
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        notification().init("ch1", {});
        const result = await notification().requestPermission("ch1");

        expect(result).toBe("denied");

        Object.defineProperty(window, "Notification", {
            value: origNotification,
            writable: true,
            configurable: true,
        });
        uninstallMockServiceWorker();
    });
});

describe("OmniFaces.Notification.getPermission", () => {

    afterEach(() => {
        uninstallMockNotification();
    });

    test("returns current permission", () => {
        installMockNotification("denied");
        expect(notification().getPermission()).toBe("denied");
    });

    test("returns unsupported when Notification API is missing", () => {
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        expect(notification().getPermission()).toBe("unsupported");

        Object.defineProperty(window, "Notification", {
            value: origNotification,
            writable: true,
            configurable: true,
        });
        uninstallMockServiceWorker();
    });
});

describe("OmniFaces.Notification event handlers", () => {

    afterEach(() => {
        uninstallMockNotification();
    });

    test("onclick is called for matching channel", () => {
        installMockNotification("granted");
        const onclick = jest.fn();
        notification().init("ch1", { onclick });

        window.dispatchEvent(new CustomEvent("omnifaces.notificationclick", {
            detail: { tag: "test", data: { channel: "ch1" } },
        }));

        expect(onclick).toHaveBeenCalledTimes(1);
        expect(onclick.mock.calls[0][0].detail).toEqual({ tag: "test", data: { channel: "ch1" } });
    });

    test("onclose is called for matching channel", () => {
        installMockNotification("granted");
        const onclose = jest.fn();
        notification().init("ch1", { onclose });

        window.dispatchEvent(new CustomEvent("omnifaces.notificationclose", {
            detail: { tag: "test", data: { channel: "ch1" } },
        }));

        expect(onclose).toHaveBeenCalledTimes(1);
        expect(onclose.mock.calls[0][0].detail).toEqual({ tag: "test", data: { channel: "ch1" } });
    });

    test("onclick is not called for non-matching channel", () => {
        installMockNotification("granted");
        const onclick = jest.fn();
        notification().init("ch1", { onclick });

        window.dispatchEvent(new CustomEvent("omnifaces.notificationclick", {
            detail: { tag: "test", data: { channel: "ch2" } },
        }));

        expect(onclick).not.toHaveBeenCalled();
    });

    test("dispatches to correct channel when multiple channels exist", () => {
        installMockNotification("granted");
        const onclick1 = jest.fn();
        const onclick2 = jest.fn();
        notification().init("ch1", { onclick: onclick1 });
        notification().init("ch2", { onclick: onclick2 });

        window.dispatchEvent(new CustomEvent("omnifaces.notificationclick", {
            detail: { tag: "test", data: { channel: "ch2" } },
        }));

        expect(onclick1).not.toHaveBeenCalled();
        expect(onclick2).toHaveBeenCalledTimes(1);
    });
});
