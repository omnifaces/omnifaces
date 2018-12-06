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
package org.omnifaces.component.util;

import javax.faces.view.facelets.FaceletContext;

import org.omnifaces.taghandler.ComponentExtraHandler;

/**
 * Interface to be implemented by components that wish to receive the {@link FaceletContext} for the
 * Facelet in which they are declared.
 * 
 * <p>
 * This has to be combined with the {@link ComponentExtraHandler}.
 * 
 * @since 2.0
 * @author Arjan Tijms
 * 
 */
public interface FaceletContextConsumer {

	void setFaceletContext(FaceletContext faceletContext);
	
}
