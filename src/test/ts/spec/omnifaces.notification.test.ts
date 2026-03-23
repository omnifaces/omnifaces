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
        notification().init({ icon: "/icon.png" });
    });

    test("calls onunsupported when Notification API is missing", () => {
        const onunsupported = jest.fn();
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        notification().init({ onunsupported });

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

        notification().init({ onunsupported });

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

    test("shows notification with title and options", async () => {
        installMockNotification("granted");
        notification().init({});
        notification().show("Test Title", { body: "Test body", tag: "custom" });

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Test Title", expect.objectContaining({
            body: "Test body",
            tag: "custom",
        }));
    });

    test("falls back to default options", async () => {
        installMockNotification("granted");
        notification().init({ icon: "/default.png" });
        notification().show("Title");

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Title", expect.objectContaining({
            icon: "/default.png",
        }));
    });

    test("overrides default options with explicit options", async () => {
        installMockNotification("granted");
        notification().init({ icon: "/default.png" });
        notification().show("Title", { icon: "/custom.png" });

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Title", expect.objectContaining({
            icon: "/custom.png",
        }));
    });

    test("passes data option for URL action", async () => {
        installMockNotification("granted");
        notification().init({});
        notification().show("Click me", { data: { url: "https://example.com" } });

        await mockSW.ready;

        expect(showNotificationMock).toHaveBeenCalledWith("Click me", expect.objectContaining({
            data: { url: "https://example.com" },
        }));
    });

    test("does nothing when permission is not granted", async () => {
        installMockNotification("denied");
        notification().init({});
        notification().show("Title");

        await mockSW.ready;

        expect(showNotificationMock).not.toHaveBeenCalled();
    });

    test("does nothing when Notification API is missing", () => {
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        notification().init({});
        expect(() => notification().show("Title")).not.toThrow();

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

    test("returns permission and calls onpermissionchange", async () => {
        installMockNotification("granted");
        const onpermissionchange = jest.fn();
        notification().init({ onpermissionchange });

        const result = await notification().requestPermission();

        expect(result).toBe("granted");
        expect(onpermissionchange).toHaveBeenCalledWith("granted");
    });

    test("returns denied when Notification API is missing", async () => {
        const origNotification = window.Notification;
        delete (window as any).Notification;
        mockSW = installMockServiceWorker();

        notification().init({});
        const result = await notification().requestPermission();

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

    test("onclick is called on notificationclick event", () => {
        installMockNotification("granted");
        const onclick = jest.fn();
        notification().init({ onclick });

        window.dispatchEvent(new CustomEvent("omnifaces.notificationclick", {
            detail: { tag: "test", data: null },
        }));

        expect(onclick).toHaveBeenCalledTimes(1);
        expect(onclick.mock.calls[0][0].detail).toEqual({ tag: "test", data: null });
    });

    test("onclose is called on notificationclose event", () => {
        installMockNotification("granted");
        const onclose = jest.fn();
        notification().init({ onclose });

        window.dispatchEvent(new CustomEvent("omnifaces.notificationclose", {
            detail: { tag: "test", data: null },
        }));

        expect(onclose).toHaveBeenCalledTimes(1);
        expect(onclose.mock.calls[0][0].detail).toEqual({ tag: "test", data: null });
    });
});
