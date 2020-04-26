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
package org.omnifaces.event;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.omnifaces.component.input.HashParam;

/**
 * <p>
 * This event is fired by <code>&lt;o:hashParam&gt;</code> when hash parameters have been changed in the client side.
 * This is particularly useful in case you intend to (re)run some action based on the new hash parameters, before
 * rendering takes place.
 * <p>
 * Noted should be that the <code>&lt;o:hashParam&gt;</code> fires this as a CDI event, not as a JSF event.
 *
 * @author Bauke Scholtz
 * @since 3.2
 * @see HashParam
 */
public class HashChangeEvent extends ValueChangeEvent {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new hash change event.
	 * @param context The involved faces context.
	 * @param oldHashQueryString The old hash query string.
	 * @param newHashQueryString The new hash query string.
	 */
	public HashChangeEvent(FacesContext context, String oldHashQueryString, String newHashQueryString) {
		super(context, context.getViewRoot(), oldHashQueryString, newHashQueryString);
	}

	// Below overrides are purely to update the javadoc.

	/**
	 * Returns the current view root.
	 * @return The current view root.
	 */
	@Override
	public UIViewRoot getComponent() {
		return (UIViewRoot) super.getComponent();
	}

	/**
	 * Returns the old hash query string value.
	 * @return The old hash query string value.
	 */
	@Override
	public String getOldValue() {
		return (String) super.getOldValue();
	}

	/**
	 * Returns the new hash query string value.
	 * @return The new hash query string value.
	 */
	@Override
	public String getNewValue() {
		return (String) super.getNewValue();
	}
}
