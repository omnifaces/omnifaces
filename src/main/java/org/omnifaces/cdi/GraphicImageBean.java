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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Date;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import org.omnifaces.component.output.GraphicImage;
import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.el.MethodReference;
import org.omnifaces.resourcehandler.DefaultResourceHandler;
import org.omnifaces.resourcehandler.DynamicResource;
import org.omnifaces.resourcehandler.GraphicResource;
import org.omnifaces.resourcehandler.GraphicResourceHandler;

/**
 * <p>
 * Stereo type that designates a bean with one or more methods returning <code>byte[]</code> or <code>InputStream</code>
 * as a named application scoped bean specifically for serving graphic images via <code>&lt;o:graphicImage&gt;</code>
 * component or <code>#{of:graphicImageURL()}</code> EL functions.
 * <pre>
 * import org.omnifaces.cdi.GraphicImageBean;
 *
 * &#64;GraphicImageBean
 * public class Images {
 *
 *     &#64;Inject
 *     private ImageService service;
 *
 *     public byte[] get(Long id) {
 *         return service.getContent(id);
 *     }
 *
 * }
 * </pre>
 * <p>
 * When using <code>@Named @ApplicationScoped</code> instead, serving graphic images via a Faces page will continue to
 * work, but when the server restarts, then hotlinking/bookmarking will stop working until the Faces page referencing the
 * same bean method is requested for the first time. This is caused by a security restriction which should prevent users
 * from invoking arbitrary bean methods by manipulating the URL. The <code>@GraphicImageBean</code> basically enables
 * endusers to invoke any public method returning a <code>byte[]</code> or <code>InputStream</code> on the bean by just
 * a HTTP GET request.
 *
 * <h2>Usage</h2>
 * <p>
 * You can use <code>#{of:graphicImageURL()}</code> EL functions to generate URLs referring the
 * <code>&#64;GraphicImageBean</code> bean, optionally with the image <code>type</code> and <code>lastModified</code>
 * arguments. Below are some usage examples:
 * <pre>
 * &lt;ui:repeat value="#{bean.products}" var="product"&gt;
 *
 *     &lt;!-- Basic, using default type and last modified. --&gt;
 *     &lt;a href="#{of:graphicImageURL('images.full(product.imageId)')}"&gt;
 *         &lt;o:graphicImage value="#{images.thumb(product.imageId)}" /&gt;
 *     &lt;/a&gt;
 *
 *     &lt;!-- With specified type and default last modified. --&gt;
 *     &lt;a href="#{of:graphicImageURLWithType('images.full(product.imageId)', 'png')}"&gt;
 *         &lt;o:graphicImage value="#{images.thumb(product.imageId)}" type="png" /&gt;
 *     &lt;/a&gt;
 *
 *     &lt;!-- With specified type and last modified. --&gt;
 *     &lt;a href="#{of:graphicImageURLWithTypeAndLastModified('images.full(product.imageId)', 'png', product.lastModified)}"&gt;
 *         &lt;o:graphicImage value="#{images.thumb(product.imageId)}" type="png" lastModified="#{product.lastModified}" /&gt;
 *     &lt;/a&gt;
 * &lt;/ui:repeat&gt;
 * </pre>
 * <p>
 * Note that in the <code>#{of:graphicImageURL()}</code> EL functions the expression string represents the same value as
 * you would use in <code>&lt;o:graphicImage&gt;</code> and that it must be a quoted string. Any nested quotes can be
 * escaped with backslash.
 * <p>
 * The <code>type</code> argument/attribute is the image type represented as file extension. E.g. "webp", "jpg", "png", "gif",
 * "ico", "svg", "bmp", "tiff", etc. When unspecified then the content type will default to <code>"image"</code>
 * without any subtype. This should work for most images in most browsers. This may however fail on newer images or in
 * older browsers. In that case, you can explicitly specify the image type via the <code>type</code> argument/attribute
 * which must represent a valid file extension.
 * <p>
 * The <code>lastModified</code> argument/attribute is the "last modified" timestamp, can be {@link Long} or
 * {@link Date}, or otherwise an attempt will be made to parse it as {@link Long}. When unspecified, then the "default
 * resource maximum age" as set in either the Mojarra specific context parameter
 * <code>com.sun.faces.defaultResourceMaxAge</code> or MyFaces specific context parameter
 * <code>org.apache.myfaces.RESOURCE_MAX_TIME_EXPIRES</code> will be used, else a default of 1 week will be assumed.
 *
 * @since 2.5
 * @author Bauke Scholtz
 * @see GraphicImage
 * @see GraphicResource
 * @see DynamicResource
 * @see GraphicResourceHandler
 * @see DefaultResourceHandler
 * @see ExpressionInspector
 * @see MethodReference
 */
@Inherited
@Documented
@Stereotype
@Named
@ApplicationScoped
@Retention(RUNTIME)
@Target(TYPE)
public @interface GraphicImageBean {
	//
}