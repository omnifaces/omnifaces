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
package org.omnifaces.test;

import static java.time.Duration.ofSeconds;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withMessageBundle;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

@ExtendWith(ArquillianExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class OmniFacesIT {

	protected WebDriver browser;

	@ArquillianResource
	protected URL baseURL;

	@BeforeAll
	public void setup() {
		String arquillianBrowser = System.getProperty("arquillian.browser");

		switch (arquillianBrowser) {
			case "chrome":
				WebDriverManager.chromedriver().setup();
				ChromeDriver chrome = new ChromeDriver(new ChromeOptions().addArguments("--no-sandbox", "--headless"));
				chrome.setLogLevel(Level.INFO);
				browser = chrome;
				break;
			default:
				throw new UnsupportedOperationException("arquillian.browser='" + arquillianBrowser + "' is not yet supported");
		}

		PageFactory.initElements(browser, this);
	}

	@BeforeEach
	public void init() {
        open(getClass().getSimpleName() + ".xhtml");
	}

    @AfterAll
    public void teardown() {
    	browser.quit();
    }

	protected void refresh() {
		init();
	}

	protected void open(String pageName) {
		browser.get(baseURL + pageName);
	}

	protected String openNewTab(WebElement elementWhichOpensNewTab) {
		Set<String> oldTabs = browser.getWindowHandles();
		elementWhichOpensNewTab.click();
		Set<String> newTabs = new HashSet<>(browser.getWindowHandles());
		newTabs.removeAll(oldTabs); // Just to be sure; it's nowhere in Selenium API specified whether tabs are ordered.
		String newTab = newTabs.iterator().next();
		browser.switchTo().window(newTab);
		return newTab;
	}

	protected void openWithQueryString(String queryString) {
		open(getClass().getSimpleName() + ".xhtml?" + queryString);
	}

	protected void openWithHashString(String hashString) {
		open(getClass().getSimpleName() + ".xhtml?" + System.currentTimeMillis() + "#" + hashString); // Query string trick is necessary because Selenium driver may not forcibly reload page.
	}

	protected void closeCurrentTabAndSwitchTo(String tabToSwitch) {
		open(null); // This trick gives @ViewScoped unload opportunity to hit server.
		browser.close();
		browser.switchTo().window(tabToSwitch);
	}

	/**
	 * Work around because Selenium WebDriver API doesn't support triggering JS events.
	 */
	protected void triggerOnchange(WebElement input, String messagesId) {
		clearTextContent(messagesId);
		guardAjax(() -> executeScript("document.getElementById('" + input.getAttribute("id") + "').onchange();"));
		waitUntilTextContent(messagesId);
	}

	protected void guardHttp(Runnable action) {
		action.run();
		waitUntil(() -> executeScript("return document.readyState=='complete'"));
	}

	protected void guardAjax(Runnable action) {
		String uuid = UUID.randomUUID().toString();
		executeScript("window.$ajax=true;(window.jsf?jsf:faces).ajax.addOnEvent(data=>{if(data.status=='complete')window.$ajax='" + uuid + "'})");
		action.run();
		waitUntil(() -> executeScript("return window.$ajax=='" + uuid + "' || (!window.$ajax && document.readyState=='complete')")); // window.$ajax will be falsey when ajax redirect has occurred.
	}

	protected void guardPrimeFacesAjax(Runnable action) {
		action.run();
		waitUntil(() -> executeScript("return !!window.PrimeFaces && PrimeFaces.ajax.Queue.isEmpty()"));
	}

	/**
	 * Work around because Selenium WebDriver API doesn't recognize iframe based ajax upload in guard.
	 */
	protected void guardAjaxUpload(Runnable action, WebElement messages) {
		clearTextContent(messages);
		guardAjax(action);
		waitUntilTextContent(messages);
	}

	private void waitUntil(Supplier<Boolean> predicate) {
		new WebDriverWait(browser, ofSeconds(3)).until($ -> predicate.get());
	}

	protected void waitUntilTextContent(String elementId) {
		waitUntil(() -> {
			try {
				return !browser.findElement(By.id(elementId)).getText().isBlank();
			}
			catch (StaleElementReferenceException ignore) {
				return false; // Will retry next.
			}
		});
	}

	protected void waitUntilTextContent(WebElement element) {
		waitUntil(() -> !element.getText().isBlank());
	}

	protected void waitUntilTextContains(WebElement element, String expectedString) {
		waitUntil(() -> element.getText().contains(expectedString));
	}

	@SuppressWarnings("unchecked")
	protected <T> T executeScript(String script) {
		return (T) ((JavascriptExecutor) browser).executeScript(script);
	}

	protected void clearTextContent(WebElement messages) {
		clearTextContent(messages.getAttribute("id"));
	}

	protected void clearTextContent(String messagesId) {
		executeScript("document.getElementById('" + messagesId + "').innerHTML='';");
	}

	protected static String stripJsessionid(String url) {
		return url.split(";jsessionid=", 2)[0];
	}

	protected static String stripHostAndJsessionid(String url) {
		try {
			URIBuilder builder = new URIBuilder(url);
			builder.setScheme(null);
			builder.setHost(null);
			return stripJsessionid(builder.toString());
		}
		catch (URISyntaxException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	protected static boolean isFaces4Used() {
		return System.getProperty("profile.id").endsWith("4");
	}

	protected static boolean isLibertyUsed() {
		return System.getProperty("profile.id").startsWith("liberty-");
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
				.addAsWebInfResource("WEB-INF/beans.xml", "beans.xml")
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
				case withDevelopmentStage:
				case withErrorPage:
					archive.addAsWebInfResource("WEB-INF/500.xhtml");
					break;
				case withFacesViews:
				case withFacesViewsLowercasedRequestURI:
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
			archive.addAsLibraries(maven.resolve("org.primefaces:primefaces:jar:jakarta:" + System.getProperty("primefaces.version")).withTransitivity().asFile());
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
		withVersionedResourceHandler,
		withViewExpiredExceptionHandler,
		withViewResourceHandler;
	}

	public static enum WebXml {
		basic,
		distributable,
		withDevelopmentStage,
		withErrorPage,
		withFacesViews,
		withFacesViewsLowercasedRequestURI,
		withMultiViews,
		withThreeViewsInSession,
		withSocket,
		withClientStateSaving,
		withCDNResources,
		withInterpretEmptyStringSubmittedValuesAsNull,
		withVersionedResourceHandler,
		withViewResources,
		withTaglib;
	}

}