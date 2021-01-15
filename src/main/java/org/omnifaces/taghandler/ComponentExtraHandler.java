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
package org.omnifaces.taghandler;

import jakarta.faces.view.facelets.ComponentConfig;
import jakarta.faces.view.facelets.ComponentHandler;
import jakarta.faces.view.facelets.FaceletContext;

import org.omnifaces.component.util.FaceletContextConsumer;

/**
 * Handler that can be used by components which wish to receive various extra services.
 * <p>
 * Those extra services consist of:
 *
 * <ul>
 * <li> Receiving the {@link FaceletContext} for the Facelet in which the component appears
 * </ul>
 *
 * <p>
 * The handler has to be used alongside a component declaration in a Facelets <code>*-taglib.xml</code>, e.g.
 *
 * <pre>
 * 	&lt;tag&gt;
 *		&lt;tag-name&gt;someComponent&lt;/tag-name&gt;
 *		&lt;component&gt;
 *			&lt;component-type&gt;com.example.SomeComponent&lt;/component-type&gt;
 *			&lt;handler-class&gt;org.omnifaces.taghandler.ComponentExtraHandler&lt;/handler-class&gt;
 *		&lt;/component&gt;
 *	&lt;/tag&gt;
 * </pre>
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class ComponentExtraHandler extends ComponentHandler {

	/**
	 * The tag constructor.
	 * @param config The tag config.
	 */
	public ComponentExtraHandler(ComponentConfig config) {
		super(config);
	}

	@Override
	public void setAttributes(FaceletContext ctx, Object component) {
		super.setAttributes(ctx, component);

		if (component instanceof FaceletContextConsumer) {
			FaceletContextConsumer faceletContextConsumer = (FaceletContextConsumer) component;

			faceletContextConsumer.setFaceletContext(ctx);
		}

	}

}
