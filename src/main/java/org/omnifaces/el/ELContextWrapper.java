/*
 * Copyright 2021 OmniFaces
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

import java.util.Locale;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;
import jakarta.faces.FacesWrapper;

/**
 * <p>Provides a simple implementation of {@link ELContext} that can
 * be sub-classed by developers wishing to provide specialized behavior
 * to an existing {@link ELContext} instance. The default
 * implementation of all methods is to call through to the wrapped
 * {@link ELContext}.</p>
 *
 * <p>Usage: extend this class and provide the instance we are wrapping to the overloaded constructor.</p>
 *
 * @author Arjan Tijms
 */
public class ELContextWrapper extends ELContext implements FacesWrapper<ELContext> {

	private ELContext elContext;

	public ELContextWrapper() {
		//
	}

	public ELContextWrapper(ELContext elContext) {
		this.elContext = elContext;
	}

	@Override
	public ELContext getWrapped() {
		return elContext;
	}

	@Override
	public void setPropertyResolved(boolean resolved) {
		getWrapped().setPropertyResolved(resolved);
	}

	@Override
	public boolean isPropertyResolved() {
		return getWrapped().isPropertyResolved();
	}

	@Override
	public void putContext(@SuppressWarnings("rawtypes") Class key, Object contextObject) {
		getWrapped().putContext(key, contextObject);
	}

	@Override
	public Object getContext(@SuppressWarnings("rawtypes") Class key) {
		return getWrapped().getContext(key);
	}

	@Override
	public ELResolver getELResolver() {
		return getWrapped().getELResolver();
	}

	@Override
	public FunctionMapper getFunctionMapper() {
		return getWrapped().getFunctionMapper();
	}

	@Override
	public Locale getLocale() {
		return getWrapped().getLocale();
	}

	@Override
	public void setLocale(Locale locale) {
		getWrapped().setLocale(locale);
	}

	@Override
	public VariableMapper getVariableMapper() {
		return getWrapped().getVariableMapper();
	}

}