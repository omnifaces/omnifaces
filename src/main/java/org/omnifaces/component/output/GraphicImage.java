/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.component.output;

import static java.lang.Boolean.parseBoolean;
import static org.omnifaces.resourcehandler.DefaultResourceHandler.RES_NOT_FOUND;
import static org.omnifaces.util.Renderers.writeAttributes;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.el.MethodReference;
import org.omnifaces.resourcehandler.DefaultResourceHandler;
import org.omnifaces.resourcehandler.DynamicResource;
import org.omnifaces.resourcehandler.GraphicResource;
import org.omnifaces.resourcehandler.GraphicResourceHandler;
import org.omnifaces.util.Faces;

/**
 * <p>
 * The <code>&lt;o:graphicImage&gt;</code> is a component that extends the standard <code>&lt;h:graphicImage&gt;</code>
 * with support for referencing an {@link InputStream} or <code>byte[]</code> property in the <code>value</code>
 * attribute, optionally as a data URI.
 *
 * <h3>Data URI</h3>
 * <p>
 * Set <code>dataURI</code> attribute to true in order to render image in
 * <a href="http://en.wikipedia.org/wiki/Data_URI_scheme">data URI format</a>.
 * <pre>
 * &lt;o:graphicImage name="icon.png" dataURI="true" /&gt; &lt;!-- JSF resource as data URI --&gt;
 * &lt;o:graphicImage value="#{bean.icon}" dataURI="true" /&gt; &lt;!-- byte[]/InputStream property as data URI --&gt;
 * </pre>
 * <p>
 * This basically renders the image inline in HTML output immediately during JSF render response phase. This approach
 * is very useful for a "preview" feature of uploaded images and works also in combination with view scoped beans. This
 * approach is however <em>not</em> recommended for "permanent" and/or "large" images as it doesn't offer the browser
 * any opportunity to cache the images for reuse, ~10KB would typically be the max even less so if there are more such
 * images on the same page.
 *
 * <h3>Image streaming</h3>
 * <p>
 * When not rendered as data URI, the {@link InputStream} or <code>byte[]</code> property <strong>must</strong> point to
 * a <em>stateless</em> <code>@ApplicationScoped</code> bean (both JSF and CDI scopes are supported). The property will
 * namely be evaluated at the moment the browser requests the image content based on the URL as specified in HTML
 * <code>&lt;img src&gt;</code>, which is usually a different request than the one which rendered the JSF page.
 * E.g.
 * <pre>
 * &#64;Named
 * &#64;RequestScoped
 * public class Bean {
 *
 *     private List&lt;Image&gt; images; // Image class should NOT have "content" property, or at least it be lazy loaded.
 *
 *     &#64;Inject
 *     private ImageService service;
 *
 *     &#64;PostConstruct
 *     public void init() {
 *         images = service.list();
 *     }
 *
 *     public List&lt;Image&gt; getImages() {
 *         return images;
 *     }
 *
 * }
 * </pre>
 * <pre>
 * &#64;Named
 * &#64;ApplicationScoped
 * public class ImageStreamer {
 *
 *     &#64;Inject
 *     private ImageService service;
 *
 *     public byte[] getById(Long id) {
 *         return service.getContent(id);
 *     }
 *
 * }
 * </pre>
 * <pre>
 * &lt;ui:repeat value="#{bean.images}" var="image"&gt;
 *     &lt;o:graphicImage value="#{imageStreamer.getById(image.id)}" /&gt;
 * &lt;/ui:repeat&gt;
 * </pre>
 * <p>
 * A <code>@RequestScoped</code> and <code>@SessionScoped</code> bean would theoretically work, but this is wrong design
 * (a servlet is inherently also application scoped and stateless, not without reason). A <code>@ViewScoped</code>
 * wouldn't work because the image request doesn't share the JSF view state.
 * <p>
 * In case the property is a method expression taking arguments, each of those arguments will be converted to a string
 * HTTP request parameter and back to actual objects using the converters registered by class as available via
 * {@link Application#createConverter(Class)}. So, most of standard types like {@link Long} are already implicitly
 * supported. In case you need to supply a custom object as argument for some reason, you need to explicitly register
 * a converter for it yourself via <code>&#64;FacesConverter(forClass)</code>.
 *
 * <h3>Caching</h3>
 * <p>
 * In case your "image" entity supports it, you can also supply the "last modified" property which will be used in the
 * <code>ETag</code> and <code>Last-Modified</code> headers and in <code>If-Modified-Since</code> checks, hereby
 * improving browser caching. The <code>lastModified</code> attribute supports both {@link Date} and {@link Long} as
 * timestamp in milliseconds.
 * <pre>
 * &lt;ui:repeat value="#{bean.images}" var="image"&gt;
 *     &lt;o:graphicImage value="#{imageStreamer.getById(image.id)}" lastModified="#{image.lastModified}" /&gt;
 * &lt;/ui:repeat&gt;
 * </pre>
 *
 * <h3>Image types</h3>
 * <p>
 * When rendered as data URI, the content type will be guessed based on content header. So far, JPEG, PNG, GIF, ICO,
 * SVG, BMP and TIFF are recognized. If the content header is unrecognized, or when the image is rendered as regular
 * image source, then the content type will default to <code>"image"</code> without any subtype. This should work for
 * most images in most browsers. This may however fail on newer images or in older browsers. In that case, you can
 * explicitly specify the image type via the <code>type</code> attribute which must represent a valid file extension.
 * E.g.
 * <pre>
 * &lt;o:graphicImage value="#{imageStreamer.getById(image.id)}" type="svg" /&gt;
 * </pre>
 * <p>
 * The content type will be resolved via {@link Faces#getMimeType(String)}. You can add unrecognized ones as
 * <code>&lt;mime-mapping&gt;</code> in <code>web.xml</code>. E.g.
 * <pre>
 * &lt;mime-mapping&gt;
 *     &lt;extension&gt;svg&lt;/extension&gt;
 *     &lt;mime-type&gt;image/svg+xml&lt;/mime-type&gt;
 * &lt;/mime-mapping&gt;
 * </pre>
 *
 * <h3>SVG view modes</h3>
 * <p>
 * When serving a SVG image, you can use <code>fragment</code> attribute to trigger
 * <a href="http://www.w3.org/TR/SVG/linking.html#LinksIntoSVG">SVG view modes</a>
 * (beware of <a href="http://caniuse.com/#feat=svg-fragment">browser support</a>).
 * E.g.
 * <pre>
 * &lt;o:graphicImage value="#{imageStreamer.getById(image.id)}" type="svg" fragment="svgView(viewBox(0,50,200,200))" /&gt;
 * </pre>
 *
 * <h3>Design notes</h3>
 * <p>
 * The bean class name and method name will end up in the image source URL. Although this is technically harmless and
 * not tamperable by hackers, you might want to choose a "sensible" class and method name for this purpose.
 * <p>
 * Like <code>&lt;h:graphicImage&gt;</code>, the <code>value</code> attribute is <strong>ignored</strong>
 * when the <code>name</code> attribute is specified (for JSF resources). And, the <code>value</code> attribute of
 * <code>&lt;o:graphicImage&gt;</code> does <strong>not</strong> support URLs anymore. For that, just keep using
 * <code>&lt;h:graphicImage&gt;</code> or even plain <code>&lt;img&gt;</code>.
 *
 * @author Bauke Scholtz
 * @since 2.0
 * @see GraphicResource
 * @see DynamicResource
 * @see GraphicResourceHandler
 * @see DefaultResourceHandler
 * @see ExpressionInspector
 * @see MethodReference
 */
