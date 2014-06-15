/*
 * Copyright 2012 OmniFaces.
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
package org.omnifaces.component.input.componentidparam;

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

	private static final long serialVersionUID = -5527348022747113123L;

	private final FacesContext facesContext;
	private final List<String> componentIds;
	private final List<String> clientIds;
	private final boolean renderChildren;

	private ResponseWriter responseWriter;

	public ConditionalWriterListener(FacesContext facesContext, List<String> componentIds, List<String> clientIds, boolean renderChildren) {
		this.facesContext = facesContext;
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
		responseWriter = facesContext.getResponseWriter();
		facesContext.setResponseWriter(new ConditionalResponseWriter(responseWriter, facesContext, componentIds, clientIds, renderChildren));
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		facesContext.setResponseWriter(responseWriter);
	}

}
