/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.el;

import static java.beans.Introspector.decapitalize;
import static org.omnifaces.util.Utils.isOneInstanceOf;
import static org.omnifaces.util.Utils.startsWithOneOf;

import java.beans.FeatureDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;

import org.omnifaces.util.Faces;

/**
 * This EL resolver basically creates an implicit object <code>#{faces}</code> in EL scope.
 * <p>
 * All methods of {@link Faces} utility class which start with "get" or "is", and take no parameters, and return
 * either <code>String</code> or <code>boolean</code>, and are not related to response nor to session or flash (for
 * which already implicit EL objects <code>#{session}</code> and <code>#{flash}</code> exist), will be available as
 * properties of the implicit object <code>#{faces}</code>. Examples are:
 * <pre>
 * #{faces.development}
 * #{faces.serverInfo}
 * #{faces.ajaxRequest}
 * #{faces.requestBaseURL}
 * #{faces.requestURLWithQueryString}
 * </pre>
 *
 * @author Bauke Scholtz
 * @see Faces
 * @since 2.6
 */
public class FacesELResolver extends ELResolver {

	private static final Map<String, Method> FACES_PROPERTIES = new ConcurrentHashMap<>();

	static {
		for (Method method : Faces.class.getDeclaredMethods()) {
			if (method.getParameterTypes().length > 0 || !isOneInstanceOf(method.getReturnType(), String.class, boolean.class)) {
				continue;
			}

			String name = method.getName();

			if (startsWithOneOf(name, "get", "is")) {
				String property = decapitalize(name.replaceFirst("(get|is)", ""));

				if (!startsWithOneOf(property, "response", "session", "flash")) {
					FACES_PROPERTIES.put(property, method);
				}
			}
		}
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return (base == this) ? Object.class : null;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		if (base == null && "faces".equals(property)) {
			context.setPropertyResolved(true);
			return getClass();
		}
		else if (base == this && property != null) {
			context.setPropertyResolved(true);
			return getCommonPropertyType(context, base);
		}

		return null;
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		if (base == null && "faces".equals(property)) {
			context.setPropertyResolved(true);
			return this;
		}
		else if (base == this && property != null) {
			Method method = FACES_PROPERTIES.get(property);

			if (method == null) {
				throw new PropertyNotFoundException("#{faces." + property + "} does not exist.");
			}

			context.setPropertyResolved(true);

			try {
				return method.invoke(null);
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}

		return null;
	}

	@Override
	public void setValue(ELContext context, Object base, Object property, Object val) {
		// NOOP.
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		return true;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
		return null;
	}

}