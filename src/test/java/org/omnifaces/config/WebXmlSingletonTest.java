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
package org.omnifaces.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.omnifaces.test.WebappClassLoaders.createWebappClassLoader;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An <code>exception-type</code> in <code>web.xml</code> usually refers to an application class, which is not necessarily visible to the class loader of
 * OmniFaces itself, such as in Quarkus dev mode.
 * <p>
 * {@link WebXmlSingleton} is initialized only once per class loader, so this test can cover only one <code>web.xml</code>.
 */
class WebXmlSingletonTest {

	private static final String EXCEPTION_CLASS_NAME = "com.example.WebappException";
	private static final String EXCEPTION_SOURCE = "package com.example; public class WebappException extends RuntimeException {}";
	private static final String WEB_XML_PATH = "/WEB-INF/web.xml";

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
		TestFacesContext.uninstall();
	}

	@Test
	void exceptionTypeVisibleToContextClassLoaderOnlyIsMapped() throws Exception {
		URLClassLoader webappClassLoader = createWebappClassLoader(directory, EXCEPTION_CLASS_NAME, EXCEPTION_SOURCE);
		Thread.currentThread().setContextClassLoader(webappClassLoader);
		TestFacesContext.install(createServletContext(createWebXml(EXCEPTION_CLASS_NAME)));

		WebXml webXml = WebXmlSingleton.INSTANCE;
		Throwable exception = (Throwable) webappClassLoader.loadClass(EXCEPTION_CLASS_NAME).getConstructor().newInstance();

		assertEquals("/webapp-error.xhtml", webXml.findErrorPageLocation(exception));
		assertEquals("/error.xhtml", webXml.findErrorPageLocation(new RuntimeException()));
	}

	private URL createWebXml(String exceptionType) throws Exception {
		Path webXmlFile = directory.resolve("web.xml");
		Files.write(webXmlFile, String.format(""
			+ "<web-app>"
			+ "  <error-page>"
			+ "    <exception-type>%s</exception-type>"
			+ "    <location>/webapp-error.xhtml</location>"
			+ "  </error-page>"
			+ "  <error-page>"
			+ "    <error-code>500</error-code>"
			+ "    <location>/error.xhtml</location>"
			+ "  </error-page>"
			+ "</web-app>", exceptionType).getBytes(UTF_8));
		return webXmlFile.toUri().toURL();
	}

	private static ServletContext createServletContext(URL webXml) {
		return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(), new Class[] { ServletContext.class }, (proxy, method, args) ->
			"getResource".equals(method.getName()) && WEB_XML_PATH.equals(args[0]) ? webXml : null);
	}

	/**
	 * Makes the given servlet context available via <code>Servlets#getContext()</code>.
	 */
	private static class TestFacesContext extends FacesContextWrapper {

		private final ExternalContext externalContext;

		private TestFacesContext(ServletContext servletContext) {
			super(null);
			externalContext = new ExternalContextWrapper(null) {
				@Override
				public Object getContext() {
					return servletContext;
				}
			};
		}

		@Override
		public ExternalContext getExternalContext() {
			return externalContext;
		}

		static void install(ServletContext servletContext) {
			setCurrentInstance(new TestFacesContext(servletContext));
		}

		static void uninstall() {
			setCurrentInstance(null);
		}
	}

}
