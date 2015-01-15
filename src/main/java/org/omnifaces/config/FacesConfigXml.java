/*
 * Copyright 2015 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.config;

import static org.omnifaces.util.Faces.getServletContext;
import static org.omnifaces.util.Faces.hasContext;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>
 * This configuration enum parses the <code>/WEB-INF/faces-config.xml</code> and all <code>/META-INF/faces-config.xml</code> files
 * found in the classpath and offers methods to obtain information from them which is not available by the standard
 * JSF API.
 *
 * <h3>Usage</h3>
 * <p>
 * Some examples:
 * <pre>
 * // Get the &lt;resource-bundle&gt; (which are essentially mappings of resource bundle base name and variable name).
 * Map&lt;String, String&gt; resourceBundles = FacesConfigXml.INSTANCE.getResourceBundles();
 * </pre>
 *
 * @author Bauke Scholtz
 * @author Michele Mariotti
 * @since 2.1
 */
public enum FacesConfigXml {

    // Enum singleton -------------------------------------------------------------------------------------------------

    /**
     * Returns the lazily loaded enum singleton instance.
     * <p>
     * Note: if this is needed in e.g. a {@link Filter} which is called before the {@link FacesServlet} is invoked,
     * then it won't work if the <code>INSTANCE</code> hasn't been referenced before. Since JSF installs a special
     * "init" {@link FacesContext} during startup, one option for doing this initial referencing is in a
     * {@link ServletContextListener}. The data this enum encapsulates will then be available even where there is no
     * {@link FacesContext} available. If there's no other option, then you need to manually invoke
     * {@link #init(ServletContext)} whereby you pass the desired {@link ServletContext}.
     */
    INSTANCE;

    // Private constants ----------------------------------------------------------------------------------------------

    private static final Logger logger = Logger.getLogger(FacesConfigXml.class.getName());

    private static final String APP_FACES_CONFIG_XML = "/WEB-INF/faces-config.xml";

    private static final String LIB_FACES_CONFIG_XML = "META-INF/faces-config.xml";

    private static final String XPATH_RESOURCE_BUNDLE = "application/resource-bundle";

    private static final String XPATH_VAR = "var";

    private static final String XPATH_BASE_NAME = "base-name";

    private static final String ERROR_NOT_INITIALIZED =
        "FacesConfigXml is not initialized yet. Please use #init(ServletContext) method to manually initialize it.";

    private static final String LOG_INITIALIZATION_ERROR =
        "FacesConfigXml failed to initialize. Perhaps your faces-config.xml contains a typo?";

    // Properties -----------------------------------------------------------------------------------------------------

    private final AtomicBoolean initialized = new AtomicBoolean();

    private Map<String, String> resourceBundles;

    // Init -----------------------------------------------------------------------------------------------------------

    /**
     * Perform automatic initialization whereby the servlet context is obtained from the faces context.
     */
    private void init() {
        if (!initialized.get() && hasContext()) {
            init(getServletContext());
        }
    }

    /**
     * Perform manual initialization with the given servlet context, if not null and not already initialized yet.
     * @param servletContext The servlet context to obtain the faces-config.xml from.
     * @return The current {@link FacesConfigXml} instance, initialized and all.
     */
    public FacesConfigXml init(ServletContext servletContext) {
        if (servletContext != null && !initialized.getAndSet(true)) {
            try {
                Element facesConfigXml = loadFacesConfigXml(servletContext).getDocumentElement();
                XPath xpath = XPathFactory.newInstance().newXPath();
                resourceBundles = parseResourceBundles(facesConfigXml, xpath);
            }
            catch (Exception e) {
                initialized.set(false);
                logger.log(Level.SEVERE, LOG_INITIALIZATION_ERROR, e);
                throw new RuntimeException(e);
            }
        }

        return this;
    }

    // Getters --------------------------------------------------------------------------------------------------------

    /**
     * Returns a mapping of all resource bundle base names by var.
     * @return A mapping of all resource bundle base names by var.
     */
    public Map<String, String> getResourceBundles() {
        checkInitialized();
        return resourceBundles;
    }

    private void checkInitialized() {
        // This init() call is performed here instead of in constructor, because WebLogic loads this enum as a CDI
        // managed bean (in spite of having a VetoAnnotatedTypeExtension) which in turn implicitly invokes the enum
        // constructor and thus causes an init while JSF context isn't fully initialized and thus the faces context
        // isn't available yet. Perhaps it's fixed in newer WebLogic versions.
        init();

        if (!initialized.get()) {
            throw new IllegalStateException(ERROR_NOT_INITIALIZED);
        }
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Load, merge and return all <code>faces-config.xml</code> files found in the classpath
     * into a single {@link Document}.
     */
    private static Document loadFacesConfigXml(ServletContext context) throws Exception {
        DocumentBuilder builder = createDocumentBuilder();
        Document document = builder.newDocument();
        document.appendChild(document.createElement("all-faces-configs"));
        URL url = context.getResource(APP_FACES_CONFIG_XML);

        if (url != null) { // faces-config.xml is optional.
            parseAndAppendChildren(url, builder, document);
        }

        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(LIB_FACES_CONFIG_XML);

        while (urls.hasMoreElements()) {
            parseAndAppendChildren(urls.nextElement(), builder, document);
        }

        return document;
    }

    /**
     * Returns an instance of {@link DocumentBuilder} which doesn't validate, nor is namespace aware nor expands entity
     * references (to keep it as lenient as possible).
     */
    private static DocumentBuilder createDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    /**
     * Parse the given URL as a document using the given builder and then append all its child nodes to the given
     * document.
     */
    private static void parseAndAppendChildren(URL url, DocumentBuilder builder, Document document) throws Exception {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);

        try (InputStream input = connection.getInputStream()) {
            NodeList children = builder.parse(input).getDocumentElement().getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                document.getDocumentElement().appendChild(document.importNode(children.item(i), true));
            }
        }
    }

    /**
     * Create and return a mapping of all resource bundle base names by var found in the given document.
     */
    private static Map<String, String> parseResourceBundles(Element facesConfigXml, XPath xpath) throws Exception {
        Map<String, String> resourceBundles = new LinkedHashMap<>();
        NodeList resourceBundleNodes = getNodeList(facesConfigXml, xpath, XPATH_RESOURCE_BUNDLE);

        for (int i = 0; i < resourceBundleNodes.getLength(); i++) {
            Node node = resourceBundleNodes.item(i);

            String var = xpath.compile(XPATH_VAR).evaluate(node).trim();
            String baseName = xpath.compile(XPATH_BASE_NAME).evaluate(node).trim();

            if (!resourceBundles.containsKey(var)) {
                resourceBundles.put(var, baseName);
            }
        }

        return Collections.unmodifiableMap(resourceBundles);
    }

    // Helpers of helpers (JAXP hell) ---------------------------------------------------------------------------------

    private static NodeList getNodeList(Node node, XPath xpath, String expression) throws Exception {
        return (NodeList) xpath.compile(expression).evaluate(node, XPathConstants.NODESET);
    }

}