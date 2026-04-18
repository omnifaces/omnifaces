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
import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withCustomCDNResourceHandler;
import static org.omnifaces.test.OmniFacesIT.FacesConfig.withMessageBundle;
import static org.omnifaces.util.ResourcePaths.stripTrailingSlash;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.utils.URIBuilder;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
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
import org.openqa.selenium.devtools.v147.log.Log;
import org.openqa.selenium.devtools.v147.network.Network;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

@ExtendWith(ArquillianExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
public abstract class OmniFacesIT {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected WebDriver browser;
    protected Map<String, String> networkResponses = new LinkedHashMap<>();
    protected List<String> consoleErrors = new ArrayList<>();

    @ArquillianResource
    protected URL baseURL;

    protected String contextPath;

    @BeforeAll
    public void setup() {
        logger.fine(this + "#setup(); " + browser + "; " + baseURL);
        Logger.getLogger(RemoteWebDriver.class.getPackageName()).setLevel(Level.WARNING); // Tone down super verbose WebDriver#findElement logging.
        var arquillianBrowser = System.getProperty("arquillian.browser");

        browser = switch (arquillianBrowser) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                var originalClassLoader = Thread.currentThread().getContextClassLoader();

                try {
                    Thread.currentThread().setContextClassLoader(ChromeDriver.class.getClassLoader()); // Because quarkus-arquillian loads
                                                                                                       // selenium-remote-driver and selenium-chrome-driver from
                                                                                                       // different classloaders and this would cause Chrome
                                                                                                       // driver to throw java.util.ServiceConfigurationError:
                                                                                                       // org.openqa.selenium.remote.AdditionalHttpCommands:
                                                                                                       // org.openqa.selenium.chrome.AddHasCasting not a subtype
                    var chrome = new ChromeDriver(new ChromeOptions().addArguments("--no-sandbox", "--headless"));
                    chrome.setLogLevel(Level.INFO);

                    var devTools = chrome.getDevTools();
                    devTools.createSession();
                    devTools.send(Network.enable(empty(), empty(), empty(), empty(), empty()));
                    devTools.addListener(Network.responseReceived(), event -> {
                        String body;

                        try {
                            body = devTools.send(Network.getResponseBody(event.getRequestId())).getBody();
                        }
                        catch (Exception e) {
                            body = e.toString();
                        }

                        networkResponses.put(stripHostAndJsessionid(event.getResponse().getUrl()), body);
                    });
                    devTools.send(Log.enable());
                    devTools.addListener(Log.entryAdded(), entry -> {
                        if ("error".equalsIgnoreCase(entry.getLevel().toString())) {
                            consoleErrors.add(entry.getText());
                        }
                    });

                    yield chrome;
                }
                finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            }
            default -> throw new UnsupportedOperationException("arquillian.browser='" + arquillianBrowser + "' is not yet supported");
        };

        PageFactory.initElements(browser, this);
    }

    @BeforeEach
    public void init() {
        logger.fine(this + "#init(); " + browser + "; " + baseURL);

        if (browser == null) {
            setup(); // Because quarkus-arquillian doesn't recognize the different lifecycle of @BeforeAll on a @TestInstance(Lifecycle.PER_CLASS) and forgets
                     // to invoke it on each instantiation.
        }

        try {
            if (!baseURL.toExternalForm().endsWith("/")) {
                baseURL = new URL(baseURL + "/"); // And for some reason quarkus-arquillian forgets the trailing slash?
            }

            contextPath = stripTrailingSlash(baseURL.getPath());
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException();
        }

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
        networkResponses.clear();
        consoleErrors.clear();
        browser.get(baseURL + pageName);
    }

    protected String openNewTab(WebElement elementWhichOpensNewTab) {
        var oldTabs = browser.getWindowHandles();
        elementWhichOpensNewTab.click();
        Set<String> newTabs = new HashSet<>(browser.getWindowHandles());
        newTabs.removeAll(oldTabs); // Just to be sure; it's nowhere in Selenium API specified whether tabs are ordered.
        var newTab = newTabs.iterator().next();
        browser.switchTo().window(newTab);
        waitUntil(() -> executeScript("return document.readyState=='complete'"));
        return newTab;
    }

    protected void openWithQueryString(String queryString) {
        open(getClass().getSimpleName() + ".xhtml?" + queryString);
    }

    protected void openWithHashString(String hashString) {
        open(getClass().getSimpleName() + ".xhtml?" + System.currentTimeMillis() + "#" + hashString); // Query string trick is necessary because Selenium driver
                                                                                                      // may not forcibly reload page.
    }

    protected void closeCurrentTabAndSwitchTo(String tabToSwitch) {
        open(null); // This trick gives @ViewScoped unload opportunity to hit server.
        browser.close();
        browser.switchTo().window(tabToSwitch);
    }

    protected void guardHttp(Runnable action) {
        networkResponses.clear();
        consoleErrors.clear();
        executeScript("window.$http=true");
        action.run();
        waitUntil(() -> executeScript("return !window.$http && document.readyState=='complete'"));
    }

    protected void guardAjax(Runnable action) {
        networkResponses.clear();
        consoleErrors.clear();
        var uuid = UUID.randomUUID().toString();
        executeScript("window.$ajax=true;faces.ajax.addOnEvent(data=>{if(data.status=='complete')window.$ajax='" + uuid + "'})");
        action.run();
        waitUntil(() -> executeScript("return window.$ajax=='" + uuid + "' || (!window.$ajax && document.readyState=='complete')")); // window.$ajax will be
                                                                                                                                     // falsey when ajax
                                                                                                                                     // redirect has occurred.
    }

    protected void guardPrimeFacesAjax(Runnable action) {
        networkResponses.clear();
        consoleErrors.clear();
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

    protected String getResponseBody() {
        return networkResponses.entrySet().stream().filter(entry -> browser.getCurrentUrl().endsWith(entry.getKey())).map(Entry::getValue).findFirst()
            .orElseThrow();
    }

    private void waitUntil(Supplier<Boolean> predicate) {
        new WebDriverWait(browser, ofSeconds(3)).until($ -> predicate.get());
    }

    protected void waitUntilTextContent(String elementId) {
        waitUntilTextContent(elementId, null);
    }

    protected void waitUntilTextContent(String elementId, Runnable ajaxPoller) {
        waitUntil(() -> {
            try {
                if (!browser.findElement(By.id(elementId)).getText().isBlank()) {
                    return true;
                }
            }
            catch (StaleElementReferenceException ignore) {
                // Will retry next.
            }

            if (ajaxPoller != null) {
                guardAjax(ajaxPoller);
            }

            return false;
        });
    }

    protected void waitUntilTextContent(WebElement element) {
        waitUntil(() -> !element.getText().isBlank());
    }

    protected void waitUntilTextContains(WebElement element, String expectedString) {
        waitUntil(() -> element.getText().contains(expectedString));
    }

    protected void waitFor(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Wait interrupted", e);
        }
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
            var builder = new URIBuilder(url);
            builder.setScheme(null);
            builder.setHost(null);
            return stripJsessionid(builder.toString());
        }
        catch (URISyntaxException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    protected static boolean isLibertyUsed() {
        return System.getProperty("profile.id").startsWith("liberty-");
    }

    protected static boolean isQuarkusUsed() {
        return System.getProperty("profile.id").startsWith("quarkus-");
    }

    protected static boolean isMyFacesUsed() {
        return System.getProperty("profile.id").endsWith("-myfaces");
    }

    protected static <T extends OmniFacesIT> WebArchive createWebArchive(Class<T> testClass) {
        return buildWebArchive(testClass).createDeployment();
    }

    protected static <T extends OmniFacesIT> ArchiveBuilder buildWebArchive(Class<T> testClass) {
        return new ArchiveBuilder(testClass);
    }

    protected static class ArchiveBuilder {

        private final WebArchive archive;
        private final boolean treatWarAsWebFragmentJar;
        private final Map<String, String> quarkusProperties = new HashMap<>();
        private boolean facesConfigSet;
        private boolean webXmlSet;
        private boolean primeFacesSet;

        private <T extends OmniFacesIT> ArchiveBuilder(Class<T> testClass) {
            var packageName = testClass.getPackage().getName();
            var className = testClass.getSimpleName();
            var warName = className + ".war";

            archive = create(WebArchive.class, warName)
                .addPackage(packageName)
                .deleteClass(testClass)
                .addAsLibrary(new File(System.getProperty("omnifaces.jar")));

            treatWarAsWebFragmentJar = isQuarkusUsed();
            addWebInfResource("WEB-INF/beans.xml", "beans.xml");

            var warLibraries = System.getProperty("war.libraries");

            if (warLibraries != null) {
                archive.addAsLibraries(Maven.resolver().resolve(warLibraries.split("\\s*,\\s*")).withTransitivity().asFile());
            }

            addWebResources(new File(testClass.getClassLoader().getResource(packageName).getFile()), "");
        }

        private void addWebResources(File root, String directory) {
            for (var file : root.listFiles()) {
                var path = directory + "/" + file.getName();

                if (file.isFile()) {
                    addWebResource(file, path);
                }
                else if (file.isDirectory()) {
                    addWebResources(file, path);
                }
            }
        }

        private void addWebResource(File file, String path) {
            if (treatWarAsWebFragmentJar) {
                archive.addAsResource(file, "META-INF/resources/" + path);
            }
            else {
                archive.addAsWebResource(file, path);
            }
        }

        private void addWebResource(String name, String path) {
            if (treatWarAsWebFragmentJar) {
                archive.addAsResource(name, "META-INF/resources/" + path);
            }
            else {
                archive.addAsWebResource(name, path);
            }
        }

        private void addWebInfResource(String name, String path) {
            if (treatWarAsWebFragmentJar) {
                archive.addAsResource(name, "META-INF/" + path);
            }
            else {
                archive.addAsWebInfResource(name, path);
            }
        }

        private void addWebResource(String name) {
            addWebResource(name, name);
        }

        private void addQuarkusPropertyIfNecessary(String name, String value) {
            quarkusProperties.put(name, value);
        }

        public ArchiveBuilder withFacesConfig(FacesConfig facesConfig) {
            if (facesConfigSet) {
                throw new IllegalStateException("There can be only one faces-config.xml");
            }

            addWebInfResource("WEB-INF/faces-config.xml/" + facesConfig.name() + ".xml", "faces-config.xml");

            if (facesConfig == withMessageBundle) {
                archive.addAsResource("messages.properties");
            }
            else if (facesConfig == withCustomCDNResourceHandler) {
                archive.addClass(CustomCDNResourceHandler.class);
            }

            facesConfigSet = true;
            return this;
        }

        public ArchiveBuilder withWebXml(WebXml webXml) {
            if (webXmlSet) {
                throw new IllegalStateException("There can be only one web.xml");
            }

            addWebInfResource("WEB-INF/web.xml/" + webXml.name() + ".xml", "web.xml");

            switch (webXml) {
                case withDevelopmentStage :
                    addQuarkusPropertyIfNecessary("jakarta.faces.PROJECT_STAGE", "Development");
                case withErrorPage :
                    addWebResource("WEB-INF/500.xhtml");
                    break;
                case withFacesViews :
                case withFacesViewsLowercasedRequestURI :
                case withMultiViews :
                    addWebResource("WEB-INF/404.xhtml");
                    break;
                default :
                    break;
            }

            webXmlSet = true;
            return this;
        }

        public ArchiveBuilder withPrimeFaces() {
            if (primeFacesSet) {
                throw new IllegalStateException("There can be only one PrimeFaces library");
            }

            var maven = Maven.resolver();
            archive
                .addAsLibraries(maven.resolve("org.primefaces:primefaces:jar:jakarta:" + System.getProperty("primefaces.version")).withTransitivity().asFile());
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

            if (isQuarkusUsed() && !quarkusProperties.isEmpty()) {
                archive.addAsResource(
                    new StringAsset(quarkusProperties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(joining("\n"))),
                    "application.properties"
                );
            }

            return archive;
        }

    }

    public enum FacesConfig {
        basic,
        withFullAjaxExceptionHandler,
        withCombinedResourceHandler,
        withMessageBundle,
        withCDNResourceHandler,
        withCustomCDNResourceHandler,
        withVersionedResourceHandler,
        withViewExpiredExceptionHandler,
        withViewResourceHandler,
        withSupportedLocales;
    }

    public enum WebXml {
        basic,
        distributable,
        withDevelopmentStage,
        withErrorPage,
        withFacesViews,
        withFacesViewsLowercasedRequestURI,
        withMultiViews,
        withThreeViewsInSession,
        withClientStateSaving,
        withCDNResources,
        withInterpretEmptyStringSubmittedValuesAsNull,
        withVersionedResourceHandler,
        withViewResources,
        withTaglib;
    }

}
