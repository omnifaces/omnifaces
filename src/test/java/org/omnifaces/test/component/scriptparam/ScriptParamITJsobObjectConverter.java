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
package org.omnifaces.test.component.scriptparam;

import java.io.StringReader;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.json.Json;
import javax.json.JsonObject;

@FacesConverter(forClass = JsonObject.class)
public class ScriptParamITJsobObjectConverter implements Converter<JsonObject> {

	@Override
	public String getAsString(FacesContext context, UIComponent component, JsonObject modelValue) {
		if (modelValue == null) {
			return "";
		}

		return modelValue.toString();
	}

	@Override
	public JsonObject getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (submittedValue == null || submittedValue.isEmpty()) {
			return null;
		}

		try {
			return Json.createReader(new StringReader(submittedValue)).readObject();
		}
		catch (Exception e) {
			throw new ConverterException("Not a valid JSON object", e);
		}
	}

}
