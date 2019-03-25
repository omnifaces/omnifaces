/*
 * Copyright 2019 OmniFaces
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
package org.omnifaces.cdi.jsparam;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import org.omnifaces.util.Components;
import org.omnifaces.util.Faces;

public class JSParamActionListener implements ActionListener {
	@Override
	public void processAction(ActionEvent event) throws AbortProcessingException {
		Components.getCurrentForm().getChildren().forEach(component -> {
			final Object o = Components.getCurrentForm().getAttributes().get("instance");

			if (component instanceof HtmlInputHidden) {
				final HtmlInputHidden input = (HtmlInputHidden) component;
				final String value = Faces.getRequestParameter(input.getClientId());
				final AnnotatedField field = (AnnotatedField) input.getAttributes().get("field");
				final Class<?> baseType = field.getJavaMember().getType();

				field.getJavaMember().setAccessible(true);

				try {
					if (baseType == Integer.class || baseType == int.class) {
						field.getJavaMember().setInt(o, Integer.parseInt(value));
					} else if (baseType == String.class) {
						field.getJavaMember().set(o, value);
					} else {
						field.getJavaMember().set(o,
							new ObjectMapper().readValue(value, baseType)
						);
					}
				} catch (IOException | IllegalAccessException | IllegalArgumentException ex) {
					throw new InjectionException(ex);
				}
			}
		});
	}
}
