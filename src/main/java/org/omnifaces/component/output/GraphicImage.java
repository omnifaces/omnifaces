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

import static org.omnifaces.resourcehandler.DefaultResource.RES_NOT_FOUND;
import static org.omnifaces.util.Renderers.writeAttributes;
import static org.omnifaces.util.Utils.coalesce;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.omnifaces.resourcehandler.GraphicResource;
import org.omnifaces.resourcehandler.GraphicResourceHandler;

/**
 * <p>
 * The <code>&lt;o:graphicImage&gt;</code> is a component that extends the standard <code>&lt;h:graphicImage&gt;</code>
 * with support for referencing an {@link InputStream} or <code>byte[]</code> property in the <code>value</code> attribute,
 * optionally as a data URI (not recommended for "large" images, ~10KB would typically be the max, even less so if there are more
 * such images on the same page).
 * <pre>
 * &lt;o:graphicImage value="#{bean.icon}" dataURI="true" /&gt;
 * </pre>
 * <p>
 * When not served as data URI, the property must point to a <b>stateless</b> <code>@ApplicationScoped</code> bean
 * (both JSF and CDI scopes are supported). E.g.
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
 * &lt;ui:repeat value="#{bean.thumbnails}" var="thumbnail"&gt;
 *     &lt;o:graphicImage value="#{imageStreamer.getById(thumbnail.id)}" /&gt;
 * &lt;/ui:repeat&gt;
 * </pre>
 * <p>
 * In case your "thumbnail" supports it, you can also supply the "last modified" property which will be used in the
 * <code>ETag</code> and <code>Last-Modified</code> headers and in <code>If-Modified-Since</code> checks, hereby
 * improving browser caching. The <code>lastModified</code> attribute supports both {@link Long} and {@link Date}.
 * <pre>
 * &lt;ui:repeat value="#{bean.thumbnails}" var="thumbnail"&gt;
 *     &lt;o:graphicImage value="#{imageStreamer.getById(thumbnail.id)}" lastModified="#{thumbnail.lastModified}" /&gt;
 * &lt;/ui:repeat&gt;
 * </pre>
 * <p>
 * In case the property is a method expression taking arguments, each of those arguments will be converted to a string
 * HTTP request parameter and back to actual objects using the converters registered by class as available via
 * {@link Application#createConverter(Class)}. So, most of standard types like {@link Long} are already implicitly
 * supported. In case you need to supply a custom object as argument for some reason, you need to explicitly register
 * a converter for it yourself via <code>&#64;FacesConverter(forClass)</code>.
 * <p>
 * Note: the bean class name and method name will end up in the image source URL. Although this is technically harmless and
 * not tamperable by hackers, you might want to choose a "safe" class and method name for this purpose.
 * Note: like <code>&lt;h:graphicImage&gt;</code>, the <code>value</code> attribute is <strong>ignored</strong>
 * when the <code>name</code> attribute is specified (for JSF resources).
 *
 * @author Bauke Scholtz
 * @since 2.0
 * @see GraphicResource
 * @see GraphicResourceHandler
 */
@FacesComponent(GraphicImage.COMPONENT_TYPE)
public class GraphicImage extends HtmlGraphicImage {

	// Constants ------------------------------------------------------------------------------------------------------

	public static final String COMPONENT_TYPE = "org.omnifaces.component.output.GraphicImage";
	public static final Map<String, String> ATTRIBUTE_NAMES = collectAttributeNames();
	private static final Map<String, String> collectAttributeNames() {
		Map<String, String> attributeNames = new HashMap<>();

		for (PropertyKeys propertyKey : PropertyKeys.values()) {
			String name = propertyKey.name();
			attributeNames.put(name, "styleClass".equals(name) ? "class" : propertyKey.toString());
		}

		return Collections.unmodifiableMap(attributeNames);
	}

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs the GraphicImage component.
	 */
	public GraphicImage() {
		setRendererType(null);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code>.
	 */
	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("img", this);
		writer.writeURIAttribute("src", getSrc(context), "value");
		writeAttributes(writer, this, GraphicImage.ATTRIBUTE_NAMES);
		writer.endElement("img");
	}

	/**
	 * Returns the URL needed for the 'src' attribute.
	 * @param context The involved faces context.
	 * @return The URL needed for the 'src' attribute.
	 */
	protected String getSrc(FacesContext context) {
		String name = (String) getAttributes().get("name");
		String library = (String) getAttributes().get("library");

		Resource resource = null;

		if (name != null) {
			resource = context.getApplication().getResourceHandler().createResource(name, library);
		}
		else {
			resource = GraphicResource.create(context, this);
		}

		if (resource != null) {
			return context.getExternalContext().encodeResourceURL(resource.getRequestPath());
		}
		else {
			return RES_NOT_FOUND;
		}
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