/*
 * Copyright OmniFaces
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
package org.omnifaces.test.cdi.param;

import static org.omnifaces.util.ComponentsLocal.getExpectedValueType;
import static org.omnifaces.util.Messages.asConverterException;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(forClass = ParamITBaseEntity.class)
public class ParamITBaseEntityConverter implements Converter<ParamITBaseEntity> {

    @Inject
    private ParamITBaseEntityService service;

    @Override
    public String getAsString(FacesContext context, UIComponent component, ParamITBaseEntity value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public ParamITBaseEntity getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null) {
            return null;
        }

        try {
            Class<? extends ParamITBaseEntity> type = getExpectedValueType(context, component);
            var id = Long.valueOf(value);
            return service.find(type, id);
        }
        catch (Exception e) {
            throw asConverterException("Cannot convert because it threw " + e);
        }
    }

}
