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
package org.omnifaces.component.output.cache.el;

import static org.omnifaces.util.Faces.getContext;

import java.io.Serializable;
import java.util.Objects;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.omnifaces.component.output.Cache;
import org.omnifaces.el.ValueExpressionWrapper;

/**
 * A value expression implementation that caches its main value at the moment it's evaluated and uses
 * this cache value in future evaluations.
 *
 * @author Arjan Tijms
 *
 */
public class CachingValueExpression extends ValueExpressionWrapper {

	private static final long serialVersionUID = 1L;

	private final String name;
	private final transient Cache cache;

	public CachingValueExpression(String name, ValueExpression valueExpression, Cache cache) {
		super(valueExpression);
		this.name = name;
		this.cache = cache;
	}

	@Override
	public Object getValue(ELContext elContext) {
		FacesContext facesContext = getContext(elContext);

		Serializable value = cache.getCacheAttribute(facesContext, name);
		if (value == null) {
			value = (Serializable) super.getValue(elContext);
			cache.setCacheAttribute(facesContext, name, value);
		}

		return value;
	}

	@Override
	public boolean equals(Object object) {
		return super.equals(object) && Objects.equals(name, ((CachingValueExpression) object).name);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hashCode(name);
	}

}