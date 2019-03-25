/*
 * Copyright 2019 OmniFaces
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
package org.omnifaces.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link JSParam} allows you to inject client-side evaluated JavaScript results
 * and make them readily available in a CDI managed bean.
 * <p>
 * A field or property annotated with <code>&#64;</code>{@link JSParam} will not be instantly available when the parent
 * managed bean is initialized and <code>&#64;</code>{@link PostConstruct} is called. Instead, the value is injected as
 * soon as initial page load has completed.
 *
 * <h3>Usage</h3>
 *
 * <h4>Simple types</h4>
 * <p>
 * The example below injects the time zone offset of the client browser into <code>timezoneOffset</code>.
 * <pre>
 * &#64;Inject &#64;JSParam("new Date().getTimezoneOffset()")
 * private String timeZoneOffset;
 * </pre>
 * <p>
 * You can also do something simpler (just injecting the already stored screen width of the client computer);
 * <pre>
 * &#64;Inject &#64;JSParam("window.screen.width")
 * private int screenWidth;
 * </pre>
 *
 * <h4>Complex types</h4>
 * <p>
 * <code>&#64;</code>{@link JSParam} also supports injection of complex JavaScript types. To make this work, an
 * equivalent Java class needs to be defined and used as the injection point. The implementation uses Jackson to handle
 * the mapping onto the Java object - so the defined class can use any of the Jackson specific JSON annotations to
 * control the conversion.
 * <pre>
 * @JsonIgnoreProperties(ignoreUnknown = true)
 * public static class Navigator {
 *     @JsonProperty private String vendor;
 *     @JsonProperty private String userAgent;
 *     @JsonProperty private String language;
 *
 *     public getVendor() {
 *         return vendor;
 *     }
 *
 *     public getUserAgent() {
 *         return userAgent;
 *     }
 *
 *     public getLanguage() {
 *         return language;
 *     }
 * }
 *
 * &#64;Inject &#64;JSParam("navigator")
 * private Navigator navigator;
 * </p>
 * <p>
 * Note that the implementation only traverses complex JavaScript types one level. However, if you need to recursively
 * collect certain data, this can be done with some custom JavaScript code.
 *
 * @since 3.3
 * @author Adam Waldenberg, Ejwa Software/Hosting
 * @see JSParamExtension
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JSParam {
	String value();
}
