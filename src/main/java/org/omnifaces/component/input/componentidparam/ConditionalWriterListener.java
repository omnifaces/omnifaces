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
package org.omnifaces.component.input.componentidparam;

import static org.omnifaces.util.FacesLocal.getContextAttribute;
import static org.omnifaces.util.FacesLocal.setContextAttribute;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.omnifaces.component.input.ComponentIdParam;

/**
 * PhaseListener intended to work in conjunction with the {@link ComponentIdParam} component.
 * <p>
 * This installs a {@link ResponseWriter} that only renders specific components.
 *
 * @since 1.1
 * @author Arjan Tijms
 *
 */
public class ConditionalWriterListener implements PhaseListener {

	private static final long serialVersionUID = 1L;

	private final List<String> componentIds;
	private final List<String> clientIds;
	private final boolean renderChildren;

	public ConditionalWriterListener(List<String> componentIds, List<String> clientIds, boolean renderChildren) {
		this.componentIds = componentIds;
		this.clientIds = clientIds;
		this.renderChildren = renderChildren;
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		FacesContext context = event.getFacesContext();
		ResponseWriter originalWriter = context.getResponseWriter();
		setContextAttribute(context, this + "_writer", originalWriter);
		context.setResponseWriter(new ConditionalResponseWriter(originalWriter, context, componentIds, clientIds, renderChildren));
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		FacesContext context = event.getFacesContext();
		ResponseWriter originalWriter = getContextAttribute(context, this + "_writer");
		context.setResponseWriter(originalWriter);
	}

}
