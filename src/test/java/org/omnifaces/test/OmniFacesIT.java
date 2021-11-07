/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.arquillian.graphene.Graphene.guardNoRequest;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withMessageBundle;
import static org.omnifaces.util.Utils.isOneOf;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@RunWith(Arquillian.class)
public abstract class OmniFacesIT {

	@Drone
	protected WebDriver browser;

	@ArquillianResource
	protected URL baseURL;

	@Before
	public void init() {
		open(getClass().getSimpleName() + ".xhtml");
	}

	protected void refresh() {
		init();
	}

	protected void open(String pageName) {
		browser.get(baseURL + pageName);
		waitGui(browser);
	}

	protected String openNewTab(WebElement elementWhichOpensNewTab) {
		Set<String> oldTabs = browser.getWindowHandles();
		guardNoRequest(elementWhichOpensNewTab).click();
		Set<String> newTabs = new HashSet<>(browser.getWindowHandles());
		newTabs.removeAll(oldTabs); // Just to be sure; it's nowhere in Selenium API specified whether tabs are ordered.
		String newTab = newTabs.iterator().next();
		browser.switchTo().window(newTab);
		waitGui(browser);
		return newTab;
	}

	protected void openWithQueryString(String queryString) {
		open(getClass().getSimpleName() + ".xhtml?" + queryString);
	}

	protected void openWithHashString(String hashString) {
		open(getClass().getSimpleName() + ".xhtml?" + System.currentTimeMillis() + "#" + hashString); // Query string trick is necessary because Selenium driver may not forcibly reload page.
	}

	protected void closeCurrentTabAndSwitchTo(String tabToSwitch) {
		browser.close();
		browser.switchTo().window(tabToSwitch);
	}

	/**
	 * Work around because Selenium WebDriver API doesn't support triggering JS events.
	 */
	protected void triggerOnchange(WebElement input, WebElement messages) {
		clearMessages(messages);
		executeScript("document.getElementById('" + input.getAttribute("id") + "').onchange();");
		waitUntilTextContent(messages);
	}

	/**
	 * Work around because Selenium WebDriver API doesn't recognize iframe based ajax upload in guard.
	 */
	protected void guardAjaxUpload(WebElement submit, WebElement messages) {
		clearMessages(messages);
		submit.click();
		waitUntilTextContent(messages);
	}

	protected void waitUntilTextContent(WebElement element) {
		waitGui(browser).withTimeout(3, SECONDS).until().element(element).text().not().equalTo("");
	}

	@SuppressWarnings("unchecked")
	protected <T> T executeScript(String script) {
		return (T) ((JavascriptExecutor) browser).executeScript(script);
	}

	private void clearMessages(WebElement messages) {
		executeScript("document.getElementById('" + messages.getAttribute("id") + "').innerHTML='';");
	}

	protected static String stripJsessionid(String url) {
		return url.split(";jsessionid=", 2)[0];
	}

	protected static boolean isTomee() {
		return "tomee".equals(System.getProperty("profile.id"));
	}

	protected static boolean isMojarra() {
		return isOneOf(System.getProperty("profile.id"), "wildfly", "payara");
	}

	protected static <T extends OmniFacesIT> WebArchive createWebArchive(Class<T> testClass) {
		return buildWebArchive(testClass).createDeployment();
	}

	protected static <T extends OmniFacesIT> ArchiveBuilder buildWebArchive(Class<T> testClass) {
		return new ArchiveBuilder(testClass);
	}

	protected static class ArchiveBuilder {

		private WebArchive archive;
		private boolean facesConfigSet;
		private boolean webXmlSet;
		private boolean primeFacesSet;

		private <T extends OmniFacesIT> ArchiveBuilder(Class<T> testClass) {
			String packageName = testClass.getPackage().getName();
			String className = testClass.getSimpleName();
			String warName = className + ".war";

			archive = create(WebArchive.class, warName)
				.addPackage(packageName)
				.deleteClass(testClass)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				.addAsLibrary(new File(System.getProperty("omnifaces.jar")));

			String warLibraries = System.getProperty("war.libraries");

			if (warLibraries != null) {
				archive.addAsLibraries(Maven.resolver().resolve(warLibraries.split("\\s*,\\s*")).withTransitivity().asFile());
			}

			addWebResources(new File(testClass.getClassLoader().getResource(packageName).getFile()), "");
		}

		private void addWebResources(File root, String directory) {
			for (File file : root.listFiles()) {
				String path = directory + "/" + file.getName();

				if (file.isFile()) {
					archive.addAsWebResource(file, path);
				}
				else if (file.isDirectory()) {
					addWebResources(file, path);
				}
			}
		}

		public ArchiveBuilder withFacesConfig(FacesConfig facesConfig) {
			if (facesConfigSet) {
				throw new IllegalStateException("There can be only one faces-config.xml");
			}

			archive.addAsWebInfResource("WEB-INF/faces-config.xml/" + facesConfig.name() + ".xml", "faces-config.xml");

			if (facesConfig == withMessageBundle) {
				archive.addAsResource("messages.properties");
			}

			facesConfigSet = true;
			return this;
		}

		public ArchiveBuilder withWebXml(WebXml webXml) {
			if (webXmlSet) {
				throw new IllegalStateException("There can be only one web.xml");
			}

			archive.setWebXML("WEB-INF/web.xml/" + webXml.name() + ".xml");

			switch (webXml) {
				case withErrorPage:
					archive.addAsWebInfResource("WEB-INF/500.xhtml");
					break;
				case withFacesViews:
				case withMultiViews:
					archive.addAsWebInfResource("WEB-INF/404.xhtml");
					break;
				default:
					break;
			}

			webXmlSet = true;
			return this;
		}

		public ArchiveBuilder withPrimeFaces() {
			if (primeFacesSet) {
				throw new IllegalStateException("There can be only one PrimeFaces library");
			}

			MavenResolverSystem maven = Maven.resolver();
			archive.addAsLibraries(maven.resolve("org.primefaces:primefaces:8.0").withTransitivity().asFile());
			primeFacesSet = true;
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
		withFullAjaxExceptionHandler,
		withCombinedResourceHandler,
		withMessageBundle,
		withCDNResourceHandler,
		withViewExpiredExceptionHandler,
		withViewResourceHandler;
	}

	public static enum WebXml {
		basic,
		distributable,
		withErrorPage,
		withFacesViews,
		withMultiViews,
		withThreeViewsInSession,
		withSocket,
		withClientStateSaving,
		withCDNResources,
		withInterpretEmptyStringSubmittedValuesAsNull,
		withViewResources;
	}

}