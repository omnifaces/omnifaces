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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import org.omnifaces.cdi.cookie.RequestCookieProducer;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link Cookie} allows you to inject a HTTP request cookie value from the current
 * Faces context in a CDI managed bean. It's basically like
 * <code>&#64;ManagedProperty("#{cookie.cookieName.value}") private String cookieName;</code>
 * in a "plain old" Faces managed bean.
 * <p>
 * By default the name of the cookie is taken from the name of the variable into which injection takes place.
 * The example below injects the cookie with name <code>foo</code>.
 * <pre>
 * &#64;Inject &#64;Cookie
 * private String foo;
 * </pre>
 * <p>
 * The name can be optionally specified via the <code>name</code> attribute.
 * The example below injects the cookie with name <code>foo</code> into a variable named <code>bar</code>.
 * <pre>
 * &#64;Inject &#64;Cookie(name="foo")
 * private String bar;
 * </pre>
 * <p>
 * Validation is by design not supported as cookies are usually beyond enduser's control.
 * TODO: conversion?
 *
 * @since 2.1
 * @author Bauke Scholtz
 * @see RequestCookieProducer
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER })
public @interface Cookie {

	/**
	 * (Optional) The name of the request cookie. If not specified the name of the injection target field will be used.
	 *
	 * @return The name of the request cookie.
	 */
	@Nonbinding	String name() default "";

}