@FacesComponent(GraphicImage.COMPONENT_TYPE)
public class GraphicImage extends HtmlGraphicImage {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.GraphicImage";
	public static final Map<String, String> ATTRIBUTE_NAMES = collectAttributeNames();
	private static Map<String, String> collectAttributeNames() {
		Map<String, String> attributeNames = new HashMap<>();

		for (PropertyKeys propertyKey : PropertyKeys.values()) {
			String name = propertyKey.name();
			attributeNames.put(name, "styleClass".equals(name) ? "class" : propertyKey.toString());
		}

		return Collections.unmodifiableMap(attributeNames);
	}

	private static final String ERROR_MISSING_VALUE = "o:graphicImage 'value' attribute is required.";

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs the GraphicImage component.
	 */
	public GraphicImage() {
		setRendererType(null);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("img", this);
		writer.writeAttribute("src", getSrc(context), "value"); // writeURIAttribute kills URL fragment identifiers.
		writeAttributes(writer, this, GraphicImage.ATTRIBUTE_NAMES);
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("img");
	}

	/**
	 * Returns the URL needed for the 'src' attribute.
	 * @param context The involved faces context.
	 * @return The URL needed for the 'src' attribute.
	 * @throws IOException When something fails at I/O level.
	 */
	protected String getSrc(FacesContext context) throws IOException {
		String name = (String) getAttributes().get("name");
		boolean dataURI = parseBoolean(String.valueOf(getAttributes().get("dataURI")));

		Resource resource;

		if (name != null) {
			String library = (String) getAttributes().get("library");
			resource = context.getApplication().getResourceHandler().createResource(name, library);

			if (resource == null) {
				return RES_NOT_FOUND;
			}

			if (dataURI && resource.getContentType().startsWith("image")) {
				resource = new GraphicResource(resource.getInputStream(), resource.getContentType());
			}
		}
		else {
			ValueExpression value = getValueExpression("value");

			if (value == null) {
				throw new IllegalArgumentException(ERROR_MISSING_VALUE);
			}

			String type = (String) getAttributes().get("type");

			if (dataURI) {
				resource = new GraphicResource(value.getValue(context.getELContext()), type);
			}
			else {
				resource = GraphicResource.create(context, value, type, getAttributes().get("lastModified"));
			}
		}

		String fragment = (String) getAttributes().get("fragment");
		String fragmentString = dataURI || isEmpty(fragment) ? "" : ((fragment.charAt(0) == '#' ? "" : "#") + fragment);
		return context.getExternalContext().encodeResourceURL(resource.getRequestPath()) + fragmentString;
	}

	/**
	 * Returns an empty string as default value instead of <code>null</code>, so that the attribute is always rendered,
	 * as mandated by HTML5.
	 */
	@Override
	public String getAlt() {
		return coalesce(super.getAlt(), "");
	}

}