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
package org.omnifaces.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.faces.FacesWrapper;

/**
 * <p>Provides a simple implementation of {@link ELResolver} that can
 * be sub-classed by developers wishing to provide specialized behavior
 * to an existing {@link ELResolver} instance. The default
 * implementation of all methods is to call through to the wrapped
 * {@link ELResolver}.</p>
 *
 * <p>Usage: extend this class and override {@link #getWrapped} to
 * return the instance we are wrapping, or provide this instance to the overloaded constructor.</p>
 *
 * @author Arjan Tijms
 */
public class ELResolverWrapper extends ELResolver implements FacesWrapper<ELResolver> {

	private ELResolver elResolver;

	public ELResolverWrapper() {
		//
	}

	public ELResolverWrapper(ELResolver elResolver) {
		this.elResolver = elResolver;
	}

	@Override
	public ELResolver getWrapped() {
		return elResolver;
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		return getWrapped().getValue(context, base, property);
	}

	@Override
	public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
		return getWrapped().invoke(context, base, method, paramTypes, params);
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		return getWrapped().getType(context, base, property);
	}

	@Override
	public void setValue(ELContext context, Object base, Object property, Object value) {
		getWrapped().setValue(context, base, property, value);
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		return getWrapped().isReadOnly(context, base, property);
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
		return getWrapped().getFeatureDescriptors(context, base);
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		return getWrapped().getCommonPropertyType(context, base);
	}

}