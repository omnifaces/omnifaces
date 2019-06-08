/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.util;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

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

/**
 * <p>
 * Collection of utility methods for the JAXP API in general.
 *
 * @author Bauke Scholtz
 * @since 2.1
 */
public final class Xml {

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
		DocumentBuilder builder = createDocumentBuilder();
		Document document = builder.newDocument();
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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		factory.setExpandEntityReferences(false);

		try {
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			return factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new UnsupportedOperationException(e);
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
		for (URL url : urls) {
			if (url == null) {
				continue;
			}

			URLConnection connection = url.openConnection();
			connection.setUseCaches(false);

			try (InputStream input = connection.getInputStream()) {
				NodeList children = builder.parse(input).getDocumentElement().getChildNodes();

				for (int i = 0; i < children.getLength(); i++) {
					document.getDocumentElement().appendChild(document.importNode(children.item(i), true));
				}
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
		return node.getFirstChild().getNodeValue().trim();
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
			NodeList nodeList = getNodeList(createDocument(asList(url)).getDocumentElement(), XPathFactory.newInstance().newXPath(), expression);
			List<String> nodeTextContents = new ArrayList<>(nodeList.getLength());

			for (int i = 0; i < nodeList.getLength(); i++) {
				nodeTextContents.add(getTextContent(nodeList.item(i)));
			}

			return nodeTextContents;
		}
		catch (IOException | SAXException | XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
	}

}