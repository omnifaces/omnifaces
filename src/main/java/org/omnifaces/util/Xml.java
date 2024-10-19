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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.logging.Level.FINE;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * <p>
 * Collection of utility methods for the JAXP API in general.
 *
 * <h2>This class is not listed in showcase! Should I use it?</h2>
 * <p>
 * This class is indeed intented for internal usage only. We won't add methods here on user request. We only add methods
 * here once we encounter non-DRY code in OmniFaces codebase. The methods may be renamed/changed without notice.
 * <p>
 * We don't stop you from using it if you found it in the Javadoc and you think you find it useful, but you have to
 * accept the risk that the method signatures can be changed without notice. This utility class exists because OmniFaces
 * intends to be free of 3rd party dependencies.
 *
 * @author Bauke Scholtz
 * @since 2.1
 */
public final class Xml {

    private static final Logger logger = Logger.getLogger(Xml.class.getName());

    // Constructors ---------------------------------------------------------------------------------------------------

    private Xml() {
        // Hide constructor.
    }

    // Create ---------------------------------------------------------------------------------------------------------

    /**
     * Creates a single XML {@link Document} based on given URLs representing XML documents. All those XML documents
     * are merged into a single root element named <code>root</code>.
     * @param urls The URLs representing XML documents.
     * @return A single XML document containing all given XML documents.
     * @throws IOException When an I/O error occurs.
     * @throws SAXException When a XML parsing error occurs.
     */
    public static Document createDocument(List<URL> urls) throws IOException, SAXException {
        var builder = createDocumentBuilder();
        var document = builder.newDocument();
        document.appendChild(document.createElement("root"));
        parseAndAppendChildren(builder, document, urls);
        return document;
    }

    /**
     * Creates an instance of {@link DocumentBuilder} which doesn't validate, nor is namespace aware nor expands entity
     * references and disables external entity processing (to keep it as lenient and secure as possible).
     * @return A lenient instance of {@link DocumentBuilder}.
     */
    public static DocumentBuilder createDocumentBuilder() {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            unsetAttributeIgnoringIAE(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
            unsetAttributeIgnoringIAE(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
            return factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static void unsetAttributeIgnoringIAE(DocumentBuilderFactory factory, String name) {
        try {
            factory.setAttribute(name, "");
        }
        catch (IllegalArgumentException e) {
            logger.log(FINE, format("Cannot unset attribute '%s'; falling back to default.", name), e);
        }
    }

    // Manipulate -----------------------------------------------------------------------------------------------------

    /**
     * Parse the given URLs as a document using the given builder and then append all its child nodes to the given
     * document.
     * @param builder The document builder.
     * @param document The document.
     * @param urls The URLs representing XML documents.
     * @throws IOException When an I/O error occurs.
     * @throws SAXException When a XML parsing error occurs.
     */
    public static void parseAndAppendChildren(DocumentBuilder builder, Document document, List<URL> urls) throws IOException, SAXException {
        for (var url : urls) {
            if (url == null) {
                continue;
            }

            var connection = url.openConnection();
            connection.setUseCaches(false);

            try (var input = connection.getInputStream()) {
                var children = builder.parse(input).getDocumentElement().getChildNodes();

                for (var i = 0; i < children.getLength(); i++) {
                    document.getDocumentElement().appendChild(document.importNode(children.item(i), true));
                }
            }
            catch (SAXParseException e) {
                throw new SAXException("Cannot parse " + url.toExternalForm(), e);
            }
        }
    }

    // Traverse -------------------------------------------------------------------------------------------------------

    /**
     * Convenience method to return a node list matching given XPath expression.
     * @param node The node to return node list matching given XPath expression for.
     * @param xpath The XPath instance.
     * @param expression The XPath expression to match node list.
     * @return A node list matching given XPath expression
     * @throws XPathExpressionException When the XPath expression contains a syntax error.
     */
    public static NodeList getNodeList(Node node, XPath xpath, String expression) throws XPathExpressionException {
        return (NodeList) xpath.compile(expression).evaluate(node, XPathConstants.NODESET);
    }

    /**
     * Convenience method to return trimmed text content of given node. This uses
     * <code>getFirstChild().getNodeValue()</code> instead of <code>getTextContent()</code> to workaround some buggy
     * JAXP implementations.
     * @param node The node to return text content for.
     * @return Trimmed text content of given node.
     */
    public static String getTextContent(Node node) {
        return (node.getChildNodes().getLength() == 1 ? node.getFirstChild().getNodeValue() : node.getTextContent()).trim();
    }

    /**
     * Convenience method to return a list of node text contents for given URL representing XML document matching given
     * XPath expression.
     * @param url The URL representing XML document.
     * @param expression The XPath expression to match node list whose text content has to be collected.
     * @return A list of node text contents.
     * @since 2.6.3
     */
    public static List<String> getNodeTextContents(URL url, String expression) {
        try {
            var nodeList = getNodeList(createDocument(asList(url)).getDocumentElement(), XPathFactory.newInstance().newXPath(), expression);
            var nodeTextContents = new ArrayList<String>(nodeList.getLength());

            for (var i = 0; i < nodeList.getLength(); i++) {
                nodeTextContents.add(getTextContent(nodeList.item(i)));
            }

            return nodeTextContents;
        }
        catch (IOException | SAXException | XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }

}