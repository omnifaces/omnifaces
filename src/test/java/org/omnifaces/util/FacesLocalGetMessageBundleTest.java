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
package org.omnifaces.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Locale.ENGLISH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The message bundle in <code>faces-config.xml</code> usually refers to an application resource, which is not necessarily visible to the class loader of
 * OmniFaces itself, such as in Quarkus dev mode.
 */
class FacesLocalGetMessageBundleTest {

	private static final String MESSAGE_BUNDLE = "com.example.messages";

	@TempDir
	Path directory;

	private ClassLoader originalClassLoader;

	@BeforeEach
	void setUp() {
		originalClassLoader = Thread.currentThread().getContextClassLoader();
	}

	@AfterEach
	void tearDown() {
		Thread.currentThread().setContextClassLoader(originalClassLoader);
	}

	@Test
	void messageBundleVisibleToContextClassLoaderOnlyIsLoaded() throws Exception {
		Path messageBundle = directory.resolve(MESSAGE_BUNDLE.replace('.', '/') + ".properties");
		Files.createDirectories(messageBundle.getParent());
		Files.write(messageBundle, "greeting=hello".getBytes(ISO_8859_1));
		Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] { directory.toUri().toURL() }, FacesLocal.class.getClassLoader()));

		assertEquals("hello", FacesLocal.getMessageBundle(createFacesContext(MESSAGE_BUNDLE)).getString("greeting"));
	}

	@Test
	void unknownMessageBundleIsRejected() {
		FacesContext context = createFacesContext(MESSAGE_BUNDLE);

		assertThrows(MissingResourceException.class, () -> FacesLocal.getMessageBundle(context));
	}

	@Test
	void absentMessageBundleIsNull() {
		assertNull(FacesLocal.getMessageBundle(createFacesContext(null)));
	}

	private static FacesContext createFacesContext(String messageBundle) {
		UIViewRoot viewRoot = new UIViewRoot() {
			@Override
			public Locale getLocale() {
				return ENGLISH;
			}
		};

		Application application = new ApplicationWrapper(null) {
			@Override
			public String getMessageBundle() {
				return messageBundle;
			}
		};

		return new FacesContextWrapper(null) {
			@Override
			public Application getApplication() {
				return application;
			}

			@Override
			public UIViewRoot getViewRoot() {
				return viewRoot;
			}
		};
	}

}
