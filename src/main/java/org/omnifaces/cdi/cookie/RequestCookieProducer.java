/*
 * Copyright 2016 OmniFaces
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
package org.omnifaces.cdi.cookie;

import static org.omnifaces.util.Beans.getQualifier;
import static org.omnifaces.util.Faces.getRequestCookie;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.omnifaces.cdi.Cookie;

/**
 * <p>
 * Producer for injecting a JSF request cookie as defined by the <code>&#64;</code>{@link Cookie} annotation.
 *
 * @since 2.1
 * @author Bauke Scholtz
 */
@Dependent
public class RequestCookieProducer {

	@SuppressWarnings("unused") // Workaround for OpenWebBeans not properly passing it as produce() method argument.
	@Inject
	private InjectionPoint injectionPoint;

	@Produces
	@Cookie
	public String produce(InjectionPoint injectionPoint) {
		Cookie cookie = getQualifier(injectionPoint, Cookie.class);
		String name = cookie.name().isEmpty() ? injectionPoint.getMember().getName() : cookie.name();
		return getRequestCookie(name);
	}

}