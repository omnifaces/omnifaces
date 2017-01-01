/*
 * Copyright 2017 OmniFaces
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
package org.omnifaces.context;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;

import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.cdi.viewscope.ViewScopeManager;
import org.omnifaces.util.Faces;

/**
 * This external context takes care that the {@link Flash} will be ignored during an unload request.
 *
 * @author Bauke Scholtz
 * @since 2.2
 * @see OmniExternalContextFactory
 */
public class OmniExternalContext extends ExternalContextWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Flash DUMMY_FLASH = new DummyFlash();

	// Variables ------------------------------------------------------------------------------------------------------

	private ExternalContext wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new OmniFaces external context around the given wrapped external context.
	 * @param wrapped The wrapped external context.
	 */
	public OmniExternalContext(ExternalContext wrapped) {
		this.wrapped = wrapped;
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * If the current request is an unload request from {@link ViewScoped}, then return a dummy flash scope which does
	 * not modify the flash state, else return the original flash scope.
	 */
	@Override
	public Flash getFlash() {
		return ViewScopeManager.isUnloadRequest(Faces.getContext()) ? DUMMY_FLASH : super.getFlash();
	}

	@Override
	public ExternalContext getWrapped() {
		return wrapped;
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	/**
	 * A dummy flash class which does absolutely nothing with regard to the flash scope.
	 */
	private static class DummyFlash extends Flash {

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public Object get(Object key) {
			return null;
		}

		@Override
		public Object put(String key, Object value) {
			return null;
		}

		@Override
		public Object remove(Object key) {
			return null;
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> m) {
			// NOOP.
		}

		@Override
		public void clear() {
			// NOOP.
		}

		@Override
		public Set<String> keySet() {
			return emptySet();
		}

		@Override
		public Collection<Object> values() {
			return emptySet();
		}

		@Override
		public Set<java.util.Map.Entry<String, Object>> entrySet() {
			return emptySet();
		}

		@Override
		public boolean isKeepMessages() {
			return false;
		}

		@Override
		public void setKeepMessages(boolean newValue) {
			// NOOP.
		}

		@Override
		public boolean isRedirect() {
			return false;
		}

		@Override
		public void setRedirect(boolean newValue) {
			// NOOP.
		}

		@Override
		public void putNow(String key, Object value) {
			// NOOP.
		}

		@Override
		public void keep(String key) {
			// NOOP.
		}

		@Override
		public void doPrePhaseActions(FacesContext ctx) {
			// NOOP.
		}

		@Override
		public void doPostPhaseActions(FacesContext ctx) {
			// NOOP.
		}

	}

}