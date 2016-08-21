/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.test;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.openqa.selenium.WebDriver;

public abstract class OmniFacesIT {

	@Drone
	protected WebDriver browser;

	@ArquillianResource
	protected URL contextPath;

	@Before
	public void init() {
		browser.get(contextPath + getClass().getSimpleName() + ".xhtml");
	}

	public static class ArchiveBuilder {

		private WebArchive archive;
		private boolean facesConfigSet;
		private boolean webXmlSet;

		public static <T extends OmniFacesIT> WebArchive createWebArchive(Class<T> testClass) {
			return buildWebArchive(testClass).createDeployment();
		}

		public static <T extends OmniFacesIT> ArchiveBuilder buildWebArchive(Class<T> testClass) {
			return new ArchiveBuilder(testClass);
		}

		private <T extends OmniFacesIT> ArchiveBuilder(Class<T> testClass) {
			String packageName = testClass.getPackage().getName();
			String className = testClass.getSimpleName();
			String warName = className + ".war";
			String xhtmlName = packageName + "/" + className + ".xhtml";

			archive = create(WebArchive.class, warName)
				.addPackage(packageName)
				.deleteClass(testClass)
				.addAsWebResource(xhtmlName)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				.addAsLibrary(new File(System.getProperty("omnifaces.jar")));
		}

		public ArchiveBuilder withFacesConfig(FacesConfig facesConfig) {
			if (facesConfigSet) {
				throw new IllegalStateException("There can be only one faces-config.xml");
			}

			archive.addAsWebInfResource("WEB-INF/faces-config.xml/" + facesConfig.name() + ".xml", "faces-config.xml");
			facesConfigSet = true;
			return this;
		}

		public ArchiveBuilder withWebXml(WebXml webXml) {
			if (webXmlSet) {
				throw new IllegalStateException("There can be only one web.xml");
			}

			archive.setWebXML("WEB-INF/web.xml/" + webXml.name() + ".xml");

			if (webXml == WebXml.withErrorPage) {
				archive.addAsWebInfResource("WEB-INF/500.xhtml");
			}

			webXmlSet = true;
			return this;
		}

		public WebArchive createDeployment() {
			if (!facesConfigSet) {
				withFacesConfig(FacesConfig.basic);
			}

			if (!webXmlSet) {
				withWebXml(WebXml.basic);
			}

			return archive;
		}
	}

	public static enum FacesConfig {
		basic,
		withFullAjaxExceptionHandler;
	}

	public static enum WebXml {
		basic,
		withErrorPage;
	}

}