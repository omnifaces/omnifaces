/*
 * Copyright 2016 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;
import javax.inject.Qualifier;

import org.omnifaces.component.output.GraphicImage;

/**
 * <p>
 * Stereo type that designates a bean as an application scoped bean for serving graphic images via
 * <code>&lt;o:graphicImage&gt;</code> component or <code>#{of:graphicImageURL()}</code> EL functions.
 * <pre>
 * import javax.inject.Named;
 * import org.omnifaces.cdi.GraphicImageScoped;
 *
 * &#64;Named
 * &#64;GraphicImageScoped
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
 * When using {@link ApplicationScoped} instead, serving graphic images via a JSF page will continue to work, but
 * when the server restarts, then hotlinking/bookmarking will stop working until the JSF page referencing the same
 * bean method is requested for the first time. The {@link GraphicImageScoped} basically enables serving images without
 * the need to reference them via a JSF page.
 *
 * @since 2.5
 * @author Bauke Scholtz
 * @see GraphicImage
 *
 */
@Documented
@Qualifier
@Stereotype
@ApplicationScoped
@Retention(RUNTIME)
@Target(TYPE)
public @interface GraphicImageScoped {
	//
}