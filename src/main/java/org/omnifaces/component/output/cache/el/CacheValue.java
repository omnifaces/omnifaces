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
package org.omnifaces.component.output.cache.el;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.component.output.Cache;

/**
 * CacheValue is a replacement for <code>ui:param</code> and <code>c:set</code> that only evaluates a value expression once
 * and thereafter resolves it from the cache.
 * <p>
 * A <code>CacheValue</code> piggybacks onto a parent <code>Cache</code> component for the control of caching scope and
 * other parameters.
 *
 * @author Arjan Tijms
 *
 */
public class CacheValue extends TagHandler {

	private final TagAttribute name;
	private final TagAttribute value;

	public CacheValue(TagConfig config) {
		super(config);
		this.name = this.getRequiredAttribute("name");
		this.value = this.getRequiredAttribute("value");
	}

	@Override
	public void apply(FaceletContext ctx, UIComponent parent) throws IOException {

		Cache cacheComponent;
		if (parent instanceof Cache) {
			cacheComponent = (Cache) parent;
		} else {
			throw new IllegalStateException("CacheValue components needs to have a Cache component as direct parent.");
		}

		String nameStr = name.getValue(ctx);

		// The original value expression we get inside the Facelets tag
		ValueExpression valueExpression = value.getValueExpression(ctx, Object.class);

		ctx.getVariableMapper().setVariable(nameStr, new CachingValueExpression(nameStr, valueExpression, cacheComponent));
	}
}